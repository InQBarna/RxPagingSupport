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

import android.app.Activity;
import android.support.test.rule.ActivityTestRule;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
public class InjectedActivityTestRule<T extends Activity> extends ActivityTestRule<T> {
    public InjectedActivityTestRule(Class<T> activityClass) {
        super(activityClass);
    }
    /* private GlobalsComponent component;
    private TestDataSource dataConnection;


    public InjectedActivityTestRule(Class<T> activityClass) {
        super(activityClass);
        this.dataConnection = new TestDataSource();
    }

    @Override
    protected void afterActivityLaunched() {

        component = DaggerGlobalsComponent.builder().dataModule(new TestDataModule(dataConnection.getNetSource(), dataConnection.getCacheManager())).build();

        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).setComponent(component);
        }
    }

    public GlobalsComponent getComponent() {
        return component;
    }

    public TestDataSource getDataConnection() {
        return dataConnection;
    }*/
}
