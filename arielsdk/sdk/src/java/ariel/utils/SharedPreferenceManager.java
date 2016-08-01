package ariel.utils;

/**
 * Created by mikalackis on 1.8.16..
 */

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author mikalackis
 *         <p>
 *         Helper class for storing shared prefferences
 */
public final class SharedPreferenceManager {

    public static final String SHARED_PREFS = "ariel_preferences";

    /**
     * keys
     */
    public static final String KEY_LOCKSCREEN_PASSWORD = "ariel_lock_password";
    public static final String KEY_LOCKSCREEN_PATTERN = "ariel_lock_pattern";

    private static SharedPreferenceManager sInstance;
    private Context mContext;

    public static SharedPreferenceManager getInstance(final Context context) {
        if(sInstance == null){
            sInstance = new SharedPreferenceManager(context);
        }
        return sInstance;
    }

    private SharedPreferenceManager(final Context ctx) {
        mContext = ctx;
    }

    public Integer getIntegerPreference(final String key, final int defaultValue) {
        final SharedPreferences settings = mContext.getSharedPreferences(SHARED_PREFS, 0);
        return settings.getInt(key, defaultValue);
    }

    public void setIntegerPreference(final String key, final Integer value) {
        final SharedPreferences settings = mContext.getSharedPreferences(SHARED_PREFS, 0);
        final SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public long getLongPreference(final String key, final long defaultValue) {
        final SharedPreferences settings = mContext.getSharedPreferences(SHARED_PREFS, 0);
        return settings.getLong(key, defaultValue);
    }

    public void setLongPreference(final String key, final long value) {
        final SharedPreferences settings = mContext.getSharedPreferences(SHARED_PREFS, 0);
        final SharedPreferences.Editor editor = settings.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public boolean getBoolPreference(final String key, final boolean defaultValue) {
        final SharedPreferences settings = mContext.getSharedPreferences(SHARED_PREFS, 0);
        return settings.getBoolean(key, defaultValue);
    }

    public void setBoolPreference(final String key, final boolean value) {
        final SharedPreferences settings = mContext.getSharedPreferences(SHARED_PREFS, 0);
        final SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public String getStringPreference(final String key, final String defaultValue) {
        final SharedPreferences settings = mContext.getSharedPreferences(SHARED_PREFS, 0);
        return settings.getString(key, defaultValue);
    }

    public void setStringPreference(final String key, final String value) {
        final SharedPreferences settings = mContext.getSharedPreferences(SHARED_PREFS, 0);
        final SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

}