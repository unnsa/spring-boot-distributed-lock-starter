package com.unn.distributedlock.util;

@FunctionalInterface
public interface RunnableThrowable {
    void run() throws Throwable;
}
