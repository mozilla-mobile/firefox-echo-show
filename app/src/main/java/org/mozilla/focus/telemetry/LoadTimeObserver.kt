/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry

import android.os.SystemClock
import android.support.v4.app.Fragment
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.Log.Priority
import org.mozilla.focus.architecture.NonNullObserver
import org.mozilla.focus.session.Session
import org.mozilla.focus.utils.UrlUtils

private const val LOG_TAG: String = "LoadTimeObserver"

object LoadTimeObserver {
    private const val MIN_LOAD_TIME: Long = 40
    private const val MAX_PROGRESS = 99

    @JvmStatic
    fun addObservers(session: Session, fragment: Fragment) {
        var startLoadTime: Long = 0
        var urlLoading: String? = null

        session.loading.observe(fragment, object : NonNullObserver<Boolean>() {
            public override fun onValueChanged(value: Boolean) {
                if (value) {
                    if ((urlLoading != null && urlLoading != session.url.value) || urlLoading == null) {
                        urlLoading = session.url.value
                        startLoadTime = SystemClock.elapsedRealtime()
                        log("zerdatime $startLoadTime - page load $urlLoading start")
                    }
                } else {
                    // Progress of 99 means the page completed loading and wasn't interrupted.
                    if (urlLoading != null &&
                            session.url.value == urlLoading &&
                            session.progress.value == MAX_PROGRESS) {
                        log("Loaded page")
                        val endTime = SystemClock.elapsedRealtime()
                        log("zerdatime $endTime - page load stop")
                        val elapsedLoad = endTime - startLoadTime
                        log("$elapsedLoad - elapsed load")
                        // Even internal pages take longer than 40 ms to load, let's not send any loads faster than this
                        if (elapsedLoad > MIN_LOAD_TIME && !UrlUtils.isLocalizedContent(urlLoading)) {
                            log("Sent load to histogram")
                            TelemetryWrapper.addLoadToHistogram(elapsedLoad)
                        }
                    }
                }
            }
        })
        session.url.observe(fragment, object : NonNullObserver<String>() {
            public override fun onValueChanged(value: String) {
                if ((urlLoading != null && urlLoading != value) || urlLoading == null) {
                    startLoadTime = SystemClock.elapsedRealtime()
                    log("zerdatime $startLoadTime - url changed, new page load start")
                    urlLoading = value
                }
            }
        })
    }
}

private fun log(msg: String) = Log.log(priority = Priority.INFO, tag = LOG_TAG, message = msg)
