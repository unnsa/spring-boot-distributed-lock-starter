package com.unn.distributedlock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 在方法上标注，表示进入该方法需要获取redis分布式锁，
 * 如果获取失败将阻塞不断重试获取锁，直到获取倒锁为止
 * @author yangjiyun
 */
@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    /**
     * 锁的名字
     */
    String name();

    /**
     * 锁的业务key
     */
    String key();

    /**
     * 锁的过期时间
     */
    long expiredTime();

    /**
     * 过期时间单位
     * 默认毫秒
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;


}
