# B+ Offline Share

Official Android release: 1.0.0. Candidate1.0.1 is a pre-release with durable private text queue, profile sync, remove-device and delivery/read status. See release101-notes.txt and PROTOCOL-v2-SYNC.md for compatibility and test limits. Android versionCode23. Package com.bplus.share, original signing certificate.

Direct sharing on the same trusted Wi-Fi network: QR/nearby pairing, chats, files, voice notes and Android share targets. Android8 or later, Arabic and English. Existing Downloads/B+ Share storage path retained for compatibility. No Firebase/internet transport in this release. Local HTTP is not encrypted, use trusted private networks only.

## Install

Use the APK on the current GitHub release. Install OVER the existing app, never uninstall or clear app data to perform this version reset. The display version changes from1.1.17 to1.0.0, but Android versionCode increases from22 to23 with the same package and signer. The old updater cannot discover a lower display version, so this reset needs a manual APK install.

## Future updates

Increase Android versionCode and add an exact `BPLUS_VERSION_CODE: N` line to each release body. The new updater uses this build code so display-version resets do not break future updates. It still validates package, increasing APK build code and the installed signing certificate. Semantic-version comparison is a fallback for legacy release metadata.

## Validation and limits

Approved1.1.17 UI/state, unverified voice-positioning experiments excluded. Storage/chat init and transfer code untouched. Signed SDK34 build, package/signer metadata, build-code selection and updater fault-injection checks pass. Actual Android installation, retention of chats/settings and runtime UI of this build are not independently tested. No promise of data backup or guaranteed upgrade recovery. Both devices must run compatible versions and remain reachable. Windows is a separate app.

See release100-notes.txt and tests-reset100.py. Prior source history remains in git; obsolete release entries, APK assets and tags are removed as requested.
