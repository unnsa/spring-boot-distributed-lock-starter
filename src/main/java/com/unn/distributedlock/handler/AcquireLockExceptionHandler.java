package com.unn.distributedlock.handler;

import com.unn.distributedlock.annotation.DistributedLock;

/**
 * 获取锁失败异常处理器
 * 自定义处理器续实现该接口并重新handler方法
 */
public interface AcquireLockExceptionHandler {

    /**
     * 处理逻辑
     *
     * @param args 被锁方法的请求参数
     * @param e    获取锁失败异常信息
     * @param distributedLock 锁的信息
     */
    void handler(Object[] args, Throwable e, DistributedLock distributedLock);
}
