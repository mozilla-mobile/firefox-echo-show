/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry

import android.content.Context
import android.net.http.SslError
import android.os.StrictMode
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText.AutocompleteResult
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.home.BundledHomeTile
import org.mozilla.focus.home.CustomHomeTile
import org.mozilla.focus.home.HomeTile
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.focus.toolbar.ToolbarEvent
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.event.TelemetryEvent
import org.mozilla.telemetry.measurement.DefaultSearchMeasurement
import org.mozilla.telemetry.measurement.SearchesMeasurement
import org.mozilla.telemetry.net.HttpURLConnectionTelemetryClient
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder
import org.mozilla.telemetry.ping.TelemetryMobileEventPingBuilder
import org.mozilla.telemetry.schedule.jobscheduler.JobSchedulerTelemetryScheduler
import org.mozilla.telemetry.serialize.JSONPingSerializer
import org.mozilla.telemetry.storage.FileTelemetryStorage

@Suppress(
        // Yes, this a large class with a lot of functions. But it's very simple and still easy to read.
        "TooManyFunctions",
        "LargeClass"
)
object TelemetryWrapper {
    private const val TELEMETRY_APP_NAME = "FirefoxConnect"

    private var APP_START_EVENT_SENT = false

    private object Category {
        val ACTION = "action"
        val AGGREGATE = "aggregate"
        val ERROR = "error"
    }

    private object Method {
        val TYPE_URL = "type_url"
        val TYPE_QUERY = "type_query"
        val CLICK = "click"
        val CHANGE = "change"
        val FOREGROUND = "foreground"
        val BACKGROUND = "background"
        val SHOW = "show"
        val HIDE = "hide"
        val PAGE = "page"
        val RESOURCE = "resource"
        val REMOVE = "remove"
        val STARTUP_COMPLETE = "startup_complete"
    }

    private object Object {
        val TOOLBAR = "search_bar" // search_bar to be consistent with other products.
        val SETTING = "setting"
        val APP = "app"
        val HOME = "home"
        val BROWSER = "browser"
        const val HOME_TILE = "home_tile"
        val TURBO_MODE = "turbo_mode"
        val PIN_PAGE = "pin_page"
    }

    internal object Value {
        val URL = "url"
        val RELOAD = "refresh"
        val CLEAR_DATA = "clear_data"
        val BACK = "back"
        val FORWARD = "forward"
        val HOME = "home"
        val SETTINGS = "settings"
        val ON = "on"
        val OFF = "off"
        val TILE_BUNDLED = "bundled"
        val TILE_CUSTOM = "custom"
        val OVERLAY = "overlay"
    }

    private object Extra {
        val AUTOCOMPLETE = "autocomplete"
        val ERROR_CODE = "error_code"

        // We need this second source key because we use SOURCE when using this key.
        // For the value, "autocomplete_source" exceeds max extra key length.
        val AUTOCOMPLETE_SOURCE = "autocompl_src"
    }

    @JvmStatic
    fun init(context: Context) {
        // When initializing the telemetry library it will make sure that all directories exist and
        // are readable/writable.
        val threadPolicy = StrictMode.allowThreadDiskWrites()
        try {
            val telemetryEnabled = DataUploadPreference.isEnabled(context)

            val configuration = TelemetryConfiguration(context)
                    .setServerEndpoint("https://incoming.telemetry.mozilla.org")
                    .setAppName(TELEMETRY_APP_NAME)
                    .setUpdateChannel(BuildConfig.BUILD_TYPE)
                    .setPreferencesImportantForTelemetry(
                            TelemetrySettingsProvider.PREF_CUSTOM_HOME_TILE_COUNT,
                            TelemetrySettingsProvider.PREF_TOTAL_HOME_TILE_COUNT
                    )
                    .setSettingsProvider(TelemetrySettingsProvider(context))
                    .setCollectionEnabled(telemetryEnabled)
                    .setUploadEnabled(telemetryEnabled)

            val serializer = JSONPingSerializer()
            val storage = FileTelemetryStorage(configuration, serializer)
            val client = HttpURLConnectionTelemetryClient()
            val scheduler = JobSchedulerTelemetryScheduler()

            TelemetryHolder.set(Telemetry(configuration, storage, client, scheduler)
                    .addPingBuilder(TelemetryCorePingBuilder(configuration))
                    .addPingBuilder(TelemetryMobileEventPingBuilder(configuration))
                    .setDefaultSearchProvider(createDefaultSearchProvider(context)))
        } finally {
            StrictMode.setThreadPolicy(threadPolicy)
        }
    }

    private fun createDefaultSearchProvider(context: Context): DefaultSearchMeasurement.DefaultSearchEngineProvider {
        return DefaultSearchMeasurement.DefaultSearchEngineProvider {
            SearchEngineManager.getInstance()
                    .getDefaultSearchEngine(context)
                    .identifier
        }
    }

