/**
 * Copyright (C) 2018-2019 The ArielOS Project
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

package arielos.firewall;

/**
 * Network firewall interface that allows controlling network access per app.
 */
public interface FirewallInterface {

    /**
     * Constant values are based on constants from NetworkPolicyManager
     */
    public static final int FIREWALL_REJECT_ALL = 0x40000;

    /** Reject network usage on cellular network
     */
    public static final int FIREWALL_REJECT_CELLULAR = 0x10000;

    /** Reject network usage on wifi network
     */
    public static final int FIREWALL_REJECT_WIFI = 0x8000;

    /** Reject network usage on virtual private network
     */
    public static final int FIREWALL_REJECT_VPN = 0x20000;

    /**
     * No specific network policy, use system default.
     */
    public static final int FIREWALL_NONE = 0x0;

    /**
     * Reject access to mobile data from background
     */
    public static final int FIREWALL_REJECT_METERED_BACKGROUND = 0x1;

    /**
     * Allow access to mobile data from background
     */
    public static final int FIREWALL_ALLOW_METERED_BACKGROUND = 0x4;

    /**
     * Restricts access to wifi to the provided UID.
     *
     * @param uid can be retrieved using package manager, with package name
     */
    public void restrictWifi(int uid, boolean restrict);

    /**
     * Restricts access to cellular to the provided UID.
     *
     * @param uid can be retrieved using package manager, with package name
     */
    public void restrictCellular(int uid, boolean restrict);

    /**
     * Restricts access to vpn to the provided UID.
     *
     * @param uid can be retrieved using package manager, with package name
     */
    public void restrictVpn(int uid, boolean restrict);

    /**
     * Restricts access to all network operations to the provided UID.
     *
     * @param uid can be retrieved using package manager, with package name
     */
    public void restrictNetworking(int uid, boolean restrict);

    /**
     * Register listener to receive changes related to network policy changes
     *
     * Note: only one listener can be registered per instance.
     *
     * @param [FirewallPolicyListener] instance of the listener
     */
    public void registerListener(FirewallPolicyListener listener);

    /**
     * Unregister listener to receive changes related to network policy changes
     *
     * Note: only one listener can be registered per instance.
     */
    public void unregisterListener();

    /**
     * Convert network policy provided by the NetworkPolicyManager
     * to our own firewall policy constant (ie FIREWALL_REJECT_ALL)
     */
    public int networkPolicyToFirewallPolicy(int policy);

    /**
     * Retrieve current UID policy
     */
    public int getUidPolicy(int uid);

    /**
     * Restricts background usage of mobile data.
     *
     * @param uid can be retrieved using package manager, with package name
     */
    public void restrictMeteredBackground(int uid, boolean restrict);

}
