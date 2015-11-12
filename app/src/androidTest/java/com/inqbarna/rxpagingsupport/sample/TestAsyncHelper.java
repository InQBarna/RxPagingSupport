/*                                                                              
 *    Copyright 2015 InQBarna Kenkyuu Jo SL                                     
 *                                                                              
 *    Licensed under the Apache License, Version 2.0 (the "License");           
 *    you may not use this file except in compliance with the License.          
 *    You may obtain a copy of the License at                                   
 *                                                                              
 *        http://www.apache.org/licenses/LICENSE-2.0                            
 *                                                                              
 *    Unless required by applicable law or agreed to in writing, software       
 *    distributed under the License is distributed on an "AS IS" BASIS,         
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  
 *    See the License for the specific language governing permissions and       
 *    limitations under the License.                                            
 *                                                                              
 */
package com.inqbarna.rxpagingsupport.sample;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author David García <david.garcia@inqbarna.com>
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
        return countDownLatch.await(tsSecs, TimeUnit.SECONDS);
    }

    public void countDown() {
        countDownLatch.countDown();
    }
}
