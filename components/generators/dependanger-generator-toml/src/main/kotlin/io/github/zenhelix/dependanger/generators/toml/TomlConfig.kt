package io.github.zenhelix.dependanger.generators.toml

public data class TomlConfig(
    val filename: String,
    val includeComments: Boolean,
    val sortSections: Boolean,
    val useInlineVersions: Boolean,
    val includeDeprecationComments: Boolean,
) {
    public companion object {
        public const val DEFAULT_FILENAME: String = "libs.versions.toml"

        public val DEFAULT: TomlConfig = TomlConfig(
            filename = DEFAULT_FILENAME,
            includeComments = true,
            sortSections = true,
            useInlineVersions = false,
            includeDeprecationComments = true,
        )
    }
}
