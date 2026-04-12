package io.github.zenhelix.dependanger.effective

/**
 * Canonical identifiers for all metadata processors.
 */
public object ProcessorIds {
    public const val PROFILE: String = "profile"
    public const val METADATA_CONVERSION: String = "metadata-conversion"
    public const val EXTRACTED_VERSIONS: String = "extracted-versions"
    public const val BOM_IMPORT: String = "bom-import"
    public const val LIBRARY_FILTER: String = "library-filter"
    public const val VERSION_FALLBACK: String = "version-fallback"
    public const val VERSION_RESOLVER: String = "version-resolver"
    public const val BUNDLE_FILTER: String = "bundle-filter"
    public const val PLUGIN_FILTER: String = "plugin-filter"
    public const val USED_VERSIONS: String = "used-versions"
    public const val VALIDATION_DUPLICATES: String = "validation-duplicates"
    public const val VALIDATION_REFERENCES: String = "validation-references"
    public const val VALIDATION_CIRCULAR: String = "validation-circular"
    public const val COMPAT_RULES: String = "compat-rules"
}
