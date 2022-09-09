/**
 * Copyright (C) 2018-2019 The LineageOS Project
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

package com.arielos.internal.security;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import arielos.app.ArielContextConstants;
import arielos.security.SecurityInterface;
import arielos.security.ISecurityInterface;
import arielos.security.IEscrowTokenStateChangeCallback;

public class SecurityInterfaceImpl implements SecurityInterface {

    private static final String TAG = "SecurityInterface";

    private static ISecurityInterface sService;

    private Context mContext;

    public SecurityInterfaceImpl(Context context) {
        Context appContext = context.getApplicationContext();
        mContext = appContext == null ? context : appContext;
        sService = getService();
        if (context.getPackageManager().hasSystemFeature(
                ArielContextConstants.Features.SECURITY) && sService == null) {
            throw new RuntimeException("Unable to get SecurityInterfaceService. The service" +
                    " either crashed, was not started, or the interface has been called to early" +
                    " in SystemServer init");
                }
    }

    /** @hide **/
    private ISecurityInterface getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(ArielContextConstants.ARIEL_SECURITY_INTERFACE);
        sService = ISecurityInterface.Stub.asInterface(b);

        if (b == null) {
            Log.e(TAG, "null service. SAD!");
            return null;
        }

        sService = ISecurityInterface.Stub.asInterface(b);
        return sService;
    }

    @Override
    public void generateEscrowToken(int userId, byte[] token, IEscrowTokenStateChangeCallback callback) {
        if (sService == null) {
            return;
        }
        try {
            sService.generateEscrowToken(userId, token, callback);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return;
    }

    @Override
    public boolean hasPendingEscrowToken(int userId) {
        if (sService == null) {
            return false;
        }
        try {
           return sService.hasPendingEscrowToken(userId);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return false;
    }

    @Override
    public long setLockoutAttemptDeadline(int userId, int timeoutMs) {
        if (sService == null) {
            return -1;
        }
        try {
           return sService.setLockoutAttemptDeadline(userId, timeoutMs);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return -1;
    }

    @Override
    public boolean unlockUserWithToken(long tokenHandle, byte[] token, int userId) {
        if (sService == null) {
            return false;
        }
        try {
           return sService.unlockUserWithToken(tokenHandle, token, userId);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return false;
    }

    @Override
    public boolean removeEscrowToken(long handle, int userId) {
        if (sService == null) {
            return false;
        }
        try {
           return sService.removeEscrowToken(handle, userId);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return false;
    }
    
    @Override
    public boolean isEscrowTokenActive(long handle, int userId) {
        if (sService == null) {
            return false;
        }
        try {
           return sService.isEscrowTokenActive(handle, userId);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return false;
    }

    @Override
    public boolean setLockCredentialWithToken(byte[] credential, int type, long tokenHandle, byte[] token, int userId) {
        if (sService == null) {
            return false;
        }
        try {
           return sService.setLockCredentialWithToken(credential, type, tokenHandle, token, userId);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return false;
    }

    @Override
    public boolean startPeeking() {
        if (sService == null) {
            return false;
        }
        try {
           return sService.startPeeking();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return false;
    }

    @Override
    public boolean stopPeeking() {
        if (sService == null) {
            return false;
        }
        try {
           return sService.stopPeeking();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return false;
    }
}
