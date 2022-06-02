package com.unn.distributedlock.core;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * redis锁
 *
 * @author yangjiyun
 */
public interface RedisLock extends Lock {


    /**
     * Conditions are not supported
     */
    default Condition newCondition() {
        throw new UnsupportedOperationException("Conditions are not supported");
    }

    /**
     * 获取锁的名字
     * @return 锁的名字 lockName
     */
    String getLockName();

    /**
     * 获取锁的key
     * @return 锁的key    registryKey:lockName
     */
    String getLockKey();
}
