## Android1.0.11
BPLUS_VERSION_CODE: 32

- On API HTTP403/429, test-version channel checks GitHub releases.atom as a backup. Stable channel never uses the unlabeled feed or silently offers beta releases.
- A usable fallback requires a same-repository version-tag link, unique Android build marker and exact APK filename/byte size/SHA-256 in release notes. Invalid entries fail closed. HTTPS download size/hash and exact package/version/code/installed-signer verification remain mandatory.
- Successful manual/automatic checks share a persistent five-minute, channel-separated cache. Rate-limit backoff respects server reset/Retry-After; no guessed reset hour is shown. Stable channel shows reset time when supplied, otherwise try-later. A releases-page shortcut is available. Failed checks are not presented as up-to-date.
- Existing1.0.10 or older cannot gain this fallback until1.0.11 is installed. If currently API-blocked, use this GitHub APK to update in place without uninstall/data clearing.

Validation: clean SDK34 build, original signer/package and version1.0.11/code32. Actual Java parser tested against live Atom fixture, stable exclusion, wrong repo, missing marker/digest and DTD/entity refusal. Actual HTTP fault injection verifies403->feed, cache, persistent backoff and no stable fallback. Existing updater integrity/URL/resume/hash tests and removal/auto-send regressions pass. Illustrative Arabic rate-limit/feed messages inspected. Real Android XML parser/UI/network/installer remain untested; no attached device. XML DTD/entity declarations explicitly rejected before parsing for Android-compatible fail-closed behavior. Android share-picker redesign is NOT included.
