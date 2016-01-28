package com.inqbarna.rxpagingsupport;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.FakeSparseArray;
import android.util.SparseArray;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 28/1/16
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AndroidSchedulers.class, PageManager.class})
public class PageManagerTest {


    private Settings settings;


    @Mock
    RxStdDispatcher.RxPageSource<Item> networkSource;

    @Mock
    RxStdDispatcher.RxPageSource<Item> diskSource;

    @Mock
    SparseArray<PageRequest> sparseArray;

    private FakeSparseArray<PageRequest> fakeArray;

    private RxStdDispatcher<Item> itemDispatcher;

    private static Executor mainService;
    private Settings.Logger logger = new Settings.Logger() {
        @Override
        public void error(@NonNull String msg, @Nullable Throwable throwable) {
            System.out.println(decorateMessage(msg));
            if (null != throwable) {
                throwable.printStackTrace(System.err);
            }
        }

        @Override
        public void info(@NonNull String msg, @Nullable Throwable throwable) {
            System.out.println(decorateMessage(msg));
        }

        @Override
        public void debug(@NonNull String msg, @Nullable Throwable throwable) {
            System.out.println(decorateMessage(msg));
        }
    };

    @NonNull
    private static String decorateMessage(@NonNull String msg) {
        StringBuilder builder = new StringBuilder();
        final Thread thread = Thread.currentThread();
        builder.append(thread.getName()).append("(").append(thread.getId()).append(") => ").append(msg);
        return builder.toString();
    }

    @BeforeClass
    public static void initializeOnce() {
        mainService = Executors.newScheduledThreadPool(6);
    }

    @Before
    public void setup() throws Exception {

        settings = Settings.builder().setPageSpan(3).setPageSize(10).disableFallbackToCacheOnNetworkFailure().setDeliveryScheduler(Schedulers.trampoline()).setLogger(logger)
                           .build();
        PowerMockito.mockStatic(AndroidSchedulers.class);
        PowerMockito.when(AndroidSchedulers.mainThread()).thenReturn(Schedulers.immediate());

        MockitoAnnotations.initMocks(this);

        itemDispatcher = RxStdDispatcher.newInstance(settings, networkSource, networkSource);
        whenNew(SparseArray.class).withAnyArguments().thenReturn(sparseArray);
        fakeArray = new FakeSparseArray<>(5);

        doAnswer(AdditionalAnswers.delegatesTo(fakeArray)).when(sparseArray).size();
        doAnswer(AdditionalAnswers.delegatesTo(fakeArray)).when(sparseArray).clear();
        doAnswer(AdditionalAnswers.delegatesTo(fakeArray)).when(sparseArray).delete(anyInt());
        doAnswer(AdditionalAnswers.delegatesTo(fakeArray)).when(sparseArray).indexOfKey(anyInt());
        doAnswer(AdditionalAnswers.delegatesTo(fakeArray)).when(sparseArray).get(anyInt());
        doAnswer(AdditionalAnswers.delegatesTo(fakeArray)).when(sparseArray).put(anyInt(), any(PageRequest.class));

    }

    @Test
    public void testBiggerPages() throws InterruptedException {

        doAnswer(toAnswer(Observable.empty())).when(networkSource).processRequest(anyPage());
        doAnswer(toAnswer(generateSourcedItems(Source.Network, 0, 0, settings.getPageSize()).delaySubscription(1, TimeUnit.SECONDS, Schedulers.immediate()))).when(networkSource).processRequest(page(0));
        doAnswer(toAnswer(generateSourcedItems(Source.Network, 1, settings.getPageSize(), 18).delaySubscription(5, TimeUnit.SECONDS, Schedulers.immediate()))).when(networkSource).processRequest(page(1));
        doAnswer(toAnswer(generateSourcedItems(Source.Network, 2, 28, 15))).when(networkSource).processRequest(page(2));


        PageManager<Item> manager = new PageManager<Item>(settings);
        manager.beginConnection(itemDispatcher);

        verify(networkSource, times(3)).processRequest(anyPage());
        assertThat(manager.getTotalCount(), is(15 + 18));
    }

