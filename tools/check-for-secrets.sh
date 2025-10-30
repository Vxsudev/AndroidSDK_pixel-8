#!/bin/bash
# 
# Secrets Safety Check Script
# =============================
# This script checks for files that should never be committed to the repository.
# It's used both as a pre-commit hook (optional) and in CI to prevent secrets from being leaked.
#
# Exit codes:
#   0 - No secrets found (safe)
#   1 - Secrets found (unsafe, should fail CI/commit)

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🔍 Checking for secret files that should not be committed..."

# List of patterns to check (without .template extension)
FORBIDDEN_PATTERNS=(
    "google-services.json"
    ".*\.jks"
    ".*\.keystore"
    "release.jks"
    "release.keystore"
    "debug.jks"
    "debug.keystore"
    ".*\.p12"
    ".*\.pem"
    "local.properties"
)

# Function to check if file is allowed (is a template)
is_template_file() {
    local file="$1"
    if [[ "$file" == *.template ]]; then
        return 0  # True - is a template
    fi
    return 1  # False - not a template
}

# Track if any forbidden files were found
FOUND_SECRETS=0

# Check each pattern
for pattern in "${FORBIDDEN_PATTERNS[@]}"; do
    # Use git ls-files to find tracked files matching the pattern
    # Exclude template files explicitly
    while IFS= read -r file; do
        if ! is_template_file "$file"; then
            # Check if it's in an excluded directory
            if [[ ! "$file" =~ ^(build/|\.gradle/|\.idea/) ]]; then
                echo -e "${RED}❌ FORBIDDEN FILE FOUND:${NC} $file"
                echo "   This file should not be committed to the repository."
                echo "   Please remove it with: git rm --cached $file"
                FOUND_SECRETS=1
            fi
        fi
    done < <(git ls-files | grep -E "$pattern" || true)
done

# Check for google-services.json in specific locations (not templates)
GOOGLE_SERVICES_FILES=$(git ls-files | grep -E 'google-services.*\.json$' | grep -v '\.template$' || true)
if [ -n "$GOOGLE_SERVICES_FILES" ]; then
    echo -e "${RED}❌ FORBIDDEN: google-services.json files found:${NC}"
    echo "$GOOGLE_SERVICES_FILES"
    echo ""
    echo "These files contain Firebase credentials and must not be committed."
    echo "Only .template files should be in the repository."
    FOUND_SECRETS=1
fi

# Check for keystore files
KEYSTORE_FILES=$(git ls-files | grep -E '\.(jks|keystore|p12)$' || true)
if [ -n "$KEYSTORE_FILES" ]; then
    echo -e "${RED}❌ FORBIDDEN: Keystore files found:${NC}"
    echo "$KEYSTORE_FILES"
    echo ""
    echo "Keystore files contain signing keys and must not be committed."
    FOUND_SECRETS=1
fi

# Final verdict
echo ""
if [ $FOUND_SECRETS -eq 1 ]; then
    echo -e "${RED}═══════════════════════════════════════════════════${NC}"
    echo -e "${RED}❌ SECRETS DETECTED - COMMIT BLOCKED${NC}"
    echo -e "${RED}═══════════════════════════════════════════════════${NC}"
    echo ""
    echo "Please remove the above files before committing."
    echo "To remove a file from git (but keep it locally):"
    echo "  git rm --cached <filename>"
    echo ""
    echo "Make sure these patterns are in .gitignore:"
    echo "  - google-services.json (except *.template)"
    echo "  - *.jks"
    echo "  - *.keystore"
    echo "  - local.properties"
    exit 1
else
    echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}✅ NO SECRETS DETECTED - SAFE TO COMMIT${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
    exit 0
fi
