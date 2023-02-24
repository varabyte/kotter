package com.varabyte.kotter.platform.ref

import kotlin.native.ref.WeakReference as NativeWeakReference

actual typealias WeakReference<T> = NativeWeakReference<T>