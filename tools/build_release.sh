#!/bin/bash

# Signs a release APK.
# To use:
# - Add key file to <project-dir>/connect.jks
# - Update BUILD_TOOLS if it doesn't match your path
# - Insert password when prompted
#
# Script created from docs:
#   https://developer.android.com/studio/publish/app-signing#sign-manually

BUILD_TOOLS=~/Library/Android/sdk/build-tools/27.0.3 # Update me on error!

BUILD_PATH=app/build/outputs/apk/amazonWebview/release
FINAL_NAME=app-amazonWebview-release.apk

# Assert pre-conditions.
if [ ! -d $BUILD_TOOLS ]; then
    echo "Build tools not found; update BUILD_TOOLS variable in script to continue."
    exit 1
fi
if [ ! -f connect.jks ]; then
    echo "Expected <proj-dir>/connect.jks for signing key."
    exit 1
fi
if [ ! -f .sentry_dsn_release ]; then
    echo "Error: expected <project-dir>/.sentry_dsn_release for Sentry key"
    exit 1
fi

# via https://unix.stackexchange.com/a/155077
if output=$(git status --porcelain) && [ -n "$output" ]; then
    echo "Error: uncommited git changes: exiting."
    exit 1
fi

# Ensure the tests are passing.
./quality/pre-push-recommended.sh || exit 1

# Build.
./gradlew -q clean assembleAmazonWebViewRelease || exit 1

# See sign_release.sh for details.
zip --quiet --delete $BUILD_PATH/app-amazonWebview-release-unsigned.apk \
    'META-INF/*kotlin_module' \
    'META-INF/*version' \
    'META-INF/proguard/*' \
    'META-INF/services/*' \
    'META-INF/web-fragment.xml' || exit 1

# Align.
$BUILD_TOOLS/zipalign -v -p 4 \
    $BUILD_PATH/app-amazonWebview-release-unsigned.apk \
    $BUILD_PATH/app-amazonWebview-release-unsigned-aligned.apk || exit 1

# Sign.
$BUILD_TOOLS/apksigner sign --ks connect.jks \
    --out $BUILD_PATH/$FINAL_NAME \
    $BUILD_PATH/app-amazonWebview-release-unsigned-aligned.apk || exit 1

# Verify.
$BUILD_TOOLS/apksigner verify -Werr $BUILD_PATH/$FINAL_NAME || exit 1

echo "Build successful. Opening build directory... Look for $FINAL_NAME"

# Open build dir on macOS.
open $BUILD_PATH
