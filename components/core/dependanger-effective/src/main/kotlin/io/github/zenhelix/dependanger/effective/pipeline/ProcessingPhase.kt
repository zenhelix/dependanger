package io.github.zenhelix.dependanger.effective.pipeline

public enum class ExecutionMode {
    SEQUENTIAL,
    PARALLEL_IO,
    PARALLEL_COMPUTE,
}

public enum class ProcessingPhase(
    public val order: Int,
    public val executionMode: ExecutionMode = ExecutionMode.SEQUENTIAL,
) {
    PROFILE(5),
    METADATA_CONVERSION(10),
    EXTRACTED_VERSIONS(12),
    BOM_IMPORT(15, ExecutionMode.PARALLEL_IO),
    LIBRARY_FILTER(20),
    VERSION_FALLBACK(25),
    VERSION_RESOLVER(30),
    BUNDLE_FILTER(40),
    PLUGIN_FILTER(45),
    PLUGIN(50),
    USED_VERSIONS(60),
    VALIDATION(65),
    COMPAT_RULES(66),
    UPDATE_CHECK(70, ExecutionMode.PARALLEL_IO),
    COMPATIBILITY_ANALYSIS(75),
    SECURITY_CHECK(80, ExecutionMode.PARALLEL_IO),
    LICENSE_CHECK(85, ExecutionMode.PARALLEL_IO),
    TRANSITIVE_RESOLVER(90, ExecutionMode.PARALLEL_IO),
}
