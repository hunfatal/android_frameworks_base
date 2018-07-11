/*
 * Copyright (C) 2018 The ResurrectionRemix Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.widget.Switch;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.R.drawable;
import com.android.systemui.qs.QSHost;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.LocationController.LocationChangeCallback;

import org.lineageos.internal.logging.LineageMetricsLogger;

/** Quick settings tile: Gps **/
public class GpsTile extends QSTileImpl<BooleanState> {
    private final Icon mEnableIcon = ResourceIcon.get(R.drawable.ic_qs_gps_on);
    private final Icon mDisableIcon = ResourceIcon.get(R.drawable.ic_qs_gps_off);

    private static final Intent LOCATION_SETTINGS_INTENT =
            new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

    private final LocationController mController;
    private final KeyguardMonitor mKeyguard;
    private final Callback mCallback = new Callback();

    public GpsTile(QSHost host) {
        super(host);
        mController = Dependency.get(LocationController.class);
        mKeyguard = Dependency.get(KeyguardMonitor.class);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick() {
        final boolean wasEnabled = mState.value;
        MetricsLogger.action(mContext, getMetricsCategory(), !wasEnabled);
        mController.setGpsEnabled(!wasEnabled);
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (mController == null) {
            return;
        }

        final boolean gpsEnabled = mController.isGpsEnabled();

        // Work around for bug 15916487: don't show location tile on top of lock screen. After the
        // bug is fixed, this should be reverted to only hiding it on secure lock screens:
        // state.visible = !(mKeyguard.isSecure() && mKeyguard.isShowing());
        state.value = gpsEnabled;
        checkIfRestrictionEnforcedByAdminOnly(state, UserManager.DISALLOW_SHARE_LOCATION);
        
        state.label = mContext.getString(R.string.quick_settings_gps_label);
        
        if (gpsEnabled) {
            state.contentDescription = mContext.getString(R.string.quick_settings_gps_on);
            state.icon = mEnableIcon;
        } else {
            state.contentDescription = mContext.getString(R.string.quick_settings_gps_off);
            state.icon = mDisableIcon;
        }

        state.expandedAccessibilityClassName = Switch.class.getName();
    }
    
    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_gps_label);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_LOCATION;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_gps_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_gps_changed_off);
        }
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (mController == null) {
            return;
        }

        if (listening) {
            mController.addCallback(mCallback);
            mKeyguard.addCallback(mCallback);
        } else {
            mController.removeCallback(mCallback);
            mKeyguard.removeCallback(mCallback);
        }        
    }

    private final class Callback implements LocationChangeCallback, KeyguardMonitor.Callback {
        @Override
        public void onLocationSettingsChanged(boolean enabled) {
            refreshState();
        }

        @Override
        public void onKeyguardShowingChanged() {
            refreshState();
        }
    }
}
