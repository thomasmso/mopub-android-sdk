// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class NetworkingTest {
    private Activity context;

    @Before
    public void setUp() {
        context = Robolectric.buildActivity(Activity.class).create().get();
    }

    @After
    public void tearDown() {
        Networking.clearForTesting();
    }

    @Test
    public void getUserAgent_usesCachedUserAgent() {
        Networking.setUserAgentForTesting("some cached user agent");
        String userAgent = Networking.getUserAgent(context);

        assertThat(userAgent).isEqualTo("some cached user agent");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Config(sdk = Build.VERSION_CODES.JELLY_BEAN)
    @Test
    public void getUserAgent_withSdkVersion16_shouldIncludeAndroid() {
        String userAgent = Networking.getUserAgent(context);

        assertThat(userAgent).containsIgnoringCase("android");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Config(sdk = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void getUserAgent_withSdkVersionGreaterThan16_shouldIncludeAndroid() {
        String userAgent = Networking.getUserAgent(context);

        assertThat(userAgent).containsIgnoringCase("android");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Config(sdk = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void getUserAgent_withSdkVersionGreaterThan16_whenOnABackgroundThread_shouldReturnHttpAgent() throws InterruptedException {
        final String[] userAgent = new String[1];
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread() {
            @Override
            public void run() {
                userAgent[0] = Networking.getUserAgent(context);

                latch.countDown();
            }
        }.start();

        latch.await(500, TimeUnit.MILLISECONDS);
        // Robolectric's default http agent is null which gets rewritten to an empty String.
        assertThat(userAgent[0]).isEqualTo("");

    }

    @Test
    public void getCachedUserAgent_usesCachedUserAgent() {
        Networking.setUserAgentForTesting("some cached user agent");
        String userAgent = Networking.getCachedUserAgent();

        assertThat(userAgent).isEqualTo("some cached user agent");
    }
}
