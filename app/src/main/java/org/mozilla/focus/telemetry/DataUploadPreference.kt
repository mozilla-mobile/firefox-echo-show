/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.StrictMode
import android.preference.PreferenceManager
import org.mozilla.focus.R
import org.mozilla.telemetry.TelemetryHolder

private const val PREF_KEY_TELEMETRY = R.string.pref_key_telemetry

/** A data container for for the "Send usage data" preference the user can switch. */
@SuppressLint("StaticFieldLeak") // We intentionally hold the application context.
internal object DataUploadPreference : SharedPreferences.OnSharedPreferenceChangeListener {
    // Sentry needs a reference to the applicationContext. We do not store the Activity context.
    private lateinit var appContext: Context
    private lateinit var telemetryKey: String

    @JvmStatic
    fun init(appContext: Context) {
        telemetryKey = appContext.getString(PREF_KEY_TELEMETRY)
        this.appContext = appContext.applicationContext

        PreferenceManager.getDefaultSharedPreferences(appContext)
                .registerOnSharedPreferenceChangeListener(DataUploadPreference)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key.equals(telemetryKey) && sharedPreferences != null) {
            onEnabledChanged(appContext, sharedPreferences.getBoolean(telemetryKey, false))
        }
    }

    fun isEnabled(context: Context): Boolean {
        // The first access to shared preferences will require a disk read.
        val threadPolicy = StrictMode.allowThreadDiskReads()
        try {
            val resources = context.resources
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)

            val telemetryDefaultValue = resources.getBoolean(R.bool.pref_telemetry_default_value)
            return preferences.getBoolean(resources.getString(PREF_KEY_TELEMETRY), telemetryDefaultValue)
        } finally {
            StrictMode.setThreadPolicy(threadPolicy)
        }
    }

    private fun onEnabledChanged(context: Context, enabled: Boolean) {
        TelemetryHolder.get()
                .configuration
                .setUploadEnabled(enabled).isCollectionEnabled = enabled

        SentryWrapper.onIsEnabledChanged(context, enabled)
    }
}
