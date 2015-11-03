package com.inqbarna.rxpagingsupport.sample;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.inqbarna.rxpagingsupport.Page;
import com.inqbarna.rxpagingsupport.PageRequest;
import com.inqbarna.rxpagingsupport.RxDataConnection;
import com.inqbarna.rxpagingsupport.RxPagedAdapter;
import com.inqbarna.rxpagingsupport.Settings;

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
    public DataConnection<DataItem> provideDataConnection(final RxDataConnection<DataItem> dataConnection) {
        return new DataConnection<DataItem>() {
            @Override
            public void connectWith(RxPagedAdapter<DataItem, ?> adapter) {
                adapter.beginConnection(dataConnection);
            }
        };
    }

    @Provides
    @Singleton
    public RxDataConnection<DataItem> provideRxDataConnection() {
        return new RxDataConnection<DataItem>() {
            @Override
            protected Observable<? extends Page<DataItem>> processDiskRequest(PageRequest pageRequest) {
                return Observable.empty();
            }

            @Override
            protected Observable<? extends Page<DataItem>> processNetRequest(PageRequest pageRequest) {
                return Observable.empty();
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
