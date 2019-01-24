/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

/**
 * Simple delegate that simplifies working with [WeakReference]s
 */
class WeakReferenceDelegate<T> {

    private var reference: WeakReference<T>? = null

    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T? {
        return reference?.get()
    }

    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: T?) {
        reference = if (value != null) {
            WeakReference(value)
        } else {
            null
        }
    }
}
