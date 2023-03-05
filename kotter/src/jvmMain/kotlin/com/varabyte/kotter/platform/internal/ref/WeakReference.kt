package com.varabyte.kotter.platform.internal.ref

import java.lang.ref.WeakReference as JvmWeakReference

actual typealias WeakReference<T> = JvmWeakReference<T>