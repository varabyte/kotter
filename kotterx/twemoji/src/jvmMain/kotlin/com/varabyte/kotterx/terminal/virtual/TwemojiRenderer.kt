package com.varabyte.kotterx.terminal.virtual

import com.github.weisj.jsvg.SVGDocument
import com.github.weisj.jsvg.parser.LoaderContext
import com.github.weisj.jsvg.parser.SVGLoader
import com.github.weisj.jsvg.view.ViewBox
import com.varabyte.kotter.terminal.virtual.EmojiRenderer
import java.awt.Graphics2D
import java.awt.Rectangle
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.swing.JComponent

class TwemojiRenderer : EmojiRenderer {
    private val loader = SVGLoader()
    private val loaderContext = LoaderContext.createDefault()
    private val cache = mutableMapOf<String, SVGDocument?>()

    private val zipFile by lazy {
        val stream = EmojiRenderer::class.java.getResourceAsStream("/twemoji-svg.zip")
            ?: throw IllegalStateException("Emoji resource ZIP not found")

        // ZipFile constructor needs an actual file on disk, not bytes embedded in a resource stream
        val tempZip = Files.createTempFile("kotter-virtual-terminal-twemoji-svg-", ".zip")
        val tempZipFile = tempZip.toFile()
        tempZipFile.deleteOnExit()
        stream.use { Files.copy(it, tempZip, StandardCopyOption.REPLACE_EXISTING) }

        ZipFile(tempZipFile)
    }

    private fun getCodePointStringForGrapheme(grapheme: String): String {
        // twemoji file names are lowercased
        return grapheme.codePoints().toArray().joinToString("-") { it.toString(16).lowercase() }
    }

    private fun getSvg(grapheme: String): SVGDocument? {
        val codePointStr = getCodePointStringForGrapheme(grapheme)

        synchronized(cache) {
            return cache.getOrPut(codePointStr) {
                var entry: ZipEntry? = zipFile.getEntry("$codePointStr.svg")
                // Some emoji are old black and white fonts plus a variant selector; but Twemoji doesn't include that
                // variant selector on some of its emoji file names, so remove it and try again. The popular heart
                // emoji is such an example (2764-FE0F).
                if (entry == null && codePointStr.endsWith("-fe0f")) {
                    entry = zipFile.getEntry("${codePointStr.dropLast(5)}.svg")
                }
                if (entry == null) return@getOrPut null

                zipFile.getInputStream(entry).use { stream -> loader.load(stream, null, loaderContext) }
            }
        }
    }

    override fun render(
        g2d: Graphics2D,
        component: JComponent,
        grapheme: String,
        bounds: Rectangle
    ): Boolean {
        val svg = getSvg(grapheme) ?: return false
        svg.render(component, g2d, ViewBox(bounds))
        return true
    }
}