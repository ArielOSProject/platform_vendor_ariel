/**
 * Copyright (C) 2018-2019 The LineageOS Project
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

package lineageos.arielos.security;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import lineageos.app.LineageContextConstants;

public interface SecurityInterface {
    /**
     * Allows an application to use the Trust interface to display trusted
     * security messages to the user.
     * This is a system-only permission, user-installed apps cannot use it
     */
    public static final String SECURITY_INTERFACE_PERMISSION = "arielos.permission.SECURITY_INTERFACE";

    public void generateEscrowToken();

    public boolean hasPendingEscrowToken(int userId);
}
