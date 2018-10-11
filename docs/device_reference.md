# Echo Show device reference
    - Fire OS 5: Based on Android 5.1 (Lollipop, API level 22)
    - Fire OS 6: Based on Android 7.1 (Nougat, API level 25)

There are currently two devices:

| Gen | Release | Code  | OS | Size | Resources | DPI  |
|:---:|:-------:|:-----:|:--:|:----:|:---------:|:----:|
| 2nd | 2018    | AEOBP | 5  | 10.1"| xlarge    | mdpi |
| 1st | 2017    | AEOKN | 5  | 7"   | large     | mdpi |

The behavior between running code on device and running code in an Android emulator is different (see below). However, locally built apps **cannot be installed on production Echo Show devices** so if you don't have access to a developer device, the best way to develop is on the `WXGA 10.1` Android emulator, which has a very similar screen to the 2nd generation Echo Show. We haven't identified a good emulator to replicate the 7" 1st gen device yet.

### Distinguishing devices in code
Within Android resources, we use the screen size metric to distinguish devices: no qualifiers selects a 1st generation device and `xlarge` overrides the no-qualifier resources for a 2nd generation device. We use screen size because it's simple and less prescriptive compared to other resource metrics like `sw*dp` and `w*dp`.

### Things to note when using an Android emulator
- Device notifications appear differently (if there are any at all on the Echo Show)
- Swiping from the top of the screen will reveal a navigation bar including settings: this is different from the android settings and notification tray
- The bottom navigation (back, home, recent apps) does not exist on the Echo Show
- The Echo Show supports some voice commands with Firefox such as, "Alexa, open Firefox" and emulators do not
