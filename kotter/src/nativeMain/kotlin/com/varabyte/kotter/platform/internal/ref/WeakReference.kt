package com.varabyte.kotter.platform.internal.ref

import kotlin.native.ref.WeakReference as NativeWeakReference

actual typealias WeakReference<T> = NativeWeakReference<T>