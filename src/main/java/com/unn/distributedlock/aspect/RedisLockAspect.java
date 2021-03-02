package com.unn.distributedlock.aspect;

import com.unn.distributedlock.annotation.DistributedLock;
import com.unn.distributedlock.core.RedisLockRegistry;
import com.unn.distributedlock.util.RedisLockRegistryUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

/**
 * 分布式锁注解处理切面
 *
 * @author yangjiyun
 */
@Aspect
@Component
@Order(0)
@Slf4j
@RequiredArgsConstructor
public class RedisLockAspect {
    private final RedisLockRegistryUtil redisLockRegistryUtil;


    @Around(value = "@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        Lock lock = getLock(distributedLock);
        var lockedOp = Optional.ofNullable(getLockOperate(distributedLock)
                .apply(lock))
                .filter(r -> r);
        if (!lockedOp.isPresent()) {
            log.info("Failed to lock mutex at " + distributedLock.key());
            return null;
        }
        log.debug("------加锁成功,开始处理业务逻辑------");
        Object proceed = joinPoint.proceed();
        lock.unlock();
        log.debug("------业务逻辑处理完毕，释放锁------");
        return proceed;
    }

    @Before(value = "@annotation(distributedLock)")
    public void before(JoinPoint joinPoint, DistributedLock distributedLock) {
    }

    @AfterReturning(value = "@annotation(distributedLock)")
    public void afterReturning(JoinPoint joinPoint, DistributedLock distributedLock) {
    }

    @AfterThrowing(value = "@annotation(distributedLock)", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, DistributedLock distributedLock, Throwable ex) {
    }


    /**
     * 获取锁
     */
    private Lock getLock(DistributedLock distributedLock) {
        RedisLockRegistry lockRegistry;
        if (distributedLock.keepLease()) {
            lockRegistry = redisLockRegistryUtil.getLockRegistryAutoKeepLease(distributedLock.name(), distributedLock.expiredTime(), distributedLock.expiredTimeUnit());
        } else {
            lockRegistry = redisLockRegistryUtil.getLockRegistryNoKeepLease(distributedLock.name(), distributedLock.expiredTime(), distributedLock.expiredTimeUnit());
        }
        return lockRegistry
                .obtain(distributedLock.key());
    }

    /**
     * 获取获取锁的方式
     * 等待时间大于0 用tryLock(long time, TimeUnit unit)，否则用lock()
     */
    private Function<Lock, Boolean> getLockOperate(DistributedLock distributedLock) {
        if (distributedLock.waitTime() > 0) {
            return lock -> {
                try {
                    return lock.tryLock(distributedLock.waitTime(), distributedLock.waitTimeUnit());
                } catch (InterruptedException ignore) {
                    return false;
                }
            };
        }
        return lock -> {
            lock.lock();
            return true;
        };
    }

}
