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

import ariel.context.ArielContextConstants;
import ariel.firewall.IArielFirewallManager;

import ariel.platform.Manifest;

import com.ariel.platform.internal.ArielSystemService;
import com.ariel.platform.internal.daemon.ArielNativeDaemonConnector;
import com.ariel.platform.internal.daemon.IArielNativeDaemonConnectorCallbacks;
import com.ariel.platform.internal.daemon.ArielNativeDaemonConnector.Command;
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
        public boolean disableNetworking(String uids) {
            mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.ACCESS_FIREWALL_MANAGER, null);
            boolean result = false;
            try {
                final Command cmd = new Command("disable_networking", uids);
                Log.d(TAG, "List of uids: " + uids);
                NativeDaemonEvent event = mConnector.execute(cmd);
                Log.d(TAG, "Message from daemon: " + event.getMessage());
                result = true;
            } catch (NativeDaemonConnectorException e) {
                int code = e.getCode();
                e.printStackTrace();
                Log.d(TAG, "Arielfw excetion: " + code + ", msg: " + e.getMessage());
                result = false;
            }
            Log.d(TAG, "disableNetworking completed!");
            return result;
        }

        @Override
        public boolean clearRules() {
            mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.ACCESS_FIREWALL_MANAGER, null);
            boolean result = false;
            try {
                final Command cmd = new Command("clear_rules");
                NativeDaemonEvent event = mConnector.execute(cmd);
                Log.d(TAG, "Message from daemon: " + event.getMessage());
                result = true;
            } catch (NativeDaemonConnectorException e) {
                int code = e.getCode();
                e.printStackTrace();
                Log.d(TAG, "Arielfw excetion: " + code + ", msg: " + e.getMessage());
                result = false;
            }
            return result;
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

}
