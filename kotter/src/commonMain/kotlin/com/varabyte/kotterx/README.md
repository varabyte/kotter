The `kotterx` namespace is for useful add-ons that are built on top of core primitives (e.g. one layer above
`kotter.foundation`). Although they aren't technically fundamental to Kotter, they are nonetheless provided because they
are so widely beneficial.

It's possible this code will be separated into its own artifact at some point, but even if that never happens, at least
conceptually it's worth thinking about them that way. In other words, code in `com.varabyte.kotter` should NEVER import
anything from `com.varabyte.kotterx`.