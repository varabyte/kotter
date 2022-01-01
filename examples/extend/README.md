A simple demonstration of how you might "extend" Kotter using extension methods, so you, too, can create functions like
`input`, `offscreen`, `aside`, `onKeyPressed`, etc.

Doing this requires understanding some implementation details underneath Kotter, specifically lifecycles, various
scopes, and Kotter's thread-safe, lifecycle-aware datastore, which is explained in more depth in
[the README](https://github.com/varabyte/kotter#extending-kotter). Think of this example like a code companion for that
section.