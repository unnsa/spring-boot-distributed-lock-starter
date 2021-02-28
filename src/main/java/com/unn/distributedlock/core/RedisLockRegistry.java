package com.unn.distributedlock.core;

import com.unn.distributedlock.script.RedisLockScript;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.integration.support.locks.ExpirableLockRegistry;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 可重入分布式锁
 */
@Slf4j
public final class RedisLockRegistry implements ExpirableLockRegistry, DisposableBean {

    private static final long DEFAULT_EXPIRE_AFTER = 60000L;


    private final Map<String, DefaultRedisLock> locks = new ConcurrentHashMap<>();

    private final String clientId = UUID.randomUUID().toString();

    private final String registryKey;

    private final StringRedisTemplate redisTemplate;

    private final RedisScript<Boolean> obtainLockScript;

    private final RedisScript<Boolean> releaseLockScript;

    private final RedisScript<Boolean> nbReleaseLockScript;

    private final long expireAfter;

    /**
     * 当方法耗时比较长的时候，为了防止锁过期，是否自动延长锁的过期时间
     */
    private final boolean autoExtendTime;

    private final TimeUnit timeUtil;
    /**
     * 自动延长锁的过期时间线程池 TODO
     */
    private final ExecutorService autoExtendExecutor = Executors.newSingleThreadExecutor();

    {
        this.obtainLockScript = new DefaultRedisScript<>(RedisLockScript.OBTAIN_LOCK_SCRIPT, Boolean.class);
        this.releaseLockScript = new DefaultRedisScript<>(RedisLockScript.RELEASE_LOCK_SCRIPT, Boolean.class);
        this.nbReleaseLockScript = new DefaultRedisScript<>(RedisLockScript.NB_RELEASE_LOCK_SCRIPT, Boolean.class);
    }

    /**
     * An {@link ExecutorService} to call {@link StringRedisTemplate#delete} in
     * the separate thread when the current one is interrupted.
     */
    private Executor executor =
            Executors.newCachedThreadPool(new CustomizableThreadFactory("redis-lock-registry-"));

    /**
     * Flag to denote whether the {@link ExecutorService} was provided via the setter and
     * thus should not be shutdown when {@link #destroy()} is called
     */
    private boolean executorExplicitlySet;

    /**
     * Constructs a lock registry with the default (60 second) lock expiration.
     *
     * @param connectionFactory The connection factory.
     * @param registryKey       The key prefix for locks.
     */
    public RedisLockRegistry(RedisConnectionFactory connectionFactory, String registryKey) {
        this(connectionFactory, registryKey, DEFAULT_EXPIRE_AFTER, TimeUnit.MILLISECONDS, true);
    }

    /**
     * Constructs a lock registry with the supplied lock expiration.
     *
     * @param connectionFactory The connection factory.
     * @param registryKey       The key prefix for locks.
     * @param expireAfter       The expiration.
     * @param timeUtil          The expiration time unit
     * @param autoExtendTime    true: extend time,false : no
     */
    public RedisLockRegistry(RedisConnectionFactory connectionFactory, String registryKey, long expireAfter, TimeUnit timeUtil, boolean autoExtendTime) {
        Assert.notNull(connectionFactory, "'connectionFactory' cannot be null");
        Assert.notNull(registryKey, "'registryKey' cannot be null");
        this.redisTemplate = new StringRedisTemplate(connectionFactory);
        this.registryKey = registryKey;
        this.expireAfter = expireAfter;
        this.timeUtil = timeUtil;
        this.autoExtendTime = autoExtendTime;
    }

    /**
     * Set the {@link Executor}, where is not provided then a default of
     * cached thread pool Executor will be used.
     *
     * @param executor the executor service
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
        this.executorExplicitlySet = true;
    }

    /**
     * Obtains the lock associated with the parameter object.
     *
     * @param lockKey The object with which the lock is associated.
     * @return The associated lock.
     */
    @Override
    public Lock obtain(Object lockKey) {
        Assert.isInstanceOf(String.class, lockKey);
        return this.locks.computeIfAbsent((String) lockKey, DefaultRedisLock::new);
    }

    /**
     * Remove locks last acquired more than 'age' ago that are not currently locked.
     *
     * @param age the time since the lock was last obtained.
     * @throws IllegalStateException if the registry configuration does not support this feature.
     */
    @Override
    public void expireUnusedOlderThan(long age) {
        Iterator<Map.Entry<String, DefaultRedisLock>> iterator = this.locks.entrySet().iterator();
        long now = System.currentTimeMillis();
        while (iterator.hasNext()) {
            Map.Entry<String, DefaultRedisLock> entry = iterator.next();
            DefaultRedisLock lock = entry.getValue();
            if (now - lock.getLockedAt() > age && !lock.isAcquiredInThisProcess()) {
                iterator.remove();
            }
        }
    }

