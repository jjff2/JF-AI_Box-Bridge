# Release Notes

## v1.0.0 — Initial Community Release

Initial source release of JF AIBox Bridge for compatible Android AI Boxes and VW Group MIB2.5 head units.

### Included

- Vehicle dashboard, supported EXLAP telemetry and available TPMS information.
- Supported Android media information and playback controls.
- iPhone/iPad Safari Touchpad, supported remote gestures and JPEG-based mirroring.
- Local Wi-Fi QR code entry points, app shortcuts and connection diagnostics.
- JF software activation and renewal screens removed.
- Published EXLAP reference authentication profiles retained by default and attributed in `EXLAP_ATTRIBUTION.md`.

### Important notes

- The maintainer has reported successful installation and use on their AI Box; broader device compatibility is not guaranteed.
- This source release is not itself a prebuilt APK. Publish a tested APK separately under GitHub Releases.
- Mirroring uses JPEG snapshots, not H.264/WebRTC 30 FPS streaming.
- Android Notification Access is needed for supported media metadata, Accessibility for remote gestures, and screen-capture approval for mirroring.
- Use only on trusted local networks and while safely parked.
