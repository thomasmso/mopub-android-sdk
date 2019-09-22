// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowInsets;

import com.mopub.common.AdFormat;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DeviceUtils;
import com.mopub.mobileads.factories.CustomEventInterstitialAdapterFactory;

import java.util.Map;

import static com.mopub.common.Constants.AD_EXPIRATION_DELAY;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.WILL_DISAPPEAR;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_NOT_FOUND;
import static com.mopub.mobileads.MoPubErrorCode.EXPIRED;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.DESTROYED;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.IDLE;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.LOADING;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.READY;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.SHOWING;

public class MoPubInterstitial implements CustomEventInterstitialAdapter.CustomEventInterstitialAdapterListener {
    @VisibleForTesting
    enum InterstitialState {
        /**
         * Waiting to something to happen. There is no interstitial currently loaded.
         */
        IDLE,

        /**
         * Loading an interstitial.
         */
        LOADING,

        /**
         * Loaded and ready to be shown.
         */
        READY,

        /**
         * The interstitial is showing.
         */
        SHOWING,

        /**
         * No longer able to accept events as the internal InterstitialView has been destroyed.
         */
        DESTROYED
    }

    @NonNull private MoPubInterstitialView mInterstitialView;
    @Nullable private CustomEventInterstitialAdapter mCustomEventInterstitialAdapter;
    @Nullable private InterstitialAdListener mInterstitialAdListener;
    @NonNull private Activity mActivity;
    @NonNull private Handler mHandler;
    @NonNull private final Runnable mAdExpiration;
    @NonNull private volatile InterstitialState mCurrentInterstitialState;

