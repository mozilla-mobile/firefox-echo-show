/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.os.Build

/**
 * Helpers that help identify a device.
 */
class DeviceInfo {

    /** Echo Show models. */
    enum class Model {
        FIVE,
        SECOND_GEN,
        FIRST_GEN,

        UNKNOWN
    }

    /**
     * The model name of the current device. In general, you should use the resource system over
     * this to more robustly identify device configurations rather than specific devices: see
     * https://github.com/mozilla-mobile/firefox-echo-show/blob/master/docs/device_reference.md#distinguishing-devices-in-code
     * for details.
     */
    val deviceModel: Model by lazy {
        when (Build.MODEL) {
            "AEOCH" -> Model.FIVE
            "AEOBP" -> Model.SECOND_GEN
            "AEOKN" -> Model.FIRST_GEN

            else -> Model.UNKNOWN
        }
    }
}
