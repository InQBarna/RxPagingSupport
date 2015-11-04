package com.inqbarna.rxpagingsupport.sample;

import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.inqbarna.rxpagingsupport.Page;
import com.inqbarna.rxpagingsupport.Settings;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public InjectedActivityTestRule<MainActivity> activityRule = new InjectedActivityTestRule<MainActivity>(MainActivity.class);

//    @Test
    public void simpleTest() {
        onView(withId(R.id.recycler)).check(ViewAssertions.matches(hasDescendant(allOf(withId(R.id.progress), isDisplayed()))));
    }

    @Test
    public void testLoadingShown() throws InterruptedException {

        onView(withId(R.id.recycler)).check(ViewAssertions.matches(hasDescendant(allOf(withId(R.id.progress), isDisplayed()))));

        Settings settings = activityRule.getComponent().getRxSettings();
        final TestAsyncHelper helper = new TestAsyncHelper();
        helper.configureCountDown(settings.getPageSpan());

        TestDataSource dataConnection = activityRule.getDataConnection();
        dataConnection.setDataSourceListener(
                new TestDataSource.DataSourceListener() {
                    @Override
                    public void onNewNetworkPage(Page<DataItem> dataItemPage, int networkPages) {
                        Log.d("TEST", "generated new network page (total): " + networkPages);
                        helper.countDown();
                    }

                    @Override
                    public void onNewDiskPage(Page<DataItem> dataItemPage, int diskPages) {

                    }
                }
        );


        activityRule.getActivity().beginBindingData();

        boolean countEnded = helper.awaitCountdown(4);
        assertThat(countEnded, is(true));

        assertThat(dataConnection.getNetworkPages(), is(settings.getPageSpan()));
        onView(withId(R.id.recycler)).check(ViewAssertions.matches(not(hasDescendant(allOf(withId(R.id.progress), isDisplayed())))));
        assertThat(dataConnection.getDiskPages(), is(0));
    }
}
