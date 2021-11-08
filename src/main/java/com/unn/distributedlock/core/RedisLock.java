package com.unn.distributedlock.core;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * redis锁
 *
 * @author yangjiyun
 */
interface RedisLock extends Lock {


    /**
     * Conditions are not supported
     */
    default Condition newCondition() {
        throw new UnsupportedOperationException("Conditions are not supported");
    }
}
