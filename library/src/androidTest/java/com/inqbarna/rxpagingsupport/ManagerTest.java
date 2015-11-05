package com.inqbarna.rxpagingsupport;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
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
    private             Settings.Logger logger    = new Settings.Logger() {
        @Override
        public void error(@NonNull String msg, @Nullable Throwable throwable) {
            Log.e("RxLibTest", msg, throwable);
        }

        @Override
        public void info(@NonNull String msg, @Nullable Throwable throwable) {
            Log.i("RxLibTest", msg, throwable);
        }

        @Override
        public void debug(@NonNull String msg, @Nullable Throwable throwable) {
            Log.d("RxLibTest", msg, throwable);
        }
    };
    private Settings        settings;
    private TestAsyncHelper netAsyncHelper;
    private TestAsyncHelper diskAsyncHelper;

    private static View DUMMY_VIEW = new View(InstrumentationRegistry.getTargetContext());

    private class TestDispatcher extends RxStdDispatcher<Item> {

        private Page.PageRecycler<Item> networkRecycler = new Page.PageRecycler<Item>() {
            @Override
            public void onRecycled(Page<Item> page) {
                netAsyncHelper.countDown();
            }
        };
        private Page.PageRecycler<Item> diskRecycler    = new Page.PageRecycler<Item>() {
            @Override
            public void onRecycled(Page<Item> page) {
                diskAsyncHelper.countDown();
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
        protected void doBindViewHolder(DummyHolder holder, int position) {

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
        manager.beginConnection(dispatcher);

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
        manager.beginConnection(dispatcher);
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
        verify(netSrc, times(numPageAsk)).processRequest(anyTargetPage()); // two pages requested...

        // first item should now be...
        assertThat(manager.getItem(0).absIds, is((numPageAsk * PAGE_SIZE)));
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

    private void addPage(OngoingStubbing<Observable<? extends Page<Item>>> when, Source source, int i) {
        when.thenReturn(generateSourcedItems(source, i));
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
        return Observable.just(new Page<>(page, off, source, items));
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
