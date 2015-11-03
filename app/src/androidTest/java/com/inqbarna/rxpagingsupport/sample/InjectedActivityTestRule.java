package com.inqbarna.rxpagingsupport.sample;

import android.app.Activity;
import android.support.test.rule.ActivityTestRule;

import com.inqbarna.rxpagingsupport.RxDataConnection;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
public class InjectedActivityTestRule<T extends Activity> extends ActivityTestRule<T> {

    private GlobalsComponent component;
    private TestDataSource dataConnection;


    public InjectedActivityTestRule(Class<T> activityClass) {
        super(activityClass);
        this.dataConnection = new TestDataSource();
    }

    @Override
    protected void afterActivityLaunched() {

//        component = DaggerGlobalsComponent.builder().dataModule(new TestDataModule(dataConnection)).build();
//
//        if (getActivity() instanceof BaseActivity) {
//            ((BaseActivity) getActivity()).setComponent(component);
//        }
    }

    public GlobalsComponent getComponent() {
        return component;
    }

    public TestDataSource getDataConnection() {
        return dataConnection;
    }
}
