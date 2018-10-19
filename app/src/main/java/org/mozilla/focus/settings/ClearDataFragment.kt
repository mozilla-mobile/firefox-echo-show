/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.settings

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_cleardata.*
import org.mozilla.focus.R
import org.mozilla.focus.iwebview.WebViewProvider
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.LiveDataEvent

/**
 * An event for when the user clears their browsing data. To listen for the event, observe
 * on [liveData]: a new instance will be set each time the event occurs.
 *
 * This class wraps the file-private mutable LiveData instance in a read-only interface exposed
 * outside of this file.
 */
object UserClearDataEvent { val liveData: LiveData<LiveDataEvent> = mutableClearEventLiveData }
private val mutableClearEventLiveData = MutableLiveData<LiveDataEvent>()

// Wrap the LiveData assignment in a function to explain what it does.
private fun sendUserClearDataEvent() { mutableClearEventLiveData.value = LiveDataEvent() }

/**
 * Fragment used in Settings to clear cookies and browsing history.
 */
class ClearDataFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_cleardata, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        confirmButton.apply {
            isSelected = true
            setOnClickListener {
                WebViewProvider.deleteGlobalData(view.context)
                sendUserClearDataEvent() // to delete WebView instance data.
                TelemetryWrapper.clearDataEvent()
                finishFragment()
            }
        }
        cancelButton.setOnClickListener {
            finishFragment()
        }
    }

    private fun finishFragment() {
        activity?.supportFragmentManager?.popBackStack()
    }
}
