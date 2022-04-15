It's Wordle! You know [the one](https://nytimes.com/wordle).

This example doesn't really focus on any particular one feature of Kotter to demonstrate it, but rather, it serves as a
fairly realistic example of a whole program. Among other things, it uses...

- `liveVar` for state management
- `textAnimOf` for drawing an animated ellipsis while waiting for dictionaries to download
- `input` for requesting input from the user
- `onInputChanged` and `onInputEntered` to handle input in the `run` block.
- `addTimer` for revealing the tile colors slowly instead of all at once
- Setting section `data` for high contrast mode, which can then act as a sort of scoped global data accessible across
  the whole section.
- Kotter-scope extension methods (e.g. `RenderScope.setColorFor(tile)`)
  - Also used to separate models (e.g. `Board`, `Row`) and their rendering
    (e.g. `RenderScope.renderBoard`, `RenderScope.renderRow`)

Overall, you get a flow that ping pongs between rendering the game state and code for handling the user's input.

Wordle's success is a testament to its execution, because as you can see here, the idea, stripped down to its core
essence, is very simple! You can find the main logic for testing a Wordle row in the `Row.from` function, just 30 lines
of code that took less than 30 minutes to figure out. If you squint at it right, that algorithm is worth 7 figures
according to the NYTimes! (Of course, there are additional factors, especially the social network effect and easy access
to the game through web browsers with mobile support, but c'mon, it's fun to oversimplify).

![Example in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-wordle.gif)