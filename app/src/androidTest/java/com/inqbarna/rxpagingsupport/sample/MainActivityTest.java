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

import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.DisableOnAndroidDebug;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.inqbarna.rxpagingsupport.Page;
import com.inqbarna.rxpagingsupport.PageManager;
import com.inqbarna.rxpagingsupport.RxPagedAdapter;
import com.inqbarna.rxpagingsupport.Settings;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest implements DataModuleRule.DataModuleClient {

    //    @Rule
    //    public InjectedActivityTestRule<MainActivity> activityRule = new InjectedActivityTestRule<MainActivity>(MainActivity.class);
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<MainActivity>(MainActivity.class, false, false);

    @Rule
    public TestRule rule = RuleChain.outerRule(new DisableOnAndroidDebug(new Timeout(20, TimeUnit.SECONDS)))
                                    .around(activityRule)
                                    .around(new DataModuleRule(this));

    TestDataSource testDataSource;

    @Mock
    PageManager<DataItem> pageManagerMock;

    @Captor
    ArgumentCaptor<RecyclerView.Adapter> adapterCaptor;

    @Captor
    ArgumentCaptor<Bundle> bundleCaptor;

    private GlobalsComponent component;


    @Override
    public void mockSetup() {
        MockitoAnnotations.initMocks(this);
        testDataSource = new TestDataSource();
        final TestDataModule dataModule = new TestDataModule(pageManagerMock, testDataSource.getNetSource(), testDataSource.getCacheManager());
        final App app = App.get(InstrumentationRegistry.getTargetContext());
        app.setDataModule(dataModule);

        when(pageManagerMock.getTotalCount()).thenReturn(50);
        when(pageManagerMock.isLastPageSeen()).thenReturn(true);
        doAnswer(itemAnswer(dataModule.provideRxSettings(dataModule.provideRxPageLogger()))).when(pageManagerMock).getItem(anyInt());
    }

    private Answer itemAnswer(final Settings settings) {
        return new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                int itemIdx = (int)invocationOnMock.getArguments()[0];
                return new DataItem(itemIdx % settings.getPageSize(), itemIdx);
            }
        };
    }

    @Override
    public void regularSetup() {
        testDataSource = new TestDataSource();
        final TestDataModule dataModule = new TestDataModule(null, testDataSource.getNetSource(), testDataSource.getCacheManager());
        App.get(InstrumentationRegistry.getTargetContext()).setDataModule(dataModule);
    }

    @Test
    @DataModuleRule.DataModuleMode(useMock = true)
    public void saveRestoreCalled() {
        activityRule.launchActivity(null);

        // screwed up...
        final MainActivity activity = activityRule.getActivity();
//        activity.per
    }

    @Test
    public void testLoadingShown() throws InterruptedException {
        activityRule.launchActivity(null);
        onView(withId(R.id.recycler)).check(ViewAssertions.matches(hasDescendant(allOf(withId(R.id.progress), isDisplayed()))));

        MainActivity activity = activityRule.getActivity();

        Settings settings = activity.getComponent().getRxSettings();
        final TestAsyncHelper helper = new TestAsyncHelper();
        helper.configureCountDown(settings.getPageSpan());

        TestDataSource dataConnection = testDataSource;
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
        checkDataOnAdapter(settings, adapter, 0);


        int decissionPageThreshold = (settings.getPageSpan() - 1) / 2;

        helper.configureCountDown(decissionPageThreshold);

        // scroll to last...
        int targetPos = adapter.getTotalCount();
        onView(withId(R.id.recycler)).perform(scrollToPosition(targetPos));

        countEnded = helper.awaitCountdown(4);
        assertThat(countEnded, is(true));

        //        InstrumentationRegistry.getInstrumentation().waitForIdle(new EmptyRunnable());

        assertThat(dataConnection.getNetworkPages(), is(settings.getPageSpan() + decissionPageThreshold));
        assertThat(dataConnection.getDiskPages(), is(0));
        assertThat(dataConnection.getCacheSize(), is(settings.getPageSpan() + decissionPageThreshold));
        checkDataOnAdapter(settings, adapter, decissionPageThreshold * settings.getPageSize());
        DataItem first = adapter.getItem(0);
        assertThat(first.getOwnerPage(), not(0)); // ensure we have not moved forward

    }


    private void checkDataOnAdapter(Settings settings, RxPagedAdapter<DataItem, ?> adapter, int firstExpectedIdx) {
        // ensure adapter has settings pages * size items
        assertThat(adapter.getTotalCount(), is(settings.getPageSize() * settings.getPageSpan()));

        // check they're ordered....
        List<DataItem> diList = new ArrayList<>();
        List<Matcher<? super DataItem>> orderMatcher = new ArrayList<>();
        for (int i = 0; i < adapter.getTotalCount(); i++) {
            diList.add(adapter.getItem(i));
            orderMatcher.add(dataItemAtIdx(firstExpectedIdx + i));
        }
        assertThat(diList, contains(orderMatcher));
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
