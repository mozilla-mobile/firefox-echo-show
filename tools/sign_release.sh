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
else
    if [ ! -f .sentry_dsn_release ]; then
        echo "Error: expected <project-dir>/.sentry_dsn_release for Sentry key"
        exit 1
    fi

    # via https://unix.stackexchange.com/a/155077
    if output=$(git status --porcelain) && [ -n "$output" ]; then
        echo "Error: uncommited git changes: exiting."
        exit 1
    fi

    # `git name-rev` sample output: HEAD tags/v1.0.0.5
    # When a commit without a tag is checked out, "tags/..."" will be replaced with "undefined".
    IS_ON_GIT_TAG=`git name-rev --tags HEAD | grep --only-matching --invert-match undefined`
    if [[ -z $IS_ON_GIT_TAG ]]; then
        echo "Error: currently checked out commit does not have a tag (as releases should)."
        exit 1
    fi

    GIT_TAG_VERSION=`git name-rev --tags HEAD | grep -o "v[0-9.]\+$" | grep -o "[0-9.]\+$"`
    GRADLE_VERSION=`cat app/build.gradle | grep versionName | grep --only-matching "[0-9.]\+"`
    if [[ $GIT_TAG_VERSION != $GRADLE_VERSION ]]; then
        echo "Error: git tag version - $GIT_TAG_VERSION - does not match gradle version - $GRADLE_VERSION."
        echo "    Did you increment the build version?"
        exit 1
    fi

    echo "Building release v$GRADLE_VERSION."

    # Ensure the tests are passing.
    ./quality/pre-push-recommended.sh || exit 1
fi

# Build the release build.
./gradlew --quiet clean assembleAmazonWebviewRelease || exit 1

# Strip unsigned content from the JAR. When running `apksigner verify`, most files in META-INF
# will give warnings that they're not protected by the signature. I could not figure out what
# these files are intended to do but removing them does not seem to affect the app. StackOverflow
# suggests removing the files entirely: https://stackoverflow.com/a/47394901 However, MANIFEST.MF,
# "The manifest file that is used to define extension and package related data." is protected by
# the signature so instead we remove everything else. More on MANIFEST.MF in JAR format:
# https://docs.oracle.com/javase/7/docs/technotes/guides/jar/jar.html
#
# This problem may not exist with APK signature scheme v2 but we use v1 (JAR signing) on Autograph.
zip --quiet --delete $BUILD_DIR/app-amazonWebview-release-unsigned.apk \
    'META-INF/*kotlin_module' \
    'META-INF/*version' \
    'META-INF/proguard/*' \
    'META-INF/services/*' \
    'META-INF/web-fragment.xml' || exit 1

# Align and sign via autograph. Signing can be found here:
# https://github.com/mozilla-services/autograph/blob/a1bee1add785ae41a284b1d5873010817d1fa79f/signer/apk/jar.go#L69-L71
curl -F "input=@$BUILD_DIR/app-amazonWebview-release-unsigned.apk" \
    -o $BUILD_DIR/app-amazonWebview-release.apk \
    -H "Authorization: $AUTH_TOKEN" \
    $SERVER || exit 1

# Verify.
$BUILD_TOOLS/apksigner verify -Werr $BUILD_DIR/$FINAL_NAME || exit 1

echo "Build and sign successful. Opening build directory... Look for $FINAL_NAME"

# Open build dir on macOS.
open $BUILD_DIR
