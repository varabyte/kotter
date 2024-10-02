An example that shows how to generate native releases for Kotter applications.

To run this sample on your machine, you must call the correct `linkDebugExecutable...` Gradle task for your OS.
For example:

```bash
# in kotter/examples/native

# Linux
$ ./gradlew linkDebugExecutableLinux64 && ./build/bin/linux64/debugExecutable/native.kexe

# Windows
$ ./gradlew linkDebugExecutableMingw64 && ./build/bin/mingw64/debugExecutable/native.exe

# Mac (M series)
$ ./gradlew linkDebugExecutableMacosArm64 && ./build/bin/macosArm64/debugExecutable/native.kexe
# For intel, use linkDebugExecutableMacosX64
# Getting this to work will require setting up Xcode on your machine, which is outside the scope
# of this README
```

*Currently, Kotter supports Linux, Windows, Mac M series, and Mac Intel. Visit [this issue](https://github.com/varabyte/kotter/issues/93)
if you have suggestions for other native targets.*

To build a production binary, call:

```bash
# Linux
$ ./gradlew linkReleaseExecutableLinux64
```

Check out this example's [`build.gradle.kts`](build.gradle.kts) file to see how this is set up.

For native builds, you must build them on the proper host machine -- for example, Windows binaries should get built on
Windows machines and Linux binaries should get built on Linux machines.

You can use something like GitHub CI to provision different host machines in order to generate and publish artifacts for
all different platforms.
