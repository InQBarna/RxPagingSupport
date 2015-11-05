package com.inqbarna.rxpagingsupport.sample;

import com.inqbarna.rxpagingsupport.RxStdDispatcher;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
public class TestDataModule extends DataModule {

    private RxStdDispatcher.RxPageSource<DataItem> pageSource;
    private RxStdDispatcher.RxPageCacheManager<DataItem> cacheManager;

    public TestDataModule(
            RxStdDispatcher.RxPageSource<DataItem> pageSource,
            RxStdDispatcher.RxPageCacheManager<DataItem> cacheManager) {
        this.pageSource = pageSource;
        this.cacheManager = cacheManager;
    }

    @Override
    public boolean autoConnect() {
        return false;
    }

    @Override
    public RxStdDispatcher.RxPageSource<DataItem> providePageSource() {
        return pageSource;
    }

    @Override
    public RxStdDispatcher.RxPageCacheManager<DataItem> provideCacheManager() {
        return cacheManager;
    }
}
