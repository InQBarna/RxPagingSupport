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
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

/**
 * @author David García <david.garcia@inqbarna.com>
 */
public class PageManager<T> {

    private static final Comparator<PageInfo> PAGE_INFO_COMPARATOR = new Comparator<PageInfo>() {
        @Override
        public int compare(PageInfo lhs, PageInfo rhs) {
            return lhs.pageNumber - rhs.pageNumber;
        }
    };

    private static final Func1<Object, Boolean> NO_FILTER = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object o) {
            return true;
        }
    };

    public interface PageLoadErrorListener {
        void onError(Throwable error);
    }

    public enum ManagerEventKind {
        SavingState,
        StateSaved,
        RestoringState,
        StateRestored,
        LastPageReceived,
        Recycle
    }

    public static class ManagerEvent {
        public final Object           arg;
        public final ManagerEventKind kind;

        private ManagerEvent(ManagerEventKind kind, Object arg) {
            this.arg = arg;
            this.kind = kind;
        }

        static ManagerEvent event(ManagerEventKind kind) {
            return event(kind, null);
        }

        static ManagerEvent event(ManagerEventKind kind, Object arg) {
            return new ManagerEvent(kind, arg);
        }
    }

    private static class PageInfo<T> implements Parcelable {
        IdxRange adapterRange;
        int      pageNumber;
        Source   pageSource;
        List<T>  pageItems;
        int      globalStartIdx;
        int      globalEndIdx;

        private PageInfo() {
        }

        T getItem(int absoluteIdx) {
            return pageItems.get(absoluteIdx - adapterRange.from);
        }

        void setItems(Collection<? extends T> items, int sourceOffset, Func1<Object, Boolean> filterFunc) {
            int inputSize = items.size();
            pageItems = new ArrayList<>(inputSize);
            globalStartIdx = sourceOffset;
            globalEndIdx = sourceOffset + inputSize - 1;
            for (T item : items) {
                if (filterFunc.call(item)) {
                    pageItems.add(item);
                }
            }

            if (pageItems.size() > 0) {
                adapterRange = new IdxRange(0, pageItems.size() - 1);
            } else {
                adapterRange = new IdxRange(0, 0);
            }
        }

        int getSize() {
            return null == pageItems ? 0 : pageItems.size();
        }

        static <T> PageInfo<T> fromPage(Page<T> page, Func1<Object, Boolean> filterFunc) throws Throwable {
            PageInfo<T> info = new PageInfo<>();
            info.pageNumber = page.getPage();
            info.pageSource = page.getSource();
            info.setItems(page.getItems(), page.getOffset(), filterFunc);

            return info;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("PageInfo{");
            sb.append("pageSource=").append(pageSource);
            sb.append(", pageNumber=").append(pageNumber);
            sb.append(", pageItems=").append(null != pageItems ? pageItems.size() : 0);
            sb.append('}');
            return sb.toString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(adapterRange.from);
            dest.writeInt(adapterRange.to);
            dest.writeInt(pageNumber);
            dest.writeInt(pageSource.ordinal());
            dest.writeInt(pageItems.size());
            dest.writeInt(globalStartIdx);
            dest.writeInt(globalEndIdx);
        }

        private PageInfo(Parcel in) {
            int from = in.readInt();
            int to = in.readInt();
            adapterRange = new IdxRange(from, to);
            pageNumber = in.readInt();
            int tmpInt = in.readInt();
            pageSource = Source.values()[tmpInt];
            tmpInt = in.readInt();
            pageItems = Collections.nCopies(tmpInt, null);
            globalStartIdx = in.readInt();
            globalEndIdx = in.readInt();
        }

        public static final Creator<PageInfo> CREATOR = new Creator<PageInfo>() {
            @Override
            public PageInfo createFromParcel(Parcel source) {
                return new PageInfo(source);
            }

            @Override
            public PageInfo[] newArray(int size) {
                return new PageInfo[size];
            }
        };
    }

    public void beginConnection(RxPageDispatcher<T> dispatcher) {
        if (null != activeReceptionSubscription) {
            activeReceptionSubscription.unsubscribe();
        }

        if (null == eventBehaviorSubject || eventBehaviorSubject.hasCompleted()) {
            eventBehaviorSubject = BehaviorSubject.create();
        }
        dispatcher.setEvents(eventBehaviorSubject.serialize());
        activeReceptionSubscription = bindToIncomes(
                requestsSubject.serialize().map(dispatcher).flatMap(
                        new Func1<Observable<? extends Page<T>>, Observable<? extends Page<T>>>() {
                            @Override
                            public Observable<? extends Page<T>> call(Observable<? extends Page<T>> observable) {
                                return observable.subscribeOn(deliverMessagesScheduler);
                            }
                        }
                ));
        if (null != activeReceptionSubscription) {
            onBoundToIncomes();
        }
    }

    public void setFilterFunc(Func1<T, Boolean> filterFunc) {
        if (filterFunc == null) {
            this.filterFunc = NO_FILTER;
        } else {
            this.filterFunc = (Func1<Object, Boolean>) filterFunc;
        }
    }

    private Subscription bindToIncomes(Observable<Page<T>> incomes) {
        if (!isDisposed()) {
            final Subscription subscription = incomes.observeOn(AndroidSchedulers.mainThread())
                                                     .subscribe(pageIncomeObserver);

            return subscription;
        } else {
            return null;
        }
    }

    private void onBoundToIncomes() {
//        AndroidSchedulers.mainThread().createWorker()
//                         .schedule(
//                                 new Action0() {
//                                     @Override
//                                     public void call() {
//                                         sendInitialRequests();
//                                     }
//                                 }, 300, TimeUnit.MILLISECONDS
//                         );
        sendInitialRequests();
    }

    private void sendInitialRequests() {
        if (pages.isEmpty()) {
            int numPagesReq = settings.getPagesToPrefetch();
            for (int i = 0; i < numPagesReq; i++) {
                requestPage(i, true);
            }
        } else {
            // Ok, we must be recovering state... request exactly same pages, from Cache.... (current pages are just placeholders)
            settings.getLogger().info("Loading pages from saved state, get contents from disk cache", null);
            int shouldRequest = settings.getPageSpan();
            int lastRequested = 0;
            for (PageInfo<T> pageInfo : pages) {
                lastRequested = pageInfo.pageNumber;
                requestPage(lastRequested, false);
                shouldRequest--;
            }

            while (shouldRequest > 0) {
                requestPage(++lastRequested, false);
                shouldRequest--;
            }
        }
    }


    private ManagerScrollListener scrollListener = new ManagerScrollListener();

    private void requestPage(int pageNo, boolean checkNotInPages) {
        if (pageNo < 0 || (lastPageSeen && pageNo > maxPageNumberSeen)) {
            // do not request, avoid work load
            return;
        }
        PageRequest pageRequest = null;
        synchronized (pendingRequests) {
            boolean requestPending = pendingRequests.indexOfKey(pageNo) >= 0;
            if (!requestPending) {


                PageRequest.Type type;
                if (pageNo <= maxPageNumberSeen) {
                    type = PageRequest.Type.Disk;
                } else {
                    type = PageRequest.Type.Network;
                }

                // ensure we're not requesting a page already available
                if (checkNotInPages) {
                    for (PageInfo<T> infos : pages) {
                        if (infos.pageNumber == pageNo) {
                            return;
                        }
                    }
                }

                pageRequest = PageRequest.createFromPageAndSize(type, pageNo, settings.getPageSize());
                pendingRequests.put(pageNo, pageRequest);
            }
        }

        final int pageRequested = pageNo;
        if (null != pageRequest) {
            settings.getLogger().debug("Will send request: " + pageRequest, null);
            dispatchToSubject(
                    new Action0() {
                        @Override
                        public void call() {
                            PageRequest req = null;
                            synchronized (pendingRequests) {
                                req = pendingRequests.get(pageRequested);
                            }
                            if (null != req) {
                                requestsSubject.onNext(req);
                            } else {
                                settings.getLogger().error("Expected a pending request for page " + pageRequested + " but wasn't found", null);
                            }
                        }
                    }
            );
        } else {
            settings.getLogger().debug("Page " + pageNo + " was already in request queue", null);
        }
    }

    public T getItem(int pos) {
        NavigableSet<IdxRange> ranges = pageMap.navigableKeySet();
        for (IdxRange range : ranges) {
            if (range.fitsInside(pos)) {
                return pageMap.get(range).getItem(pos);
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Position ").append(pos).append(" not found in pages!\n")
                .append("Num pages: ").append(pages.size()).append(" in list, ").append(pageMap.size()).append(" in map\n");
        for (IdxRange r : ranges) {
            builder.append(r).append(" with page: ").append(pageMap.get(r)).append("\n");
        }

        builder.append("In list:\n");
        for (PageInfo<T> pi : pages) {
            builder.append(pi).append("\n");
        }
        settings.getLogger().error("Item not found => " + builder.toString(), null);
        return null;
    }

    private Observer<? super Page<T>> pageIncomeObserver = new Observer<Page<T>>() {
        @Override
        public void onCompleted() {
            settings.getLogger().debug("Source completed, acknowledge last page", null);
            activeReceptionSubscription = null;
            if (null != doOnComplete) {
                final Scheduler.Worker worker = deliverMessagesScheduler.createWorker();
                worker.schedule(doOnComplete);
                doOnComplete = null;
            }
            clearPendingRequests();
            acknowledgeLastPage(false);
        }

        @Override
        public void onError(Throwable e) {
            settings.getLogger().error("Error loading pages", e);
            activeReceptionSubscription = null;
            acknowledgeLastPage(false);
            clearPendingRequests();
            forwardError(e);
        }

        @Override
        public void onNext(Page<T> tPage) {
            removePendigRequest(tPage.getPage());
            if (!isDisposed()) {
                addPage(tPage);
            }
            tPage.recycle(); // after added, release any resource...
        }
    };

    private synchronized boolean isDisposed() {
        return disposed;
    }

    private void clearPendingRequests() {
        synchronized (pendingRequests) {
            final int qSize = pendingRequests.size();
            if (qSize > 0) {
                settings.getLogger().info("Will empty pending requests with " + qSize + " uncompleted requests", null);
                pendingRequests.clear();
            }
        }
    }

    private void removePendigRequest(int page) {
        synchronized (pendingRequests) {
            pendingRequests.delete(page);
        }
    }

    private boolean hasPendingRequests() {
        synchronized (pendingRequests) {
            return pendingRequests.size() != 0;
        }
    }

    private final Settings settings;

    private int maxPageNumberSeen = -1;
    private boolean lastPageSeen;
    private int     numPages;
    private int     totalCount;
    private boolean registeringMovement;
    private boolean disposed;
    private int     minAmountItemsLoaded;

    private Func1<Object, Boolean> filterFunc = NO_FILTER;

    private Scheduler deliverMessagesScheduler;

    private       NavigableSet<PageInfo<T>>           pages;
    private       NavigableMap<IdxRange, PageInfo<T>> pageMap;
    private final SparseArray<PageRequest>            pendingRequests;

    @Nullable private RecyclerView.Adapter          adapter;
    private           PublishSubject<PageRequest>   requestsSubject;
    private           PageLoadErrorListener         errorListener;
    public            BehaviorSubject<ManagerEvent> eventBehaviorSubject;

    private Subscription activeReceptionSubscription;
    private Action0 doOnComplete = null;

    private static class IdxRange implements Comparable<IdxRange> {
        private int from;
        private int to;

        IdxRange(int from, int to) {
            if (from < 0) {
                throw new IllegalArgumentException("Index may not be smaller than 0");
            }

            if (to < from) {
                throw new IllegalArgumentException("Invalid range size given, cannot be negative");
            }

            this.from = from;
            this.to = to;
        }

        private IdxRange(int single) {
            if (single < 0) {
                throw new IllegalArgumentException("Index may not be smaller than 0");
            }
            this.from = single;
            this.to = single;
        }

        boolean fitsInside(int pos) {
            return pos >= from && pos <= to;
        }

        IdxRange offset(int v) {
            return new IdxRange(from + v, to + v);
        }

        static IdxRange needle(int searchIdx) {
            return new IdxRange(searchIdx);
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
        public String toString() {
            return "IdxRange{" +
                    "from=" + from +
                    ", to=" + to +
                    '}';
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

    private interface State {
        String LastSeen      = "pagemanager.lastseen";
        String MaxPageNumber = "pagemanager.maxpagenumber";
        String Pages         = "pagemanager.pages";
    }

    public PageManager(Settings settings) {
        this(null, settings, null);
    }

    public PageManager(RecyclerView.Adapter adapter, Settings settings, Bundle savedInstanceState) {
        this.adapter = adapter;
        this.settings = settings;
        pages = new ConcurrentSkipListSet<>(PAGE_INFO_COMPARATOR);
        numPages = 0;
        totalCount = 0;
        registeringMovement = false;
        minAmountItemsLoaded = settings.getPagesToPrefetch() * settings.getPageSize();
        requestsSubject = PublishSubject.create();
        eventBehaviorSubject = BehaviorSubject.create();
        disposed = false;
        lastPageSeen = false;
        maxPageNumberSeen = -1;
        pageMap = new ConcurrentSkipListMap<>();
        deliverMessagesScheduler = settings.getDeliveryScheduler();
        pendingRequests = new SparseArray<>(5);

        if (null != savedInstanceState) {
            initStateFromBundle(savedInstanceState);
        }
    }

    void initStateFromBundle(@NonNull Bundle savedInstanceState) {
        dispatchToEvents(ManagerEvent.event(ManagerEventKind.RestoringState));
        lastPageSeen = savedInstanceState.getBoolean(State.LastSeen);
        maxPageNumberSeen = savedInstanceState.getInt(State.MaxPageNumber);
        final ArrayList<PageInfo<T>> pageInfoStore = savedInstanceState.getParcelableArrayList(State.Pages);
        numPages = pageInfoStore.size();
        for (int i = 0, pageInfoStoreSize = pageInfoStore.size(); i < pageInfoStoreSize; i++) {
            PageInfo<T> pageInfo = pageInfoStore.get(i);
            totalCount += pageInfo.getSize();
            pageMap.put(pageInfo.adapterRange, pageInfo);
            pages.add(pageInfo);
        }
        dispatchToEvents(ManagerEvent.event(ManagerEventKind.StateRestored, savedInstanceState));
    }

    public synchronized void setAdapter(RecyclerView.Adapter adapter) {
        this.adapter = adapter;
    }

    private synchronized RecyclerView.Adapter getAdapter() {
        return adapter;
    }

    public void addPage(Page<T> page) {

        PageInfo<T> newPage;
        int pageSizeReceived;
        try {
            pageSizeReceived = page.getSize();
            if (page.isEmpty()) {
                settings.getLogger().debug("empty page " + page + ", acknowledge last page", null);
                acknowledgeLastPage(true);
                return;
            }

            settings.getLogger().debug("Got next page: " + page.getPage() + ", size: " + pageSizeReceived, null);
            newPage = PageInfo.fromPage(page, filterFunc);
        } catch (Throwable throwable) {
            settings.getLogger().error("Error on incoming page: " + page.getPage(), throwable);
            forwardError(throwable);
            return;
        }

        boolean notifyLastPageAfter = false;
        int newPageSize = newPage.getSize();
        if (pageSizeReceived < settings.getPageSize()) {
            settings.getLogger().debug(
                    "Probably we got last page, because it's smaller than requested page size..." + newPageSize + " < " + settings.getPageSize() + " pageNo = "
                            + newPage.pageNumber, null);
            notifyLastPageAfter = true;
        }

        final RecyclerView.Adapter targetAdapter = getAdapter();
        if (numPages == 0) {
            // ok, just add the first page... nothing special to do.... (but initialize counters)
            insertPage(newPage, false, true);
        } else {

            //__, __, 2, 3, __, 5, 6, 7, __, __

            // both cannot be null, because numPages is != 0
            PageInfo<T> ceiling = pages.ceiling(newPage);
            PageInfo<T> floor = pages.floor(newPage);

            if (null == ceiling) {
                // we're adding page on the top side....

                addNewPageAtEnd(newPage);
            } else if (null == floor) {
                // we're adding page on the bottom side
                // we have to offset every page on the right of this...

                addNewPageAtBegining(newPage);

            } else {
                if (pages.comparator().compare(ceiling, floor) == 0) {
                    // both are the same, and seems that we're replacing page then!
                    int offsetSize = replacePages(newPage, floor);
                    applyOffsetToItemsFrom(newPage, offsetSize);
                    // we changed items... (else, we replaced a page)
                    if (offsetSize > 0) {
                        // we replaced pages, with bigger page...
                        if (null != targetAdapter) {
                            targetAdapter.notifyItemRangeInserted(newPage.adapterRange.to - offsetSize, offsetSize);
                            targetAdapter.notifyItemRangeChanged(newPage.adapterRange.from, newPageSize - offsetSize);
                        }
                    } else if (offsetSize < 0) {
                        if (null != targetAdapter) {
                            targetAdapter.notifyItemRangeRemoved(newPage.adapterRange.to, -offsetSize);
                            targetAdapter.notifyItemRangeChanged(newPage.adapterRange.from, newPageSize);
                        }
                    } else {
                        // we just replaced, with same size page...
                        if (null != targetAdapter && newPageSize != 0) {
                            targetAdapter.notifyItemRangeChanged(newPage.adapterRange.from, newPageSize);
                        }
                    }
                } else {
                    // we're a page, in the middle of floor ... ceiling
                    addNewPageAfter(newPage, floor);
                }
            }
        }

        if (notifyLastPageAfter) {
            acknowledgeLastPage(true);
        }

        if (!hasPendingRequests()) {
            if (totalCount < minAmountItemsLoaded && !lastPageSeen) {
                requestPage(maxPageNumberSeen + 1, false);
            } else if (null != expectedMove) {
                expectedMove.run();
                expectedMove = null;
            }
        }
    }

    private void addNewPageAfter(PageInfo<T> newPage, PageInfo<T> floor) {
        if (getEstimatedNumPages() == settings.getPageSpan()) {
            // which side to remove, left or right?
            final int itemsLeft = pages.headSet(newPage, false).size();
            final int itemsRight = pages.tailSet(newPage, false).size();
            if (itemsLeft >= itemsRight) {
                removeFirstPage();
                if (itemsLeft == 1) {
                    floor = null;
                }
            } else {
                removeLastPage();
                if (itemsRight == 1) {
                    floor = null;
                }
            }
        }

        if (null != floor) {
            newPage.adapterRange = newPage.adapterRange.offset(floor.adapterRange.to + 1);
        }
        insertPage(newPage, true);
    }

    public synchronized void setErrorListener(PageLoadErrorListener listener) {
        this.errorListener = listener;
    }

    private void addNewPageAtBegining(PageInfo<T> newPage) {
        if (getEstimatedNumPages() == settings.getPageSpan()) {
            removeLastPage();
        }


        insertPage(newPage, true);
    }

    private void removeLastPage() {
        PageInfo<T> toRemove = pages.pollLast();
        pageMap.remove(toRemove.adapterRange);
        totalCount -= toRemove.getSize();
        numPages--;
        final RecyclerView.Adapter targetAdapter = getAdapter();
        if (null != targetAdapter) {
            targetAdapter.notifyItemRangeRemoved(toRemove.adapterRange.from, toRemove.getSize());
        }
    }

    private void applyOffsetToItemsFrom(PageInfo<T> floor, int offsetSize) {
        if (offsetSize == 0)
            return; // nothing to do
        NavigableSet<PageInfo<T>> tailInfos = pages.tailSet(floor, false);
        List<PageInfo<T>> aboveFloor = new ArrayList<>(offsetSize > 0 ? tailInfos.descendingSet() : tailInfos);
        for (PageInfo<T> pi : aboveFloor) {
            offsetPage(pi, offsetSize);
        }
    }

    private int getEstimatedNumPages() {
        return (int)(((double)totalCount / settings.getPageSize()) + 0.5);
    }

    private void addNewPageAtEnd(PageInfo<T> newPage) {
        if (getEstimatedNumPages() == settings.getPageSpan()) {
            removeFirstPage();
        }

        newPage.adapterRange = newPage.adapterRange.offset(totalCount);
        insertPage(newPage, false);
    }

    private void removeFirstPage() {
        PageInfo<T> toRemove = pages.pollFirst();
        pageMap.remove(toRemove.adapterRange);
        totalCount -= toRemove.getSize();
        numPages--;
        final RecyclerView.Adapter targetAdapter = getAdapter();
        if (null != targetAdapter) {
            targetAdapter.notifyItemRangeRemoved(toRemove.adapterRange.from, toRemove.getSize());
        }
        applyOffsetToItemsFrom(toRemove, -toRemove.getSize());
    }

    private void acknowledgeLastPage(boolean fromEmptyPage) {
        if (!lastPageSeen) {
            settings.getLogger().debug("Acknowledged LAST page seen", null);
            lastPageSeen = true;
            final RecyclerView.Adapter targetAdapter = getAdapter();
            if (null != targetAdapter) {
                targetAdapter.notifyItemRemoved(totalCount); // ok, last page... because it's empty...
            }
            dispatchToEvents(ManagerEvent.event(ManagerEventKind.LastPageReceived));
            //            if (fromEmptyPage) {
            //                dispatchToSubject(
            //                        new Action0() {
            //                            @Override
            //                            public void call() {
            //                                requestsSubject.onCompleted();
            //                            }
            //                        }
            //                );
            //            }
        }
    }

    private void forwardError(Throwable throwable) {
        PageLoadErrorListener listener;
        synchronized (this) {
            listener = errorListener;
        }

        if (null != listener) {
            listener.onError(throwable);
        }
    }

    private int replacePages(PageInfo<T> newPage, PageInfo<T> oldPage) {
        int offsetSize;
        newPage.adapterRange = newPage.adapterRange.offset(oldPage.adapterRange.from);
        int newPageSize = newPage.getSize();
        offsetSize = newPageSize - oldPage.getSize();
        pageMap.remove(oldPage.adapterRange); // remove it because we're replacing the page
        pages.remove(oldPage);
        if (newPageSize > 0) {
            pageMap.put(newPage.adapterRange, newPage);
            pages.add(newPage);
        }
        totalCount += offsetSize;
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

    private void insertPage(PageInfo<T> newPage, boolean withOffsets) {
        insertPage(newPage, withOffsets, false);
    }

    private void insertPage(PageInfo<T> newPage, boolean withOffsets, boolean notifyFullUpdate) {

        // special case, all items where filtered... account page seen, but do not add it...
        if (newPage.getSize() == 0) {
            maxPageNumberSeen = Math.max(maxPageNumberSeen, newPage.pageNumber);
            return;
        }

        if (withOffsets) {
            applyOffsetToItemsFrom(newPage, newPage.getSize());
        }
        pageMap.put(newPage.adapterRange, newPage);
        pages.add(newPage);
        totalCount += newPage.getSize();
        maxPageNumberSeen = Math.max(maxPageNumberSeen, newPage.pageNumber);
        numPages++;

        final RecyclerView.Adapter targetAdapter = getAdapter();
        if (null != targetAdapter) {
            if (notifyFullUpdate) {
                targetAdapter.notifyDataSetChanged();
            } else {
                targetAdapter.notifyItemRangeInserted(newPage.adapterRange.from, newPage.getSize());
            }
        }
    }


    public int getTotalCount() {
        return totalCount;
    }

    private int getExpectedTotalItems() {
        return numPages * settings.getPageSize();
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
        dispatchToEvents(ManagerEvent.event(ManagerEventKind.SavingState));
        outState.putBoolean(State.LastSeen, lastPageSeen);
        outState.putInt(State.MaxPageNumber, maxPageNumberSeen);
        ArrayList<PageInfo<T>> pageInfos = new ArrayList<>(pages);
        outState.putParcelableArrayList(State.Pages, pageInfos);
        List<Page<T>> pagesSaved = new ArrayList<>(pageInfos.size());
        for (int i = 0, sz = pageInfos.size(); i < sz; i++) {
            final PageInfo<T> info = pageInfos.get(i);
            pagesSaved.add(new Page<>(info.pageNumber, info.globalStartIdx, info.pageSource, info.pageItems));
        }
        dispatchToEvents(ManagerEvent.event(ManagerEventKind.StateSaved, pagesSaved));
    }

    public void enableMovementDetection(RecyclerView view) {
        if (registeringMovement) {
            throw new IllegalStateException("You are already registeringMovement... disableMovementDetection first from old view");
        }
        checkNoDisposed();
        settings.getLogger().debug("Enabling scroll listener for movement detection", null);
        scrollListener.resetState();
        view.addOnScrollListener(scrollListener);
        registeringMovement = true;
    }

    public void disableMovementDetection(RecyclerView view) {
        if (registeringMovement) {
            settings.getLogger().debug("Disabling scroll listener for movement detection", null);
            view.removeOnScrollListener(scrollListener);
            registeringMovement = false;
        }
    }

    public synchronized void recycle() {
        if (!disposed) {
            disposed = true;
            scrollListener.resetState();
            settings.getLogger().debug("Disposing Page manager", null);

            dispatchToSubject(
                    new Action0() {
                        @Override
                        public void call() {
                            requestsSubject.onCompleted();
                        }
                    });

            doOnComplete = new Action0() {
                @Override
                public void call() {
                    dispatchToEvents(ManagerEvent.event(ManagerEventKind.Recycle), true);
                }
            };
        }
    }

    private void dispatchToSubject(Action0 action) {
        action.call();
    }

    private void dispatchToEvents(ManagerEvent event) {
        dispatchToEvents(event, false);
    }

    private void dispatchToEvents(final ManagerEvent event, final boolean terminate) {
        if (null != eventBehaviorSubject && !eventBehaviorSubject.hasCompleted()) {
            eventBehaviorSubject.onNext(event);
            if (terminate) {
                eventBehaviorSubject.onCompleted();
                eventBehaviorSubject = null;
            }
        }
    }

    int getNumPages() {
        return pages.size();
    }

    int getFirstItemOffset() {
        if (pages.size() == 0) {
            return 0;
        }
        final PageInfo<T> first = pages.first();
        return first.globalStartIdx;
    }

    int getLastItemOffset() {
        if (pages.size() == 0) {
            return 0;
        }

        final PageInfo<T> last = pages.last();
        return last.globalEndIdx;
    }

    Source getSourceOfPos(int adapterPosition) {
        if (adapterPosition >= totalCount) {
            return null;
        }
        IdxRange containingPageRange = pageMap.floorKey(IdxRange.needle(adapterPosition));
        return pageMap.get(containingPageRange).pageSource;
    }

    private Runnable expectedMove = null;

    private class ManagerScrollListener extends RecyclerView.OnScrollListener {
        final int WAIT          = 0;
        final int GET_DIRECTION = 1;
        final int SETTLE_DOWN   = 2;

        private final int DOWN = 1;
        private final int UP   = 2;
        private final int NONE = 0;

        private int direction = NONE;
        private int myState   = WAIT; // initial state

        private long absDisplacement = 0;

        void resetState() {
            myState = WAIT;
            direction = NONE;
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            settings.getLogger().debug("Scroll state change detected: " + newState, null);
            switch (newState) {
                case RecyclerView.SCROLL_STATE_SETTLING:
                    if (myState == GET_DIRECTION) {
                        myState = SETTLE_DOWN;
                    } else if (myState == WAIT && null != activeReceptionSubscription) {
                        myState = GET_DIRECTION;
                    }
                    break;
                case RecyclerView.SCROLL_STATE_IDLE:
                    if (myState != WAIT) {
                        myState = WAIT;
                        if (absDisplacement > 0) {
                            int height = onMovingDown(recyclerView);
                            if (height > 0) {
                                expectedMove = expectedDown(recyclerView, height);
                            }
                        } else if (absDisplacement < 0) {
                            int height = onMovingUp(recyclerView);
                            if (height > 0) {
                                expectedMove = expectedDown(recyclerView, -height);
                            }
                        }
                    }
                    break;
                case RecyclerView.SCROLL_STATE_DRAGGING:
                    expectedMove = null;
                    if (myState == WAIT && null != activeReceptionSubscription) {
                        absDisplacement = 0;
                        myState = GET_DIRECTION;
                    }
                default:
                    // no-op
            }
        }

        private Runnable expectedDown(final RecyclerView recyclerView, final int dy) {
            return new Runnable() {
                @Override
                public void run() {
                    recyclerView.smoothScrollBy(0, dy);
                }
            };
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            //            settings.getLogger().debug("Scroll detected dy = " + dy, null);
            if (myState == GET_DIRECTION) {
                absDisplacement += dy;
            }
        }

        private int onMovingUp(RecyclerView recyclerView) {
            final int nChilds = recyclerView.getChildCount();
            if (nChilds == 0) {
                return -1; // should never happen indeed (we can scroll because there are elements!)
            }

            final View firstChild = recyclerView.getChildAt(0);
            final int firstChildPos = recyclerView.getChildAdapterPosition(firstChild);
            if (firstChildPos <= settings.getPrefetchDistance()) {
                int toRenew =  settings.getPagesToPrefetch();
                int reqPage = -1;
                if (pages.size() > 0) {
                    reqPage = pages.first().pageNumber - 1;
                }

                while (reqPage >= 0 && toRenew > 0) {
                    requestPage(reqPage--, true);
                    toRenew--;
                }

            }
            return firstChild.getHeight();
        }

        private int onMovingDown(RecyclerView recyclerView) {
            final int nChilds = recyclerView.getChildCount();
            if (nChilds == 0) {
                return -1; // should never happen indeed (we can scroll because there are elements!)
            }
            final View lastChildView = recyclerView.getChildAt(nChilds - 1);
            final int lastChild = recyclerView.getChildAdapterPosition(lastChildView);
            if (getTotalCount() - 1 - lastChild <= settings.getPrefetchDistance()) {
                int toRenew = settings.getPagesToPrefetch();
                int pageRequest = maxPageNumberSeen + 1;
                if (pages.size() > 0) {
                    pageRequest = pages.last().pageNumber + 1;
                }
                while (toRenew > 0) {
                    requestPage(pageRequest++, true);
                    toRenew--;
                }
            }
            return lastChildView.getHeight();
        }
    }
}
