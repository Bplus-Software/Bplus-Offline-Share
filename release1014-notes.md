## Android1.0.14
BPLUS_VERSION_CODE: 35

Includes staged1.0.13 incoming-pairing fixes (completion event, acceptance grace, duplicate-request lock, confirm retries).

- Screen-scoped Nearby discovery waits for actual TCP47200 receiver readiness before advertising. Receiver startup error is visible instead of advertising an unavailable endpoint.
- Local interface/IPv4 snapshot is checked every3s, without remote polling. Changes restart NSD. Resolve requests serialized with up to3 attempts and6s callback timeout; profile loads get bounded retry. Approval polling starts independently of NSD errors.
- Explicit Refresh and Copy details. Diagnostics contain local interface/IP, selected IPv4, receiver/service/registration/resolve/profile states. No pairing/session secrets. Keep diagnostics private unless user chooses to share them.
- Explicit http/https URLs in private chat text are underlined and tappable, sent/received and existing/new messages. Opens external browser/app only on tap. Userinfo and executable/file/intent schemes refused. No automatic preview fetch. Long-press selection retained; links do nothing while selecting.

Validation: clean SDK34 original-signer build, com.bplus.share1.0.14/code35.22 pairing gate/endpoint JVM assertions passed;10 pure URL cases passed. Updater integrity and auto-send regressions passed. Source checks verify receiver gate, resolve serialization/retry/timeout, network restart, independent approval polling, Refresh and diagnostic actions. Arabic illustrative discovery/link renders visually inspected. Actual Android NSD callbacks, lifecycle, hotspot-host multicast, browser tap/long-press behavior, radio/interoperability and installation NOT tested on phone.

This does not prove the cause of Basem's exact discovery failure or fix multicast blocked by hotspot/router. No Android UDP broadcast/subnet fallback and no interface-selection change; conservative LAN-interface whitelist remains. No new background discoverability. Old phone app discovery code was unchanged1.0.10 through1.0.12. Windows gateway/direct-IP fallback is separately useful. 

### Already-paired photo refresh
- Authenticated /sync v2 hello requests and replies now include cleaned profile photo (raw base64 JPEG, <=40k characters/<=256px) and photoHash (SHA256 of the photo string). Ordinary text/status replies remain photo-free.
- Existing periodic hello refreshes both directions while apps are running. Explicit photo replaces saved avatar, explicit empty photo removes it, absent photo from an older peer preserves the old avatar. Hash verified when supplied. Wrong peer/profile ID refused. Email/phone remain excluded from sync/discovery/groups.
- Chat avatar and Devices/list views refresh after profile updates; no re-pair or chat migration. Older counterpart must add the same hello photo support to send its changed photo.
- Six actual hash/encoding cases and actual Store replacement/missing/empty/wrong-ID tests (bitmap/hash doubles) pass. Privacy checks remain green. Actual Android bitmap decode/network/Windows interoperability and UI refresh are not phone-tested.

1.0.14 is the combined batch after1.0.12;1.0.13 was staged only and is included. Preserve all older GitHub releases. User approval to fix/publish supplied6:26, publication owned by shared publisher.
