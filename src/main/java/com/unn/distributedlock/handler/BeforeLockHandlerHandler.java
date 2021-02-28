package com.unn.distributedlock.handler;

import com.unn.distributedlock.annotation.DistributedLock;

/**
 * 加锁之前的处理
 * 自定义处理器续实现该接口并重新handler方法
 */
public interface BeforeLockHandlerHandler {

    /**
     * 处理逻辑
     *
     * @param args   被锁方法的请求参数
     */
    void handler(Object[] args, DistributedLock distributedLock);
}
