package com.unn.distributedlock.util;

import com.unn.distributedlock.core.RedisLock;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * 使用锁工具类
 * @author jiyunyang
 */
@UtilityClass
public class LockUtil {
    /**
     * 获取到指定的锁后执行操作
     *
     * @param lock     锁对象
     * @param waitTime 等待时间
     * @param timeUnit 时间单位
     * @param supplier 要执行的操作
     * @param <R>      返回值类型
     * @return 操作返回值
     */
    @SneakyThrows
    public static <R> R executeWhenGetLock(Lock lock, long waitTime, TimeUnit timeUnit, SupplierThrowable<R> supplier) {
        if (lock.tryLock(waitTime, timeUnit)) {
            try {
                return supplier.get();
            } finally {
                lock.unlock();
            }
        } else {
            throw new InterruptedException("未获取到指定的锁" + getLockKeyIfRedisLock(lock));
        }
    }


    /**
     * 获取到指定的锁后执行操作
     *
     * @param lock     锁对象
     * @param supplier 要执行的操作
     * @param <R>      返回值类型
     * @return 操作返回值
     */
    @SneakyThrows
    public static <R> R executeWhenGetLock(Lock lock, SupplierThrowable<R> supplier) {
        if (lock.tryLock()) {
            try {
                return supplier.get();
            } finally {
                lock.unlock();
            }
        } else {
            throw new InterruptedException("未获取到指定的锁" + getLockKeyIfRedisLock(lock));
        }
    }


    /**
     * 获取到指定的锁后执行操作
     *
     * @param lock     锁对象
     * @param runnable 要执行的操作
     */
    @SneakyThrows
    public static void executeWhenGetLock(Lock lock, RunnableThrowable runnable) {
        if (lock.tryLock()) {
            try {
                runnable.run();
            } finally {
                lock.unlock();
            }
        } else {
            throw new InterruptedException("未获取到指定的锁" + getLockKeyIfRedisLock(lock));
        }
    }


    /**
     * 获取到指定的锁后执行操作
     *
     * @param lock     锁对象
     * @param waitTime 等待时间
     * @param timeUnit 时间单位
     * @param runnable 要执行的操作
     */
    @SneakyThrows
    public static void executeWhenGetLock(Lock lock, long waitTime, TimeUnit timeUnit, RunnableThrowable runnable) {
        if (lock.tryLock(waitTime, timeUnit)) {
            try {
                runnable.run();
            } finally {
                lock.unlock();
            }
        } else {
            throw new InterruptedException("未获取到指定的锁" + getLockKeyIfRedisLock(lock));
        }
    }

    private String getLockKeyIfRedisLock(Lock lock) {
        if (lock instanceof RedisLock) {
            return ((RedisLock) lock).getLockKey();
        }
        return "";
    }
}
