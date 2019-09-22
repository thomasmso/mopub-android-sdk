// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Comparator;
import java.util.Locale;

class MoPubSampleAdUnit implements Comparable<MoPubSampleAdUnit> {

    private static final String AD_UNIT_ID = "adUnitId";
    static final String DESCRIPTION = "description";
    private static final String AD_TYPE = "adType";
    private static final String IS_USER_DEFINED = "isCustom";
    private static final String ID = "id";

    // Note that entries are also sorted in this order
    enum AdType {
        BANNER("Banner", BannerDetailFragment.class),
        MEDIUM_RECTANGLE("Medium Rectangle", MediumRectangleDetailFragment.class),
        INTERSTITIAL("Interstitial", InterstitialDetailFragment.class),
        REWARDED_VIDEO("Rewarded Video", RewardedVideoDetailFragment.class),
        LIST_VIEW("Native List View", NativeListViewFragment.class),
        RECYCLER_VIEW("Native Recycler View", NativeRecyclerViewFragment.class),
        CUSTOM_NATIVE("Native Gallery (Custom Stream)", NativeGalleryFragment.class);

        String getName() {
            return name;
        }

        private final String name;
        private final Class<? extends Fragment> fragmentClass;

        AdType(final String name, final Class<? extends Fragment> fragmentClass) {
            this.name = name;
            this.fragmentClass = fragmentClass;
        }

        private Class<? extends Fragment> getFragmentClass() {
            return fragmentClass;
        }

        static AdType fromFragmentClassName(final String fragmentClassName) {
            for (final AdType adType : values()) {
                if (adType.fragmentClass.getName().equals(fragmentClassName)) {
                    return adType;
                }
            }

            return null;
        }

        @Nullable
        static AdType fromDeeplinkString(@Nullable final String adType) {
            if (adType == null) {
                return null;
            }
            switch (adType.toLowerCase(Locale.US)) {
                case "banner":
                    return BANNER;
                case "interstitial":
                    return INTERSTITIAL;
                case "mediumrectangle":
                    return MEDIUM_RECTANGLE;
                case "rewarded":
                    return REWARDED_VIDEO;
                case "native":
                    return LIST_VIEW;
                case "nativetableplacer":
                    return RECYCLER_VIEW;
                case "nativecollectionplacer":
                    return CUSTOM_NATIVE;
                default:
                    return null;
            }
        }
    }

    static final Comparator<MoPubSampleAdUnit> COMPARATOR =
            new Comparator<MoPubSampleAdUnit>() {
                @Override
                public int compare(MoPubSampleAdUnit a, MoPubSampleAdUnit b) {
                    if (a.mAdType != b.mAdType) {
                        return a.mAdType.ordinal() - b.mAdType.ordinal();
                    }

                    return a.mDescription.compareTo(b.mDescription);
                }
            };

    static class Builder {
        private final String mAdUnitId;
        private final AdType mAdType;

        private String mDescription;
        private boolean mIsUserDefined;
        private long mId;

        Builder(final String adUnitId, final AdType adType) {
            mAdUnitId = adUnitId;
            mAdType = adType;
            mId = -1;
        }

        Builder description(final String description) {
            mDescription = description;
            return this;
        }

        Builder isUserDefined(boolean userDefined) {
            mIsUserDefined = userDefined;
            return this;
        }

        Builder id(final long id) {
            mId = id;
            return this;
        }

        MoPubSampleAdUnit build() {
            return new MoPubSampleAdUnit(this);
        }
    }

    private final String mAdUnitId;
    private final AdType mAdType;
    private final String mDescription;
    private final boolean mIsUserDefined;
    private final long mId;

    private MoPubSampleAdUnit(final Builder builder) {
        mAdUnitId = builder.mAdUnitId;
        mAdType = builder.mAdType;
        mDescription = builder.mDescription;
        mIsUserDefined = builder.mIsUserDefined;
        mId = builder.mId;
    }

    Class<? extends Fragment> getFragmentClass() {
        return mAdType.getFragmentClass();
    }

    String getAdUnitId() {
        return mAdUnitId;
    }

    String getDescription() {
        return mDescription;
    }

    String getFragmentClassName() {
        return mAdType.getFragmentClass().getName();
    }

    String getHeaderName() {
        return mAdType.name;
    }

    long getId() {
        return mId;
    }

    boolean isUserDefined() {
        return mIsUserDefined;
    }

    Bundle toBundle() {
        final Bundle bundle = new Bundle();
        bundle.putLong(ID, mId);
        bundle.putString(AD_UNIT_ID, mAdUnitId);
        bundle.putString(DESCRIPTION, mDescription);
        bundle.putSerializable(AD_TYPE, mAdType);
        bundle.putBoolean(IS_USER_DEFINED, mIsUserDefined);

        return bundle;
    }

    static MoPubSampleAdUnit fromBundle(final Bundle bundle) {
        final long id = bundle.getLong(ID, -1L);
        final String adUnitId = bundle.getString(AD_UNIT_ID);
        final AdType adType = (AdType) bundle.getSerializable(AD_TYPE);
        final String description = bundle.getString(DESCRIPTION);
        final boolean isUserDefined = bundle.getBoolean(IS_USER_DEFINED, false);
        final Builder builder = new MoPubSampleAdUnit.Builder(adUnitId, adType);
        builder.description(description);
        builder.id(id);
        builder.isUserDefined(isUserDefined);

        return builder.build();
    }

    @Override
    public int compareTo(@NonNull MoPubSampleAdUnit that) {
        if (mAdType != that.mAdType) {
            return mAdType.ordinal() - that.mAdType.ordinal();
        }

        return mAdUnitId.compareTo(that.mAdUnitId);
    }

    @Override
    public int hashCode() {
        int result = 11;
        result = 31 * result + mAdType.ordinal();
        result = 31 * result + (mIsUserDefined ? 1 : 0);
        result = 31 * result + mDescription.hashCode();
        result = 31 * result + mAdUnitId.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (!(o instanceof MoPubSampleAdUnit)) {
            return false;
        }

        final MoPubSampleAdUnit that = (MoPubSampleAdUnit) o;

        return that.mAdType.equals(this.mAdType) &&
                that.mIsUserDefined == this.mIsUserDefined &&
                that.mDescription.equals(this.mDescription) &&
                that.mAdUnitId.equals(this.mAdUnitId);
    }

    @Override
    @NonNull
    public String toString() {
        return mDescription == null ? "" : mDescription;
    }
}
