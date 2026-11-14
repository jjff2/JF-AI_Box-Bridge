# Permissions, Privacy, and Safety

JF AIBox Bridge is a community Android app for a trusted local network. **Installing it is not risk-free.** The main risks are network exposure, powerful Android permissions, unintended remote control, and driver distraction.

## Permission details

**Accessibility (optional, for Touchpad control):** The `JF Remote Touchpad` Accessibility Service can perform screen navigation and gestures. Android may warn that Accessibility services can observe or control the device. Only enable it on devices you trust, and disable it when not using remote control.

**Screen capture (optional, for mirroring):** Android asks you to approve screen capture through MediaProjection. Anything visible on the Android screen may be transmitted to a connected browser, including private messages or notifications. Stop mirroring before opening sensitive apps.

**Notification access (optional, for media metadata):** The app declares a notification-listener service for supported Android media information. Notification access is broader than music metadata alone; review Android's permission prompt and enable only if you need the feature.

**Internet / Wi-Fi / network state:** Required for EXLAP connectivity and the local HTTP dashboard/Touchpad services. Local HTTP traffic is **not end-to-end encrypted**. A trusted private network is essential.

**Foreground service:** Used to support background operation where the Android OS permits it.

**Audio settings:** Used for supported volume/media operations. Some vehicle head-unit functions may be unavailable on a standard Android phone.

**Legacy external storage permission:** The manifest requests `WRITE_EXTERNAL_STORAGE` only up to Android 9 (API 28). This is not a general modern Android storage permission.

## Local HTTP security

The application exposes local web endpoints, typically **8765 (dashboard)** and **18777 (Touchpad)**. The Touchpad implementation limits some requests to private-network clients, but **private IP access is not the same as authentication**. Anyone with suitable network reachability could potentially interact with exposed functions. The dashboard uses an app-generated token for some endpoints, but tokens must still be treated as sensitive.

- Use a private, trusted Wi-Fi network only.
- Do **not** expose these ports via router port forwarding, public IP, VPN exit nodes, or internet tunnels.
- Do not share QR codes or access-token URLs publicly.
- Disconnect or disable remote access when it is not needed.
- Avoid screen sharing when private information is displayed.

## Vehicle safety

This is an unofficial community project. **Do not operate the dashboard, Touchpad, or Android screen while driving.** Use only when safely parked. Vehicle data may be delayed, inaccurate, missing, or incompatible; do not rely on this app for safety-critical driving decisions. Never use it to override vehicle safety functions.

## Privacy and warranty

The project is designed around local-network communication; **this does not establish that every third-party library, Android device, or installed app sends no data externally**. Review source code and your device's network behavior before making privacy guarantees. Use may affect device stability or conflict with head-unit configurations. The author makes no warranty of compatibility, fitness, or vehicle safety.

## Responsible use

Only connect to a head unit or Android device that you own or have permission to operate. Do not publish credentials, tokens, vehicle identifiers, or personal data in GitHub issues or logs.
