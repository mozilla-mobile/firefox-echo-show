/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.toolbar

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import mozilla.components.browser.domains.DomainAutoCompleteProvider
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.support.ktx.android.content.res.pxToDp
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.mozilla.focus.R
import org.mozilla.focus.TouchInterceptorLayout
import org.mozilla.focus.ext.children
import org.mozilla.focus.ext.isScreenXLarge
import org.mozilla.focus.toolbar.ToolbarEvent.BACK
import org.mozilla.focus.toolbar.ToolbarEvent.FORWARD
import org.mozilla.focus.toolbar.ToolbarEvent.HOME
import org.mozilla.focus.toolbar.ToolbarEvent.LOAD_URL
import org.mozilla.focus.toolbar.ToolbarEvent.PIN_ACTION
import org.mozilla.focus.toolbar.ToolbarEvent.RELOAD
import org.mozilla.focus.toolbar.ToolbarEvent.SETTINGS

private const val TOOLBAR_BUTTON_BACKGROUND = R.drawable.toolbar_button_background
private const val BUTTON_ACTION_MARGIN_DP = 16

enum class ToolbarEvent {
    HOME, SETTINGS, BACK, FORWARD, RELOAD, LOAD_URL, TURBO, PIN_ACTION;

    companion object {
        const val VAL_CHECKED = "checked"
        const val VAL_UNCHECKED = "unchecked"
    }
}

/** A collection of callbacks to modify the toolbar. */
class ToolbarCallbacks(
    val onDisplayUrlUpdate: (url: String?) -> Unit,
    val onLoadingUpdate: (isLoading: Boolean) -> Unit,
    val onProgressUpdate: (progress: Int) -> Unit
)

typealias OnToolbarEvent = (
    event: ToolbarEvent,
    value: String?,
    autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?
) -> Unit

/**
 * Helper class for constructing and using the shared toolbar for navigation and homescreen.
 */
object ToolbarIntegration {

    /**
     * A map that keeps strong references to [OnSharedPreferenceChangeListener]s until the object it
     * manipulates, [BrowserToolbar], is GC'd (i.e. their lifecycles are the same). This is necessary
     * because [SharedPreferences.registerOnSharedPreferenceChangeListener] doesn't keep strong
     * references so someone else, this object, has to.
     */
    // private val weakToolbarToSharedPrefListeners = WeakHashMap<BrowserToolbar, OnSharedPreferenceChangeListener>()

    /**
     * Add the components of toolbar and returns a collection of callbacks to modify the toolbar
     * at runtime.
     *
     * We return callbacks, rather than the internal toolbar views, because it allows us to:
     * - Group all the low-level toolbar logic in this file
     * - Put all toolbar interactions behind an "interface" rather than coupling code to raw toolbar views
     * - Make the code more testable, due to the "interface" ^
     */
    @SuppressWarnings("LongMethod")
    fun setup(
        lifecycleOwner: LifecycleOwner,
        viewModel: ToolbarViewModel,
        toolbar: BrowserToolbar,
        toolbarStateProvider: ToolbarStateProvider,
        onToolbarEvent: OnToolbarEvent
    ): ToolbarCallbacks {
        val context = toolbar.context

        viewModel.isToolbarImportantForAccessibility.observe(lifecycleOwner, Observer {
            toolbar.setIsImportantForAccessibility(it!!)
        })

        toolbar.displaySiteSecurityIcon = false
        toolbar.hint = toolbar.context.getString(R.string.urlbar_hint)

        configureToolbarSpacing(toolbar)
        initTextChangeListeners(context, toolbar, onToolbarEvent)
        val progressBarController = configureProgressBar(context, toolbar)
        val pinButton = addToolbarButtons(context, toolbar, toolbarStateProvider, onToolbarEvent)

        // Some component workarounds.
        configureURLBarText(toolbar)

        return ToolbarCallbacks(
                onDisplayUrlUpdate = { url -> onDisplayUrlUpdate(toolbar, toolbarStateProvider, url, pinButton) },
                onLoadingUpdate = progressBarController::onLoadingUpdate,
                onProgressUpdate = progressBarController::onProgressUpdate
        )
    }

