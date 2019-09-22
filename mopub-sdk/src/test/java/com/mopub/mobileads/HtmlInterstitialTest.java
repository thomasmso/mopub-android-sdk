// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.TestHtmlInterstitialWebViewFactory;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowApplication;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.common.DataKeys.CLICKTHROUGH_URL_KEY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class HtmlInterstitialTest extends ResponseBodyInterstitialTest {
    private CustomEventInterstitialListener customEventInterstitialListener;
    private Activity context;
    private Map<String,Object> localExtras;
    private Map<String,String> serverExtras;
    private HtmlInterstitialWebView htmlInterstitialWebView;
    private String expectedResponse;
    private long broadcastIdentifier;

    @Before
    public void setUp() throws Exception {
        subject = new HtmlInterstitial();

        expectedResponse = "this is the response";
        htmlInterstitialWebView = TestHtmlInterstitialWebViewFactory.getSingletonMock();
        context = Robolectric.buildActivity(Activity.class).create().get();
        customEventInterstitialListener = mock(CustomEventInterstitialListener.class);
        localExtras = new HashMap<String, Object>();
        serverExtras = new HashMap<String, String>();
        serverExtras.put(HTML_RESPONSE_BODY_KEY, expectedResponse);

        broadcastIdentifier = 2222;
        localExtras.put(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
    }

    @Ignore("pending")
    @Test
    public void loadInterstitial_shouldNotifyCustomEventInterstitialListenerOnLoaded() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);

//        verify(customEventInterstitialListener).onInterstitialLoaded();
    }

    @Test
    public void loadInterstitial_whenNoHtmlResponsePassedIn_shouldCallLoadFailUrl() throws Exception {
        serverExtras.remove(HTML_RESPONSE_BODY_KEY);
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);

        assertThat(TestHtmlInterstitialWebViewFactory.getLatestListener()).isNull();
        assertThat(TestHtmlInterstitialWebViewFactory.getLatestClickthroughUrl()).isNull();
        verify(customEventInterstitialListener).onInterstitialFailed(NETWORK_INVALID_STATE);
        verify(htmlInterstitialWebView, never()).loadHtmlResponse(anyString());
    }

    @Test
    public void showInterstitial_withMinimumExtras_shouldStartMoPubActivityWithDefaults() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);
        subject.showInterstitial();

        Intent nextStartedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(nextStartedActivity.getStringExtra(CLICKTHROUGH_URL_KEY)).isNull();
        assertThat(nextStartedActivity.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(nextStartedActivity.getComponent().getClassName()).isEqualTo("com.mopub.mobileads.MoPubActivity");
    }

    @Test
    public void showInterstitial_shouldStartMoPubActivityWithAllExtras() throws Exception {
        serverExtras.put(CLICKTHROUGH_URL_KEY, "clickthroughUrl");

        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);
        subject.showInterstitial();

        Intent nextStartedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(nextStartedActivity.getStringExtra(CLICKTHROUGH_URL_KEY)).isEqualTo("clickthroughUrl");
        assertThat(nextStartedActivity.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(nextStartedActivity.getComponent().getClassName()).isEqualTo("com.mopub.mobileads.MoPubActivity");
    }

    @Test
    public void loadInterstitial_shouldConnectListenerToBroadcastReceiver() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);

        Intent intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, broadcastIdentifier);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener).onInterstitialShown();

        intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_DISMISS, broadcastIdentifier);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener).onInterstitialDismissed();
    }

    @Test
    public void onInvalidate_shouldDisconnectListenerToBroadcastReceiver() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);
        subject.onInvalidate();

        Intent intent;
        intent = new Intent(ACTION_INTERSTITIAL_SHOW);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener, never()).onInterstitialShown();

        intent = new Intent(ACTION_INTERSTITIAL_DISMISS);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener, never()).onInterstitialDismissed();
    }
}
