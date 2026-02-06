package io.github.zenhelix.dependanger.generators.toml

public data class TomlConfig(
    val filename: String = "libs.versions.toml",
    val includeComments: Boolean = true,
    val sortSections: Boolean = true,
    val useInlineVersions: Boolean = false,
)
