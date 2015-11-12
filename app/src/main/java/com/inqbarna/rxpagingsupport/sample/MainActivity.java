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
