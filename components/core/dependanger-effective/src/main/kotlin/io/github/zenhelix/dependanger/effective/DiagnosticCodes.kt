package io.github.zenhelix.dependanger.effective

/**
 * Canonical diagnostic message codes used by processors.
 */
public object DiagnosticCodes {

    public object Bom {
        public const val SNAPSHOT_WARNING: String = "BOM_SNAPSHOT_WARNING"
        public const val UNRESOLVED_VERSION: String = "BOM_UNRESOLVED_VERSION"
        public const val VERSION_RANGE: String = "BOM_VERSION_RANGE"
        public const val CACHE_CORRUPT: String = "BOM_CACHE_CORRUPT"
        public const val CIRCULAR: String = "CIRCULAR_BOM"
        public const val DEPTH_EXCEEDED: String = "BOM_DEPTH_EXCEEDED"
        public const val VERSION_IMPORTED: String = "BOM_VERSION_IMPORTED"
        public const val NO_DEPS: String = "BOM_NO_DEPS"
        public const val STALE_CACHE: String = "BOM_STALE_CACHE"
        public const val FETCH_FAILED: String = "BOM_FETCH_FAILED"
        public const val AUTH_REQUIRED: String = "BOM_AUTH_REQUIRED"
        public const val INVALID_XML: String = "INVALID_BOM_XML"
        public const val UNRESOLVED_PROPERTY: String = "UNRESOLVED_BOM_PROPERTY"
        public const val DUPLICATE_ENTRY: String = "BOM_DUPLICATE_ENTRY"
        public const val CACHE_READONLY: String = "BOM_CACHE_READONLY"
    }

    public object Profile {
        public const val APPLIED: String = "PROFILE_APPLIED"
        public const val NOT_FOUND: String = "PROFILE_NOT_FOUND"
    }

    public object Version {
        public const val RESOLVED: String = "VERSION_RESOLVED"
        public const val UNRESOLVED: String = "UNRESOLVED_VERSION"
        public const val EXTRACTED_CREATED: String = "EXTRACTED_VERSION_CREATED"
        public const val FALLBACK_APPLIED: String = "FALLBACK_APPLIED"
        public const val UNUSED_REMOVED: String = "UNUSED_VERSION_REMOVED"
    }

    public object Library {
        public const val FILTERED: String = "LIBRARY_FILTERED"
    }

    public object Plugin {
        public const val FILTERED: String = "PLUGIN_FILTERED"
        public const val VERSION_UNRESOLVED: String = "PLUGIN_VERSION_UNRESOLVED"
    }

    public object Bundle {
        public const val LIBRARY_MISSING: String = "BUNDLE_LIBRARY_MISSING"
        public const val EMPTIED: String = "BUNDLE_EMPTIED"
    }

    public object Validation {
        public const val DUPLICATE_ALIAS: String = "VALIDATION_DUPLICATE_ALIAS"
        public const val UNRESOLVED_REF: String = "VALIDATION_UNRESOLVED_REF"
        public const val BUNDLE_REF_MISSING: String = "VALIDATION_BUNDLE_REF_MISSING"
        public const val CIRCULAR_EXTENDS: String = "VALIDATION_CIRCULAR_EXTENDS"
        public const val DEPRECATED_REF: String = "VALIDATION_DEPRECATED_REF"
        public const val INVALID_COORDINATES: String = "VALIDATION_INVALID_COORDINATES"
    }

    public object Update {
        public const val UPDATES_AVAILABLE: String = "UPDATES_AVAILABLE"
        public const val ALL_UP_TO_DATE: String = "ALL_UP_TO_DATE"
        public const val REPO_UNREACHABLE: String = "UPDATE_REPO_UNREACHABLE"
        public const val TIMEOUT: String = "UPDATE_TIMEOUT"
        public const val LIB_NOT_FOUND: String = "UPDATE_LIB_NOT_FOUND"
        public const val INVALID_VERSION: String = "UPDATE_INVALID_VERSION"
        public const val RATE_LIMITED: String = "UPDATE_RATE_LIMITED"
    }

