## Android1.0.10
BPLUS_VERSION_CODE: 31

- After accepting a nearby request, the phone shows Completing pairing and keeps the handshake open. Accept alone is not reported as success.
- Once final confirmation has stored the paired peer, show Paired with the device name, then automatically return to Chats/main after1.4 seconds. Outgoing phone-initiated pairing follows the same success navigation.
- Expired/cancelled requests stop waiting. Leaving Nearby still stops discovery/advertisement and clears outstanding requests. No always-on background discoverability.
- Existing chat/file data and package/signing identity preserved. Install over the existing app, never uninstall/clear data.

Validation: clean SDK34 build; original signer verified, com.bplus.share, version1.0.10/code31. Eight actual JVM completion-state assertions and17 nearby gate/endpoint assertions passed. Removal, exact-asset updater and auto-send regressions passed. Approved illustrative Arabic waiting/success renders inspected. Physical-phone pairing/navigation, radio discovery, UI measurements, cancellation latency and installer remain untested. This does not claim PC discovery timing improvements; Windows owns its retry changes.
