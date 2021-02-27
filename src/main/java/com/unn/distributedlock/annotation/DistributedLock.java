package com.unn.distributedlock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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


}
