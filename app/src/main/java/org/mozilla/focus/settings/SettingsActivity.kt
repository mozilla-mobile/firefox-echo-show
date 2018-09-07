/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceScreen
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_settings.*
import org.mozilla.focus.R
import org.mozilla.focus.SearchBus
import org.mozilla.focus.ext.children
import org.mozilla.focus.iwebview.IWebView
import org.mozilla.focus.iwebview.WebViewProvider
import org.mozilla.focus.widget.INTENT_ORIGIN
import org.mozilla.focus.widget.INTERNAL

/**
 * Settings activity with nested settings screens.
 *
 * We use AppCompatActivity and use fragments within the PreferenceScreens.
 * The activity must implement some PreferenceFragmentCompat interfaces in
 * order to handle nested PreferenceScreen navigation.
 */
class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
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

        styleActionBar()
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

    override fun startActivity(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW &&
                intent.getStringExtra(INTENT_ORIGIN) == INTERNAL) {
            intent.data?.also { SearchBus.push(it) }
            // If the request came from inside the app, close this activity
            // and forward the request to where it can be handled
            finish()
        } else {
            super.startActivity(intent)
        }
    }

    /*
     * Handle showing a nested PreferenceScreen.
     */
    override fun onPreferenceStartScreen(preferenceFragmentCompat: PreferenceFragmentCompat,
                                         preferenceScreen: PreferenceScreen): Boolean {
        supportActionBar?.title = preferenceScreen.title
        preferenceFragmentCompat.preferenceScreen = preferenceScreen
        return true
    }

    /*
     * Handle launching Fragments from the PreferenceScreens.
     */
    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat?, pref: Preference?): Boolean {
        val fragment = Fragment.instantiate(this, pref?.fragment, pref?.extras)
        supportFragmentManager.beginTransaction()
                .setBreadCrumbTitle(pref?.title)
                .replace(R.id.settings_container, fragment, FRAGMENT_TAG)
                .addToBackStack(null)
                .commit()
        supportActionBar?.title = pref?.title
        return true
    }

    /*
     * We don't use a static newInstance() call to create this fragment because it takes no
     * arguments. If that changes, we'll need to do that.
     */
    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.settings)
        }
    }

    /**
     * We were unable to find a way to apply these styles to the ActionBar
     * through XML without providing an entirely custom layout.  These
     * styles are necessary to match the native settings page styling
     */
    private fun styleActionBar() {
        this.settings_container.rootView
                .findViewById<Toolbar>(R.id.action_bar)
                .children()
                .forEach { toolbarChild ->
                    val layoutParams = toolbarChild.layoutParams as? Toolbar.LayoutParams ?: return
                    toolbarChild.layoutParams = layoutParams.apply {
                        gravity = Gravity.CENTER_VERTICAL
                        marginEnd = 40
                    }
                }
    }
}
