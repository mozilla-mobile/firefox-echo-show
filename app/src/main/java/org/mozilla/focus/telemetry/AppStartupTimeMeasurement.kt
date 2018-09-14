/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry

import android.os.SystemClock

/**
 * Encapsulates measurement of app startup times.
 */
object AppStartupTimeMeasurement {
    private var appStartMillis = -1L
    private var appStartEventSent = false

    @JvmStatic
    fun applicationOnCreate() {
        appStartMillis = SystemClock.uptimeMillis()
    }

    // Fragment.onResume gets called after Activity.onResume.
    fun fragmentOnResume() {
        if (appStartEventSent) { return }
        val startupMillis = SystemClock.uptimeMillis() - appStartMillis
        TelemetryWrapper.startupCompleteEvent(startupMillis)
        appStartEventSent = true
    }
}
