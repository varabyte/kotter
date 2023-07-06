package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.platform.net.*
import com.varabyte.kotter.runtime.internal.ansi.*
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.kotterx.test.terminal.*
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import kotlin.test.Test

class LinkSupportTest {
    @Test
    fun `can define links`() = testSession { terminal ->
        section {
            text("Hello there, you should "); link("https://example1.com", "click here"); textLine()
            text("You can define "); link("https://example2.com", "multiple links"); textLine("!")
            text("If display text not set, the uri is used: "); link("https://example3.com"); textLine()
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Hello there, you should ${
                Ansi.Osc.Codes.openLink(Uri("https://example1.com")).toFullEscapeCode()
            }click here${Ansi.Osc.Codes.CLOSE_LINK.toFullEscapeCode()}",
            "You can define ${
                Ansi.Osc.Codes.openLink(Uri("https://example2.com")).toFullEscapeCode()
            }multiple links${Ansi.Osc.Codes.CLOSE_LINK.toFullEscapeCode()}!",
            "If display text not set, the uri is used: ${
                Ansi.Osc.Codes.openLink(Uri("https://example3.com")).toFullEscapeCode()
            }https://example3.com${Ansi.Osc.Codes.CLOSE_LINK.toFullEscapeCode()}",
            "${Ansi.Csi.Codes.Sgr.RESET}"
        ).inOrder()
    }

    @Test
    fun `urls must be properly formatted`() = testSession {
        section {
            assertThrows<UriSyntaxException> {
                link("uh oh bad uri", "uh oh")
            }
        }.run()
    }

    @Test
    fun `it is an error to nest a link within a link`() = testSession {
        section {
            link(Uri("https://outer.link.com")) {
                assertThrows<IllegalStateException> {
                    link("https://uh.oh.com", "Uh oh")
                }
            }
        }.run()
    }
}
