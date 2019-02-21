/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.MutableLiveData
import android.support.design.widget.AppBarLayout
import android.support.v7.app.AppCompatActivity
import mozilla.components.browser.toolbar.BrowserToolbar
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserAppBarLayoutControllerTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule() // necessary for LiveData tests

    private lateinit var initController: BrowserAppBarLayoutController

    private lateinit var viewModel: BrowserAppBarViewModel
    private lateinit var isAppBarHidden: MutableLiveData<Boolean>

    private lateinit var appBarLayout: AppBarLayout

    @Before
    fun setUp() {
        // LiveData observers won't emit events unless the lifecycle is in the started state: we use a dummy
        // Activity in the started state for this purpose.
        val lifecycleOwner: LifecycleOwner = Robolectric.buildActivity(AppCompatActivity::class.java)
            .create()
            .start()
            .get()

        isAppBarHidden = MutableLiveData()
        viewModel = mock(BrowserAppBarViewModel::class.java).also {
            `when`(it.isAppBarHidden).thenReturn(isAppBarHidden)
            `when`(it.isToolbarScrollEnabled).thenReturn(MutableLiveData())
        }

        appBarLayout = mock(AppBarLayout::class.java)
        val toolbar = mock(BrowserToolbar::class.java)
        initController = BrowserAppBarLayoutController(viewModel, appBarLayout, toolbar).apply {
            init(lifecycleOwner)
        }
    }

    @Test
    fun `WHEN the app bar visibility is updated in the model THEN the toolbar never animates`() {
        isAppBarHidden.value = true
        verify(appBarLayout, times(1)).setExpanded(anyBoolean(), eq(false))
        isAppBarHidden.value = false
        verify(appBarLayout, times(2)).setExpanded(anyBoolean(), eq(false))
    }

    @Test
    fun `WHEN the app bar visibility is updated in the model THEN the UI does the same`() {
        isAppBarHidden.value = true
        verify(appBarLayout, times(1)).setExpanded(eq(true), anyBoolean())
        isAppBarHidden.value = false
        verify(appBarLayout, times(1)).setExpanded(eq(false), anyBoolean())
    }
}
