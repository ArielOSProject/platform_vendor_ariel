/*
 * Copyright (C) 2008 The Android Open Source Project
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

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <android/log.h>
#include <errno.h>
#include <sys/stat.h>
#include <unistd.h>
#include <sys/inotify.h>
#include <poll.h>
#include "CommandListener.h"
#include <cutils/klog.h>
#include <cutils/properties.h>
#include <cutils/sockets.h>
#include <android-base/logging.h>
#include "FirewallManager.h"
//#include <android-base/stringprintf.h>

//#include <inotifytools/inotifytools.h>
//#include <inotifytools/inotify.h>

//using android::base::StringPrintf;

#include <private/android_filesystem_config.h>

#define APPNAME "ArielFW"

static void parse_args(int argc, char** argv);

int main(int argc, char** argv) {
    // only allow system user to run this command
    //uid_t current_uid = getuid();
    //if (current_uid != AID_ROOT && current_uid != AID_SHELL) error(1, 0, "not allowed");

    setenv("ANDROID_LOG_TAGS", "*:v", 1);
    android::base::InitLogging(argv, android::base::LogdLogger(android::base::SYSTEM));

    LOG(INFO) << "Arielfw (the awakening) firing up";

    FirewallManager *fwm = FirewallManager::Instance();

    /* Create our singleton managers */
    if (!(fwm = FirewallManager::Instance())) {
        LOG(ERROR) << "Unable to create FirewallManager";
        exit(1);
    }

    int prepareCode = fwm->prepareFirewall();
    LOG(INFO) << "Firewall prepared with status code: " << prepareCode;

    CommandListener *cl;

    parse_args(argc, argv);

    // Quickly throw a CLOEXEC on the socket we just inherited from init
    fcntl(android_get_control_socket("arielfw"), F_SETFD, FD_CLOEXEC);

    cl = new CommandListener();

    if (cl->startListener()) {
            PLOG(ERROR) << "Unable to start CommandListener";
            exit(1);
    }

    while(1) {
            sleep(1000);
    }

    LOG(ERROR) << "Arielfw exiting";

    exit(0);
}


static void parse_args(int argc, char** argv) {}
