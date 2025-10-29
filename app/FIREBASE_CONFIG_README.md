# Firebase Configuration Notice

## ⚠️ IMPORTANT: google-services.json

The `google-services.json` file in this directory contains Firebase configuration for the app.

### Current Status
- The file has been updated to use the new package name: `com.vxsudev.androidsdk`
- The Firebase project ID and credentials are from the original development project

### Before Release

**YOU MUST**:

1. **Update Firebase Console** to register the new package name:
   - Go to https://console.firebase.google.com/
   - Select project: `sdk-android-dfa5d`
   - Add a new Android app with package name: `com.vxsudev.androidsdk`
   - OR update the existing app's package name

2. **Add Release SHA-256 Fingerprint**:
   ```bash
   keytool -list -v -keystore ../release.jks -alias release
   ```
   Copy the SHA-256 fingerprint and add it to Firebase Console

3. **Download Updated google-services.json**:
   - After adding the app/updating the package name in Firebase Console
   - Download the new `google-services.json` file
   - Replace this file with the downloaded version

4. **Configure OAuth for Google Fit**:
   - Google Cloud Console → APIs & Services → Credentials
   - Create OAuth 2.0 Client ID for Android
   - Use package name: `com.vxsudev.androidsdk`
   - Use SHA-256 from your release keystore

### Security Note

If this repository is public, consider:
- NOT committing the actual `google-services.json` (add to `.gitignore`)
- Using placeholder values for public repos
- Injecting the real file via CI/CD during builds
- Using separate Firebase projects for dev/staging/production

### Current Configuration

The current file has been modified but may not work until you:
1. Register `com.vxsudev.androidsdk` in Firebase Console
2. Download the updated configuration file

**Package Name in File**: `com.vxsudev.androidsdk`
**Firebase Project**: `sdk-android-dfa5d`
**Project Number**: `149436593993`

---

**Note**: The `.gitignore` file has been configured to ignore `google-services.json` files, but this one is currently tracked because it was committed previously. If you want to remove it from git history, run:
```bash
git rm --cached google-services.json
```
