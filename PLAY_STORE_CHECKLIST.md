# Play Store Release Checklist

This document outlines the required steps to prepare this app for Google Play Store release.

## ✅ Completed Configuration

- [x] Updated applicationId from `com.example.sdk` to `com.vxsudev.androidsdk`
- [x] Fixed Android Gradle Plugin version to stable release (8.7.3)
- [x] Enabled ProGuard/R8 minification for release builds
- [x] Added ProGuard keep rules for reflection-accessed classes
- [x] Updated targetSdk to 35 (latest stable)
- [x] Removed build artifacts from repository
- [x] Updated .gitignore to prevent future commits of build artifacts and secrets
- [x] Removed google-services.json from repository (must be added securely)

## 🔧 Required Before Release

### 1. Generate Release Keystore

**CRITICAL**: Never commit the keystore file to git!

```bash
keytool -genkey -v -keystore release.jks \
  -alias release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Store the keystore file and passwords securely (use a password manager).

### 2. Configure Signing in build.gradle.kts

Add the signing configuration to `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = "release"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... rest of release config
        }
    }
}
```

**Best Practice**: Use environment variables or CI/CD secrets for passwords, never hardcode them.

### 3. Firebase & Google Services Configuration

#### a. Register Release Package Name
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Add `com.vxsudev.androidsdk` as a new Android app or update existing

#### b. Add Release SHA-256 Fingerprint
Get your release keystore fingerprint:
```bash
keytool -list -v -keystore release.jks -alias release
```

Add the SHA-256 fingerprint to Firebase Console:
- Project Settings → Your App → Add Fingerprint

#### c. Download google-services.json
Download the updated `google-services.json` for your release configuration and place it in `app/` directory.

**SECURITY NOTE**: If your repository is public, DO NOT commit `google-services.json`. Instead:
- Keep it in secure storage
- Use CI/CD to inject it during build
- Or use different Firebase projects for dev/prod

### 4. Google Fit API Setup

Since the app uses Google Fit API (health data):

1. **Enable Google Fit API** in [Google Cloud Console](https://console.cloud.google.com/)
2. **Create OAuth 2.0 Client ID** for your release package:
   - Package name: `com.vxsudev.androidsdk`
   - SHA-256 fingerprint: (from your release keystore)
3. **Configure OAuth consent screen** with:
   - App name
   - User support email
   - Developer contact information
   - Scopes for Google Fit access

### 5. Privacy Policy (REQUIRED for Play Store)

Apps accessing health/fitness data MUST have a privacy policy.

**Create and host a privacy policy that includes:**
- What data you collect (Google Fit health data, activity data)
- How you use the data
- How you store the data
- How users can request data deletion
- Third-party services used (Firebase, Google Fit)

**Add privacy policy URL to:**
- Google Play Console → Store presence → Privacy policy
- In-app (visible before user grants permissions)

### 6. Play Console Data Safety Form

Complete the Data Safety form in Play Console:
- Declare collection of health/fitness data
- Specify if data is shared with third parties
- Explain data usage
- Confirm security practices

### 7. Build Release Bundle

```bash
# Clean build
./gradlew clean

# Build release AAB (Android App Bundle)
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

### 8. Verify Release Bundle

```bash
# Verify signing
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab

# Check bundle details with bundletool (install from Android developer tools)
bundletool build-apks --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app.apks \
  --mode=universal
```

### 9. Test Release Build

Before uploading to Play Store:
1. Install the release build on a test device
2. Test Google Sign-In with release SHA-256
3. Test Google Fit permissions and data access
4. Verify Firebase functionality (Firestore, Auth, Storage)
5. Test all core features

### 10. Play Store Upload

1. Go to [Google Play Console](https://play.google.com/console/)
2. Create or select your app
3. Go to "Release" → "Production" (or "Internal testing" first)
4. Upload `app-release.aab`
5. Complete store listing:
   - App name
   - Short description
   - Full description
   - Screenshots (phone & tablet)
   - Feature graphic
   - App icon
   - Content rating questionnaire
   - Privacy policy URL

### 11. Post-Upload Verification

After upload, Play Console will:
- Run automated security scans
- Check for policy violations
- Review health data handling (may require manual review)
- Verify SDK versions and permissions

**Review time**: 1-7 days (longer for apps with health data)

## 🔒 Security Best Practices

1. **Never commit**:
   - `release.jks` or any keystore files
   - `google-services.json` (if repo is public)
   - Passwords or API keys

2. **Use CI/CD**:
   - Store secrets in GitHub Actions, GitLab CI, or Bitbucket Pipelines
   - Inject credentials at build time
   - Use encrypted secret storage

3. **Rotate credentials**:
   - If any credentials are accidentally committed, rotate them immediately
   - Update Firebase project credentials
   - Consider creating new OAuth clients

## 📝 Additional Notes

### Version Management
Update version before each release in `app/build.gradle.kts`:
```kotlin
versionCode = 2  // Increment for each release
versionName = "1.1"  // Semantic version
```

### Required Permissions Justification
The app requests sensitive permissions. Be prepared to justify in Play Console:
- `ACTIVITY_RECOGNITION`: Required for Google Fit activity data
- `BODY_SENSORS`: Required for heart rate and health metrics
- `INTERNET`: Required for Firebase and cloud sync

### Content Rating
Complete the content rating questionnaire in Play Console. This app likely qualifies for "Everyone" but review carefully.

### Target Audience
If collecting data from minors, additional compliance is required (COPPA, GDPR).

## 📞 Support

For issues with:
- **Firebase**: Check [Firebase Support](https://firebase.google.com/support)
- **Play Store**: Use [Play Console Help](https://support.google.com/googleplay/android-developer)
- **Google Fit API**: See [Fit API Documentation](https://developers.google.com/fit)

---

**Last Updated**: 2025-10-29
**Target Release Version**: 1.0 (versionCode: 1)
