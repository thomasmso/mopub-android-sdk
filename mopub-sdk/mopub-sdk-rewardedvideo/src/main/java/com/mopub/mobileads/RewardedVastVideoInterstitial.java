// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import androidx.annotation.Nullable;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;

class RewardedVastVideoInterstitial extends VastVideoInterstitial {
    public static final String ADAPTER_NAME = RewardedVastVideoInterstitial.class.getSimpleName();

    interface RewardedVideoInterstitialListener extends CustomEventInterstitialListener {
        void onVideoComplete();
    }

    @Nullable private RewardedVideoBroadcastReceiver mRewardedVideoBroadcastReceiver;

    @Override
    public void loadInterstitial(
            Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {
        MoPubLog.log(LOAD_ATTEMPTED, ADAPTER_NAME);
        super.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);

        if (customEventInterstitialListener instanceof RewardedVideoInterstitialListener) {
            mRewardedVideoBroadcastReceiver = new RewardedVideoBroadcastReceiver(
                    (RewardedVideoInterstitialListener) customEventInterstitialListener,
                    mBroadcastIdentifier);
            mRewardedVideoBroadcastReceiver.register(mRewardedVideoBroadcastReceiver, context);
        }
    }

    @Override
    public void onVastVideoConfigurationPrepared(final VastVideoConfig vastVideoConfig) {
        MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
        if (vastVideoConfig != null) {
            vastVideoConfig.setIsRewardedVideo(true);
        }
        super.onVastVideoConfigurationPrepared(vastVideoConfig);
    }

    @Override
    public void onInvalidate() {
        super.onInvalidate();
        if (mRewardedVideoBroadcastReceiver != null) {
            mRewardedVideoBroadcastReceiver.unregister(mRewardedVideoBroadcastReceiver);
        }
    }

    @VisibleForTesting
    @Deprecated
    @Nullable
    RewardedVideoBroadcastReceiver getRewardedVideoBroadcastReceiver() {
        return mRewardedVideoBroadcastReceiver;
    }
}
