/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helpers

/**
 * A collection of local assertions.
 */
object Assert {

    /**
     * Fails the test with an error message appropriate for when an unexpected branch of an
     * Either is taken.
     */
    fun <T : Any> failUnexpectedEitherBranch(value: T): Nothing {
        throw IllegalStateException("""Unexpected Either branch taken. Received:
            |    type: ${value::class.java.simpleName}
            |    value: $value
            """.trimMargin())
    }
}
