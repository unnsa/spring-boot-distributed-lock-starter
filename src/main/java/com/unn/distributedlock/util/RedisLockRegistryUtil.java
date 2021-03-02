package com.unn.distributedlock.util;

import com.unn.distributedlock.core.RedisLockRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * 方便获取RedisLockRegistry
 */
@Component
@RequiredArgsConstructor
public class RedisLockRegistryUtil {
    private final RedisConnectionFactory redisConnectionFactory;
    private final Map<String, RedisLockRegistry> redisLockRegistryMap = new ConcurrentHashMap<>();

    public RedisLockRegistry getLockRegistryAutoKeepLease(String registryKey,
                                                          long expireAfter,
                                                          TimeUnit timeUtil) {
        String registryName = registryKey + "-keepLease";
        return Optional.ofNullable(redisLockRegistryMap.get(registryName))
                .orElseGet(() -> {
                    RedisLockRegistry redisLockRegistry = new RedisLockRegistry(redisConnectionFactory,
                            registryKey,
                            expireAfter,
                            timeUtil,
                            true);
                    redisLockRegistryMap.put(registryName, redisLockRegistry);
                    return redisLockRegistry;
                });
    }

    public RedisLockRegistry getLockRegistryNoKeepLease(String registryKey,
                                                        long expireAfter,
                                                        TimeUnit timeUtil) {
        String registryName = registryKey + "-doNotKeepLease";
        return Optional.ofNullable(redisLockRegistryMap.get(registryName))
                .orElseGet(() -> {
                    RedisLockRegistry redisLockRegistry = new RedisLockRegistry(redisConnectionFactory,
                            registryKey,
                            expireAfter,
                            timeUtil,
                            false);
                    redisLockRegistryMap.put(registryName, redisLockRegistry);
                    return redisLockRegistry;
                });
    }

    /**
     * 获取自动续租的锁
     *
     * @param registryKey aka lock name
     * @param expireAfter 过期时间
     * @param timeUtil    过期时间单位
     * @param lockKey     锁的key
     * @return Lock
     */
    public Lock getLockAutoKeepLease(String registryKey,
                                     long expireAfter,
                                     TimeUnit timeUtil,
                                     String lockKey) {
        return getLockRegistryAutoKeepLease(registryKey, expireAfter, timeUtil).obtain(lockKey);
    }

    /**
     * 获取锁
     *
     * @param registryKey aka lock name
     * @param expireAfter 过期时间
     * @param timeUtil    过期时间单位
     * @param lockKey     锁的key
     * @return Lock
     */
    public Lock getLockNoKeepLease(String registryKey,
                                   long expireAfter,
                                   TimeUnit timeUtil,
                                   String lockKey) {
        return getLockRegistryNoKeepLease(registryKey, expireAfter, timeUtil).obtain(lockKey);
    }
}
