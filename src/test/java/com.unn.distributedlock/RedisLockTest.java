package com.unn.distributedlock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RedisLockTestApplication.class)
public class RedisLockTest {
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
}
