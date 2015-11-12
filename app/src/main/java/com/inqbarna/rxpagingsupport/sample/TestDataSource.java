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

import android.util.Log;
import android.util.SparseArray;

import com.inqbarna.rxpagingsupport.Page;
import com.inqbarna.rxpagingsupport.PageRequest;
import com.inqbarna.rxpagingsupport.RxStdDispatcher;
import com.inqbarna.rxpagingsupport.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;

/**
 * @author David García <david.garcia@inqbarna.com>
 */
public class TestDataSource {

    public interface DataSourceListener {

        void onNewNetworkPage(Page<DataItem> dataItemPage, int networkPages);

        void onNewDiskPage(Page<DataItem> dataItemPage, int diskPages);
    }

    private RxStdDispatcher.RxPageSource<DataItem> netSource = new RxStdDispatcher.RxPageSource<DataItem>() {
        @Override
        public Observable<? extends Page<DataItem>> processRequest(PageRequest pageRequest) {
            return processNetRequest(pageRequest);
        }
    };

    private SparseArray<List<DataItem>> cachedPages = new SparseArray<>();

    private RxStdDispatcher.RxPageCacheManager<DataItem> cacheManager = new RxStdDispatcher.RxPageCacheManager<DataItem>() {
        @Override
        public void storePage(Page<DataItem> page) {
            try {
                cachedPages.put(page.getOffset(), page.getItems());
            } catch (Throwable throwable) {
                Log.e("TEST", "Error caching page", throwable);
            }
        }

        @Override
        public Observable<? extends Page<DataItem>> processRequest(PageRequest pageRequest) {
            return processDiskRequest(pageRequest);
        }
    };

    public RxStdDispatcher.RxPageSource<DataItem> getNetSource() {
        return netSource;
    }

    public RxStdDispatcher.RxPageCacheManager<DataItem> getCacheManager() {
        return cacheManager;
    }

    private DataSourceListener listener;
    private int                networkPages;
    private int                diskPages;
    private Random random = new Random();

    public int getNetworkPages() {
        return networkPages;
    }

    public int getDiskPages() {
        return diskPages;
    }

    public void setDataSourceListener(DataSourceListener listener) {
        this.listener = listener;
    }

    private Page<DataItem> generatePage(PageRequest request, Source source) {
        List<DataItem> items = new ArrayList<>();
        for (int i = request.getOffset(); i <= request.getEnd(); i++) {
            items.add(new DataItem(request.getPage(), i));
        }
        return new Page<>(request.getPage(), request.getOffset(), source, items);
    }

    private Observable<? extends Page<DataItem>> processDiskRequest(PageRequest pageRequest) {
        List<DataItem> items = cachedPages.get(pageRequest.getOffset());
        if (null == items) {
            return Observable.error(new NoSuchElementException("There's no that item cached"));
        } else {
            return Observable.just(new Page<>(pageRequest.getPage(), pageRequest.getOffset(), Source.Cache, items))
                    .doOnNext(
                            new Action1<Page<DataItem>>() {
                                @Override
                                public void call(Page<DataItem> dataItemPage) {
                                    accountDiskPage(dataItemPage);
                                }
                            }
                    );
        }
    }

    private void accountNewNetworkPage(Page<DataItem> dataItemPage) {
        networkPages++;
        if (null != listener) {
            listener.onNewNetworkPage(dataItemPage, networkPages);
        }
    }

    private Observable<? extends Page<DataItem>> processNetRequest(PageRequest pageRequest) {
        long delayMs = 300 + random.nextInt(700);
        return Observable.just(generatePage(pageRequest, Source.Network))
                         .delaySubscription(delayMs, TimeUnit.MILLISECONDS)
                         .doOnNext(
                                 new Action1<Page<DataItem>>() {
                                     @Override
                                     public void call(Page<DataItem> dataItemPage) {
                                         accountNewNetworkPage(dataItemPage);
                                     }
                                 }
                         );
    }

    public int getCacheSize() {
        return cachedPages.size();
    }

    private void accountDiskPage(Page<DataItem> dataItemPage) {
        diskPages++;
        if (null != listener) {
            listener.onNewDiskPage(dataItemPage, diskPages);
        }
    }
}
