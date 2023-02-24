An example that shows how to generate native releases for Kotter applications.

Check out this project's `build.gradle.kts` file to see how this is set up.

For native builds, you must build them on the proper host machine -- for example, Windows binaries should get built on
Windows machines and Linux binaries should get built on Linux machines.

You can probably use something like GitHub CI to provision different host macines in order to generate artifacts for all
three platforms.