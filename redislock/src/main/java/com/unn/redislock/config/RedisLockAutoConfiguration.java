package com.unn.redislock.config;

import com.unn.distributedlock.core.DistributeLockAspect;
import com.unn.redislock.RedisLockRegistryUtil;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
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
@Import({DistributeLockAspect.class, RedisLockRegistryUtil.class})
public class RedisLockAutoConfiguration {

}
