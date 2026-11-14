# JF AIBox Bridge

**Version 1.0.0 | Free Community Project | VW Group MIB2.5 | Android Phone / AI Box**

An Android-based vehicle information dashboard and remote-control solution for compatible Volkswagen Group MIB2.5 infotainment systems.

## About This Project

JF AIBox Bridge is an independent community project designed to display vehicle information through EXLAP using an Android phone or Android AI Box.

Many talented developers are currently working on modifying native MIB head unit interfaces. While those projects continue to evolve, JF AIBox Bridge offers an alternative way to access vehicle information without modifying the native head unit interface.

This project is not intended to replace native MIB modifications. It is simply another option for enthusiasts who want to explore vehicle data and additional features in the meantime.

**Current compatibility:** Developed for compatible VW Group MIB2.5 head units, including MU1101-based systems. Compatibility with other head units, vehicles, Android devices, and firmware versions has not been fully verified.

No separate JF activation code, subscription, or cloud account is required.

## Features

### Vehicle Information Dashboard
- Display supported real-time vehicle data through EXLAP
- Monitor available tire pressure (TPMS) information
- View supported vehicle status and telemetry
- Automatic head unit IP detection and connection diagnostics

### Media Dashboard
- Display available music information and playback status
- Show supported track details
- Control supported media playback
- Access media functions through the dashboard

### iPhone / iPad Safari Touchpad
- Remotely control the Android device using Safari
- Touchpad gestures and navigation controls
- Supported media commands
- Launch supported Android app shortcuts
- View Android screen snapshots through JPEG-based screen mirroring

### Additional Features
- Local Wi-Fi connectivity
- QR codes for accessing browser-based controls
- Connection and compatibility diagnostics
- Customizable dashboard settings
- No separate JF software activation

Feature availability depends on the vehicle, head unit firmware, Android permissions, network configuration, and installed applications.

## Download APK

**Latest release: v1.0.0**

[Download JF AIBox Bridge v1.0.0](https://github.com/jjff2/JF-AI_Box-Bridge/releases/tag/v1.0.0)

Open the release page and download `app.apk` from the Assets section.

### Installation

1. Download the APK from the official GitHub release.
2. Copy the APK to your Android phone or Android AI Box.
3. Allow installation from the selected source if Android requests permission.
4. Install and launch JF AIBox Bridge.
5. Connect your Android device to the compatible MIB2.5 head unit's Wi-Fi network.
6. Open the application and check the EXLAP connection status.

For detailed instructions, see [Installation and Wi-Fi Setup](INSTALLATION.md).

Only install APKs from sources you trust.

## First-Time Setup

### Connect to MIB2.5

1. Open Android Wi-Fi settings.
2. Connect to the compatible MIB2.5 head unit's Wi-Fi network.
3. Launch JF AIBox Bridge.
4. Select **Auto Detect MU1101 IP**, or manually enter the correct head unit IP address.
5. Check the EXLAP connection status.
6. Supported vehicle data should appear once the connection is established and the necessary services are available.

A general internet hotspot alone does not provide access to EXLAP vehicle data.

### iPhone / iPad Remote Touchpad

1. Connect your iPhone/iPad and Android device to a local network where they can communicate.
2. Open the Wi-Fi / Safari Bridge QR Code page in JF AIBox Bridge.
3. Scan the Touchpad QR code using your iPhone or iPad.
4. Open the link in Safari.
5. Enable the required Android Accessibility service for remote touch control.
6. Approve Android screen capture if you want to use screen mirroring.

The Touchpad typically uses port `18777`, while the local dashboard typically uses port `8765`.

These services are intended for trusted local networks only. Do not expose them to the public internet.

## Android Permissions

Different features require different Android permissions.

| Feature | Required Permission |
|---|---|
| Vehicle dashboard | Compatible Wi-Fi / EXLAP connection |
| Music information | Notification Access |
| Remote Touchpad | Accessibility Service |
| Screen mirroring | Android Screen Capture / MediaProjection |

You do not need to enable every permission to use the basic vehicle dashboard.

Accessibility can provide extensive control over the Android device. Screen mirroring may expose private information.

Please review [Permissions, Privacy and Safety](PERMISSIONS_AND_SAFETY.md) before enabling these features.

## Community Appreciation

A special thanks to everyone who has contributed to the research and development of native MIB interface modifications.

Many developers have generously shared their knowledge, research, tools, and public resources with the community.

These contributions have been extremely valuable and have helped make projects like JF AIBox Bridge possible.

I truly appreciate the effort everyone has put into exploring and improving the capabilities of Volkswagen Group infotainment systems.

This project is also my small contribution back to the community.

I hope it can be useful to other enthusiasts, and I welcome feedback, suggestions, and contributions.

## EXLAP Research and Attribution

JF AIBox Bridge uses publicly documented EXLAP reference authentication profiles and research from:

**[EXLAP MIB2 Object Atlas](https://github.com/ijord/EXLAP-mib2-atlas)**

The referenced project identifies its material under the Creative Commons Attribution 4.0 International (CC BY 4.0) license.

The published reference authentication profile values are included as application defaults, so users normally do not need to manually enter EXLAP credentials.

Special thanks to the original researchers and contributors for making this information publicly available.

Please see [EXLAP Attribution](EXLAP_ATTRIBUTION.md) for further details.

The use of this research does not imply official endorsement or guarantee compatibility with any particular vehicle.

## Safety and Privacy

- Use the application and remote controls only while safely parked.
- Do not interact with the dashboard while driving.
- Remote control permissions may allow connected local-network clients to operate the Android device.
- Screen mirroring may expose notifications, messages, passwords, and other private information.
- Local HTTP connections are not end-to-end encrypted.
- Never expose ports `8765` or `18777` directly to the internet.
- This application is not intended to serve as a safety-critical vehicle instrument.

Use the application at your own risk.

## Documentation

- [Installation and Wi-Fi Setup](INSTALLATION.md)
- [Android Permissions and Safety](PERMISSIONS_AND_SAFETY.md)
- [EXLAP Attribution](EXLAP_ATTRIBUTION.md)
- [EXLAP Setup](EXLAP_SETUP.md)
- [Troubleshooting](TROUBLESHOOTING.md)
- [Release Notes](RELEASE_NOTES.md)
- [GitHub Publishing Checklist](GITHUB_PUBLISHING.md)

## Source Code and Licensing

The source code is publicly available for community inspection, learning, and collaboration.

The repository includes original application code and references to third-party research and resources.

Public availability does not automatically grant permission to redistribute or relicense every component. A license for the original application code has not yet been specified.

The CC BY 4.0 attribution associated with the EXLAP MIB2 Object Atlas applies to the referenced research material and does not automatically apply to the entire JF AIBox Bridge application.

## Feedback and Contributions

Feedback, suggestions, testing results, and contributions are welcome!

If you encounter an issue, please open a GitHub Issue and include relevant information such as:

- Android device or AI Box model
- Android version
- MIB2.5 head unit model and firmware
- JF AIBox Bridge version
- Network configuration
- Description of the issue

Please remove passwords, personal information, private tokens, and vehicle identifiers from any shared logs or screenshots.

## Disclaimer

JF AIBox Bridge is an independent community project.

It is not affiliated with, sponsored by, or endorsed by Volkswagen, Audi, Škoda, SEAT, or any other vehicle manufacturer.

Compatibility and functionality may vary depending on hardware and software configurations.

---

**Developed and shared by JF VAG — Taiwan**

*Thanks to everyone contributing to the MIB development community. Keep exploring, sharing, and improving!*
