/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.architecture

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.mozilla.focus.ext.serviceLocator

/**
 * Returns an instance of the view model for the given application building block (fragment, activity)
 * for the given lifecycle. This is a wrapper around the framework's [ViewModelProvider] using the application's
 * [ViewModelFactory] to reduce code duplication.
 */
object FirefoxViewModelProviders {

    fun of(activity: FragmentActivity): ViewModelProvider {
        val viewModelFactory = ViewModelFactory(activity.serviceLocator)
        return ViewModelProviders.of(activity, viewModelFactory)
    }

    fun of(fragment: Fragment): ViewModelProvider {
        // If we're attempting to retrieve a view model, we should be attached to a context already.
        val viewModelFactory = ViewModelFactory(fragment.context!!.serviceLocator)
        return ViewModelProviders.of(fragment, viewModelFactory)
    }
}
