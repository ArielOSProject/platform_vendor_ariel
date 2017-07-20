package ariel.device;

import android.content.Context;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog;

import ariel.app.ArielContextConstants;
import ariel.app.IArielFirewallManager;

/**
 * Created by mikalackis on 29.7.16..
 */
public class ArielHardwareManager {

    private static final String TAG = "ArielHardwareManager";
    private static boolean localLOGV = true;

    private Context mContext;

    private static IArielHardwareManager sService;

    private static ArielHardwareManager sArielHardwareManagerInstance;
    private ArielHardwareManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();

        if (context.getPackageManager().hasSystemFeature(
                ArielContextConstants.Features.HARDWARE) && sService == null) {
            Log.wtf(TAG, "Unable to get ArielHardwareService. The service either" +
                    " crashed, was not started, or the interface has been called to early in" +
                    " SystemServer init");
        }
    }

    /**
     * Get or create an instance of the {@link cyanogenmod.app.CMStatusBarManager}
     * @param context
     * @return {@link cyanogenmod.app.CMStatusBarManager}
     */
    public static ArielHardwareManager getInstance(Context context) {
        if (sArielHardwareManagerInstance == null) {
            sArielHardwareManagerInstance = new ArielHardwareManager(context);
        }
        return sArielHardwareManagerInstance;
    }

    public String getUniqueDeviceId() {
        if (sService == null) {
            Log.w(TAG, "not connected to ArielHardwareService");
            return null;
        }
        try {
            return sService.getUniqueDeviceId();
        } catch (RemoteException e) {
            Slog.w("ArielHardwareManager", "warning: no ariel hardware service");
            return null;
        }
    }

    public String getUniquePseudoDeviceId() {
        if (sService == null) {
            Log.w(TAG, "not connected to ArielHardwareService");
            return null;
        }
        try {
            return sService.getUniquePseudoDeviceId();
        } catch (RemoteException e) {
            Slog.w("ArielHardwareManager", "warning: no ariel hardware service");
            return null;
        }
    }

    public byte[] getPersistentData(String key) {
        if (sService == null) {
            Log.w(TAG, "not connected to ArielHardwareService");
            return null;
        }
        try {
            return sService.getPersistentData(key);
        } catch (RemoteException e) {
            Slog.w("ArielHardwareManager", "warning: no ariel hardware service");
            return null;
        }
    }

    public boolean setPersistentData(String key, byte[] buffer) {
        if (sService == null) {
            Log.w(TAG, "not connected to ArielHardwareService");
            return null;
        }
        try {
            return sService.setPersistentData(key, buffer);
        } catch (RemoteException e) {
            Slog.w("ArielHardwareManager", "warning: no ariel hardware service");
            return null;
        }
    }

    /** @hide */
    public IArielHardwareManager getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(ArielContextConstants.ARIEL_HARDWARE_SERVICE);
        if (b != null) {
            sService = IArielHardwareManager.Stub.asInterface(b);
            return sService;
        }
        return null;
    }

}