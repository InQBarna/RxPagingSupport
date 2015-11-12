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

import android.app.Application;
import android.content.Context;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 5/11/15
 */
public class App extends Application {

    private DataModule dataModule;

    @Override
    public void onCreate() {
        super.onCreate();
        dataModule = createDataModule();
    }

    protected DataModule createDataModule() {
        return new DataModule();
    }

    public static App get(Context context) {
        return ((App) context.getApplicationContext());
    }

    public void setDataModule(DataModule dataModule) {
        this.dataModule = dataModule;
    }

    public static DataModule getDataModule(Context context) {
        return App.get(context).dataModule;
    }
}
