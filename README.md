# AndroidSDK Pixel 8

Android SDK application with smart watch data visualization, Google Fit integration, and Firebase backend.

## Features

- 📊 Smart watch data visualization with charts
- 🏃 Google Fit API integration for health data
- 🔥 Firebase Firestore for data storage
- ☁️ Firebase Cloud Storage for file uploads
- 🔄 Runtime environment switching (Dev, Staging, Production)
- 📱 Android 14 (API 34) support

## Environment Configuration & Testing

### Overview

This app supports **runtime environment switching** using Firebase. You can configure multiple Firebase projects (Dev, Staging, Production) and switch between them at runtime without rebuilding the app.

The app uses runtime initialization of `FirebaseApp` instances by parsing `google-services-<env>.json` files from the app's `assets/` directory. This approach allows:
- ✅ Building the app without committing real Firebase credentials
- ✅ Testers to safely switch environments
- ✅ CI/CD to build without secrets in the repository

### Local Development Setup

#### Prerequisites

- Android Studio Arctic Fox or later
- JDK 17
- Android SDK with API 34
- Firebase project(s) for your environments

#### Step 1: Get Your Firebase Configuration Files

For each environment you want to use:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (e.g., your Dev project)
3. Click the gear icon → **Project Settings**
4. Scroll to **Your apps** section
5. Click **Download google-services.json**
6. Save it with an environment-specific name

#### Step 2: Add Configuration Files Locally

**For the default environment (required for build):**
```bash
# Copy your default/production google-services.json
cp ~/Downloads/google-services.json app/google-services.json
```

**For runtime environment switching (optional):**
```bash
# Copy your environment-specific configs to assets
cp ~/Downloads/google-services-dev.json app/src/main/assets/google-services-dev.json
cp ~/Downloads/google-services-staging.json app/src/main/assets/google-services-staging.json
```

**Note:** These files are in `.gitignore` and will NOT be committed to the repository.

#### Step 3: Build and Run

```bash
./gradlew assembleDebug
```

Or simply click "Run" in Android Studio.

### Testing Environment Switching

1. Launch the app on a device or emulator
2. At the top of the screen, you'll see an **Environment Spinner** and **Apply Env** button
3. Select an environment from the spinner:
   - **Default**: Uses the default Firebase app (from `app/google-services.json`)
   - **Dev**: Uses `google-services-dev.json` from assets
   - **Staging**: Uses `google-services-staging.json` from assets
4. Click **Apply Env**
5. A toast message will show the selected environment's `projectId` and `storageBucket`
6. If a config file is missing, you'll see a user-friendly error message (the app won't crash)

**Verify in Logs:**
```bash
adb logcat | grep CloudStorageManager
```

Look for log messages like:
```
CloudStorageManager: ✅ FirebaseApp initialized: dev (projectId: my-dev-project)
CloudStorageManager: 📋 Parsed config: projectId=my-dev-project, storageBucket=my-dev-project.appspot.com
```

### CI/CD Setup

#### GitHub Actions (Included)

The repository includes a `.github/workflows/build-and-test.yml` workflow that:
- ✅ Runs on every push and pull request
- ✅ Builds the app with a placeholder `google-services.json` (no real secrets)
- ✅ Runs unit tests
- ✅ Runs lint checks
- ✅ Performs a secrets safety scan to prevent committing real credentials

**The CI build works without any secrets** because it uses a placeholder configuration.

#### Injecting Real Credentials in Private CI (Optional)

If you want to build with real Firebase credentials in a private CI system:

1. **Store your `google-services.json` as a GitHub Secret:**
   - Go to your repository → Settings → Secrets and variables → Actions
   - Create a new secret named `GOOGLE_SERVICES_JSON`
   - Paste the entire contents of your `google-services.json` file

2. **Modify the workflow to use the secret:**
   ```yaml
   - name: Create google-services.json from secret
     run: echo '${{ secrets.GOOGLE_SERVICES_JSON }}' > app/google-services.json
   ```

3. **Add environment-specific configs (optional):**
   - Create secrets like `GOOGLE_SERVICES_DEV_JSON`, `GOOGLE_SERVICES_STAGING_JSON`
   - Write them to `app/src/main/assets/` in the workflow

