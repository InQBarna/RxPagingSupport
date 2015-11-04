package com.inqbarna.rxpagingsupport.sample;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.inqbarna.rxpagingsupport.Page;
import com.inqbarna.rxpagingsupport.PageRequest;
import com.inqbarna.rxpagingsupport.RxStdDispatcher;
import com.inqbarna.rxpagingsupport.RxPageDispatcher;
import com.inqbarna.rxpagingsupport.RxPagedAdapter;
import com.inqbarna.rxpagingsupport.Settings;

import java.util.Set;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import rx.Observable;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
@Module
public class DataModule {

    @Provides
    @Singleton
    public DataConnection<DataItem> provideDataConnection(final RxPageDispatcher<DataItem> dataConnection) {
        return new DataConnection<DataItem>() {
            @Override
            public void connectWith(RxPagedAdapter<DataItem, ?> adapter) {
                adapter.beginConnection(dataConnection);
            }
        };
    }

    @Provides
    @Singleton
    public RxPageDispatcher<DataItem> provideRxDataConnection(Settings settings, RxStdDispatcher.RxPageSource<DataItem> netSource, RxStdDispatcher.RxPageCacheManager<DataItem> cacheManager) {
        return RxStdDispatcher.newInstance(settings, cacheManager, netSource);
    }

    @Provides
    @Singleton
    public RxStdDispatcher.RxPageSource<DataItem> providePageSource() {
        return new RxStdDispatcher.RxPageSource<DataItem>() {
            @Override
            public Observable<? extends Page<DataItem>> processRequest(PageRequest pageRequest) {
                return Observable.error(new UnsupportedOperationException("Not yet")); // TODO: 4/11/15 something mor interesting?
            }
        };
    }

    @Provides
    @Singleton
    public RxStdDispatcher.RxPageCacheManager<DataItem> provideCacheManager() {
        return new RxStdDispatcher.RxPageCacheManager<DataItem>() {
            @Override
            public void storePage(Page<DataItem> page) {
                // no-op
            }

            @Override
            public Observable<? extends Page<DataItem>> processRequest(PageRequest pageRequest) {
                return Observable.error(new UnsupportedOperationException("Not yet"));
            }
        };
    }

    @Provides
    @Singleton
    public Settings.Logger provideRxPageLogger() {
        return new Settings.Logger() {
            public static final String TAG = "RxPaging";

            @Override
            public void error(@NonNull String msg, @Nullable Throwable throwable) {
                Log.e(TAG, msg, throwable);
            }

            @Override
            public void info(@NonNull String msg, @Nullable Throwable throwable) {
                Log.i(TAG, msg, throwable);
            }

            @Override
            public void debug(@NonNull String msg, @Nullable Throwable throwable) {
                Log.d(TAG, msg, throwable);
            }
        };
    }

    @Provides
    @Singleton
    public Settings provideRxSettings(Settings.Logger logger) {
        return Settings.builder().setLogger(logger).build();
    }

}
