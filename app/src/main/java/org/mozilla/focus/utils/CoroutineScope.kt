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

    /**
     * Returns the coroutine context scoped to the Fragment's view lifecycle.
     *
     * @throws KotlinNullPointerException if accessed before [onCreateView] is ever called (a UI job should never start
     * before the view hierarchy is created).
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + viewLifecycleCancelJob!!

    private var viewLifecycleCancelJob: Job? = null

    fun onCreateView() {
        if (viewLifecycleCancelJob?.isCancelled == false) {
            throw IllegalStateException("onCreateView unexpectedly called twice before onDestroyView")
        }

        viewLifecycleCancelJob = Job()
    }

    fun onDestroyView() {
        // A user of this class may try to create a job on the UI thread after onDestroyView is called (e.g.
        // a background coroutine finishes and tries to execute on the UI thread). In these cases, we don't
        // want the application to throw so we don't null this job: instead the caller will get the cancelled
        // job and their job will fail to execute.
        viewLifecycleCancelJob!!.cancel()
    }
}
