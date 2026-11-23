# B+ Share

Phone-to-phone file transfer for Android. Pair two phones with a QR code and send files of any type directly over local Wi-Fi. No accounts, no cloud.

> **Trial software, v1.0.** Built and packaged automatically; not yet tested on real devices. The first install on your phones is the real test - please report anything that misbehaves.

## Features (v1.0)
- Send files of any type between two Android phones over local Wi-Fi (direct, nothing goes online)
- Pair by scanning a QR code; pair with more than two devices
- Devices list with online status, per-device chat thread where messages and files travel together
- Groups: send one batch to every device in a group
- Auto-send: pick a folder and a target; the moment a new file appears it is sent automatically
- English (LTR) and Arabic (RTL) UI, Cairo font, dark/light theme
- In-app update check against GitHub releases
- Internet (relay) transfer mode: planned for the next version
- Small native app (~0.5 MB APK), Android 8+

## Install
1. Download **BplusShare-1.0.apk** from the latest release.
2. On your phone allow installing apps from your browser/files app when Android asks.
3. Open B+ Share on both phones (both on the same Wi-Fi), pick Send on one and Receive on the other, and scan the QR code.

## Build
No Gradle needed: `build.sh` compiles with aapt2 + ecj + d8 + apksigner against a local Android SDK (platform 34, build-tools 34.0.0) and JDK 17. Requires `ecj.jar` (Eclipse compiler) and `lambda-stub.jar` (included).

Developed for Basem Helmy (ابو عمر) - basem.sadoun@gmail.com
