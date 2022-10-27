package com.varabyte.kotter.foundation

import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotterx.test.foundation.testSession
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class LiveVarTest {
    @Test
    fun `changing a livevar triggers a rerender`() = testSession {
        var renderCount = 0
        var dummyVar by liveVarOf(false)
        section {
            textLine(dummyVar.toString())
            ++renderCount
        }.run {
            dummyVar = true
        }

        assertThat(renderCount).isEqualTo(2)
    }

    @Test
    fun `changing a livevar won't trigger a rerender if not read in the section`() = testSession {
        var renderCount = 0
        var dummyVar by liveVarOf(false)
        section {
            ++renderCount
        }.run {
            dummyVar = true
        }

        assertThat(renderCount).isEqualTo(1)
    }
}