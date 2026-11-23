# B+ Share

Native Android app for direct phone-to-phone file sharing on a trusted local Wi-Fi network. Android 8 or later. English and Arabic, light and dark themes.

## Version 1.0.1

- Wi-Fi/hotspot setup guidance before QR pairing and file offers; cellular addresses are no longer used in QR codes.
- Pairing errors show the target address, this phone's address and the failure reason, with retry, Wi-Fi settings and token-free Copy details.
- GitHub updater: visible check/download/error states, percentage progress, bounded requests, up to three download attempts and resumable partial downloads. Redirects are supported. Final size, available GitHub digest, package identity, newer version and matching signing certificate are checked before installation.
- Android's installer remains in control. Unknown-app installation permission is requested when needed, with a return path.
- Pairing now checks the QR token. History is no longer copied automatically when pairing. An explicit confirmation on the paired screen is required. History copies include text and file records, not original file bytes.

## Install and test

Download the APK from the latest GitHub release and install it over the previous version. Version 1.0 only checks for releases, so the first upgrade to 1.0.1 must be installed manually. The new downloader applies to later updates.

Connect both phones to the same trusted Wi-Fi network, or try one phone's hotspot with the other joined. Keep both apps open. First try pairing and a small file. Guest Wi-Fi may prevent phones from communicating.

This is trial software. Android runtime, installer permission screens, hotspot support and real two-phone transfers have not been verified for 1.0.1. JVM fault-injection tests cover the updater network implementation, not the complete Android app. See `tests/results.txt` and `tests-updater.py`.

## Features and limits

QR pairing, multiple paired devices, per-device chat, file offers and groups are implemented. Watched-folder sending requires a chosen folder and target and is off by default. Background behavior depends on Android's restrictions and has not been tested. Internet relay mode is not implemented.

Local transfers use unencrypted HTTP with bearer tokens. Use trusted private networks only. Do not share pairing QR codes. Ordinary file transfer has no resume/checksum support and files of 2 GB or larger are not supported safely. See `PROTOCOL-v1.md` for the current wire format and limitations.

## Build

`build.sh` uses Android SDK platform/build-tools 34, JDK 17, aapt2, ecj and d8. Set up the paths in the script for your environment. Signing requires your own keystore and a signing password provided through `BPLUS_KEYSTORE_PASSWORD`. Do not commit signing keys, credentials or generated APKs.

Developed for Basem Helmy.
