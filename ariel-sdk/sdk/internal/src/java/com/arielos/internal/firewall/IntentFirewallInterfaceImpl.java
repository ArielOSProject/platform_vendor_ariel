/**
 * Copyright (c) 2015, The CyanogenMod Project
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

package com.arielos.internal.firewall;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import java.util.List;

import arielos.app.ArielContextConstants;
import arielos.firewall.IntentFirewallInterface;
import arielos.firewall.IIntentFirewallInterface;

/**
 * ArielOS Intent firewall manager
 */
public class IntentFirewallInterfaceImpl implements IntentFirewallInterface{
    private static final String TAG = "IntentFirewallInterface";
    private static boolean localLOGV = true;

    private Context mContext;

    private static IIntentFirewallInterface sService;

    public IntentFirewallInterfaceImpl(Context context) {
        Context appContext = context.getApplicationContext();
        mContext = appContext == null ? context : appContext;
        sService = getService();

        if (context.getPackageManager().hasSystemFeature(
                ArielContextConstants.Features.INTENT_FIREWALL) && sService == null) {
            Log.wtf(TAG, "Unable to get ArielIntentFirewallService. The service either" +
                    " crashed, was not started, or the interface has been called to early in" +
                    " SystemServer init");
        }
    }

    @Override
    public boolean disableApp(String packageName) {
        if (sService == null) {
            Log.w(TAG, "not connected to ArielIntentFirewallService");
            return false;
        }

        if (localLOGV) Log.v(TAG, "Invoking disableApp");
        try {
            if (localLOGV) Log.v(TAG, "Passing packageName: "+packageName);
            return sService.disableApp(packageName);
        } catch (RemoteException e) {
            Slog.w("ArielIntentFirewallManager", "warning: no ariel intent firewall service");
            return false;
        }
    }

    @Override
    public boolean enableApp(String packageName) {
        if (sService == null) {
            Log.w(TAG, "not connected to ArielIntentFirewallService");
            return false;
        }

        if (localLOGV) Log.v(TAG, "Invoking enableApp");
        try {
            if (localLOGV) Log.v(TAG, "Passing packageName: "+packageName);
            return sService.enableApp(packageName);
        } catch (RemoteException e) {
            Slog.w("ArielIntentFirewallManager", "warning: no ariel intent firewall service");
            return false;
        }
    }

    @Override
    public boolean enableBroadcast(String broadcast) {
        if (sService == null) {
            Log.w(TAG, "not connected to ArielIntentFirewallService");
            return false;
        }

        if (localLOGV) Log.v(TAG, "Invoking enableApp");
        try {
            if (localLOGV) Log.v(TAG, "Passing broadcast: "+broadcast);
            return sService.enableBroadcast(broadcast);
        } catch (RemoteException e) {
            Slog.w("ArielIntentFirewallManager", "warning: no ariel intent firewall service");
            return false;
        }
    }

    @Override
    public boolean disableBroadcast(String broadcast) {
        if (sService == null) {
            Log.w(TAG, "not connected to ArielIntentFirewallService");
            return false;
        }

        if (localLOGV) Log.v(TAG, "Invoking enableApp");
        try {
            if (localLOGV) Log.v(TAG, "Passing broadcast: "+broadcast);
            return sService.disableBroadcast(broadcast);
        } catch (RemoteException e) {
            Slog.w("ArielIntentFirewallManager", "warning: no ariel intent firewall service");
            return false;
        }
    }

    /** @hide */
    public IIntentFirewallInterface getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(ArielContextConstants.ARIEL_INTENT_FIREWALL_SERVICE);
        if (b != null) {
            sService = IIntentFirewallInterface.Stub.asInterface(b);
            return sService;
        }
        return null;
    }

}
