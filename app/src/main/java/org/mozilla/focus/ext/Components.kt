/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import mozilla.components.concept.toolbar.Toolbar

// This may get added in the components: https://github.com/mozilla-mobile/android-components/issues/264
fun Toolbar.ActionToggleButton.setSelected(willBeSelected: Boolean) {
    if (isSelected() != willBeSelected) { toggle() }
}
