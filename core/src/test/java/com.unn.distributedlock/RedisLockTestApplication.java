package com.unn.distributedlock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class RedisLockTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisLockTestApplication.class, args);
    }


}
