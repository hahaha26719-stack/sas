# WebView APK (camera stripped out)

A minimal Android app that loads **one website** in a full-screen WebView, with
**camera access completely stripped out** at every layer.

## 👉 The only thing you edit: `config.properties`

```properties
APP_NAME=My Web App            # ← the name shown under the app icon
WEBSITE_URL=https://example.com # ← the site the app loads
```

Change those two lines, rebuild, done. The build reads this file automatically
and wires the name + URL into the app — you never touch any Kotlin, XML, or
Gradle files.

(Optional advanced keys in the same file: `APP_ID`, `VERSION_NAME`, `VERSION_CODE`.
You normally don't need them.)

## How the camera is stripped out

Blocked at four independent layers, so there is no path to the camera:

1. **No permission** — `android.permission.CAMERA` is never declared, so Android
   can never grant it.
2. **Hardware marked unused** — `uses-feature android:name="android.hardware.camera*"
   android:required="false"`.
3. **WebRTC / getUserMedia denied** — `WebChromeClient.onPermissionRequest` calls
   `request.deny()` on every request.
4. **File-capture blocked + JS kill switch** — `<input capture="camera">` is
   cancelled, and `navigator.mediaDevices.getUserMedia` is overridden to reject.

Verify any built APK yourself:

```bash
$ANDROID_HOME/build-tools/34.0.0/aapt dump permissions MyWebApp.apk
# -> you will NOT see android.permission.CAMERA
```

## Building the APK

### On your own machine (recommended)
1. Open the project in **Android Studio** (it creates `local.properties` for you),
   or install the Android SDK and set `ANDROID_HOME`.
2. Build:
   ```bash
   ./gradlew assembleRelease
   ```
3. APK appears at `app/build/outputs/apk/release/app-release.apk`.

> Requires **JDK 17** (newer JDKs break the Android/Kotlin toolchain).

### Prebuilt APK
A ready-to-install APK is in [`dist/MyWebApp.apk`](dist/MyWebApp.apk), signed with
the standard Android **debug key** — fine for sideloading and testing. For a
Play Store release, generate your own keystore and sign a proper release build.

## Install on a device
```bash
adb install dist/MyWebApp.apk
```
Or copy the `.apk` to the phone and tap it (enable "install from unknown sources").

## Project layout
```
config.properties                      ← YOU EDIT THIS (name + URL)
app/src/main/AndroidManifest.xml       ← no camera permission
app/src/main/java/.../MainActivity.kt  ← WebView + camera-blocking logic
dist/MyWebApp.apk                      ← prebuilt APK
```
