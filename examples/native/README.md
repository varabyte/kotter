An example that shows how to generate native releases for Kotter applications.

To run this sample, in a terminal, call the correct `linkDebugExecutable...`. For example, on Linux:

```bash
# in kotter/examples/native
$ ./gradlew linkDebugExecutableLinux64 && ./build/bin/native/debugExecutable/native.kexe
```

To build a production binary, call:

```bash
$ ./gradlew linkReleaseExecutableLinux64
```

Check out this example's [`build.gradle.kts`](build.gradle.kts) file to see how this is set up.

For native builds, you must build them on the proper host machine -- for example, Windows binaries should get built on
Windows machines and Linux binaries should get built on Linux machines.

You can use something like GitHub CI to provision different host machines in order to generate and publish artifacts for
all different platforms.