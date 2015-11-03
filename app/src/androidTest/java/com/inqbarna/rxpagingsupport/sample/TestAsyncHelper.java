package com.inqbarna.rxpagingsupport.sample;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by david on 3/11/15.
 */
public class TestAsyncHelper {
    private ReentrantLock lock;
    private Condition condition;
    private Map<String, Object> data;
    private CountDownLatch countDownLatch;

    public TestAsyncHelper() {
        lock = new ReentrantLock();
        condition = lock.newCondition();
        data = new HashMap<>();
    }

    public <T> T getData(String key) throws InterruptedException {
        lock.lock();
        try {
            while (!data.containsKey(key)) {
                condition.await();
            }
            Object o = data.remove(key);
            return (T)o;
        } finally {
            lock.unlock();
        }
    }

    public void putData(String key, Object data) {
        lock.lock();
        try {
            this.data.put(key, data);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void configureCountDown(int val) {
        countDownLatch = new CountDownLatch(val);
    }

    public boolean awaitCountdown(long timeout) throws InterruptedException {
        if (null == countDownLatch) {
            throw new NullPointerException("Call configure countdown first");
        }
        return countDownLatch.await(timeout, TimeUnit.SECONDS);
    }

    public void countDown() {
        countDownLatch.countDown();
    }
}
