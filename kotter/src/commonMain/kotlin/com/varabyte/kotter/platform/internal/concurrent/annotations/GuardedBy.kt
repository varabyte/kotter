package com.varabyte.kotter.platform.internal.concurrent.annotations

// This is a fork of https://jcip.net/annotations/doc/net/jcip/annotations/GuardedBy.html, done in service of migrating
// Kotter over to a multiplatform world.
//
// This annotation does not currently do anything except serve as documentation.

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
internal annotation class GuardedBy(val value: String)