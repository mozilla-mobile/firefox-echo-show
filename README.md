# Firefox

## Build instructions
1. Clone the repository:

2. Import the project into Android Studio or build on the command line:

  ```shell
  ./gradlew clean app:assembleAmazonWebviewDebug
  ```
3. Make sure to select the right build variant in Android Studio: **amazonWebviewDebug**

### Running
You can run on a TV emulator. Also try a 7-in landscape tablet.

And then install via Android Studio or adb. Only a single development device
can be connected to a Fire TV at a time. Note that while you can install on an
Android TV emulator, the behavior is different from Fire TV's and should not be
relied upon.

### Pre-push hooks
Since we don't have CI, if you're pushing code, please add a pre-push hook. To use the
recommended hook, run this command from the project root:
```sh
ln -s ../../quality/pre-push-recommended.sh .git/hooks/pre-push
```

To push without running the pre-push hook (e.g. doc updates):
```sh
git push <remote> --no-verify
```

## License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/
