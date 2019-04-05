/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.mozilla.focus.utils.ActivityUiCoroutineScope.Companion.getAndInit
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineScope] that is scoped to an Activity's lifecycle and launches jobs on the main thread.
 *
 * Get a new, initialized instance with [getAndInit].
 */
class ActivityUiCoroutineScope @VisibleForTesting(otherwise = PRIVATE) constructor() : LifecycleObserver, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + lifecycleCancelJob

    private val lifecycleCancelJob = Job()

    fun init(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        lifecycleCancelJob.cancel()
    }

    companion object {
        fun getAndInit(lifecycle: Lifecycle): ActivityUiCoroutineScope {
            return ActivityUiCoroutineScope().apply {
                init(lifecycle)
            }
        }
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
