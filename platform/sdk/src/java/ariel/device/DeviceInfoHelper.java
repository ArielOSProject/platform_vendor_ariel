package ariel.security;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Base64;
import android.view.WindowManagerGlobal;

import com.android.internal.widget.LockPatternUtils;

import android.net.NetworkPolicyManager;

import com.ariel.hardware.UniqueDeviceId;


import java.io.IOException;
import java.io.RandomAccessFile;

import android.util.Log;

import ariel.utils.SharedPreferenceManager;

/**
 * Created by mikalackis on 29.7.16..
 */
public class DeviceInfoHelper {

    public static String getDeviceUID() {
        Log.i("LockPatternUtilsHelper", "About to check password");
        enforceCallingOrSelfPermission(ariel.platform.Manifest.permission.READ_DEVICE_UID, "You cant have device UID!!!");
        return UniqueDeviceId.getUniqueDeviceId();
    }

}