/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2015 The CyanogenMod Project
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

package com.android.systemui.qs.tiles;

import com.android.internal.logging.MetricsConstants;
import com.android.internal.logging.MetricsLogger;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.android.systemui.R;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSDetailItemsList;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.LocationController.LocationSettingsChangeCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cyanogenmod.app.StatusBarPanelCustomTile;

/** Quick settings tile: GPS **/
public class GpsTile extends QSTile<QSTile.BooleanState> {

    private static final Intent LOCATION_SETTINGS_INTENT =
            new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

    private final Icon mEnableIcon = ResourceIcon.get(R.drawable.ic_qs_gps_on);
    private final Icon mDisableIcon = ResourceIcon.get(R.drawable.ic_qs_gps_off);

    private final LocationController mController;
    private final KeyguardMonitor mKeyguard;
    private final Callback mCallback = new Callback();

    public GpsTile(Host host) {
        super(host);
        mController = host.getLocationController();
        mKeyguard = host.getKeyguardMonitor();
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mController.addSettingsChangedCallback(mCallback);
            mKeyguard.addCallback(mCallback);
        } else {
            mController.removeSettingsChangedCallback(mCallback);
            mKeyguard.removeCallback(mCallback);
        }
    }

    @Override
    protected void handleClick() {
        boolean wasEnabled = mController.isGpsEnabled();
        mController.setGpsEnabled(!wasEnabled);
        MetricsLogger.action(mContext, getMetricsCategory(), !wasEnabled);
        refreshState();
    }

    @Override
    protected void handleLongClick() {
        mHost.startActivityDismissingKeyguard(LOCATION_SETTINGS_INTENT);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final boolean gpsEnabled = mController.isGpsEnabled();

        state.visible = true;
        state.label = mContext.getString(R.string.quick_settings_gps_label);
        state.value = gpsEnabled;
        
        if (gpsEnabled) {
            state.contentDescription = mContext.getString(R.string.quick_settings_gps_on);
            state.icon = mEnableIcon;
        } else {
            state.contentDescription = mContext.getString(R.string.quick_settings_gps_off);
            state.icon = mDisableIcon;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_LOCATION;
    }

    @Override
    protected String composeChangeAnnouncement() {
        final boolean gpsEnabled = mController.isGpsEnabled();
        if (gpsEnabled) {
            return mContext.getString(R.string.accessibility_quick_settings_gps_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_gps_changed_off);
        }
    }

    private final class Callback implements LocationSettingsChangeCallback,
            KeyguardMonitor.Callback {
        @Override
        public void onLocationSettingsChanged(boolean enabled) {
            refreshState();
        }

        @Override
        public void onKeyguardChanged() {
            refreshState();
        }
    };
}
