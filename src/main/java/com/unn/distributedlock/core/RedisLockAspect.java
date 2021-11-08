package com.unn.distributedlock.core;

import com.unn.distributedlock.annotation.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String PARAMETER_EXPRESSION = "\\$\\{(.*?)\\}";
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(PARAMETER_EXPRESSION);

    private Optional<String> getParamName(String str) {
        return Optional.ofNullable(str)
                .map(PARAMETER_PATTERN::matcher)
                .filter(Matcher::find)
                .map(m -> m.group(1));
    }


    @Around(value = "@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        Lock lock = getLock(joinPoint, distributedLock);
        var lockedOp = Optional.ofNullable(getLockOperate(distributedLock)
                        .apply(lock))
                .filter(r -> r);
        if (!lockedOp.isPresent()) {
            log.info("Failed to lock mutex at " + distributedLock.key());
            return null;
        }
        log.debug("------lock success------");
        Object proceed = joinPoint.proceed();
        lock.unlock();
        log.debug("------release lock success------");
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
    private Lock getLock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        RedisLockRegistry lockRegistry;
        String[] parameterNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();
        Class<?>[] parameterTypes = ((CodeSignature) joinPoint.getSignature()).getParameterTypes();
        Object[] args = joinPoint.getArgs();
        Optional<String> registryKeyOp = getParamName(distributedLock.name());
        Optional<String> lockKeyOp = getParamName(distributedLock.key());
        for (int i = 0; i < parameterNames.length; i++) {
            if (registryKeyOp
                    .filter(parameterNames[i]::equals)
                    .isPresent() && parameterTypes[i] == String.class) {
                registryKeyOp = Optional.ofNullable((String) args[i]);
            }

            if (lockKeyOp
                    .filter(parameterNames[i]::equals)
                    .isPresent() && parameterTypes[i] == String.class) {
                lockKeyOp = Optional.ofNullable((String) args[i]);
            }
        }
        String registryKey = registryKeyOp
                .orElse(distributedLock.name());
        String lockKey = lockKeyOp
                .orElse(distributedLock.key());
        if (distributedLock.keepLease()) {
            lockRegistry = redisLockRegistryUtil.getLockRegistryAutoKeepLease(registryKey, distributedLock.expiredTime(), distributedLock.expiredTimeUnit());
        } else {
            lockRegistry = redisLockRegistryUtil.getLockRegistryNotKeepLease(registryKey, distributedLock.expiredTime(), distributedLock.expiredTimeUnit());
        }
        return lockRegistry
                .obtain(lockKey);
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
