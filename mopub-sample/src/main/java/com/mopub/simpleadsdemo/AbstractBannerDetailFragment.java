// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubView;

import static com.mopub.mobileads.MoPubView.BannerAdListener;
import static com.mopub.simpleadsdemo.Utils.hideSoftKeyboard;
import static com.mopub.simpleadsdemo.Utils.logToast;

/**
 * A base class for creating banner style ads with various height and width dimensions.
 * <p>
 * A subclass simply needs to specify the height and width of the ad in pixels, and this class will
 * inflate a layout containing a programmatically rescaled {@link MoPubView} that will be used to
 * display the ad.
 */
public abstract class AbstractBannerDetailFragment extends Fragment implements BannerAdListener {
    private MoPubView mMoPubView;
    private MoPubSampleAdUnit mMoPubSampleAdUnit;
    @Nullable private CallbacksAdapter mCallbacksAdapter;

    public abstract MoPubView.MoPubAdSize getAdSize();

    private enum BannerCallbacks {
        LOADED("onBannerLoaded"),
        FAILED("onBannerFailed"),
        CLICKED("onBannerClicked"),
        EXPANDED("onBannerExpanded"),
        COLLAPSED("onBannerCollapsed");

        BannerCallbacks(@NonNull final String name) {
            this.name = name;
        }

        @NonNull
        private final String name;

        @Override
        @NonNull
        public String toString() {
            return name;
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mMoPubView.loadAd();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.banner_detail_fragment, container, false);
        final DetailFragmentViewHolder views = DetailFragmentViewHolder.fromView(view);

        mMoPubSampleAdUnit = MoPubSampleAdUnit.fromBundle(getArguments());
        mMoPubView = (MoPubView) view.findViewById(R.id.banner_mopubview);
        LinearLayout.LayoutParams layoutParams =
                (LinearLayout.LayoutParams) mMoPubView.getLayoutParams();
        mMoPubView.setLayoutParams(layoutParams);
        mMoPubView.setAdSize(getAdSize());

        views.mKeywordsField.setText(getArguments().getString(MoPubListFragment.KEYWORDS_KEY, ""));
        views.mUserDataKeywordsField.setText(getArguments().getString(MoPubListFragment.USER_DATA_KEYWORDS_KEY, ""));
        hideSoftKeyboard(views.mKeywordsField);

        final String adUnitId = mMoPubSampleAdUnit.getAdUnitId();
        views.mDescriptionView.setText(mMoPubSampleAdUnit.getDescription());
        views.mAdUnitIdView.setText(adUnitId);
        views.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String keywords = views.mKeywordsField.getText().toString();
                final String userDataKeywords = views.mUserDataKeywordsField.getText().toString();
                setupMoPubView(adUnitId, keywords, userDataKeywords);
                mMoPubView.loadAd();
            }
        });

        final RecyclerView callbacksView = view.findViewById(R.id.callbacks_recycler_view);
        final Context context = getContext();
        if (callbacksView != null && context != null) {
            callbacksView.setLayoutManager(new LinearLayoutManager(context));
            mCallbacksAdapter = new CallbacksAdapter(context);
            mCallbacksAdapter.generateCallbackList(BannerCallbacks.class);
            callbacksView.setAdapter(mCallbacksAdapter);
        }

        mMoPubView.setBannerAdListener(this);
        setupMoPubView(adUnitId, null, null);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mMoPubView != null) {
            mMoPubView.destroy();
            mMoPubView = null;
        }
    }

    private void setupMoPubView(final String adUnitId, final String keywords, final String userDataKeywords) {
        mMoPubView.setAdUnitId(adUnitId);
        mMoPubView.setKeywords(keywords);
        mMoPubView.setUserDataKeywords(userDataKeywords);
        if (mCallbacksAdapter != null) {
            mCallbacksAdapter.generateCallbackList(BannerCallbacks.class);
        }
    }

    private String getName() {
        if (mMoPubSampleAdUnit == null) {
            return MoPubSampleAdUnit.AdType.BANNER.getName();
        }
        return mMoPubSampleAdUnit.getHeaderName();
    }

    // BannerAdListener
    @Override
    public void onBannerLoaded(MoPubView banner) {
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), getName() + " loaded.");
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(BannerCallbacks.LOADED.toString());
    }

    @Override
    public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode) {
        final String errorMessage = (errorCode != null) ? errorCode.toString() : "";
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), getName() + " failed to load: " + errorMessage);
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(BannerCallbacks.FAILED.toString(), errorMessage);
    }

    @Override
    public void onBannerClicked(MoPubView banner) {
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), getName() + " clicked.");
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(BannerCallbacks.CLICKED.toString());
    }

    @Override
    public void onBannerExpanded(MoPubView banner) {
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), getName() + " expanded.");
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(BannerCallbacks.EXPANDED.toString());
    }

    @Override
    public void onBannerCollapsed(MoPubView banner) {
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), getName() + " collapsed.");
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(BannerCallbacks.COLLAPSED.toString());
    }
}
