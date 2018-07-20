/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_cleardata.*
import org.mozilla.focus.R
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.telemetry.TelemetryWrapper

class ClearDataFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_cleardata, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        confirmButton.apply {
            isSelected = true
            setOnClickListener {
                settingsWebView.cleanup()
                SessionManager.getInstance().removeAllSessions()
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
