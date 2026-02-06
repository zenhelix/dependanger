package io.github.zenhelix.dependanger.effective.pipeline

public enum class ProcessingPhase(public val order: Int) {
    PROFILE(5),
    METADATA_CONVERSION(10),
    EXTRACTED_VERSIONS(12),
    BOM_IMPORT(15),
    LIBRARY_FILTER(20),
    VERSION_FALLBACK(25),
    VERSION_RESOLVER(30),
    BUNDLE_FILTER(40),
    PLUGIN_FILTER(45),
    PLUGIN(50),
    USED_VERSIONS(60),
    VALIDATION(65),
    COMPAT_RULES(66),
    UPDATE_CHECK(70),
    COMPATIBILITY_ANALYSIS(75),
    SECURITY_CHECK(80),
    LICENSE_CHECK(85),
    TRANSITIVE_RESOLVER(90),
}
