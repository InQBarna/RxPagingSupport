package com.inqbarna.rxpagingsupport.sample;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.inqbarna.rxpagingsupport.RxPagedAdapter;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
public class TestHolder extends RecyclerView.ViewHolder implements RxPagedAdapter.LoadHolder {


    @Nullable
    @Bind(R.id.row_text)
    TextView text;

    public TestHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    @Override
    public void setLoadingState(boolean loading) {

    }

    public void bindTo(DataItem item) {
        if (null != text) {
            text.setText(item.getShowText());
        }
    }
}
