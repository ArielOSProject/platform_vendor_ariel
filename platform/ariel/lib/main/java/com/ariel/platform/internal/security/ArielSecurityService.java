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

package com.ariel.platform.internal.security;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.android.internal.widget.LockPatternUtils;
import com.ariel.platform.internal.ArielSystemService;

import ariel.context.ArielContextConstants;
import ariel.security.IArielSecurityManager;

/**
 * Internal service which manages interactions with system ui elements
 *
 * @hide
 */
public class ArielSecurityService extends ArielSystemService {
    private static final String TAG = "ArielSecurityService";

    private Context mContext;
    private Handler mHandler = new Handler();

    private LockPatternUtils mLockPatternUtils;

    public ArielSecurityService(Context context) {
        super(context);
        mContext = context;
        mLockPatternUtils = new LockPatternUtils(context);
    }

    @Override
    public String getFeatureDeclaration() {
        return ArielContextConstants.Features.SECURITY;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "register ArielSecurityService: " + this);
        publishBinderService(ArielContextConstants.ARIEL_SECURITY_SERVICE, mService);
    }

    private final IBinder mService = new IArielSecurityManager.Stub() {

        @Override
        public void setFingerprintEnabled() {
            /**
             * TODO
             */
        }

//        @Override
//        public String getUniqueDeviceId() {
//            mContext.enforceCallingOrSelfPermission(
//                    Manifest.permission.READ_DEVICE_UID, null);
//
//            return UniqueDeviceId.getUniqueDeviceId();
//        }
//
//        @Override
//        public String getUniquePseudoDeviceId() {
//            mContext.enforceCallingOrSelfPermission(
//                    Manifest.permission.READ_DEVICE_UID, null);
//
//            return UniqueDeviceId.getUniquePseudoDeviceId();
//        }
//
//        @Override
//        public byte[] getPersistentData(String key) {
//
//            mContext.enforceCallingOrSelfPermission(
//                    Manifest.permission.READ_PERSISTENT_STORAGE, null);
//
//            return PersistentStorage.get(key);
//        }
//
//        @Override
//        public boolean setPersistentData(String key, byte[] buffer) {
//            mContext.enforceCallingOrSelfPermission(
//                    Manifest.permission.WRITE_PERSISTENT_STORAGE, null);
//
//            return PersistentStorage.set(key, buffer);
//        }
//
//        @Override
//        public long getRemainingBatteryTime() {
//            mContext.enforceCallingOrSelfPermission(
//                    Manifest.permission.ACCESS_BATTERY_STATS, null);
//
//            return DeviceBattery.getRemainingBatteryTime();
//        }
//
//        @Override
//        public long getChargeRemainingTime() {
//            mContext.enforceCallingOrSelfPermission(
//                    Manifest.permission.ACCESS_BATTERY_STATS, null);
//
//            return DeviceBattery.getChargeRemainingTime();
//        }
//
//        @Override
//        public boolean isCharging() {
//            mContext.enforceCallingOrSelfPermission(
//                    Manifest.permission.ACCESS_BATTERY_STATS, null);
//
//            return DeviceBattery.isCharging();
//        }
//
//        @Override
//        public long getScreenOnTime() {
//            mContext.enforceCallingOrSelfPermission(
//                    Manifest.permission.ACCESS_BATTERY_STATS, null);
//
//            return DeviceBattery.getScreenOnTime();
//        }
//
//        @Override
//        public void setDataEnabled(boolean enabled) {
//            /**
//             * TODO
//             * implement permission check here!!!!!!!!
//             */
//            TelephonyManager mTelephonyManager = TelephonyManager.from(mContext);
//            mTelephonyManager.setDataEnabled(enabled);
//        }

    };

}
