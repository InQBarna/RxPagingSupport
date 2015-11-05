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
