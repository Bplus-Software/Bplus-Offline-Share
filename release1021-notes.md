Android 1.0.21 beta, build 42

BPLUS_VERSION_CODE: 42
BplusShare-1.0.21.apk | 697307 bytes | SHA-256 ebd9f0bf7ad7e432bb95b75c3cf2d2c4133e5d6596830617c753c60a518aece0

Android version-only release to keep Android and Windows version numbers aligned. No Android behavior change from1.0.20. All Android app source and resources unchanged except manifest versionName1.0.21/versionCode42. APK classes.dex byte-identical to1.0.20.

Same signer as1.0.18/1.0.19/1.0.20:3f212fa85d56b444fb6ea7df09d0646c795b04bb833b7389e1388a025bb3cc52. No uninstall or re-pair required from those versions.1.0.17 or older requires uninstall/reinstall due prior signer replacement, losing local app data.

Verified: full resource/Java/D8/zipalign build, APK v2/v3 signature, manifest version and package, exact source/resources diff restricted to manifest, unchanged dex. No new device testing.1.0.20 limitations remain: actual Android MediaStore/SAF delete/consent and PC-Android end-to-end not verified on a real device. See1.0.20 notes for role/local-delete semantics. This Android build does not add PC tray/toasts/Explorer features.
