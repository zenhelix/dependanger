package io.github.zenhelix.dependanger.maven.pom.writer

public data class PomWriterConfig(
    val prettyPrint: Boolean = true,
    val indent: String = "    ",
    val includeXmlDeclaration: Boolean = true,
)
