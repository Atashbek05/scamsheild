# Release Build Guide

## 1. Generate a keystore

Run this command once and store the `.jks` file in a safe place **outside** the repository:

```bash
keytool -genkey -v \
  -keystore scamshield-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias scamshield
```

You will be prompted for:
- Keystore password (`storePassword`)
- Key alias password (`keyPassword`)
- Distinguished name fields (name, org, country, etc.)

> **Keep the `.jks` file and all passwords in a password manager. Losing them means you can never update the app on Play Store.**

## 2. Create keystore.properties

Create the file `keystore.properties` in the **project root** (next to `settings.gradle.kts`).  
This file is already in `.gitignore` — never commit it.

```properties
storeFile=/absolute/path/to/scamshield-release.jks
storePassword=your_store_password
keyAlias=scamshield
keyPassword=your_key_password
```

On Windows use forward slashes or escape backslashes:

```properties
storeFile=C:/Keys/scamshield-release.jks
```

## 3. Build a signed release APK / AAB

```bash
# AAB (required for Play Store)
./gradlew bundleRelease

# APK (for direct distribution / testing)
./gradlew assembleRelease
```

Output locations:
- AAB: `app/build/outputs/bundle/release/app-release.aab`
- APK: `app/build/outputs/apk/release/app-release.apk`

## 4. Upload to Play Store

1. Open [Google Play Console](https://play.google.com/console)
2. Create the app (first time) or open the existing listing
3. Go to **Release → Production → Create new release**
4. Upload `app-release.aab`
5. Fill in release notes and submit for review

## 5. Before each release

- Increment `versionCode` (must be higher than the previous release)
- Update `versionName` to a user-visible string (e.g. `"1.1"`)

Both are in `app/build.gradle.kts` → `defaultConfig`.
