package com.unn.distributedlock.event;

import com.unn.distributedlock.annotation.DistributedLock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 解锁之后的事件
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AfterUnlockSuccessEvent {

    /**
     * 被锁方法的请求参数
     */
    private Object[] args;
    /**
     * 锁的相关信息
     */
    private DistributedLock distributedLock;
}
