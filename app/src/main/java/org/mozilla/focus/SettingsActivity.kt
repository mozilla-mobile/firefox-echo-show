/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceScreen
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import org.mozilla.focus.iwebview.IWebView
import org.mozilla.focus.iwebview.WebViewProvider

/**
 * Settings activity with nested settings screens.
 *
 * We use AppCompatActivity and use fragments within the PreferenceScreens.
 *
 * We have to override some methods in PreferenceFragmentCompat in order to handle PreferenceScreen
 * navigation.
 */
class SettingsActivity : AppCompatActivity() {
    companion object {
        const val FRAGMENT_TAG = "settingsFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.menu_settings)
        }

        supportFragmentManager.addOnBackStackChangedListener {
            supportFragmentManager.apply {
                val count = backStackEntryCount
                val title = if (count > 0) {
                    getBackStackEntryAt(count - 1).breadCrumbTitle
                } else {
                    // If there's no backstack, we're an the main settings screen.
                    getString(R.string.menu_settings)
                }
                supportActionBar?.title = title
            }
        }

        supportFragmentManager.beginTransaction()
                .add(R.id.settings_container, SettingsFragment(), FRAGMENT_TAG)
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return if (name == IWebView::class.java.name) {
            // Inject our implementation of IWebView from the WebViewProvider.
            val webview = WebViewProvider.create(this, attrs)
            (webview as IWebView).setBlockingEnabled(false)
            return webview
        } else super.onCreateView(name, context, attrs)
    }

    class SettingsFragment : PreferenceFragmentCompat(),
            PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
            PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.settings)
        }

        override fun onPreferenceStartScreen(preferenceFragmentCompat: PreferenceFragmentCompat,
                                             preferenceScreen: PreferenceScreen): Boolean {
            (activity as AppCompatActivity).supportActionBar?.title = preferenceScreen.title
            preferenceFragmentCompat.preferenceScreen = preferenceScreen
            return true
        }

        /*
         * Handle launching Fragments from the PreferenceScreens.
         */
        override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat?, pref: Preference?): Boolean {
            val fragment = Fragment.instantiate(context, pref?.fragment, pref?.extras)
            (activity as AppCompatActivity).apply {
                supportFragmentManager.beginTransaction()
                        .setBreadCrumbTitle(pref?.title)
                        .replace(R.id.settings_container, fragment, FRAGMENT_TAG)
                        .addToBackStack(null)
                        .commit()
                supportActionBar?.title = pref?.title
            }
            return true
        }

        override fun getCallbackFragment(): Fragment {
            return this
        }
    }
}
