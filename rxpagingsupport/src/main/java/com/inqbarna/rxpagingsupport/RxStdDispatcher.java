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
package com.inqbarna.rxpagingsupport;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * @author David García <david.garcia@inqbarna.com>
 */
public class RxStdDispatcher<T> implements RxPageDispatcher<T> {

    // PRE: one or the other sources will be non-null
    protected final RxPageSource<T>     networkSource;
    protected final RxPageSource<T>     diskSource;
    protected final RxPageCacheStore<T> cacheStore;
    protected final Settings            settings;

    protected RxStdDispatcher(@NonNull Settings settings, @NonNull RxPageSource<T> networkSource, @Nullable RxPageSource<T> diskSource) {
        this.networkSource = networkSource;
        this.diskSource = diskSource;
        this.settings = settings;
        this.cacheStore = null;
    }

    protected RxStdDispatcher(@NonNull Settings settings, @NonNull RxPageCacheManager<T> cacheManager, @Nullable RxPageSource<T> networkSource) {
        this.settings = settings;
        this.diskSource = cacheManager;
        this.cacheStore = cacheManager;
        this.networkSource = networkSource;
    }

    public static <T> RxStdDispatcher<T> newInstance(
            @NonNull Settings settings, @NonNull RxPageCacheManager<T> cacheManager, @Nullable RxPageSource<T> networkSource) {
        return new RxStdDispatcher<>(settings, cacheManager, networkSource);
    }

    public static <T> RxStdDispatcher<T> newInstance(@NonNull Settings settings, @NonNull RxPageCacheManager<T> cacheManager) {
        return newInstance(settings, cacheManager, null);
    }

    public static <T> RxStdDispatcher<T> newInstance(
            @NonNull Settings settings, @NonNull RxPageSource<T> networkSource, @Nullable RxPageSource<T> diskSource) {
        return new RxStdDispatcher<>(settings, networkSource, diskSource);
    }

    public interface RxPageSource<T> {
        Observable<? extends Page<T>> processRequest(PageRequest pageRequest);
    }

    public interface RxPageCacheStore<T> {
        void storePage(Page<T> page);
    }

    public interface RxPageCacheManager<T> extends RxPageSource<T>, RxPageCacheStore<T> {

    }

    @Override
    public void setEvents(Observable<PageManager.ManagerEvent> events) {
        // default do nothing
    }

    @Override
    public final Observable<? extends Page<T>> call(PageRequest pageRequest) {
        final PageRequest.Type type = pageRequest.getType();
        switch (type) {
            case Network:
                return processNetRequest(pageRequest);
            case Disk:
                return processDiskRequest(pageRequest, true);
            default:
                throw new UnsupportedOperationException("WTF?");
        }
    }

    protected Observable<? extends Page<T>> processDiskRequest(PageRequest pageRequest, boolean failIfNoSource) {
        if (null == diskSource) {
            if (failIfNoSource) {
                return Observable.error(new RxPagingException("Error in disk request (" + pageRequest + ") and no source defined", pageRequest));
            } else {
                return Observable.empty();
            }
        } else {
            return diskSource.processRequest(pageRequest);
        }
    }

    protected Observable<? extends Page<T>> processNetRequest(final PageRequest pageRequest) {
        final boolean diskFallback = settings.hasCacheFallbackEnabled();
        if (null == networkSource) {
            if (diskFallback) {
                return processDiskRequest(pageRequest, true);
            } else {
                return Observable.error(new RxPagingException("No network source available, and cache fallback is not enabled", pageRequest));
            }
        } else {
            Observable<? extends Page<T>> observable = networkSource.processRequest(pageRequest);

            if (null != cacheStore) {
                observable = observable.doOnNext(
                        new Action1<Page<T>>() {
                            @Override
                            public void call(Page<T> tPage) {
                                cacheStore.storePage(tPage);
                            }
                        }
                );
            }

            if (diskFallback) {
                observable = prepareDiskFallback((Observable<Page<T>>) observable, pageRequest);
            }
            return observable;
        }
    }

    private Observable<? extends Page<T>> prepareDiskFallback(Observable<Page<T>> observable, final PageRequest pageRequest) {
        final Func1<Throwable, Observable<? extends Page<T>>> func1 = new Func1<Throwable, Observable<? extends Page<T>>>() {
            @Override
            public Observable<? extends Page<T>> call(Throwable throwable) {
                settings.getLogger().info("Failed request on network " + pageRequest + ", doing fallback to disk cache", null);
                return processDiskRequest(pageRequest, false);
            }
        };
        return observable.onErrorResumeNext(func1);
    }
}
