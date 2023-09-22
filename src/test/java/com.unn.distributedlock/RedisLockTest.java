package com.unn.distributedlock;

import com.unn.distributedlock.core.RedisLockRegistryFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RedisLockTestApplication.class)
public class RedisLockTest {
    @Autowired
    private BusinessService businessService;
    @Autowired
    private RedisLockRegistryFactory redisLockRegistryFactory;


    @Test
    public void test() throws InterruptedException {
        try {
            System.out.println(businessService.doSomething());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test3() throws InterruptedException {
        try {
            System.out.println(businessService.doSomething("unn", "QWERTYU"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test2() throws InterruptedException {
        Lock lock = redisLockRegistryFactory.getLockAutoKeepLease("lockName", 10, TimeUnit.SECONDS, "lockKey");
        if (!lock.tryLock(10, TimeUnit.SECONDS)) {
            //do something
            return;
        }
        lock.unlock();
    }
}
