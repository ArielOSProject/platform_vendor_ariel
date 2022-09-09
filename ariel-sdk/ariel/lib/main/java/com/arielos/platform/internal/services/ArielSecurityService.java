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

package com.arielos.platform.internal.services;

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

import arielos.app.ArielContextConstants;
import arielos.security.ISecurityInterface;
import arielos.security.IEscrowTokenStateChangeCallback;
import arielos.security.SecurityInterface;
import com.arielos.platform.internal.ArielSystemService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.NoSuchElementException;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtils.EscrowTokenStateChangeCallback;
import com.android.internal.widget.LockscreenCredential;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_NONE;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PASSWORD;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PASSWORD_OR_PIN;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PATTERN;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PIN;
import com.android.server.policy.keyguard.KeyguardServiceDelegate;
import com.android.server.policy.keyguard.KeyguardStateMonitor.StateCallback;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.server.policy.WindowManagerPolicy.OnKeyguardExitResult;
import android.view.IWindowManager;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.app.trust.TrustManager;
import com.android.server.SystemService;
import android.os.UserHandle;
import java.security.SecureRandom;
import android.util.Log;
import android.util.Base64;

/** @hide **/
public class ArielSecurityService extends ArielSystemService {
    private static final String TAG = "ArielSecurityService";

    private Context mContext;
    private LockPatternUtils mLPU;
    private KeyguardServiceDelegate mKeyguardDelegate;
    private IWindowManager mWindowManagerService;

    public ArielSecurityService(Context context) {
        super(context);
        mContext = context;
        if (context.getPackageManager().hasSystemFeature(ArielContextConstants.Features.SECURITY)) {
            publishBinderService(ArielContextConstants.ARIEL_SECURITY_INTERFACE, mService);
        } else {
            Log.wtf(TAG, "Ariel Security service started by system server but feature xml not" +
                    " declared. Not publishing binder service!");
        }
    }

    @Override
    public String getFeatureDeclaration() {
        return ArielContextConstants.Features.SECURITY;
    }

    @Override
    public void onStart() {
        mLPU = new LockPatternUtils(mContext);
        mWindowManagerService = WindowManagerGlobal.getWindowManagerService();
        mKeyguardDelegate = new KeyguardServiceDelegate(mContext,
                new StateCallback() {
                    @Override
                    public void onTrustedChanged() {
                        // doing nothing here
                    }

                    @Override
                    public void onShowingChanged() {
                        // doing nothing here
                    }
                });
    }

    @Override
    public void onBootPhase(int phase) {
        // make sure boot completed before doing anything
        if (phase == SystemService.PHASE_SYSTEM_SERVICES_READY) {
            mKeyguardDelegate.bindService(mContext);
        }
    }

    /* Public methods implementation */

    private void enforceSecurityPermission() {
        mContext.enforceCallingOrSelfPermission(SecurityInterface.SECURITY_INTERFACE_PERMISSION,
                "You do not have permissions to use the Security interface");
    }

    private void clearUserLockLocked() {

    }

    /**
     * Create an escrow token for the current user, which can later be used to unlock FBE
     * or change user password.
     *
     * After adding, if the user currently has lockscreen password, he will need to perform a
     * confirm credential operation in order to activate the token for future use. If the user
     * has no secure lockscreen, then the token is activated immediately.
     *
     * <p>This method is only available to code running in the system server process itself.
     *
     * @return a unique 64-bit token handle which is needed to refer to this token later.
     */
    private void generateEscrowTokenLocked(int userId, byte[] token, IEscrowTokenStateChangeCallback calllback) {
        Log.e(TAG, "Got the token in  bytes: "+token);
        Log.e(TAG, "Converting it to string shows: "+Base64.encode(token, Base64.DEFAULT));
        mLPU.addEscrowToken(token, userId, new EscrowTokenStateChangeCallback() {
            public void onEscrowTokenActivated(long handle, int userid) {
                // todo store the token somewhere
                Log.e(TAG, "Activated escrow token!!!");
                Log.e(TAG, "Escrow handle: "+handle);
                Log.e(TAG, "Escrow userid: "+userid);
                try {
                    calllback.onEscrowTokenActivated(handle, userid);
                }
                catch(RemoteException e) {
                    e.printStackTrace();
                }
            }
        }); 
    }

      /**
     * Set and store the lockout deadline, meaning the user can't attempt his/her unlock
     * pattern until the deadline has passed.
     * @return the chosen deadline.
     */
    public long setLockoutAttemptDeadlineLocked(int userId, int timeoutMs) {
        return mLPU.setLockoutAttemptDeadline(userId, timeoutMs);
    }

    private boolean hasPendingEscrowTokenLocked(int userId) {
        return mLPU.hasPendingEscrowToken(userId);
    }

