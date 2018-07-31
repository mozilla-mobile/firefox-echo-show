/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

/**
 * An object assigned a LiveData instance to send a stateless event. See
 * [org.mozilla.focus.settings.UserClearDataEvent] for example usage.
 *
 * This implementation is inspired by:
 *   https://medium.com/google-developers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150
 */
class LiveDataEvent
