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
