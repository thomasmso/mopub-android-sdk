// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.os.Handler;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mraid.RewardedMraidController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class RewardedMraidCountdownRunnableTest {

    @Mock
    RewardedMraidController mockRewardedMraidController;
    @Mock Handler mockHandler;

    RewardedMraidCountdownRunnable subject;

    @Before
    public void setup() {
        subject = new RewardedMraidCountdownRunnable(mockRewardedMraidController, mockHandler);
    }

    @Test
    public void doWork_whenPlayableIsCloseable_shouldMakePlayableCloseable() {
        when(mockRewardedMraidController.isPlayableCloseable()).thenReturn(true);

        subject.doWork();

        int currentElapsedTimeMillis = subject.getCurrentElapsedTimeMillis();
        verify(mockRewardedMraidController).updateCountdown(currentElapsedTimeMillis);
        verify(mockRewardedMraidController).showPlayableCloseButton();
    }

    @Test
    public void doWork_whenPlayableIsNotCloseable_shouldNotMakePlayableCloseable() {
        when(mockRewardedMraidController.isPlayableCloseable()).thenReturn(false);

        subject.doWork();

        int currentElapsedTimeMillis = subject.getCurrentElapsedTimeMillis();
        verify(mockRewardedMraidController).updateCountdown(currentElapsedTimeMillis);
        verify(mockRewardedMraidController, never()).showPlayableCloseButton();
    }
}
