package com.inqbarna.rxpagingsupport.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity {

    private static final String TAG = "RxPaging";

    @Bind(R.id.recycler)
    RecyclerView recyclerView;

    private TestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TestAdapter(getComponent().getRxSettings(), savedInstanceState);
        recyclerView.setAdapter(adapter);
        if (getComponent().shouldAutoConnect()) {
            beginBindingData();
        }

        recyclerView.addItemDecoration(adapter.newDebugDecoration(recyclerView));

    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }


    public void beginBindingData() {
        getComponent().getDataConnection().connectWith(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.enableMovementDetection(recyclerView);
    }

    @Override
    protected void onStop() {
        adapter.disableMovementDetection(recyclerView);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        adapter.recycle();
        super.onDestroy();
    }
}
