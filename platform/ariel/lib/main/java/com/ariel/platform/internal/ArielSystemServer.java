/**
 * Created by mikalackis on 7.3.17..
 * <p>
 * Copyright (C) 2016 The CyanogenMod Project
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Copyright (C) 2016 The CyanogenMod Project
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Copyright (C) 2016 The CyanogenMod Project
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

package com.ariel.platform.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.util.Slog;

import com.android.server.LocalServices;
import com.android.server.SystemServiceManager;
import com.android.server.SystemConfig;

import android.util.ArraySet;

import java.util.Iterator;

import android.os.IDeviceIdleController;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.app.backup.IBackupManager;
import android.os.UserHandle;

import android.os.RemoteException;

import com.ariel.platform.internal.common.ArielSystemServiceHelper;

import android.provider.Settings;
import android.app.admin.DevicePolicyManager;
import android.util.Slog;
import android.content.pm.PackageManager.NameNotFoundException;
import android.app.backup.IBackupManager;
import android.os.RemoteException;
import android.content.ComponentName;
import android.content.pm.PackageManager;

/**
 * Base CM System Server which handles the starting and states of various CM
 * specific system services. Since its part of the main looper provided by the system
 * server, it will be available indefinitely (until all the things die).
 */
public class ArielSystemServer {
    private static final String TAG = "ArielSystemServer";
    private Context mSystemContext;
    private ArielSystemServiceHelper mSystemServiceHelper;

    private static final String ENCRYPTING_STATE = "trigger_restart_min_framework";
    private static final String ENCRYPTED_STATE = "1";

    private static final String GOOGLE_BACKUP_TRANSPORT1 = "com.google.android.gms/.backup.BackupTransportService";
    private static final String GOOGLE_BACKUP_TRANSPORT2 = "com.google.android.backup/.BackupTransportService";

