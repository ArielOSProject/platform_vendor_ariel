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

package ariel.app;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import java.util.List;

import ariel.app.IArielFirewallManager;

/**
 * The CMStatusBarManager allows you to publish and remove CustomTiles within the
 * Quick Settings Panel.
 *
 * <p>
 * Each of the publish methods takes an int id parameter and optionally a
 * {@link String} tag parameter, which may be {@code null}.  These parameters
 * are used to form a pair (tag, id), or ({@code null}, id) if tag is
 * unspecified.  This pair identifies this custom tile from your app to the
 * system, so that pair should be unique within your app.  If you call one
 * of the publish methods with a (tag, id) pair that is currently active and
 * a new set of custom tile parameters, it will be updated.  For example,
 * if you pass a new custom tile icon, the old icon in the panel will
 * be replaced with the new one.  This is also the same tag and id you pass
 * to the {@link #removeTile(int)} or {@link #removeTile(String, int)} method to clear
 * this custom tile.
 *
 * <p>
 * To get the instance of this class, utilize CMStatusBarManager#getInstance(Context context)
 *
 * @see cyanogenmod.app.CustomTile
 */
public class ArielFirewallManager {
    private static final String TAG = "ArielFirewallManager";
    private static boolean localLOGV = true;

    private Context mContext;

    private static IArielFirewallManager sService;

    private static ArielFirewallManager sArielFirewallManagerInstance;
    private ArielFirewallManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();

        if (context.getPackageManager().hasSystemFeature(
                ariel.app.ArielContextConstants.Features.FIREWALL) && sService == null) {
            Log.wtf(TAG, "Unable to get ArielFirewallService. The service either" +
                    " crashed, was not started, or the interface has been called to early in" +
                    " SystemServer init");
        }
    }

    /**
     * Get or create an instance of the {@link cyanogenmod.app.CMStatusBarManager}
     * @param context
     * @return {@link cyanogenmod.app.CMStatusBarManager}
     */
    public static ArielFirewallManager getInstance(Context context) {
        if (sArielFirewallManagerInstance == null) {
            sArielFirewallManagerInstance = new ArielFirewallManager(context);
        }
        return sArielFirewallManagerInstance;
    }

    public void disableNetworking(String uids) {
        if (sService == null) {
            Log.w(TAG, "not connected to ArielFirewallService");
            return;
        }

        if (localLOGV) Log.v(TAG, "Invoking disableNetworking");
        try {
            if (localLOGV) Log.v(TAG, "Passing uids: "+uids);
            sService.disableNetworking(uids);
        } catch (RemoteException e) {
            Slog.w("ArielFirewallManager", "warning: no ariel firewal service");
        }
    }

    public void clearRules() {
        if (sService == null) {
            Log.w(TAG, "not connected to ArielFirewallService");
            return;
        }

        if (localLOGV) Log.v(TAG, "Invoking purge iptables");
        try {
            sService.clearRules();
        } catch (RemoteException e) {
            Slog.w(TAG, "warning: no ariel firewal service");
        }
    }

    /** @hide */
    public IArielFirewallManager getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(ArielContextConstants.ARIEL_FIREWALL_SERVICE);
        if (b != null) {
            sService = IArielFirewallManager.Stub.asInterface(b);
            return sService;
        }
        return null;
    }
}
