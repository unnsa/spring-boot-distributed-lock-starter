package com.unn.distributedlock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
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
