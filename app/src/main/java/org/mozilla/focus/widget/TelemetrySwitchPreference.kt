/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.preference.PreferenceScreen
import android.support.v7.preference.PreferenceViewHolder
import android.support.v7.preference.SwitchPreferenceCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import org.mozilla.focus.R
import org.mozilla.focus.browser.Browser

/**
 * This class allows us to set a clickable link within a [PreferenceScreen] summary.
 */
class TelemetrySwitchPreference(context: Context, attrs: AttributeSet)
    : SwitchPreferenceCompat(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        (holder?.findViewById(android.R.id.summary) as TextView).apply {
            text = getTextSpan()
            visibility = View.VISIBLE
        }.let { summaryTv ->
            summaryTv.linksClickable = true
            summaryTv.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun getTextSpan(): Spannable {
        val (text, linkStart, linkEnd) = getSummaryText()
        val span = SpannableString(text)
        // Set text clicks to toggle button state
        span.setSpan(mainTextSpan(), 0, linkStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        // Set link clicks to open the URL
        span.setSpan(linkTextSpan(), linkStart, linkEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        // Set the trailing space to toggle button state.  If we don't do this,
        // any trailing empty space after the text will open our link on click
        span.setSpan(mainTextSpan(), linkEnd, linkEnd + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return span
    }

    private data class SummaryText(val text: String, val linkIndex: Int, val endLinkIndex: Int)
    /**
     * At this time, we only need this behavior for one specific view, so
     * generalizing the text would add unnecessary complexity. If this changes
     * in the future this class can be updated
     */
    private fun getSummaryText(): SummaryText {
        val summary = context.getString(R.string.preference_mozilla_telemetry_summary2,
                context.getString(R.string.app_name_extended_show))
        val learnMore = context.getString(R.string.preference_mozilla_telemetry_summary)
        val fullText = "$summary $learnMore "
        val learnMoreIndex = fullText.indexOf(learnMore)
        return SummaryText(fullText, learnMoreIndex, learnMoreIndex + learnMore.length)
    }

    private fun linkTextSpan(): ClickableSpan = object : ClickableSpan() {
        override fun onClick(widget: View?) {
            val intent = Browser.getIntent(context,
                    Uri.parse("https://www.mozilla.org/privacy/firefox/#health-report"))
            context.startActivity(intent)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun mainTextSpan(): ClickableSpan = object : ClickableSpan() {
        override fun onClick(widget: View?) {
            performClick()
        }
        // This span should not be styled as a link
        override fun updateDrawState(ds: TextPaint?) {}
    }
}
