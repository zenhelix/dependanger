package io.github.zenhelix.dependanger.effective.spi

import kotlinx.serialization.Serializable

@Serializable
public enum class ReportFormat {
    JSON, YAML, MARKDOWN, HTML
}

@Serializable
public enum class ReportSection {
    SUMMARY, LIBRARIES, PLUGINS, BUNDLES, VERSIONS, UPDATES, COMPATIBILITY,
    VULNERABILITIES, DEPRECATED, LICENSES, TRANSITIVES, CONSTRAINTS, VALIDATION
}
