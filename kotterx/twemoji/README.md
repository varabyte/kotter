The Kotter virtual terminal exposes an EmojiRenderer interface for external implementation, which this module provides.

The artifact produced by this project bundles SVGs from the [Twemoji (Twitter Emoji) project](https://github.com/twitter/twemoji), allowing for a
cross-platform emoji experience (otherwise, emojis are ridiculously temperamental). It should add about 4-5MB of size
to the produced application.

This project also makes use of [JSVG](https://github.com/weisJ/jsvg), a lightweight Java SVG renderer.

Both the Twemoji and JSVG projects are MIT Licensed. 