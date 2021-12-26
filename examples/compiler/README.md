A fake compiler example, mimicking the interface of something like bazel or gradle.

This demo helps showcase the `aside` function, which allows one to generate extra text that appears before the active,
dynamically updating text. Here, the dynamic text is the state of the threads, while the static text is a trail of
"compilation succeeded" and "compilation failed" messages.

![Example in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-compiler.gif)