/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.support.v4.app.FragmentManager
import org.mozilla.focus.browser.BrowserFragment
import org.mozilla.focus.home.NavigationOverlayFragment

fun FragmentManager?.getBrowserFragment(): BrowserFragment? =
        this?.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as BrowserFragment?

fun FragmentManager?.getNavigationOverlay(): NavigationOverlayFragment? =
        this?.findFragmentByTag(NavigationOverlayFragment.FRAGMENT_TAG) as NavigationOverlayFragment?
