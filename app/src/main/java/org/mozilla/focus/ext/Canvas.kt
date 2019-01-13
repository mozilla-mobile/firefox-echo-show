/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.graphics.Canvas

/**
 * Saves the canvas state, executes the function, and restores the canvas state.
 *
 * TODO: replace with KTX.
 */
fun Canvas.withSave(functionBlock: Canvas.() -> Unit) {
    save()
    functionBlock()
    restore()
}
