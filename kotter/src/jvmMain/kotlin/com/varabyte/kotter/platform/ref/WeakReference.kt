package com.varabyte.kotter.platform.ref

import java.lang.ref.WeakReference as JvmWeakReference

actual typealias WeakReference<T> = JvmWeakReference<T>