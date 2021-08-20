package com.varabyte.konsole

import com.varabyte.konsole.core.KonsoleBlock
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.text.RESET_COMMAND

fun konsole(block: KonsoleScope.() -> Unit) {
    val konsoleBlock = KonsoleBlock()
    val scope = KonsoleScope(konsoleBlock)
    scope.block()

    // Clear state for next block!
    konsoleBlock.applyCommand(RESET_COMMAND)

    // TODO: This needs to be done on another thread
    print(konsoleBlock.toString())
    System.out.flush()
}