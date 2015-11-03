package com.inqbarna.rxpagingsupport.sample;

import com.inqbarna.rxpagingsupport.RxPagedAdapter;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
public interface DataConnection<T> {
    void connectWith(RxPagedAdapter<T, ?> adapter);
}
