package com.unn.distributedlock;

import com.unn.distributedlock.core.RedisLockRegistryUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = RedisLockTestApplication.class)
public class RedisLockTest {
    @Autowired
    private BusinessService businessService;
    @Autowired
    private RedisLockRegistryUtil redisLockRegistryUtil;


    //@Test
    public void test() throws InterruptedException {
        try {
            System.out.println(businessService.doSomething());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void test2() throws InterruptedException {
        Lock lock = redisLockRegistryUtil.getLockAutoKeepLease("lockName", 10, TimeUnit.SECONDS, "lockKey");
        if (!lock.tryLock(10,TimeUnit.SECONDS)){
            //do something
            return;
        }
        lock.unlock();
    }
}
