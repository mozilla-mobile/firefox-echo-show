# Echo Show device reference
    - Fire OS 5: Based on Android 5.1 (Lollipop, API level 22)
    - Fire OS 6: Based on Android 7.1 (Nougat, API level 25)

There are currently two devices:

| Gen | Release | Code  | OS | Size | Resources | DPI  |
|:---:|:-------:|:-----:|:--:|:----:|:---------:|:----:|
| 2nd | 2018    | AEOBP | 5  | 10.1"| xlarge    | mdpi |
| 1st | 2017    | AEOKN | 5  | 7"   | large     | mdpi |

While behavior is not guaranteed to be the same, you can use the `WXGA 10.1` Android emulator to mimic the when developing.

### Distinguishing devices
Within Android resources, we use the screen size metric to distinguish devices: no qualifiers selects a 1st generation device and `xlarge` overrides the no-qualifier resources for a 2nd generation device. We use screen size because it's simple and less prescriptive compared to other resource metrics like `sw*dp` and `w*dp`.
