# GitHub Copilot Instructions for AndroidSDK_pixel-8

## Project Overview

This is an Android health and fitness tracking application built for Pixel 8 devices. The app integrates with Google Fit API to collect and visualize health data, using Firebase for backend services.

**Package Name**: `com.vxsudev.androidsdk`
**Language**: Kotlin and Java
**Min SDK**: 26 (Android 8.0)
**Target SDK**: 36 (Android 14)
**Compile SDK**: 36

## Technology Stack

- **Build System**: Gradle (Kotlin DSL)
- **UI**: XML layouts with ViewBinding
- **Backend**: Firebase (Analytics, Firestore, Auth, Storage)
- **Health Data**: Google Fit API
- **Charts**: MPAndroidChart v3.1.0
- **JSON**: Gson
- **Testing**: JUnit, AndroidX Test, Espresso

## Project Structure

```
AndroidSDK_pixel-8/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/vxsudev/androidsdk/  # Main source code
│   │   │   ├── res/                            # Resources (layouts, drawables, etc.)
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                               # Unit tests
│   │   └── androidTest/                        # Instrumented tests
│   ├── build.gradle.kts                        # App-level build config
│   └── proguard-rules.pro                      # ProGuard/R8 rules
├── build.gradle.kts                            # Root build config
├── settings.gradle.kts                         # Project settings
└── gradle/libs.versions.toml                   # Version catalog
```

## Development Guidelines

### Code Style

1. **Language Preference**: Use Kotlin for new code. Java is acceptable for maintaining existing Java files.
2. **Naming Conventions**:
   - Classes: PascalCase (e.g., `HealthDataManager`)
   - Functions: camelCase (e.g., `fetchHealthData`)
   - Constants: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)
   - Resources: snake_case (e.g., `activity_main.xml`, `btn_submit`)
3. **ViewBinding**: Always use ViewBinding for accessing views, never use `findViewById()`
4. **Null Safety**: Leverage Kotlin's null safety features; avoid using `!!` operator

### Architecture & Patterns

- Follow MVVM pattern where applicable
- Keep UI logic separate from business logic
- Use lifecycle-aware components (ViewModel, LiveData)
- Keep Activities/Fragments lightweight

### Android Best Practices

1. **Permissions**: Always check and request runtime permissions before accessing sensitive APIs (Google Fit, sensors)
2. **Background Work**: Use WorkManager for background tasks, not Services
3. **Memory Leaks**: Avoid holding Activity/Context references in long-lived objects
4. **Resources**: Use string resources for user-facing text (not hardcoded strings)
5. **Accessibility**: Include content descriptions for UI elements

### Firebase Integration

- All Firebase services are managed via Firebase BoM (Bill of Materials)
- Always handle Firebase Auth state changes
- Use Firestore security rules properly
- Never commit `google-services.json` to the repository
- Handle offline scenarios gracefully

### Google Fit API

- Request only necessary scopes
- Handle permission denials gracefully
- Cache data locally when possible
- Respect user privacy and data access preferences
- Test with different OAuth configurations

### Security & Privacy

1. **Secrets Management**:
   - Never commit keystore files (`*.jks`, `*.keystore`)
   - Never commit API keys or credentials
   - Use environment variables for sensitive data
   - Keep `google-services.json` out of version control (if repo is public)

2. **ProGuard/R8**:
   - All reflection-accessed classes must have keep rules in `proguard-rules.pro`
   - Test release builds thoroughly after adding new classes
   - Keep rules for Firebase, Google Fit, and data models are already configured

3. **Data Privacy**:
   - Health data is sensitive - handle with care
   - Implement proper data deletion mechanisms
   - Follow GDPR and health data regulations
   - Update privacy policy when adding new data collection

## Building & Testing

