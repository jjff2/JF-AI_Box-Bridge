# JF AIBox Bridge

**Version 1.0.0 · Free community release · Android AI Box / compatible VW Group infotainment**

JF AIBox Bridge is an Android dashboard and local-network companion app for compatible Volkswagen Group MIB2.5 head units, including tested MU1101-based setups. It displays supported vehicle data and media information, and provides an iPhone/iPad Safari Touchpad for remotely controlling the Android device.

**No JF activation code, subscription, or cloud account is required.** The app is an independent community project, not affiliated with or endorsed by Volkswagen, Audi, Škoda, or SEAT.

> **Status:** The maintainer reports a successful installation and basic operation on their own AI Box. Compatibility across other Android devices, firmware versions, and vehicles has not been established. The repository contains source code; an APK should be downloaded from **Releases** only after the maintainer uploads a tested build.

## Features

- **Vehicle dashboard:** Live supported EXLAP telemetry, subject to head-unit compatibility.
- **Tire pressure (TPMS):** View available tire-pressure values and status where the vehicle exposes them.
- **Media dashboard:** Display supported media-session information, track details and playback controls.
- **Safari Touchpad:** Use an iPhone or iPad browser to send supported taps, swipes, navigation and media commands to the Android AI Box.
- **Screen mirroring:** View the Android screen in Safari using JPEG-based snapshots, after approving Android screen capture. **Not 20–30 FPS H.264/WebRTC video.**
- **App shortcuts:** Launch or manage supported Android app shortcuts.
- **Local QR codes:** Open browser controls using QR codes shown inside the app.
- **Connection diagnostics and settings:** Check local connectivity, EXLAP status, and relevant services.
- **No JF license screen:** Install and launch without a separate JF software activation process.

Availability of individual functions depends on Android permissions, head-unit software, network routing and installed applications.

## Install the app

**For regular users:** Download an APK from the repository's **Releases** page when one is available, copy it to your Android AI Box, allow installation from that source if Android asks, install, and launch **JF AI Box Bridge**. Only install APKs published by a source you trust.

**For developers:** Build the APK from this source using Android Studio, JDK 17, Android SDK 35, and a compatible Gradle 8.9 setup. This source archive does not bundle the Gradle Wrapper. See [Installation Guide](INSTALLATION.md).

## First-time setup: Android permissions

You do **not** need to grant every permission for basic vehicle dashboard use. Enable the following only for features you want:

| Feature | Android setting / prompt | What to do |
|---|---|---|
| Android music metadata | **Settings → Notification access** (may appear under Special app access) | Enable access for **JF AI Box Bridge** so the app can read supported active media sessions. This permission can expose notification content beyond music. |
| iPhone/iPad remote taps and swipes | **Settings → Accessibility → JF Remote Touchpad** | Enable the service and acknowledge Android's warning. This grants powerful control of the Android device. |
| Screen mirroring to Safari | Android **Start recording or casting?** / screen-capture dialog | Start mirroring in the app, then approve the Android prompt. Approval may be required again after a restart. |
| Local Wi-Fi / EXLAP | Android Wi-Fi settings | Connect the AI Box to the compatible head-unit network; use the app's connection diagnostics. |

The exact settings names vary by Android version and AI Box manufacturer. For more detail, see [Permissions and Safety](PERMISSIONS_AND_SAFETY.md).

## Wi-Fi setup: two different connections

**A. Vehicle data (EXLAP)**

1. On the Android AI Box, open **Android Settings → Wi-Fi**.
2. Join the compatible **car head unit's Wi-Fi network** (for example, the Wi-Fi broadcast by a supported MU1101 installation). A general internet hotspot alone will not provide vehicle EXLAP data.
3. Open JF AIBox Bridge and choose **Auto Detect MU1101 IP**. If necessary, enter the correct head-unit IP manually.
4. Check the EXLAP/vehicle connection status. Supported data will appear if the head unit exposes the necessary services.

**B. iPhone/iPad Safari Touchpad**

1. Connect the iPhone/iPad and the Android AI Box to a **local network where the devices can reach one another**. This may be the car Wi-Fi, a shared router, or a compatible hotspot arrangement. Internet access by itself is not sufficient.
2. On the AI Box, open the app's **Wi-Fi / Safari Bridge QR Code** screen.
3. Scan the **Touchpad** QR code with iPhone/iPad and open it in **Safari**. Alternatively, use `http://AI_BOX_IP:18777/` with the actual IP displayed by the app.
4. Enable the Android Accessibility service to allow taps and swipes. Start mirroring and approve the screen-capture prompt if you want a live view.

The dashboard's local HTTP service typically uses port **8765**, and the Touchpad typically uses **18777**. Some routers isolate Wi-Fi clients. **Never forward these ports to the internet or share private QR access links publicly.**

## EXLAP attribution and authentication

JF AIBox Bridge uses published EXLAP reference authentication profiles and research documented by the **[EXLAP MIB2 Object Atlas](https://github.com/ijord/EXLAP-mib2-atlas)**. The source project identifies its material as **CC BY 4.0**; attribution and a link to the license are provided in [EXLAP_ATTRIBUTION.md](EXLAP_ATTRIBUTION.md). This is a reference to third-party research, **not an official Volkswagen endorsement or a guarantee of compatibility**.

The published reference profile values are included as application defaults, so users normally **do not enter EXLAP usernames or passwords**. EXLAP authentication is distinct from JF software licensing; JF activation has been removed. Optional developer overrides are described in [EXLAP Setup](EXLAP_SETUP.md).

## Safety and privacy

- **Use only while safely parked.** Do not operate the Touchpad or look at the dashboard while driving.
- Remote control uses Android Accessibility and may allow connected local-network clients to control the Android device.
- Screen mirroring may show messages, notifications, passwords, and other private information.
- Local HTTP connections are not end-to-end encrypted; use a trusted private Wi-Fi network.
- Do not expose ports **8765** or **18777** on the public internet.
- This app is not a safety-critical vehicle instrument and is provided without a compatibility or performance guarantee.

See [Permissions, Privacy and Safety](PERMISSIONS_AND_SAFETY.md).

## Documentation

- [Installation and Wi-Fi Setup](INSTALLATION.md)
- [Android Permissions and Safety](PERMISSIONS_AND_SAFETY.md)
- [EXLAP Attribution](EXLAP_ATTRIBUTION.md)
- [EXLAP Setup](EXLAP_SETUP.md)
- [Troubleshooting](TROUBLESHOOTING.md)
- [Release Notes](RELEASE_NOTES.md)
- [GitHub Publishing Checklist](GITHUB_PUBLISHING.md)

## Source licensing

This repository contains original application code as well as material referencing third-party EXLAP research and other possible third-party assets. **A public repository does not automatically grant permission to redistribute or relicense every component.** The maintainer should choose a license for the original code after checking ownership and third-party obligations. The CC BY 4.0 attribution above applies to the referenced EXLAP Atlas material, not automatically to the entire application.

## Feedback

Please open a GitHub Issue with the AI Box model, Android version, head-unit model, app version, Wi-Fi topology and reproduction steps. Remove passwords, private tokens, personal information and vehicle identifiers from logs or screenshots before sharing.
