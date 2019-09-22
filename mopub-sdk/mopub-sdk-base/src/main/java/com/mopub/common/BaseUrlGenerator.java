// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.DisplayCutout;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.network.Networking;
import com.mopub.network.PlayServicesUrlRewriter;

public abstract class BaseUrlGenerator {

    /**
     * The ad unit id which identifies a spot for an ad to be placed.
     */
    protected static final String AD_UNIT_ID_KEY = "id";

    /**
     * nv = native version. This is the version of MoPub.
     */
    protected static final String SDK_VERSION_KEY = "nv";

    /**
     * User ifa or mopub-generated identifier.
     */
    protected static final String UDID_KEY = "udid";

    /**
     * "Do not track." Equal to 1 when limit ad tracking is turned on. Equal to 0 otherwise.
     */
    protected static final String DNT_KEY = "dnt";

    /**
     * Always the mopub-generated identifier.
     */
    protected static final String MOPUB_ID_KEY = "mid";

    /**
     * Bundle ID, as in package name.
     */
    protected static final String BUNDLE_ID_KEY = "bundle";

    /**
     * The current consent state.
     */
    protected static final String CURRENT_CONSENT_STATUS_KEY = "current_consent_status";

    /**
     * The version of the vendor list that has been consented to. Null if no consent given.
     */
    protected static final String CONSENTED_VENDOR_LIST_VERSION_KEY = "consented_vendor_list_version";

    /**
     * The version of the privacy policy that has been consented to. Null if no consent given.
     */
    protected static final String CONSENTED_PRIVACY_POLICY_VERSION_KEY = "consented_privacy_policy_version";

    /**
     * Whether or not GDPR applies to this user. Can be different from whether or not this user is
     * in a GDPR region.
     */
    protected static final String GDPR_APPLIES = "gdpr_applies";

    /**
     * "1" if the publisher has forced GDPR rules to apply to this app. "0" if this is not set.
     */
    protected static final String FORCE_GDPR_APPLIES = "force_gdpr_applies";

    private static final String WIDTH_KEY = "w";
    private static final String HEIGHT_KEY = "h";

    /**
     * The width of the screen's safe area. See
     * https://developer.android.com/guide/topics/display-cutout
     * for details on how this is calculated.
     */
    private static final String SAFE_WIDTH_KEY = "cw";

    /**
     * The height of the screen's safe area. See
     * https://developer.android.com/guide/topics/display-cutout
     * for details on how this is calculated.
     */
    private static final String SAFE_HEIGHT_KEY = "ch";

    /**
     * Optional parameter to pass application engine name
     */
    private static final String APP_ENGINE_NAME = "e_name";

    /**
     * Optional, version of the application engine
     */
    private static final String APP_ENGINE_VERSION = "e_ver";

    /**
     * Optional, wrapper version.
     */
    private static final String WRAPPER_VERSION = "w_ver";

    private StringBuilder mStringBuilder;
    private boolean mFirstParam;
    private static AppEngineInfo mAppEngineInfo = null;
    private static String sWrapperVersion;

    public abstract String generateUrlString(String serverHostname);

    protected void initUrlString(String serverHostname, String handlerType) {
        mStringBuilder = new StringBuilder(Networking.getScheme()).append("://")
                .append(serverHostname).append(handlerType);
        mFirstParam = true;
    }

    protected String getFinalUrlString() {
        return mStringBuilder.toString();
    }

    protected void addParam(String key, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }

