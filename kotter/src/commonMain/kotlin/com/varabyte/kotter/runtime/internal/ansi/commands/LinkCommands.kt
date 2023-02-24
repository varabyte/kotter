package com.varabyte.kotter.runtime.internal.ansi.commands

import com.varabyte.kotter.platform.net.Uri
import com.varabyte.kotter.runtime.internal.ansi.Ansi

/**
 * A command for starting a block that allows for clickable links in the terminal.
 *
 * See also: https://gist.github.com/egmontkob/eb114294efbcd5adb1944c9f3cb5feda#the-escape-sequence
 */
// Note: Link commands support parameters using key/value pairs in a forward proof way, although currently it doesn't
// seem like there are any really worth worrying about in practice. According to the docs, only "id" is valid right now
// and I don't think Kotter applications need it. So for simplicity, we don't allow them to be set for now.
internal class OpenLinkCommand(uri: Uri) : AnsiOscCommand(Ansi.Osc.Codes.openLink(uri, emptyMap()))

/**
 * A command for closing a block opened by [OpenLinkCommand].
 */
internal val CloseLinkCommand = AnsiOscCommand(Ansi.Osc.Codes.CLOSE_LINK)
