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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.inqbarna.rxpagingsupport.PageManager;
import com.inqbarna.rxpagingsupport.RxStdDispatcher;
import com.inqbarna.rxpagingsupport.RxPageDispatcher;
import com.inqbarna.rxpagingsupport.RxPagedAdapter;
import com.inqbarna.rxpagingsupport.Settings;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
@Module
public class DataModule {

    private TestDataSource tds = new TestDataSource();

    @Provides
    @Singleton
    public DataConnection<DataItem> provideDataConnection(final RxPageDispatcher<DataItem> dataConnection) {
        return new DataConnection<DataItem>() {
            @Override
            public void connectWith(RxPagedAdapter<DataItem, ?> adapter) {
                adapter.beginConnection(dataConnection, null);
            }
        };
    }

    @Provides
    @Singleton
    public boolean autoConnect() {
        return true;
    }

    @Provides
    @Singleton
    public RxPageDispatcher<DataItem> provideRxDataConnection(Settings settings, RxStdDispatcher.RxPageSource<DataItem> netSource, RxStdDispatcher.RxPageCacheManager<DataItem> cacheManager) {
        return RxStdDispatcher.newInstance(settings, cacheManager, netSource);
    }

    @Provides
    @Singleton
    public RxStdDispatcher.RxPageSource<DataItem> providePageSource() {
        return tds.getNetSource();
    }

    @Provides
    @Singleton
    public RxStdDispatcher.RxPageCacheManager<DataItem> provideCacheManager() {
        return tds.getCacheManager();
    }

    @Provides
    public PageManager<DataItem> providePageManager(Settings settings) {
        return new PageManager<>(settings);
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