    @Test
    public void testOrderedItems() {
        int pageSize = settings.getPageSize();
        doAnswer(toAnswer(Observable.empty())).when(networkSource).processRequest(anyPage());
        doAnswer(toAnswer(generateSourcedItems(Source.Network, 0, 0, pageSize))).when(networkSource).processRequest(page(0));
        doAnswer(toAnswer(generateSourcedItems(Source.Network, 1, pageSize, pageSize))).when(networkSource).processRequest(page(1));
        doAnswer(toAnswer(generateSourcedItems(Source.Network, 2, 2 * pageSize, 1))).when(networkSource).processRequest(page(2));


        PageManager<Item> manager = new PageManager<Item>(settings);
        manager.beginConnection(itemDispatcher);

        assertThat(manager.getTotalCount(), is(2 * pageSize + 1));
        assertThat(manager.getNumPages(), is(3));
        assertThat(manager, itemsInOrder());
    }

    private Matcher<? super PageManager<Item>> itemsInOrder() {
        return new CustomTypeSafeMatcher<PageManager<Item>>("all items are in order") {
            @Override
            protected boolean matchesSafely(PageManager<Item> itemManager) {

                int prevPos = -1;

                for (int i = 0, sz = itemManager.getTotalCount(); i < sz; i++) {
                    final Item item = itemManager.getItem(i);
                    if (prevPos != -1) {
                        int diff = item.absIds - prevPos;
                        if (diff != 1) {
                            return false;
                        }
                    }
                    prevPos = item.absIds;
                }

                return true;
            }
        };
    }

    @Test
    public void testSuperBig() {
        int pageSize = settings.getPageSize();
        doAnswer(toAnswer(Observable.empty())).when(networkSource).processRequest(anyPage());
        doAnswer(toAnswer(generateSourcedItems(Source.Network, 0, 0, 3 * pageSize))).when(networkSource).processRequest(page(0));
        doAnswer(toAnswer(generateSourcedItems(Source.Network, 1, 3 * pageSize, 3 * pageSize))).when(networkSource).processRequest(page(1));
        doAnswer(toAnswer(generateSourcedItems(Source.Network, 2, 6 * pageSize, 3 * pageSize))).when(networkSource).processRequest(page(2));

        PageManager<Item> manager = new PageManager<Item>(settings);
        manager.beginConnection(itemDispatcher);

        assertThat(manager.getTotalCount(), is(3 * pageSize));
        assertThat(manager.getNumPages(), is(1));

    }

    private Answer toAnswer(final Object result) {
        return new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return result;
            }
        };
    }

    private PageRequest anyPage() {
        return argThat(
                new CustomTypeSafeMatcher<PageRequest>("any page request") {
                    @Override
                    protected boolean matchesSafely(PageRequest item) {
                        logger.debug("Maching page " + item.getPage() + " through Any Page matcher", null);
                        return true;
                    }
                }
        );
    }

    private PageRequest page(final int numPage) {
        return argThat(
                new CustomTypeSafeMatcher<PageRequest>("page request for page = " + numPage) {
                    @Override
                    protected boolean matchesSafely(PageRequest item) {
                        logger.debug("Checking page argument " + item.getPage() + ", against " + numPage, null);

                        return item.getPage() == numPage;
                    }
                });
    }

    @After
    public void cleanUp() {

    }

    private Observable<Page<Item>> generateSourcedItems(Source source, int page, int initialOffset, int numItems) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < numItems; i++) {
            items.add(new Item(page, initialOffset + i));
        }
        settings.getLogger().debug("Generating data for page: " + page, null);
        final Observable<Page<Item>> result = pageToObservable(new Page<>(page, initialOffset, source, items));
        settings.getLogger().debug("Result Obs: " + result.getClass().getName() + "@" + Integer.toHexString(result.hashCode()), null);
        return result;
    }

    @NonNull
    private Observable<Page<Item>> pageToObservable(final Page<Item> page) {
        Observable<Page<Item>> observable = Observable.create(
                new Observable.OnSubscribe<Page<Item>>() {
                    @Override
                    public void call(Subscriber<? super Page<Item>> subscriber) {
                        logger.debug("OnSubscribe for page " + page.getPage() + " called", null);
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(page);
                            subscriber.onCompleted();
                        }
                    }
                }
        );

        logger.debug("Created observable : " + observable, null);
        return observable;
//        return Observable.from(Collections.singletonList(page));
    }

}