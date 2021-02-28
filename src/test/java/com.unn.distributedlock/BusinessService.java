package com.unn.distributedlock;


import com.unn.distributedlock.annotation.DistributedLock;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class BusinessService {

    @DistributedLock(name = "doit", key = "f*k", waitTime = 1, expiredTime = 10, expiredTimeUnit = TimeUnit.SECONDS)
    public String doSomething() throws InterruptedException {
        int i = 1;
        do {
            Thread.sleep(1000);
            System.out.println("第" + i + "秒");
        } while (++i < 10);
        return "finish";
    }

    @DistributedLock(name = "doit", key = "f*k", expiredTime = 30, expiredTimeUnit = TimeUnit.SECONDS)
    public String doSomethingReentrant(int i) throws InterruptedException {
        System.out.println("第" + i + "次");
        if (i < 0) {
            return "";
        }
        Thread.sleep(1000);
        doSomethingReentrant(--i);
        return "finish";
    }

}
