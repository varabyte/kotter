package com.varabyte.kotter.platform.collections

internal fun <K, V> MutableMap<K, V>.computeIfAbsent(key: K, compute: (K) -> V): V {
    val oldValue = get(key)
    if (oldValue == null) {
        val newValue = compute(key)
        put(key, newValue)
        return newValue
    }

    return oldValue
}
