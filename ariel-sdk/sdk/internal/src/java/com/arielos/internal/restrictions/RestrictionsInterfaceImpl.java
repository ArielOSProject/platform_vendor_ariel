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

package com.arielos.internal.restrictions;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.os.UserHandle;
import android.os.UserManager;

import arielos.app.ArielContextConstants;
import arielos.restrictions.RestrictionsInterface;

public class RestrictionsInterfaceImpl implements RestrictionsInterface {

    private static final String TAG = "RestrictionsInterface";

    private Context mContext;
    private UserManager mUserManager;

    public RestrictionsInterfaceImpl(Context context) {
        Context appContext = context.getApplicationContext();
        mContext = appContext == null ? context : appContext;
        mUserManager = UserManager.get(mContext);
    }

    @Override
    public void disallowFactoryReset(boolean disallow) {
        Log.i(TAG, "disallowFactoryReset = "+disallow);
        mUserManager.setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, disallow,
            new UserHandle(UserHandle.myUserId()));
    }

    @Override
    public void disallowDebuggingFeatures(boolean disallow) {
        Log.i(TAG, "disallowDebuggingFeatures = "+disallow);
        mUserManager.setUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES, disallow,
            new UserHandle(UserHandle.myUserId()));
    }

}
