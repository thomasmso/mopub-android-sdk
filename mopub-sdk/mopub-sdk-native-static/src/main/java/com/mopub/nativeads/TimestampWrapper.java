// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.os.SystemClock;
import androidx.annotation.NonNull;

class TimestampWrapper<T> {
    @NonNull final T mInstance;
    long mCreatedTimestamp;

    TimestampWrapper(@NonNull final T instance) {
        mInstance = instance;
        mCreatedTimestamp = SystemClock.uptimeMillis();
    }
}
