## Android1.0.13, staged only
BPLUS_VERSION_CODE: 34

- Incoming acceptance locks discovery request buttons so the same screen cannot start a second cross-request while completing. Newly discovered rows inherit the lock. Further incoming prompts wait until the current handshake ends.
- On final /nearby/confirm response, receiver sends a local completion event to the visible Nearby screen in addition to its periodic state check. Saved+confirmed pairing shows Paired then returns to Chats after1.4s, same as outgoing.
- Acceptance gets its own60-second completion grace instead of only the remainder of the original request timeout. Reproduced old bug: accept at59s, next sender1.4s poll was already expired.
- Android pairing work is separate from queued discovery-profile loads. Final confirm retries up to3 times; same authenticated request can retrieve paired status/credentials and retry confirm. Existing request-secret/IP binding and explicit approval remain required; no token exposed before approval.
- Timeout/cancel/failure unlocks request controls with a clear message instead of leaving Completing pairing. No false Paired merely from tapping Accept. Both devices still need Nearby open until handshake completes; leaving closes the gate as before.

Validation: clean SDK34 signed build, original signer/package, version1.0.13/code34.22 actual JVM request-gate/endpoint assertions pass including fresh acceptance grace, late acceptance, repeat status/confirm and refusing parallel accepted requests. Existing updater, import, removal and auto-send regressions pass. Source verifies locked row buttons, direct completion-event plus polling, separate worker, bounded retries and error state.

Important: his phone screenshot confirms the old row-button inconsistency, but provides no endpoint logs proving why that particular request did not finish. Late acceptance is a reproduced failure, not a proven account of his exact attempt. No physical-device lifecycle/UI/radio/interoperability test was possible. Partner on an older release still must send final confirmation; this change never substitutes a fake success for missing trust. No installer/data reset required. Stage only until release approval.
