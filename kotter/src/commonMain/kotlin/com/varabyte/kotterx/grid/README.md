# Grids
Simple grid layout tools for creating arbitrary sized grids with rows and columns. With the optional use of the provided
`wrapText` and `wrapTextLine` extension functions, styles of the text will be preserved while wrapping them to fit
within predefined widths of the cells.

## APIs

`grid` - Defines the start of a grid section. Required and *must* surround all instances of *cell*
* `width` - when using `wrapText` or `wrapTextLine`, this lets them know how long the text should be before inserting newlines.
* `columns` - the number of cells per row. You *must* fill a row, or it will render out of order or not at all.
* `GridStyle` - Used to control styling between cells. 
  * `leftRightWalls` - whether walls should be printed between cells.
  * `topBottomWalls` - whether walls should be printed above and below cells.
  * `leftRightPadding` - how much padding between cell container and text (excluding the wall if present)

`cell` - an individual container, or column within a row. Can be empty. Must be a child of `grid`

`wrapText` - will constrain the length of text defined by `grid` to not exceed the `width` by adding newlines. Must
be a child of `grid` or `cell`.
* `text` - the text to wrap

`wrapTextLine` - same as `wrapText`, but will add a newline after the last line of text.

## Usage

### Simple Example

```kotlin
session {
    section {
        grid(width = 6, columns = 2, GridStyle(leftRightWalls = true, topBottomWalls = true, leftRightPadding = 1)) {
            cell {
                textLine("Cell1")
            }
            cell {
                textLine("Cell2")
            }

            cell {
                // empty cell needed for padding
            }
            cell {
                textLine("Cell4")
            }
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

### Explanation
`grid` performs appropriate setup for the offscreen buffer and final rendering of the child cells. `cell` is defining a
single container of the grid, much like a row and column in a table. Note the usage of an empty third cell. When using
grids, all rows must be "full" or they won't render correctly. Likewise, if a row doesn't need a cell in a given
position, it is OK to use empty cells. They will fill the entire container with spaces to ensure proper sizing.

### Styling Example
Grids can use styling within cells, and other Kotter primitives.

```kotlin
grid(width = 6, columns = 2) {
    cell {
        textLine("Cell1")
    }
    cell {
        yellow {
            textLine("Cell2")
        }
    }

    bold {
        cell {
            // empty cell needed for padding
        }
        cell {
            textLine("Cell4")
        }
    }
}
```

## Future Work
* Customizable walls, including colors.
* word wrapping
* multi-width cells
* multi-height cells
* nested tables
* top-bottom padding
* suggestions?