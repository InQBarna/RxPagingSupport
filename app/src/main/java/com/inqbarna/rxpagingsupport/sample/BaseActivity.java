package com.inqbarna.rxpagingsupport.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
public class BaseActivity extends AppCompatActivity {
    private GlobalsComponent component;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setComponent(DaggerGlobalsComponent.builder().dataModule(App.getDataModule(this)).build());
    }

    public GlobalsComponent getComponent() {
        return component;
    }

    public void setComponent(GlobalsComponent component) {
        this.component = component;
        onComponentChanged();
    }

    protected void onComponentChanged() {

    }
}
