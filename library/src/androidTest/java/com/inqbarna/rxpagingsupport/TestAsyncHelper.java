package com.inqbarna.rxpagingsupport;

import android.os.Debug;
import android.support.test.InstrumentationRegistry;

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

    public boolean awaitCountdown(long tsSecs) throws InterruptedException {
        if (null == countDownLatch) {
            throw new NullPointerException("Call configure countdown first");
        }
        if (Debug.isDebuggerConnected()) {
            countDownLatch.await();
//            Thread.sleep(2000); // lets add 2 seconds sleep to give time to swap threads
            return true;
        } else {
            final boolean result = countDownLatch.await(tsSecs, TimeUnit.SECONDS);
//            Thread.sleep(2000); // lets add 2 seconds sleep to give time to swap threads
            return result;
        }
    }

    public void countDown() {
        countDownLatch.countDown();
    }
}
