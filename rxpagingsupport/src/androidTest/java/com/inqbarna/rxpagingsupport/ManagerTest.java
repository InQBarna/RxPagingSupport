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

import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.DisableOnAndroidDebug;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 5/11/15
 */
@RunWith(AndroidJUnit4.class)
public class ManagerTest {

    public static final int             PAGE_SPAN = 5;
    public static final int             PAGE_SIZE = 10;
    public static final String          TAG       = "RxLibTest";
    private             Settings.Logger logger    = new Settings.Logger() {
        @Override
        public void error(@NonNull String msg, @Nullable Throwable throwable) {
            Log.e(TAG, decorateMessage(msg), throwable);
        }

        @Override
        public void info(@NonNull String msg, @Nullable Throwable throwable) {
            Log.i(TAG, decorateMessage(msg), throwable);
        }

        @Override
        public void debug(@NonNull String msg, @Nullable Throwable throwable) {
            Log.d(TAG, decorateMessage(msg), throwable);
        }
    };

    @NonNull
    private static String decorateMessage(@NonNull String msg) {
        StringBuilder builder = new StringBuilder();
        final Thread thread = Thread.currentThread();
        builder.append(thread.getName()).append("(").append(thread.getId()).append(") => ").append(msg);
        return builder.toString();
    }

    @Rule
    public TestRule rule = new DisableOnAndroidDebug(new Timeout(20, TimeUnit.SECONDS));

    private Settings        settings;
    private TestAsyncHelper netAsyncHelper;
    private TestAsyncHelper diskAsyncHelper;

    private static View DUMMY_VIEW = new View(InstrumentationRegistry.getTargetContext());

    private class TestDispatcher extends RxStdDispatcher<Item> {

        private Page.PageRecycler<Item> networkRecycler = new Page.PageRecycler<Item>() {
            @Override
            public void onRecycled(Page<Item> page) {
                Log.d(TAG, decorateMessage("net page recycled: " + page.getPage() + " from: " + page.getSource() + " will countDowns"));
                netAsyncHelper.countDown();
                Log.d(TAG, decorateMessage("net after countdown"));
            }
        };
        private Page.PageRecycler<Item> diskRecycler    = new Page.PageRecycler<Item>() {
            @Override
            public void onRecycled(Page<Item> page) {
                Log.d(TAG, decorateMessage("disk page recycled: " + page.getPage() + " from: " + page.getSource() + " will countDowns"));
                diskAsyncHelper.countDown();
                Log.d(TAG, decorateMessage("disk after countdown"));
            }
        };

        public TestDispatcher(
                @NonNull Settings settings, @NonNull RxPageSource<Item> networkSource,
                @Nullable RxPageSource<Item> diskSource) {
            super(settings, networkSource, diskSource);
        }

        public TestDispatcher(
                @NonNull Settings settings, @NonNull RxPageCacheManager<Item> cacheManager,
                @Nullable RxPageSource<Item> networkSource) {
            super(settings, cacheManager, networkSource);
        }

        @Override
        protected Observable<? extends Page<Item>> processNetRequest(PageRequest pageRequest) {
            return super.processNetRequest(pageRequest)
                        .map(
                                new Func1<Page<Item>, Page<Item>>() {
                                    @Override
                                    public Page<Item> call(Page<Item> itemPage) {
                                        itemPage.setPageRecycler(networkRecycler);
                                        return itemPage;
                                    }
                                }
                        );
        }

        @Override
        protected Observable<? extends Page<Item>> processDiskRequest(PageRequest pageRequest, boolean failIfNoSource) {
            return super.processDiskRequest(pageRequest, failIfNoSource)
                        .map(
                                new Func1<Page<Item>, Page<Item>>() {
                                    @Override
                                    public Page<Item> call(Page<Item> itemPage) {
                                        itemPage.setPageRecycler(diskRecycler);
                                        return itemPage;
                                    }
                                }
                        );
        }
    }

