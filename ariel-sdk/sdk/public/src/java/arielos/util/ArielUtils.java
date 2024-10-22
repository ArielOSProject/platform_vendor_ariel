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

package arielos.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import arielos.providers.ArielSettings;
import arielos.providers.ArielSettings.ArielSettingNotFoundException;

/**
 * Utility methods used by the platform and apps
 */
public final class ArielUtils {

    private final String TAG = "ArielUtils";

    private Context mContext;

    public ArielUtils(Context context) {
        mContext = context;
    }

    public boolean isPanicModeActive() {
        try {
            int panicModeActive = ArielSettings.Secure.getInt(mContext.getContentResolver(),
                                                                ArielSettings.Secure.PANIC_MODE);
            return panicModeActive == 1;
        } catch(ArielSettingNotFoundException exception) {
            Log.e(TAG, "ArielSetting not found!");
        }
        return false;
    }

    /**
     * Check if uid matches com.ariel.guardian package
     */
    public boolean isArielGuardian(int uid) {
        PackageManager pm = mContext.getPackageManager();
        String[] packagesForUid = pm.getPackagesForUid(uid);
        if(packagesForUid != null && packagesForUid.length>0) {
            for(int i=0; i<packagesForUid.length; i++) {
                String packageName = packagesForUid[i];
                if(packageName.equals("com.ariel.guardian")) {
                    return true;
                }
            }
        }
        return false;
    }
}
