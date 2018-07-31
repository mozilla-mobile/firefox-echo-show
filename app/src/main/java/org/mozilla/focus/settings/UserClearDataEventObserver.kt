/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.settings

import android.arch.lifecycle.Observer
import android.support.v7.app.AppCompatActivity
import org.mozilla.focus.ScreenController
import org.mozilla.focus.utils.LiveDataEvent

/** An observer for when the user clears their browsing data. See [UserClearDataEvent] for details. */
class UserClearDataEventObserver(private val activity: AppCompatActivity) : Observer<LiveDataEvent> {
    override fun onChanged(event: LiveDataEvent?) {
        // The global WebView state has been destroyed by Settings but we must still clean up
        // local WebView state. The browserFragment's initial state is that there is 1) no page
        // loaded and 2) there is no back stack. However, this is impossible to replicate in an
        // existing BrowserFragment: loading a blank page will add a page to the back stack. As
        // such, our only option is to remove and recreate the BrowserFragment. This is more correct
        // because it creates a new session too.
        ScreenController.recreateBrowserScreen(activity.supportFragmentManager)
    }
}
