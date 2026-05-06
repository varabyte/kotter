# Kotter v1.3.0 Release Notes

> **Note:** This is a first draft of the release notes for v1.3.0, based on all commits on the `dev` branch since tag
> `v1.2.1`. Items may need further verification, reordering, or rewording before the final release.

---

## Breaking Changes

### Naming convention updates (ALL_CAPS → PascalCase)

In line with the [Kotlin style guide for singleton objects](https://kotlinlang.org/docs/coding-conventions.html#property-names),
several constants previously using `SCREAMING_SNAKE_CASE` have been renamed to `PascalCase`. The old names carry
`@Deprecated` annotations with replacement suggestions where possible.

**`Keys` object** — all key constants renamed, e.g.:

| Before | After |
|---|---|
| `Keys.SPACE` | `Keys.Space` |
| `Keys.ENTER` | `Keys.Enter` |
| `Keys.LEFT` | `Keys.Left` |
| `Keys.UP` | `Keys.Up` |
| `Keys.ESC` | `Keys.Esc` |
| *(and all others)* | *(same pattern)* |

**`BorderCharacters` presets** — renamed, e.g.:

| Before | After |
|---|---|
| `BorderCharacters.ASCII` | `BorderCharacters.Ascii` |
| `BorderCharacters.CURVED` | `BorderCharacters.Curved` |
| `BorderCharacters.BOX_THIN` | `BorderCharacters.BoxThin` |
| `BorderCharacters.BOX_DOUBLE` | `BorderCharacters.BoxDouble` |

**`GridCharacters` presets** — same pattern (e.g. `GridCharacters.CURVED` → `GridCharacters.Curved`).

### `CharKey` is now case-insensitive; `UpperX` variants removed

`CharKey` equality now ignores case, so `Keys.Q` matches both `'q'` and `'Q'` keypresses. The old `Keys.UpperA`,
`Keys.UpperB`, … variants have been removed. If you need to distinguish case, use the new helpers:

```kotlin
when (key) {
    Keys.Q -> if (key.isUpper()) quit() else confirmThenQuit()
}
```

### `CharKey.code` renamed to `CharKey.char`

`CharKey.code` is deprecated with a `ReplaceWith("char")` annotation. Update usages to `key.char`.

### Minimum JVM Kotlin compatibility bumped to 1.8

Due to a Kotlin toolchain update, the minimum supported Kotlin version for JVM targets has been raised from 1.7 to 1.8.
Kotlin/Native targets continue to require 1.9+.

### `session.abortOnEof` moved to `session.defaults.abortOnEof`

The top-level `Session.abortOnEof` property is now located under `session.defaults.abortOnEof` as part of a broader
consolidation of session-wide defaults (see *Session defaults* below).

---

## New Features

### `TextMetrics` — proper Unicode character width support

A new `TextMetrics` class is now the single source of truth for measuring how many terminal columns a string occupies.
This correctly handles double-width characters (e.g. CJK), grapheme clusters, emoji, and other non-ASCII text.

`TextMetrics` is passed into `session()` and exposed on the session object so you can use it in your own code:

```kotlin
session {
    val width = textMetrics.renderWidthOf("日本語")  // returns 6, not 3
}
```

### `kotterx:vt-twemoji` — consistent cross-platform emoji in the Virtual Terminal

A new optional artifact (`com.varabyte.kotterx:vt-twemoji`) provides an `EmojiRenderer` backed by
[Twemoji](https://github.com/twitter/twemoji) SVGs. Adding this dependency makes the Virtual Terminal render emoji
consistently across platforms (Linux, macOS, Windows) rather than relying on each OS's built-in font:

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.varabyte.kotterx:vt-twemoji:<version>")
}
```

No code changes needed — the renderer is picked up automatically via a service loader.

### Session defaults

A new `session.defaults` property groups configurable global defaults:

| Property | Description |
|---|---|
| `session.defaults.animDuration` | Default frame duration for `textAnimOf` / `renderAnimOf` |
| `session.defaults.abortOnEof` | Whether `runUntilInputEntered` exits on EOF (Ctrl+D) by default |
| `session.defaults.borderCharacters` | Default border style used by `bordered {}` |
| `session.defaults.gridCharacters` | Default grid style used by `grid {}` |
| `session.defaults.renderTimeout` | Default timeout used in `kotter-test-support` render assertions |

### More control over `input` blocks

**`onInputCursorChanged` callback** — a new callback that fires when the cursor moves, and lets you override both the cursor index and its display width:

```kotlin
runUntilInputEntered {
    onInputCursorChanged { cursorIndex = /* your logic */ }
}
```

**`InputEdit` in `onInputChanged`** — the lambda now receives an `edit` parameter describing exactly what changed (what was inserted/deleted and at which index), enabling advanced IME-style transformations.

**EOF abort / auto-complete for `input`** — `runUntilInputEntered` gains an `abortOnEof` parameter; a separate option allows the current input value to be auto-submitted on EOF.

### Grid improvements

* **Horizontal lines toggle** — pass `showHorizontalLines = false` to `grid {}` to hide row separators.
* **Cell metrics** — a `metrics` object is now available inside each grid cell, exposing `metrics.width` (the usable column count). Useful for truncating content to fit:
  ```kotlin
  grid(Cols(20, 20)) {
      cell { text(longString.truncateToWidth(metrics.width)) }
  }
  ```
* **Default style changed** — grids now default to `GridCharacters.BoxThin` (was `ASCII`) for a nicer out-of-the-box look; tests still use `ASCII`.

### `EllipsisPresets`

Predefined `Ellipsis` instances are now available in `EllipsisPresets` (e.g. `EllipsisPresets.Clip`,
`EllipsisPresets.End`, …) for convenience with `truncateToWidth`.

### Terminal size exposed on `Session`

`session.terminal.width` / `session.terminal.height` are now accessible directly from the session object (not just
from inside a `section {}` block).

### Virtual Terminal scrollbar control

The Virtual Terminal's vertical scrollbar can now be hidden via `VirtualTerminal.create(showScrollbar = false)`.

### Implicit newlines in the in-memory terminal

The in-memory test terminal now properly simulates line wrapping when rendered text exceeds the configured width,
matching the behaviour of a real terminal.

### New example: Hiragana IME

A new `examples/hiragana` demo shows how to use the new `onInputCursorChanged` / `InputEdit` APIs to build a simple
romaji-to-hiragana input method editor.

---

## Bug Fixes

* **Escape key false positives** — fixed a timing issue where escape-sequence keys (arrows, function keys, etc.) could occasionally fire a spurious bare `ESC` keypress.
* **Crash on wide characters near render boundary** — fixed a crash that occurred when a double-width character was placed at the very edge of the render area.
* **Virtual Terminal: terminal width not respected** — the VT now correctly honours the `width` parameter passed to `VirtualTerminal.create()`.
* **Offscreen `text` not respecting `maxWidth`** — offscreen rendering now clips output correctly when a maximum width is set.
* **Trailing newline timing** — sections no longer eagerly append a trailing newline until the section completes, fixing visual glitches during rapid re-renders.
* **Rerender issue with implicit newlines** — fixed incorrect re-render when implicit line-wrap newlines were involved.
* **Virtual Terminal text wrapping** — VT autowrap now uses Unicode render widths rather than raw character counts, fixing garbled output for wide characters.
* **Virtual Terminal row size inconsistencies** — several edge cases in row-size tracking have been resolved.
* **Virtual Terminal cursor position** — fixed incorrect calculation of the character under the cursor.
* **Virtual Terminal Swing text wrapping conflict** — Swing's own text wrapping no longer interferes with Kotter's explicit layout.
* **Virtual Terminal clickable regions** — fixed unexpected extra clickable areas outside intentional interactive zones.
* **Virtual Terminal OOB on mouse hover** — fixed an `IndexOutOfBoundsException` triggered by hovering near the edge of the terminal area.
* **Virtual Terminal horizontal scrollbar** — fixed several layout issues with the horizontal scrollbar.
* **Hiragana double-combo characters** — fixed matching logic that missed compound romaji sequences (e.g. `kka` → `っか`).

---

## Infrastructure & Tooling

* **JLine updated; Jansi removed** — JLine now uses its built-in JNI provider instead of the bundled Jansi library. This simplifies the dependency tree and improves Windows compatibility. The previously bundled `jansi-1.18.jar` has been deleted.
* **Kotlin updated to 2.x** with full JDK 24 support across all examples and CI.
* **Gradle updated to 8.14.3**.
* **Examples moved to a separate `includeBuild` subproject** — the `examples/` directory is now an independent Gradle project that references the main Kotter build via `includeBuild`. This avoids polluting the main build graph.
* **Publishing migrated to the vanniktech Maven Publish plugin**.
* **Gradle build caching enabled** for faster incremental builds.
* **Dokka updated to 2.0.0**.
* **Snapshot repository URL updated** to the new Maven Central Portal URL (`https://central.sonatype.com/repository/maven-snapshots/`).
* **GitHub Actions**: new workflows for HTML coverage reports and test-failure artifact uploads; dynamic badge action version updated.
