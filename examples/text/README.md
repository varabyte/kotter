It's Wordle! You know [the one](https://www.nytimes.com/games/wordle/index.html).

This example doesn't necessarily focus on any particular feature, but instead serves as a basic example for how one can
structure a real Kotter application.

The heartbeat of this program is a `gameState` live var, which the `run` method updates periodically. When that state
changes, the game area repaints.

A fair bit of work is spent to get the animated feeling of revealing squares working. See the `addAnim` call and the
`Revealing` gamestate for more deatils about that.

The part that drives the Wordle guessing logic (which lives in the `Row` class) is actually relatively simple and
straightforward. This just goes to show how incredible Wordle's execution was, taking the world by storm using some
smart approaches (one word a day, sleek animations, simple presentation, and network effects via social media) on top of
an algorithm you could easily otherwise crank out in less than 30 minutes.