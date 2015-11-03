package com.inqbarna.rxpagingsupport;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by david on 3/11/15.
 */
public abstract class RxDataConnection<T> implements Func1<PageRequest, Observable<? extends Page<T>>> {
    @Override
    public final Observable<? extends Page<T>> call(PageRequest pageRequest) {
        final PageRequest.Type type = pageRequest.getType();
        switch (type) {
            case Network:
                return processNetRequest(pageRequest);
            case Disk:
                return processDiskRequest(pageRequest);
            case Prefetch:
            default:
                throw new UnsupportedOperationException("Not yet, what about prefetch?");
        }
    }

    protected abstract Observable<? extends Page<T>> processDiskRequest(PageRequest pageRequest);

    protected abstract Observable<? extends Page<T>> processNetRequest(PageRequest pageRequest);
}
