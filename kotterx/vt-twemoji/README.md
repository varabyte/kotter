The Kotter virtual terminal exposes an EmojiRenderer interface for external implementation, which this module provides.
It bundles SVGs from the [Twemoji (Twitter Emoji) project](https://github.com/twitter/twemoji), allowing for a
cross-platform emoji experience (otherwise, emojis are ridiculously tempermental).

This project also makes use of [JSVG](https://github.com/weisJ/jsvg), a lightweight Java SVG renderer.

Both the Twemoji and JSVG projects are MIT Licensed. 