// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.mopub.common.AdReport;
import com.mopub.common.Constants;
import com.mopub.common.DataKeys;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.ReflectionTarget;
import com.mopub.mobileads.factories.CustomEventBannerFactory;

import java.util.Map;
import java.util.TreeMap;

import static com.mopub.common.DataKeys.AD_HEIGHT;
import static com.mopub.common.DataKeys.AD_REPORT_KEY;
import static com.mopub.common.DataKeys.AD_WIDTH;
import static com.mopub.common.DataKeys.BANNER_IMPRESSION_PIXEL_COUNT_ENABLED;
import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_NOT_FOUND;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;

public class CustomEventBannerAdapter implements InternalCustomEventBannerListener {
    public static final int DEFAULT_BANNER_TIMEOUT_DELAY = Constants.TEN_SECONDS_MILLIS;

    private boolean mInvalidated;
    private MoPubView mMoPubView;
    private Context mContext;
    @Nullable private CustomEventBanner mCustomEventBanner;
    private Map<String, Object> mLocalExtras;
    private Map<String, String> mServerExtras;

    private final Handler mHandler;
    private final Runnable mTimeout;

    private int mImpressionMinVisibleDips = Integer.MIN_VALUE;
    private int mImpressionMinVisibleMs = Integer.MIN_VALUE;
    private boolean mIsVisibilityImpressionTrackingEnabled = false;
    @Nullable private BannerVisibilityTracker mVisibilityTracker;

