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

package org.lineageos.internal.arielos.security;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import lineageos.app.LineageContextConstants;
import lineageos.arielos.security.SecurityInterface;
import lineageos.arielos.security.ISecurityInterface;

public class SecurityInterfaceImpl implements SecurityInterface {

    private static final String TAG = "SecurityInterface";

    private static ISecurityInterface sService;

    private Context mContext;

    public SecurityInterfaceImpl(Context context) {
        Context appContext = context.getApplicationContext();
        mContext = appContext == null ? context : appContext;
        sService = getService();
        if (context.getPackageManager().hasSystemFeature(
                LineageContextConstants.Features.SECURITY) && sService == null) {
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
        IBinder b = ServiceManager.getService(LineageContextConstants.ARIEL_SECURITY_INTERFACE);
        sService = ISecurityInterface.Stub.asInterface(b);

        if (b == null) {
            Log.e(TAG, "null service. SAD!");
            return null;
        }

        sService = ISecurityInterface.Stub.asInterface(b);
        return sService;
    }

    @Override
    public void generateEscrowToken() {
        if (sService == null) {
            return;
        }
        try {
            sService.generateEscrowToken();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return;
    }

    @Override
    public void hasPendingEscrowToken() {
        if (sService == null) {
            return;
        }
        try {
            sService.hasPendingEscrowToken();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return;
    }
}
