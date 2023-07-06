package com.varabyte.kotter.platform.net

// From https://mathiasbynens.be/demo/url-regex
// TODO: Consider creating a proper multiplatform version of URI
private val URI_REGEX = Regex("""(https?|ftp)://[^\s/$.?#].\S*""")

actual class Uri actual constructor(private val uri: String) {
    init {
        if (!URI_REGEX.matches(uri)) {
            throw UriSyntaxException(uri)
        }
    }

    override fun toString() = uri
}

actual class UriSyntaxException(badUri: String) : Exception("Malformatted URI: $badUri")
