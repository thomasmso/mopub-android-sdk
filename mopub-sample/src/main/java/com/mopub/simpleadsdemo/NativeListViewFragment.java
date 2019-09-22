// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import com.mopub.nativeads.FacebookAdRenderer;
import com.mopub.nativeads.FlurryCustomEventNative;
import com.mopub.nativeads.FlurryNativeAdRenderer;
import com.mopub.nativeads.FlurryViewBinder;
import com.mopub.nativeads.GooglePlayServicesAdRenderer;
import com.mopub.nativeads.MediaViewBinder;
import com.mopub.nativeads.MoPubAdAdapter;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.MoPubVideoNativeAdRenderer;
import com.mopub.nativeads.RequestParameters;
import com.mopub.nativeads.ViewBinder;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static com.mopub.nativeads.MoPubNativeAdPositioning.MoPubServerPositioning;
import static com.mopub.nativeads.RequestParameters.NativeAdAsset;

public class NativeListViewFragment extends Fragment {
    private MoPubAdAdapter mAdAdapter;
    private MoPubSampleAdUnit mAdConfiguration;
    private RequestParameters mRequestParameters;

    @Override
    public View onCreateView(final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mAdConfiguration = MoPubSampleAdUnit.fromBundle(getArguments());
        final View view = inflater.inflate(R.layout.native_list_view_fragment, container, false);
        final ListView listView = (ListView) view.findViewById(R.id.native_list_view);
        final DetailFragmentViewHolder views = DetailFragmentViewHolder.fromView(view);
        views.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If your app already has location access, include it here.
                final Location location = null;
                final String keywords = views.mKeywordsField.getText().toString();
                final String userDataKeywords = views.mUserDataKeywordsField.getText().toString();

                // Setting desired assets on your request helps native ad networks and bidders
                // provide higher-quality ads.
                final EnumSet<NativeAdAsset> desiredAssets = EnumSet.of(
                        NativeAdAsset.TITLE,
                        NativeAdAsset.TEXT,
                        NativeAdAsset.ICON_IMAGE,
                        NativeAdAsset.MAIN_IMAGE,
                        NativeAdAsset.CALL_TO_ACTION_TEXT);

                mRequestParameters = new RequestParameters.Builder()
                        .location(location)
                        .keywords(keywords)
                        .userDataKeywords(userDataKeywords)
                        .desiredAssets(desiredAssets)
                        .build();

                mAdAdapter.loadAds(mAdConfiguration.getAdUnitId(), mRequestParameters);
            }
        });
        final String adUnitId = mAdConfiguration.getAdUnitId();
        views.mDescriptionView.setText(mAdConfiguration.getDescription());
        views.mAdUnitIdView.setText(adUnitId);
        views.mKeywordsField.setText(getArguments().getString(MoPubListFragment.KEYWORDS_KEY, ""));
        views.mUserDataKeywordsField.setText(getArguments().getString(MoPubListFragment.USER_DATA_KEYWORDS_KEY, ""));

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1);
        for (int i = 0; i < 100; ++i) {
            adapter.add("Item " + i);
        }

        // Create an ad adapter that gets its positioning information from the MoPub Ad Server.
        // This adapter will be used in place of the original adapter for the ListView.
        mAdAdapter = new MoPubAdAdapter(getActivity(), adapter, new MoPubServerPositioning());

        // Set up a renderer that knows how to put ad data in your custom native view.
        final MoPubStaticNativeAdRenderer staticAdRender = new MoPubStaticNativeAdRenderer(
                new ViewBinder.Builder(R.layout.native_ad_list_item)
                        .titleId(R.id.native_title)
                        .textId(R.id.native_text)
                        .mainImageId(R.id.native_main_image)
                        .iconImageId(R.id.native_icon_image)
                        .callToActionId(R.id.native_cta)
                        .privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
                        .build());

        // Set up a renderer for a video native ad.
        final MoPubVideoNativeAdRenderer videoAdRenderer = new MoPubVideoNativeAdRenderer(
                new MediaViewBinder.Builder(R.layout.video_ad_list_item)
                        .titleId(R.id.native_title)
                        .textId(R.id.native_text)
                        .mediaLayoutId(R.id.native_media_layout)
                        .iconImageId(R.id.native_icon_image)
                        .callToActionId(R.id.native_cta)
                        .privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
                        .build());

        // Set up a renderer for Facebook video ads.
        final FacebookAdRenderer facebookAdRenderer = new FacebookAdRenderer(
                new FacebookAdRenderer.FacebookViewBinder.Builder(R.layout.native_ad_fan_list_item)
                        .titleId(R.id.native_title)
                        .textId(R.id.native_text)
                        .mediaViewId(R.id.native_media_view)
                        .adIconViewId(R.id.native_icon)
                        .callToActionId(R.id.native_cta)
                        .adChoicesRelativeLayoutId(R.id.native_privacy_information_icon_layout)
                        .build());

        // Set up a renderer for Flurry ads.
        Map<String, Integer> extraToResourceMap = new HashMap<>(3);
        extraToResourceMap.put(FlurryCustomEventNative.EXTRA_SEC_BRANDING_LOGO,
                R.id.flurry_native_brand_logo);
        extraToResourceMap.put(FlurryCustomEventNative.EXTRA_APP_CATEGORY,
                R.id.flurry_app_category);
        extraToResourceMap.put(FlurryCustomEventNative.EXTRA_STAR_RATING_IMG,
                R.id.flurry_star_rating_image);
        ViewBinder flurryBinder = new ViewBinder.Builder(R.layout.native_ad_flurry_list_item)
                .titleId(R.id.flurry_native_title)
                .textId(R.id.flurry_native_text)
                .mainImageId(R.id.flurry_native_main_image)
                .iconImageId(R.id.flurry_native_icon_image)
                .callToActionId(R.id.flurry_native_cta)
                .addExtras(extraToResourceMap)
                .build();
        FlurryViewBinder flurryViewBinder = new FlurryViewBinder.Builder(flurryBinder)
                .videoViewId(R.id.flurry_native_video_view)
                .build();
        final FlurryNativeAdRenderer flurryRenderer = new FlurryNativeAdRenderer(flurryViewBinder);

        // Set up a renderer for AdMob ads.
        final GooglePlayServicesAdRenderer googlePlayServicesAdRenderer = new GooglePlayServicesAdRenderer(
                new MediaViewBinder.Builder(R.layout.video_ad_list_item)
                        .titleId(R.id.native_title)
                        .textId(R.id.native_text)
                        .mediaLayoutId(R.id.native_media_layout)
                        .iconImageId(R.id.native_icon_image)
                        .callToActionId(R.id.native_cta)
                        .privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
                        .build());

        // Register the renderers with the MoPubAdAdapter and then set the adapter on the ListView.
        // The first renderer that can handle a particular native ad gets used.
        // We are prioritizing network renderers.
        mAdAdapter.registerAdRenderer(googlePlayServicesAdRenderer);
        mAdAdapter.registerAdRenderer(flurryRenderer);
        mAdAdapter.registerAdRenderer(facebookAdRenderer);
        mAdAdapter.registerAdRenderer(staticAdRender);
        mAdAdapter.registerAdRenderer(videoAdRenderer);
        listView.setAdapter(mAdAdapter);

        mAdAdapter.loadAds(mAdConfiguration.getAdUnitId(), mRequestParameters);
        return view;
    }

    @Override
    public void onDestroyView() {
        // You must call this or the ad adapter may cause a memory leak.
        mAdAdapter.destroy();
        super.onDestroyView();
    }
}