    public CustomEventBannerAdapter(@NonNull MoPubView moPubView,
            @NonNull String className,
            @NonNull Map<String, String> serverExtras,
            long broadcastIdentifier,
            @Nullable AdReport adReport) {
        Preconditions.checkNotNull(serverExtras);
        mHandler = new Handler();
        mMoPubView = moPubView;
        mContext = moPubView.getContext();
        mTimeout = new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(CUSTOM, "CustomEventBannerAdapter failed with code " +
                        NETWORK_TIMEOUT.getIntCode() + " and message " +
                        NETWORK_TIMEOUT);
                onBannerFailed(NETWORK_TIMEOUT);
                invalidate();
            }
        };

        MoPubLog.log(CUSTOM,  "Attempting to invoke custom event: " + className);
        try {
            mCustomEventBanner = CustomEventBannerFactory.create(className);
        } catch (Exception exception) {
            MoPubLog.log(CUSTOM,  "Couldn't locate or instantiate custom event: " + className + ".");
            mMoPubView.loadFailUrl(ADAPTER_NOT_FOUND);
            return;
        }

        // Attempt to load the JSON extras into mServerExtras.
        mServerExtras = new TreeMap<>(serverExtras);

        // Parse banner impression tracking headers to determine if we are in visibility experiment
        parseBannerImpressionTrackingHeaders();

        mLocalExtras = mMoPubView.getLocalExtras();
        if (mMoPubView.getLocation() != null) {
            mLocalExtras.put("location", mMoPubView.getLocation());
        }
        mLocalExtras.put(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
        mLocalExtras.put(AD_REPORT_KEY, adReport);
        mLocalExtras.put(AD_WIDTH, mMoPubView.getAdWidth());
        mLocalExtras.put(AD_HEIGHT, mMoPubView.getAdHeight());
        mLocalExtras.put(BANNER_IMPRESSION_PIXEL_COUNT_ENABLED, mIsVisibilityImpressionTrackingEnabled);
    }

    @ReflectionTarget
    void loadAd() {
        if (isInvalidated() || mCustomEventBanner == null) {
            return;
        }

        mHandler.postDelayed(mTimeout, getTimeoutDelayMilliseconds());

        // Custom event classes can be developed by any third party and may not be tested.
        // We catch all exceptions here to prevent crashes from untested code.
        try {
            mCustomEventBanner.loadBanner(mContext, this, mLocalExtras, mServerExtras);
        } catch (Exception e) {
            MoPubLog.log(CUSTOM, "loadAd() failed with code " +
                    MoPubErrorCode.INTERNAL_ERROR.getIntCode() + " and message " +
                    MoPubErrorCode.INTERNAL_ERROR);
            onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
        }
    }

    @ReflectionTarget
    void invalidate() {
        if (mCustomEventBanner != null) {
            // Custom event classes can be developed by any third party and may not be tested.
            // We catch all exceptions here to prevent crashes from untested code.
            try {
                mCustomEventBanner.onInvalidate();
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE,  "Invalidating a custom event banner threw an exception", e);
            }
        }
        if (mVisibilityTracker != null) {
            try {
                mVisibilityTracker.destroy();
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE,  "Destroying a banner visibility tracker threw an exception", e);
            }
            mVisibilityTracker = null;
        }
        mContext = null;
        mCustomEventBanner = null;
        mLocalExtras = null;
        mServerExtras = null;
        mInvalidated = true;
    }

    boolean isInvalidated() {
        return mInvalidated;
    }

    @Deprecated
    @VisibleForTesting
    int getImpressionMinVisibleDips() {
        return mImpressionMinVisibleDips;
    }

    @Deprecated
    @VisibleForTesting
    int getImpressionMinVisibleMs() {
        return mImpressionMinVisibleMs;
    }

    @Deprecated
    @VisibleForTesting
    boolean isVisibilityImpressionTrackingEnabled() {
        return mIsVisibilityImpressionTrackingEnabled;
    }

    @Nullable
    @Deprecated
    @VisibleForTesting
    BannerVisibilityTracker getVisibilityTracker() {
        return mVisibilityTracker;
    }

    private void cancelTimeout() {
        mHandler.removeCallbacks(mTimeout);
    }

    private int getTimeoutDelayMilliseconds() {
        if (mMoPubView == null) {
            return DEFAULT_BANNER_TIMEOUT_DELAY;
        }

        return mMoPubView.getAdTimeoutDelay(DEFAULT_BANNER_TIMEOUT_DELAY);
    }

    private void parseBannerImpressionTrackingHeaders() {
        final String impressionMinVisibleDipsString =
                mServerExtras.get(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS);
        final String impressionMinVisibleMsString =
                mServerExtras.get(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS);

        if (!TextUtils.isEmpty(impressionMinVisibleDipsString)
                && !TextUtils.isEmpty(impressionMinVisibleMsString)) {
            try {
                mImpressionMinVisibleDips = Integer.parseInt(impressionMinVisibleDipsString);
            } catch (NumberFormatException e) {
                MoPubLog.log(CUSTOM,  "Cannot parse integer from header "
                        + DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS);
            }

            try {
                mImpressionMinVisibleMs = Integer.parseInt(impressionMinVisibleMsString);
            } catch (NumberFormatException e) {
                MoPubLog.log(CUSTOM,  "Cannot parse integer from header "
                        + DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS);
            }

            if (mImpressionMinVisibleDips > 0 && mImpressionMinVisibleMs >= 0) {
                    mIsVisibilityImpressionTrackingEnabled = true;
            }
        }
    }

    /*
     * CustomEventBanner.Listener implementation
     */
    @Override
    public void onBannerLoaded(View bannerView) {
        if (isInvalidated()) {
            return;
        }

        MoPubLog.log(CUSTOM, "onBannerLoaded() success. Attempting to show.");

        cancelTimeout();

        if (mMoPubView != null) {
            mMoPubView.creativeDownloaded();

            // If visibility impression tracking is enabled for banners, fire all impression
            // tracking URLs (AdServer, MPX, 3rd-party) for both HTML and MRAID banner types when
            // visibility conditions are met.
            //
            // Else, retain old behavior of firing AdServer impression tracking URL if and only if
            // banner is not HTML.
            if (mIsVisibilityImpressionTrackingEnabled &&
                    mCustomEventBanner != null &&
                    mCustomEventBanner.isAutomaticImpressionAndClickTrackingEnabled()) {
                // Disable autoRefresh temporarily until an impression happens.
                mMoPubView.pauseAutoRefresh();
                // Set up visibility tracker and listener if in experiment
                mVisibilityTracker = new BannerVisibilityTracker(mContext, mMoPubView, bannerView,
                        mImpressionMinVisibleDips, mImpressionMinVisibleMs);
                mVisibilityTracker.setBannerVisibilityTrackerListener(
                        new BannerVisibilityTracker.BannerVisibilityTrackerListener() {
                    @Override
                    public void onVisibilityChanged() {
                        mMoPubView.trackNativeImpression();
                        if (mCustomEventBanner != null) {
                            mCustomEventBanner.trackMpxAndThirdPartyImpressions();
                        }
                        mMoPubView.resumeAutoRefresh();
                    }
                });
            }

            mMoPubView.setAdContentView(bannerView);

            // Old behavior
            if (!mIsVisibilityImpressionTrackingEnabled &&
                    mCustomEventBanner != null &&
                    mCustomEventBanner.isAutomaticImpressionAndClickTrackingEnabled()) {
                if (!(bannerView instanceof HtmlBannerWebView)) {
                    mMoPubView.trackNativeImpression();
                }
            }

            MoPubLog.log(CUSTOM, "onBannerLoaded() - Show successful.");
        } else {
            MoPubLog.log(CUSTOM, "onBannerLoaded() - Show failed with code " +
                    MoPubErrorCode.INTERNAL_ERROR.getIntCode() + " and message " +
                    MoPubErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public void onBannerFailed(MoPubErrorCode errorCode) {
        if (isInvalidated()) {
            return;
        }

        cancelTimeout();

        if (mMoPubView != null) {
            if (errorCode == null) {
                errorCode = UNSPECIFIED;
            }
            mMoPubView.loadFailUrl(errorCode);
        }
    }

    @Override
    public void onBannerExpanded() {
        if (isInvalidated()) {
            return;
        }

        mMoPubView.engageOverlay();
        mMoPubView.adPresentedOverlay();
    }

    @Override
    public void onBannerCollapsed() {
        if (isInvalidated()) {
            return;
        }

        mMoPubView.dismissOverlay();
        mMoPubView.adClosed();
    }

    @Override
    public void onBannerClicked() {
        if (isInvalidated()) {
            return;
        }

        if (mMoPubView != null) {
            mMoPubView.registerClick();
        }
    }

    @Override
    public void onBannerImpression() {
        if (isInvalidated()) {
            return;
        }

        if (mMoPubView != null &&
                mCustomEventBanner != null &&
                !mCustomEventBanner.isAutomaticImpressionAndClickTrackingEnabled()) {
            mMoPubView.trackNativeImpression();
            if (mIsVisibilityImpressionTrackingEnabled) {
                mCustomEventBanner.trackMpxAndThirdPartyImpressions();
            }
        }
    }

    @Override
    public void onLeaveApplication() {
        onBannerClicked();
    }

    @Override
    public void onPauseAutoRefresh() {
        if (mMoPubView != null) {
            mMoPubView.engageOverlay();
        }
    }

    @Override
    public void onResumeAutoRefresh() {
        if (mMoPubView != null) {
            mMoPubView.dismissOverlay();
        }
    }
}
