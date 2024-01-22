# Grids
Simple grid layout tools for creating arbitrary sized grids with rows and columns.

You specify the number of columns explicitly; rows are auto added as you declare new grid cells.

## Usage

### Simple Example

```kotlin
session {
    section {
        grid(Cols.uniform(2, width = 6), leftRightPadding = 1) {
            cell { textLine("Cell1") }
            cell { textLine("Cell2") }
            cell() // Skip over empty cell
            cell { textLine("Cell4") }
            // Alternately, `cell(row = 1, col = 1) { textLine("Cell4") }`
            // and no need to specify the empty cell
        }
    }.run()
}
```

the output from the above

```
------------------
| Cell   | Cell2  |
------------------
|        | Cell4  |
------------------
```
