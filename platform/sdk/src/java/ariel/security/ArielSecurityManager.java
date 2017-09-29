package ariel.security;

import android.content.Context;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog;

import ariel.context.ArielContextConstants;

/**
 * Created by mikalackis on 29.7.16..
 */
public class ArielSecurityManager {

    private static final String TAG = "ArielSecurityManager";
    private static boolean localLOGV = true;

    private Context mContext;

    private static IArielSecurityManager sService;

    private static ArielSecurityManager sArielSecurityManagerInstance;
    private ArielSecurityManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();

        if (context.getPackageManager().hasSystemFeature(
                ArielContextConstants.Features.SECURITY) && sService == null) {
            Log.wtf(TAG, "Unable to get ArielSecurityService. The service either" +
                    " crashed, was not started, or the interface has been called to early in" +
                    " SystemServer init");
        }
    }

    /**
     * Get or create an instance of the {@link cyanogenmod.app.CMStatusBarManager}
     * @param context
     * @return {@link cyanogenmod.app.CMStatusBarManager}
     */
    public static ArielSecurityManager getInstance(Context context) {
        if (sArielSecurityManagerInstance == null) {
            sArielSecurityManagerInstance = new ArielSecurityManager(context);
        }
        return sArielSecurityManagerInstance;
    }

//    public String getUniqueDeviceId() {
//        if (sService == null) {
//            Log.w(TAG, "not connected to ArielHardwareService");
//            return null;
//        }
//        try {
//            return sService.getUniqueDeviceId();
//        } catch (RemoteException e) {
//            Slog.w("ArielHardwareManager", "warning: no ariel hardware service");
//            return null;
//        }
//    }
//
    public void setFingerprintEnabled(){
        if (sService == null) {
            Log.w(TAG, "not connected to ArielSecurityService");
            return;
        }
        try {
            sService.setFingerprintEnabled();
        } catch (RemoteException e) {
            Slog.w("ArielSecurityService", "warning: no ariel hardware service");
            return;
        }
//    }

    /** @hide */
    public IArielSecurityManager getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(ArielContextConstants.ARIEL_SECURITY_SERVICE);
        if (b != null) {
            sService = IArielSecurityManager.Stub.asInterface(b);
            return sService;
        }
        return null;
    }

}