        mStringBuilder.append(getParamDelimiter());
        mStringBuilder.append(key);
        mStringBuilder.append("=");
        mStringBuilder.append(Uri.encode(value));
    }

    protected void addParam(String key, Boolean value) {
        if (value == null) {
            return;
        }

        mStringBuilder.append(getParamDelimiter());
        mStringBuilder.append(key);
        mStringBuilder.append("=");
        mStringBuilder.append(value ? "1" : "0");
    }

    private String getParamDelimiter() {
        if (mFirstParam) {
            mFirstParam = false;
            return "?";
        }
        return "&";
    }

    protected void setApiVersion(String apiVersion) {
        addParam("v", apiVersion);
    }

    protected void setAppVersion(String appVersion) {
        addParam("av", appVersion);
    }

    protected void setExternalStoragePermission(boolean isExternalStoragePermissionGranted) {
        addParam("android_perms_ext_storage", isExternalStoragePermissionGranted ? "1" : "0");
    }

    protected void setDeviceInfo(String... info) {
        StringBuilder result = new StringBuilder();
        if (info == null || info.length < 1) {
            return;
        }

        for (int i=0; i<info.length-1; i++) {
            result.append(info[i]).append(",");
        }
        result.append(info[info.length-1]);

        addParam("dn", result.toString());
    }

    /**
     * Appends special keys/values for advertising id and do-not-track. PlayServicesUrlRewriter will
     * replace these templates with the correct values when the request is processed.
     */
    protected void appendAdvertisingInfoTemplates() {
        addParam(UDID_KEY, PlayServicesUrlRewriter.UDID_TEMPLATE);
        addParam(DNT_KEY, PlayServicesUrlRewriter.DO_NOT_TRACK_TEMPLATE);
        addParam(MOPUB_ID_KEY, PlayServicesUrlRewriter.MOPUB_ID_TEMPLATE);
    }

    /**
     *
     * @param engineInfo {@link com.mopub.common.AppEngineInfo}
     */
    public static void setAppEngineInfo(@NonNull final AppEngineInfo engineInfo) {
        mAppEngineInfo = engineInfo;
    }

    /**
     * Sets the 'w_ver' param. This is not intended for publisher use.
     *
     * @param wrapperVersion The wrapper version.
     */
    public static void setWrapperVersion(@NonNull final String wrapperVersion) {
        Preconditions.checkNotNull(wrapperVersion);
        
        sWrapperVersion = wrapperVersion;
    }

    /**
     * Add parameters 'e_name' and 'e_ver' to the URL
     */
    protected void appendAppEngineInfo() {
        final AppEngineInfo info = mAppEngineInfo;
        if (info != null) {
            addParam(APP_ENGINE_NAME, info.mName);
            addParam(APP_ENGINE_VERSION, info.mVersion);
        }
    }

    protected void appendWrapperVersion() {
        addParam(WRAPPER_VERSION, sWrapperVersion);
    }

    /**
     * Adds the width and height.
     *
     * @param dimensions The width and height of the screen
     * @param requestedAdSize The requested width and height for the ad.
     * @param windowInsets The WindowInsets for the current window.
     */
    protected void setDeviceDimensions(@NonNull final Point dimensions, 
                                       @Nullable final Point requestedAdSize,
                                       @Nullable final WindowInsets windowInsets) {
        final int requestedWidth = ((requestedAdSize != null) ? requestedAdSize.x : 0);
        final int requestedHeight = ((requestedAdSize != null) ? requestedAdSize.y : 0);
      
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && windowInsets != null
                && windowInsets.getDisplayCutout() != null) {
            final DisplayCutout displayCutout = windowInsets.getDisplayCutout();
            final int safeWidth = dimensions.x - displayCutout.getSafeInsetLeft() - displayCutout.getSafeInsetRight();
            final int safeHeight = dimensions.y - displayCutout.getSafeInsetTop() - displayCutout.getSafeInsetBottom();
          
            addParam(SAFE_WIDTH_KEY, "" + Math.min(safeWidth, requestedWidth));
            addParam(SAFE_HEIGHT_KEY, "" + Math.min(safeHeight, requestedHeight));
        } else {
            addParam(SAFE_WIDTH_KEY, "" + requestedWidth);
            addParam(SAFE_HEIGHT_KEY, "" + requestedHeight);
        }
      
        addParam(WIDTH_KEY, "" + dimensions.x);
        addParam(HEIGHT_KEY, "" + dimensions.y);
    }
}
