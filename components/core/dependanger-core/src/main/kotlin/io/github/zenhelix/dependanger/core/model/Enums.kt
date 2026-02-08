package io.github.zenhelix.dependanger.core.model

import kotlinx.serialization.Serializable

@Serializable
public enum class ValidationAction {
    FAIL, WARN, INFO, IGNORE
}

@Serializable
public enum class Severity {
    ERROR, WARNING, INFO
}

@Serializable
public enum class VersionConstraintType {
    SAME_VERSION, SAME_MAJOR, SAME_MAJOR_MINOR
}

@Serializable
public enum class KmpTarget {
    JVM, JS, ANDROID, IOS, MACOS, LINUX, MINGW, NATIVE, WASM_JS, WASM_WASI
}

@Serializable
public enum class ConflictResolutionStrategy {
    HIGHEST, FIRST, FAIL
}

@Serializable
public enum class ReportFormat {
    JSON, YAML, MARKDOWN, HTML
}

@Serializable
public enum class ReportSection {
    SUMMARY, LIBRARIES, PLUGINS, BUNDLES, VERSIONS, UPDATES, COMPATIBILITY,
    VULNERABILITIES, DEPRECATED, LICENSES, TRANSITIVES, CONSTRAINTS, VALIDATION
}
