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
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;
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
 * @author David García <david.garcia@inqbarna.com>
 */
public class PageManager<T> {

    private static final Comparator<PageInfo> PAGE_INFO_COMPARATOR = new Comparator<PageInfo>() {
        @Override
        public int compare(PageInfo lhs, PageInfo rhs) {
            return lhs.pageNumber - rhs.pageNumber;
        }
    };

    public interface PageLoadErrorListener {
        void onError(Throwable error);
    }

    private static class PageInfo<T> implements Parcelable {
        IdxRange adapterRange;
        int     pageNumber;
        Source  pageSource;
        List<T> pageItems;
        int     globalStartIdx;
        int     globalEndIdx;

        private PageInfo() {
        }

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

        static <T> PageInfo<T> fromPage(Page<T> page) throws Throwable {
            PageInfo<T> info = new PageInfo<>();
            info.pageNumber = page.getPage();
            info.pageSource = page.getSource();
            info.setItems(page.getItems(), page.getOffset());
            info.adapterRange = new IdxRange(0, page.getSize() - 1);
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
            pageItems = new ArrayList<>(tmpInt);
            Collections.fill(pageItems, null);
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
        beginConnection(dispatcher, null);
    }

    public void beginConnection(RxPageDispatcher<T> dispatcher, Action0 onEndAction) {
        if (null != activeReceptionSubscription) {
            activeReceptionSubscription.unsubscribe();
        }

        activeReceptionSubscription = bindToIncomes(requestsSubject.flatMap(dispatcher), onEndAction);
        if (null != activeReceptionSubscription) {
            onBoundToIncomes();
        }
    }


    private Subscription bindToIncomes(Observable<Page<T>> incomes, final Action0 onEndAction) {
        if (!disposed) {
            final Subscription subscription = incomes.observeOn(AndroidSchedulers.mainThread())
                                                     .finallyDo(
                                                             new Action0() {
                                                                 @Override
                                                                 public void call() {
                                                                     if (null != onEndAction) {
                                                                         onEndAction.call();
                                                                     }
                                                                 }
                                                             }
                                                     )
                                                     .subscribe(pageIncomeObserver);

            return subscription;
        } else {
            if (null != onEndAction) {
                onEndAction.call();
            }
            return null;
        }
    }

