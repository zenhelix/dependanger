package io.github.zenhelix.dependanger.maven.pom.writer

import io.github.zenhelix.dependanger.maven.pom.util.MavenConstants

public data class PomWriterConfig(
    val prettyPrint: Boolean = true,
    val indent: String = MavenConstants.DEFAULT_INDENT,
    val includeXmlDeclaration: Boolean = true,
) {
    init {
        require(!prettyPrint || indent.isNotEmpty()) { "indent must not be empty when prettyPrint is enabled" }
    }
}
