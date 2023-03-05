package com.varabyte.kotter.runtime.render

/**
 * A [RenderScope] used for the [aside] method.
 *
 * While it seems unnecessary to create an empty class like this, this can be useful if library authors want to provide
 * extension methods that only apply to `aside` scopes.
 */
class AsideRenderScope(renderer: Renderer<AsideRenderScope>): OneShotRenderScope(renderer)
