package com.unn.distributedlock.event;

import com.unn.distributedlock.annotation.DistributedLock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取锁失败异常时的事件
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AcquireLockExceptionEvent {
    /**
     * 被锁方法的请求参数
     */
    private Object[] args;
    /**
     * 获取锁失败异常信息
     */
    Throwable e;
    /**
     * 锁的信息
     */
    DistributedLock distributedLock;

}
