/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.view.ScaleGestureDetector
import android.view.View
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.focus.browser.FullscreenCallbacks.ExitFullscreenOnScaleGestureListener
import org.mozilla.focus.iwebview.IWebView
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FullscreenCallbacksTest {

    @Test
    fun `WHEN the scale gesture listener receives scale down events THEN exit fullscreen is called`() {
        with(ExitFullscreenOnScaleGestureMocks.create()) {
            `when`(detector.scaleFactor).thenReturn(0.95f)
            runOnScaleBeginToEnd(listener, detector)
            verify(callback, times(1)).fullScreenExited()
        }
    }

    @Test
    fun `WHEN the scale gesture listener receives scale up events THEN exit fullscreen is not called`() {
        with(ExitFullscreenOnScaleGestureMocks.create()) {
            `when`(detector.scaleFactor).thenReturn(1.05f)
            runOnScaleBeginToEnd(listener, detector)
            verify(callback, never()).fullScreenExited()
        }
    }

    @Test
    fun `WHEN the scale gesture listener receives 1f scale events THEN exit fullscreen is not called`() {
        with(ExitFullscreenOnScaleGestureMocks.create()) {
            `when`(detector.scaleFactor).thenReturn(1f)
            runOnScaleBeginToEnd(listener, detector)
            verify(callback, never()).fullScreenExited()
        }
    }

    @Test
    fun `WHEN the scale gesture starts scaling but receives no onScale events THEN exit fullscreen is not called`() {
        with(ExitFullscreenOnScaleGestureMocks.create()) {
            assertTrue("Test assumes onScaleBegin returns true to accept additional events", listener.onScaleBegin(detector))
            listener.onScaleEnd(detector)
            verify(callback, never()).fullScreenExited()
        }
    }

    @Test
    fun `WHEN the scale gesture listener receives no events, exit fullscreen is not called`() {
        val callback = mock(IWebView.FullscreenCallback::class.java)
        ExitFullscreenOnScaleGestureListener(callback, View(RuntimeEnvironment.systemContext))
        verify(callback, never()).fullScreenExited()
    }

    private fun runOnScaleBeginToEnd(listener: ScaleGestureDetector.OnScaleGestureListener, detector: ScaleGestureDetector) {
        assertTrue("Test assumes onScaleBegin returns true to accept additional events", listener.onScaleBegin(detector))
        repeat(6) {
            assertTrue("Test assumes onScale returns true to handle event; i=$it", listener.onScale(detector))
        }
        listener.onScaleEnd(detector)
    }

    private class ExitFullscreenOnScaleGestureMocks(
            val listener: ExitFullscreenOnScaleGestureListener,
            val detector: ScaleGestureDetector,
            val callback: IWebView.FullscreenCallback
    ) {

        companion object {
            fun create(): ExitFullscreenOnScaleGestureMocks {
                val callback = mock(IWebView.FullscreenCallback::class.java)
                val listener = ExitFullscreenOnScaleGestureListener(callback, View(RuntimeEnvironment.systemContext))
                val detector = mock(ScaleGestureDetector::class.java)
                return ExitFullscreenOnScaleGestureMocks(listener, detector, callback)
            }
        }
    }
}
