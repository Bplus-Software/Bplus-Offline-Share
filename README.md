# B+ Share

Native Android app for direct phone-to-phone file sharing on a trusted local Wi-Fi network. Android 8 or later. English and Arabic, light and dark themes.

## Version 1.0.3

- Failed text messages retain a concrete HTTP/network failure reason. Tap the failed message to review the reason, inspect connectivity or retry that exact message. Retry uses a stable local message ID rather than updating the last pending message.
- Header information button tests the saved peer address, shows local/target IP and result, and offers Wi-Fi settings and token-free Copy details. A failed ping does not establish the cause.
- Retrying after an uncertain delivery may duplicate a message, since wire protocol v1 has no acknowledgement IDs/deduplication. The app warns before retry. No automatic resend was added.
- File menu adds Share via another app using the Android system chooser and temporary read access. Choose WhatsApp/Messenger or another compatible installed app, then select and confirm the recipient there. Unavailable old files still require a local copy.
- File failures retain safe reasons and offer a confirmed Send file again action.
- JVM tests of the actual message network methods cover HTTP success, HTTP 403, refused connection, ping and a bounded response stall. The exact 1.0.2 updater downloaded the candidate 1.0.3 APK through a redirect with matching SHA-256. Android UI/installer/share-sheet operation remains untested.

## Version 1.0.2

- A Chats button on the main screen opens the paired-device conversation list.
- New sent/received file messages retain local content URIs. Image cards load sampled thumbnails; document cards show file type, name, size and state. Tap to open using Android; long-press for Open, Forward, Remove from chat or Locate file.
- Forward selects a paired device or group and asks for confirmation. Remove from chat removes only the local message, never the underlying file or another device's copy.
- Old/migrated file records do not contain the original files. Locate file lets the user explicitly associate a local copy; nothing is recovered automatically.
- Emoji insertion and quick replies fill the composer for editing before Send. No message reactions, launcher shortcuts or internet relay were added.
- Received files record their content URI and MIME type. Incomplete receives are discarded. Device-local file URIs are stripped from copied chat history.
- The exact 1.0.1 downloader passed a redirected download of the candidate 1.0.2 APK with matching SHA-256 in a JVM test. Android installer, permission prompts and file viewing remain untested on-device.

## Version 1.0.1

- Wi-Fi/hotspot setup guidance before QR pairing and file offers; cellular addresses are no longer used in QR codes.
- Pairing errors show the target address, this phone's address and the failure reason, with retry, Wi-Fi settings and token-free Copy details.
- GitHub updater: visible check/download/error states, percentage progress, bounded requests, up to three download attempts and resumable partial downloads. Redirects are supported. Final size, available GitHub digest, package identity, newer version and matching signing certificate are checked before installation.
- Android's installer remains in control. Unknown-app installation permission is requested when needed, with a return path.
- Pairing now checks the QR token. History is no longer copied automatically when pairing. An explicit confirmation on the paired screen is required. History copies include text and file records, not original file bytes.

## Install and test

Download the APK from the latest GitHub release and install it over the previous version. Version 1.0 only checks for releases, so the first upgrade to 1.0.1 must be installed manually. The new downloader applies to later updates.

Connect both phones to the same trusted Wi-Fi network, or try one phone's hotspot with the other joined. Keep both apps open. First try pairing and a small file. Guest Wi-Fi may prevent phones from communicating.

This is trial software. Android runtime, installer permission screens, hotspot support and real two-phone transfers have not been verified for 1.0.2. JVM fault-injection tests cover the updater network implementation, not the complete Android app. See `tests/results.txt` and `tests-updater.py`.

## Features and limits

QR pairing, multiple paired devices, per-device chat, file offers and groups are implemented. Watched-folder sending requires a chosen folder and target and is off by default. Background behavior depends on Android's restrictions and has not been tested. Internet relay mode is not implemented.

Local transfers use unencrypted HTTP with bearer tokens. Use trusted private networks only. Do not share pairing QR codes. Ordinary file transfer has no resume/checksum support and files of 2 GB or larger are not supported safely. See `PROTOCOL-v1.md` for the current wire format and limitations.

## Build

`build.sh` uses Android SDK platform/build-tools 34, JDK 17, aapt2, ecj and d8. Set up the paths in the script for your environment. Signing requires your own keystore and a signing password provided through `BPLUS_KEYSTORE_PASSWORD`. Do not commit signing keys, credentials or generated APKs.

Developed for Basem Helmy.
