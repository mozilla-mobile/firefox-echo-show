/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.support.v4.app.Fragment

val Fragment?.isVisibleAndNonNull: Boolean
    get() = this != null && this.isVisible
