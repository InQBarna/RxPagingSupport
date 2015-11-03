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

    public int getPage() {
        return page;
    }

    public int getOffset() {
        return offset;
    }

    public Source getSource() {
        return source;
    }

    public int getSize() {
        return size;
    }

    public List<T> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
