#!/bin/bash

# Clean builds a release build and signs it with our release keys using the Autograph
# server. This script has no arguments. This script *MUST BE RUN FROM THE PROJECT ROOT.*
#
# To use this script, you'll need to create a <project-dir>/.autograph_config file
# with:
# ```
# AUTH_TOKEN=<autograph-auth-token>
# SERVER=<autograph-server-url-prod-or-staging>
# ```
#
# To get these credentials, contact the Cloud Services team.
#
# This script is based on the official Autograph documentation:
# https://mozilla.github.io/application-services/docs/applications/signing-android-apps.html
#
# For CI use, the documentation also links to a production TaskCluster configuration.
#
# If you're not creating a release but want to create a release build for local testing, you
# can append the `--test` flag to ignore some release checks.

BUILD_TOOLS=~/Library/Android/sdk/build-tools/28.0.2 # Update me on error!

BUILD_DIR=app/build/outputs/apk/amazonWebview/release
FINAL_NAME=app-amazonWebview-release.apk

# Assert pre-conditions.
source .autograph_config || exit 1
if [[ -z $AUTH_TOKEN ]]; then
    echo "Error: AUTH_TOKEN not defined in .autograph_config"
    exit 1
fi
if [[ -z $SERVER ]]; then
    echo "Error: SERVER not defined in .autograph_config"
    exit 1
fi
if [ ! -d $BUILD_TOOLS ]; then
    echo "Error: Build tools not found; update BUILD_TOOLS variable in script to continue."
    exit 1
fi

# Assert pre-conditions, if test flag is not specified.
if [[ $1 = "--test" ]]; then
    echo "--test specified: DISABLING RELEASE CHECKS"
    ASSEMBLE_FLAGS=-PnoValidate
else
    if [ ! -f .sentry_dsn_release ]; then
        echo "Error: expected <project-dir>/.sentry_dsn_release for Sentry key"
        exit 1
    fi

    GRADLE_VERSION=`cat app/build.gradle | grep versionName | grep --only-matching "[0-9.]\+"`
    echo "Building release v$GRADLE_VERSION."

    # Ensure the tests are passing.
    ./quality/pre-push-recommended.sh || exit 1
fi

# Build the release build.
./gradlew --quiet clean assembleAmazonWebviewRelease $ASSEMBLE_FLAGS || exit 1

# Sign via autograph. Signing can be found here:
# https://github.com/mozilla-services/autograph/blob/a1bee1add785ae41a284b1d5873010817d1fa79f/signer/apk/jar.go#L69-L71
curl -F "input=@$BUILD_DIR/app-amazonWebview-release-unsigned.apk" \
    -o $BUILD_DIR/$FINAL_NAME \
    -H "Authorization: $AUTH_TOKEN" \
    $SERVER || exit 1

# Align ZIP to 4 byte addresses
zipalign -v 4 $BUILD_DIR/$FINAL_NAME $BUILD_DIR/app-amazonWebview-release-aligned.apk
mv -f $BUILD_DIR/app-amazonWebview-release-aligned.apk $BUILD_DIR/$FINAL_NAME


# We don't use `-Werr` (treat warnings as errors) because errors related to certain AndroidX
# files are expected here.
#
# When the build is signed by Autograph, files inside the META-INF folder are not signed (due to
# an outdated dependency on their end). We previously stripped these files out of the APK to
# avoid warnings, but after migrating to AndroidX important files are now stored in this
# directory (they appear to be Jetified libraries, but we haven't verified that). As we can't
# strip these files out, we stop failing on warnings instead.
#
# Verify.
$BUILD_TOOLS/apksigner verify $BUILD_DIR/$FINAL_NAME || exit 1

echo "Build and sign successful. Opening build directory... Look for $FINAL_NAME"

# Open build dir on macOS.
open $BUILD_DIR