    private void onBoundToIncomes() {
        AndroidSchedulers.mainThread().createWorker()
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
                requestPage(i, true);
            }
        } else {
            // Ok, we must be recovering state... request exactly same pages, from Cache.... (current pages are just placeholders)
            settings.getLogger().info("Loading pages from saved state, get contents from disk cache", null);
            for (PageInfo<T> pageInfo : pages) {
                requestPage(pageInfo.pageNumber, false);
            }
        }
    }


    private ManagerScrollListener scrollListener = new ManagerScrollListener();

    private void requestPage(int pageNo, boolean checkNotInPages) {
        PageRequest pageRequest = null;
        synchronized (pendingRequests) {
            boolean requestPending = pendingRequests.indexOfKey(pageNo) >= 0;
            if (!requestPending) {
                pageNo = Math.max(0, lastPageSeen ? Math.min(pageNo, maxPageNumberSeen) : pageNo); // safety clipping
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
            settings.getLogger().debug("Source completed, acknowledge last pate", null);
            activeReceptionSubscription = null;
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
            addPage(tPage);
            tPage.recycle(); // after added, release any resource...
        }
    };

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

    private final Settings settings;

    private int maxPageNumberSeen = -1;
    private boolean lastPageSeen;
    private int     numPages;
    private int     totalCount;
    private boolean registeringMovement;
    private boolean disposed;

    private Scheduler deliverMessagesScheduler;

    private       NavigableSet<PageInfo<T>>           pages;
    private       NavigableMap<IdxRange, PageInfo<T>> pageMap;
    private final SparseArray<PageRequest>            pendingRequests;

    private final RecyclerView.Adapter        adapter;
    private       PublishSubject<PageRequest> requestsSubject;
    private       PageLoadErrorListener       errorListener;

    private Subscription activeReceptionSubscription;

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
        maxPageNumberSeen = -1;
        pageMap = new ConcurrentSkipListMap<>();
        deliverMessagesScheduler = settings.getDeliveryScheduler();
        pendingRequests = new SparseArray<>(5);

        if (null != savedInstanceState) {
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
        }
    }

    public void addPage(Page<T> page) {

        PageInfo<T> newPage;
        try {
            if (page.isEmpty()) {
                settings.getLogger().debug("empty page " + page + ", acknowledge last page", null);
                acknowledgeLastPage(true);
                return;
            }

            settings.getLogger().debug("Got next page: " + page.getPage() + ", size: " + page.getSize(), null);
            newPage = PageInfo.fromPage(page);
        } catch (Throwable throwable) {
            settings.getLogger().error("Error on incoming page: " + page.getPage(), throwable);
            forwardError(throwable);
            return;
        }

        if (newPage.getSize() != settings.getPageSize()) {
            settings.getLogger().info(
                    "Probably we got last page, because it's smaller than requested page size..." + newPage.getSize() + " < " + settings.getPageSize() + " pageNo = "
                            + newPage.pageNumber, null);
            // TODO: 4/11/15 do something special here? or let the request next page return the empty page....
        }

        if (numPages == 0) {
            // ok, just add the first page... nothing special to do.... (but initialize counters)
            insertPage(newPage, false);
            adapter.notifyDataSetChanged();
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
                        adapter.notifyItemRangeInserted(newPage.adapterRange.to - offsetSize, offsetSize);
                        adapter.notifyItemRangeChanged(newPage.adapterRange.from, newPage.getSize() - offsetSize);
                    } else if (offsetSize < 0) {
                        adapter.notifyItemRangeRemoved(newPage.adapterRange.to, -offsetSize);
                        adapter.notifyItemRangeChanged(newPage.adapterRange.from, newPage.getSize());
                    } else {
                        // we just replaced, with same size page...
                        adapter.notifyItemRangeChanged(newPage.adapterRange.from, newPage.getSize());
                    }
                } else {
                    // we're a page, in the middle of floor ... ceiling
                    addNewPageAfter(newPage, floor);
                }
            }
        }
    }

    private void addNewPageAfter(PageInfo<T> newPage, PageInfo<T> floor) {
        if (numPages == settings.getPageSpan()) {
            // which side to remove, left or right?
            final int itemsLeft = pages.headSet(newPage, false).size();
            final int itemsRight = pages.tailSet(newPage, false).size();
            if (itemsLeft >= itemsRight) {
                removeFirstPage();
            } else {
                removeLastPage();
            }
        }

        newPage.adapterRange = newPage.adapterRange.offset(floor.adapterRange.to + 1);
        insertPage(newPage, true);
    }

    public synchronized void setErrorListener(PageLoadErrorListener listener) {
        this.errorListener = listener;
    }

    private void addNewPageAtBegining(PageInfo<T> newPage) {
        if (numPages == settings.getPageSpan()) {
            removeLastPage();
        }


        insertPage(newPage, true);
    }

    private void removeLastPage() {
        PageInfo<T> toRemove = pages.pollLast();
        pageMap.remove(toRemove.adapterRange);
        totalCount -= toRemove.getSize();
        numPages--;
        adapter.notifyItemRangeRemoved(toRemove.adapterRange.from, toRemove.getSize());
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

    private void addNewPageAtEnd(PageInfo<T> newPage) {
        if (numPages == settings.getPageSpan()) {
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
        adapter.notifyItemRangeRemoved(toRemove.adapterRange.from, toRemove.getSize());
        applyOffsetToItemsFrom(toRemove, -toRemove.getSize());
    }

    private void acknowledgeLastPage(boolean fromEmptyPage) {
        if (!lastPageSeen) {
            settings.getLogger().debug("Got last \"empty\" page", null);
            lastPageSeen = true;
            adapter.notifyItemRemoved(totalCount); // ok, last page... because it's empty...
            if (fromEmptyPage) {
                dispatchToSubject(
                        new Action0() {
                            @Override
                            public void call() {
                                requestsSubject.onCompleted();
                            }
                        }
                );
            }
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
        offsetSize = newPage.getSize() - oldPage.getSize();
        pageMap.remove(oldPage.adapterRange); // remove it because we're replacing the page
        pageMap.put(newPage.adapterRange, newPage);
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

    private void insertPage(PageInfo<T> newPage, boolean withOffsets) {
        if (withOffsets) {
            applyOffsetToItemsFrom(newPage, newPage.getSize());
        }
        pageMap.put(newPage.adapterRange, newPage);
        pages.add(newPage);
        totalCount += newPage.getSize();
        maxPageNumberSeen = Math.max(maxPageNumberSeen, newPage.pageNumber);
        numPages++;
        adapter.notifyItemRangeInserted(newPage.adapterRange.from, newPage.getSize());
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
        outState.putBoolean(State.LastSeen, lastPageSeen);
        outState.putInt(State.MaxPageNumber, maxPageNumberSeen);
        ArrayList<PageInfo<T>> pageInfos = new ArrayList<>(pages);
        outState.putParcelableArrayList(State.Pages, pageInfos);
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

    public void recycle() {
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
        }
    }

    private void dispatchToSubject(Action0 action) {
        if (null != activeReceptionSubscription) {
            deliverMessagesScheduler.createWorker().schedule(action);
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
                            onMovingDown(recyclerView);
                        } else if (absDisplacement < 0) {
                            onMovingUp(recyclerView);
                        }
                    }
                    break;
                case RecyclerView.SCROLL_STATE_DRAGGING:
                    if (myState == WAIT && null != activeReceptionSubscription) {
                        absDisplacement = 0;
                        myState = GET_DIRECTION;
                    }
                default:
                    // no-op
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            //            settings.getLogger().debug("Scroll detected dy = " + dy, null);
            if (myState == GET_DIRECTION) {
                absDisplacement += dy;
            }
        }

        private void onMovingUp(RecyclerView recyclerView) {
            final int nChilds = recyclerView.getChildCount();
            if (nChilds == 0) {
                return; // should never happen indeed (we can scroll because there are elements!)
            }

            final int hidenItemCount = getTotalCount() - nChilds;

            final int hidenPagesCount = hidenItemCount / settings.getPageSize();
            final int firstChildPos = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(0));
            final int numSidePages = (settings.getPageSpan() - 1) / 2;
            final IdxRange floorKey = pageMap.floorKey(IdxRange.needle(firstChildPos)); // this gives which page first child lives within
            // NOTE: floorKey must not be null, otherwise we're doing something very bad....
            final SortedMap<IdxRange, PageInfo<T>> headMap = pageMap.headMap(floorKey, true);
            if (headMap.size() <= numSidePages) {
                int reqPage = 0;
                if (headMap.size() > 0) {
                    final PageInfo<T> firstPage = headMap.get(headMap.firstKey());
                    reqPage = Math.max(0, firstPage.pageNumber - 1);
                }
                int toRenew = Math.min(hidenPagesCount, numSidePages - headMap.size() + 1);
                while (reqPage >= 0 && toRenew > 0) {
                    requestPage(reqPage--, true);
                    toRenew--;
                }
            }
        }

        private void onMovingDown(RecyclerView recyclerView) {
            final int nChilds = recyclerView.getChildCount();
            final int hidenItemCount = getTotalCount() - nChilds;
            final int hidenPagesCount = hidenItemCount / settings.getPageSize();
            final int lastChild = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(nChilds - 1));
            final int numSidePages = (settings.getPageSpan() - 1) / 2;
            final IdxRange floorKey = pageMap.floorKey(IdxRange.needle(lastChild));
            // NOTE: floorKey must not be null, otherwise we're doing something very bad....
            final SortedMap<IdxRange, PageInfo<T>> tailMap = pageMap.tailMap(floorKey);
            if (tailMap.size() <= numSidePages) {
                // ok, last item shown is within span renew portion...

                int pageNoRequest = maxPageNumberSeen + 1;
                if (tailMap.size() > 0) {
                    PageInfo<T> lastPage = tailMap.get(tailMap.lastKey());
                    pageNoRequest = lastPage.pageNumber + 1;
                }
                int toRenew = Math.min(hidenPagesCount, numSidePages - tailMap.size() + 1);
                while (toRenew > 0) {
                    requestPage(pageNoRequest++, true);
                    toRenew--;
                }
            }
        }
    }
}
