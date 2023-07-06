package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.platform.net.*
import com.varabyte.kotter.runtime.internal.ansi.commands.*
import com.varabyte.kotter.runtime.render.*

/**
 * Render text backed by a link, so it can be clicked to navigate to some URI.
 *
 * NOTE: It is not guaranteed that this feature is supported in every terminal, so you may not want to use it if it's
 * really important for the user to be able to click on the URL. If the feature isn't supported, [displayText] will
 * be rendered as plain text.
 *
 * @param uri The URI to visit when this text is clicked on.
 * @param displayText The user readable text to show which is backed by the [uri] when clicked. If not set, it will
 *   default to the value of the [uri]. It doesn't seem like it would be common to leave it out, since otherwise you can
 *   just print the URL out using the [text] method; however, some terminals add special link decorations that you may
 *   want to show up to call attention to the link.
 */
fun RenderScope.link(uri: Uri, displayText: CharSequence = uri.toString()) {
    link(uri) { text(displayText) }
}

fun RenderScope.link(uri: CharSequence, displayText: CharSequence = uri) {
    link(Uri(uri.toString()), displayText)
}

/**
 * Render a block of commands backed by a link, so anywhere it can be clicked to navigate to some URI.
 *
 * NOTE: It is not guaranteed that this feature is supported in every terminal, so you may not want to use it if it's
 * really important for the user to be able to click on the URL. If the feature isn't supported, the block will be
 * rendered as plain text.
 *
 * If you try to open a new link block within an existing link block, this method will throw an exception.
 */
// Bug(#91): Currently closed off from public use for now, since it seems needlessly powerful. However, we have a bug
// where people can request that we make this available in a future version.
// If we ever make this public, then uncomment out the alternate convenience version below
internal fun RenderScope.link(uri: Uri, block: RenderScope.() -> Unit) {
    val lastOpen = section.renderer.commands.indexOfLast { command -> command is OpenLinkCommand }
    val lastClose = section.renderer.commands.indexOfLast { command -> command === CloseLinkCommand }

    check(lastOpen < 0 || lastOpen < lastClose) {
        "Attempted to open a link block within another link block"
    }

    applyCommand(OpenLinkCommand(uri))
    block()
    applyCommand(CloseLinkCommand)
}

// Bug(#91): Uncomment me if the above method ever becomes public
//fun RenderScope.link(uri: String, block: RenderScope.() -> Unit) {
//    link(URI(uri), block)
//}
