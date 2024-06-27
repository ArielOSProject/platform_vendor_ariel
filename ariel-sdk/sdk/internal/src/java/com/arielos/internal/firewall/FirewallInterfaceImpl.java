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

package com.arielos.internal.firewall;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import java.util.List;

import android.net.NetworkPolicyManager;
import android.net.INetworkPolicyListener;

import arielos.app.ArielContextConstants;
import arielos.firewall.FirewallInterface;
import arielos.firewall.FirewallPolicyListener;
import arielos.platform.Manifest;

import static android.net.NetworkPolicyManager.POLICY_REJECT_CELLULAR;
import static android.net.NetworkPolicyManager.POLICY_REJECT_VPN;
import static android.net.NetworkPolicyManager.POLICY_REJECT_WIFI;
import static android.net.NetworkPolicyManager.POLICY_REJECT_ALL;
import static android.net.NetworkPolicyManager.POLICY_NONE;

/**
 * ArielOS firewall manager
 */
public class FirewallInterfaceImpl implements FirewallInterface {
    private static final String TAG = "FirewallInterface";
    private static boolean localLOGV = false;

    private Context mContext;

    private NetworkPolicyManager networkManager;

    private FirewallPolicyListener mFirewallPolicyListener = null;

    private final INetworkPolicyListener mNetPolicyListener = new NetworkPolicyManager.Listener() {
        @Override
        public void onUidPoliciesChanged(int uid, int uidPolicies) {
            if (localLOGV) {
                Log.v(TAG, "onUidPoliciesChanged: " + uid);
            }
            if(mFirewallPolicyListener != null) {
                mFirewallPolicyListener.onUidPoliciesChanged(uid, uidPolicies);
            }
        }
    };


    public FirewallInterfaceImpl(Context context) {
        Context appContext = context.getApplicationContext();
        mContext = appContext == null ? context : appContext;
        networkManager = (NetworkPolicyManager) context
                .getSystemService(Context.NETWORK_POLICY_SERVICE);

        if (!context.getPackageManager().hasSystemFeature(
                ArielContextConstants.Features.FIREWALL)) {
            Log.wtf(TAG, "Unable to get Ariel Firewall feature.");
        }

    }

