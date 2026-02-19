# OMAPI Policy Hook

**Use at your own risk.**  
**Xposed/LSPosed is required.**

OMAPI Policy Hook modifies access control behavior in
`com.android.se.security.AccessControlEnforcer` to relax ARA/ARF checks and
enable full access in supported environments.

## Download

Get the latest APK from Releases:  
https://github.com/soralis0912/OMAPI-PolicyHook/releases

## How It Works

The module hooks
`com.android.se.security.AccessControlEnforcer.readSecurityProfile`,
disables `mUseArf` and `mUseAra`, and sets `mFullAccess`.

If changes do not take effect immediately, restart the SE service:

```bash
su -c killall com.android.se
```

Logcat tag: `OMAPI-PolicyHook`  
Reference:
[AccessControlEnforcer.java](https://cs.android.com/android/platform/superproject/main/+/main:packages/apps/SecureElement/src/com/android/se/security/AccessControlEnforcer.java;l=129)
