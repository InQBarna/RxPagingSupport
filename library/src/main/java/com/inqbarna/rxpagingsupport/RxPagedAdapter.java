package com.inqbarna.rxpagingsupport;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

/**
 * Created by david on 14/10/15.
 */
public abstract class RxPagedAdapter<T, VH extends RecyclerView.ViewHolder & RxPagedAdapter.LoadHolder> extends RecyclerView.Adapter<VH> {

    public interface LoadHolder {
        void setLoadingState(boolean loading);
    }

    private PageManager<T> manager;

    public RxPagedAdapter() {
        manager = new PageManager<>(this);
    }

    @Override
    public final VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public final void onBindViewHolder(VH holder, int position) {

    }

    protected abstract VH createLoadingViewHolder(ViewGroup parent);

    protected abstract void doBindViewHolder(VH holder, int position);
    protected abstract VH doCreateViewHolder(ViewGroup parent, int viewType);


    @Override
    public int getItemCount() {
        return manager.isLastPageSeen() ? manager.getTotalCount() + 1 : manager.getTotalCount();
    }
}
