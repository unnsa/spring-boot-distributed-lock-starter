package com.unn.distributedlock.script;

/**
 * redis脚本
 *
 * @author yangjiyun
 */
public @interface RedisLockScript {

    /**
     * 获取锁
     */
    String OBTAIN_LOCK_SCRIPT =
            "local lockClientId = redis.call('GET', KEYS[1])\n" +
                    "if lockClientId == ARGV[1] then\n" +
                    "  redis.call('PEXPIRE', KEYS[1], ARGV[2])\n" +
                    "  return true\n" +
                    "elseif not lockClientId then\n" +
                    "  redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])\n" +
                    "  return true\n" +
                    "end\n" +
                    "return false";

    /**
     * 释放锁
     */
    String RELEASE_LOCK_SCRIPT =
            "local lockClientId = redis.call('GET', KEYS[1])\n" +
                    "if lockClientId == ARGV[1] then\n" +
                    "  redis.call('DEL', KEYS[1])\n" +
                    "  return true\n" +
                    "end\n" +
                    "return false";
}