    public interface InterstitialAdListener {
        void onInterstitialLoaded(MoPubInterstitial interstitial);
        void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode);
        void onInterstitialShown(MoPubInterstitial interstitial);
        void onInterstitialClicked(MoPubInterstitial interstitial);
        void onInterstitialDismissed(MoPubInterstitial interstitial);
    }

    public MoPubInterstitial(@NonNull final Activity activity, @NonNull final String adUnitId) {
        mActivity = activity;

        mInterstitialView = new MoPubInterstitialView(mActivity);
        mInterstitialView.setAdUnitId(adUnitId);

        mCurrentInterstitialState = IDLE;

        mHandler = new Handler();
        mAdExpiration = new Runnable() {
            @Override
            public void run() {
                MoPubLog.log(CUSTOM, "Expiring unused Interstitial ad.");
                attemptStateTransition(IDLE, true);
                if (!SHOWING.equals(mCurrentInterstitialState) &&
                        !DESTROYED.equals(mCurrentInterstitialState)) {
                    // double-check the state in case the runnable fires right after the state
                    // transition but before it's cancelled
                    mInterstitialView.adFailed(EXPIRED);
                }
            }
        };
    }

    private boolean attemptStateTransition(@NonNull final InterstitialState endState) {
        return attemptStateTransition(endState, false);
    }

    /**
     * Attempts to transition to the new state. All state transitions should go through this method.
     * Other methods should not be modifying mCurrentInterstitialState.
     *
     * @param endState     The desired end state.
     * @param force Whether or not this is part of a force transition. Force transitions
     *                     can happen from IDLE, LOADING, or READY. It will ignore
     *                     the currently loading or loaded ad and attempt to load another.
     * @return {@code true} if a state change happened, {@code false} if no state change happened.
     */
    @VisibleForTesting
    synchronized boolean attemptStateTransition(@NonNull final InterstitialState endState,
            boolean force) {
        Preconditions.checkNotNull(endState);

        final InterstitialState startState = mCurrentInterstitialState;

        /**
         * There are 50 potential cases. Any combination that is a no op will not be enumerated
         * and returns false. The usual case goes IDLE -> LOADING -> READY -> SHOWING -> IDLE. At
         * most points, having the force refresh flag into IDLE resets MoPubInterstitial and clears
         * the interstitial adapter. This cannot happen while an interstitial is showing. Also,
         * MoPubInterstitial can be destroyed arbitrarily, and once this is destroyed, it no longer
         * can perform any state transitions.
         */
        switch (startState) {
            case IDLE:
                switch(endState) {
                    case LOADING:
                        // Going from IDLE to LOADING is the usual load case
                        invalidateInterstitialAdapter();
                        mCurrentInterstitialState = LOADING;
                        updatedInsets();
                        if (force) {
                            // Force-load means a pub-initiated force refresh.
                            mInterstitialView.forceRefresh();
                        } else {
                            // Otherwise, do a normal load
                            mInterstitialView.loadAd();
                        }
                        return true;
                    case READY:
                        MoPubLog.log(CUSTOM, "Attempted transition from IDLE to " +
                                "READY failed due to no known load call.");
                        return false;
                    case SHOWING:
                        MoPubLog.log(CUSTOM, "No interstitial loading or loaded.");
                        return false;
                    case DESTROYED:
                        setInterstitialStateDestroyed();
                        return true;
                    default:
                        return false;
                }
            case LOADING:
                switch (endState) {
                    case IDLE:
                        // Being forced back into idle while loading resets MoPubInterstitial while
                        // not forced just means the load failed. Either way, it should reset the
                        // state back into IDLE.
                        invalidateInterstitialAdapter();
                        mCurrentInterstitialState = IDLE;
                        return true;
                    case LOADING:
                        if (!force) {
                            // Cannot load more than one interstitial at a time
                            MoPubLog.log(CUSTOM, "Already loading an interstitial.");
                        }
                        return false;
                    case READY:
                        // This is the usual load finished transition
                        MoPubLog.log(LOAD_SUCCESS);
                        mCurrentInterstitialState = READY;
                        // Expire MoPub ads to synchronize with MoPub Ad Server tracking window
                        if (AdTypeTranslator.CustomEventType
                                .isMoPubSpecific(mInterstitialView.getCustomEventClassName())) {
                            mHandler.postDelayed(mAdExpiration, AD_EXPIRATION_DELAY);
                        }
                        if (mInterstitialView.mAdViewController != null) {
                            mInterstitialView.mAdViewController.creativeDownloadSuccess();
                        }
                        if (mInterstitialAdListener != null) {
                            mInterstitialAdListener.onInterstitialLoaded(this);
                        }
                        return true;
                    case SHOWING:
                        MoPubLog.log(CUSTOM, "Interstitial is not ready to be shown yet.");
                        return false;
                    case DESTROYED:
                        setInterstitialStateDestroyed();
                        return true;
                    default:
                        return false;
                }
            case READY:
                switch (endState) {
                    case IDLE:
                        if (force) {
                            // This happens on a force refresh or an ad expiration
                            invalidateInterstitialAdapter();
                            mCurrentInterstitialState = IDLE;
                            return true;
                        }
                        return false;
                    case LOADING:
                        // This is to prevent loading another interstitial while one is loaded.
                        MoPubLog.log(CUSTOM, "Interstitial already loaded. Not loading another.");
                        // Let the ad listener know that there's already an ad loaded
                        if (mInterstitialAdListener != null) {
                            mInterstitialAdListener.onInterstitialLoaded(this);
                        }
                        return false;
                    case SHOWING:
                        // This is the usual transition from ready to showing
                        showCustomEventInterstitial();
                        mCurrentInterstitialState = SHOWING;
                        mHandler.removeCallbacks(mAdExpiration);
                        return true;
                    case DESTROYED:
                        setInterstitialStateDestroyed();
                        return true;
                    default:
                        return false;
                }
            case SHOWING:
                switch(endState) {
                    case IDLE:
                        if (force) {
                            MoPubLog.log(CUSTOM, "Cannot force refresh while showing an interstitial.");
                            return false;
                        }
                        // This is the usual transition when done showing this interstitial
                        invalidateInterstitialAdapter();
                        mCurrentInterstitialState = IDLE;
                        return true;
                    case LOADING:
                        if (!force) {
                            MoPubLog.log(CUSTOM, "Interstitial already showing. Not loading another.");
                        }
                        return false;
                    case SHOWING:
                        MoPubLog.log(CUSTOM, "Already showing an interstitial. Cannot show it again.");
                        return false;
                    case DESTROYED:
                        setInterstitialStateDestroyed();
                        return true;
                    default:
                        return false;
                }
            case DESTROYED:
                // Once destroyed, MoPubInterstitial is no longer functional.
                MoPubLog.log(CUSTOM, "MoPubInterstitial destroyed. Ignoring all requests.");
                return false;
            default:
                return false;
        }
    }

    /**
     * Sets MoPubInterstitial to be destroyed. This should only be called by attemptStateTransition.
     */
    private void setInterstitialStateDestroyed() {
        invalidateInterstitialAdapter();
        mInterstitialAdListener = null;
        mInterstitialView.setBannerAdListener(null);
        mInterstitialView.destroy();
        mHandler.removeCallbacks(mAdExpiration);
        mCurrentInterstitialState = DESTROYED;
    }

    private void updatedInsets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            final Window window = mActivity.getWindow();
            if (window == null) {
                return;
            }
            final WindowInsets insets = window.getDecorView().getRootWindowInsets();
            if (insets == null) {
                return;
            }
            mInterstitialView.setWindowInsets(insets);
        }
    }

    public void load() {
        MoPubLog.log(LOAD_ATTEMPTED);
        attemptStateTransition(LOADING);
    }

    public boolean show() {
        MoPubLog.log(SHOW_ATTEMPTED);
        return attemptStateTransition(SHOWING);
    }

    public void forceRefresh() {
        attemptStateTransition(IDLE, true);
        attemptStateTransition(LOADING, true);
    }

    public boolean isReady() {
        return mCurrentInterstitialState == READY;
    }

    boolean isDestroyed() {
        return mCurrentInterstitialState == DESTROYED;
    }

    Integer getAdTimeoutDelay(int defaultValue) {
        return mInterstitialView.getAdTimeoutDelay(defaultValue);
    }

    @NonNull
    MoPubInterstitialView getMoPubInterstitialView() {
        return mInterstitialView;
    }

    private void showCustomEventInterstitial() {
        if (mCustomEventInterstitialAdapter != null) {
            mCustomEventInterstitialAdapter.showInterstitial();
        }
    }

    private void invalidateInterstitialAdapter() {
        if (mCustomEventInterstitialAdapter != null) {
            mCustomEventInterstitialAdapter.invalidate();
            mCustomEventInterstitialAdapter = null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void setKeywords(@Nullable final String keywords) {
        mInterstitialView.setKeywords(keywords);
    }

    @Nullable
    public String getKeywords() {
        return mInterstitialView.getKeywords();
    }

    public void setUserDataKeywords(@Nullable final String userDataKeywords) {
        mInterstitialView.setUserDataKeywords(userDataKeywords);
    }

    @Nullable
    public String getUserDataKeywords() {
        return mInterstitialView.getUserDataKeywords();
    }

    @NonNull
    public Activity getActivity() {
        return mActivity;
    }

    @Nullable
    public Location getLocation() {
        return mInterstitialView.getLocation();
    }

    public void destroy() {
        attemptStateTransition(DESTROYED);
    }

    public void setInterstitialAdListener(@Nullable final InterstitialAdListener listener) {
        mInterstitialAdListener = listener;
    }

    @Nullable
    public InterstitialAdListener getInterstitialAdListener() {
        return mInterstitialAdListener;
    }

    public void setTesting(boolean testing) {
        mInterstitialView.setTesting(testing);
    }

    public boolean getTesting() {
        return mInterstitialView.getTesting();
    }

    public void setLocalExtras(Map<String, Object> extras) {
        mInterstitialView.setLocalExtras(extras);
    }

    @NonNull
    public Map<String, Object> getLocalExtras() {
        return mInterstitialView.getLocalExtras();
    }

    /*
     * Implements CustomEventInterstitialAdapter.CustomEventInterstitialListener
     * Note: All callbacks should be no-ops if the interstitial has been destroyed
     */

    @Override
    public void onCustomEventInterstitialLoaded() {
        if (isDestroyed()) {
            return;
        }

        attemptStateTransition(READY);
    }

    @Override
    public void onCustomEventInterstitialFailed(@NonNull final MoPubErrorCode errorCode) {
        if (isDestroyed()) {
            return;
        }

        if (mCurrentInterstitialState == LOADING) {
            MoPubLog.log(LOAD_FAILED, errorCode.getIntCode(), errorCode);
        } else if (mCurrentInterstitialState == SHOWING) {
            MoPubLog.log(SHOW_FAILED, errorCode.getIntCode(), errorCode);
        }

        if (!mInterstitialView.loadFailUrl(errorCode)) {
            attemptStateTransition(IDLE);
        }
    }

    @Override
    public void onCustomEventInterstitialShown() {
        if (isDestroyed()) {
            return;
        }

        MoPubLog.log(SHOW_SUCCESS);

        if (mCustomEventInterstitialAdapter == null ||
                mCustomEventInterstitialAdapter.isAutomaticImpressionAndClickTrackingEnabled()) {
            mInterstitialView.trackImpression();
        }

        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialShown(this);
        }
    }

    @Override
    public void onCustomEventInterstitialClicked() {
        if (isDestroyed()) {
            return;
        }
        MoPubLog.log(CLICKED);

        mInterstitialView.registerClick();

        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialClicked(this);
        }
    }

    @Override
    public void onCustomEventInterstitialImpression() {
        if (isDestroyed()) {
            return;
        }

        if (mCustomEventInterstitialAdapter != null &&
                !mCustomEventInterstitialAdapter.isAutomaticImpressionAndClickTrackingEnabled()) {
            mInterstitialView.trackImpression();
        }
    }

    @Override
    public void onCustomEventInterstitialDismissed() {
        if (isDestroyed()) {
            return;
        }
        MoPubLog.log(WILL_DISAPPEAR);

        attemptStateTransition(IDLE);

        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialDismissed(this);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public class MoPubInterstitialView extends MoPubView {
        public MoPubInterstitialView(Context context) {
            super(context);
            setAutorefreshEnabled(false);
        }

        @Nullable String getCustomEventClassName() {
            return mAdViewController.getCustomEventClassName();
        }

        @Override
        public AdFormat getAdFormat() {
            return AdFormat.INTERSTITIAL;
        }

        @Override
        protected void loadCustomEvent(String customEventClassName, Map<String, String> serverExtras) {
            if (mAdViewController == null) {
                return;
            }

            if (TextUtils.isEmpty(customEventClassName)) {
                MoPubLog.log(CUSTOM, "Couldn't invoke custom event because the server did not specify one.");
                loadFailUrl(ADAPTER_NOT_FOUND);
                return;
            }

            if (mCustomEventInterstitialAdapter != null) {
                mCustomEventInterstitialAdapter.invalidate();
            }

            MoPubLog.log(CUSTOM, "Loading custom event interstitial adapter.");

            mCustomEventInterstitialAdapter = CustomEventInterstitialAdapterFactory.create(
                    MoPubInterstitial.this,
                    customEventClassName,
                    serverExtras,
                    mAdViewController.getBroadcastIdentifier(),
                    mAdViewController.getAdReport());
            mCustomEventInterstitialAdapter.setAdapterListener(MoPubInterstitial.this);
            mCustomEventInterstitialAdapter.loadInterstitial();
        }

        protected void trackImpression() {
            MoPubLog.log(CUSTOM, "Tracking impression for interstitial.");
            if (mAdViewController != null) mAdViewController.trackImpression();
        }

        @Override
        protected void adFailed(MoPubErrorCode errorCode) {
            attemptStateTransition(IDLE);
            if (mInterstitialAdListener != null) {
                mInterstitialAdListener.onInterstitialFailed(MoPubInterstitial.this, errorCode);
            }
        }

        @Override
        protected Point resolveAdSize() {
            return DeviceUtils.getDeviceDimensions(mActivity);
        }
    }

    @VisibleForTesting
    @Deprecated
    void setHandler(@NonNull final Handler handler) {
        mHandler = handler;
    }

    @VisibleForTesting
    @Deprecated
    void setInterstitialView(@NonNull MoPubInterstitialView interstitialView) {
        mInterstitialView = interstitialView;
    }

    @VisibleForTesting
    @Deprecated
    void setCurrentInterstitialState(@NonNull final InterstitialState interstitialState) {
        mCurrentInterstitialState = interstitialState;
    }

    @VisibleForTesting
    @Deprecated
    @NonNull
    InterstitialState getCurrentInterstitialState() {
        return mCurrentInterstitialState;
    }

    @VisibleForTesting
    @Deprecated
    void setCustomEventInterstitialAdapter(@NonNull final CustomEventInterstitialAdapter
            customEventInterstitialAdapter) {
        mCustomEventInterstitialAdapter = customEventInterstitialAdapter;
    }
}