    private fun configureURLBarText(toolbar: BrowserToolbar) {
        // Components doesn't configure the text properly.
        // TODO: Replace with the components implementation:
        // https://github.com/mozilla-mobile/android-components/issues/756
        val urlBar = toolbar.displayToolbar.children().first { it is TextView } as TextView
        urlBar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)

        val textColor = ContextCompat.getColor(toolbar.context, R.color.photonGrey10)
        urlBar.setHintTextColor(textColor)
        urlBar.setTextColor(textColor)
    }

    private fun configureToolbarSpacing(toolbar: BrowserToolbar) {
        val res = toolbar.context.resources
        val dp16 = res.pxToDp(16)
        val dp20 = res.pxToDp(20)
        val dp72 = res.pxToDp(72)

        toolbar.setPadding(dp72, dp20, dp72, dp20)
        toolbar.urlBoxMargin = dp16
        toolbar.setUrlTextPadding(dp16, 0, 0, 0)
        toolbar.browserActionMargin = res.pxToDp(BUTTON_ACTION_MARGIN_DP)
    }

    private fun configureProgressBar(context: Context, toolbar: BrowserToolbar): ProgressBarController {
        val urlBoxBackground = UrlBoxBackgroundWithProgress(context)
        toolbar.urlBoxView = urlBoxBackground
        val progressBarController = ProgressBarController(urlBoxBackground)
        return progressBarController
    }

    @Suppress("ReplaceSingleLineLet") // Trailing let improves readability/removability of our workaround.
    private fun addToolbarButtons(
        context: Context,
        toolbar: BrowserToolbar,
        toolbarStateProvider: ToolbarStateProvider,
        onToolbarEvent: OnToolbarEvent
    ): ChangeableVisibilityButton {
        val res = context.resources
        fun getDrawable(@DrawableRes drawableId: Int): Drawable =
            ResourcesCompat.getDrawable(res, drawableId, null)!!

        val homescreenButton = BrowserToolbar.Button(
                getDrawable(R.drawable.ic_grid),
                context.getString(R.string.homescreen_title),
                background = TOOLBAR_BUTTON_BACKGROUND) { onToolbarEvent(HOME, null, null) }
                .let { WorkaroundAction(it) }
        toolbar.addNavigationAction(homescreenButton)

        val backButton = BrowserToolbar.Button(
                getDrawable(R.drawable.ic_back),
                context.getString(R.string.content_description_back),
                background = TOOLBAR_BUTTON_BACKGROUND,
                visible = toolbarStateProvider::isBackEnabled) { onToolbarEvent(BACK, null, null) }
                .let { WorkaroundAction(it) }
        toolbar.addNavigationAction(backButton)

        val forwardButton = BrowserToolbar.Button(
                getDrawable(R.drawable.ic_forward),
                context.getString(R.string.content_description_forward),
                toolbarStateProvider::isForwardEnabled,
                background = TOOLBAR_BUTTON_BACKGROUND) { onToolbarEvent(FORWARD, null, null) }
                .let { WorkaroundAction(it) }
        toolbar.addNavigationAction(forwardButton)

        val refreshButton = BrowserToolbar.Button(
                getDrawable(R.drawable.ic_refresh),
                context.getString(R.string.content_description_reload),
                background = TOOLBAR_BUTTON_BACKGROUND,
                visible = { !toolbarStateProvider.isStartupHomepageVisible() }) { onToolbarEvent(RELOAD, null, null) }
                .let { WorkaroundAction(it) }
        toolbar.addPageAction(refreshButton)

        val pinButton = ChangeableVisibilityButton(
                imageDrawable = getDrawable(R.drawable.ic_pin),
                imageDrawableSelected = getDrawable(R.drawable.ic_pin_filled),
                contentDescription = context.getString(R.string.pin_label),
                contentDescriptionSelected = context.getString(R.string.homescreen_unpin_a11y),
                background = R.drawable.toolbar_toggle_background,
                visibility = {
                    if (!toolbarStateProvider.isStartupHomepageVisible()) View.VISIBLE
                    else View.INVISIBLE
                }) { isSelected ->
            onToolbarEvent(PIN_ACTION, if (isSelected) ToolbarEvent.VAL_CHECKED else ToolbarEvent.VAL_UNCHECKED, null)
        }
        toolbar.addBrowserAction(pinButton)

        /*
        val turboButton = BrowserToolbar.ToggleButton(imageResource = R.drawable.ic_rocket,
                imageResourceSelected = R.drawable.ic_rocket_filled,
                contentDescription = context.getString(R.string.turbo_mode_enable_a11y),
                contentDescriptionSelected = context.getString(
                        R.string.turbo_mode_disable_a11y),
                background = R.drawable.toolbar_toggle_background,
                selected = Settings.getInstance(toolbar.context).isBlockingEnabled) { isSelected ->
            onToolbarEvent(TURBO, if (isSelected) ToolbarEvent.VAL_CHECKED else ToolbarEvent.VAL_UNCHECKED, null)
        }
        toolbar.addBrowserAction(turboButton)
        */

        // Non-xlarge screens use the margin between action items.
        if (res.configuration.isScreenXLarge) {
            // A margin is added to either side of the space so we remove those margins from the width we want.
            val actionSpaceWidth = 200 - res.pxToDp(BUTTON_ACTION_MARGIN_DP) * 2
            toolbar.addBrowserAction(Toolbar.ActionSpace(actionSpaceWidth))
        }

        val settingsButton = BrowserToolbar.Button(
                getDrawable(R.drawable.ic_settings),
                context.getString(R.string.menu_settings),
                background = TOOLBAR_BUTTON_BACKGROUND) { onToolbarEvent(SETTINGS, null, null) }
                .let { WorkaroundAction(it) }
        toolbar.addBrowserAction(settingsButton)

        val brandIcon = Toolbar.ActionImage(
            getDrawable(R.drawable.ic_firefox_and_workmark),
            ""
        ).let { WorkaroundAction(it, shouldTintIcon = false) }
        toolbar.addBrowserAction(brandIcon)

        /*
        val sharedPrefsListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == IWebView.TRACKING_PROTECTION_ENABLED_PREF) {
                turboButton.setSelected(sharedPreferences.getBoolean(key, true /* unused */),
                        notifyListener = true) // Allows BrowserFragment to respond.
            }
        }
        Settings.getInstance(toolbar.context).preferences.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
        weakToolbarToSharedPrefListeners[toolbar] = sharedPrefsListener
        */
        return pinButton
    }

    private fun initTextChangeListeners(
        context: Context,
        toolbar: BrowserToolbar,
        onToolbarEvent: OnToolbarEvent
    ) {
        val domainAutoCompleteProvider = DomainAutoCompleteProvider().apply {
            initialize(context)
        }
        toolbar.setAutocompleteFilter { value, view ->
            view?.let {
                val suggestion = domainAutoCompleteProvider.autocomplete(value)
                view.applyAutocompleteResult(
                        InlineAutocompleteEditText.AutocompleteResult(suggestion.text,
                                suggestion.source, suggestion.size, { suggestion.url }))
            }
        }

        toolbar.setOnUrlCommitListener { urlStr ->
            val result = domainAutoCompleteProvider.autocomplete(urlStr)
            val autocompleteResult = InlineAutocompleteEditText.AutocompleteResult(result.text, result.source, result.size)
            onToolbarEvent(LOAD_URL, urlStr, autocompleteResult)
        }

        toolbar.rootView?.findViewById<TouchInterceptorLayout>(R.id.main_content)
                ?.setOnTouchOutsideViewListener(toolbar) {
                    toolbar.displayMode()
                }
    }

    private fun onDisplayUrlUpdate(
        toolbar: BrowserToolbar,
        toolbarStateProvider: ToolbarStateProvider,
        url: String?,
        pinButton: Toolbar.ActionToggleButton
    ) {
        fun updateDisplayToolbarText() {
            if (url != null) {
                toolbar.url = ToolbarText.getDisplayText(Uri.parse(url)).fold(
                        { displayText -> displayText },
                        { "" }) // HintText is returned. Passing empty str to a TextView will display the hint.
            }
        }

        updateDisplayToolbarText()
        pinButton.setSelected(toolbarStateProvider.isURLPinned(),
                notifyListener = false) // We don't want to actually pin/unpin.
        toolbar.invalidateActions()
    }
}

