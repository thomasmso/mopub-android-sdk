// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.RelativeLayout;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class VastVideoGradientStripWidgetTest {
    private Context context;
    private VastVideoGradientStripWidget subject;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
    }

    @Test
    public void constructor_whenForcePortrait_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void constructor_whenForceLandscape_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);

        // If not forcing orientation, visibility depends on device orientation,
        // which is initially ORIENTATION_UNDEFINED in tests
        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void constructor_whenUseDeviceOrientation_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);

        // If not forcing orientation, visibility depends on device orientation,
        // which is initially ORIENTATION_UNDEFINED in tests
        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void constructor_whenForceOrientationUndefined_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);

        // If force orientation undefined, visibility depends on device orientation,
        // which is initially ORIENTATION_UNDEFINED in tests
        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    // Video is still playing, forcing portrait orientation

    @Test
    public void onConfigurationChanged_whenForcePortraitAndDeviceInPortrait_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForcePortraitAndDeviceInLandscape_shouldBeVisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForcePortraitAndDeviceOrientationUndefined_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    // Video is still playing, forcing landscape orientation

    @Test
    public void onConfigurationChanged_whenForceLandscapeAndDeviceInPortrait_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForceLandscapeAndDeviceInLandscape_shouldBeVisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForceLandscapeAndDeviceOrientationUndefined_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    // Video is still playing, use device orientation

    @Test
    public void onConfigurationChanged_whenUseDeviceOrientationAndDeviceInPortrait_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenUseDeviceOrientationAndDeviceInLandscape_shouldBeVisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenUseDeviceOrientationAndDeviceOrientationUndefined_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    // Video is still playing, force orientation undefined

    @Test
    public void onConfigurationChanged_whenForceOrientationUndefinedAndDeviceInPortrait_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForceOrientationUndefinedAndDeviceInLandscape_shouldBeVisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForceOrientationUndefinedAndDeviceOrientationUndefined_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, View.VISIBLE, RelativeLayout.ALIGN_TOP, 0);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    // Video is complete

    @Test
    public void notifyVideoComplete_withCompanionAd_shouldSetVisibilityForCompanionAd() throws Exception {
        final int visibilityForCompanionAd = View.VISIBLE;
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                true, visibilityForCompanionAd, RelativeLayout.ALIGN_TOP, 0);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void notifyVideoComplete_withoutCompanionAd_shouldBeGone() throws Exception {
        final int visibilityForCompanionAd = View.VISIBLE;
        subject = new VastVideoGradientStripWidget(context, GradientDrawable.Orientation.TOP_BOTTOM,
                false, visibilityForCompanionAd, RelativeLayout.ALIGN_TOP, 0);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
    }
}
