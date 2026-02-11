package io.github.zenhelix.dependanger.effective.pipeline

public enum class ExecutionMode {
    SEQUENTIAL,
    PARALLEL_IO,
    PARALLEL_COMPUTE,
}

public open class ProcessingPhase(
    public val name: String,
    public val order: Int,
    public val executionMode: ExecutionMode,
) {
    override fun equals(other: Any?): Boolean = other is ProcessingPhase && name == other.name
    override fun hashCode(): Int = name.hashCode()
    override fun toString(): String = "ProcessingPhase($name, order=$order)"

    public companion object {
        public val PROFILE: ProcessingPhase = ProcessingPhase("PROFILE", 5, ExecutionMode.SEQUENTIAL)
        public val METADATA_CONVERSION: ProcessingPhase = ProcessingPhase("METADATA_CONVERSION", 10, ExecutionMode.SEQUENTIAL)
        public val EXTRACTED_VERSIONS: ProcessingPhase = ProcessingPhase("EXTRACTED_VERSIONS", 12, ExecutionMode.SEQUENTIAL)
        public val BOM_IMPORT: ProcessingPhase = ProcessingPhase("BOM_IMPORT", 15, ExecutionMode.SEQUENTIAL)
        public val LIBRARY_FILTER: ProcessingPhase = ProcessingPhase("LIBRARY_FILTER", 20, ExecutionMode.SEQUENTIAL)
        public val VERSION_FALLBACK: ProcessingPhase = ProcessingPhase("VERSION_FALLBACK", 25, ExecutionMode.SEQUENTIAL)
        public val VERSION_RESOLVER: ProcessingPhase = ProcessingPhase("VERSION_RESOLVER", 30, ExecutionMode.SEQUENTIAL)
        public val BUNDLE_FILTER: ProcessingPhase = ProcessingPhase("BUNDLE_FILTER", 40, ExecutionMode.SEQUENTIAL)
        public val PLUGIN_FILTER: ProcessingPhase = ProcessingPhase("PLUGIN_FILTER", 45, ExecutionMode.SEQUENTIAL)
        public val PLUGIN: ProcessingPhase = ProcessingPhase("PLUGIN", 50, ExecutionMode.SEQUENTIAL)
        public val USED_VERSIONS: ProcessingPhase = ProcessingPhase("USED_VERSIONS", 60, ExecutionMode.SEQUENTIAL)
        public val VALIDATION: ProcessingPhase = ProcessingPhase("VALIDATION", 65, ExecutionMode.SEQUENTIAL)
        public val COMPAT_RULES: ProcessingPhase = ProcessingPhase("COMPAT_RULES", 66, ExecutionMode.SEQUENTIAL)
        public val UPDATE_CHECK: ProcessingPhase = ProcessingPhase("UPDATE_CHECK", 100, ExecutionMode.PARALLEL_IO)
        public val TRANSITIVE_RESOLVER: ProcessingPhase = ProcessingPhase("TRANSITIVE_RESOLVER", 101, ExecutionMode.SEQUENTIAL)
        public val COMPATIBILITY_ANALYSIS: ProcessingPhase = ProcessingPhase("COMPATIBILITY_ANALYSIS", 110, ExecutionMode.SEQUENTIAL)
        public val SECURITY_CHECK: ProcessingPhase = ProcessingPhase("SECURITY_CHECK", 120, ExecutionMode.PARALLEL_IO)
        public val LICENSE_CHECK: ProcessingPhase = ProcessingPhase("LICENSE_CHECK", 130, ExecutionMode.PARALLEL_IO)
    }
}
