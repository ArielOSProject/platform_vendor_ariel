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

package arielos.security;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import arielos.app.ArielContextConstants;
import arielos.security.IEscrowTokenStateChangeCallback;

public interface SecurityInterface {
    /**
     * Allows an application to use the Trust interface to display trusted
     * security messages to the user.
     * This is a system-only permission, user-installed apps cannot use it
     */
    public static final String SECURITY_INTERFACE_PERMISSION = "arielos.permission.MANAGE_SECURITY";

    public void generateEscrowToken(int userId, byte[] token, IEscrowTokenStateChangeCallback callback);

    public boolean hasPendingEscrowToken(int userId);

    public long setLockoutAttemptDeadline(int userId, int timeoutMs);

    public boolean unlockUserWithToken(long tokenHandle, byte[] token, int userId);

    public boolean removeEscrowToken(long handle, int userId);
    
    public boolean isEscrowTokenActive(long handle, int userId);

    public boolean setLockCredentialWithToken(byte[] credential, int type, int requestedQuality, long tokenHandle, byte[] token, int userId);
}
