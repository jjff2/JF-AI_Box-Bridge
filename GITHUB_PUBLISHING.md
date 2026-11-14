# Publish JF AIBox Bridge on GitHub (Maintainer Guide)

## Step 1 — Create the repository

1. Sign in to https://github.com/.
2. Open https://github.com/new.
3. **Repository name:** `JF-AIBox-Bridge`
4. **Description:** `Open-source Android AI Box dashboard and Safari Touchpad for compatible VW Group MIB2.5 head units.`
5. Select **Public**.
6. Do not initialize with a new README or `.gitignore` because they are already in this package.
7. Select **Create repository**.

## Step 2 — Upload the source files

1. Unzip the project on your PC.
2. On your empty repository page, choose **uploading an existing file** (or **Add file → Upload files**).
3. Upload the **contents** of the extracted folder, preserving the `app/` directory and all subdirectories. Do not upload only the ZIP file.
4. Commit message: `Initial public release v1.0.0`.
5. Select **Commit changes**.
6. Check that the repository contains `app/`, `README.md`, `INSTALLATION.md`, and `EXLAP_ATTRIBUTION.md`.

For a complete Android project, GitHub Desktop (https://desktop.github.com/) is often more reliable than uploading nested folders through the website. Clone the empty repository, copy the extracted files into it, commit, and push.

## Step 3 — Review before announcing the release

- Confirm the repository is Public and the README renders correctly.
- Confirm `EXLAP_ATTRIBUTION.md` links to the source and CC BY 4.0 license.
- Check that no personal tokens, logs, or private credentials have been added.
- Verify the APK was built from the source you intend to publish.
- Choose a license for code you own after reviewing third-party code/assets. Do not imply CC BY 4.0 covers the entire app.

## Step 4 — Publish an APK for regular users

1. In the repository, open **Releases** (or visit `https://github.com/YOUR_USERNAME/JF-AIBox-Bridge/releases`).
2. Select **Draft a new release**.
3. Create tag `v1.0.0`.
4. Release title: `JF AIBox Bridge v1.0.0`.
5. Paste the release notes below.
6. Upload the **tested APK** as a release asset (do not upload a source ZIP instead of an APK).
7. Select **Publish release**.

### Suggested GitHub Release text

**JF AIBox Bridge v1.0.0 — Initial Community Release**

- Vehicle dashboard and supported EXLAP telemetry
- TPMS data where available
- Android media information and playback controls
- iPhone/iPad Safari Touchpad and JPEG screen mirroring
- App shortcuts, Wi-Fi QR entry points and connection diagnostics
- No JF activation code or subscription

**Setup:** Connect the Android AI Box to a compatible head-unit Wi-Fi for EXLAP. Enable Android Notification Access for media metadata, Accessibility for remote gestures, and approve Screen Capture for mirroring. Keep the iPhone/iPad and AI Box reachable on a trusted local network. See the README and Installation Guide.

**EXLAP attribution:** Reference profiles and research: [EXLAP MIB2 Object Atlas](https://github.com/ijord/EXLAP-mib2-atlas), identified by its authors as CC BY 4.0. See EXLAP_ATTRIBUTION.md.

**Warning:** Unofficial community software; not endorsed by vehicle manufacturers. Use only while parked. Device/firmware compatibility varies. Local remote-control services must not be exposed to the internet.
