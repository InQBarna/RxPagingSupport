package com.inqbarna.rxpagingsupport;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by david on 14/10/15.
 */
public class Page<T> {
    private final int page;
    private final int offset;
    private final Source source;
    private int size;
    private List<T> items;

    private Throwable error;

    public Page(int page, int offset, Source source) {
        // this is, for last page...

        // we will know it is last page because there are no items... actually, all other
        // arguments should be ignored for an empty page (size == 0)
        this(page, offset, source, Collections.<T>emptyList());
    }

    public Page(int page, int offset, Source source, @NonNull List<T> items) {
        this.page = page;
        this.offset = offset;
        this.source = source;
        this.items = new ArrayList<>(items);
        this.size = this.items.size();
    }

    public Page(int page, Source source, Throwable error) {
        this.error = error;
        this.page = page;
        this.offset = -1; // not important
        this.source = source;
    }

    public int getPage() {
        return page;
    }

    public int getOffset() throws Throwable {
        checkError();
        return offset;
    }

    public Source getSource() {
        return source;
    }

    public int getSize() throws Throwable {
        checkError();
        return size;
    }

    private void checkError() throws Throwable {
        if (null != error) {
            throw error;
        }
    }

    public List<T> getItems() throws Throwable {
        checkError();
        return items;
    }

    public boolean isEmpty() throws Throwable {
        checkError();
        return size == 0;
    }
}
