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
import android.os.Bundle;

import arielos.app.ArielContextConstants;
import arielos.security.ISecurityInterface;
import arielos.security.IEscrowTokenStateChangeCallback;
import arielos.security.IKeyguardStateCallback;
import arielos.security.SecurityInterface;
import com.arielos.platform.internal.ArielSystemService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
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
import java.util.List;
import android.util.Base64;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.face.FaceManager;
import android.hardware.face.Face;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.Fingerprint;

/** @hide **/
public class ArielSecurityService extends ArielSystemService {
    private static final String TAG = "ArielSecurityService";

    private Context mContext;
    private LockPatternUtils mLPU;
    private KeyguardServiceDelegate mKeyguardDelegate;
    private IWindowManager mWindowManagerService;
    private FingerprintManager mFingerprintManager;
    private FaceManager mFaceManager;
    private Boolean lastKeyguardShowingState = null;

    private ArrayList<IKeyguardStateCallback> mKeyguardStateListeners = new ArrayList();

    private FingerprintManager getFingerprintManagerOrNull(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            return (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        } else {
            return null;
        }
    }

    private boolean hasFingerprintHardware(Context context) {
        final FingerprintManager fingerprintManager = getFingerprintManagerOrNull(context);
        return fingerprintManager != null && fingerprintManager.isHardwareDetected();
    }

