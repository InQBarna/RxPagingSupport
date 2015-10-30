package com.inqbarna.rxpagingsupport;

import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by david on 14/10/15.
 */
public class PageManager<T> {

    private static final int MAX_PAGES = 5; // TODO: 18/10/15 make this configurable

    private static final Comparator<PageInfo> PAGE_INFO_COMPARATOR = new Comparator<PageInfo>() {
        @Override
        public int compare(PageInfo lhs, PageInfo rhs) {
            return lhs.pageNumber - rhs.pageNumber;
        }
    };

    private static class PageInfo<T> {
        int startIdx;
        int endIdx;
        int pageNumber;
        Source pageSource;
        List<T> pageItems;

        T getItem(int absoluteIdx) {
            return pageItems.get(absoluteIdx - startIdx);
        }

        static <T> PageInfo<T> emptyPage(int pageNumber) {
            PageInfo<T> info = new PageInfo<>();
            info.pageNumber = pageNumber;
            return info;
        }
    }

    private int maxPageNumberSeen = -1;
    private boolean lastPageSeen;
    private int numPages;
    private int firstPage;
    private int lastPage;
    private int totalCount;
    private NavigableSet<PageInfo<T>> pages;

    private final RecyclerView.Adapter adapter;

    public PageManager(RecyclerView.Adapter adapter) {
        this.adapter = adapter;
        pages = new ConcurrentSkipListSet<>(PAGE_INFO_COMPARATOR);
        firstPage = -1;
        lastPage = -1;
        numPages = 0;
    }

    public void addPage(Page<T> page) {
        PageInfo<T> initPage = PageInfo.emptyPage(page.getPage());
        if (numPages == 0) {

        } else {
            int inPageNo = initPage.pageNumber;
            int firstPageNo = firstPage;
            int lastPageNo = lastPage;
        }
    }

    public int getTotalCount() {
        return totalCount;
    }

    public boolean isLastPageSeen() {
        return lastPageSeen;
    }
}
