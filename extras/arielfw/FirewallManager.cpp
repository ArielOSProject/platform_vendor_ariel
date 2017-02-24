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


#define LOG_TAG "FirewallManager"

#include <sstream>
#include <stdlib.h>
#include <iostream>
#include <algorithm>
#include <sys/wait.h>
#include <sys/types.h>
#include <unistd.h>
#include <errno.h>

#include <cutils/klog.h>
#include <android-base/logging.h>

#include "FirewallManager.h"

FirewallManager *FirewallManager::sInstance = NULL;

FirewallManager *FirewallManager::Instance() {
        if (!sInstance)
                sInstance = new FirewallManager();
        return sInstance;
}

FirewallManager::FirewallManager() {
        setenv("ANDROID_LOG_TAGS", "*:v", 1);
        android::base::InitLogging(NULL, android::base::LogdLogger(android::base::SYSTEM));
// constructor
}

FirewallManager::~FirewallManager() {
// destructor
}

int FirewallManager::prepareFirewall() {
        std::stringstream ss;
        //Create the arielfw chains if necessary
        ss << "iptables -L arielfw >/dev/null 2>/dev/null || iptables --new arielfw || exit 2" << "&&"
           << "iptables -L arielfw-3g >/dev/null 2>/dev/null || iptables --new arielfw-3g || exit 3" << "&&"
           << "iptables -L arielfw-wifi >/dev/null 2>/dev/null || iptables --new arielfw-wifi || exit 4" << "&&"
           << "iptables -L arielfw-reject >/dev/null 2>/dev/null || iptables --new arielfw-reject || exit 5" << "&&"
        // Add arielfw chain to OUTPUT chain if necessary
        // PROBAJ DA DODAS I INPUT PRAVILO ZA ARIELFW
        << "iptables -L OUTPUT | grep -q arielfw || iptables -A OUTPUT -j arielfw || exit 6" << "&&"
        << "iptables -L INPUT | grep -q arielfw || iptables -A INPUT -j arielfw || exit 7" << "&&"
        // Flush existing rules
        << "iptables -F arielfw || exit 8" << "&&"
        << "iptables -F arielfw-3g || exit 9" << "&&"
        << "iptables -F arielfw-wifi || exit 10" << "&&"
        << "iptables -F arielfw-reject || exit 11" << "&&"
        //create reject rule with log support
        << "iptables -A arielfw-reject -j LOG --log-prefix \"[ARIELFW] \" --log-uid" << "&&"
        << "iptables -A arielfw-reject -j REJECT || exit 12" << "&&"
        // allow local connections (like local sockets)
        << "iptables -A INPUT -i lo -j ACCEPT || exit 13" << "&&"
        << "iptables -A OUTPUT -o lo -j ACCEPT || exit 14";

        //# Main rules (per interface)
        for(const std::string &protocol : ITFS_3G) {
                ss << "&&"
                   << "iptables -A arielfw -o " << protocol << " -j arielfw-3g || exit" << "&&"
                   << "iptables -A arielfw -i " << protocol << " -j arielfw-3g || exit";
        }

        for(const std::string &protocol : ITFS_WIFI) {
                ss << "&&"
                   << "iptables -A arielfw -o " << protocol << " -j arielfw-wifi || exit" << "&&"
                   << "iptables -A arielfw -i " << protocol << " -j arielfw-wifi || exit";
        }

        // enable processes dhcp and wifi
        //ss << "&&"
        //   << "iptables -A arielfw-wifi -m owner --uid-owner 1014 -j RETURN || exit"
        //   << "&&"
        //   << "iptables -A arielfw-wifi -m owner --uid-owner 1010 -j RETURN || exit";

        // execute commands
        int returnValue = system(ss.str().c_str());
        return returnValue;
}

int FirewallManager::indexOf(int array[], int length, int seek) {
        for (int i = 0; i < length; i++)
        {
                if (array[i] == seek) return i;
        }
        return -1;
}

int FirewallManager::disableWifi(int pid[], int pid_size) {

        // first we need to clear out existing rules
        //int initFw = prepareFirewall();


        std::stringstream ss;

        bool all_apps = indexOf(pid, pid_size, SPECIAL_UID_ANY) > -1;

        LOG(INFO) << "disableWifi all_apps : " << all_apps;
        if(all_apps) {
                LOG(INFO) << "I am disabling your wifi man.";
                /* block any application on this interface */
                ss << "iptables -A arielfw-wifi -j arielfw-reject || exit";
        } else {
                LOG(INFO) << "Disable wifi per application";
                /* release/block individual applications on this interface */
                for (int i = 0; i < pid_size; i++)
                {
                        int processId = pid[i];

                        if(processId >= 0) {
                                ss << "iptables -A arielfw-wifi -m owner --uid-owner " << processId
                                   << " -j arielfw-reject || exit";
                                if(i<pid_size-1) {
                                        ss << " && ";
                                }
                        }
                }
        }

        // execute commands
        int returnValue = system(ss.str().c_str());
        LOG(INFO) << "disableWifi all_apps error : " << errno << " and return value " << returnValue;
        return returnValue;

}

int FirewallManager::enableWifi(int pid[], int pid_size) {
        return 0;
}

int FirewallManager::disableData(int pid[], int pid_size) {
        std::stringstream ss;

        bool all_apps = indexOf(pid, pid_size, SPECIAL_UID_ANY) > -1;

        LOG(INFO) << "disableData all_apps : " << all_apps;
        if(all_apps) {
                LOG(INFO) << "I am disabling your data man.";
                /* block any application on this interface */
                ss << "iptables -A arielfw-3g -j arielfw-reject || exit";
        } else {
                LOG(INFO) << "Disable data per application";
                /* release/block individual applications on this interface */
                for (int i = 0; i < pid_size; i++)
                {
                        int processId = pid[i];

                        if(processId >= 0) {
                                ss << "iptables -A arielfw-3g -m owner --uid-owner " << processId
                                   << " -j arielfw-reject || exit";
                                if(i<pid_size-1) {
                                        ss << " && ";
                                }
                        }
                }
        }

        // execute commands
        int returnValue = system(ss.str().c_str());
        LOG(INFO) << "disableWifi all_apps error : " << errno << " and return value " << returnValue;
        return returnValue;

}

int FirewallManager::enableData(int pid[], int pid_size) {
        return 0;
}

int FirewallManager::disableNetworking(int pid[], int pid_size) {
        int initFw = prepareFirewall();

        if(initFw < 0) {
                // something is terribly wrong!
                return FW_ERROR;
        }
        else{
                int res = disableWifi(pid, pid_size);
                res = disableData(pid, pid_size);
                return res;
        }
        return 0;
}

int FirewallManager::clearRules() {
        std::stringstream ss;

        // purge arielfw rules
        ss << "iptables -F arielfw" << "&&"
           << "iptables -F arielfw-reject" << "&&"
           << "iptables -F arielfw-3g" << "&&"
           << "iptables -F arielfw-wifi";

        int returnValue = system(ss.str().c_str());
        return returnValue;
}
