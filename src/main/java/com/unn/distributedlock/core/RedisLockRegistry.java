/*
 * Copyright 2014-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of {@link ExpirableLockRegistry} providing a distributed lock using Redis.
 * Locks are stored under the key {@code registryKey:lockKey}. Locks expire after
 * (default 60) seconds. Threads unlocking an
 * expired lock will get an {@link IllegalStateException}. This should be
 * considered as a critical error because it is possible the protected
 * resources were compromised.
 * <p>
 * Locks are reentrant.
 * <p>
 * <b>However, locks are scoped by the registry; a lock from a different registry with the
 * same key (even if the registry uses the same 'registryKey') are different
 * locks, and the second cannot be acquired by the same thread while the first is
 * locked.</b>
 * <p>
 * <b>Note: This is not intended for low latency applications.</b> It is intended
 * for resource locking across multiple JVMs.
 * <p>
 * {@link Condition}s are not supported.
 *
 * @author Gary Russell
 * @author Konstantin Yakimov
 * @author Artem Bilan
 * @author Vedran Pavic
 * @since 4.0
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

    private final long expireAfter;

    {
        this.obtainLockScript = new DefaultRedisScript<>(RedisLockScript.OBTAIN_LOCK_SCRIPT, Boolean.class);
        this.releaseLockScript = new DefaultRedisScript<>(RedisLockScript.RELEASE_LOCK_SCRIPT, Boolean.class);
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
        this(connectionFactory, registryKey, DEFAULT_EXPIRE_AFTER);
    }

    /**
     * Constructs a lock registry with the supplied lock expiration.
     *
     * @param connectionFactory The connection factory.
     * @param registryKey       The key prefix for locks.
     * @param expireAfter       The expiration in milliseconds.
     */
    public RedisLockRegistry(RedisConnectionFactory connectionFactory, String registryKey, long expireAfter) {
        Assert.notNull(connectionFactory, "'connectionFactory' cannot be null");
        Assert.notNull(registryKey, "'registryKey' cannot be null");
        this.redisTemplate = new StringRedisTemplate(connectionFactory);
        this.registryKey = registryKey;
        this.expireAfter = expireAfter;
    }

    /**
     * Set the {@link Executor}, where is not provided then a default of
     * cached thread pool Executor will be used.
     *
     * @param executor the executor service
     * @since 5.0.5
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
        this.executorExplicitlySet = true;
    }

    @Override
    public Lock obtain(Object lockKey) {
        Assert.isInstanceOf(String.class, lockKey);
        String path = (String) lockKey;
        return this.locks.computeIfAbsent(path, DefaultRedisLock::new);
    }

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
            if (!this.localLock.tryLock(time, unit)) {
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
                    RedisLockRegistry.this.redisTemplate.execute(RedisLockRegistry.this.obtainLockScript,
                            Collections.singletonList(this.lockKey), RedisLockRegistry.this.clientId,
                            String.valueOf(RedisLockRegistry.this.expireAfter));

            boolean result = Boolean.TRUE.equals(success);

            if (result) {
                this.lockedAt = System.currentTimeMillis();
            }
            return result;
        }

        /**
         * 释放锁
         */
        private boolean releaseLock() {
            Boolean success =
                    RedisLockRegistry.this.redisTemplate.execute(RedisLockRegistry.this.releaseLockScript,
                            Collections.singletonList(this.lockKey), RedisLockRegistry.this.clientId,
                            String.valueOf(RedisLockRegistry.this.expireAfter));

            return Boolean.TRUE.equals(success);
        }

        @Override
        public void unlock() {
            if (!this.localLock.isHeldByCurrentThread()) {
                throw new IllegalStateException("You do not own lock at " + this.lockKey);
            }
            if (this.localLock.getHoldCount() > 1) {
                this.localLock.unlock();
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
            if (this.unlinkAvailable) {
                try {
                    RedisLockRegistry.this.redisTemplate.unlink(this.lockKey);
                } catch (Exception ex) {
                    log.warn("The UNLINK command has failed (not supported on the Redis server?); " +
                            "falling back to the regular DELETE command", ex);
                    this.unlinkAvailable = false;
                    RedisLockRegistry.this.redisTemplate.delete(this.lockKey);
                }
            } else {
                RedisLockRegistry.this.redisTemplate.delete(this.lockKey);
            }
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Conditions are not supported");
        }

		public boolean isAcquiredInThisProcess() {
			return RedisLockRegistry.this.clientId.equals(
					RedisLockRegistry.this.redisTemplate.boundValueOps(this.lockKey).get());
		}


        private RedisLockRegistry getOuterType() {
            return RedisLockRegistry.this;
        }

    }

}
