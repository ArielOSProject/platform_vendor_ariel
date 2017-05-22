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

import ariel.app.IArielIntentFirewallManager;

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
public class ArielIntentFirewallManager {
    private static final String TAG = "ArielIntentFirewallManager";
    private static boolean localLOGV = true;

    private Context mContext;

    private static IArielIntentFirewallManager sService;

    private static ArielIntentFirewallManager sArielIntentFirewallManagerInstance;
    private ArielIntentFirewallManager(Context context) {
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
    public static ArielIntentFirewallManager getInstance(Context context) {
        if (sArielIntentFirewallManagerInstance == null) {
            sArielIntentFirewallManagerInstance = new ArielIntentFirewallManager(context);
        }
        return sArielIntentFirewallManagerInstance;
    }

    public void disableApp(String packageName) {
        if (sService == null) {
            Log.w(TAG, "not connected to ArielIntentFirewallService");
            return;
        }

        if (localLOGV) Log.v(TAG, "Invoking disableApp");
        try {
            if (localLOGV) Log.v(TAG, "Passing packageName: "+packageName);
            sService.disableApp(packageName);
        } catch (RemoteException e) {
            Slog.w("ArielIntentFirewallManager", "warning: no ariel intent firewall service");
        }
    }

    public void enableApp() {
        if (sService == null) {
            Log.w(TAG, "not connected to ArielIntentFirewallService");
            return;
        }

        if (localLOGV) Log.v(TAG, "Invoking enableApp");
        try {
            if (localLOGV) Log.v(TAG, "Passing packageName: "+packageName);
            sService.enableApp(packageName);
        } catch (RemoteException e) {
            Slog.w("ArielIntentFirewallManager", "warning: no ariel intent firewall service");
        }
    }

    /** @hide */
    public IArielIntentFirewallManager getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(ArielContextConstants.ARIEL_INTENT_FIREWALL_SERVICE);
        if (b != null) {
            sService = IArielIntentFirewallManager.Stub.asInterface(b);
            return sService;
        }
        return null;
    }
}
