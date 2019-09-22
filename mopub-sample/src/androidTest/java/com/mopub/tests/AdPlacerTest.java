package com.mopub.tests;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.mopub.framework.models.AdLabels;
import com.mopub.framework.pages.AdDetailPage;
import com.mopub.simpleadsdemo.R;
import com.mopub.tests.base.MoPubBaseTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdPlacerTest extends MoPubBaseTestCase {

    // Test Variables
    private static final String TITLE = AdLabels.AD_PLACER;
    private static final String WEB_PAGE_LINK = "https://www.mopub.com/click-test/";

    /*
     * Verify that the Rewarded Video Ad loads & shows on the app.
     */
    @Test
    public void adsListPage_withClickOnMoPubAdPlacerSample_shouldLoadMoPubAdPlacerAd() {
        onData(hasToString(startsWith(TITLE)))
                .inAdapterView(withId(android.R.id.list))
                .perform(click());

        final AdDetailPage adDetailPage = new AdDetailPage();

        final ViewInteraction element = onView(allOf(withId(R.id.native_main_image)));

        assertTrue(adDetailPage.waitForElement(element));
    }

    /*
     * Verify that the user is correctly navigated to MoPub browser.
     */
    @Test
    public void adsDetailsPage_withClickOnAd_shouldShowMoPubBrowser() {
        onData(hasToString(startsWith(TITLE)))
                .inAdapterView(withId(android.R.id.list))
                .perform(click());

        final AdDetailPage adDetailPage = new AdDetailPage();

        ViewInteraction nativeMainImageElement = onView(allOf(withId(R.id.native_main_image))); //show MoPub browser on Ad click
        adDetailPage.clickElement(nativeMainImageElement);

        final ViewInteraction browserLinkElement = onView(withText(WEB_PAGE_LINK));

        assertTrue(adDetailPage.waitForElement(browserLinkElement));
    }
}
