/**
 * Copyright (c) 2015, The CyanogenMod Project
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ariel.platform.internal.firewall;

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import ariel.app.ArielContextConstants;
import ariel.app.IArielIntentFirewallManager;

import ariel.platform.Manifest;

import com.ariel.platform.internal.ArielSystemService;

import java.io.File;
import java.io.FileOutputStream;

import java.lang.StringBuffer;


/**
 * Internal service which manages interactions with system ui elements
 *
 * @hide
 */
public class ArielIntentFirewallService extends ArielSystemService {
    private static final String TAG = "ArielIntentFirewallService";

    private static final String ARIELFW_TAG = "ArielIntentFW";

    private final String RULES_DIR = "/data/system/ifw";
    private final String RULE_FILE = "%s.xml";

    private Context mContext;
    private Handler mHandler = new Handler();

    public ArielIntentFirewallService(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getFeatureDeclaration() {
        return ArielContextConstants.Features.FIREWALL;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "registerArielFirewall arielfirewall: " + this);
        publishBinderService(ArielContextConstants.ARIEL_INTENT_FIREWALL_SERVICE, mService);
    }

    private final IBinder mService = new IArielIntentFirewallManager.Stub() {

        @Override
        public void disableApp(String packageName) {
            mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.INTENT_FIREWALL, null);
            createRuleFile(packageName);
            Log.d(TAG, "disableApp completed!");
        }

        @Override
        public void enableApp(String packageName) {
            mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.INTENT_FIREWALL, null);
            removeRuleFile(packageName);
            Log.d(TAG, "enableApp completed!");
        }

    };

    private boolean createRuleFile(final String packageName) {
        try {
            Log.d(TAG, "Creating file");
            File rulesDir = new File(RULES_DIR, String.format(RULE_FILE, packageName));
            FileOutputStream fos = new FileOutputStream(rulesDir);
            StringBuffer sb = new StringBuffer();
            sb.append("<rules>\n");
            // write activity rules
            sb.append("<activity block=\"true\" log=\"true\">\n");
            sb.append("<intent-filter />\n");
            sb.append("<package-filter name=\"" + packageName + "\" />\n");
            sb.append("</activity>\n");
            // write service rules
            sb.append("<service block=\"true\" log=\"true\">\n");
            sb.append("<intent-filter />\n");
            sb.append("<package-filter name=\"" + packageName + "\" />\n");
            sb.append("</service>\n");
            // write broadcast rules
            sb.append("<broadcast block=\"true\" log=\"true\">\n");
            sb.append("<intent-filter />\n");
            sb.append("<package-filter name=\"" + packageName + "\" />\n");
            sb.append("</broadcast>\n");

            sb.append("</rules>");
            fos.write(sb.toString().getBytes());
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean removeRuleFile(final String packageName) {
        try {
            Log.d(TAG, "Creating file");
            File rulesDir = new File(RULES_DIR, String.format(RULE_FILE, packageName));
            rulesDir.delete();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean onCheckHoldWakeLock(int code) {
        return false;
    }

//    private static void checkCallerIsSystemOrSameApp(String pkg) {
//        if (isCallerSystem()) {
//            return;
//        }
//        final int uid = Binder.getCallingUid();
//        try {
//            ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(
//                    pkg, 0, UserHandle.getCallingUserId());
//            if (ai == null) {
//                throw new SecurityException("Unknown package " + pkg);
//            }
//            if (!UserHandle.isSameApp(ai.uid, uid)) {
//                throw new SecurityException("Calling uid " + uid + " gave package"
//                        + pkg + " which is owned by uid " + ai.uid);
//            }
//        } catch (RemoteException re) {
//            throw new SecurityException("Unknown package " + pkg + "\n" + re);
//        }
//    }

//    private static boolean isUidSystem(int uid) {
//        final int appid = UserHandle.getAppId(uid);
//        return (appid == android.os.Process.SYSTEM_UID
//                || uid == 0);
//    }

}
