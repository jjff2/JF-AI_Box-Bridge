# Installation and Wi-Fi Setup

## 1. Supported devices

This app is intended primarily for an **Android AI Box** connected to a compatible VW Group infotainment system. An Android phone may run the app, but vehicle telemetry, external display behavior, and remote control are **not guaranteed**. Requires **Android 8.0 (API 26) or later** according to the project configuration.

## 2. Build and install

There is **no prebuilt APK in this source release**.

1. Download the repository source and extract it.
2. Install Android Studio, JDK 17, and Android SDK 35.
3. Open the extracted project folder in Android Studio.
4. Configure a compatible Gradle 8.9 installation if prompted; the Gradle Wrapper is not bundled.
5. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)** for a debug build.
6. Transfer the generated APK to the Android AI Box or Android phone, then install it. Android may ask you to allow installation from that file manager or browser.
7. Launch **JF AI Box Bridge**. **No JF activation code, subscription, or renewal is required.**

The maintainer reports successful installation and operation on one AI Box. Builds on other machines and compatibility with other devices have not been independently verified. Android Studio menus may differ by version.

## 3. Connect the Android AI Box to the vehicle

For EXLAP vehicle data, the Android AI Box must be able to reach the **head unit's EXLAP service**. In supported MU1101 installations, this commonly means connecting the AI Box to the **vehicle head unit's Wi-Fi network**, not simply any internet hotspot.

1. Open Android Wi-Fi settings on the AI Box.
2. Select the Wi-Fi network broadcast by the compatible head unit, if available.
3. Return to JF AIBox Bridge and open connection settings.
4. Select **Auto Detect MU1101 IP**. If discovery fails, enter the correct head-unit IP address manually.
5. Check the app's connection/diagnostics screen. Some firmware configurations do not expose the required EXLAP services.

**EXLAP note:** End users normally do not need to type a JF license key. However, this source project has optional internal EXLAP configuration values, and compatibility/authentication cannot be guaranteed for every head unit.

## 4. Connect an iPhone or iPad to Safari Touchpad

The **iPhone/iPad and Android AI Box must be able to reach each other over the same local network or another directly routed private network**. Examples:

- **Head-unit Wi-Fi:** Both devices join the vehicle head unit's Wi-Fi, if it permits client-to-client communication.
- **Home/travel router:** Both devices join the same router; the AI Box must still be able to reach EXLAP separately if vehicle telemetry is needed.
- **Phone hotspot:** The AI Box joins a compatible phone hotspot; the Safari device must have a working local route to the AI Box. Hotspot client isolation and Apple hotspot behavior vary.

On the AI Box, open **Wi-Fi / Safari Bridge QR Code** and scan the **Touchpad QR** using Safari. Alternatively enter `http://AI_BOX_IP:18777/` in Safari, replacing `AI_BOX_IP` with the actual local IP shown by the app. The dashboard typically uses `http://AI_BOX_IP:8765/` with the app-generated access token/QR link; do not invent a token.

**Do not forward ports 8765 or 18777 on a router. Do not use these services on public Wi-Fi.**

## 5. Enable optional features

- **Remote taps/swipes:** On Android, open **Settings > Accessibility** and enable **JF Remote Touchpad** after reviewing the system warning.
- **Screen mirroring:** In the app, start mirroring and approve the Android screen-capture confirmation. This permission may need to be granted again after restarting.
- **Android media information:** If prompted, open Android **Notification Access** settings and allow the app to read media notifications/session data. This access may also expose notification content; enable only if desired.

## 6. First-use checklist

- App opens without a JF activation prompt.
- Android AI Box is connected to the intended Wi-Fi network.
- Head-unit IP is detected or entered, if using EXLAP.
- Dashboard connection status is visible.
- iPhone/iPad Safari opens the Touchpad page via the QR code.
- Remote gestures work after Accessibility is enabled.
- Mirroring works only after screen-capture approval.

For connection issues, see [Troubleshooting](TROUBLESHOOTING.md).
