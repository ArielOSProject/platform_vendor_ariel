/**
 * Copyright (c) 2015, The CyanogenMod Project
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

package com.ariel.platform.internal.hardware;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.ariel.platform.internal.ArielSystemService;

import ariel.app.ArielContextConstants;
import ariel.platform.Manifest;
import ariel.device.IArielHardwareManager;

import com.ariel.hardware.UniqueDeviceId;
import com.ariel.hardware.PersistentStorage;


/**
 * Internal service which manages interactions with system ui elements
 *
 * @hide
 */
public class ArielHardwareService extends ArielSystemService {
    private static final String TAG = "ArielHardwareService";

    private Context mContext;
    private Handler mHandler = new Handler();

    public ArielHardwareService(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getFeatureDeclaration() {
        return ArielContextConstants.Features.HARDWARE;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "register arielhardwareservice: " + this);
        publishBinderService(ArielContextConstants.ARIEL_HARDWARE_SERVICE, mService);
    }

    private final IBinder mService = new IArielHardwareManager.Stub() {

        @Override
        public String getUniqueDeviceId() {
            mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.READ_DEVICE_UID, null);

            return UniqueDeviceId.getUniqueDeviceId();
        }

        @Override
        public byte[] getPersistentData(String key) {

            mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.READ_PERSISTENT_STORAGE, null);

            return PersistentStorage.get(key);
        }

        @Override
        public boolean setPersistentData(String key, byte[] buffer) {
            mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.WRITE_PERSISTENT_STORAGE, null);

            return PersistentStorage.set(key, buffer);
        }

    };

}
