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

package com.arielos.platform.internal;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.SystemServiceManager;

import com.arielos.platform.internal.common.ArielSystemServiceHelper;

/**
 * Base Lineage System Server which handles the starting and states of various Lineage
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
        mSystemContext = systemContext;
        mSystemServiceHelper = new ArielSystemServiceHelper(mSystemContext);
    }

    public static boolean coreAppsOnly() {
        // Only run "core" apps+services if we're encrypting the device.
        final String cryptState = SystemProperties.get("vold.decrypt");
        final boolean isAlarmBoot = SystemProperties.getBoolean("ro.alarm_boot", false);
        return ENCRYPTING_STATE.equals(cryptState) ||
               ENCRYPTED_STATE.equals(cryptState) ||
               isAlarmBoot;
    }

    /**
     * Invoked via reflection by the SystemServer
     */
    private void run() {
        // Start services.
        try {
            startArielServices();
        } catch (Throwable ex) {
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting ariel system services", ex);
            throw ex;
        }
    }

    private void startArielServices() {
        final Context context = mSystemContext;
        final SystemServiceManager ssm = LocalServices.getService(SystemServiceManager.class);
        String[] externalServices = context.getResources().getStringArray(
                com.arielos.platform.internal.R.array.config_externalArielServices);

        for (String service : externalServices) {
            try {
                Slog.i(TAG, "Attempting to start service " + service);
                ArielSystemService arielSystemService =  mSystemServiceHelper.getServiceFor(service);
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
                reportWtf("starting " + service , e);
            }
        }
    }

    private void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        Slog.wtf(TAG, "BOOT FAILURE " + msg, e);
    }
}
