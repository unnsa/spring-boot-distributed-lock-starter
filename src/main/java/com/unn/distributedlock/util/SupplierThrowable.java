package com.unn.distributedlock.util;

@FunctionalInterface
public interface SupplierThrowable<T> {
    T get() throws Throwable;
}
