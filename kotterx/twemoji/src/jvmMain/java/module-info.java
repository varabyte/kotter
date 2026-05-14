module com.varabyte.kotterx.twemoji {
    requires java.base;
    requires java.desktop;
    requires java.compiler;
    requires kotlin.stdlib;
    requires com.varabyte.kotter;
    requires com.github.weisj.jsvg;
    // import java.awt.Graphics2D
    //import java.awt.Rectangle
    //import java.nio.file.Files
    //import java.nio.file.StandardCopyOption
    //import java.util.zip.ZipEntry
    //import java.util.zip.ZipFile
    //import javax.swing.JComponent

    provides com.varabyte.kotter.terminal.virtual.EmojiRenderer with com.varabyte.kotterx.terminal.virtual.TwemojiRenderer;
}