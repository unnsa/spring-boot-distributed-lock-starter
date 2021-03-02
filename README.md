## 分布式锁
1. 支持重入

2. 支持自动续租.
   当到达锁的过期时间`expireAfter`的2/3时,如果还未解锁,则自动续租,将锁的过期时间重置为`expireAfter`
   
3. 使用lua脚本, 操作redis具有原子性

4. 例子:

   ```java
    @Autowired
   private RedisLockRegistryUtil redisLockRegistryUtil;
   public void test2() throws InterruptedException {
           Lock lock = redisLockRegistryUtil.getLockAutoKeepLease("lockName", 10, TimeUnit.SECONDS, "lockKey");
           if (!lock.tryLock(10,TimeUnit.SECONDS)){
               //do something
               return;
           }
           lock.unlock();
       }
   ```

   

