package com.varabyte.kotter.platform.internal.ref

expect class WeakReference<T: Any>(referred: T) {
    fun get(): T?
}