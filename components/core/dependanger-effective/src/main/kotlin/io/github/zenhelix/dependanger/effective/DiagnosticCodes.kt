package io.github.zenhelix.dependanger.effective

/**
 * Canonical diagnostic message codes used by processors.
 */
public object DiagnosticCodes {

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

    public object Compatibility {
        public const val CUSTOM_RULE_DEFERRED: String = "COMPAT_CUSTOM_RULE_DEFERRED"
        public const val NO_CUSTOM_RULES: String = "COMPAT_NO_CUSTOM_RULES"
        public const val CUSTOM_RULE: String = "COMPAT_CUSTOM_RULE"
        public const val CUSTOM_HANDLER_NOT_FOUND: String = "COMPAT_CUSTOM_HANDLER_NOT_FOUND"
        public const val CUSTOM_RULE_FAILED: String = "COMPAT_CUSTOM_RULE_FAILED"
    }
}
