package com.inqbarna.rxpagingsupport.sample;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.inqbarna.rxpagingsupport.Settings;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RxPaging";

    @Bind(R.id.recycler)
    RecyclerView recyclerView;

    private TestAdapter adapter;

    private Settings.Logger logger = new Settings.Logger() {
        @Override
        public void error(@NonNull String msg, @Nullable Throwable throwable) {
            Log.e(TAG, msg, throwable);
        }

        @Override
        public void info(@NonNull String msg, @Nullable Throwable throwable) {
            Log.i(TAG, msg, throwable);
        }

        @Override
        public void debug(@NonNull String msg, @Nullable Throwable throwable) {
            Log.d(TAG, msg, throwable);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TestAdapter(Settings.builder().setLogger(logger).build(), savedInstanceState);
        recyclerView.setAdapter(adapter);
    }
}
