/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.settings

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceScreen
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_settings.*
import org.mozilla.focus.R
import org.mozilla.focus.browser.Browser
import org.mozilla.focus.browser.URLs.APP_ABOUT
import org.mozilla.focus.browser.URLs.PRIVACY_NOTICE
import org.mozilla.focus.ext.children

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

    /*
     * Handle showing a nested PreferenceScreen.
     */
    override fun onPreferenceStartScreen(
        preferenceFragmentCompat: PreferenceFragmentCompat,
        preferenceScreen: PreferenceScreen
    ): Boolean {
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

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            val browserIntentUri: Uri? = when (preference.key) {
                getString(R.string.pref_key_privacy_notice) -> PRIVACY_NOTICE
                getString(R.string.pref_key_about) -> APP_ABOUT
                else -> null
            }

            return if (browserIntentUri == null) {
                super.onPreferenceTreeClick(preference)
            } else {
                // We're opening the settings links in the browser, instead of a standalone WebView,
                // because the OS has a bug that only permits a single WebView at a time (#433).
                //
                // The recommended way to open an Activity through Preferences is defining the <intent>
                // in XML. However, to specify a class, you must also specify a hard-coded package and
                // our package changes on build type (debug suffix) so we must write code to start the Activity.
                val browserIntent = Browser.getIntent(preference.context, browserIntentUri)
                startActivity(browserIntent)
                true
            }
        }

        override fun onCreateRecyclerView(inflater: LayoutInflater?, parent: ViewGroup?, savedInstanceState: Bundle?): RecyclerView {
            // Removing start/end padding from this view fixes an issue where,
            // when preferences without icons were used, dividers would stick
            // out beyond their accompanying text. We attempted to solve the
            // problem using PreferenceCategory.iconSpaceReserved, but saw no
            // effect for unknown reasons
            return super.onCreateRecyclerView(inflater, parent, savedInstanceState).apply {
                setPaddingRelative(0, paddingTop, 0, paddingBottom)
            }
        }
    }

    /**
     * We were unable to find a way to apply these styles to the ActionBar
     * through XML without providing an entirely custom layout.  These
     * styles are necessary to match the native settings page styling
     */
    private fun styleActionBar() {
        val toolbar = settings_container.rootView.findViewById<Toolbar>(R.id.action_bar)
        toolbar.children()
                .forEach { toolbarChild ->
                    val layoutParams = toolbarChild.layoutParams as? Toolbar.LayoutParams ?: return
                    toolbarChild.layoutParams = layoutParams.apply {
                        gravity = Gravity.CENTER_VERTICAL
                    }
                }

        // Align the title with the settings items. Unfortunately, this is done relative to the start edge.
        val settingsItemMarginStart = settings_container.paddingStart
        val backButton = toolbar.children().first { it is ImageView }
        toolbar.titleMarginStart = settingsItemMarginStart - backButton.width - toolbar.paddingStart
    }
}
