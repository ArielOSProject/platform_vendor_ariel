/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <string>

class FirewallManager {

private:
    static FirewallManager *sInstance;

    const int SPECIAL_UID_ANY	= -10;

    const int FW_ERROR = -99;

    const std::string ITFS_WIFI[4] = {"tiwlan+", "wlan+", "eth+", "ra+"};
    const std::string ITFS_3G[8] = {"rmnet+","pdp+","ppp+","uwbr+","wimax+","vsnet+","ccmni+","usb+"};

    int indexOf(int array[], int length, int seek);

public:
    virtual ~FirewallManager();

    int prepareFirewall();

    int disableWifi(int pid[], int pid_size);
    int enableWifi(int pid[], int pid_size);
    int disableData(int pid[], int pid_size);
    int enableData(int pid[], int pid_size);
    int disableNetworking(int pid[], int pid_size);
    int clearRules();

    static FirewallManager *Instance();

private:
    FirewallManager();
};

