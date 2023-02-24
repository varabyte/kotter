package com.varabyte.kotter.platform.concurrent.annotations

// This is a fork of https://jcip.net/annotations/doc/net/jcip/annotations/ThreadSafe.html, done in service of migrating
// Kotter over to a multiplatform world.

@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ThreadSafe
