package com.inqbarna.rxpagingsupport;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subjects.PublishSubject;

/**
 * Created by david on 14/10/15.
 */
public class PageManager<T> {

    private static final Comparator<PageInfo> PAGE_INFO_COMPARATOR = new Comparator<PageInfo>() {
        @Override
        public int compare(PageInfo lhs, PageInfo rhs) {
            return lhs.pageNumber - rhs.pageNumber;
        }
    };

    private static class PageInfo<T> {
        IdxRange adapterRange;
        int     pageNumber;
        Source  pageSource;
        List<T> pageItems;
        int     globalStartIdx;
        int     globalEndIdx;


        T getItem(int absoluteIdx) {
            return pageItems.get(absoluteIdx - adapterRange.from);
        }

        void setItems(Collection<? extends T> items, int sourceOffset) {
            pageItems = new ArrayList<>(items);
            globalStartIdx = sourceOffset;
            globalEndIdx = sourceOffset + items.size() - 1;
        }

        int getSize() {
            return null == pageItems ? 0 : pageItems.size();
        }

        static <T> PageInfo<T> fromPage(Page<T> page) {
            PageInfo<T> info = new PageInfo<>();
            info.pageNumber = page.getPage();
            info.pageSource = page.getSource();
            info.setItems(page.getItems(), page.getOffset());
            info.adapterRange = new IdxRange(0, page.getSize() - 1);
            return info;
        }
    }

    public class ConnectionManager {
        public Observable<PageRequest> getPageRequests() {
            return requestsSubject;
        }

        public void establishConnection(Observable<Page<T>> incomes) {
            if (null != activeReceptionSubscription) {
                activeReceptionSubscription.unsubscribe();
            }

            activeReceptionSubscription = bindToIncomes(incomes);
        }
    }

    public void beginConnection(RxDataConnection<T> dataConnection) {
        ConnectionManager connectionManager = new ConnectionManager();
        connectionManager.establishConnection(connectionManager.getPageRequests().flatMap(dataConnection));
    }

    private Subscription bindToIncomes(Observable<Page<T>> incomes) {
        final Subscription subscription = incomes.observeOn(AndroidSchedulers.mainThread())
                                              .subscribe(pageIncomeObserver);
        onBoundToIncomes();
        return subscription;
    }

    private void onBoundToIncomes() {
        deliverMessagesScheduler.createWorker()
                .schedule(
                        new Action0() {
                            @Override
                            public void call() {
                                sendInitialRequests();
                            }
                        }, 300, TimeUnit.MILLISECONDS
                );
    }

    private void sendInitialRequests() {
        if (pages.isEmpty()) {
            int numPagesReq = settings.getPageSpan();
            for (int i = 0; i < numPagesReq; i++) {
                PageRequest pageRequest = PageRequest.createFromPageAndSize(PageRequest.Type.Network, i, settings.getPageSize());
                settings.getLogger().debug("Will send request: " + pageRequest, null);
                requestsSubject.onNext(pageRequest);
            }
        } else {
            settings.getLogger().info("Not doing any initial request, lists are not empty", null);
        }
    }


