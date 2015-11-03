package com.inqbarna.rxpagingsupport.sample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.inqbarna.rxpagingsupport.RxPagedAdapter;
import com.inqbarna.rxpagingsupport.Settings;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
public class TestAdapter extends RxPagedAdapter<DataItem, TestHolder>{
    public TestAdapter(Settings settings, Bundle savedInstanceState) {
        super(settings, savedInstanceState);
    }

    @Override
    protected TestHolder createLoadingViewHolder(ViewGroup parent) {
        final ProgressBar view = new ProgressBar(parent.getContext());
        view.setId(R.id.progress);
        view.setIndeterminate(true);
        return new TestHolder(view);
    }

    @Override
    protected void doBindViewHolder(TestHolder holder, int position) {
        holder.bindTo(getItem(position));
    }

    @Override
    protected TestHolder doCreateViewHolder(ViewGroup parent, int viewType) {
        return new TestHolder(LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        int vt = super.getItemViewType(position);
        if (vt != RESERVED_LOADING_TYPE) {
            return R.layout.row;
        } else {
            return RESERVED_LOADING_TYPE;
        }
    }
}
