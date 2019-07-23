# Echo Show device reference
    - Fire OS 5: Based on Android 5.1 (Lollipop, API level 22)
    - Fire OS 6: Based on Android 7.1 (Nougat, API level 25)

| Series | Release | Code  | OS | Size  | Resources | DPI  | Emulator  | Resolution | Available Height* |
|:------:|:-------:|:-----:|:--:|:-----:|:---------:|:----:|:---------:|:----------:|:------------------|
| 5      | 2019    | AEOCH | ?  | ?     | large     | ?    | ?         | 960x480    | 456dp
| 2      | 2018    | AEOBP | 5  | 10.1" | xlarge    | mdpi | WXGA 10.1 | 1280x800   | 770dp
| 1      | 2017    | AEOKN | 5  | 7"    | large     | mdpi | ?         | 1024x600   | 570dp

We recommend **developing** on Android tablet emulators for developer efficiency: the recommended Android emulator images are mentioned above.

However, we ultimately recommend **verifying behavior** on device: the behavior on device differs from the emulator. Important note: **locally built apps cannot be installed on production Echo Show devices** so if you do not have a developer device, use the emulators.

* Note that available height does not match actual height.  Available height is the device's actual resolution minus space for the toolbar and similar "screen decorations". You can retrieve the available height values by querying resources.configuration.screenHeightDp on device. See [documentation][res height] for details.

### Known differences between Echo Show devices and Android tablet emulators
From a user experience perspective, we've noticed the following differences:
- Device notifications appear differently (we don't think there are any on Echo Show)
- Swiping from the top of the Echo Show screen will reveal a navigation bar including settings: this is different from the android settings and notification tray
- The bottom navigation view (back, home, recent apps) does not exist on the Echo Show
- The Echo Show supports some voice commands with Firefox such as, "Alexa, open Firefox" and emulators do not
- The Echo Show's physical screen has different pixel densities than the Android emulator, making the images look stretched in different ways

From an implementation perspective, we've noticed the following differences:
- `onStop` is not called when the device times out ([#172](https://github.com/mozilla-mobile/firefox-echo-show/issues/172))
- If more than one WebView is created on the Echo Show device, the app will crash unlike Android

## Distinguishing devices in code
We distinguish devices using [the screen height metric][res height] from the Android resources system:
- All devices will use resources with no qualifiers (e.g. `layout`)
- Devices will use the largest qualified file that is smaller than or equal to their `available height` 

See the table above to see which screen size metric each device uses.

We previously used screen size, but were forced to change because the series 5 shares the same bucket as the series 1, despite being substantially smaller.

[res height]: https://developer.android.com/guide/topics/resources/providing-resources#ScreenHeightQualifier
