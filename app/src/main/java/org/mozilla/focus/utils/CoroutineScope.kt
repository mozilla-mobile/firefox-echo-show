/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineScope] that is scoped to an Activity's lifecycle and launches jobs on the main thread.
 *
 * Callers must call [init] to register the scope to the Activity.
 */
class ActivityUiCoroutineScope : LifecycleObserver, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() {
            if (!wasInitCalled) throw IllegalStateException("Expected init to be called before accessing context")
            return Dispatchers.Main + lifecycleCancelJob
        }

    private var wasInitCalled = false
    private val lifecycleCancelJob = Job()

    fun init(lifecycle: Lifecycle) {
        wasInitCalled = true
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        lifecycleCancelJob.cancel()
    }
}

/**
 * A [CoroutineScope] that is scoped to a Fragment's view lifecycle and launches jobs on the main thread.
 *
 * Callers must explicitly call [onCreateView] and [onDestroyView] to attach the lifecycle:
 * unfortunately, Android's [android.arch.lifecycle.LifecycleObserver] does not forward fragment
 * view lifecycle events.
 */
class FragmentViewUiCoroutineScope : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + viewLifecycleCancelJob!!

    private var viewLifecycleCancelJob: Job? = null

    fun onCreateView() {
        if (viewLifecycleCancelJob != null) {
            throw IllegalStateException("onCreateView unexpectedly called twice before onDestroyView")
        }

        viewLifecycleCancelJob = Job()
    }

    fun onDestroyView() {
        // To reduce exceptions thrown from lifecycle errors, we don't null the job.
        // If a job is created around this cancelled job, it will just fail to execute.
        viewLifecycleCancelJob!!.cancel()
    }
}
