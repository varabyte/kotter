A simple example which supports building native executables using the [Gradle plugin for GraalVM Native Image](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html).

You can create a native executable using the `native-image` tool from GraalVM. It basically looks at your app, figures out which classes and methods are actually used at runtime, and then compiles everything into a standalone binary for your OS.

The `native-image` tool lives in the bin directory of your GraalVM install and needs a few system dependencies (like a C compiler and standard libraries). You can install those with the package manager on your machine if they are missing.

For full setup details, check the official [documentation](https://www.graalvm.org/latest/reference-manual/native-image/).

Build executable: `gradle nativeCompile`

Run executable: `./build/native/nativeCompile/graalvm`
