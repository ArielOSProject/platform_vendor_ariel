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

import android.content.Context;
import android.os.SystemProperties;
import android.util.Slog;

import com.android.server.LocalServices;
import com.android.server.SystemServiceManager;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;

import com.ariel.platform.internal.common.ArielSystemServiceHelper;

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

    public ArielSystemServer(Context systemContext) {
        Slog.i(TAG, "ArielSystemServer initialized");
        mSystemContext = systemContext;
        mSystemServiceHelper = new ArielSystemServiceHelper(mSystemContext);
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
            startServices();
            setDeviceOwner();
        } catch (Throwable ex) {
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting cm system services", ex);
            throw ex;
        }
    }

    private void setDeviceOwner() {
        final Context context = mSystemContext;
        DevicePolicyManager mDPM =
                (DevicePolicyManager) mSystemContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        Slog.i(TAG, "Setting device owner info...");
        ComponentName cn = new ComponentName("com.ariel.guardian",
                "com.ariel.guardian.receivers.ArielDeviceAdminReceiver");

        if(mDPM.isDeviceOwnerApp("com.ariel.guardian")){
            Slog.i(TAG, "We are the owner, all cool");
        }
        else{
            Slog.i(TAG, "We are not the owner, take us in");
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

                //we nuke this exception and continue
                try{
                    //if the admin is activated, set the owner
                    if(mDPM.isAdminActive(cn)){
                        boolean result = mDPM.setDeviceOwner(cn, "ArielGuardian");
                        if (result) {
                            Slog.i(TAG, "Setting device owner success!");
                        } else {
                            Slog.i(TAG, "Setting device owner failed...");
                        }
                    }
                }
                catch(IllegalStateException ex){
                    Slog.e("ArielSystemServer", "IllegalState device owner!!!!!!");
                    ex.printStackTrace();
                }

                Slog.i(TAG, "New device owner: " + mDPM.getDeviceOwner());
            }
            catch(Exception e){
                Slog.e("ArielSystemServer", "Error setting device owner!!!!!!");
                e.printStackTrace();
            }
        }
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