    public object License {
        public const val CHECK_COMPLETE: String = "LICENSE_CHECK_COMPLETE"
        public const val NO_LIBS: String = "LICENSE_NO_LIBS"
        public const val ALL_COMPLIANT: String = "LICENSE_ALL_COMPLIANT"
        public const val VIOLATIONS_FOUND: String = "LICENSE_VIOLATIONS_FOUND"
        public const val DENIED_FOUND: String = "LICENSE_DENIED_FOUND"
        public const val UNKNOWN_FOUND: String = "LICENSE_UNKNOWN_FOUND"
        public const val COPYLEFT_FOUND: String = "LICENSE_COPYLEFT_FOUND"
        public const val COPYLEFT_WARNING: String = "LICENSE_COPYLEFT"
        public const val UNKNOWN_WARNING: String = "LICENSE_UNKNOWN"
        public const val RESOLVE_FAILED: String = "LICENSE_RESOLVE_FAILED"
        public const val CACHE_CORRUPTED: String = "LICENSE_CACHE_CORRUPTED"
        public const val STALE_CACHE: String = "LICENSE_STALE_CACHE"
        public const val INCLUDE_TRANSITIVES_NOT_SUPPORTED: String = "LICENSE_INCLUDE_TRANSITIVES_NOT_SUPPORTED"
    }

    public object Transitive {
        public const val RESOLVED: String = "TRANSITIVE_RESOLVED"
        public const val NO_LIBS: String = "TRANSITIVE_NO_LIBS"
        public const val CONFLICT: String = "TRANSITIVE_CONFLICT"
        public const val POM_NOT_FOUND: String = "TRANSITIVE_POM_NOT_FOUND"
        public const val REPO_UNAVAILABLE: String = "TRANSITIVE_REPO_UNAVAILABLE"
        public const val CYCLE_DETECTED: String = "TRANSITIVE_CYCLE_DETECTED"
        public const val MAX_DEPTH: String = "TRANSITIVE_MAX_DEPTH"
        public const val LARGE_TREE: String = "TRANSITIVE_LARGE_TREE"
        public const val MAX_EXCEEDED: String = "TRANSITIVE_MAX_EXCEEDED"
        public const val UNUSED_CONSTRAINT: String = "TRANSITIVE_UNUSED_CONSTRAINT"
        public const val SNAPSHOT: String = "TRANSITIVE_SNAPSHOT"
        public const val TIMEOUT: String = "TRANSITIVE_TIMEOUT"
        public const val STALE_CACHE: String = "TRANSITIVE_STALE_CACHE"
        public const val POM_PARSE_FAILED: String = "TRANSITIVE_POM_PARSE_FAILED"
    }

    public object Compatibility {
        public const val CUSTOM_RULE_DEFERRED: String = "COMPAT_CUSTOM_RULE_DEFERRED"
        public const val NO_CUSTOM_RULES: String = "COMPAT_NO_CUSTOM_RULES"
        public const val CUSTOM_RULE: String = "COMPAT_CUSTOM_RULE"
        public const val CUSTOM_HANDLER_NOT_FOUND: String = "COMPAT_CUSTOM_HANDLER_NOT_FOUND"
        public const val CUSTOM_RULE_FAILED: String = "COMPAT_CUSTOM_RULE_FAILED"
    }

    public object Security {
        public const val SCAN_COMPLETE: String = "SECURITY_SCAN_COMPLETE"
        public const val NO_VULNS: String = "SECURITY_NO_VULNS"
        public const val VULNERABILITY_FOUND: String = "SECURITY_VULNERABILITY_FOUND"
        public const val API_UNREACHABLE: String = "SECURITY_API_UNREACHABLE"
        public const val STALE_CACHE: String = "SECURITY_STALE_CACHE"
        public const val TIMEOUT: String = "SECURITY_TIMEOUT"
        public const val RATE_LIMITED: String = "SECURITY_RATE_LIMITED"
        public const val INVALID_CVSS: String = "SECURITY_INVALID_CVSS"
        public const val CVSS_V2: String = "SECURITY_CVSS_V2"
    }
}
