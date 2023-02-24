package com.varabyte.kotter.platform.collections

internal fun <E> MutableList<E>.removeIf(filter: (E) -> Boolean): Boolean {
    var removed = false

    val each = listIterator()
    while (each.hasNext()) {
        if (filter(each.next())) {
            each.remove()
            removed = true
        }
    }
    return removed
}
