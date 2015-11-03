package com.inqbarna.rxpagingsupport.sample;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.not;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public InjectedActivityTestRule<MainActivity> activityRule = new InjectedActivityTestRule<MainActivity>(MainActivity.class);


    @Test
    public void testLoadingShown() {
        activityRule.launchActivity(new Intent(InstrumentationRegistry.getTargetContext(), MainActivity.class));
        onView(withId(R.id.recycler)).check(ViewAssertions.matches(hasDescendant(allOf(withId(R.id.progress), isDisplayed()))));
    }

    @Test
    public void loadFirstPagesHidesProgress() throws InterruptedException {
        activityRule.launchActivity(new Intent(InstrumentationRegistry.getTargetContext(), MainActivity.class));
        activityRule.getActivity().beginBindingData();

        Thread.sleep(2000);

        onView(withId(R.id.recycler)).check(ViewAssertions.matches(not(hasDescendant(allOf(withId(R.id.progress), isDisplayed())))));
    }
}
