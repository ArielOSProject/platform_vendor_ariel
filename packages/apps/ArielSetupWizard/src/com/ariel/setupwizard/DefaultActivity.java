/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.ariel.setupwizard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.util.Slog;
import android.content.pm.PackageManager.NameNotFoundException;
import android.app.backup.IBackupManager;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.RemoteException;

/**
 * Application that sets the provisioned bit, like SetupWizard does.
 */
public class DefaultActivity extends Activity {


    public static final String TAG = "DefaultActivity";

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.main);

    }

    public void onContinueButtonClick(View v) {

        setDeviceOwner();

        try {
            IBackupManager ibm = IBackupManager.Stub.asInterface(
                    ServiceManager.getService(Context.BACKUP_SERVICE));
            ibm.setBackupServiceActive(UserHandle.USER_OWNER, true);
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed activating backup service.", e);
        }

        PackageManager pm = getPackageManager();

        if(!isPackageInstalled("com.google.android.setupwizard", pm)){
            // provisioning complete!
            Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 1);
        }

        // Add a persistent setting to allow other apps to know the device has been provisioned.
        //Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
        //Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 1);

        // remove this activity from the package manager.
        ComponentName name = new ComponentName(this, DefaultActivity.class);
        pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        // terminate the activity.
        finish();
    }

    private boolean isPackageInstalled(String packagename, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packagename, 0);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    private void setDeviceOwner() {
        DevicePolicyManager mDPM =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        Slog.i(TAG, "Setting device owner info...");
        ComponentName cn = new ComponentName("com.ariel.guardian",
                "com.ariel.guardian.receivers.ArielDeviceAdminReceiver");

        try {
            // first, we need to set ourselves as active admin
            mDPM.setActiveAdmin(cn, true);

            // second, set ourselves as device owner
            // btw at this point bellow code wont work
            // because the upper statement will cause an exception :)
            boolean result = mDPM.setDeviceOwner(cn.getPackageName());
            if (result) {
                Slog.i(TAG, "Setting device owner success!");
            } else {
                Slog.i(TAG, "Setting device owner failed...");
            }

            Slog.i(TAG, "New device owner: " + mDPM.getDeviceOwner());
        } catch (IllegalStateException e) {
            Slog.e("ArielSystemServer", "Set active admin failed!!");
            e.printStackTrace();
        }

        Slog.i(TAG, "New device owner: " + mDPM.getDeviceOwner());
    }

}

