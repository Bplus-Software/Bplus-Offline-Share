## Android1.0.12
BPLUS_VERSION_CODE: 33

- Redesigned Share to B+ Offline Share from other apps: file-summary card, filename/size rows and total for multiple files, text preview, paired-device profile avatars (initials when absent), name, device-type icon and fresh reachability state. Arabic/RTL and English/LTR use the app theme and Cairo.
- Tap a device to select only. Fixed Continue to <device> and Cancel footer; Continue opens named-target Send/Cancel confirmation. Existing per-file send confirmation remains. No send on selection, no multi-recipient or automatic send added.
- Platform type uses explicit saved platform metadata only, otherwise generic Device. Names/model strings are never guessed into a platform. Existing saved peers may therefore show generic Device.
- Each screen load/resume checks paired peers afresh on a bounded three-worker pool; only a successful ping with matching peer ID shows available. Checking and unreachable states are explicit, not cached online claims. Selected target is revalidated against current pairing before confirmation/send.
- Existing import limits, content-URI validation, file staging and cancellation cleanup remain. Received/saved files, existing chats and app identity are not migrated or deleted. Install over existing app, never uninstall/clear data.

Validation: clean SDK34 release build, package com.bplus.share version1.0.12/code33 and original signer.14 actual JVM picker-rule checks pass (explicit platform mapping, unknown names, identity reachability). Import bounds/source protections, updater integrity/URL/resume/hash, removal and automatic-send regressions pass. Source checks verify paired-only list, selection never sends, fixed footer and confirmation. Approved Arabic/English illustrative renders visually inspected; no device/emulator attached. Actual phone share-sheet layout/font scaling/navigation/URI grants/reachability and installation remain unverified.

Existing1.0.11 updater fallback/cache retained unchanged. Use test versions ON for prereleases; older installed apps blocked by API403 need the GitHub download to update in place.
