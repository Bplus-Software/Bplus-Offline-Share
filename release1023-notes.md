Android1.0.23 beta,build44

BPLUS_VERSION_CODE: 44
BplusShare-1.0.23.apk | 697307 bytes | SHA-256 0731e9fb567d55a6cd04d6159db6a7af1754fa6a542d504aebd9dc16cbcafe3e

Direct-chat recall interoperability fix is coordinated with Windows1.0.23. Windows1.0.22 lacked /recall and did not save wire/key receipts; Android received404 but hid it behind generic failure. Windows1.0.23 adds tracked receipts and /recall. Android already sends wire/key/senderToken and accepts exact status=deleted ACK. This release preserves specific HTTP/status/transport failure details instead of swallowing them, with Copy details inside failed-operation dialog. Wording now both devices in English/Arabic, including related cross-device copy.

unknown/file_refused remain unconfirmed, keep original sender row/capability and allow retry. Only deleted removes sender row. Unknown-receipt and unsupported404 messages explain update/resend rather than guessing files by filename. IMPORTANT: files sent to Windows1.0.22 or older have no receipt there and cannot be retroactively safely recalled. Resend after both apps update, or delete the old copy locally on the receiving PC.

Verified Android receiver11 auth/receipt/idempotence assertions and actual loopback HTTP4 tests using exact extracted RecallAction.call implementation: wire request, deleted/unknown/file_refused ACK,403 diagnostic. Full resource/Java/D8/zipalign/signature/manifest checks. These are NOT physical Android-Windows end-to-end tests. Windows counterpart tests are reported by Windows maintainer separately. Android MediaStore refusal remains possible and returns file_refused honestly. Off-device illustration, not runtime screenshot.

Same signer as1.0.18 onward:3f212fa85d56b444fb6ea7df09d0646c795b04bb833b7389e1388a025bb3cc52. com.bplus.share/versionCode44/versionName1.0.23. No uninstall/re-pair from1.0.18 onward. Developer mode remains OFF by default; failure details allowed inside errors.
