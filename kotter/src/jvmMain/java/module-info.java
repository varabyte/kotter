module com.varabyte.kotter {
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;

    requires org.jline.terminal;
    requires org.jline.terminal.jni;

    exports com.varabyte.kotter.foundation;
    exports com.varabyte.kotter.foundation.anim;
    exports com.varabyte.kotter.foundation.collections;
    exports com.varabyte.kotter.foundation.input;
    exports com.varabyte.kotter.foundation.render;
    exports com.varabyte.kotter.foundation.shutdown;
    exports com.varabyte.kotter.foundation.text;
    exports com.varabyte.kotter.foundation.timer;
    exports com.varabyte.kotter.platform.concurrent.locks;
    exports com.varabyte.kotter.platform.net;
    exports com.varabyte.kotter.runtime;
    exports com.varabyte.kotter.runtime.concurrent;
    exports com.varabyte.kotter.runtime.coroutines;
    exports com.varabyte.kotter.runtime.render;
    exports com.varabyte.kotter.runtime.terminal;
    exports com.varabyte.kotter.runtime.terminal.inmemory;
    exports com.varabyte.kotter.terminal.system;
    exports com.varabyte.kotter.terminal.virtual;
    exports com.varabyte.kotterx.decorations;
    exports com.varabyte.kotterx.grid;
    exports com.varabyte.kotterx.text;
    exports com.varabyte.kotterx.util;
    exports com.varabyte.kotterx.util.collections;

    uses com.varabyte.kotter.terminal.virtual.EmojiRenderer;
}