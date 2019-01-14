# Telemetry
For clients that have "Send usage data" enabled, Firefox for Echo Show sends a "core" ping and an "event" ping to Mozilla's telemetry service. Sending telemetry can be disabled in the app's settings. Builds of Firefox for Echo Show have telemetry enabled by default ("opt-out").

## Core ping

Firefox for Echo Show creates and tries to send a "core" ping whenever the app goes to the background. This core ping uses the same format as Firefox for Android and is [documented on firefox-source-docs.mozilla.org](https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/data/core-ping.html).

## Event ping

In addition to the core ping an event ping for UI telemetry is generated and sent as soon as the app is sent to the background.

### Settings

As part of the event ping the most recent state of the user's setting is sent (default values in **bold**):

| Setting                  | Key                             | Value
|--------------------------|---------------------------------|----------------------
| Total home tile count    | total_home_tile_count           | `<int>`
| Custom home tile count   | custom_home_tile_count          | `<int>`


### Events

The event ping contains a list of events ([see event format on readthedocs.io](https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/collection/events.html)) for the following actions:

#### Sessions

| Event                                    | category | method     | object | value  |
|------------------------------------------|----------|------------|--------|--------|
| Start session (App is in the foreground) | action   | foreground | app    |        |
| Stop session (App is in the background)  | action   | background | app    |        |

N.B. Sessions as recorded today are unlikely to match your intuitive definition of a "session". Sessions will sometimes stop when the browser part of the application is partially obscured (`onPause/onResume`). In addition to situations where the session intuitively stops (e.g. device sleep or switching apps), on Echo Show the session also ends for:
- Opening Firefox settings
- Opening the device settings
- Unrelated voice commands that display visuals

The session does not end for:
- Unrelated voice commands that do not display visuals (N.B: only tested Alexa error responses)
- Opening the virtual keyboard

If this session probe doesn't fit your use case, you might be able to use the `process_start_timestamp` probe - recorded by telemetry library when the process starts - to make your queries.

#### General

| Event                                  | category | method                | object     | value                   | extras.                             |
|----------------------------------------|----------|-----------------------|------------|-------------------------|-------------------------------------|
| Settings: confirms clear data dialog   | action   | change                | setting    | clear_data              |                                     |
| Startup complete                       | aggregate| startup_complete      | app        | startup time in MS      |                                     |
| Page load histogram                    | histogram| foreground            | browser    | histogram of page loads |                                     |
| Fullscreen exited                      | action   | hide                  | fullscreen |                         | `{"scale_gesture": "true"/"false"}` |

#### Toolbar
| Event                                  | category | method                | object     | value   | extras.                      |
|----------------------------------------|----------|-----------------------|------------|---------|------------------------------|
| Home clicked                           | action   | click                 | toolbar    | home    |                              |
| Back clicked                           | action   | click                 | toolbar    | back    |                              |
| Forward clicked                        | action   | click                 | toolbar    | forward |                              |
| Refresh clicked                        | action   | click                 | toolbar    | refresh |                              |
| Settings clicked                       | action   | click                 | toolbar    | settings|                              |
| Pin/unpin clicked                      | action   | change                | pin_page   | on/off  |                              |
| URL entered                            | action   | type_url              | search_bar |         | `{autocomplete: true/false}` |
| Search query entered                   | action   | type_query            | search_bar |         |                              |

#### Home
| Event                                  | category | method                | object     | value                   | extras.    |
|----------------------------------------|----------|-----------------------|------------|-------------------------|------------|
| Tile clicked                           | action   | click                 | home_tile  | bundled/custom/youtube* |            |
| Tile removed                           | action   | remove                | home_tile  | bundled/custom          |            |
| Dismiss home (by clicking toolbar overlay)|action | hide                  | home       | overlay                 |            |

(*) A click on the YouTube tile will result in 1 'bundled' event and 1 'youtube' event being sent

#### SSL Errors

| Event                                      | category | method   | object  | extras  |
|--------------------------------------------|----------|----------|---------|---------|
| SSL Error From Page                        | error    | page     | browser |`error`* |
| SSL Error From Resource                    | error    | resource | browser |`error`* |

(*)`error` is a JSON map containing the primary SSL Error 

```JavaScript
{
  "error_code": "SSL_DATE_INVALID"  // Primary SSL Error
}
```

| Possible Error Codes |
|----------------------|
| SSL_DATE_INVALID     |
| SSL_EXPIRED          |
|SSL_IDMISMATCH        |
|SSL_NOTYETVALID       |
|SSL_UNTRUSTED         |
|SSL_INVALID           |
|Undefined SSL Error   |

### Limits

* An event ping will contain up to but no more than 500 events
* No more than 40 pings per type (core/event) are stored on disk for upload at a later time
* No more than 100 pings are sent per day

## Implementation notes

* Event pings are generated (and stored on disk) whenever the onStop() callback of the main activity is triggered. This happens whenever the main screen of the app is no longer visible (The app is in the background or another screen is displayed on top of the app).

* Whenever we are storing pings we are also scheduling an upload. We are using Android’s JobScheduler API for that. This allows the system to run the background task whenever it is convenient and certain criterias are met. The only criteria we are specifying is that we require an active network connection. In most cases this job is executed immediately after the app is in the background.

* Whenever an upload fails we are scheduling a retry. The first retry will happen after 30 seconds (or later if there’s no active network connection at this time). For further retries a exponential backoff policy is used: [30 seconds] * 2 ^ (num_failures - 1)

* An earlier retry of the upload can happen whenever the app is coming to the foreground and sent to the background again (the previous scheduled job is reset and we are starting all over again).

