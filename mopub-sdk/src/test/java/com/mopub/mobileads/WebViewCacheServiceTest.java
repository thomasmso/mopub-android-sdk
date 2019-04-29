// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.os.Handler;

import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mraid.MraidController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(SdkTestRunner.class)
public class WebViewCacheServiceTest {

    @Mock private BaseWebView baseWebView;
    @Mock private Interstitial interstitial;
    @Mock private ExternalViewabilitySessionManager viewabilityManager;
    @Mock private Handler handler;
    @Mock private MraidController mraidController;
    private long broadcastIdentifier;

    @Before
    public void setUp() throws Exception {
        WebViewCacheService.clearAll();
        WebViewCacheService.setHandler(handler);
        broadcastIdentifier = 12345;
    }

    @Test
    public void storeWebView_shouldPopulateMap() {
        WebViewCacheService.storeWebViewConfig(broadcastIdentifier, interstitial,
                baseWebView, viewabilityManager, mraidController);

        final Map<Long, WebViewCacheService.Config> configs = WebViewCacheService.getWebViewConfigs();
        assertThat(configs.size()).isEqualTo(1);
        assertThat(configs.get(broadcastIdentifier).getWebView()).isEqualTo(baseWebView);
        assertThat(configs.get(broadcastIdentifier).getWeakInterstitial().get()).isEqualTo(interstitial);
        assertThat(configs.get(broadcastIdentifier).getViewabilityManager()).isEqualTo(viewabilityManager);
        assertThat(configs.get(broadcastIdentifier).getController()).isEqualTo(mraidController);
    }

    @Test
    public void storeWebView_withEmptyCache_shouldNotSetRunnableForTrimCache() {
        WebViewCacheService.storeWebViewConfig(broadcastIdentifier, interstitial,
                baseWebView, viewabilityManager, mraidController);

        verifyZeroInteractions(handler);
        final Map<Long, WebViewCacheService.Config> configs = WebViewCacheService.getWebViewConfigs();
        assertThat(configs.size()).isEqualTo(1);
        assertThat(configs.get(broadcastIdentifier).getWebView()).isEqualTo(baseWebView);
        assertThat(configs.get(broadcastIdentifier).getWeakInterstitial().get()).isEqualTo(interstitial);
        assertThat(configs.get(broadcastIdentifier).getViewabilityManager()).isEqualTo(viewabilityManager);
        assertThat(configs.get(broadcastIdentifier).getController()).isEqualTo(mraidController);
    }

    @Test
    public void storeWebView_withNonEmptyCache_shouldSetRunnableForTrimCache() {
        WebViewCacheService.storeWebViewConfig(broadcastIdentifier, interstitial,
                baseWebView, viewabilityManager, mraidController);
        WebViewCacheService.storeWebViewConfig(broadcastIdentifier + 1, interstitial,
                baseWebView, viewabilityManager, mraidController);

        verify(handler).removeCallbacks(WebViewCacheService.sTrimCacheRunnable);
        verify(handler).postDelayed(WebViewCacheService.sTrimCacheRunnable,
                WebViewCacheService.TRIM_CACHE_FREQUENCY_MILLIS);
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void storeWebView_withMaxSizeReached_shouldTrimCache_shouldIgnoreStoreRequest() {
        for(int i = 0; i < WebViewCacheService.MAX_SIZE; i++) {
            WebViewCacheService.storeWebViewConfig(broadcastIdentifier + i, interstitial,
                    baseWebView, viewabilityManager, mraidController);
        }
        final Map<Long, WebViewCacheService.Config> configs = WebViewCacheService.getWebViewConfigs();
        assertThat(configs.size()).isEqualTo(WebViewCacheService.MAX_SIZE);

        WebViewCacheService.storeWebViewConfig(broadcastIdentifier - 1, interstitial, baseWebView,
                viewabilityManager, mraidController);

        // This is called MAX_SIZE - 1 times since trim() is not called on the first run due to
        // the maps being empty. And then this is called an additional time to test the one
        // after MAX_SIZE is reached.
        verify(handler, times(WebViewCacheService.MAX_SIZE)).removeCallbacks(
                WebViewCacheService.sTrimCacheRunnable);
        verify(handler, times(WebViewCacheService.MAX_SIZE)).postDelayed(
                WebViewCacheService.sTrimCacheRunnable,
                WebViewCacheService.TRIM_CACHE_FREQUENCY_MILLIS);

        assertThat(configs.size()).isEqualTo(WebViewCacheService.MAX_SIZE);
        assertThat(configs.get(broadcastIdentifier - 1)).isNull();
    }

    @Test
    public void popWebView_shouldReturnWebView_shouldRemoveMappings() {
        WebViewCacheService.storeWebViewConfig(broadcastIdentifier, interstitial,
                baseWebView, viewabilityManager, mraidController);

        final WebViewCacheService.Config result = WebViewCacheService.popWebViewConfig(broadcastIdentifier);

        assertThat(WebViewCacheService.getWebViewConfigs()).isEmpty();
        assertThat(result.getWebView()).isEqualTo(baseWebView);
        assertThat(result.getWeakInterstitial().get()).isEqualTo(interstitial);
        assertThat(result.getViewabilityManager()).isEqualTo(viewabilityManager);
        assertThat(result.getController()).isEqualTo(mraidController);

    }

    @Test
    public void trimCache_shouldRemoveStaleWebViews() {
        WebViewCacheService.storeWebViewConfig(broadcastIdentifier, interstitial,
                baseWebView, viewabilityManager, mraidController);
        WebViewCacheService.storeWebViewConfig(broadcastIdentifier + 1, mock(ResponseBodyInterstitial.class),
                baseWebView, viewabilityManager, mraidController);

        final Map<Long, WebViewCacheService.Config> configs = WebViewCacheService.getWebViewConfigs();
        // This clears the WeakReference, which allows the cache to remove the WebView associated
        // with that interstitial.
        configs.get(broadcastIdentifier + 1).getWeakInterstitial().clear();

        WebViewCacheService.trimCache();

        final Map<Long, WebViewCacheService.Config> configsResult = WebViewCacheService.getWebViewConfigs();

        assertThat(configsResult.size()).isEqualTo(1);
        assertThat(configs.get(broadcastIdentifier).getWebView()).isEqualTo(baseWebView);
        assertThat(configs.get(broadcastIdentifier).getWeakInterstitial().get()).isEqualTo(interstitial);
        assertThat(configs.get(broadcastIdentifier).getViewabilityManager()).isEqualTo(viewabilityManager);
        assertThat(configs.get(broadcastIdentifier).getController()).isEqualTo(mraidController);
        assertThat(configsResult.get(broadcastIdentifier + 1)).isNull();
    }
}
