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

import java.util.Arrays;

import com.android.server.SystemService;

import ariel.app.ArielContextConstants;
import ariel.app.IArielFirewallManager;

import com.ariel.platform.internal.firewall.Api;

import ariel.platform.Manifest;

import java.util.List;
import java.util.ArrayList;

import com.ariel.platform.internal.R;

import com.ariel.platform.internal.daemon.ArielNativeDaemonConnector;
import com.ariel.platform.internal.daemon.IArielNativeDaemonConnectorCallbacks;
import com.ariel.platform.internal.daemon.ArielNativeDaemonConnector.Command;
import com.ariel.platform.internal.daemon.ArielNativeDaemonConnector.SensitiveArg;
import com.android.server.NativeDaemonConnectorException;
import com.android.server.NativeDaemonEvent;

import java.util.concurrent.CountDownLatch;


/**
 * Internal service which manages interactions with system ui elements
 *
 * @hide
 */
public class ArielFirewallService extends ArielSystemService implements IArielNativeDaemonConnectorCallbacks {
    private static final String TAG = "ArielFirewallService";

    private static final String ARIELFW_TAG = "ArielFW";

    private Context mContext;
    private Handler mHandler = new Handler();

    private final ArielNativeDaemonConnector mConnector;
    private final Thread mConnectorThread;

    /**
     * Maximum number of ASEC containers allowed to be mounted.
     */
    private static final int MAX_CONTAINERS = 250;

    // Two connectors - mConnector & mCryptConnector
    private final CountDownLatch mConnectedSignal = new CountDownLatch(1);

    public ArielFirewallService(Context context) {
        super(context);
        mContext = context;

        mConnector = new ArielNativeDaemonConnector(this, "arielfw", MAX_CONTAINERS * 2, ARIELFW_TAG, 25,
                null);
        mConnector.setDebug(true);
        //mConnector.setWarnIfHeld(mLock);
        mConnectorThread = new Thread(mConnector, ARIELFW_TAG);

        start();
    }

    private void start() {
        mConnectorThread.start();
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
        public void disableNetworking(String uids) {
            if (isCallerSystem()) {
                //Api.applyIptablesRulesImpl(mContext, uidsWifi, uids3g, true);
                try {
                    final Command cmd = new Command("disable_networking", uids);
                    Log.d(TAG, "List of uids: " + uids);
                    NativeDaemonEvent event = mConnector.execute(cmd);
                    Log.d(TAG, "Message from daemon: " + event.getMessage());
                } catch (NativeDaemonConnectorException e) {
                    int code = e.getCode();
                    e.printStackTrace();
                    Log.d(TAG, "Arielfw excetion: " + code + ", msg: " + e.getMessage());
//                    if (code == VoldResponseCode.OpFailedStorageBusy) {
//                        rc = StorageResultCode.OperationFailedStorageBusy;
//                    } else {
//                        rc = StorageResultCode.OperationFailedInternalError;
//                    }
                }
            } else {
                enforceSystemOrSystemUI("You have to be system to do this!!!");
            }
            Log.d(TAG, "applyIptablesRulesImpl completed!");
        }

        @Override
        public void clearRules() {
            if (isCallerSystem()) {
                try {
                    final Command cmd = new Command("clear_rules");
                    NativeDaemonEvent event = mConnector.execute(cmd);
                    Log.d(TAG, "Message from daemon: " + event.getMessage());
                } catch (NativeDaemonConnectorException e) {
                    int code = e.getCode();
                    e.printStackTrace();
                    Log.d(TAG, "Arielfw excetion: " + code + ", msg: " + e.getMessage());
//                    if (code == VoldResponseCode.OpFailedStorageBusy) {
//                        rc = StorageResultCode.OperationFailedStorageBusy;
//                    } else {
//                        rc = StorageResultCode.OperationFailedInternalError;
//                    }
                }
            } else

            {
                enforceSystemOrSystemUI("You have to be system to do this!!!");
            }
        }

    };

    public void onDaemonConnected() {
        Log.d(TAG, "Arielfw daemon connected");
        mConnectedSignal.countDown();
    }

    public boolean onCheckHoldWakeLock(int code) {
        return false;
    }

    public boolean onEvent(int code, String raw, String[] cooked) {
        // TODO: NDC translates a message to a callback, we could enhance NDC to
        // directly interact with a state machine through messages
//        NativeEvent event = new NativeEvent(code, raw, cooked);
//        mNsdStateMachine.sendMessage(NsdManager.NATIVE_DAEMON_EVENT, event);
        Log.d(TAG, "NativeDaemonEvent");
        return true;
    }

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
        mContext.enforceCallingPermission(Manifest.permission.ACCESS_FIREWALL_MANAGER,
                message);
    }

}
