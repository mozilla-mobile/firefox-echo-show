/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

/**
 * Allows assisted dependency injection.
 *
 * An object can be injected with a Provider<T> instead of a value of T.  This
 * is particularly useful in allowing us to inject values through constructors
 * that normally would not yet be available during construction, but will only
 * need to be used lazily.
 */
class Provider<T> {
    private var _value: T? = null

    fun getValue(): T? {
        return _value
    }

    fun setValue(value: T?) {
        _value = value
    }
}