/** A [Toolbar.Action] that works around limitations in the components. */
private class WorkaroundAction(
    private val baseAction: Toolbar.Action,
    private val shouldTintIcon: Boolean = true
) : Toolbar.Action by baseAction {
    override fun createView(parent: ViewGroup) = baseAction.createView(parent).apply {
        if (shouldTintIcon && this is ImageView) {
            tintToolbarIconColor()
        }

        removePaddingAddedByComponents()
    }
}

/**
 * A [BrowserToolbar.ToggleButton] that can be set to different visibilities
 */
private class ChangeableVisibilityButton(
    imageDrawable: Drawable,
    imageDrawableSelected: Drawable,
    contentDescription: String,
    contentDescriptionSelected: String,
    @DrawableRes background: Int,
    val visibility: () -> Int,
    listener: (Boolean) -> Unit
) : BrowserToolbar.ToggleButton(
        imageDrawable,
        imageDrawableSelected,
        contentDescription,
        contentDescriptionSelected,
        background = background,
        listener = listener
) {
    override fun bind(view: View) {
        super.bind(view)
        view.visibility = visibility()
    }

    override fun createView(parent: ViewGroup): View = super.createView(parent).apply {
        // We can't use WorkaroundAction for this functionality because we need the
        // Toolbar.ActionToggleButton return type.
        (this as ImageView).tintToolbarIconColor()
        removePaddingAddedByComponents()
    }
}