        /**
     * Unlock the specified user by an pre-activated escrow token. This should have the same effect
     * on device encryption as the user entering his lockscreen credentials for the first time after
     * boot, this includes unlocking the user's credential-encrypted storage as well as the keystore
     *
     * <p>This method is only available to code running in the system server process itself.
     *
     * @return {@code true} if the supplied token is valid and unlock succeeds,
     *         {@code false} otherwise.
     */
    // TODO still to figure out how to use unlockUserWithToken for full unlock
    private boolean unlockUserWithTokenLocked(long tokenHandle, byte[] token, int userId) {
        // proveriti sta se desi ako se ne pozove ovo ali se pozove setKeyguardEnabled(false)
        boolean isUnlocked = mLPU.unlockUserWithToken(tokenHandle, token, userId);
        if(isUnlocked) {
            Log.e(TAG, "lpu notified unlock");
            // dismiss the keyguard here ( && mKeyguardDelegate.isShowing())
            if (mKeyguardDelegate != null) {
                Log.e(TAG, "mKeyguardDelegate not null, calling trust manager...");
                final TrustManager trustManager = mContext.getSystemService(TrustManager.class);
                trustManager.setDeviceLockedForUser(userId, false);
                Log.e(TAG, "mKeyguardDelegate not null, disabling keyguard...");
                // maybe the feature should be something like peek:
                // 1. unlock the keyguard
                // 2. set the timer to relock it again after 10 minutes or reboot
                mKeyguardDelegate.setKeyguardEnabled(false);
                // ask the keyguard to prompt the user to authenticate if necessary
                // try {
                    mKeyguardDelegate.dismiss(new IKeyguardDismissCallback.Stub() {
                        @Override
                        public void onDismissError() throws RemoteException {
                            Log.e(TAG, "keyguard: onDismissError");
                        }
        
                        @Override
                        public void onDismissSucceeded() throws RemoteException {
                            Log.e(TAG, "keyguard: onDismissSucceeded");
                        }
        
                        @Override
                        public void onDismissCancelled() throws RemoteException {
                            Log.e(TAG, "keyguard: onDismissCancelled");
                        }
                    }, "ariel_unlock");
                // }
                // catch(RemoteException e) {
                //     e.printStackTrace();
                // }
            } else  {
                // do nothing here
                Log.e(TAG, "mKeyguardDelegate is null");
            }
        }
        return isUnlocked;
    }

    /**
     * Remove an escrow token.
     *
     * <p>This method is only available to code running in the system server process itself.
     *
     * @return true if the given handle refers to a valid token previously returned from
     * {@link #addEscrowToken}, whether it's active or not. return false otherwise.
     */
    private boolean removeEscrowTokenLocked(long handle, int userId) {
        return mLPU.removeEscrowToken(handle, userId);
    }

    /**
     * Check if the given escrow token is active or not. Only active token can be used to call
     * {@link #setLockCredentialWithToken} and {@link #unlockUserWithToken}
     *
     * <p>This method is only available to code running in the system server process itself.
     */
    private boolean isEscrowTokenActiveLocked(long handle, int userId) {
        return mLPU.isEscrowTokenActive(handle, userId);
    }

    /**
     * Change a user's lock credential with a pre-configured escrow token.
     *
     * <p>This method is only available to code running in the system server process itself.
     *
     * @param credential The new credential to be set
     * @param tokenHandle Handle of the escrow token
     * @param token Escrow token
     * @param userHandle The user who's lock credential to be changed
     * @return {@code true} if the operation is successful.
     */
    private boolean setLockCredentialWithTokenLocked(byte[] credential, int type,
            long tokenHandle, byte[] token, int userId) {
        // todo this implementation only considers PINS, this needs to be updated if we are to support other methods
        // convert credential to string first
        Log.d(TAG, "We only support PIN for now!");
        LockscreenCredential lockCredential;
        if(type == CREDENTIAL_TYPE_NONE) {
            // no pin to be set, will clear lock screen protection
            lockCredential = LockscreenCredential.createNone();

        } else if(type == CREDENTIAL_TYPE_PIN) {
            // create a pin
            CharSequence newPin = new String(credential);
            lockCredential = LockscreenCredential.createPin(newPin);    
        } else {
            // abort, we do not support anything else yet
            return false;
        }
        return mLPU.setLockCredentialWithToken(lockCredential, tokenHandle, token, userId);
    }

    private boolean startPeekingLocked() {
        if (mKeyguardDelegate != null) {
            mKeyguardDelegate.setKeyguardEnabled(false);
        }
        return true;
    }

    private boolean stopPeekingLocked() {
        if (mKeyguardDelegate != null) {
            mKeyguardDelegate.setKeyguardEnabled(true);
        }
        return true;
    }

    /* Service */

    private final IBinder mService = new ISecurityInterface.Stub() {

        @Override
        public void generateEscrowToken(int userId, byte[] token, IEscrowTokenStateChangeCallback calllback) {
            enforceSecurityPermission();
            generateEscrowTokenLocked(userId, token, calllback);
        }

        @Override
        public boolean hasPendingEscrowToken(int userId) {
            enforceSecurityPermission();
            return hasPendingEscrowTokenLocked(userId);
        }

        @Override
        public long setLockoutAttemptDeadline(int userId, int timeoutMs) {
            enforceSecurityPermission();
            return setLockoutAttemptDeadlineLocked(userId, timeoutMs);
        }

        @Override
        public boolean unlockUserWithToken(long tokenHandle, byte[] token, int userId) {
            enforceSecurityPermission();
            return unlockUserWithTokenLocked(tokenHandle, token, userId);
        }

        @Override
        public boolean removeEscrowToken(long handle, int userId) {
            enforceSecurityPermission();
            return removeEscrowTokenLocked(handle, userId);
        }

        @Override
        public boolean isEscrowTokenActive(long handle, int userId) {
            enforceSecurityPermission();
            return isEscrowTokenActiveLocked(handle, userId);
        }

        @Override
        public boolean setLockCredentialWithToken(byte[] credential, int type, long tokenHandle, byte[] token, int userId) {
            enforceSecurityPermission();
            return setLockCredentialWithTokenLocked(credential, type, tokenHandle, token, userId);
        }

        @Override
        public boolean startPeeking() {
            enforceSecurityPermission();
            return startPeekingLocked();
        }

        @Override
        public boolean stopPeeking() {
            enforceSecurityPermission();
            return stopPeekingLocked();
        }

    };
}
