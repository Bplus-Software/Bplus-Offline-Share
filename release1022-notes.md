Android 1.0.22 beta, build 43

BPLUS_VERSION_CODE: 43
BplusShare-1.0.22.apk | 697307 bytes | SHA-256 c21424247c920903f4136ae869afe2ea996e141fde4a4c66c4eec6154f9bf45a

Developer mode (Settings), default OFF, persisted locally. OFF hides timing Copy details under successful files and transfers, plus routine discovery/updater copy controls. Actual failed-transfer error text opens an error dialog with Copy details; failed-delete summary also retains details in its error dialog. ON restores diagnostic controls, including timing data under files/transfers and discovery/updater controls. No extra permissions, network requests or automatic diagnostic upload. Existing diagnostic data is retained locally, not erased. Returning to chat refreshes the setting.

Root cause: timing metadata was attached to every completed file, and UI showed Copy details whenever metadata existed, including successful sends. Visibility now depends on Developer mode. Also checked transfer history and successful/already-gone delete summaries for the same clutter.

Verified: full resources/Java/D8/zipalign, APK v2/v3 signature, manifest com.bplus.share/43/1.0.22; DeveloperMode default-OFF and persisted toggle tests; source UI guards for chat/transfers/discovery/updater/delete. Off-device illustration checked for setting + OFF/ON states, not a runtime screenshot. No physical Android runtime verification. Earlier actual MediaStore/SAF-delete and PC-Android end-to-end limitations remain unchanged.

Same signer as1.0.18-1.0.21:3f212fa85d56b444fb6ea7df09d0646c795b04bb833b7389e1388a025bb3cc52. No uninstall/re-pair from those versions.1.0.17 or older still requires reinstall due earlier signer replacement and loses local app data.
