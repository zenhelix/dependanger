package io.github.zenhelix.dependanger.effective.pipeline

public enum class ExecutionMode {
    SEQUENTIAL,
    PARALLEL_IO,
    PARALLEL_COMPUTE,
}

public open class ProcessingPhase(
    public val name: String,
    public val executionMode: ExecutionMode,
) {
    override fun equals(other: Any?): Boolean = other is ProcessingPhase && name == other.name
    override fun hashCode(): Int = name.hashCode()
    override fun toString(): String = "ProcessingPhase($name)"

    public companion object {
        public val PROFILE: ProcessingPhase = ProcessingPhase("PROFILE", ExecutionMode.SEQUENTIAL)
        public val METADATA_CONVERSION: ProcessingPhase = ProcessingPhase("METADATA_CONVERSION", ExecutionMode.SEQUENTIAL)
        public val EXTRACTED_VERSIONS: ProcessingPhase = ProcessingPhase("EXTRACTED_VERSIONS", ExecutionMode.SEQUENTIAL)
        public val BOM_IMPORT: ProcessingPhase = ProcessingPhase("BOM_IMPORT", ExecutionMode.SEQUENTIAL)
        public val LIBRARY_FILTER: ProcessingPhase = ProcessingPhase("LIBRARY_FILTER", ExecutionMode.SEQUENTIAL)
        public val VERSION_FALLBACK: ProcessingPhase = ProcessingPhase("VERSION_FALLBACK", ExecutionMode.SEQUENTIAL)
        public val VERSION_RESOLVER: ProcessingPhase = ProcessingPhase("VERSION_RESOLVER", ExecutionMode.SEQUENTIAL)
        public val BUNDLE_FILTER: ProcessingPhase = ProcessingPhase("BUNDLE_FILTER", ExecutionMode.SEQUENTIAL)
        public val PLUGIN_FILTER: ProcessingPhase = ProcessingPhase("PLUGIN_FILTER", ExecutionMode.SEQUENTIAL)
        public val PLUGIN: ProcessingPhase = ProcessingPhase("PLUGIN", ExecutionMode.SEQUENTIAL)
        public val USED_VERSIONS: ProcessingPhase = ProcessingPhase("USED_VERSIONS", ExecutionMode.SEQUENTIAL)
        public val VALIDATION: ProcessingPhase = ProcessingPhase("VALIDATION", ExecutionMode.SEQUENTIAL)
        public val COMPAT_RULES: ProcessingPhase = ProcessingPhase("COMPAT_RULES", ExecutionMode.SEQUENTIAL)
        public val UPDATE_CHECK: ProcessingPhase = ProcessingPhase("UPDATE_CHECK", ExecutionMode.PARALLEL_IO)
        public val TRANSITIVE_RESOLVER: ProcessingPhase = ProcessingPhase("TRANSITIVE_RESOLVER", ExecutionMode.SEQUENTIAL)
        public val COMPATIBILITY_ANALYSIS: ProcessingPhase = ProcessingPhase("COMPATIBILITY_ANALYSIS", ExecutionMode.SEQUENTIAL)
        public val SECURITY_CHECK: ProcessingPhase = ProcessingPhase("SECURITY_CHECK", ExecutionMode.PARALLEL_IO)
        public val LICENSE_CHECK: ProcessingPhase = ProcessingPhase("LICENSE_CHECK", ExecutionMode.PARALLEL_IO)
    }
}
