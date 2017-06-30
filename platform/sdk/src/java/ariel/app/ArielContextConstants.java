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

package ariel.app;

import android.annotation.SdkConstant;

/**
 * @hide
 * TODO: We need to somehow make these managers accessible via getSystemService
 */
public final class ArielContextConstants {

    /**
     * @hide
     */
    private ArielContextConstants() {
        // Empty constructor
    }

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link cyanogenmod.app.ArielFirewallManager} for informing the user of
     * background events.
     *
     * @see android.content.Context#getSystemService
     * @see cyanogenmod.app.ArielFirewallManager
     */
    public static final String ARIEL_FIREWALL_SERVICE = "arielfirewall";

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link cyanogenmod.app.ArielFirewallManager} for informing the user of
     * background events.
     *
     * @see android.content.Context#getSystemService
     * @see cyanogenmod.app.ArielFirewallManager
     */
    public static final String ARIEL_INTENT_FIREWALL_SERVICE = "arielintentfirewall";

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link cyanogenmod.app.ArielFirewallManager} for informing the user of
     * background events.
     *
     * @see android.content.Context#getSystemService
     * @see cyanogenmod.app.ArielFirewallManager
     */
    public static final String ARIEL_HARDWARE_SERVICE = "arielhardwareservice";

    /**
     * Features supported by the CMSDK.
     */
    public static class Features {

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the cm status bar service
         * utilzed by the cmsdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String FIREWALL = "com.ariel.firewall";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the cm status bar service
         * utilzed by the cmsdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String HARDWARE = "com.ariel.hardware";

    }
}
