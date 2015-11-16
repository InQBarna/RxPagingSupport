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

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author David García <david.garcia@inqbarna.com>
 */
public class Page<T> {

    private final int page;

    private final int offset;
    private final Source source;
    private int size;
    private List<T> items;
    private Throwable error;

    public static <T> Page<T> empty() {
        return new Page<>();
    }

    public interface PageRecycler<T> {
        void onRecycled(Page<T> page);
    }

    private PageRecycler<T> pageRecycler;

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
        this.pageRecycler = null;
    }

    public Page(int page, Source source, Throwable error) {
        this.error = error;
        this.page = page;
        this.offset = -1; // not important
        this.source = source;
        this.pageRecycler = null;
    }

    private Page() {
        this.error = null;
        this.page = -1;
        this.offset = -1;
        this.source = Source.Cache;
        this.pageRecycler = null;
        this.size = 0;
        this.items = Collections.emptyList();
    }

    public void setPageRecycler(PageRecycler<T> recycler) {
        this.pageRecycler = recycler;
    }

    public int getPage() {
        return page;
    }

    public int getOffset() throws Throwable {
        checkError();
        return offset;
    }

    public boolean hasError() {
        return null != error;
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

    public void recycle() {
        if (null != pageRecycler) {
            pageRecycler.onRecycled(this);
        }
    }
}
