# Troubleshooting

## The app opens, but vehicle data is blank

Check that the AI Box can reach the compatible head unit's Wi-Fi and EXLAP service. Run **Auto Detect MU1101 IP** or enter the correct head-unit IP. Vehicle data depends on firmware and EXLAP support; some fields may not be available.

## Safari cannot open the QR-code link

Confirm the iPhone/iPad and AI Box can communicate on the local network. The AI Box IP may change after reconnecting Wi-Fi. Try `http://AI_BOX_IP:18777/` for Touchpad. Client isolation, VPNs, firewalls, or hotspot routing may block access. Internet connectivity alone does not prove local reachability.

## Touchpad opens but taps or swipes do nothing

Enable the Android **JF Remote Touchpad** Accessibility Service and confirm the app is running. Some Android vendors restrict background services or accessibility gestures.

## Screen mirror shows a blank image

Start mirroring on the Android device and accept the Android screen-capture prompt. Confirm the Android version and AI Box support MediaProjection. The current release uses **JPEG frames**, not WebRTC/H.264 playback.

## Music metadata is missing

Check Android notification-listener access, media playback state, and whether the music app exposes a compatible media session. Vehicle EXLAP media features vary by firmware.

## Can I install it on an Android phone?

Possibly, provided the device meets the Android requirement. However, this project is designed for an AI Box connected to a compatible head unit. Phone-only operation, vehicle data, media integration, and background behavior are not guaranteed.

## Do I need a license code?

No JF software activation code or subscription is required. Android system permissions and any head-unit-specific connection requirements are separate.

## Is it safe to expose the web interface to the internet?

**No.** The HTTP interfaces are intended only for trusted local networks. Do not forward ports 8765 or 18777.
