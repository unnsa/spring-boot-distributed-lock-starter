package com.unn.distributedlock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 在方法上标注，表示进入该方法需要获取redis分布式锁，
 * 如果获取失败将阻塞不断重试获取锁，直到获取倒锁为止
 *
 * @author yangjiyun
 */
@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    /**
     * 锁注册器的key
     */
    String registryKey();

    /**
     * 锁的key
     */
    String lockKey();

    /**
     * 锁的过期时间
     */
    long expiredTime();

    /**
     * 过期时间单位
     * 默认毫秒
     */
    TimeUnit expiredTimeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 获取锁时的等待时间，超时不再尝试获取
     * 小于等于0时会一直阻塞重试，直到获取到锁
     */
    long waitTime() default -1;

    /**
     * 过期时间单位
     * 默认毫秒
     */
    TimeUnit waitTimeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 当方法耗时比较长的时候，为了防止锁过期，是否自动延长锁的过期时间
     * true时自动续期
     */
    boolean keepLease() default true;


}
