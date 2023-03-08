# Migrating

If any releases introduce a significant change to any of Kotter's APIs, this document will provide steps to help users
migrate their codebase over to newer versions.

## 1.0.x to 1.1.0

1.1.0 upgrades Kotter from a JVM library to a multiplatform library, adding Kotlin/Native support (for Windows, Linux,
and Mac OS targets).

While a lot of Kotter converted over smoothly, some APIs that were too closely tied to the JVM had to be updated.

### Build script

The coordinate `com.varabyte.kotter:kotter` must now be declared as `kotter-jvm`:

```kotlin
// BEFORE:
dependencies {
  implementation("com.varabyte.kotter:kotter:1.0.2")
  //                                  ^^^^^^
}

// AFTER:
dependencies {
  implementation("com.varabyte.kotter:kotter-jvm:1.1.0")
  //                                  ^^^^^^^^^^
}
```

### Code changes

* Java `Duration` -> Kotlin `Duration`
  * **You will probably have a few instances of these in your codebase, but fixing them is straightforward.**
  * e.g. `Duration.ofMillis(250)` -> `250.milliseconds`
  * e.g. `Duration.ofSeconds(5)` -> `5.seconds`
  * e.g. `duration.toMillis()` -> `duration.inWholeMilliseconds`
  * See also: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/
* `java.net.URI` -> `com.varabyte.kotter.platform.net.Uri`
  * You probably aren't using this.
* `java.util.concurrent.locks.ReentrantReadWriteLock.write` -> `com.varabyte.kotter.platform.concurrent.locks.ReentrantReadWriteLock.write`
  * You probably aren't using this.
