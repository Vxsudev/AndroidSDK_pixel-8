#!/usr/bin/env bash
set -euo pipefail

# apply-upgrade.sh
# Safe automated edits to upgrade:
# - Gradle wrapper distributionUrl -> Gradle 8.8
# - Root AGP version -> 8.9.1 (handles build.gradle.kts or build.gradle)
# - app module: compileSdk -> 36 and targetSdk -> 36
# - app module: ensure androidx.core/core-ktx 1.17.0 and androidx.activity 1.11.0 are present
#
# Usage (from repo root):
#   bash scripts/apply-upgrade.sh
#
# The script makes .bak backups of edited files (e.g. build.gradle.kts.bak).
# Review diffs after running, then run: ./gradlew wrapper --gradle-version 8.8
# and test locally: ./gradlew clean assembleDebug --stacktrace

readonly GRADLE_DIST="https://services.gradle.org/distributions/gradle-8.8-bin.zip"
readonly AGP_VERSION="8.9.1"
readonly COMPILE_SDK="36"
readonly TARGET_SDK="36"

echo "Starting upgrade script..."

backup() {
  local f="$1"
  if [ -f "$f" ]; then
    cp -a "$f" "$f.bak"
    echo "Backed up $f -> $f.bak"
  fi
}

update_gradle_wrapper() {
  local file="gradle/wrapper/gradle-wrapper.properties"
  if [ -f "$file" ]; then
    backup "$file"
    if grep -q '^distributionUrl=' "$file"; then
      printf '%s\n' "Setting distributionUrl -> $GRADLE_DIST in $file"
      sed -i.bak "s|^distributionUrl=.*|distributionUrl=${GRADLE_DIST}|" "$file"
    else
      printf '%s\n' "No distributionUrl found in $file, appending one."
      echo "distributionUrl=${GRADLE_DIST}" >> "$file"
    fi
  else
    echo "Warning: $file not found. Skipping gradle wrapper file update."
  fi
}

update_root_agp_kts() {
  local file="build.gradle.kts"
  if [ -f "$file" ]; then
    backup "$file"
    echo "Updating AGP versions in $file -> $AGP_VERSION"
    perl -0777 -pe "s/id\\s*\\(\\s*\"com.android.application\"\\s*\\)\\s*version\\s*\\\"[^\\\"]+\\\"/id(\"com.android.application\") version \"${AGP_VERSION}\"/g" -i.bak "$file"
    perl -0777 -pe "s/id\\s*\\(\\s*\"com.android.library\"\\s*\\)\\s*version\\s*\\\"[^\\\"]+\\\"/id(\"com.android.library\") version \"${AGP_VERSION}\"/g" -i.bak "$file"
    if ! grep -q 'id("com.android.application")' "$file"; then
      if grep -q '^plugins\s*{' "$file"; then
        echo "Adding com.android.application/library plugin entries into plugins block in $file"
        awk -v ins="    id(\"com.android.application\") version \"${AGP_VERSION}\" apply false\n    id(\"com.android.library\") version \"${AGP_VERSION}\" apply false" \
            'BEGIN{p=0} {print} /^plugins[[:space:]]*{/{ if(!p){print ins; p=1}}' "$file" > "$file.tmp" && mv "$file.tmp" "$file"
      else
        echo "No plugins block found in $file; plugins could not be auto-inserted. Please edit manually if needed."
      fi
    fi
  fi
}

update_root_agp_groovy() {
  local file="build.gradle"
  if [ -f "$file" ]; then
    backup "$file"
    echo "Updating AGP classpath in $file -> $AGP_VERSION"
    if grep -q 'com.android.tools.build:gradle' "$file"; then
      perl -0777 -pe "s/classpath\\s*\\(\\s*\"com.android.tools.build:gradle:[^\\\"]+\"\\s*\\)/classpath(\"com.android.tools.build:gradle:${AGP_VERSION}\")/g" -i.bak "$file"
    else
      echo "No com.android.tools.build:gradle classpath found in $file. Skipping edit."
    fi
  fi
}

