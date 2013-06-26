/*
 * Copyright (C) 2012 The CyanogenMod Project
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

import android.app.ActivityManager;
import android.app.INotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class UserInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String USER_INTERFACE_CATEGORY_DISPLAY = "user_interface_category_display";
    private static final String DUAL_PANE_PREFS = "dual_pane_prefs";
    private static final String KEY_WAKEUP_WHEN_PLUGGED_UNPLUGGED = "wakeup_when_plugged_unplugged";
    private static final String LED_CATEGORY_GENERAL = "led_category_general";
    private static final String KEY_CHARGING_LED_ENABLED = "charging_led_enabled";
    private static final String KEY_LOW_BATTERY_LED_PULSE_ENABLED = "low_battery_led_pulse_enabled";
    private static final String USER_INTERFACE_CATEGORY_HALO = "user_interface_category_halo";
    private static final String KEY_HALO_ENABLED = "halo_enabled";
    private static final String KEY_HALO_STATE = "halo_state";
    private static final String KEY_HALO_HIDE = "halo_hide";
    private static final String KEY_HALO_REVERSED = "halo_reversed";
    private static final String KEY_HALO_PAUSE = "halo_pause";
    private static final String KEY_LOW_BATTERY_WARNING_POLICY = "pref_low_battery_warning_policy";

    private PreferenceCategory mUserInterfaceDisplay;
    private ListPreference mDualPanePrefs;
    private CheckBoxPreference mWakeUpWhenPluggedOrUnplugged;
    private CheckBoxPreference mChargingLedEnabled;
    private CheckBoxPreference mLowBatteryLedPulseEnabled;
    private PreferenceCategory mUserInterfaceHalo;
    private CheckBoxPreference mHaloEnabled;
    private ListPreference mHaloState;
    private CheckBoxPreference mHaloHide;
    private CheckBoxPreference mHaloReversed;
    private CheckBoxPreference mHaloPause;
    private INotificationManager mNotificationManager;
    private ListPreference mLowBatteryWarning;
    private boolean mPrimaryUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.user_interface);

        PreferenceScreen prefSet = getPreferenceScreen();

        // General
        // Dual pane, only show on selected devices
        mDualPanePrefs = (ListPreference) prefSet.findPreference(DUAL_PANE_PREFS);
        if (mDualPanePrefs != null) {
            if (!getResources().getBoolean(R.bool.config_show_user_interface_dual_pane)) {
                getPreferenceScreen().removePreference(mDualPanePrefs);
                mDualPanePrefs = null;
            } else {
                mDualPanePrefs.setOnPreferenceChangeListener(this);
                int dualPanePrefsValue = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.DUAL_PANE_PREFS, 0);
                mDualPanePrefs.setValue(String.valueOf(dualPanePrefsValue));
                updateDualPanePrefs(dualPanePrefsValue);
            }
        }

        mNotificationManager = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));

        mHaloEnabled = (CheckBoxPreference) findPreference(KEY_HALO_ENABLED);

        mHaloState = (ListPreference) findPreference(KEY_HALO_STATE);

        mHaloHide = (CheckBoxPreference) findPreference(KEY_HALO_HIDE);

        mHaloReversed = (CheckBoxPreference) findPreference(KEY_HALO_REVERSED);

        int isLowRAM = (ActivityManager.isLargeRAM()) ? 0 : 1;
        mHaloPause = (CheckBoxPreference) prefSet.findPreference(KEY_HALO_PAUSE);

        mUserInterfaceHalo = (PreferenceCategory) prefSet.findPreference(USER_INTERFACE_CATEGORY_HALO);

        // USER_OWNER is logged in
        mPrimaryUser = UserHandle.myUserId() == UserHandle.USER_OWNER;
        if (mPrimaryUser) {
            // do nothing, show all settings
        } else {
            // NON USER_OWNER is logged in
            // remove non multi-user compatible settings
            prefSet.removePreference(findPreference(KEY_HALO_ENABLED));
            prefSet.removePreference(findPreference(KEY_HALO_STATE));
            prefSet.removePreference(findPreference(KEY_HALO_HIDE));
            prefSet.removePreference(findPreference(KEY_HALO_REVERSED));
            prefSet.removePreference((PreferenceCategory) findPreference(USER_INTERFACE_CATEGORY_HALO));
        }

        // Display
        // Wake up plugged/unplugged
        mUserInterfaceDisplay = (PreferenceCategory) prefSet.findPreference(USER_INTERFACE_CATEGORY_DISPLAY);
        mWakeUpWhenPluggedOrUnplugged = (CheckBoxPreference) findPreference(KEY_WAKEUP_WHEN_PLUGGED_UNPLUGGED);
        mWakeUpWhenPluggedOrUnplugged.setChecked(Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.WAKEUP_WHEN_PLUGGED_UNPLUGGED, 1) == 1);

        mLowBatteryWarning = (ListPreference) findPreference(KEY_LOW_BATTERY_WARNING_POLICY);
        int lowBatteryWarning = Settings.System.getInt(getActivity().getContentResolver(),
                                    Settings.System.POWER_UI_LOW_BATTERY_WARNING_POLICY, 0);
        mLowBatteryWarning.setValue(String.valueOf(lowBatteryWarning));
        mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntry());
        mLowBatteryWarning.setOnPreferenceChangeListener(this); 

        // LED
        if (!getResources().getBoolean(R.bool.config_show_Led)) {
            getPreferenceScreen().removePreference((PreferenceCategory) findPreference(LED_CATEGORY_GENERAL));
        }

        mChargingLedEnabled = (CheckBoxPreference) findPreference(KEY_CHARGING_LED_ENABLED);
        if (mChargingLedEnabled != null) {
            if (!getResources().getBoolean(R.bool.config_show_Led)) {
                getPreferenceScreen().removePreference(mChargingLedEnabled);
                mChargingLedEnabled = null;
            } else {
            mChargingLedEnabled.setChecked(Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.CHARGING_LED_ENABLED, 0) != 0);
            }
        }

        mLowBatteryLedPulseEnabled = (CheckBoxPreference) findPreference(KEY_LOW_BATTERY_LED_PULSE_ENABLED);
        if (mLowBatteryLedPulseEnabled != null) {
            if (!getResources().getBoolean(R.bool.config_show_Led)) {
                getPreferenceScreen().removePreference(mLowBatteryLedPulseEnabled);
                mLowBatteryLedPulseEnabled = null;
            } else {
            mLowBatteryLedPulseEnabled.setChecked(Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOW_BATTERY_LED_PULSE_ENABLED, 1) == 1);
            }
        }

 
        mHaloEnabled.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.HALO_ENABLED, 0) == 1);

        mHaloState.setValue(String.valueOf((isHaloPolicyBlack() ? "1" : "0")));
        mHaloState.setOnPreferenceChangeListener(this);

        mHaloHide.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.HALO_HIDE, 0) == 1);

        mHaloReversed.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.HALO_REVERSED, 1) == 1);

        mHaloPause.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.HALO_PAUSE, isLowRAM) == 1);
    }

    private void updateDualPanePrefs(int value) {
        Resources res = getResources();
        if (value == 0) {
            /* dual pane deactivated */
            mDualPanePrefs.setSummary(res.getString(R.string.dual_pane_prefs_off));
        } else {
            String direction = res.getString(value == 2
                    ? R.string.dual_pane_prefs_landscape
                    : R.string.dual_pane_prefs_on);
            mDualPanePrefs.setSummary(res.getString(R.string.dual_pane_prefs_summary, direction));
        }
    }

    private boolean isHaloPolicyBlack() {
        try {
            return mNotificationManager.isHaloPolicyBlack();
        } catch (android.os.RemoteException ex) {
                // System dead
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mWakeUpWhenPluggedOrUnplugged) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.WAKEUP_WHEN_PLUGGED_UNPLUGGED,
                    mWakeUpWhenPluggedOrUnplugged.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mChargingLedEnabled) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.CHARGING_LED_ENABLED,
                    mChargingLedEnabled.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mLowBatteryLedPulseEnabled) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOW_BATTERY_LED_PULSE_ENABLED,
                    mLowBatteryLedPulseEnabled.isChecked() ? 1 : 0);
            return true;
        } else if  (preference == mHaloEnabled) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HALO_ENABLED,
                    mHaloEnabled.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mHaloHide) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HALO_HIDE,
                    mHaloHide.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mHaloReversed) {  
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HALO_REVERSED,
                    mHaloReversed.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mHaloPause) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HALO_PAUSE,
                    mHaloPause.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mDualPanePrefs) {
            int dualPanePrefsValue = Integer.valueOf((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.DUAL_PANE_PREFS, dualPanePrefsValue);
            updateDualPanePrefs(dualPanePrefsValue);
            getActivity().recreate();
            return true;
        } else if (preference == mLowBatteryWarning) {
            int lowBatteryWarning = Integer.valueOf((String) objValue);
            int index = mLowBatteryWarning.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_UI_LOW_BATTERY_WARNING_POLICY,
                    lowBatteryWarning);
            mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntries()[index]);
            return true;
       } else if (preference == mHaloState) {
            boolean state = Integer.valueOf((String) objValue) == 1;
            try {
                mNotificationManager.setHaloPolicyBlack(state);
            } catch (android.os.RemoteException ex) {
                // System dead
            }
            return true;
        }

        return false;
    }
}
