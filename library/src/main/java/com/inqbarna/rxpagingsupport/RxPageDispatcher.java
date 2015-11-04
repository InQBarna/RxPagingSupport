package com.inqbarna.rxpagingsupport;

import rx.Observable;
import rx.functions.Func1;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 4/11/15
 */
public interface RxPageDispatcher<T> extends Func1<PageRequest, Observable<? extends Page<T>>> {
}
