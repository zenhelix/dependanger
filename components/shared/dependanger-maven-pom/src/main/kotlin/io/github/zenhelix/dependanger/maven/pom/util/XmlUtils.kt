package io.github.zenhelix.dependanger.maven.pom.util

/**
 * Escapes XML special characters: & < > " '
 */
public fun String.escapeXml(): String = this
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

/**
 * Makes text safe for XML comments by replacing -- with - -
 */
public fun String.escapeXmlComment(): String = this.replace("--", "- -")
