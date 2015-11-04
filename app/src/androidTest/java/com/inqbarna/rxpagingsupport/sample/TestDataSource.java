package com.inqbarna.rxpagingsupport.sample;

import com.inqbarna.rxpagingsupport.Page;
import com.inqbarna.rxpagingsupport.PageRequest;
import com.inqbarna.rxpagingsupport.RxStdDispatcher;
import com.inqbarna.rxpagingsupport.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;

/**
 * Created by david on 3/11/15.
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

    private RxStdDispatcher.RxPageCacheManager<DataItem> cacheManager = new RxStdDispatcher.RxPageCacheManager<DataItem>() {
        @Override
        public void storePage(Page<DataItem> page) {
            // nothing yet
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
        return new Page<DataItem>(request.getPage(), request.getOffset(), source, items);
    }

    private Observable<? extends Page<DataItem>> processDiskRequest(PageRequest pageRequest) {
        return Observable.just(generatePage(pageRequest, Source.Cache)).doOnNext(
                new Action1<Page<DataItem>>() {
                    @Override
                    public void call(Page<DataItem> dataItemPage) {
                        accountDiskPage(dataItemPage);
                    }
                }
        );
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

    private void accountDiskPage(Page<DataItem> dataItemPage) {
        diskPages++;
        if (null != listener) {
            listener.onNewDiskPage(dataItemPage, diskPages);
        }
    }
}
