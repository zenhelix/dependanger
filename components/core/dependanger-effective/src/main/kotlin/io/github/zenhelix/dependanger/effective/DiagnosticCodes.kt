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

    public object Compatibility {
        public const val CUSTOM_RULE_DEFERRED: String = "COMPAT_CUSTOM_RULE_DEFERRED"
        public const val NO_CUSTOM_RULES: String = "COMPAT_NO_CUSTOM_RULES"
        public const val CUSTOM_RULE: String = "COMPAT_CUSTOM_RULE"
        public const val CUSTOM_HANDLER_NOT_FOUND: String = "COMPAT_CUSTOM_HANDLER_NOT_FOUND"
        public const val CUSTOM_RULE_FAILED: String = "COMPAT_CUSTOM_RULE_FAILED"
    }
}
