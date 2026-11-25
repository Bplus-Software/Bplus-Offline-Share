Android 1.0.18, build 39 (beta)

IMPORTANT: new signing certificate. This APK CANNOT update installed 1.0.17 or older. Uninstall the old Android app before installing this APK. Uninstalling removes local app data, including paired devices and chat history. Re-pair after installing. No automatic migration was tested or promised.

- Updater accepts exact Bplus-Software/Bplus-Offline-Share and the two legacy repository identities.
- Org-first API/feed with 404-only legacy fallback, usable before and after transfer.
- Atom preserves approved source repository; exact asset, hash, package, version and signer checks retained.
- English and Arabic updater repository labels point to Bplus Software.
- App package and Android storage folder names unchanged. All 1.0.17 features retained.

BPLUS_VERSION_CODE: 39
BplusShare-1.0.18.apk | 680923 bytes | SHA-256 0e1cf8263617e9afa4417d597b6d502e1e1d4ab6a86c1d9e1b423b1774a38cce
New signer certificate SHA-256: 3f212fa85d56b444fb6ea7df09d0646c795b04bb833b7389e1388a025bb3cc52

Verified: full resource/Java/D8 build; apksigner v2/v3; package com.bplus.share/versionCode39/versionName1.0.18; 36 updater assertions; synthetic API301/APK301 and 404-vs403 behavior; live pretransfer fallback; vault keystore backup byte-for-byte roundtrip.
Not verified: physical Android install, device discovery/transfers, post-transfer live org releases, visual rendering of updated labels.
