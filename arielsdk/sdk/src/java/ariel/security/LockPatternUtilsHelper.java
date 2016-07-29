package ariel.security;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.view.WindowManagerGlobal;
import com.android.internal.widget.LockPatternUtils;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by mikalackis on 29.7.16..
 */
public class LockPatternUtilsHelper {

    private static final String SYSTEM_DIRECTORY = "/system/";
    private static final String LOCK_PATTERN_FILE = "gesture.key";
    private static final String LOCK_PASSWORD_FILE = "password.key";

    public static void performAdminLock(String password, Context context) {
        enforceWritePermission(ariel.platform.Manifest.permission.LOCK_SCREEN, context);
        LockPatternUtils lpu = new LockPatternUtils(context);
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
    }

    public byte[] getUnlockPassword() {
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

    public byte[] getUnlockPattern() {
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

    public void savePassword(byte[] pass) {
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

    public void resetPassFile() {
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

    public void resetPatternFile() {
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

    public void savePattern(byte[] pattern) {
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