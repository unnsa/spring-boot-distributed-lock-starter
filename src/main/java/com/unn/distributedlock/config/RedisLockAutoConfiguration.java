package com.unn.distributedlock.config;

import com.unn.distributedlock.aspect.RedisLockAspect;
import com.unn.distributedlock.handler.AcquireLockExceptionHandler;
import com.unn.distributedlock.handler.AcquireLockFailedHandler;
import com.unn.distributedlock.handler.AfterUnlockSuccessHandler;
import com.unn.distributedlock.handler.BeforeLockHandlerHandler;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.support.locks.ExpirableLockRegistry;

/**
 * redis lock auto configuration
 *
 * @author yangjiyun
 */
@Configuration
@ConditionalOnClass({StringRedisTemplate.class, ExpirableLockRegistry.class,})
@AutoConfigureAfter(RedisAutoConfiguration.class)
@Import({RedisLockAspect.class})
public class RedisLockAutoConfiguration {

    /**
     * 默认的获取锁失败后的处理器注入到ioc容器
     *
     * @return 默认的获取锁失败后的处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public AcquireLockFailedHandler defaultAcquireLockFailedHandler() {
        return args -> {
        };
    }

    /**
     * 默认的获取锁失败异常处理器注入到ioc容器
     *
     * @return 默认的获取锁失败后的处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public AcquireLockExceptionHandler defaultAcquireExceptionHandler() {
        return (args, e, distributedLock) -> {
        };
    }

    /**
     * 默认的解锁之后的处理器注入到ioc容器
     *
     * @return 默认的获取锁失败后的处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public AfterUnlockSuccessHandler defaultAfterUnlockSuccessHandler() {
        return (args, distributedLock) -> {
        };
    }

    /**
     * 默认的加锁之前的处理器注入到ioc容器
     *
     * @return 默认的获取锁失败后的处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public BeforeLockHandlerHandler defaultBeforeLockHandlerHandler() {
        return (args, distributedLock) -> {
        };
    }

}
