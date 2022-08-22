/*
**
** Copyright (C) 2018-2019 The LineageOS Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package arielos.security;

import arielos.security.IEscrowTokenStateChangeCallback;

/** {@hide} */
interface ISecurityInterface {
    void generateEscrowToken(in int userId, in byte[] token, IEscrowTokenStateChangeCallback listener);
    boolean hasPendingEscrowToken(in int userId);
    long setLockoutAttemptDeadline(in int userId, in int timeoutMs);
    boolean unlockUserWithToken(in long tokenHandle, in byte[] token, in int userId);
    boolean removeEscrowToken(in long handle, in int userId);
    boolean isEscrowTokenActive(in long handle, in int userId);
    boolean setLockCredentialWithToken(in byte[] credential, in int type, in long tokenHandle, in byte[] token, in int userId);
}
