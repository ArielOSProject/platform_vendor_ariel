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

package com.ariel.platform.internal;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;

import com.android.server.SystemService;

import ariel.app.ArielContextConstants;
import ariel.app.IArielFirewallManager;
import com.ariel.platform.internal.firewall.Api;
import ariel.platform.Manifest;
import java.util.List;

import java.util.ArrayList;

import com.ariel.platform.internal.R;

/**
 * Internal service which manages interactions with system ui elements
 * @hide
 */
public class ArielFirewallService extends ArielSystemService {
    private static final String TAG = "ArielFirewallService";

    private Context mContext;
    private Handler mHandler = new Handler();

    public ArielFirewallService(Context context) {
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
        publishBinderService(ArielContextConstants.ARIEL_FIREWALL_SERVICE, mService);

    }

    private final IBinder mService = new IArielFirewallManager.Stub() {

        @Override
        public void applyIptablesRulesImpl(int[] uidsWifi, int[] uids3g, boolean showErrors) {
            if(isCallerSystem()) {
                Api.applyIptablesRulesImpl(mContext, uidsWifi, uids3g, true);
            }
            else{
                enforceSystemOrSystemUI("You have to be system to do this!!!");
            }
        }

        @Override
        public void purgeIptables() {
            if(isCallerSystem()) {
                Api.purgeIptables(mContext, true);
            }
            else{
                enforceSystemOrSystemUI("You have to be system to do this!!!");
            }
        }

    };

    private static void checkCallerIsSystemOrSameApp(String pkg) {
        if (isCallerSystem()) {
            return;
        }
        final int uid = Binder.getCallingUid();
        try {
            ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(
                    pkg, 0, UserHandle.getCallingUserId());
            if (ai == null) {
                throw new SecurityException("Unknown package " + pkg);
            }
            if (!UserHandle.isSameApp(ai.uid, uid)) {
                throw new SecurityException("Calling uid " + uid + " gave package"
                        + pkg + " which is owned by uid " + ai.uid);
            }
        } catch (RemoteException re) {
            throw new SecurityException("Unknown package " + pkg + "\n" + re);
        }
    }

    private static boolean isUidSystem(int uid) {
        final int appid = UserHandle.getAppId(uid);
        return (appid == android.os.Process.SYSTEM_UID
                || uid == 0);
    }

    private static boolean isCallerSystem() {
        return isUidSystem(Binder.getCallingUid());
    }

    private void enforceSystemOrSystemUI(String message) {
        mContext.enforceCallingPermission(Manifest.permission.APPLICATION_FIREWALL,
                message);
    }

}
