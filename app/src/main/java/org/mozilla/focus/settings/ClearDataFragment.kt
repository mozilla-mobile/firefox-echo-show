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
import org.mozilla.focus.telemetry.TelemetryWrapper

private val userClearDataEventLiveData = MutableLiveData<UserClearDataEvent>()
private fun sendUserClearDataEvent() { userClearDataEventLiveData.value = UserClearDataEvent() }

/**
 * An event for when the user clears their browsing data. To listen for the event, observe
 * on [liveData]: a new instance will be set each time the event occurs.
 */
class UserClearDataEvent {
    companion object {
        val liveData: LiveData<UserClearDataEvent> = userClearDataEventLiveData
    }
}

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
                settingsWebView.cleanup() // This will only clear global WebView settings.
                sendUserClearDataEvent()
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