    private RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            // TODO: 3/11/15 detect movement
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            // TODO: 3/11/15 detect movement
        }
    };

    public T getItem(int pos) {
        NavigableSet<IdxRange> ranges = pageMap.navigableKeySet();
        for (IdxRange range : ranges) {
            if (range.fitsInside(pos)) {
                return pageMap.get(range).getItem(pos);
            }
        }
        return null;
    }

    private Observer<? super Page<T>> pageIncomeObserver = new Observer<Page<T>>() {
        @Override
        public void onCompleted() {
            activeReceptionSubscription = null;
        }

        @Override
        public void onError(Throwable e) {
            settings.getLogger().error("Error loading pages", e);
            activeReceptionSubscription = null;
            // TODO: 30/10/15 notify onwards to the user
        }

        @Override
        public void onNext(Page<T> tPage) {
            addPage(tPage);
        }
    };

    private final Settings settings;

    private int maxPageNumberSeen = -1;
    private boolean lastPageSeen;
    private int     numPages;
    private int     totalCount;
    private boolean registeringMovement;
    private boolean disposed;

    private Scheduler deliverMessagesScheduler;

    private NavigableSet<PageInfo<T>>           pages;
    private NavigableMap<IdxRange, PageInfo<T>> pageMap;

    private final RecyclerView.Adapter        adapter;
    private       PublishSubject<PageRequest> requestsSubject;

    private Subscription activeReceptionSubscription;

    private static class IdxRange implements Comparable<IdxRange> {
        private int from;
        private int to;

        IdxRange(int from, int to) {
            if (from < 0) {
                throw new IllegalArgumentException("Index may not be smaller than 0");
            }

            if (to <= from) {
                throw new IllegalArgumentException("Invalid range size given, cannot be negative or 0 sized");
            }

            this.from = from;
            this.to = to;
        }

        boolean fitsInside(int pos) {
            return pos >= from && pos <= to;
        }

        IdxRange offset(int v) {
            return new IdxRange(from + v, to + v);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            IdxRange idxRange = (IdxRange) o;

            if (from != idxRange.from) {
                return false;
            }
            return to == idxRange.to;

        }

        @Override
        public int hashCode() {
            int result = from;
            result = 31 * result + to;
            return result;
        }

        @Override
        public int compareTo(IdxRange another) {
            return from - another.from;
        }
    }

    public PageManager(RecyclerView.Adapter adapter, Settings settings, Bundle savedInstanceState) {
        this.adapter = adapter;
        this.settings = settings;
        pages = new ConcurrentSkipListSet<>(PAGE_INFO_COMPARATOR);
        numPages = 0;
        totalCount = 0;
        registeringMovement = false;
        requestsSubject = PublishSubject.create();
        disposed = false;
        lastPageSeen = false;
        pageMap = new ConcurrentSkipListMap<>();
        deliverMessagesScheduler = settings.getDeliveryScheduler();

        // TODO: 30/10/15 restore state
    }

    public void addPage(Page<T> page) {

        if (page.isEmpty()) {
            settings.getLogger().debug("Got last \"empty\" page", null);
            lastPageSeen = true;
            adapter.notifyItemRemoved(totalCount); // ok, last page... because it's empty...
            return;
        }

        settings.getLogger().debug("Got next page: " + page.getPage() + ", size: " + page.getSize(), null);
        PageInfo<T> initPage = PageInfo.fromPage(page);
        if (numPages == 0) {
            // ok, just add the first page... nothing special to do.... (but initialize counters)
            insertPage(initPage);
            adapter.notifyDataSetChanged();
        } else {

            //__, __, 2, 3, __, 5, 6, 7, __, __

            // both cannot be null, because numPages is != 0
            PageInfo<T> ceiling = pages.ceiling(initPage);
            PageInfo<T> floor = pages.floor(initPage);
            int offsetSize;
            boolean toAdd = true;

            if (null == ceiling) {
                // we're adding page on the top side....

                initPage.adapterRange = initPage.adapterRange.offset(totalCount);
                floor = null;
                offsetSize = 0; // we won't offset pages indeed

            } else if (null == floor) {
                // we're adding page on the bottom side
                // we have to offset every page on the right of this...

                floor = initPage;

                offsetSize = initPage.getSize(); // we will offset existing pages by this page size

            } else {
                if (pages.comparator().compare(ceiling, floor) == 0) {
                    // both are the same, and seems that we're replacing page then!
                    offsetSize = replacePages(initPage, floor);
                    floor = initPage;
                    toAdd = false;
                } else {
                    // we're a page, in the middle of floor ... ceiling
                    initPage.adapterRange = initPage.adapterRange.offset(floor.adapterRange.to + 1);
                    floor = initPage;
                    offsetSize = initPage.getSize(); // we will offset existing pages by this page size
                }
            }

            if (toAdd) {
                insertPage(initPage);
            } // else, we replaced a page
            if (null != floor) {
                List<PageInfo<T>> aboveFloor = new ArrayList<>(pages.tailSet(floor, false));
                for (PageInfo<T> pi : aboveFloor) {
                    offsetPage(pi, offsetSize);
                }
            }

            if (toAdd) {
                adapter.notifyItemRangeInserted(initPage.adapterRange.from, initPage.getSize());
            } else {
                // we changed items...
                if (offsetSize > 0) {
                    // we replaced pages, with bigger page...
                    adapter.notifyItemRangeInserted(initPage.adapterRange.to - offsetSize, offsetSize);
                    adapter.notifyItemRangeChanged(initPage.adapterRange.from, initPage.getSize() - offsetSize);
                } else if (offsetSize < 0) {
                    adapter.notifyItemRangeRemoved(initPage.adapterRange.to, -offsetSize);
                    adapter.notifyItemRangeChanged(initPage.adapterRange.from, initPage.getSize());
                } else {
                    // we just replaced, with same size page...
                    adapter.notifyItemRangeChanged(initPage.adapterRange.from, initPage.getSize());
                }
            }
        }

    }

    private int replacePages(PageInfo<T> newPage, PageInfo<T> oldPage) {
        int offsetSize;
        newPage.adapterRange = newPage.adapterRange.offset(oldPage.adapterRange.from);
        offsetSize = newPage.getSize() - oldPage.getSize();
        pageMap.remove(oldPage.adapterRange); // remove it because we're replacing the page
        totalCount += offsetSize;
        pages.remove(oldPage);
        pages.add(newPage);
        return offsetSize;
    }

    private void offsetPage(PageInfo<T> pi, int offsetSize) {
        // we don't need to operate on pages, because they're based on page-idx, not in ranges

        // first, remove that from pages and from pagemap
        pageMap.remove(pi.adapterRange);

        // now apply offset, and re-insert in indexes
        pi.adapterRange = pi.adapterRange.offset(offsetSize);
        pageMap.put(pi.adapterRange, pi);
    }

    private void insertPage(PageInfo<T> initPage) {
        pageMap.put(initPage.adapterRange, initPage);
        pages.add(initPage);
        totalCount += initPage.getSize();
        maxPageNumberSeen = Math.max(maxPageNumberSeen, initPage.pageNumber);
        numPages++;
    }


    public int getTotalCount() {
        return totalCount;
    }

    public boolean isLastPageSeen() {
        return lastPageSeen;
    }

    private void checkNoDisposed() {
        if (disposed) {
            throw new IllegalStateException("The manager has been disposed, cannot be used anymore");
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        // TODO: 30/10/15 actually save something
    }

    public void enableMovementDetection(RecyclerView view) {
        if (registeringMovement) {
            throw new IllegalStateException("You are already registeringMovement... disableMovementDetection first from old view");
        }
        checkNoDisposed();
        view.addOnScrollListener(scrollListener);
        registeringMovement = true;
    }

    public void disableMovementDetection(RecyclerView view) {
        if (registeringMovement) {
            view.removeOnScrollListener(scrollListener);
            registeringMovement = false;
        }
    }

    public void recycle() {
        if (!disposed) {
            disposed = true;
            dispatchToSubject(
                    new Action0() {
                        @Override
                        public void call() {
                            requestsSubject.onCompleted();
                        }
                    });
        }
    }

    private void dispatchToSubject(Action0 action) {
        if (null != activeReceptionSubscription) {
            deliverMessagesScheduler.createWorker().schedule(action);
        }
    }
}
