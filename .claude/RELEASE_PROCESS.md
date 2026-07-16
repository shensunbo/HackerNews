# Release Process

Use this manual process to publish a signed APK. Do not commit the keystore,
`keystore.properties`, APK files, or passwords.

## One-time signing setup

1. Create and securely back up one long-lived signing key. Keep the same key for
   every future Release update of `com.example.hackernews`.

   ```bash
   mkdir -p keystore
   keytool -genkeypair -v \
     -keystore keystore/readlater-release.jks \
     -storetype PKCS12 \
     -alias readlater \
     -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Copy `keystore.properties.example` to the ignored `keystore.properties`; fill
   it with the local keystore path, alias, and passwords. Restrict both files to
   the owner and store an encrypted backup outside this repository.

## Build and validate

1. Increase `versionCode` and `versionName` in `app/build.gradle.kts`. Use a tag
   matching `versionName`, for example `v1.0.1`.
2. Run JVM tests, build the signed APK, and verify its signature:

   ```bash
   ./gradlew :app:testDebugUnitTest :app:assembleRelease
   /home/sunboshen/Android/Sdk/build-tools/37.0.0/apksigner verify --verbose \
     app/build/outputs/apk/release/app-release.apk
   sha256sum app/build/outputs/apk/release/app-release.apk \
     > app/build/outputs/apk/release/ReadLater-v<version>.apk.sha256
   ```

3. Install and manually check the Release APK on a physical device:

   ```bash
   adb install app/build/outputs/apk/release/app-release.apk
   adb shell am start -n com.example.hackernews/.MainActivity
   ```

   A Debug build has a different signing key. Before the first Release install
   on a device carrying Debug, uninstall Debug first; this clears local app data:

   ```bash
   adb uninstall com.example.hackernews
   ```

   Future Release APKs signed with the same keystore can update the installed
   Release app directly and retain its data.

## Publish to GitHub

1. Commit and push the version change, then authenticate once with `gh auth login`.
2. Create a non-draft release from `main` and upload the APK plus checksum:

   ```bash
   gh release create v<version> \
     app/build/outputs/apk/release/app-release.apk#ReadLater-v<version>.apk \
     app/build/outputs/apk/release/ReadLater-v<version>.apk.sha256#ReadLater-v<version>.apk.sha256 \
     --repo shensunbo/HackerNews \
     --target main \
     --title "ReadLater v<version>" \
     --notes "<release notes>"
   ```

3. Confirm the release is not a draft or prerelease and both assets are uploaded:

   ```bash
   gh release view v<version> --repo shensunbo/HackerNews \
     --json url,isDraft,isPrerelease,assets
   ```

## Release checklist

- [ ] Version and Git tag match.
- [ ] APK is signed and `apksigner verify` passes.
- [ ] Release APK installs and opens on Android 15+.
- [ ] APK and SHA-256 file are attached to a public, non-draft GitHub Release.
- [ ] Keystore and password configuration remain ignored by Git and backed up securely.