    @Override
    public void destroy() {
        if (!this.executorExplicitlySet) {
            ((ExecutorService) this.executor).shutdown();
        }
    }

    @Data
    private final class DefaultRedisLock implements RedisLock {

        private final String lockKey;

        private final ReentrantLock localLock = new ReentrantLock();

        private volatile boolean unlinkAvailable = true;

        private volatile long lockedAt;

        private DefaultRedisLock(String path) {
            this.lockKey = constructLockKey(path);
        }

        private String constructLockKey(String path) {
            return RedisLockRegistry.this.registryKey + ":" + path;
        }


        @Override
        public void lock() {
            this.localLock.lock();
            while (true) {
                try {
                    while (!obtainLock()) {
                        Thread.sleep(100); //NOSONAR
                    }
                    break;
                } catch (InterruptedException e) {
                    /*
                     * This method must be uninterruptible so catch and ignore
                     * interrupts and only break out of the while loop when
                     * we get the lock.
                     */
                } catch (Exception e) {
                    this.localLock.unlock();
                    rethrowAsLockException(e);
                }
            }
        }

        private void rethrowAsLockException(Exception e) {
            throw new CannotAcquireLockException("Failed to lock mutex at " + this.lockKey, e);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            this.localLock.lockInterruptibly();
            try {
                while (!obtainLock()) {
                    Thread.sleep(100); //NOSONAR
                }
            } catch (InterruptedException ie) {
                this.localLock.unlock();
                Thread.currentThread().interrupt();
                throw ie;
            } catch (Exception e) {
                this.localLock.unlock();
                rethrowAsLockException(e);
            }
        }

        @Override
        public boolean tryLock() {
            try {
                return tryLock(0, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            long now = System.currentTimeMillis();
            if (!localLock.tryLock(time, unit)) {
                return false;
            }
            try {
                long expire = now + TimeUnit.MILLISECONDS.convert(time, unit);
                boolean acquired;
                while (!(acquired = obtainLock()) && System.currentTimeMillis() < expire) { //NOSONAR
                    Thread.sleep(100); //NOSONAR
                }
                if (!acquired) {
                    this.localLock.unlock();
                }
                return acquired;
            } catch (Exception e) {
                this.localLock.unlock();
                rethrowAsLockException(e);
            }
            return false;
        }

        /**
         * 获取锁
         */
        private boolean obtainLock() {
            Boolean success =
                    redisTemplate.execute(obtainLockScript,
                            Collections.singletonList(this.lockKey), clientId,
                            String.valueOf(timeUtil.toMillis(expireAfter)));

            boolean result = Boolean.TRUE.equals(success);

            if (result) {
                this.lockedAt = System.currentTimeMillis();
            }
            return result;
        }

        //TODO
        private void addExtendTimeTask(DefaultRedisLock defaultRedisLock){
            if (autoExtendTime){

            }
        }

        /**
         * 释放锁
         */
        private boolean releaseLock() {
            return Optional.ofNullable(redisTemplate.execute(releaseLockScript,
                    Collections.singletonList(this.lockKey), clientId))
                    .filter(r -> r)
                    .isPresent();
        }

        /**
         * 非阻塞释放锁
         */
        private boolean nbReleaseLock() {
            return Optional.ofNullable(redisTemplate.execute(nbReleaseLockScript,
                    Collections.singletonList(this.lockKey), clientId))
                    .filter(r -> r)
                    .isPresent();
        }

        @Override
        public void unlock() {
            if (!localLock.isHeldByCurrentThread()) {
                throw new IllegalStateException("You do not own lock at " + this.lockKey);
            }
            if (localLock.getHoldCount() > 1) {
                localLock.unlock();
                return;
            }
            try {
                if (!isAcquiredInThisProcess()) {
                    throw new IllegalStateException("Lock was released in the store due to expiration. " +
                            "The integrity of data protected by this lock may have been compromised.");
                }

                if (Thread.currentThread().isInterrupted()) {
                    RedisLockRegistry.this.executor.execute(this::removeLockKey);
                } else {
                    releaseLock();
                }

                if (log.isDebugEnabled()) {
                    log.debug("Released lock; " + this);
                }
            } catch (Exception e) {
                ReflectionUtils.rethrowRuntimeException(e);
            } finally {
                this.localLock.unlock();
            }
        }

        private void removeLockKey() {
            if (unlinkAvailable) {
                try {
                    nbReleaseLock();
                } catch (Exception ex) {
                    log.warn("The UNLINK command has failed (not supported on the Redis server?); " +
                            "falling back to the regular DELETE command", ex);
                    this.unlinkAvailable = false;
                    releaseLock();
                }
            } else {
                releaseLock();
            }
        }


        public boolean isAcquiredInThisProcess() {
            return RedisLockRegistry.this.clientId.equals(
                    RedisLockRegistry.this.redisTemplate.boundValueOps(this.lockKey).get());
        }

    }

}
