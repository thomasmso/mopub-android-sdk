// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.app.Activity;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class PersonalInfoDataTest {

    Activity activity;
    PersonalInfoData subject;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).get();
        subject = new PersonalInfoData(activity, "adUnit");
    }

    @Test
    public void replaceLanguageMacro_withIncorrectLanguageMacro_shouldKeepStringAsIs() {
        String result = PersonalInfoData.replaceLanguageMacro(
                "someurl%LANGUAGE%%://%%LANGUAGE%/someLANGUAGE",
                activity, null);

        assertThat(result).isEqualTo("someurl%LANGUAGE%%://%%LANGUAGE%/someLANGUAGE");
    }

    @Test
    public void replaceLanguageMacro_withLanguageMacro_shouldReplaceLanguageMacro() {
        String result = PersonalInfoData.replaceLanguageMacro("someurl://%%LANGUAGE%%/somepath",
                activity, null);

        assertThat(result).isEqualTo("someurl://en/somepath");
    }
}