    static class DummyHolder extends RecyclerView.ViewHolder implements RxPagedAdapter.LoadHolder {
        public DummyHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void setLoadingState(boolean loading) {

        }
    }

    private static class TestAdapter extends RxPagedAdapter<Item, DummyHolder> {
        public TestAdapter(Settings settings) {
            super(settings, null);
        }

        @Override
        protected DummyHolder createLoadingViewHolder(ViewGroup parent) {
            return null;
        }

        @Override
        protected void doBindViewHolder(DummyHolder holder, Item item, int position) {

        }

        @Override
        protected DummyHolder doCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }
    }

    @Before
    public void doSetup() {
        netAsyncHelper = new TestAsyncHelper();
        diskAsyncHelper = new TestAsyncHelper();
        settings = Settings.builder()
                           .setPageSize(PAGE_SIZE)
                           .setPageSpan(PAGE_SPAN)
                           .setLogger(logger)
                           .build();
    }

    @Test
    public void basicInitialization() throws InterruptedException {

        PageManager<Item> manager = new PageManager<>(new TestAdapter(settings), settings, null);

        RxStdDispatcher.RxPageSource<Item> netSrc = Mockito.mock(RxStdDispatcher.RxPageSource.class);


        doAnswer(getSourcedPageAnswer(Source.Network)).when(netSrc).processRequest(anyTargetPage());
        //        doReturn(generateNetPage(0)).when(netSrc.processRequest(targetRequestPage(0)));

        RxStdDispatcher<Item> dispatcher = new TestDispatcher(settings, netSrc, null);
        netAsyncHelper.configureCountDown(PAGE_SPAN);
        manager.beginConnection(dispatcher, null);

        boolean countedDown = netAsyncHelper.awaitCountdown(PAGE_SPAN);
        assertThat(countedDown, is(true));
        assertThat(manager.getTotalCount(), is(PAGE_SPAN * PAGE_SIZE));
        verify(netSrc, times(PAGE_SPAN)).processRequest(anyTargetPage());
    }

    @Test
    public void scrollBottom() throws InterruptedException {
        final TestAdapter adapter = new TestAdapter(settings);
        PageManager<Item> manager = new PageManager<>(adapter, settings, null);
        RxStdDispatcher.RxPageSource<Item> netSrc = Mockito.mock(RxStdDispatcher.RxPageSource.class);
        doAnswer(getSourcedPageAnswer(Source.Network)).when(netSrc).processRequest(anyTargetPage());

        RxStdDispatcher<Item> dispatcher = new TestDispatcher(settings, netSrc, null);
        netAsyncHelper.configureCountDown(PAGE_SPAN);
        manager.beginConnection(dispatcher, null);
        boolean countedDown = netAsyncHelper.awaitCountdown(PAGE_SPAN);
        assertThat(countedDown, is(true));
        assertThat(manager.getTotalCount(), is(PAGE_SPAN * PAGE_SIZE));

//        reset(netSrc); // ok, we now have same than basicInitialization... do scroll

        RecyclerView recyclerViewMock = mock(RecyclerView.class);
        manager.enableMovementDetection(recyclerViewMock);

        ArgumentCaptor<RecyclerView.OnScrollListener> listenerCaptor = ArgumentCaptor.forClass(RecyclerView.OnScrollListener.class);
        verify(recyclerViewMock).addOnScrollListener(listenerCaptor.capture());

        final RecyclerView.OnScrollListener listenerCaptorValue = listenerCaptor.getValue();
        assertThat(listenerCaptorValue, notNullValue());

        when(recyclerViewMock.getChildCount()).thenReturn(PAGE_SIZE);
        when(recyclerViewMock.getChildAt(anyInt())).thenReturn(DUMMY_VIEW);
        when(recyclerViewMock.getChildAdapterPosition(dummyView())).thenReturn(PAGE_SPAN * PAGE_SIZE); // return the last possible value, should request two new pages

        int numPageAsk = (PAGE_SPAN - 1) / 2;
        netAsyncHelper.configureCountDown(numPageAsk);
        // start the scroll simulation...
        listenerCaptorValue.onScrollStateChanged(recyclerViewMock, RecyclerView.SCROLL_STATE_DRAGGING);
        listenerCaptorValue.onScrolled(recyclerViewMock, 0, 10);
        listenerCaptorValue.onScrollStateChanged(recyclerViewMock, RecyclerView.SCROLL_STATE_IDLE);

        countedDown = netAsyncHelper.awaitCountdown(5);
        InOrder inOrder = inOrder(recyclerViewMock);
        inOrder.verify(recyclerViewMock).getChildCount();
        inOrder.verify(recyclerViewMock).getChildAt(PAGE_SIZE - 1);
        inOrder.verify(recyclerViewMock).getChildAdapterPosition(dummyView());


        assertThat(countedDown, is(true)); // we received numPageAsk notifications
        assertThat(manager.getTotalCount(), is(PAGE_SPAN * PAGE_SIZE)); // size is still the full size
        verify(netSrc, times(PAGE_SPAN + numPageAsk)).processRequest(anyTargetPage()); // two extra pages requested...

        // first item should now be...
        assertThat(manager.getItem(0).absIds, is((numPageAsk * PAGE_SIZE)));
    }

    @Test
    public void shouldNotRecyclePages() throws InterruptedException {
        final TestAdapter adapter = new TestAdapter(settings);
        PageManager<Item> manager = new PageManager<>(adapter, settings, null);
        RxStdDispatcher.RxPageSource<Item> netSrc = Mockito.mock(RxStdDispatcher.RxPageSource.class);
        doAnswer(getSourcedPageAnswer(Source.Network)).when(netSrc).processRequest(anyTargetPage());
        doAnswer(getSourcedEmptyPageAnswer(Source.Network)).when(netSrc).processRequest(pageNumber(PAGE_SPAN));
        doAnswer(getSourcedEmptyPageAnswer(Source.Network)).when(netSrc).processRequest(pageNumber(PAGE_SPAN + 1));

        RxStdDispatcher.RxPageSource<Item> diskSrc = Mockito.mock(RxStdDispatcher.RxPageSource.class);
        doAnswer(getSourcedPageAnswer(Source.Cache)).when(diskSrc).processRequest(anyTargetPage());


        RxStdDispatcher<Item> dispatcher = new TestDispatcher(settings, netSrc, diskSrc);
        netAsyncHelper.configureCountDown(PAGE_SPAN);
        manager.beginConnection(dispatcher, null);
        boolean countedDown = netAsyncHelper.awaitCountdown(PAGE_SPAN);
        assertThat(countedDown, is(true));
        assertThat(manager.getTotalCount(), is(PAGE_SPAN * PAGE_SIZE));
        assertThat(manager.isLastPageSeen(), is(false));

        //        reset(netSrc); // ok, we now have same than basicInitialization... do scroll

        RecyclerView recyclerViewMock = mock(RecyclerView.class);
        manager.enableMovementDetection(recyclerViewMock);

        ArgumentCaptor<RecyclerView.OnScrollListener> listenerCaptor = ArgumentCaptor.forClass(RecyclerView.OnScrollListener.class);
        verify(recyclerViewMock).addOnScrollListener(listenerCaptor.capture());

        final RecyclerView.OnScrollListener listenerCaptorValue = listenerCaptor.getValue();
        assertThat(listenerCaptorValue, notNullValue());

        when(recyclerViewMock.getChildCount()).thenReturn(PAGE_SIZE);
        when(recyclerViewMock.getChildAt(anyInt())).thenReturn(DUMMY_VIEW);
        when(recyclerViewMock.getChildAdapterPosition(dummyView())).thenReturn(PAGE_SPAN * PAGE_SIZE); // return the last possible value, should request two new pages

        int numPageAsk = (PAGE_SPAN - 1) / 2;
        netAsyncHelper.configureCountDown(1);
        // start the scroll simulation...
        listenerCaptorValue.onScrollStateChanged(recyclerViewMock, RecyclerView.SCROLL_STATE_DRAGGING);
        listenerCaptorValue.onScrolled(recyclerViewMock, 0, 10);
        listenerCaptorValue.onScrollStateChanged(recyclerViewMock, RecyclerView.SCROLL_STATE_IDLE);

        verify(netSrc, timeout(3000).times(PAGE_SPAN + numPageAsk)).processRequest(anyTargetPage()); // two extra pages requested...
        Thread.sleep(3000);
        assertThat(manager.isLastPageSeen(), is(true));
        assertThat(manager.getTotalCount(), is(PAGE_SPAN * PAGE_SIZE)); // size is still the full size
        assertThat(manager.getItem(0).absIds, is(0)); // should have not discarded any item
        assertThat(manager.getNumPages(), is(PAGE_SPAN));


        when(recyclerViewMock.getChildAdapterPosition(dummyView())).thenReturn(0); // return the smallest element

        // go again scroll up....
        listenerCaptorValue.onScrollStateChanged(recyclerViewMock, RecyclerView.SCROLL_STATE_DRAGGING);
        listenerCaptorValue.onScrolled(recyclerViewMock, 0, -10);
        listenerCaptorValue.onScrollStateChanged(recyclerViewMock, RecyclerView.SCROLL_STATE_IDLE);

        // ok, disk source should have never been called....
        Thread.sleep(3000);
        verify(diskSrc, never()).processRequest(anyTargetPage());

    }

    @Test
    public void testLastPage() throws InterruptedException {
        final TestAdapter adapter = new TestAdapter(settings);
        PageManager<Item> manager = new PageManager<>(adapter, settings, null);
        RxStdDispatcher.RxPageSource<Item> netSrc = Mockito.mock(RxStdDispatcher.RxPageSource.class);
        doAnswer(getSourcedEmptyPageAnswer(Source.Network)).when(netSrc).processRequest(anyTargetPage());
        manager.beginConnection(new TestDispatcher(settings, netSrc, null), null);
        verify(netSrc, after(5000).times(PAGE_SPAN)).processRequest(anyTargetPage());

//        final SortedSet<SimpleDebugNotificationListener.NotificationsByObservable<?>> byObservable = simpleListener.getNotificationsByObservable();
//
//        TreeSet<SimpleDebugNotificationListener.NotificationsByObservable<?>> tmpSet = new TreeSet<>();
//        for (SimpleDebugNotificationListener.NotificationsByObservable<?> no : byObservable) {
//            tmpSet.add(no);
//            Log.e(TAG, simpleListener.toString(new TreeSet<>(tmpSet)));
//            tmpSet.clear();
//        }
        assertThat(manager.isLastPageSeen(), is(true));
    }

    @Test
    public void testFinallyCalledOnError() {
        final TestAdapter adapter = new TestAdapter(settings);
        PageManager<Item> manager = new PageManager<>(adapter, settings, null);
        RxStdDispatcher.RxPageSource<Item> netSrc = Mockito.mock(RxStdDispatcher.RxPageSource.class);
        doThrow(new NullPointerException("Testing")).when(netSrc).processRequest(anyTargetPage());

        Action0 action = Mockito.mock(Action0.class);
        manager.beginConnection(new TestDispatcher(settings, netSrc, null), action);
        verify(action, after(3000).times(1)).call();
    }

    @Test
    public void testFinallyCalledOnRecycle() {
        final TestAdapter adapter = new TestAdapter(settings);
        PageManager<Item> manager = new PageManager<>(adapter, settings, null);
        RxStdDispatcher.RxPageSource<Item> netSrc = Mockito.mock(RxStdDispatcher.RxPageSource.class);
        doAnswer(getSourcedPageAnswer(Source.Network)).when(netSrc).processRequest(anyTargetPage());

        Action0 action = Mockito.mock(Action0.class);
        manager.beginConnection(new TestDispatcher(settings, netSrc, null), action);
        manager.recycle();
        verify(action, after(3000).times(1)).call();
        verify(netSrc, never()).processRequest(anyTargetPage());
    }

    @Test
    public void testFinallyCalledOnComplete() {
        final TestAdapter adapter = new TestAdapter(settings);
        PageManager<Item> manager = new PageManager<>(adapter, settings, null);
        RxStdDispatcher.RxPageSource<Item> netSrc = Mockito.mock(RxStdDispatcher.RxPageSource.class);
        doAnswer(getSourcedPageAnswer(Source.Network)).when(netSrc).processRequest(anyTargetPage());
        doAnswer(getSourcedEmptyPageAnswer(Source.Network)).when(netSrc).processRequest(pageNumber(3));

        Action0 action = Mockito.mock(Action0.class);
        manager.beginConnection(new TestDispatcher(settings, netSrc, null), action);
        verify(action, after(3000).times(1)).call();
    }


    @Mock
    RxStdDispatcher.RxPageSource<Item> networkSource;

    @Mock
    RxStdDispatcher.RxPageSource<Item> diskSource;

    @Mock
    PageManager.PageLoadErrorListener errorListener;

    @Captor
    ArgumentCaptor<Throwable> throwableCaptor;

    @Test
    public void restoreRequestAgainOverDisk() {

        MockitoAnnotations.initMocks(this);
        TestAdapter adapter = new TestAdapter(settings);
        doAnswer(getSourcedPageAnswer(Source.Network)).when(networkSource).processRequest(anyTargetPage());
        doAnswer(getSourcedEmptyPageAnswer(Source.Network)).when(networkSource).processRequest(pageNumber(3));
        doAnswer(getSourcedEmptyPageAnswer(Source.Network)).when(networkSource).processRequest(pageNumber(4));
        doAnswer(getSourcedPageAnswer(Source.Cache)).when(diskSource).processRequest(anyTargetPage());

        PageManager<Item> manager = new PageManager<>(adapter, settings, null);
        RxStdDispatcher<Item> dispatcher = RxStdDispatcher.newInstance(settings, networkSource, diskSource);

        manager.beginConnection(dispatcher);

        verify(networkSource, after(3000).atLeast(4)).processRequest(anyTargetPage());
        verify(diskSource, never()).processRequest(anyTargetPage());
        Bundle savedInstance = new Bundle();
        manager.onSaveInstanceState(savedInstance);
        manager.recycle();
        Parcel parcel = Parcel.obtain();
        parcel.writeBundle(savedInstance);
        parcel.setDataPosition(0);
        Bundle recoveredState = parcel.readBundle();
        parcel.recycle();
        assertThat(savedInstance, not(sameInstance(recoveredState)));

        recoveredState.setClassLoader(manager.getClass().getClassLoader());
        manager = new PageManager<>(adapter, settings, recoveredState);
        manager.beginConnection(dispatcher);
        verify(diskSource, after(3000).times(3)).processRequest(anyTargetPage());
        assertThat(manager.isLastPageSeen(), is(true));
        assertThat(manager.getTotalCount(), is(30));
    }

    @Test
    public void errorForwardsFromObservable() {
        MockitoAnnotations.initMocks(this);
        TestAdapter adapter = new TestAdapter(settings);
        doAnswer(getSourcedPageAnswer(Source.Network)).when(networkSource).processRequest(anyTargetPage());
        doThrow(new NullPointerException("Hey there, NPE")).when(networkSource).processRequest(pageNumber(4));
        PageManager<Item> manager = new PageManager<>(adapter, settings, null);

        manager.setErrorListener(errorListener);
        RxStdDispatcher<Item> dispatcher = RxStdDispatcher.newInstance(settings, networkSource, null);
        manager.beginConnection(dispatcher);
        verify(errorListener, after(5000).times(1)).onError(throwableCaptor.capture());

        assertThat(throwableCaptor.getValue(), instanceOf(NullPointerException.class));
        assertThat(throwableCaptor.getValue().getMessage(), is("Hey there, NPE"));
    }

    @Test
    public void errorForwardsFromPage() {
        MockitoAnnotations.initMocks(this);
        TestAdapter adapter = new TestAdapter(settings);
        doAnswer(getSourcedPageAnswer(Source.Network)).when(networkSource).processRequest(anyTargetPage());
        doAnswer(errorPageAnswer(Source.Network, new NullPointerException("Hey there, NPE"))).when(networkSource).processRequest(pageNumber(4));
        PageManager<Item> manager = new PageManager<>(adapter, settings, null);

        manager.setErrorListener(errorListener);
        RxStdDispatcher<Item> dispatcher = RxStdDispatcher.newInstance(settings, networkSource, null);
        manager.beginConnection(dispatcher);
        verify(errorListener, after(5000).times(1)).onError(throwableCaptor.capture());

        assertThat(throwableCaptor.getValue(), instanceOf(NullPointerException.class));
        assertThat(throwableCaptor.getValue().getMessage(), is("Hey there, NPE"));
    }


    private static View dummyView() {
        return argThat(allOf(instanceOf(View.class), sameInstance(DUMMY_VIEW)));
    }

    private Answer getSourcedPageAnswer(final Source source) {
        return new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                final Object[] arguments = invocationOnMock.getArguments();
                PageRequest pr = (PageRequest) arguments[0];
                return generateSourcedItems(source, pr.getPage());
            }
        };
    }

    private Answer getSourcedEmptyPageAnswer(final Source source) {
        return new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                settings.getLogger().debug("Should deliver empty page on source: " + source, null);
                final Observable<?> objectObservable = testEmpty();
                settings.getLogger().debug("Generated Observable: " + objectObservable.getClass().getName() + "@" + Integer.toHexString(objectObservable.hashCode()), null);
                return objectObservable;
            }
        };
    }


    private Answer errorPageAnswer(final Source source, final Throwable error) {
        return new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PageRequest request = (PageRequest) invocation.getArguments()[0];
                Page page = new Page(request.getPage(), source, error);
                return Observable.just(page);
            }
        };
    }

    private <T> Observable<? extends Page<T>> testEmpty() {
        return Observable.just(Page.<T>empty());
//        return Observable.create(
//                new Observable.OnSubscribe<T>() {
//                    @Override
//                    public void call(Subscriber<? super T> subscriber) {
//                        settings.getLogger().debug("Empty subscriber just subscribed: " + subscriber.getClass().getName() + "@" + Integer.toHexString(subscriber.hashCode()), null);
//                        subscriber.onCompleted();
//                    }
//                }
//        );
    }

    private static PageRequest anyTargetPage() {
        return argThat(
                new CustomTypeSafeMatcher<PageRequest>("matches any page") {
                    @Override
                    protected boolean matchesSafely(PageRequest item) {
                        return true;
                    }
                }
        );
    }

    private static PageRequest pageNumber(final int pageNo) {
        return argThat(
                new CustomTypeSafeMatcher<PageRequest>("match page number " + pageNo) {
                    @Override
                    protected boolean matchesSafely(PageRequest item) {
                        return item.getPage() == pageNo;
                    }
                }
        );
    }

    private Observable<Page<Item>> generateNetPage(int page) {
        return generateSourcedItems(Source.Network, page);
    }

    private Observable<Page<Item>> generateSourcedItems(Source source, int page) {
        List<Item> items = new ArrayList<>();
        final int pageSize = settings.getPageSize();
        int off = page * pageSize;
        for (int i = 0; i < pageSize; i++) {
            items.add(new Item(page, off + i));
        }
        settings.getLogger().debug("Generating data for page: " + page, null);
        final Observable<Page<Item>> result = Observable.just(new Page<>(page, off, source, items));
        settings.getLogger().debug("Result Obs: " + result.getClass().getName() + "@" + Integer.toHexString(result.hashCode()), null);
        return result;
    }

    private static PageRequest targetRequestPage(final int page) {
        return argThat(
                new CustomTypeSafeMatcher<PageRequest>("page request matches page " + page) {
                    @Override
                    protected boolean matchesSafely(PageRequest item) {
                        return item.getPage() == page;
                    }
                }
        );
    }
}
