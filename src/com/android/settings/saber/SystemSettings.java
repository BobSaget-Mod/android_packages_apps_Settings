/*
 * Copyright (C) 2012 The CyanogenMod project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.saber;

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemSettings extends SettingsPreferenceFragment {
    private static final String TAG = "SystemSettings";

    private static final String KEY_USER_INTERFACE = "user_interface";
    private static final String KEY_STATUS_BAR = "status_bar";
    private static final String KEY_NAVIGATION_BAR_CATEGORY = "navigation_bar_category";
    private static final String KEY_NAVIGATION_BAR = "navigation_bar";
    private static final String KEY_LOCKSCREEN_CATEGORY = "lockscreen_category";
    private static final String KEY_NAVIGATION_RING = "navigation_ring";
    private static final String KEY_LOCKSCREEN_TARGETS = "lockscreen_targets";
    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String KEY_VOLUME_ROCKER_SETTINGS = "volume_rocker_settings";
    private static final String KEY_NOTIFICATION_PULSE_CATEGORY = "category_notification_pulse";
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_PIE_CONTROL = "pie_control";
    private static final String KEY_POWER_MENU = "power_menu";
    private static final String KEY_NOTIFICATION_DRAWER = "notification_drawer";
    private static final String KEY_NOTIFICATION_DRAWER_TABLET = "notification_drawer_tablet";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    private static final String KEY_QUICK_UNLOCK = "quick_unlock";

    private PreferenceScreen mUserInterface;
    private PreferenceScreen mNotificationPulse;
    private PreferenceScreen mPieControl;
    private PreferenceScreen mPhoneDrawer;
    private PreferenceScreen mTabletDrawer;
    private CheckBoxPreference mQuickUnlock;
    private Context mContext;

    private final Configuration mCurConfig = new Configuration();
    private boolean mPrimaryUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_settings);
        PreferenceScreen prefScreen = getPreferenceScreen();
        mContext = getActivity();

        mPhoneDrawer = (PreferenceScreen) findPreference(KEY_NOTIFICATION_DRAWER);
        mTabletDrawer = (PreferenceScreen) findPreference(KEY_NOTIFICATION_DRAWER_TABLET);

        /*if (Utils.isTablet(getActivity())) {
            if (mPhoneDrawer != null) {
                getPreferenceScreen().removePreference(mPhoneDrawer);
            }
        } else*/ {
            if (mTabletDrawer != null) {
                getPreferenceScreen().removePreference(mTabletDrawer);
            }
        }

        // Dont display the lock clock preference if its not installed
        removePreferenceIfPackageNotInstalled(findPreference(KEY_LOCK_CLOCK));
        
        // USER_OWNER is logged in
        mPrimaryUser = UserHandle.myUserId() == UserHandle.USER_OWNER;
        if (mPrimaryUser) {
            // do nothing, show all settings
        } else {
            // NON USER_OWNER is logged in
            // remove non multi-user compatible settings
            prefScreen.removePreference(findPreference(KEY_STATUS_BAR));
            prefScreen.removePreference(findPreference(KEY_PIE_CONTROL));
            prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR));
            prefScreen.removePreference(findPreference(KEY_NAVIGATION_RING));
            prefScreen.removePreference((PreferenceCategory) findPreference(KEY_NAVIGATION_BAR_CATEGORY));
            prefScreen.removePreference(findPreference(KEY_POWER_MENU));
        }

        mUserInterface = (PreferenceScreen) findPreference(KEY_USER_INTERFACE);

        // Preferences that applies to all users
        // Notification lights
        mNotificationPulse = (PreferenceScreen) findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse != null) {
            if (!getResources().getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
                prefScreen.removePreference(mNotificationPulse);
                prefScreen.removePreference((PreferenceCategory) findPreference(KEY_NOTIFICATION_PULSE_CATEGORY));
                mNotificationPulse = null;
            }
        }
        
        // Pie controls
        mPieControl = (PreferenceScreen) findPreference(KEY_PIE_CONTROL);

        mQuickUnlock = (CheckBoxPreference) findPreference(KEY_QUICK_UNLOCK);
        mQuickUnlock.setChecked(Settings.System.getInt(mContext.getApplicationContext().getContentResolver(),
                Settings.System.LOCKSCREEN_QUICK_UNLOCK, 0) != 0);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mQuickUnlock) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_QUICK_UNLOCK,
                    mQuickUnlock.isChecked() ? 1 : 0);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateLightPulseDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 0) == 1) {
            mNotificationPulse.setSummary(getString(R.string.notification_light_enabled));
        } else {
            mNotificationPulse.setSummary(getString(R.string.notification_light_disabled));
        }
    }

    private void updatePieControlDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.PIE_CONTROLS, 0) == 1) {
            mPieControl.setSummary(getString(R.string.pie_control_enabled));
        } else {
            mPieControl.setSummary(getString(R.string.pie_control_disabled));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // All users
        if (mNotificationPulse != null) {
            updateLightPulseDescription();
        }
        if (mPieControl != null) {
            updatePieControlDescription();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private boolean removePreferenceIfPackageNotInstalled(Preference preference) {
        String intentUri = ((PreferenceScreen) preference).getIntent().toUri(1);
        Pattern pattern = Pattern.compile("component=([^/]+)/");
        Matcher matcher = pattern.matcher(intentUri);

        String packageName = matcher.find() ? matcher.group(1) : null;
        if (packageName != null) {
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "package " + packageName + " not installed, hiding preference.");
                getPreferenceScreen().removePreference(preference);
                return true;
            }
        }
        return false;
    }
}
