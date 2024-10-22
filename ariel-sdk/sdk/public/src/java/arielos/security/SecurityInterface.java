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
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.RequiresPermission;
import arielos.platform.Manifest;

import arielos.app.ArielContextConstants;
import arielos.security.IEscrowTokenStateChangeCallback;

public interface SecurityInterface {

    /**
     * Broadcast Action
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @RequiresPermission(Manifest.permission.MANAGE_SECURITY)
    public static final String ARIEL_ACTION_PHONE_UNLOCKED = "arielos.intent.action.PHONE_UNLOCKED";

    /**
     * Broadcast Action
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    @RequiresPermission(Manifest.permission.MANAGE_SECURITY)
    public static final String ARIEL_ACTION_PHONE_LOCKED = "arielos.intent.action.PHONE_LOCKED";

    public void generateEscrowToken(int userId, byte[] token, IEscrowTokenStateChangeCallback callback);

    public boolean hasPendingEscrowToken(int userId);

    public long setLockoutAttemptDeadline(int userId, int timeoutMs);

    public long getLockoutAttemptDeadline(int userId);

    public boolean removeEscrowToken(long handle, int userId);

    public boolean isEscrowTokenActive(long handle, int userId);

    public boolean setLockCredentialWithToken(byte[] credential, int type, long tokenHandle, byte[] token, int userId);

    public boolean startPeeking();

    public boolean stopPeeking();

    public boolean isKeyguardShowing();

    public void setLockoutAttemptIndeterminate(int userId, boolean isActive);

    public boolean getLockoutAttemptIndeterminate(int userId);

    // NOTE: When modifying this, make sure credential sufficiency validation logic is intact.
    public static final int CREDENTIAL_TYPE_NONE = -1;
    public static final int CREDENTIAL_TYPE_PATTERN = 1;
    // This is the legacy value persisted on disk. Never return it to clients, but internally
    // we still need it to handle upgrade cases.
    public static final int CREDENTIAL_TYPE_PASSWORD_OR_PIN = 2;
    public static final int CREDENTIAL_TYPE_PIN = 3;
    public static final int CREDENTIAL_TYPE_PASSWORD = 4;
}