    fun startSession() {
        TelemetryHolder.get().recordSessionStart()
        TelemetryEvent.create(Category.ACTION, Method.FOREGROUND, Object.APP).queue()
    }

    fun stopSession() {
        TelemetryHolder.get().recordSessionEnd()
        TelemetryEvent.create(Category.ACTION, Method.BACKGROUND, Object.APP).queue()
    }

    fun startupCompleteEvent(time: Long) {
        if (APP_START_EVENT_SENT) return
        TelemetryEvent.create(Category.AGGREGATE, Method.STARTUP_COMPLETE, Object.APP, time.toString()).queue()
        APP_START_EVENT_SENT = true
    }

    fun stopMainActivity() {
        TelemetryHolder.get()
                .queuePing(TelemetryCorePingBuilder.TYPE)
                .queuePing(TelemetryMobileEventPingBuilder.TYPE)
                .scheduleUpload()
    }

    fun urlBarEvent(isUrl: Boolean, autocompleteResult: AutocompleteResult) {
        if (isUrl) {
            TelemetryWrapper.urlEnterEvent(autocompleteResult)
        } else {
            TelemetryWrapper.searchEnterEvent()
        }
    }

    private fun urlEnterEvent(autocompleteResult: AutocompleteResult) {
        TelemetryEvent.create(Category.ACTION, Method.TYPE_URL, Object.TOOLBAR)
                .extra(Extra.AUTOCOMPLETE, (!autocompleteResult.isEmpty).toString())
                .queue()
    }

    private fun searchEnterEvent() {
        TelemetryEvent.create(Category.ACTION, Method.TYPE_QUERY, Object.TOOLBAR).queue()

        val telemetry = TelemetryHolder.get()
        val searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(
                telemetry.configuration.context)
        telemetry.recordSearch(SearchesMeasurement.LOCATION_ACTIONBAR, searchEngine.identifier)
    }

    @JvmStatic
    fun sslErrorEvent(fromPage: Boolean, error: SslError) {
        // SSL Errors from https://developer.android.com/reference/android/net/http/SslError.html
        val primaryErrorMessage = when (error.primaryError) {
            SslError.SSL_DATE_INVALID -> "SSL_DATE_INVALID"
            SslError.SSL_EXPIRED -> "SSL_EXPIRED"
            SslError.SSL_IDMISMATCH -> "SSL_IDMISMATCH"
            SslError.SSL_NOTYETVALID -> "SSL_NOTYETVALID"
            SslError.SSL_UNTRUSTED -> "SSL_UNTRUSTED"
            SslError.SSL_INVALID -> "SSL_INVALID"
            else -> "Undefined SSL Error"
        }
        TelemetryEvent.create(Category.ERROR, if (fromPage) Method.PAGE else Method.RESOURCE, Object.BROWSER)
                .extra(Extra.ERROR_CODE, primaryErrorMessage)
                .queue()
    }

    fun homeTileClickEvent(tile: HomeTile) {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.HOME_TILE,
                getTileTypeAsStringValue(tile)).queue()
    }

    fun clearDataEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.SETTING, Value.CLEAR_DATA).queue()
    }

    fun toolbarEvent(event: ToolbarEvent, value: String?) {
        val isCheckedValue = when (value) {
            ToolbarEvent.VAL_CHECKED -> Value.ON
            ToolbarEvent.VAL_UNCHECKED -> Value.OFF
            else -> null
        }

        val telemetryValue = when (event) {
            ToolbarEvent.HOME -> Value.HOME
            ToolbarEvent.SETTINGS -> Value.SETTINGS

            ToolbarEvent.BACK -> Value.BACK
            ToolbarEvent.FORWARD -> Value.FORWARD
            ToolbarEvent.RELOAD -> Value.RELOAD

            // For legacy reasons, turbo has different telemetry params so we special case it.
            // Pin has a similar state change so we model it after turbo.
            ToolbarEvent.TURBO -> {
//                TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.TURBO_MODE, boolToOnOff(toolbarStateProvider)).queue()
                return
            }
            ToolbarEvent.PIN_ACTION -> {
                TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.PIN_PAGE, isCheckedValue).queue()
                return
            }

            // Load is handled in a separate event
            ToolbarEvent.LOAD_URL -> return
        }
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.TOOLBAR, telemetryValue).queue()
    }

    fun dismissHomeOverlayClickEvent() {
        TelemetryEvent.create(Category.ACTION, Method.HIDE, Object.HOME, Value.OVERLAY)
    }

    fun homeTileRemovedEvent(removedTile: HomeTile) {
        TelemetryEvent.create(Category.ACTION, Method.REMOVE, Object.HOME_TILE,
                getTileTypeAsStringValue(removedTile)).queue()
    }

    private fun getTileTypeAsStringValue(tile: HomeTile) = when (tile) {
        is BundledHomeTile -> Value.TILE_BUNDLED
        is CustomHomeTile -> Value.TILE_CUSTOM
    }
}
