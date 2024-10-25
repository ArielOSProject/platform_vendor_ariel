package arielos.os;

import android.os.SystemProperties;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Wrapper class for accessing Neon and AOSP system properties.
 */
public final class ArielSystemProperties {

    /**
     * System property that sets recovery update after OTA to on or off
     *
     * Contains a boolean as a String value
     */
    public static final String UPDATE_RECOVERY_PROP = "persist.vendor.recovery_update";

    private ArielSystemProperties(){}

    /**
     * Read a system property
     * @param context app context
     * @param key system property key
     * @return system property value
     *
     * Requires ariel.permission.READ_SYSTEM_PROPERTIES permission.
     */
    public static final String get(Context context, String key){
        if(context.checkCallingOrSelfPermission(
                        arielos.platform.Manifest.permission.READ_SYSTEM_PROPERTIES) !=
                        PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException(
            String.format("Permission denial: reading Neon system properties requires %1$s",
                        arielos.platform.Manifest.permission.READ_SYSTEM_PROPERTIES));
        }
        return SystemProperties.get(key);
    }

    /**
     * Set a system property
     * @param context app context
     * @param key system property key
     * @return system property value
     *
     * Requires ariel.permission.SET_SYSTEM_PROPERTIES permission.
     */
    public static final void set(Context context, String key, String value){
        if(context.checkCallingOrSelfPermission(
                        arielos.platform.Manifest.permission.WRITE_SYSTEM_PROPERTIES) !=
                        PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException(
            String.format("Permission denial: reading Ariel system properties requires %1$s",
                        arielos.platform.Manifest.permission.WRITE_SYSTEM_PROPERTIES));
        }
        SystemProperties.set(key, value);
    }

}