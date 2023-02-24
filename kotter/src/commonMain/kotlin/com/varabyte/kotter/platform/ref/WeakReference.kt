package com.varabyte.kotter.platform.ref

expect class WeakReference<T: Any>(referred: T) {
    fun get(): T?
}