# EXLAP Connection Setup

JF AIBox Bridge v1.0.0 includes the published reference EXLAP authentication profiles for vehicle telemetry and media connections. **Most users do not need to enter a username or password.** These are shared reference values, not a personal account or JF activation code.

1. Connect the Android AI Box to the supported head unit's Wi-Fi network.
2. Launch JF AIBox Bridge and select **Auto Detect MU1101 IP**, or set the head-unit IP manually.
3. Check the connection status and vehicle/media data. Available services depend on the head-unit firmware.

If you need to override the reference profile for your own authorized device, copy `local-exlap.properties.example` to `local-exlap.properties`, set the four EXLAP values, and rebuild. The override file is ignored by Git. Do not commit personal credentials.

The reference profiles are attributed in [EXLAP_ATTRIBUTION.md](EXLAP_ATTRIBUTION.md). They do not guarantee compatibility with all head units. Android screen-capture, notification and accessibility permissions are separate from EXLAP authentication.