### Template Files

The repository includes **template files** in:
- `app/google-services.json.template`
- `app/src/main/assets/google-services-dev.json.template`
- `app/src/main/assets/google-services-staging.json.template`

These templates:
- ❌ Do NOT contain real credentials (placeholder values only)
- ✅ Show the expected JSON structure
- ✅ Include instructions on how to obtain real configs
- ✅ Are safe to commit to the repository

**To use a template:** Copy it without the `.template` extension and replace the placeholder values with your real Firebase config.

### Security & Secrets

#### Files That Should NEVER Be Committed

The following files are automatically blocked by the secrets check script:
- `google-services.json` (except `*.template`)
- `*.jks` (keystore files)
- `*.keystore`
- `local.properties`
- `*.p12`, `*.pem` (private keys)

#### Secrets Safety Check

Run the secrets check script before committing:
```bash
./tools/check-for-secrets.sh
```

This script:
- ✅ Checks for forbidden file patterns
- ✅ Runs automatically in CI (will fail the build if secrets are found)
- ✅ Can be used as a pre-commit hook (optional)

**To install as a pre-commit hook:**
```bash
ln -s ../../tools/check-for-secrets.sh .git/hooks/pre-commit
```

### Architecture: How Runtime Environment Switching Works

1. **CloudStorageManager.getFirebaseAppForEnvironment():**
   - Reads a `google-services-<env>.json` file from `assets/`
   - Parses the JSON to extract: `project_id`, `api_key`, `mobilesdk_app_id`, `storage_bucket`
   - Builds a `FirebaseOptions` object
   - Initializes a named `FirebaseApp` (e.g., name: "dev")
   - Caches the initialized app for reuse

2. **MainActivity Environment Selector:**
   - Maps environment names to config files: `"Dev" → "google-services-dev.json"`
   - Calls `CloudStorageManager.getFirebaseAppForEnvironment()` when "Apply Env" is clicked
   - Shows toast with project details or error message
   - Does NOT crash if config file is missing (graceful degradation)

3. **Error Handling:**
   - Missing file → Returns `null`, falls back to default `FirebaseApp`
   - Invalid JSON → Logs error, returns `null`
   - All errors logged clearly for debugging

### Testing

#### Run Unit Tests
```bash
./gradlew test
```

#### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

#### Run Lint
```bash
./gradlew lint
```

### Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/vxsudev/androidsdk/
│   │   │   ├── CloudStorageManager.java    # Firebase Storage + runtime init
│   │   │   ├── MainActivity.java           # UI + environment selector
│   │   │   ├── FirestoreManager.java       # Firestore operations
│   │   │   ├── GoogleFitManager.java       # Google Fit integration
│   │   │   └── ...
│   │   ├── assets/
│   │   │   ├── google-services-dev.json.template
│   │   │   └── google-services-staging.json.template
│   │   └── res/
│   └── test/
│       └── java/com/vxsudev/androidsdk/
│           └── CloudStorageManagerTest.kt   # Unit tests for runtime init
├── google-services.json (local only, not committed)
└── google-services.json.template (template, committed)
```

### Troubleshooting

#### Build fails with "google-services.json not found"
- Copy `app/google-services.json.template` to `app/google-services.json`
- Replace placeholder values with a real config from Firebase Console

#### Environment switching shows "Config file not found"
- Make sure you've added the config file to `app/src/main/assets/`
- File name must match exactly (e.g., `google-services-dev.json`)
- Check logcat for detailed error messages

#### CI build fails with secrets detected
- Run `./tools/check-for-secrets.sh` locally to see which files are problematic
- Make sure you didn't accidentally commit `google-services.json` or keystore files
- Remove them with: `git rm --cached <filename>`

## Dependencies

- Firebase BoM 34.4.0 (Firestore, Storage, Auth, Analytics)
- Google Fit APIs 21.1.0
- MPAndroidChart 3.1.0 (charting library)
- AndroidX libraries
- JUnit + Mockito (testing)

## License

[Add your license here]

## Contributing

[Add contribution guidelines here]