    private FaceManager getFaceManagerOrNull(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE)) {
            return (FaceManager) context.getSystemService(Context.FACE_SERVICE);
        } else {
            return null;
        }
    }

    private boolean hasFaceHardware(Context context) {
        final FaceManager faceManager = getFaceManagerOrNull(context);
        return faceManager != null && faceManager.isHardwareDetected();
    }


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
        mFingerprintManager = getFingerprintManagerOrNull(mContext);
        mFaceManager = getFaceManagerOrNull(mContext);
        mWindowManagerService = WindowManagerGlobal.getWindowManagerService();
        mKeyguardDelegate = new KeyguardServiceDelegate(mContext,
                new StateCallback() {
                    @Override
                    public void onTrustedChanged() {
                        Log.d(TAG, "onTrustChanged() callback");
                    }

                    @Override
                    public void onShowingChanged() {
                        Log.d(TAG, "onShowingChanged() callback");
                        Log.d(TAG, "keyguard.isShowing = "+mKeyguardDelegate.isShowing());
                        Log.d(TAG, "keyguard.isTrusted = "+mKeyguardDelegate.isTrusted());
                        Log.d(TAG, "keyguard.hasKeyguard = "+mKeyguardDelegate.hasKeyguard());
                        Log.d(TAG, "keyguard.isSecure = "+mKeyguardDelegate.isSecure(0));
                        boolean notifyCallbacks = false;
                        if(lastKeyguardShowingState == null) {
                            lastKeyguardShowingState = mKeyguardDelegate.isShowing();
                            notifyCallbacks = true;

                        } else {
                            if(lastKeyguardShowingState != mKeyguardDelegate.isShowing() ) {
                                lastKeyguardShowingState = mKeyguardDelegate.isShowing();
                                notifyCallbacks = true;
                            } else {
                                notifyCallbacks = false;
                            }
                        }
                        if (notifyCallbacks) {
                            mKeyguardStateListeners.forEach(callback -> {
                                try {
                                    if(mKeyguardDelegate.isShowing()) {
                                        callback.onKeyguardDisplayed();
                                    } else {
                                        callback.onKeyguardDismissed();
                                    }
                                }
                                catch(RemoteException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
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
        mLPU.addEscrowToken(token, userId, new EscrowTokenStateChangeCallback() {
            public void onEscrowTokenActivated(long handle, int userid) {
                // todo store the token somewhere
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
        Log.d(TAG, "Setting lockout on user="+userId+" with timeoutMs="+timeoutMs);
        long deadLine = mLPU.setLockoutAttemptDeadline(userId, timeoutMs);
        Bundle options = new Bundle();
        // this will force the keyguard to refresh itself
        mKeyguardDelegate.doKeyguardTimeout(options);
        return deadLine;
    }

     /**
     * Set and store the lockout deadline, meaning the user can't attempt his/her unlock
     * pattern until the deadline has passed.
     * @return the chosen deadline.
     */
    public long getLockoutAttemptDeadlineLocked(int userId) {
        return mLPU.getLockoutAttemptDeadline(userId);
    }


    private boolean hasPendingEscrowTokenLocked(int userId) {
        return mLPU.hasPendingEscrowToken(userId);
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
        LockscreenCredential lockCredential;
        if(type == CREDENTIAL_TYPE_NONE) {
            // no pin to be set, will clear lock screen protection
            lockCredential = LockscreenCredential.createNone();
            // remove fingerprints
            removeFingerprints(userId);
            // remove faces
            removeFaces(userId);
        } else if(type == CREDENTIAL_TYPE_PIN) {
            // create a pin
            CharSequence newPin = new String(credential);
            lockCredential = LockscreenCredential.createPin(newPin);
        } else {
            // abort, we do not support anything else yet
            return false;
        }
        boolean setLockResult = mLPU.setLockCredentialWithToken(lockCredential, tokenHandle, token, userId);
        if(setLockResult) {
            Bundle options = new Bundle();
            // this will force the keyguard to refresh itself
            mKeyguardDelegate.doKeyguardTimeout(options);
        }
        return setLockResult;
    }

    /**
     * LineageOS 18+ versions probably have removeAll method within the FingerprintManager
     * so check that when porting to avoid iteration.
     */
    private void removeFingerprints(int userId) {
        if(hasFingerprintHardware(mContext)) {
            List<Fingerprint> fingerprints = mFingerprintManager.getEnrolledFingerprints(userId);
            for(Fingerprint fp : fingerprints) {
                Log.d(TAG, "Removing fingerprint: "+fp.getName()+"...");
                mFingerprintManager.remove(fp, userId, new FingerRemovalCallback());
            }
        } else {
            Log.d(TAG, "Fingerprint hardware not present, skipping fingerprint removal.");
        }
    }

    /**
     * LineageOS 18+ versions probably have removeAll method within the FaceManager
     * so check that when porting to avoid iteration.
     */
    private void removeFaces(int userId) {
        if(hasFaceHardware(mContext)) {
            List<Face> faces = mFaceManager.getEnrolledFaces(userId);
            for(Face fc : faces) {
                Log.d(TAG, "Removing face: "+fc.getName()+"...");
                mFaceManager.remove(fc, userId, new FaceRemovalCallback());
            }
        } else {
            Log.d(TAG, "Face hardware not present, skipping face removal.");
        }
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

    private void registerKeyguardStateListenerLocked(IKeyguardStateCallback callback) {
        mKeyguardStateListeners.add(callback);
    }

    private void unregisterKeyguardStateListenerLocked(IKeyguardStateCallback callback) {
        mKeyguardStateListeners.remove(callback);
    }

    private boolean isKeyguardShowingLocked() {
        if (mKeyguardDelegate != null) {
            return mKeyguardDelegate.isShowing();
        }
        return false;
    }

    private class FingerRemovalCallback extends FingerprintManager.RemovalCallback {
        @Override
        public void onRemovalSucceeded(Fingerprint fingerprint, int remaining) {
            Log.d(TAG, "Removed fingerprint: "+fingerprint.getName()+", remaining: "+remaining);
        }

        @Override
        public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
            Log.d(TAG, "Failed removing fingerprint: "+fp.getName()+", errMsgId: "+errMsgId+", errString: "+errString);
        }
    };

    private class FaceRemovalCallback extends FaceManager.RemovalCallback {
        @Override
        public void onRemovalSucceeded(Face face, int remaining) {
            Log.d(TAG, "Removed face: "+face.getName()+", remaining: "+remaining);
        }

        @Override
        public void onRemovalError(Face face, int errMsgId, CharSequence errString) {
            Log.d(TAG, "Failed removing face: "+face.getName()+", errMsgId: "+errMsgId+", errString: "+errString);
        }
    };

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
        public long getLockoutAttemptDeadline(int userId) {
            enforceSecurityPermission();
            return getLockoutAttemptDeadlineLocked(userId);
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

        @Override
        public void registerKeyguardStateListener(IKeyguardStateCallback callback) {
            enforceSecurityPermission();
            registerKeyguardStateListenerLocked(callback);
        }

        @Override
        public void unregisterKeyguardStateListener(IKeyguardStateCallback callback) {
            enforceSecurityPermission();
            unregisterKeyguardStateListenerLocked(callback);
        }

        @Override
        public boolean isKeyguardShowing() {
            enforceSecurityPermission();
            return isKeyguardShowingLocked();
        }

    };
}
