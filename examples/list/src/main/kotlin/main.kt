import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.collections.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.*
import java.util.*

fun Enum<*>.toDisplayName(capitalized: Boolean = false): String {
    // e.g. HOT_DOG to "Hot Dog"
    return name.split("_").joinToString(" ") { s ->
        s.lowercase()
            .let { if (capitalized) it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } else it }
    }
}

fun Collection<Enum<*>>.toDisplayText(capitalized: Boolean = false): String {
    // e.g. {KETCHUP, MUSTARD} to "Ketchup, Mustard"
    return this.joinToString { it.toDisplayName(capitalized) }
}

enum class Dish {
    BURGER,
    HOT_DOG,
    SALAD,
    PIZZA;
}

enum class Topping {
    AVOCADO,
    CHEESE,
    CHICKEN,
    KETCHUP,
    LETTUCE,
    MAYO,
    MUSHROOMS,
    MUSTARD,
    ONIONS,
    PEPPERONI,
    RELISH,
    TOMATOES,
}

val RELEVANT_TOPPINGS: Map<Dish, Set<Topping>> = mapOf(
    Dish.BURGER to setOf(
        Topping.AVOCADO,
        Topping.CHEESE,
        Topping.KETCHUP,
        Topping.LETTUCE,
        Topping.MAYO,
        Topping.MUSHROOMS,
        Topping.MUSTARD,
        Topping.ONIONS,
        Topping.TOMATOES,
    ),

    Dish.HOT_DOG to setOf(
        Topping.KETCHUP,
        Topping.LETTUCE,
        Topping.MUSTARD,
        Topping.ONIONS,
        Topping.RELISH,
    ),

    Dish.SALAD to setOf(
        Topping.AVOCADO,
        Topping.CHEESE,
        Topping.CHICKEN,
        Topping.MUSHROOMS,
        Topping.ONIONS,
        Topping.TOMATOES,
    ),

    Dish.PIZZA to setOf(
        Topping.CHEESE,
        Topping.CHICKEN,
        Topping.MUSHROOMS,
        Topping.PEPPERONI,
    ),
)

private fun Session.chooseDish(): Dish {
    var cursorIndex by liveVarOf(0)

    section {
        bold { textLine("Please choose a dish:") }
        textLine()
        Dish.values().forEachIndexed { i, dish ->
            text(if (i == cursorIndex) '>' else ' '); text(' ')
            textLine(dish.toDisplayName(capitalized = true))
        }
        textLine()
        black(isBright = true) { textLine("Use UP/DOWN to choose and ENTER to select") }
        textLine()
    }.runUntilSignal {
        onKeyPressed {
            when (key) {
                Keys.Up -> cursorIndex -= 1
                Keys.Down -> cursorIndex += 1
                Keys.Enter -> signal()
            }

            if (cursorIndex < 0) cursorIndex = Dish.values().lastIndex
            else if (cursorIndex > Dish.values().lastIndex) cursorIndex = 0
        }
    }

    return Dish.values()[cursorIndex]
}

private fun Session.chooseToppingsFor(dish: Dish): Set<Topping> {
    val toppings = RELEVANT_TOPPINGS.getValue(dish).toList()
    var cursorIndex by liveVarOf(0)
    val selectedToppings = liveSetOf<Topping>()
    val confirmIndex = toppings.size // "Confirm" option comes after the last item in the toppings list

    section {
        bold { textLine("Please choose any number of toppings:") }
        textLine()
        toppings.forEachIndexed { i, topping ->
            // Render cursor
            text(if (i == cursorIndex) '>' else ' '); text(' ')

            // Render checkbox
            text('[')
            text(if (selectedToppings.contains(topping)) 'X' else ' ')
            text(']')
            text(' ')

            // Render topping
            textLine(topping.toDisplayName(capitalized = true))
        }
        text(if (cursorIndex == confirmIndex) '>' else ' '); text(' ')
        bold { yellow { textLine("Confirm") } }
        textLine()
        black(isBright = true) {
            textLine("Use UP/DOWN to choose, SPACE to toggle selection, and ENTER to Confirm")
        }
        textLine()
    }.runUntilSignal {
        onKeyPressed {
            when (key) {
                Keys.Up -> cursorIndex -= 1
                Keys.Down -> cursorIndex += 1
                Keys.Home -> cursorIndex = 0
                Keys.End -> cursorIndex = confirmIndex
                Keys.Space -> {
                    if (cursorIndex in toppings.indices) {
                        val currTopping = toppings[cursorIndex]
                        if (selectedToppings.contains(currTopping)) {
                            selectedToppings.remove(currTopping)
                        } else {
                            selectedToppings.add(currTopping)
                        }
                    }
                }

                Keys.Enter -> {
                    // Only finish if we press ENTER on the Confirm choice
                    if (cursorIndex !in toppings.indices) {
                        signal()
                    }
                }
            }

            if (cursorIndex < 0) cursorIndex = confirmIndex
            else if (cursorIndex > confirmIndex) cursorIndex = 0
        }
    }

    return selectedToppings
}

fun main() = session {
    val dish = chooseDish()
    val toppings = chooseToppingsFor(dish)

    section {
        bold {
            textLine("You ordered a ${dish.toDisplayName()} with: ${if (toppings.isNotEmpty()) toppings.toDisplayText() else "no toppings"}.")
        }
    }.run()
}
