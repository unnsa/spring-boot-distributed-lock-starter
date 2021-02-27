package com.unn.distributedlock.config;

import com.unn.distributedlock.aspect.RedisLockAspect;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * redis lock auto configuration
 *
 * @author yangjiyun
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@Import({RedisLockAspect.class})
public class RedisLockAutoConfiguration {

}
