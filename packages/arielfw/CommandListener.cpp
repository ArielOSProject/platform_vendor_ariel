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

#include <stdlib.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <stdint.h>
#include <inttypes.h>
#include <ctype.h>
#include <cutils/klog.h>
#include <android-base/logging.h>
#include <sstream>

#define LOG_TAG "ArielfwCmdListener"

#include <cutils/fs.h>

#include <sysutils/SocketClient.h>
#include <private/android_filesystem_config.h>

#include "CommandListener.h"
#include "ResponseCode.h"

#include "FirewallManager.h"

#define DUMP_ARGS 0
#define DEBUG_APPFUSE 0

CommandListener::CommandListener() :
                 FrameworkListener("arielfw", true) {
    setenv("ANDROID_LOG_TAGS", "*:v", 1);
    android::base::InitLogging(NULL, android::base::LogdLogger(android::base::SYSTEM));

    registerCmd(new StatusCmd());
    registerCmd(new DisableWifiCmd());
    registerCmd(new DisableNetworkingCmd());
    registerCmd(new ClearRulesCmd());
}

#if DUMP_ARGS
void CommandListener::dumpArgs(int argc, char **argv, int argObscure) {
    char buffer[4096];
    char *p = buffer;

    memset(buffer, 0, sizeof(buffer));
    int i;
    for (i = 0; i < argc; i++) {
        unsigned int len = strlen(argv[i]) + 1; // Account for space
        if (i == argObscure) {
            len += 2; // Account for {}
        }
        if (((p - buffer) + len) < (sizeof(buffer)-1)) {
            if (i == argObscure) {
                *p++ = '{';
                *p++ = '}';
                *p++ = ' ';
                continue;
            }
            strcpy(p, argv[i]);
            p+= strlen(argv[i]);
            if (i != (argc -1)) {
                *p++ = ' ';
            }
        }
    }
    SLOGD("%s", buffer);
    LOG(INFO) << "dumpArgs : " << buffer;
}
#else
void CommandListener::dumpArgs(int /*argc*/, char ** /*argv*/, int /*argObscure*/) { }
#endif

int CommandListener::sendGenericOkFail(SocketClient *cli, int cond) {
    if (!cond) {
        return cli->sendMsg(ResponseCode::CommandOkay, "Command succeeded", false);
    } else {
        return cli->sendMsg(ResponseCode::OperationFailed, "Command failed", false);
    }
}

CommandListener::StatusCmd::StatusCmd() :
                 ArielfwCommand("status") {
}

int CommandListener::StatusCmd::runCommand(SocketClient *cli,
                                         int argc, char ** argv) {
    int ret = system("iptables --version");
    cli->sendMsg(ret, "iptables success", false);
    return 0;
}

CommandListener::DisableWifiCmd::DisableWifiCmd() :
                 ArielfwCommand("disable_wifi") {
}

int CommandListener::DisableWifiCmd::runCommand(SocketClient *cli,
                                         int argc, char ** argv) {

    dumpArgs(argc, argv, -1);

    LOG(INFO) << "Running command disable_wifi";
    LOG(INFO) << "Number of arguments: " << argc;

    if(argc > 1){
        std::string wifi_uids(argv[1]);

        int numElements = std::count( wifi_uids.begin(), wifi_uids.end(), ' ' );

        int wifiuids[numElements+1];

        int i = 0;
        std::stringstream ssin(wifi_uids);
        while (ssin.good() && i < numElements+1){
            ssin >> wifiuids[i];
            ++i;
        }
        for(i = 0; i < numElements+1; i++){
            LOG(INFO) << "Element " << i << " : " << wifiuids[i];
        }

        FirewallManager *fwm = FirewallManager::Instance();
        int result_code = fwm->disableWifi(wifiuids, numElements+1);

        cli->sendMsg(0, "disabled_wifi completed", false);

    }
    else{
        cli->sendMsg(1, "disabled_wifi missing arguments", false);
    }

    return 0;
}

CommandListener::DisableNetworkingCmd::DisableNetworkingCmd() :
                 ArielfwCommand("disable_networking") {
}

int CommandListener::DisableNetworkingCmd::runCommand(SocketClient *cli,
                                         int argc, char ** argv) {

    dumpArgs(argc, argv, -1);

    LOG(INFO) << "Running command disable_networking";
    LOG(INFO) << "Number of arguments: " << argc;

    if(argc > 1){
        std::string uids_string(argv[1]);

        int numElements = std::count( uids_string.begin(), uids_string.end(), ' ' );

        int uids[numElements+1];

        int i = 0;
        std::stringstream ssin(uids_string);
        while (ssin.good() && i < numElements+1){
            ssin >> uids[i];
            ++i;
        }
        for(i = 0; i < numElements+1; i++){
            LOG(INFO) << "Element " << i << " : " << uids[i];
        }

        FirewallManager *fwm = FirewallManager::Instance();
        int result_code = fwm->disableNetworking(uids, numElements+1);

        cli->sendMsg(result_code, "disabled_networking completed", false);

    }
    else{
        cli->sendMsg(1, "disabled_networking missing arguments", false);
    }

    return 0;
}

CommandListener::ClearRulesCmd::ClearRulesCmd() :
                 ArielfwCommand("clear_rules") {
}

int CommandListener::ClearRulesCmd::runCommand(SocketClient *cli,
                                         int argc, char ** argv) {
    FirewallManager *fwm = FirewallManager::Instance();
    int result_code = fwm->clearRules();

    cli->sendMsg(result_code, "clear_rules completed", false);

    return 0;
}