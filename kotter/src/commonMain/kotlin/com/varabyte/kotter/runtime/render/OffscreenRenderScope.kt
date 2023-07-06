package com.varabyte.kotter.runtime.render

/**
 * A [RenderScope] used for the [offscreen] method.
 *
 * While it seems unnecessary to create an empty class like this, this can be useful if library authors want to provide
 * extension methods that only apply to `offscreen` scopes.
 */
class OffscreenRenderScope(renderer: Renderer<OffscreenRenderScope>) : OneShotRenderScope(renderer)
