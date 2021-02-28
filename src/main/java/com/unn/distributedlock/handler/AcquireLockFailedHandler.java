package com.unn.distributedlock.handler;

/**
 * 获取锁失败处理器
 * 自定义处理器续实现该接口并重新handler方法
 */
public interface AcquireLockFailedHandler {

    /**
     * 处理逻辑
     *
     * @param args 被锁方法的请求参数
     */
    void handler(Object[] args);
}
