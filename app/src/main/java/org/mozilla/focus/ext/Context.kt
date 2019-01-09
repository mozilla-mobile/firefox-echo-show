/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.content.Context
import android.view.accessibility.AccessibilityManager
import org.mozilla.focus.FocusApplication
import org.mozilla.focus.widget.ServiceLocator

// Extension functions for the Context class

val Context.serviceLocator: ServiceLocator get() = (applicationContext as FocusApplication).serviceLocator

/**
 * The (visible) version name of the application, as specified by the <manifest> tag's versionName
 * attribute. E.g. "2.0".
 */
val Context.appVersionName: String?
    get() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return packageInfo.versionName
    }

fun Context.getAccessibilityManager() = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

// AccessibilityManager.isAccessibilityEnabled can be enabled for more things than just VoiceView. If we want
// VoiceView, we only need to check this one field; via comments on https://stackoverflow.com/a/12362545/582004.
fun Context.isVoiceViewEnabled() = getAccessibilityManager().isTouchExplorationEnabled
