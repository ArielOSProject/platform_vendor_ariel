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

package com.arielos.internal.statusbar;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.app.StatusBarManager;

import arielos.app.ArielContextConstants;
import arielos.statusbar.ArielStatusBarInterface;

public class ArielStatusBarInterfaceImpl implements ArielStatusBarInterface {

    private static final String TAG = "ArielStatusBar";

    private Context mContext;

    public ArielStatusBarInterfaceImpl(Context context) {
        Context appContext = context.getApplicationContext();
        mContext = appContext == null ? context : appContext;
    }

    @Override
    public void setStatusBarDisabled(boolean disabled) {
        StatusBarManager statusBarManager = mContext.getSystemService(StatusBarManager.class);
        if (statusBarManager != null) {
            Log.v(TAG, "Disabling status bar");
            if(disabled) {
                statusBarManager.disable(StatusBarManager.DISABLE_MASK);
                statusBarManager.disable2(StatusBarManager.DISABLE2_MASK);
            } else {
                statusBarManager.disable(StatusBarManager.DISABLE_NONE);
                statusBarManager.disable2(StatusBarManager.DISABLE_NONE);
            }
        } else {
            Log.w(TAG,
                    "Skip disabling status bar - could not get StatusBarManager");
        }
        // destroy the session
        statusBarManager = null;
    }

}
