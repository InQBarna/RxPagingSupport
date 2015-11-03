package com.inqbarna.rxpagingsupport.sample;

import android.app.Activity;
import android.support.test.rule.ActivityTestRule;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
public class InjectedActivityTestRule<T extends Activity> extends ActivityTestRule<T> {

    private GlobalsComponent component;

    public InjectedActivityTestRule(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    protected void afterActivityLaunched() {
        super.afterActivityLaunched();

        component = DaggerGlobalsComponent.builder().dataModule(new TestDataModule()).build();

        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).setComponent(component);
        }
    }

    public GlobalsComponent getComponent() {
        return component;
    }
}
