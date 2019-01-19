/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import org.mozilla.focus.architecture.FrameworkRepo
import org.mozilla.focus.ext.getAccessibilityManager

/**
 * Implementation of the Service Locator pattern. Use this class to provide dependencies without
 * making client code aware of their specific implementations (i.e., make it easier to program to
 * an interface).
 *
 * This also makes it easier to mock out dependencies during testing.
 *
 * See: https://en.wikipedia.org/wiki/Service_locator_pattern
 *
 * ### Dependencies can be defined as follows:
 *
 *   #### Lazy, app-wide Singleton:
 *   ```
 *   open val pocket by lazy { Pocket() }
 *   ```
 *
 *   #### Eager, app-wide singleton:
 *   ```
 *   open val pocket = Pocket()
 *   ```
 *
 *   #### New value each time:
 *   ```
 *   open val pocket: Pocket get() = Pocket()
 *   ```
 *
 *   #### Concrete value for interface:
 *   ```
 *   open val telemetry: TelemetryInterface by lazy { SentryWrapper() }
 *   ```
 */
open class ServiceLocator {

    val frameworkRepo = FrameworkRepo()

    fun init(applicationContext: Context) {
        // The touch state listener gets called even when the application is backgrounded so we only need to add it once.
        frameworkRepo.init(applicationContext.getAccessibilityManager())
    }
}
