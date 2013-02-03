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

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class SystemSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_NAVIGATION_BAR = "navigation_bar";
    private static final String KEY_NAV_BUTTONS_EDIT = "nav_buttons_edit";
    private static final String KEY_NAV_BUTTONS_HEIGHT = "nav_buttons_height";
    private static final String QUICK_SETTINGS_CATEGORY = "quick_settings_category";
    private static final String QUICK_PULLDOWN = "quick_pulldown";
    private static final String KEY_NOTIFICATION_PULSE_CATEGORY = "category_notification_pulse";
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";

    private ListPreference mNavButtonsHeight;
    private PreferenceScreen mNotificationPulse;
    PreferenceCategory mQuickSettingsCategory;
    ListPreference mQuickPulldown;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_settings);

        mNavButtonsHeight = (ListPreference) findPreference(KEY_NAV_BUTTONS_HEIGHT);
        mNavButtonsHeight.setOnPreferenceChangeListener(this);
        int statusNavButtonsHeight = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                 Settings.System.NAV_BUTTONS_HEIGHT, 48);
        mNavButtonsHeight.setValue(String.valueOf(statusNavButtonsHeight));
        mNavButtonsHeight.setSummary(mNavButtonsHeight.getEntry());

        // Notification lights
        mNotificationPulse = (PreferenceScreen) findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse != null) {
            if (!getResources().getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
                getPreferenceScreen().removePreference(mNotificationPulse);
                getPreferenceScreen().removePreference((PreferenceCategory) findPreference(KEY_NOTIFICATION_PULSE_CATEGORY));
            } else {
                updateLightPulseDescription();
            }
        }
        
        // Quick Settings category and pull down. Only show on phones
        mQuickSettingsCategory = (PreferenceCategory) getPreferenceScreen().findPreference(QUICK_SETTINGS_CATEGORY);
        mQuickPulldown = (ListPreference) getPreferenceScreen().findPreference(QUICK_PULLDOWN);
        if (!Utils.isPhone(getActivity())) {
            if(mQuickPulldown != null)
                getPreferenceScreen().removePreference(mQuickPulldown);
                getPreferenceScreen().removePreference((PreferenceCategory) findPreference(QUICK_SETTINGS_CATEGORY));
            } else {
                mQuickPulldown.setOnPreferenceChangeListener(this);
                int quickPulldownValue = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.QS_QUICK_PULLDOWN, 0);
                mQuickPulldown.setValue(String.valueOf(quickPulldownValue));
                updatePulldownSummary(quickPulldownValue);
                }

        // Only show the hardware keys config on a device that does not have a navbar
        boolean removeKeys = false;
        boolean removeNavbar = false;
        IWindowManager windowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            if (windowManager.hasNavigationBar()) {
                removeKeys = true;
            } else {
                removeNavbar = true;
            }
        } catch (RemoteException e) {
            // Do nothing
        }

        // Act on the above
        if (removeNavbar) {
            getPreferenceScreen().removePreference(findPreference(KEY_NAVIGATION_BAR));
        }
    }

   private void updateLightPulseDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 0) == 1) {
            mNotificationPulse.setSummary(getString(R.string.notification_light_enabled));
        } else {
            mNotificationPulse.setSummary(getString(R.string.notification_light_disabled));
        }
    }

    private void updatePulldownSummary(int value) {
        Resources res = getResources();
        if (value == 0) {
            /* quick pulldown deactivated */
            mQuickPulldown.setSummary(res.getString(R.string.quick_pulldown_off));
        } else {
            String direction = res.getString(value == 2
                    ? R.string.quick_pulldown_summary_left
                    : R.string.quick_pulldown_summary_right);
            mQuickPulldown.setSummary(res.getString(R.string.summary_quick_pulldown, direction));
        }
    }

     public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mNavButtonsHeight) {
            int statusNavButtonsHeight = Integer.valueOf((String) objValue);
            int index = mNavButtonsHeight.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.NAV_BUTTONS_HEIGHT, statusNavButtonsHeight);
            mNavButtonsHeight.setSummary(mNavButtonsHeight.getEntries()[index]);
            return true;
            } else if (preference == mQuickPulldown) {
                int quickPulldownValue = Integer.valueOf((String) objValue);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.QS_QUICK_PULLDOWN, quickPulldownValue);
                updatePulldownSummary(quickPulldownValue);
                return true;
                }
        return false;
        }

    @Override
    public void onResume() {
        super.onResume();
        updateLightPulseDescription();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
