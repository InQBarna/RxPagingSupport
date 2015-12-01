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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import rx.functions.Action0;

/**
 * @author David García <david.garcia@inqbarna.com>
 */
public abstract class RxPagedAdapter<T, VH extends RecyclerView.ViewHolder & RxPagedAdapter.LoadHolder> extends RecyclerView.Adapter<VH> {

    protected final static int RESERVED_LOADING_TYPE = R.layout.reserved_loading;

    public interface LoadHolder {
        void setLoadingState(boolean loading);
    }

    private       PageManager<T> manager;
    private final Settings       settings;

    public RxPagedAdapter(PageManager<T> manager, Settings settings, @Nullable Bundle savedInstanceState) {
        this.settings = settings;
        this.manager = manager;
        this.manager.setAdapter(this);
        this.manager.initStateFromBundle(savedInstanceState);
        initState(savedInstanceState);
    }

    public RxPagedAdapter(Settings settings, @Nullable Bundle savedInstanceState) {
        this.settings = settings;
        manager = new PageManager<>(this, settings, savedInstanceState);
        initState(savedInstanceState);
    }

    private void initState(@Nullable Bundle savedInstanceState) {
        // TODO: 25/11/15
    }

    public Settings getSettings() {
        return settings;
    }

    public void enableMovementDetection(RecyclerView view) {
        manager.enableMovementDetection(view);
    }

    public void disableMovementDetection(RecyclerView view) {
        manager.disableMovementDetection(view);
    }

    @Override
    public final VH onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == RESERVED_LOADING_TYPE) {
            return createLoadingViewHolder(parent);
        } else {
            return doCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public final void onBindViewHolder(VH holder, int position) {
        if (isLastPosition(position)) {
            holder.setLoadingState(true);
        } else {
            T item = getItem(position);
            if (null != item) {
                doBindViewHolder(holder, item, position);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isLastPosition(position)) {
            settings.getLogger().debug("Should show progress at pos " + position, null);
            return RESERVED_LOADING_TYPE;
        }
        return View.NO_ID;
    }

    public void setErrorListener(PageManager.PageLoadErrorListener listener) {
        manager.setErrorListener(listener);
    }

    private boolean isLastPosition(int position) {
        // this is true, when we should do special loading operation...
        return !manager.isLastPageSeen() && position == manager.getTotalCount();
    }

    protected abstract VH createLoadingViewHolder(ViewGroup parent);

    protected abstract void doBindViewHolder(VH holder, @NonNull T item, int position);
    protected abstract VH doCreateViewHolder(ViewGroup parent, int viewType);

    public T getItem(int pos) {
        final T item = manager.getItem(pos);
//        if (null == item) {
//            StringBuilder builder = new StringBuilder();
//            builder.append("Will crash on NPE..., no item returned:\n")
//                   .append("Item pos requested: ").append(pos).append("\n")
//                    .append("Total Count: ").append(manager.getTotalCount()).append("\n")
//                    .append("Last seen: ").append(manager.isLastPageSeen());
//            settings.getLogger().error(builder.toString(), null);
//        }
        return item;
    }

    public void beginConnection(RxPageDispatcher<T> dispatcher) {
        manager.beginConnection(dispatcher);
    }

    public int getTotalCount() {
        return manager.getTotalCount();
    }

    @Override
    public int getItemCount() {
        return !manager.isLastPageSeen() ? manager.getTotalCount() + 1 : manager.getTotalCount();
    }

    public void recycle() {
        manager.recycle();
    }

    public void onSaveInstanceState(Bundle outState) {
        manager.onSaveInstanceState(outState);
    }

    public RecyclerView.ItemDecoration newDebugDecoration(RecyclerView recyclerView) {
        return new RxDebugItemDecoration(recyclerView, manager);
    }
}
