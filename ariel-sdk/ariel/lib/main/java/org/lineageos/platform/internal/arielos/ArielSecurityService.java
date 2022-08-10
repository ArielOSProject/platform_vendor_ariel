/*
 * Copyright (C) 2018-2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.platform.internal.arielos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Pair;
import android.text.TextUtils;

import lineageos.app.LineageContextConstants;
import lineageos.arielos.security.ISecurityInterface;
import lineageos.arielos.security.SecurityInterface;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.NoSuchElementException;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtils.EscrowTokenStateChangeCallback;
import android.os.UserHandle;
import java.security.SecureRandom;
import android.util.Log;

/** @hide **/
public class ArielSecurityService extends LineageSystemService {
    private static final String TAG = "ArielSecurityService";

    private Context mContext;
    private LockPatternUtils mLPU;

    public ArielSecurityService(Context context) {
        super(context);
        mContext = context;
        if (context.getPackageManager().hasSystemFeature(LineageContextConstants.Features.SECURITY)) {
            publishBinderService(LineageContextConstants.ARIEL_SECURITY_INTERFACE, mService);
        } else {
            Log.wtf(TAG, "Ariel Security service started by system server but feature xml not" +
                    " declared. Not publishing binder service!");
        }
    }

    @Override
    public String getFeatureDeclaration() {
        return LineageContextConstants.Features.SECURITY;
    }

    @Override
    public void onStart() {
        // maybe instantiate lock pattern utils here?
        mLPU = LockPatternUtils(mSystemContext);
    }

    /* Public methods implementation */

    private void enforceSecurityPermission() {
        mContext.enforceCallingOrSelfPermission(SecurityInterface.SECURITY_INTERFACE_PERMISSION,
                "You do not have permissions to use the Security interface");
    }

    private void clearUserLockLocked() {

    }

    private void generateEscrowTokenLocked() {
        byte token[] = new byte[32]; // Minimum size token accepted
        SecureRandom random = new SecureRandom();
        random.nextBytes(token);
        Log.e(TAG, "Got the token in  bytes: "+token);
        Log.e(TAG, "Got the token: "+token.toString());
        mLPU.addEscrowToken(token, UserHandle.USER_SYSTEM, new EscrowTokenStateChangeCallback() {
            public void onEscrowTokenActivated(long handle, int userid) {
                // todo store the token somewhere
                Log.e(TAG, "Activated escrow token!!!");
                Log.e(TAG, "Escrow handle: "+handle)
                Log.e(TAG, "Escrow userid: "+userid);   
            }
        ) 
    }

    private boolean hasPendingEscrowTokenLocked(int userId) {
        return mLPU.hasPendingEscrotToken(userId);
    }

    /* Service */

    private final IBinder mService = new ISecurityInterface.Stub() {
        // @Override
        // public void clearUserLock(int feature) {
        //     enforceSecurityPermission();
        // }

        @Override
        public void generateEscrowToken() {
            enforceSecurityPermission();
            generateEscrowTokenLocked();
        }

        @Override
        public boolean hasPendingEscrowToken() {
            enforceSecurityPermission();
            return hasPendingEscrowTokenLocked();
        }

    };
}