insert_deps_into_kts() {
  local file="$1"
  local depLines="$2"
  if ! grep -q 'androidx.core:core' "$file" || ! grep -q 'androidx.activity:activity' "$file"; then
    awk -v ins="$depLines" '
      BEGIN{p=0}
      {
        print
        if ($0 ~ /^[[:space:]]*dependencies[[:space:]]*{[[:space:]]*$/ && p==0) {
          print ins
          p=1
        }
      }
    ' "$file" > "$file.tmp" && mv "$file.tmp" "$file"
    echo "Inserted dependency lines into $file"
  else
    perl -0777 -pe "s/androidx.core:core(-ktx)?:\\s*[^\\\"\\)\\n]*/androidx.core:core\\1:1.17.0/g" -i.bak "$file" || true
    perl -0777 -pe "s/androidx.activity:activity:[^\\\"\\)\\n]*/androidx.activity:activity:1.11.0/g" -i.bak "$file" || true
    echo "Dependency versions updated (where present) in $file"
  fi
}

insert_deps_into_groovy() {
  local file="$1"
  local depLines="$2"
  if ! grep -q 'androidx.core:core' "$file" || ! grep -q 'androidx.activity:activity' "$file"; then
    awk -v ins="$depLines" '
      BEGIN{p=0}
      {
        print
        if ($0 ~ /^[[:space:]]*dependencies[[:space:]]*{[[:space:]]*$/ && p==0) {
          print ins
          p=1
        }
      }
    ' "$file" > "$file.tmp" && mv "$file.tmp" "$file"
    echo "Inserted dependency lines into $file"
  else
    perl -0777 -pe "s/androidx.core:core(-ktx)?:\\s*[^\\\"\\)\\n]*/androidx.core:core\\1:1.17.0/g" -i.bak "$file" || true
    perl -0777 -pe "s/androidx.activity:activity:[^\\\"\\)\\n]*/androidx.activity:activity:1.11.0/g" -i.bak "$file" || true
    echo "Dependency versions updated (where present) in $file"
  fi
}

update_app_kts() {
  local file="app/build.gradle.kts"
  if [ -f "$file" ]; then
    backup "$file"
    echo "Updating compileSdk/targetSdk in $file -> $COMPILE_SDK / $TARGET_SDK"
    perl -0777 -pe "s/compileSdk\\s*=\\s*\\d+/compileSdk = ${COMPILE_SDK}/g" -i.bak "$file"
    perl -0777 -pe "s/targetSdk\\s*=\\s*\\d+/targetSdk = ${TARGET_SDK}/g" -i.bak "$file"

    local deps=$'    implementation("androidx.core:core:1.17.0")\n    implementation("androidx.core:core-ktx:1.17.0")\n    implementation("androidx.activity:activity:1.11.0")'
    insert_deps_into_kts "$file" "$deps"
  else
    echo "No app/build.gradle.kts found - skipping Kotlin DSL app edits."
  fi
}

update_app_groovy() {
  local file="app/build.gradle"
  if [ -f "$file" ]; then
    backup "$file"
    echo "Updating compileSdk/targetSdk in $file -> $COMPILE_SDK / $TARGET_SDK"
    perl -0777 -pe "s/compileSdk\\s+\\d+/compileSdk ${COMPILE_SDK}/g" -i.bak "$file"
    perl -0777 -pe "s/targetSdk\\s+\\d+/targetSdk ${TARGET_SDK}/g" -i.bak "$file"

    local deps=$'    implementation \"androidx.core:core:1.17.0\"\n    implementation \"androidx.core:core-ktx:1.17.0\"\n    implementation \"androidx.activity:activity:1.11.0\"'
    insert_deps_into_groovy "$file" "$deps"
  else
    echo "No app/build.gradle found - skipping Groovy app edits."
  fi
}

main() {
  update_gradle_wrapper

  if [ -f "build.gradle.kts" ]; then
    update_root_agp_kts
  elif [ -f "build.gradle" ]; then
    update_root_agp_groovy
  else
    echo "Warning: No root build.gradle(.kts) found. Please update AGP version manually to $AGP_VERSION if needed."
  fi

  update_app_kts
  update_app_groovy

  echo
  echo "Done applying edits. Files backed up with .bak suffix in-place."
  echo "Next steps (run these commands from repo root):"
  echo "  1) Review changes: git diff --staged (or git diff)"
  echo "  2) Update gradle wrapper scripts locally:"
  echo "       ./gradlew wrapper --gradle-version 8.8"
  echo "  3) Build and test:"
  echo "       ./gradlew clean assembleDebug --stacktrace"
  echo "       ./gradlew lintDebug"
  echo "       ./gradlew testDebugUnitTest"
  echo "  4) Commit the changes and push to a new branch (example):"
  echo "       git checkout -b copilot/upgrade-agp-compileSdk-36"
  echo "       git add -A"
  echo "       git commit -m \"chore: upgrade AGP to ${AGP_VERSION}, gradle wrapper to 8.8, compileSdk/targetSdk to ${COMPILE_SDK}; align AndroidX deps\""
  echo "       git push origin copilot/upgrade-agp-compileSdk-36"
  echo
  echo "If you want, after pushing run:"
  echo "  gh pr create --title \"Upgrade AGP, Gradle wrapper and compileSdk to support AndroidX 1.17 / activity 1.11\" --body-file pr-body.txt --base main"
  echo "Create a pr-body.txt file with the PR body (I can provide the exact contents if you want)."
}

main "$@"

# End of script

