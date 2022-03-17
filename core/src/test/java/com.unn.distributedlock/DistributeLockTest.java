package com.unn.distributedlock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RedisLockTestApplication.class)
public class DistributeLockTest {
    @Autowired
    private BusinessService businessService;


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
    }
}