    private IntentFilter mBootFilter = new IntentFilter();
    private BroadcastReceiver mBootReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Slog.i(TAG, "I received: "+intent.getAction());
            initiArielOS();
        }
    };

    public ArielSystemServer(Context systemContext) {
        Slog.i(TAG, "ArielSystemServer initialized");
        mSystemContext = systemContext;
        mSystemServiceHelper = new ArielSystemServiceHelper(mSystemContext);
        mBootFilter.addAction(Intent.ACTION_PRE_BOOT_COMPLETED);
        mBootFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        mSystemContext.registerReceiver(mBootReceiver, mBootFilter);
        Slog.i(TAG, "Registered boot receiver");
    }

    public static boolean coreAppsOnly() {
        // Only run "core" apps+services if we're encrypting the device.
        final String cryptState = SystemProperties.get("vold.decrypt");
        return ENCRYPTING_STATE.equals(cryptState) ||
                ENCRYPTED_STATE.equals(cryptState);
    }

    /**
     * Invoked via reflection by the SystemServer
     */
    private void run() {
        // Start services.
        try {
            Slog.i(TAG, "ArielSystemServer starting services...");
            try {
                IBackupManager ibm = IBackupManager.Stub.asInterface(
                        ServiceManager.getService(Context.BACKUP_SERVICE));
                ibm.setBackupServiceActive(UserHandle.USER_OWNER, true);
            } catch (RemoteException e) {
                throw new IllegalStateException("Failed activating backup service.", e);
            }
            startServices();
        } catch (Throwable ex) {
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting cm system services", ex);
            throw ex;
        }
    }

    private void initiArielOS() {
        PackageManager pm = mSystemContext.getPackageManager();

        int isDeviceProvisioned = Settings.Global.getInt(mSystemContext.getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 0);

        if (isDeviceProvisioned == 0) {
            setDeviceOwner();
            try {
                IBackupManager ibm = IBackupManager.Stub.asInterface(
                        ServiceManager.getService(Context.BACKUP_SERVICE));
                ibm.setBackupServiceActive(UserHandle.USER_OWNER, true);

                // try to find google backup transport
                // and set it, only if google apps are installed
                String[] availableTransports = ibm.listAllTransports();

                boolean found = false;

                for (int i = 0; i < availableTransports.length; i++) {
                    String tmpTransport = availableTransports[i];
                    Slog.i(TAG, "Checking transport: " + tmpTransport);
                    if (tmpTransport.equals(GOOGLE_BACKUP_TRANSPORT1) ||
                            tmpTransport.equals(GOOGLE_BACKUP_TRANSPORT2)) {
                        Slog.i(TAG, "Bingo! Google backup transport found");
                        // this is the one we need, set it
                        ibm.selectBackupTransport(tmpTransport);
                        found = true;
                        break;
                    } else {
                        // this is weird, it has google but not the one we know about
                        Slog.i(TAG, "Weird! Backup transport " +
                                "found but not the one we need: " + tmpTransport);
                    }
                }

                if (!found) {
                    Slog.i(TAG, "We didnt find google backup while gapps are present. Force.");
                    ibm.selectBackupTransport(GOOGLE_BACKUP_TRANSPORT1);
                } else {
                    Slog.i(TAG, "All cool, google backup transport set");
                }
            } catch (RemoteException e) {
                throw new IllegalStateException("Failed activating backup service.", e);
            }

        }
        else{
            Slog.i(TAG, "Device already provisioned!");
        }

        // Add a persistent setting to allow other apps to know the device has been provisioned.
        //Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
        //Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 1);

        // remove this activity from the package manager.
//        ComponentName name = new ComponentName(this, DefaultActivity.class);
//        pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
//                PackageManager.DONT_KILL_APP);

    }

    private void setDeviceOwner() {
        DevicePolicyManager mDPM =
                (DevicePolicyManager) mSystemContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        Slog.i(TAG, "Setting device owner info...");
        ComponentName cn = new ComponentName("com.ariel.guardian",
                "com.ariel.guardian.receivers.ArielDeviceAdminReceiver");

        try {
            // first, we need to set ourselves as active admin
            mDPM.setActiveAdmin(cn, true);

            // second, set ourselves as device owner
            // btw at this point bellow code wont work
            // because the upper statement will cause an exception :)
            boolean result = mDPM.setDeviceOwner(cn, "ArielGuardian");
            if (result) {
                Slog.i(TAG, "Setting device owner success!");
            } else {
                Slog.i(TAG, "Setting device owner failed...");
            }

            Slog.i(TAG, "New device owner: " + mDPM.getDeviceOwner());
        } catch (IllegalStateException e) {
            Slog.e("ArielSystemServer", "Set active admin failed!!");
            e.printStackTrace();
        } catch (Exception e) {
            Slog.e("ArielSystemServer", "Set active admin failed!!");
            e.printStackTrace();
        }

        Slog.i(TAG, "New device owner: " + mDPM.getDeviceOwner());
    }

    private void startServices() {
        final Context context = mSystemContext;
        final SystemServiceManager ssm = LocalServices.getService(SystemServiceManager.class);
        String[] externalServices = context.getResources().getStringArray(
                com.ariel.platform.internal.R.array.config_externalArielServices);

        for (String service : externalServices) {
            try {
                Slog.i(TAG, "Attempting to start service " + service);
                ArielSystemService arielSystemService = mSystemServiceHelper.getServiceFor(service);
                if (context.getPackageManager().hasSystemFeature(
                        arielSystemService.getFeatureDeclaration())) {
                    if (coreAppsOnly() && !arielSystemService.isCoreService()) {
                        Slog.d(TAG, "Not starting " + service +
                                " - only parsing core apps");
                    } else {
                        Slog.i(TAG, "Starting service " + service);
                        ssm.startService(arielSystemService.getClass());
                    }
                } else {
                    Slog.i(TAG, "Not starting service " + service +
                            " due to feature not declared on device");
                }
            } catch (Throwable e) {
                reportWtf("starting " + service, e);
            }
        }

        Slog.i(TAG, "ArielSystemServer services started!");
    }

    private void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        Slog.wtf(TAG, "BOOT FAILURE " + msg, e);
    }

}