private fun View.removePaddingAddedByComponents() {
    // Components adds unnecessary padding to the ImageViews.
    // TODO: replace with components implementation:
    // https://github.com/mozilla-mobile/android-components/issues/772
    setPadding(0, 0, 0, 0)
}

private fun ImageView.tintToolbarIconColor() {
    // TODO: use the components tint implementation when available:
    // https://github.com/mozilla-mobile/android-components/issues/755
    val iconColor = ContextCompat.getColor(context, R.color.photonGrey10)
    imageTintList = ColorStateList.valueOf(iconColor)
}

private val BrowserToolbar.displayToolbar: ViewGroup
    // The class is internal so we compare against its name instead of its type.
    get() = children().first { it::class.java.simpleName == "DisplayToolbar" } as ViewGroup

private val BrowserToolbar.editToolbar: ViewGroup
    // The class is internal so we compare against its name instead of its type.
    get() = children().first { it::class.java.simpleName == "EditToolbar" } as ViewGroup

private fun BrowserToolbar.setIsImportantForAccessibility(isImportantForAccessibility: Boolean) {
    // The open-navigation-overlay button will remain focused unless another view requests focus
    // (which we expect to happen). We could clear focus here to decouple this code but it's
    // unfortunately non-trivial.
    //
    // Changing focusability doesn't seem to work so we change importantForAccessibility.
    importantForAccessibility = if (isImportantForAccessibility) {
        View.IMPORTANT_FOR_ACCESSIBILITY_YES
    } else {
        View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }
}
