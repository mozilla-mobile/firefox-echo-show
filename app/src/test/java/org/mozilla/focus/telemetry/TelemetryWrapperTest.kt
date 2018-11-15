/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryWrapperTest {

    @Test
    fun `WHEN ExtraValue fromBoolean is passed true, it returns the true string`() {
        assertEquals("true", TelemetryWrapper.ExtraValue.fromBoolean(true))
    }

    @Test
    fun `WHEN ExtraValue fromBoolean is passed false, it returns the false string`() {
        assertEquals("false", TelemetryWrapper.ExtraValue.fromBoolean(false))
    }
}
