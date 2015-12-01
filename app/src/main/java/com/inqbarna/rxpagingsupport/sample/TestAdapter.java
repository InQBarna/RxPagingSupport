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
    protected void doBindViewHolder(TestHolder holder, DataItem item, int position) {
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
