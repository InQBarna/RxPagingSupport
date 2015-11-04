package com.inqbarna.rxpagingsupport.sample;

import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.inqbarna.rxpagingsupport.Page;
import com.inqbarna.rxpagingsupport.RxPagedAdapter;
import com.inqbarna.rxpagingsupport.Settings;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
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
                        Log.d("TEST", "generated new disk page (total): " + diskPages);
                        helper.countDown();
                    }
                }
        );


        activityRule.getActivity().beginBindingData();

        boolean countEnded = helper.awaitCountdown(4);
        assertThat(countEnded, is(true));

        assertThat(dataConnection.getNetworkPages(), is(settings.getPageSpan()));
        onView(withId(R.id.recycler)).check(ViewAssertions.matches(not(hasDescendant(allOf(withId(R.id.progress), isDisplayed())))));
        assertThat(dataConnection.getDiskPages(), is(0));
        assertThat(dataConnection.getCacheSize(), is(settings.getPageSpan()));

        assertThat(activityRule.getActivity().recyclerView.getAdapter(), instanceOf(RxPagedAdapter.class));
        RxPagedAdapter<DataItem, ?> adapter = (RxPagedAdapter<DataItem, ?>) activityRule.getActivity().recyclerView.getAdapter();
        checkDataOnAdapter(settings, adapter);


        /*int decissionPageThreshold = (settings.getPageSpan() - 1) / 2;
        if (decissionPageThreshold > 1) {
            // scroll not enough pages to make requests...
            onView(withId(R.id.recycler))
                    .perform(scrollToPosition((decissionPageThreshold - 1) * settings.getPageSize()));

            Thread.sleep(2000);
            checkDataOnAdapter(settings, adapter);

            DataItem first = adapter.getItem(0);
            assertThat(first.getOwnerPage(), is(0)); // ensure we have not moved forward
        }*/

        helper.configureCountDown(1);
        // scroll one more page forward...
//        int targetPos = decissionPageThreshold * settings.getPageSize();
        int targetPos = adapter.getTotalCount();
        onView(withId(R.id.recycler)).perform(scrollToPosition(targetPos));
        countEnded = helper.awaitCountdown(4);
        assertThat(countEnded, is(true));

        assertThat(dataConnection.getNetworkPages(), is(settings.getPageSpan() + 1));
        assertThat(dataConnection.getDiskPages(), is(0));
        assertThat(dataConnection.getCacheSize(), is(settings.getPageSpan() + 1));
        checkDataOnAdapter(settings, adapter);
        DataItem first = adapter.getItem(0);
        assertThat(first.getOwnerPage(), not(0)); // ensure we have not moved forward

    }

    private void checkDataOnAdapter(Settings settings, RxPagedAdapter<DataItem, ?> adapter) {
        // ensure adapter has settings pages * size items
        assertThat(adapter.getTotalCount(), is(settings.getPageSize() * settings.getPageSpan()));

        // check they're ordered....
        List<DataItem> diList = new ArrayList<>();
        List<Matcher<? super DataItem>> orderMather = new ArrayList<>();
        for (int i = 0; i < adapter.getTotalCount(); i++) {
            diList.add(adapter.getItem(i));
            orderMather.add(dataItemAtIdx(i));
        }
        assertThat(diList, contains(orderMather));
    }

    private static Matcher<DataItem> dataItemAtIdx(final int idx) {
        return new CustomTypeSafeMatcher<DataItem>("DataItem with idx: " + idx) {
            @Override
            protected boolean matchesSafely(DataItem item) {
                return idx == item.getAbsIdx();
            }
        };
    }
}
