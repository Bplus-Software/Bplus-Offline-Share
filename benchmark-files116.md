# File transfer benchmark plan

No phone throughput result is claimed by this build.

Use a known 70 MB file stored locally on both platforms, not a cloud provider. Record exact byte count and SHA-256 externally to verify output. Keep topology/devices/power modes unchanged. Run baseline and candidate three times phone to PC and three times PC to phone. Then optionally compare same file on direct phone hotspot vs third-device hotspot/router.

Record preparation/copy start/end, connect, first byte, last byte, receiver final save, ACK, retries, failure status, and cancellation. Report total elapsed separately from stream bytes divided by first-to-last-byte elapsed. Median plus min/max, no speedup claim from a single run. Also record whether source is document provider and destination MediaStore/local disk.

Candidates in this build:128KiB buffered receiving writes, cheap in-memory pairing cancellation generation for manual sends instead of per64KiB peers JSON parsing; progress events at most5Hz.64KiB payload buffers and end-only flush retained. No compression, payload hashing, fsync per chunk or rate cap was found in baseline. Watched-folder guard still checks its existing opt-in/folder epoch.

Fault checks: network loss midstream, loss after receiver commit before ACK, failed409 active job, HTTP400/403 never retry, cancellation/backoff/off-toggle, unpair/delete, unreadable or resized source, process restart. Confirm identical job id on retry and exactly one received chat row/file in normal lost-ACK case. Crash between file completion and receipt persistence remains an orphan-file risk, not guaranteed exactly-once storage.

10 seconds for70MB requires7MB/s payload;5 seconds requires14MB/s. Radio/hotspot forwarding and storage may cap below that. Device measurements required. Planned telemetry gaps: receiver phase timing and Share-sheet staging progress remain incomplete in this candidate.
