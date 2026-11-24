# Release Notes

## v1.0.3 — Four-wheel Tire Temperature Support

- Added individual tire temperatures for the front-left, front-right, rear-left and rear-right wheels alongside existing tire-pressure readings.
- Added an independent °C / °F selector with saved preferences.
- Updated the vehicle-information tile to show all four corrected temperature values.
- Applied the provisional raw temperature offset (raw value − 60 °C); actual readings and wheel mapping should be verified on the vehicle.
- Preserved the original dashboard layout, themes and pressure warning logic.

### Important: TPMS firmware prerequisite

The app can display tire temperatures only when the vehicle's TPMS control module and head unit expose the corresponding EXLAP `tyreTemperatures` data. In the maintainer's configuration, this required flashing compatible TPMS module firmware following the community information below. Installing this APK alone does not unlock tire temperature data or flash any vehicle module.

**Reference / credit:** [Drive2 community article](https://www.drive2.ru/l/748710244984567276/). The firmware procedure is external to this project; verify hardware and firmware compatibility before attempting any module update. Unsupported or unavailable values are shown as dashes. The offset and per-wheel display have not yet been independently validated on all configurations.


## v1.0.2 — Dashboard and Interface Update

- Updated `app/src/main/assets/dashboard.html` to the V8.3 responsive interface.
- Unified the shared header and navigation across vehicle, music and settings pages.
- Added a collapsible music and TPMS side panel, with additional space for vehicle readings.
- Improved tire-pressure spacing, vehicle-value font resizing, and slider-based typography settings.
- Clarified collapse and expand button icons and labels.
- Retained existing EXLAP, CAN-facing app integrations, media controls and Safari Touchpad services.

**Release:** [v1.0.2 on GitHub](https://github.com/jjff2/JF-AI_Box-Bridge/releases/tag/v1.0.2). The maintainer reports successful testing on their device. Compatibility varies with the Android device and head-unit configuration.


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