### Build Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release AAB (requires signing config)
./gradlew bundleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run lint checks
./gradlew lint
```

### Testing Requirements

1. **Unit Tests**: Required for business logic, data models, and utility classes
   - Location: `app/src/test/`
   - Framework: JUnit 4
   - Run with: `./gradlew test`

2. **Instrumented Tests**: Required for UI and integration tests
   - Location: `app/src/androidTest/`
   - Framework: AndroidX Test, Espresso
   - Run with: `./gradlew connectedAndroidTest`

3. **Test Coverage**:
   - Aim for >80% coverage on business logic
   - Test edge cases and error handling
   - Mock Firebase and Google Fit APIs in tests

### Pre-commit Checklist

Before committing code:
- [ ] Run `./gradlew build` successfully
- [ ] Run `./gradlew test` with no failures
- [ ] Run `./gradlew lint` and fix critical issues
- [ ] Verify ViewBinding is used (no `findViewById()`)
- [ ] Check for hardcoded strings (use string resources)
- [ ] Ensure no sensitive data is committed
- [ ] Update relevant documentation if needed

## Common Tasks & Examples

### Adding a New Activity

1. Create layout XML in `app/src/main/res/layout/`
2. Create Activity class in `com.vxsudev.androidsdk` package
3. Enable ViewBinding and use it to access views
4. Register Activity in `AndroidManifest.xml`
5. Add navigation logic from existing Activities
6. Add corresponding tests

### Adding a New Dependency

1. Check if dependency is already in `gradle/libs.versions.toml`
2. If not, add version to version catalog first
3. Add implementation in `app/build.gradle.kts`
4. Sync project and verify build succeeds
5. Add ProGuard keep rules if needed
6. Update documentation if it's a major dependency

### Working with Firebase

```kotlin
// Get Firestore instance
val db = Firebase.firestore

// Query data
db.collection("health_data")
    .whereEqualTo("userId", userId)
    .get()
    .addOnSuccessListener { documents ->
        // Handle success
    }
    .addOnFailureListener { exception ->
        // Handle error
    }
```

### Accessing Google Fit Data

```kotlin
// Build Fit API client
val fitnessOptions = FitnessOptions.builder()
    .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
    .build()

val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)

// Check permissions
if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
    GoogleSignIn.requestPermissions(activity, REQUEST_CODE, account, fitnessOptions)
}
```

## Release Process

For releasing to Play Store, follow the comprehensive checklist in `PLAY_STORE_CHECKLIST.md`.

Key steps:
1. Generate release keystore (never commit it)
2. Configure signing in build.gradle.kts
3. Update Firebase with release SHA-256 fingerprint
4. Configure Google Fit OAuth for release
5. Create and host privacy policy
6. Complete Play Console Data Safety form
7. Build and test release bundle
8. Upload to Play Console

## Known Issues & Limitations

1. **Network Access**: Some CI/CD environments may have restricted network access, preventing builds. Test locally first.
2. **Google Services**: Requires valid `google-services.json` for Firebase features to work.
3. **OAuth Configuration**: Google Fit APIs require proper OAuth setup with correct package name and SHA-256.
4. **Signing**: Release builds require signing configuration (not included in repository).

## Documentation

- **PLAY_STORE_CHECKLIST.md**: Complete guide for Play Store release
- **CHANGES_SUMMARY.md**: History of major changes to the codebase
- **app/FIREBASE_CONFIG_README.md**: Firebase configuration instructions

## Support & Resources

- **Android Developers**: https://developer.android.com/
- **Firebase Docs**: https://firebase.google.com/docs
- **Google Fit API**: https://developers.google.com/fit
- **Kotlin Style Guide**: https://developer.android.com/kotlin/style-guide
- **Material Design**: https://material.io/develop/android

## Tips for Copilot

When working on tasks in this repository:

1. **Always check existing patterns** before adding new code
2. **Maintain consistency** with existing code style
3. **Test thoroughly** - health data is sensitive
4. **Consider offline scenarios** - app should work without network
5. **Keep dependencies minimal** - only add what's necessary
6. **Document complex logic** - especially health data calculations
7. **Handle errors gracefully** - provide meaningful error messages
8. **Respect user privacy** - minimize data collection
9. **Follow Android lifecycle** - properly handle configuration changes
10. **Keep UI responsive** - use coroutines or background threads for heavy work

## Issue Assignment Guidelines

Good tasks for Copilot:
- Bug fixes in existing features
- Adding unit tests for existing code
- Refactoring to improve code quality
- Updating documentation
- Adding ProGuard rules for new classes
- Implementing UI improvements
- Adding error handling
- Performance optimizations

Tasks requiring human review:
- Changes to Firebase security rules
- OAuth configuration changes
- Privacy policy updates
- Play Store release process
- Major architectural changes
- Health data calculation logic
- Authentication flows

---

**Last Updated**: 2025-10-31
**Maintainer**: @Vxsudev
