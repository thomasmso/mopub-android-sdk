// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.Nullable;

import com.mopub.common.AdReport;
import com.mopub.common.DataKeys;
import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.factories.HtmlBannerWebViewFactory;

import java.lang.ref.WeakReference;
import java.util.Map;

import static com.mopub.common.DataKeys.AD_REPORT_KEY;
import static com.mopub.common.DataKeys.BANNER_IMPRESSION_PIXEL_COUNT_ENABLED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_APPEAR;
import static com.mopub.mobileads.MoPubErrorCode.INTERNAL_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;

public class HtmlBanner extends CustomEventBanner {
    public static final String ADAPTER_NAME = HtmlBanner.class.getSimpleName();
    @Nullable private HtmlBannerWebView mHtmlBannerWebView;
    @Nullable private ExternalViewabilitySessionManager mExternalViewabilitySessionManager;
    private boolean mBannerImpressionPixelCountEnabled = false;
    @Nullable private WeakReference<Activity> mWeakActivity;

    @Override
    protected void loadBanner(
            Context context,
            CustomEventBannerListener customEventBannerListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {
        MoPubLog.log(LOAD_ATTEMPTED, ADAPTER_NAME);
        final Object bannerImpressionPixelCountEnabledObject = localExtras.get(
                BANNER_IMPRESSION_PIXEL_COUNT_ENABLED);
        if (bannerImpressionPixelCountEnabledObject instanceof Boolean) {
            mBannerImpressionPixelCountEnabled = (boolean) bannerImpressionPixelCountEnabledObject;
        }

        String htmlData;
        String clickthroughUrl;
        AdReport adReport;
        if (extrasAreValid(serverExtras)) {
            htmlData = serverExtras.get(DataKeys.HTML_RESPONSE_BODY_KEY);
            clickthroughUrl = serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY);

            try {
                adReport = (AdReport) localExtras.get(AD_REPORT_KEY);
            } catch (ClassCastException e) {
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                        INTERNAL_ERROR.getIntCode(),
                        INTERNAL_ERROR);
                customEventBannerListener.onBannerFailed(INTERNAL_ERROR);
                return;
            }
        } else {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    NETWORK_INVALID_STATE.getIntCode(),
                    NETWORK_INVALID_STATE);
            customEventBannerListener.onBannerFailed(NETWORK_INVALID_STATE);
            return;
        }

        mHtmlBannerWebView = HtmlBannerWebViewFactory.create(context, adReport, customEventBannerListener, clickthroughUrl);
        AdViewController.setShouldHonorServerDimensions(mHtmlBannerWebView);

        // We only measure viewability when we have an activity context. This sets up a delayed
        // viewability session if we have the new pixel-counting banner impression tracking enabled.
        // Otherwise, set up a regular display session.

        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);
        if (context instanceof Activity) {
            final Activity activity = (Activity) context;
            mWeakActivity = new WeakReference<>(activity);
            mExternalViewabilitySessionManager = new ExternalViewabilitySessionManager(activity);
            mExternalViewabilitySessionManager.createDisplaySession(activity, mHtmlBannerWebView,
                    mBannerImpressionPixelCountEnabled);
        } else {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Unable to start viewability session for HTML banner: Context provided was not an Activity.");
        }

        mHtmlBannerWebView.loadHtmlResponse(htmlData);
        MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
    }

    @Override
    protected void onInvalidate() {
        if (mExternalViewabilitySessionManager != null) {
            mExternalViewabilitySessionManager.endDisplaySession();
            mExternalViewabilitySessionManager = null;
        }

        if (mHtmlBannerWebView != null) {
            mHtmlBannerWebView.destroy();
            mHtmlBannerWebView = null;
        }
    }

    @Override
    protected void trackMpxAndThirdPartyImpressions() {
        if (mHtmlBannerWebView == null) {
            return;
        }

        mHtmlBannerWebView.loadUrl(WEB_VIEW_DID_APPEAR.getUrl());

        // mExternalViewabilitySessionManager is usually only null if the original Context given
        // to loadBanner() was not an Activity Context. We don't need to start the deferred
        // viewability tracker since it wasn't created, and if it was, and the activity reference
        // was lost, something bad has happened, so we should drop the request.
        if (mBannerImpressionPixelCountEnabled && mExternalViewabilitySessionManager != null &&
                mWeakActivity != null) {
            final Activity activity = mWeakActivity.get();
            if (activity != null) {
                mExternalViewabilitySessionManager.startDeferredDisplaySession(activity);
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Lost the activity for deferred Viewability tracking. Dropping session.");
            }
        }
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(DataKeys.HTML_RESPONSE_BODY_KEY);
    }

    @VisibleForTesting
    boolean isBannerImpressionPixelCountEnabled() {
        return mBannerImpressionPixelCountEnabled;
    }
}
