# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Pinch to exit full screen web content like videos (#75)

### Changed
- Appearance and animations of navigation overlay to more clearly overlay browser content
- Disabled web content translation animation when entering/exiting fullscreen (#219)

### Fixed
- Toolbar not updating correctly after application state is restored (#145)
- A memory leak where the MainActivity was retained after the framework destroyed it (#153)

## [1.2] - ?
### Changed
- Sentry crash reports include a UUID to distinguish users so we can determine if it's 1 user crashing 100 times or 100 users crashing 1 time each. This identifier is only used for Sentry and can not be correlated with telemetry interaction data. See [fire TV Sentry docs](https://github.com/mozilla-mobile/firefox-tv/wiki/Crash-reporting-with-Sentry) for more details. (#565)

### Fixed
- Infrequently after removing autocomplete, the keyboard would be unusable until it was dismissed and reopened (#484)
- A crash that occurs after clearing browsing data (#540)
- A crash when loading Android Intent URIs (#500)

## [1.1] - 2018-10-11
*OTA update released to all devices on gen 2's release day (most users will not have seen v1.0)*

### Added
- Page load and app startup telemetry events
- Support for 1st generation Echo Show

### Changed
- Polish toolbar, home screen, and settings look-and-feel
- Improve home screen tile images
- Open links from Settings in the full browser
- User agent

### Removed
- System-wide VIEW intent handling

### Fixed
- Crash with unknown STR
- Overhaul screen reader accessibility
- Keyboard in toolbar includes ".com" option

### Security
- Disable JS alerts

## [1.0] - 2018-10-11
*Initial release shipped on gen 2 devices: a web browser with customizable home tiles to access your favorite sites quickly.*

### Added
- Home page with default home times: Google search, YouTube, and Wikipedia
- Ability to add/remove home tiles
- Settings: "Send usage data" preference, About page, Privacy Notice, and "Clear all cookies and site data" preference
- Toolbar: URL bar and home page, back/forward, refresh, pin/unpin, and settings buttons

[Unreleased]: https://github.com/.../compare/v1.2...HEAD
[1.2]: https://github.com/.../compare/v1.1...v1.2
[1.1]: https://github.com/.../compare/v1.0...v1.1
[1.0]: https://github.com/.../compare/06778075...v1.0
