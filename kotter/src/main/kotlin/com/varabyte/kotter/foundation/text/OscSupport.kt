package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.runtime.internal.ansi.commands.AnchorCommand
import com.varabyte.kotter.runtime.render.RenderScope
import java.net.URI

/**
 * Open an 'anchor' or hyperlink of [uri] in the current scope. Leaving the scope will close the anchor.
 */
fun RenderScope.anchor(uri: URI) {
    applyCommand(AnchorCommand(uri))
}

/**
 * Open and close an 'anchor' or hyperlink of [uri] with the given [displayText]
 */
fun RenderScope.anchor(uri: URI, displayText: CharSequence) {
    applyCommand(AnchorCommand(uri))
    text(displayText)
    applyCommand(AnchorCommand())
}
