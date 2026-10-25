# dnscrypt-proxy

dnscrypt-proxy binary used for filtering DNS queries.

https://github.com/DNSCrypt/dnscrypt-proxy

Current version: 2.1.20

Prebuilt binaries live in `bin/<abi>/dnscrypt-proxy` (`arm`, `arm64`, `x86`, `x86_64`),
taken from the upstream `dnscrypt-proxy-android_<abi>-<version>.zip` release assets
(upstream calls the 32-bit x86 build `android_i386`). `Android.bp` picks one per target arch.

