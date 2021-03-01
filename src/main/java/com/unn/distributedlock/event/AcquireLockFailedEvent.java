package com.unn.distributedlock.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取锁失败的事件
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AcquireLockFailedEvent {
    /**
     * 被锁方法的请求参数
     */
    private Object[] args;

}
