# B+ Share wire protocol v1

Status: the format used by Android 1.0 and 1.0.1. Compatibility is described from source, not a cross-platform runtime test. App version and protocol version are independent. Protocol changes must add a new `v` value rather than silently reinterpret v1 fields.

## Transport

Local IPv4 HTTP, TCP port 47200. No TLS or end-to-end encryption. Use only a trusted private network. Network routing and device isolation affect reachability. QR codes contain a bearer token, so do not share them or include tokens in logs or diagnostics.

Requests use HTTP/1.1 with an explicit Content-Length and Connection: close. Chunked request bodies are not supported. JSON is UTF-8. Binary bodies carry raw file bytes. The current receiver parses Content-Length as a signed 32-bit integer, so transfers of 2 GB or larger are not supported safely. No transfer resume or content checksum exists for ordinary file transfers in v1. The GitHub APK updater has a separate resumable downloader and is not this protocol.

## QR payload

Fields: `v` (1), `app` (`bplus-share`), `id` (device ID), `name` (device name), `ip` (local IPv4), `port` (47200), `token` (local bearer token). A file offer also has `batch`, an ephemeral batch ID that exists only while the sender process holds it.

## Endpoints

- `GET /ping?token=...`: returns JSON with `id` and `name`. v1 does not authenticate this status endpoint.
- `POST /pair?token=...`: JSON request with `id`, `name`, `token` belonging to the initiating device. Response contains the receiving device's `id` and `name`. The receiver learns the initiator IP from the socket. Android 1.0.1 checks the URL token against the QR-displaying device's token; 1.0 did not.
- `GET /manifest?token=...&batch=...`: requires the offering device's token. Response has `name` and `files`; each file has `i`, `name`, `size`.
- `GET /file?token=...&batch=...&i=...`: requires the offering device's token. Returns an octet stream with Content-Length.
- `POST /recv?token=...&name=...&from=...`: raw file bytes. `name` must be URL-encoded; `from` is sender device ID. Requires a recognized peer/local token. Receiver writes to Downloads/B+ Share. There is no transfer ID or idempotency key.
- `POST /msg?token=...`: UTF-8 JSON with `text` and `from`. Requires a recognized peer/local token. Response acknowledges receipt. No message ID or delivery deduplication exists.
- `GET /batchdone?token=...&batch=...`: acknowledges a pulled batch. Android 1.0.1 requires the offering device's token.
- `GET /history?token=...`: recognized peer/local token required. Response has `deviceName` and `threads`, mapping peer IDs to message arrays. Contains chat text and file records, not the original file bytes. Android 1.0.1 invokes it only after an explicit on-device confirmation; never automatically during pairing.

Peer/local token means a token matching the receiver's local token or one in its paired-device records. v1 does not bind each request's claimed `from` field to one token identity. Pairing tokens persist until application data is reset. This is not a hardened protocol for untrusted networks.

## Known limitations

No mutual identity confirmation, token rotation, encrypted transport, authenticated sender binding, mDNS discovery, transfer resume, or file-level integrity check. These should be designed as a protocol v2 before broad untrusted-network use. Chat/file history copies skip threads that already exist locally rather than merge them.

## 1.1.0 nearby extension

POST JSON endpoints under `/nearby/`, maximum 48,000-byte request body. Visible only while NearbyActivity is resumed. `/profile` gives a bounded display profile without tokens; `/request` accepts a cleaned profile and a random 32-character request secret, returning a request ID. `/status` requires request ID, secret and originating IP. Pending/declined/blocked/cancelled responses contain no token. Accepted status includes the receiving phone profile and token. `/confirm` accepts the sender token only for an accepted request and stores the peer. `/cancel` cancels pending or accepted requests before confirmation. Both screens must remain open to finish. Closing clears outstanding requests. Expiry 60 seconds, cooldown 30 seconds per IP, 32-request ceiling. Blocks are stored by self-claimed device ID, not cryptographic identity. This is not an encrypted or authenticated identity protocol. QR remains bearer-token pairing.

New received files use typed subfolders. No old files are moved. Received file URIs remain local and are omitted from explicit history copy.

## 1.1.1 local host groups

Separate liveGroups records, not legacy recipient lists. A paired host sends /group/invite with its known pairing token, group ID and unique per-member group key. Recipient explicitly calls /group/join; invitation alone enables no messages. /group/message accepts joined member key + IP and assigns sender from host membership, not supplied sender text. Host queues /group/deliver with each member's own key; recipient verifies host IP/key and joined state. /group/file and /group/file-deliver stream local files with the same gates and group-scoped message IDs. Duplicate file requests drain their bodies without resaving. All routes require POST. Text JSON cap 20KB, text 8000 chars, files <2GB. No cloud relay, automatic retry, offline history, host election, roster sync, removal/leave or encryption. A host ack is not proof of delivery to every member. Group secrets and IP binding are not cryptographic identity. Network changes are not automatically repaired.