    @Override
    public void restrictWifi(int uid, boolean restrict) {
        mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.FIREWALL, "You are not allowed to use ArielOS Firewall feature.");
        if (localLOGV) Log.v(TAG, "Invoking restrictWifi on "+uid+" with restrict: "+restrict+" and policy: "+POLICY_REJECT_WIFI);
        if (restrict) {
            networkManager.addUidPolicy(uid, POLICY_REJECT_WIFI);
        } else {
            networkManager.removeUidPolicy(uid, POLICY_REJECT_WIFI);
        }
    }

    @Override
    public void restrictCellular(int uid, boolean restrict) {
        mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.FIREWALL, "You are not allowed to use ArielOS Firewall feature.");
        if (localLOGV) Log.v(TAG, "Invoking restrictCellular on "+uid+" with restrict: "+restrict+" and policy: "+POLICY_REJECT_CELLULAR);
        if (restrict) {
            networkManager.addUidPolicy(uid, POLICY_REJECT_CELLULAR);
        } else {
            networkManager.removeUidPolicy(uid, POLICY_REJECT_CELLULAR);
        }
    }

    @Override
    public void restrictVpn(int uid, boolean restrict) {
        mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.FIREWALL, "You are not allowed to use ArielOS Firewall feature.");
        if (localLOGV) Log.v(TAG, "Invoking restrictVpn on "+uid+" with restrict: "+restrict+" and policy: "+POLICY_REJECT_VPN);
        if (restrict) {
            networkManager.addUidPolicy(uid, POLICY_REJECT_VPN);
        } else {
            networkManager.removeUidPolicy(uid, POLICY_REJECT_VPN);
        }
    }

    @Override
    public void restrictNetworking(int uid, boolean restrict) {
        mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.FIREWALL, "You are not allowed to use ArielOS Firewall feature.");
        if (localLOGV) Log.v(TAG, "Invoking restrictNetworking on "+uid+" with restrict: "+restrict+" and policy: "+POLICY_REJECT_ALL);
        if (restrict) {
            networkManager.addUidPolicy(uid, POLICY_REJECT_ALL);
        } else {
            networkManager.removeUidPolicy(uid, POLICY_REJECT_ALL);
        }
    }

    @Override
    public void registerListener(FirewallPolicyListener listener) {
        mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.FIREWALL, "You are not allowed to use ArielOS Firewall feature.");
        if (localLOGV) Log.v(TAG, "Invoking register listener");
        if(mFirewallPolicyListener == null) {
            // register listener
            networkManager.registerListener(mNetPolicyListener);
            mFirewallPolicyListener = listener;
        } else {
            if (localLOGV) Log.v(TAG, "Listener already registered!!");
        }
    }

    @Override
    public void unregisterListener() {
        mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.FIREWALL, "You are not allowed to use ArielOS Firewall feature.");
        if (localLOGV) Log.v(TAG, "Invoking unregisterListener");
        networkManager.unregisterListener(mNetPolicyListener);
        mFirewallPolicyListener = null;
    }

    @Override
    public int networkPolicyToFirewallPolicy(int policy) {
        mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.FIREWALL, "You are not allowed to use ArielOS Firewall feature.");
        if (localLOGV) Log.v(TAG, "Invoking networkPolicyToFirewallPolicy");
        int firewallPolicy = FirewallInterface.FIREWALL_NONE;

        if (policy != 0 && (policy & POLICY_REJECT_CELLULAR) == POLICY_REJECT_CELLULAR) {
            policy &= ~POLICY_REJECT_CELLULAR;
            firewallPolicy |= FirewallInterface.FIREWALL_REJECT_CELLULAR;
        }

        if (policy != 0 && (policy & POLICY_REJECT_VPN) == POLICY_REJECT_VPN) {
            policy &= ~POLICY_REJECT_VPN;
            firewallPolicy |= FirewallInterface.FIREWALL_REJECT_VPN;
        }

        if (policy != 0 && (policy & POLICY_REJECT_WIFI) == POLICY_REJECT_WIFI) {
            policy &= ~POLICY_REJECT_WIFI;
            firewallPolicy |= FirewallInterface.FIREWALL_REJECT_WIFI;
        }

        if (policy != 0 && (policy & POLICY_REJECT_ALL) == POLICY_REJECT_ALL) {
            policy &= ~POLICY_REJECT_ALL;
            firewallPolicy |= FirewallInterface.FIREWALL_REJECT_ALL;
        }

        return firewallPolicy;
    }

    @Override
    public int getUidPolicy(int uid) {
        mContext.enforceCallingOrSelfPermission(
                    Manifest.permission.FIREWALL, "You are not allowed to use ArielOS Firewall feature.");
        if (localLOGV) Log.v(TAG, "Invoking getUidPolicy for uid: "+uid);
        int activePolicy = networkManager.getUidPolicy(uid);
        if (localLOGV) {
            Log.d(TAG, "UID "+uid+" has active policy: "+activePolicy);
            if((activePolicy & POLICY_REJECT_CELLULAR) == POLICY_REJECT_CELLULAR) {
                Log.d(TAG, "POLICY_REJECT_CELLULAR active!");
            }
            if((activePolicy & POLICY_REJECT_WIFI) == POLICY_REJECT_WIFI) {
                Log.d(TAG, "POLICY_REJECT_WIFI active!");
            }
            if((activePolicy & POLICY_REJECT_ALL) == POLICY_REJECT_ALL) {
                Log.d(TAG, "POLICY_REJECT_ALL active!");
            }

            Log.d(TAG, "Printing policy from NetworkPolicyManager!");
            String policyToString = NetworkPolicyManager.uidPoliciesToString(activePolicy);
            Log.d(TAG, "Active network manager policy:\n"+policyToString);
        }
        return activePolicy;
    }

}
