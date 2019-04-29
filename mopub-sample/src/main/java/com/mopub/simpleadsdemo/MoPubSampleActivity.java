// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import com.mopub.common.Constants;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentData;
import com.mopub.common.privacy.ConsentDialogListener;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.ConsentStatusChangeListener;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.DeviceUtils;
import com.mopub.common.util.Reflection;
import com.mopub.mobileads.MoPubErrorCode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.mopub.common.Constants.UNUSED_REQUEST_CODE;
import static com.mopub.common.logging.MoPubLog.LogLevel.DEBUG;
import static com.mopub.common.logging.MoPubLog.LogLevel.INFO;

public class MoPubSampleActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final List<String> REQUIRED_DANGEROUS_PERMISSIONS = new ArrayList<>();

    static {
        REQUIRED_DANGEROUS_PERMISSIONS.add(ACCESS_COARSE_LOCATION);
        REQUIRED_DANGEROUS_PERMISSIONS.add(WRITE_EXTERNAL_STORAGE);
    }

    // Sample app web views are debuggable.
    static {
        setWebDebugging();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void setWebDebugging() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    private MoPubListFragment mMoPubListFragment;
    private Intent mDeeplinkIntent;
    @Nullable
    PersonalInfoManager mPersonalInfoManager;

    @Nullable DrawerLayout mDrawerLayout;

    @Nullable
    private ConsentStatusChangeListener mConsentStatusChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupNavigationDrawer(toolbar);

        List<String> permissionsToBeRequested = new ArrayList<>();
        for (String permission : REQUIRED_DANGEROUS_PERMISSIONS) {
            if (!DeviceUtils.isPermissionGranted(this, permission)) {
                permissionsToBeRequested.add(permission);
            }
        }

        // Request dangerous permissions
        if (!permissionsToBeRequested.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToBeRequested.toArray(
                    new String[permissionsToBeRequested.size()]), UNUSED_REQUEST_CODE);
        }

        // Set location awareness and precision globally for your app:
        MoPub.setLocationAwareness(MoPub.LocationAwareness.TRUNCATED);
        MoPub.setLocationPrecision(4);

        if (savedInstanceState == null) {
            createMoPubListFragment(getIntent());
        }

        final SdkConfiguration.Builder configBuilder = new SdkConfiguration.Builder("b195f8dd8ded45fe847ad89ed1d016da");
        if (BuildConfig.DEBUG) {
            configBuilder.withLogLevel(DEBUG);
        } else {
            configBuilder.withLogLevel(INFO);
        }
        MoPub.initializeSdk(this, configBuilder.build(), initSdkListener());

        mConsentStatusChangeListener = initConsentChangeListener();
        mPersonalInfoManager = MoPub.getPersonalInformationManager();
        if (mPersonalInfoManager != null) {
            mPersonalInfoManager.subscribeConsentStatusChangeListener(mConsentStatusChangeListener);
        }

        // Intercepts all logs including Level.FINEST so we can show a toast
        // that is not normally user-facing. This is only used for native ads.
        LoggingUtils.enableCanaryLogging(this);
    }

    @Override
    protected void onDestroy() {
        if (mPersonalInfoManager != null) {
            // unsubscribe or memory leak will occur
            mPersonalInfoManager.unsubscribeConsentStatusChangeListener(mConsentStatusChangeListener);
        }
        mConsentStatusChangeListener = null;
        super.onDestroy();
    }

    private void createMoPubListFragment(@NonNull final Intent intent) {
        if (findViewById(R.id.fragment_container) != null) {
            mMoPubListFragment = new MoPubListFragment();
            mMoPubListFragment.setArguments(intent.getExtras());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mMoPubListFragment, LIST_FRAGMENT_TAG).commit();

            mDeeplinkIntent = intent;
        }
    }

    @Override
    public void onNewIntent(@NonNull final Intent intent) {
        mDeeplinkIntent = intent;
    }

    @Override
    public void onPostResume() {
        super.onPostResume();
        if (mMoPubListFragment != null && mDeeplinkIntent != null) {
            mMoPubListFragment.addAdUnitViaDeeplink(mDeeplinkIntent.getData());
            mDeeplinkIntent = null;
        }
    }

    private SdkInitializationListener initSdkListener() {
        return new SdkInitializationListener() {

            @Override
            public void onInitializationFinished() {
                syncNavigationMenu();
                Utils.logToast(MoPubSampleActivity.this, "SDK initialized.");
                if (mPersonalInfoManager != null && mPersonalInfoManager.shouldShowConsentDialog()) {
                    mPersonalInfoManager.loadConsentDialog(initDialogLoadListener());
                }
            }
        };
    }

    private ConsentStatusChangeListener initConsentChangeListener() {
        return new ConsentStatusChangeListener() {

            @Override
            public void onConsentStateChange(@NonNull ConsentStatus oldConsentStatus,
                                             @NonNull ConsentStatus newConsentStatus,
                                             boolean canCollectPersonalInformation) {
                Utils.logToast(MoPubSampleActivity.this, "Consent: " + newConsentStatus.name());
                if (mPersonalInfoManager != null && mPersonalInfoManager.shouldShowConsentDialog()) {
                    mPersonalInfoManager.loadConsentDialog(initDialogLoadListener());
                }
            }
        };
    }

    private ConsentDialogListener initDialogLoadListener() {
        return new ConsentDialogListener() {

            @Override
            public void onConsentDialogLoaded() {
                if (mPersonalInfoManager != null) {
                    mPersonalInfoManager.showConsentDialog();
                }
            }

            @Override
            public void onConsentDialogLoadFailed(@NonNull MoPubErrorCode moPubErrorCode) {
                Utils.logToast(MoPubSampleActivity.this, "Consent dialog failed to load.");
            }
        };
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /*
        MoPub Sample specific test code
     */
    private static final String PROD_HOST = Constants.HOST;
    private static final String TEST_HOST = "ads-staging.mopub.com";
    private static final String PRIVACY_FRAGMENT_TAG = "privacy_info_fragment";
    private static final String NETWORKS_FRAGMENT_TAG = "networks_info_fragment";
    private static final String LIST_FRAGMENT_TAG = "list_fragment";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void setupNavigationDrawer(Toolbar toolbar) {
        mDrawerLayout = findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        final NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void syncNavigationMenu() {
        final NavigationView navigationView = findViewById(R.id.nav_view);

        final String host = Constants.HOST;
        final boolean production = PROD_HOST.equalsIgnoreCase(host);
        navigationView.getMenu().findItem(R.id.nav_production).setChecked(production);
        navigationView.getMenu().findItem(R.id.nav_staging).setChecked(!production);

        final PersonalInfoManager manager = MoPub.getPersonalInformationManager();
        if (manager != null) {
            final ConsentData consentData = manager.getConsentData();
            navigationView.getMenu().findItem(R.id.nav_force_gdpr).setChecked(consentData.isForceGdprApplies());

            final ConsentStatus consentStatus = manager.getPersonalInfoConsentStatus();

            if (consentStatus.equals(ConsentStatus.POTENTIAL_WHITELIST)) {
                navigationView.getMenu().findItem(R.id.nav_privacy_grant).setChecked(true);
            } else if (consentStatus.equals(ConsentStatus.EXPLICIT_NO)) {
                navigationView.getMenu().findItem(R.id.nav_privacy_revoke).setChecked(true);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_production:
                onNavEnvironemnt(true);
                break;
            case R.id.nav_staging:
                onNavEnvironemnt(false);
                break;
            case R.id.nav_privacy_info:
                onNavPrivacyInfo();
                break;
            case R.id.nav_privacy_grant:
                onNavChangeConsent(true);
                break;
            case R.id.nav_privacy_revoke:
                onNavChangeConsent(false);
                break;
            case R.id.nav_force_gdpr:
                onNavForceGdpr();
                break;
            case R.id.nav_adapters_info:
                onNavAdaptersInfo();
                break;
        }

        syncNavigationMenu();

        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawers();
        }

        return false;
    }

    private void onNavEnvironemnt(boolean production) {
        setEndpoint(production ? PROD_HOST : TEST_HOST);
    }

    private void onNavPrivacyInfo() {
        final FragmentManager manager = getSupportFragmentManager();
        if (manager.findFragmentByTag(PRIVACY_FRAGMENT_TAG) == null) {
            manager.beginTransaction()
                    .replace(R.id.fragment_container, new PrivacyInfoFragment(), PRIVACY_FRAGMENT_TAG)
                    .addToBackStack(PRIVACY_FRAGMENT_TAG)
                    .commit();
        }
    }

    private void onNavChangeConsent(boolean grant) {
        final FragmentManager manager = getSupportFragmentManager();
        final MoPubListFragment listFragment = (MoPubListFragment) manager.findFragmentByTag(LIST_FRAGMENT_TAG);
        if (listFragment == null) {
            MoPubLog.log(MoPubLog.SdkLogEvent.CUSTOM, getString(R.string.list_fragment_not_found));
            return; // fragment is not ready to update the consent
        }
        if (!listFragment.onChangeConsent(grant)) {
            return; // fragment is not ready to update the consent
        }

        final NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.getMenu().findItem(R.id.nav_privacy_grant).setChecked(grant);
        navigationView.getMenu().findItem(R.id.nav_privacy_revoke).setChecked(!grant);
    }

    private void onNavForceGdpr() {
        final PersonalInfoManager manager = MoPub.getPersonalInformationManager();
        if (manager != null) {
            manager.forceGdprApplies();
        }
    }

    private void onNavAdaptersInfo() {
        final FragmentManager manager = getSupportFragmentManager();
        if (manager.findFragmentByTag(NETWORKS_FRAGMENT_TAG) == null) {
            manager.beginTransaction()
                    .replace(R.id.fragment_container, new NetworksInfoFragment(), NETWORKS_FRAGMENT_TAG)
                    .addToBackStack(NETWORKS_FRAGMENT_TAG)
                    .commit();
        }
    }

    private void setEndpoint(@NonNull String host) {
        try {
            Field field = Reflection.getPrivateField(com.mopub.common.Constants.class, "HOST");
            field.set(null, host);
        } catch (Exception e) {
            MoPubLog.log(MoPubLog.SdkLogEvent.CUSTOM_WITH_THROWABLE, "Can't change HOST.", e);
        }
    }
}
