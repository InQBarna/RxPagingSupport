package com.inqbarna.rxpagingsupport.sample;

import com.inqbarna.rxpagingsupport.RxPagedAdapter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
@Module
public class DataModule {

    @Provides
    @Singleton
    public DataConnection<DataItem> provideDataConnection() {
        return new DataConnection<DataItem>() {
            @Override
            public void connectWith(RxPagedAdapter<DataItem, ?> adapter) {
                // TODO: 3/11/15 put something useful if want to demo beside testing....
            }
        };
    }

}
