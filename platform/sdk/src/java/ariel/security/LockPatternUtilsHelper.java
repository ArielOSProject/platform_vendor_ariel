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


import java.io.IOException;
import java.io.RandomAccessFile;

import android.util.Log;

import ariel.utils.SharedPreferenceManager;

/**
 * Created by mikalackis on 29.7.16..
 */
public class LockPatternUtilsHelper {

    private static final String SYSTEM_DIRECTORY = "/system/";
    private static final String LOCK_PATTERN_FILE = "gatekeeper.pattern.key";
    private static final String LOCK_PASSWORD_FILE = "gatekeeper.password.key";

    public static void performAdminLock(String password, Context context) {
        Log.i("LockPatternUtilsHelper", "About to check password");
        enforceWritePermission(ariel.platform.Manifest.permission.LOCK_SCREEN, context);
        LockPatternUtils lpu = new LockPatternUtils(context);
        // first check & backup existing password/pattern
        byte[] oldPwd = getUnlockPassword();
        if(oldPwd != null && oldPwd.length>0){
            String pwdByteAsString = Base64.encodeToString(oldPwd, Base64.NO_WRAP);
            SharedPreferenceManager.getInstance(context).setStringPreference(SharedPreferenceManager.KEY_LOCKSCREEN_PASSWORD, pwdByteAsString);
        }
        else{
            // check if pattern exists
            oldPwd = getUnlockPattern();
            if(oldPwd != null && oldPwd.length>0){
                String pwdByteAsString = Base64.encodeToString(oldPwd, Base64.NO_WRAP);
                SharedPreferenceManager.getInstance(context).setStringPreference(SharedPreferenceManager.KEY_LOCKSCREEN_PATTERN, pwdByteAsString);
            }
        }

        lpu.clearLock(UserHandle.USER_OWNER);
        lpu.saveLockPassword(password, null,
                DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX, UserHandle.USER_OWNER);

        lpu.requireCredentialEntry(UserHandle.USER_ALL);
        try {
            WindowManagerGlobal.getWindowManagerService().lockNow(null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private static void enforceWritePermission(String permission, Context context) {
        Log.i("LockPatternUtilsHelper", "Permission check: "+context.checkCallingOrSelfPermission(permission));
        if (context.checkCallingOrSelfPermission(permission)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException(
                    String.format("Permission denial: writing to settings requires %s",
                            permission));
        }
    }

    public static void clearLock(Context context) {
        LockPatternUtils lpu = new LockPatternUtils(context);
        lpu.clearLock(UserHandle.USER_OWNER);
        // restore password/pattern
        String pwd = SharedPreferenceManager.getInstance(context).getStringPreference(SharedPreferenceManager.KEY_LOCKSCREEN_PASSWORD, "");
        if(pwd!=null && pwd.length()>0){
            savePassword(pwd.getBytes());
        }
        else{
            // try to restore pattern
            pwd = SharedPreferenceManager.getInstance(context).getStringPreference(SharedPreferenceManager.KEY_LOCKSCREEN_PATTERN, "");
            if(pwd!=null && pwd.length()>0){
                savePattern(pwd.getBytes());
            }
        }

    }

    public static void disableNetworks(Context context, int uid, boolean restrictBackground){
        NetworkPolicyManager mPolicyManager = NetworkPolicyManager.from(context);
        Log.i("LockPatternUtilsHelper", "Policy: "+mPolicyManager.getUidPolicy(uid));
        mPolicyManager.setUidPolicy(uid, NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND);
        mPolicyManager.setRestrictBackground(restrictBackground);
        // some stupid comment so we can build the fucking library  sdfdsfsdfsdf
    }

    public static byte[] getUnlockPassword() {
        String dataSystemDirectory = android.os.Environment.getDataDirectory()
                .getAbsolutePath() + SYSTEM_DIRECTORY;
        String sLockPasswordFilename = dataSystemDirectory + LOCK_PASSWORD_FILE;
        try {
            RandomAccessFile raf = new RandomAccessFile(sLockPasswordFilename, "r");
            final byte[] stored = new byte[(int) raf.length()];
            return stored;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] getUnlockPattern() {
        String dataSystemDirectory = android.os.Environment.getDataDirectory()
                .getAbsolutePath() + SYSTEM_DIRECTORY;
        String sLockPatternFilename = dataSystemDirectory + LOCK_PATTERN_FILE;
        try {
            RandomAccessFile raf = new RandomAccessFile(sLockPatternFilename, "r");
            final byte[] stored = new byte[(int) raf.length()];
            return stored;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void savePassword(byte[] pass) {
        String dataSystemDirectory = android.os.Environment.getDataDirectory()
                .getAbsolutePath() + SYSTEM_DIRECTORY;
        String sLockPasswordFilename = dataSystemDirectory + LOCK_PASSWORD_FILE;
        resetPassFile();
        try {
            // Write the hash to file
            RandomAccessFile raf = new RandomAccessFile(sLockPasswordFilename, "rw");
            // Truncate the file if pattern is null, to clear the lock
            if (pass == null) {
                raf.setLength(0);
            } else {
                raf.write(pass, 0, pass.length);
            }
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void resetPassFile() {
        String dataSystemDirectory = android.os.Environment.getDataDirectory()
                .getAbsolutePath() + SYSTEM_DIRECTORY;
        String sLockPasswordFilename = dataSystemDirectory + LOCK_PASSWORD_FILE;

        try {
            // Write the hash to file
            RandomAccessFile raf = new RandomAccessFile(sLockPasswordFilename, "rw");
            // Truncate the file if pattern is null, to clear the lock
            raf.setLength(0);
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void resetPatternFile() {
        String dataSystemDirectory = android.os.Environment.getDataDirectory()
                .getAbsolutePath() + SYSTEM_DIRECTORY;
        String sLockPatternFilename = dataSystemDirectory + LOCK_PATTERN_FILE;

        try {
            // Write the hash to file
            RandomAccessFile raf = new RandomAccessFile(sLockPatternFilename, "rw");
            // Truncate the file if pattern is null, to clear the lock
            raf.setLength(0);
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void savePattern(byte[] pattern) {
        String dataSystemDirectory = android.os.Environment.getDataDirectory()
                .getAbsolutePath() + SYSTEM_DIRECTORY;
        String sLockPatternFilename = dataSystemDirectory + LOCK_PATTERN_FILE;
        resetPatternFile();
        try {
            // Write the hash to file
            RandomAccessFile raf = new RandomAccessFile(sLockPatternFilename, "rw");
            // Truncate the file if pattern is null, to clear the lock
            if (pattern == null) {
                raf.setLength(0);
            } else {
                raf.write(pattern, 0, pattern.length);
            }
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}