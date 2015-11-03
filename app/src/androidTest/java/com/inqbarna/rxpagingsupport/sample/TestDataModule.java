package com.inqbarna.rxpagingsupport.sample;

import com.inqbarna.rxpagingsupport.Page;
import com.inqbarna.rxpagingsupport.PageManager;
import com.inqbarna.rxpagingsupport.PageRequest;
import com.inqbarna.rxpagingsupport.RxPagedAdapter;
import com.inqbarna.rxpagingsupport.Source;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import rx.Observable;
import rx.functions.Func1;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
public class TestDataModule extends DataModule {

    @Override
    public DataConnection<DataItem> provideDataConnection() {
        return new TestData();
    }

    static class TestData implements DataConnection<DataItem> {
        private Func1<? super PageRequest, ? extends Observable<? extends Page<DataItem>>> requestProcessor = new Func1<PageRequest, Observable<? extends Page<DataItem>>>() {
            @Override
            public Observable<? extends Page<DataItem>> call(PageRequest pageRequest) {
                List<DataItem> items = new ArrayList<>();
                final PageRequest.Type type = pageRequest.getType();
                final int page = pageRequest.getPage();
                for (int i = pageRequest.getOffset(); i < pageRequest.getEnd(); i++) {
                    items.add(new DataItem(page, i));
                }
                switch (type) {
                    case Network:
                        return Observable.just(new Page<DataItem>(page, pageRequest.getOffset(), Source.Network, items));
                    case Disk:
                        return Observable.just(new Page<DataItem>(page, pageRequest.getOffset(), Source.Cache, items));
                    case Prefetch:
                    default:
                        throw new UnsupportedOperationException("has any sense the prefetch?");
                }
            }
        };

        @Override
        public void connectWith(RxPagedAdapter<DataItem, ?> adapter) {
            final PageManager<DataItem>.ConnectionManager connectionManager = adapter.beginConnection();
            final Observable<PageRequest> pageRequests = connectionManager.getPageRequests();

            connectionManager.establishConnection(pageRequests.flatMap(requestProcessor));
        }
    }
}
