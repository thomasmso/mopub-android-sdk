// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.mopub.common.Preconditions;

import java.io.Serializable;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

public class AdvertisingId implements Serializable {
    static final long ONE_DAY_MS = 24 * 60 * 60 * 1000;
    private static final String PREFIX_IFA = "ifa:";
    private static final String PREFIX_MOPUB = "mopub:";

    /**
     * time when mopub generated ID was rotated last time
     */
    @NonNull
    final Calendar mLastRotation;

    /**
     * Advertising ID from device, may not always be available.
     * Empty string if ifa is not available.
     */
    @NonNull
    final String mAdvertisingId;

    /**
     * virtual device ID, rotated every 24 hours
     */
    @NonNull
    final String mMopubId;

    /**
     * limit ad tracking device setting
     */
    final boolean mDoNotTrack;

    AdvertisingId(@NonNull String ifaId,
                  @NonNull String mopubId,
                  boolean limitAdTrackingEnabled,
                  long rotationTime) {
        Preconditions.checkNotNull(ifaId);
        Preconditions.checkNotNull(mopubId);

        mAdvertisingId = ifaId;
        mMopubId = mopubId;
        mDoNotTrack = limitAdTrackingEnabled;
        mLastRotation = Calendar.getInstance();
        mLastRotation.setTimeInMillis(rotationTime);
    }

    /**
     * @param consent - true means user is OK to track his data for Ad purposes
     * @return read advertising ID or UUID
     */
    public String getIdentifier(boolean consent) {
        return mDoNotTrack || !consent ? mMopubId : mAdvertisingId;
    }

    /**
     * @param consent - true means user is OK to track his data for Ad purposes
     * @return one of two: "mopub:mMopubId" or "ifa:mAdvertisingId"
     */
    @NonNull
    public String getIdWithPrefix(boolean consent) {
        if (mDoNotTrack || !consent || mAdvertisingId.isEmpty()) {
            return PREFIX_MOPUB + mMopubId;
        }
        return PREFIX_IFA + mAdvertisingId;
    }

    /**
     * Gets the ifa with the ifa prefix.
     *
     * @return The ifa, if it exists. Empty string if it doesn't.
     */
    @NonNull
    String getIfaWithPrefix() {
        if (TextUtils.isEmpty(mAdvertisingId)) {
            return "";
        }
        return PREFIX_IFA + mAdvertisingId;
    }

    /**
     * @return device Do Not Track settings
     */
    public boolean isDoNotTrack() {
        return mDoNotTrack;
    }

    @NonNull
    static AdvertisingId generateExpiredAdvertisingId() {
        Calendar time = Calendar.getInstance();
        String mopubId = generateIdString();
        return new AdvertisingId("", mopubId, false, time.getTimeInMillis() - ONE_DAY_MS - 1);
    }

    @NonNull
    static AdvertisingId generateFreshAdvertisingId() {
        Calendar time = Calendar.getInstance();
        String mopubId = generateIdString();
        return new AdvertisingId("", mopubId, false, time.getTimeInMillis());
    }

    @NonNull
    static String generateIdString() {
        return UUID.randomUUID().toString();
    }

    boolean isRotationRequired() {
        final Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        final Calendar lastRotation = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        lastRotation.setTimeInMillis(mLastRotation.getTimeInMillis());
        return (now.get(Calendar.DAY_OF_YEAR) != lastRotation.get(Calendar.DAY_OF_YEAR)) ||
                (now.get(Calendar.YEAR) != lastRotation.get(Calendar.YEAR));
    }

    @Override
    public String toString() {
        return "AdvertisingId{" +
                "mLastRotation=" + mLastRotation +
                ", mAdvertisingId='" + mAdvertisingId + '\'' +
                ", mMopubId='" + mMopubId + '\'' +
                ", mDoNotTrack=" + mDoNotTrack +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdvertisingId)) return false;

        AdvertisingId that = (AdvertisingId) o;

        if (mDoNotTrack != that.mDoNotTrack) return false;
        if (!mAdvertisingId.equals(that.mAdvertisingId)) return false;
        return mMopubId.equals(that.mMopubId);
    }

    @Override
    public int hashCode() {
        int result = mAdvertisingId.hashCode();
        result = 31 * result + mMopubId.hashCode();
        result = 31 * result + (mDoNotTrack ? 1 : 0);
        return result;
    }
}
