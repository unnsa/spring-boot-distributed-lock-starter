package com.unn.distributedlock;

import com.unn.distributedlock.handler.AfterUnlockSuccessHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class RedisLockTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisLockTestApplication.class, args);
    }

    @Bean
    public AfterUnlockSuccessHandler defaultAfterUnlockSuccessHandler() {
        return (args, distributedLock) -> {
            //doSomething
        };
    }

}
