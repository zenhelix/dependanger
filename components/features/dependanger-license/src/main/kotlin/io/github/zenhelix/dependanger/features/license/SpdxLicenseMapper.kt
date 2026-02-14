package io.github.zenhelix.dependanger.features.license

import io.github.zenhelix.dependanger.features.license.model.LicenseCategory

/**
 * Maps license names to canonical SPDX identifiers and categorizes them.
 */
public object SpdxLicenseMapper {

    /**
     * Normalizes a license name to its canonical SPDX identifier.
     * Returns null if the name cannot be mapped.
     */
    public fun normalize(licenseName: String): String? {
        val trimmed = licenseName.trim()

        // Exact match (already a valid SPDX ID)
        if (trimmed in SPDX_IDS) return trimmed

        // Case-insensitive alias lookup
        val normalized = LICENSE_ALIASES[trimmed.lowercase()]
        if (normalized != null) return normalized

        // Pattern matching
        return matchPattern(trimmed)
    }

    /**
     * Determines the category for a given SPDX identifier.
     *
     * Logic:
     * - Known SPDX ID in category map → mapped category
     * - Has spdxId but not in map → PROPRIETARY (non-standard license identifier)
     * - No spdxId but has licenseName → PROPRIETARY (license declared but not SPDX-recognized)
     * - Neither spdxId nor licenseName → UNKNOWN (no license information at all)
     */
    public fun categorize(spdxId: String?, licenseName: String? = null): LicenseCategory {
        if (spdxId != null) return CATEGORY_MAP[spdxId] ?: LicenseCategory.PROPRIETARY
        return if (licenseName != null) LicenseCategory.PROPRIETARY else LicenseCategory.UNKNOWN
    }

    private fun matchPattern(name: String): String? {
        val lower = name.lowercase()
        return when {
            lower.contains("apache") && lower.contains("2")          -> "Apache-2.0"
            lower.contains("mit")                                    -> "MIT"
            lower.contains("bsd") && lower.contains("3")             -> "BSD-3-Clause"
            lower.contains("bsd") && lower.contains("2")             -> "BSD-2-Clause"
            lower.contains("bsd")                                    -> "BSD-3-Clause"
            lower.contains("lgpl") && lower.contains("3")            -> "LGPL-3.0-only"
            lower.contains("lgpl") && lower.contains("2")            -> "LGPL-2.1-only"
            lower.contains("agpl") && lower.contains("3")            -> "AGPL-3.0-only"
            lower.contains("gpl") && lower.contains("3")             -> "GPL-3.0-only"
            lower.contains("gpl") && lower.contains("2")             -> "GPL-2.0-only"
            lower.contains("eclipse") && lower.contains("2")         -> "EPL-2.0"
            lower.contains("eclipse") && lower.contains("1")         -> "EPL-1.0"
            lower.contains("mozilla") && lower.contains("2")         -> "MPL-2.0"
            lower.contains("common development and distribution")    -> "CDDL-1.0"
            lower.contains("cddl")                                   -> "CDDL-1.0"
            lower.contains("unlicense") || lower == "the unlicense"  -> "Unlicense"
            lower.contains("public domain") || lower.contains("cc0") -> "CC0-1.0"
            else                                                     -> null
        }
    }

    private val LICENSE_ALIASES: Map<String, String> = mapOf(
        // Apache
        "the apache license, version 2.0" to "Apache-2.0",
        "apache license, version 2.0" to "Apache-2.0",
        "apache license 2.0" to "Apache-2.0",
        "apache license version 2.0" to "Apache-2.0",
        "apache-2.0" to "Apache-2.0",
        "apache 2" to "Apache-2.0",
        "apache 2.0" to "Apache-2.0",
        "apache software license, version 2.0" to "Apache-2.0",
        "apache software license - version 2.0" to "Apache-2.0",
        "the apache software license, version 2.0" to "Apache-2.0",
        "asl 2.0" to "Apache-2.0",
        "asl, version 2" to "Apache-2.0",

        // MIT
        "the mit license" to "MIT",
        "mit license" to "MIT",
        "mit" to "MIT",
        "the mit license (mit)" to "MIT",

        // BSD
        "bsd 3-clause license" to "BSD-3-Clause",
        "bsd license" to "BSD-3-Clause",
        "new bsd license" to "BSD-3-Clause",
        "the new bsd license" to "BSD-3-Clause",
        "bsd 3-clause" to "BSD-3-Clause",
        "bsd-3-clause" to "BSD-3-Clause",
        "3-clause bsd license" to "BSD-3-Clause",
        "revised bsd license" to "BSD-3-Clause",
        "bsd 2-clause license" to "BSD-2-Clause",
        "simplified bsd license" to "BSD-2-Clause",
        "bsd-2-clause" to "BSD-2-Clause",
        "the 2-clause bsd license" to "BSD-2-Clause",
        "freebsd license" to "BSD-2-Clause",

        // GPL
        "gnu general public license v3.0" to "GPL-3.0-only",
        "gnu general public license, version 3" to "GPL-3.0-only",
        "gnu general public license v2.0" to "GPL-2.0-only",
        "gnu general public license, version 2" to "GPL-2.0-only",
        "gpl-3.0" to "GPL-3.0-only",
        "gpl-2.0" to "GPL-2.0-only",
        "gpl v3" to "GPL-3.0-only",
        "gpl v2" to "GPL-2.0-only",
        "gplv3" to "GPL-3.0-only",
        "gplv2" to "GPL-2.0-only",
        "gpl-3.0-only" to "GPL-3.0-only",
        "gpl-2.0-only" to "GPL-2.0-only",
        "gpl-3.0-or-later" to "GPL-3.0-or-later",
        "gpl-2.0-or-later" to "GPL-2.0-or-later",

        // LGPL
        "gnu lesser general public license v2.1" to "LGPL-2.1-only",
        "gnu lesser general public license v3.0" to "LGPL-3.0-only",
        "gnu lesser general public license, version 2.1" to "LGPL-2.1-only",
        "lgpl-2.1" to "LGPL-2.1-only",
        "lgpl-3.0" to "LGPL-3.0-only",
        "lgpl-2.1-only" to "LGPL-2.1-only",
        "lgpl-3.0-only" to "LGPL-3.0-only",
        "lgpl 2.1" to "LGPL-2.1-only",
        "lgpl 3.0" to "LGPL-3.0-only",

        // AGPL
        "gnu affero general public license v3.0" to "AGPL-3.0-only",
        "agpl-3.0" to "AGPL-3.0-only",
        "agpl-3.0-only" to "AGPL-3.0-only",

        // Eclipse
        "eclipse public license 1.0" to "EPL-1.0",
        "eclipse public license 2.0" to "EPL-2.0",
        "eclipse public license - v 1.0" to "EPL-1.0",
        "eclipse public license - v 2.0" to "EPL-2.0",
        "eclipse public license v1.0" to "EPL-1.0",
        "eclipse public license v2.0" to "EPL-2.0",
        "epl-1.0" to "EPL-1.0",
        "epl-2.0" to "EPL-2.0",
        "eclipse distribution license - v 1.0" to "BSD-3-Clause",

        // Mozilla
        "mozilla public license 2.0" to "MPL-2.0",
        "mozilla public license, version 2.0" to "MPL-2.0",
        "mpl 2.0" to "MPL-2.0",
        "mpl-2.0" to "MPL-2.0",

        // Other
        "isc license" to "ISC",
        "isc" to "ISC",
        "the unlicense" to "Unlicense",
        "unlicense" to "Unlicense",
        "cc0 1.0 universal" to "CC0-1.0",
        "cc0-1.0" to "CC0-1.0",
        "public domain" to "CC0-1.0",
        "do what the f*ck you want to public license" to "WTFPL",
        "wtfpl" to "WTFPL",
        "common development and distribution license 1.0" to "CDDL-1.0",
        "cddl 1.0" to "CDDL-1.0",
        "cddl-1.0" to "CDDL-1.0",
        "creative commons attribution 4.0" to "CC-BY-4.0",
        "the json license" to "JSON",
        "bouncy castle licence" to "MIT",
        "indiana university extreme! lab software license" to "Apache-2.0",
        "edl 1.0" to "BSD-3-Clause",
    )

    private val CATEGORY_MAP: Map<String, LicenseCategory> = mapOf(
        // Permissive
        "MIT" to LicenseCategory.PERMISSIVE,
        "Apache-2.0" to LicenseCategory.PERMISSIVE,
        "BSD-2-Clause" to LicenseCategory.PERMISSIVE,
        "BSD-3-Clause" to LicenseCategory.PERMISSIVE,
        "ISC" to LicenseCategory.PERMISSIVE,
        "JSON" to LicenseCategory.PERMISSIVE,
        "CC-BY-4.0" to LicenseCategory.PERMISSIVE,
        "Zlib" to LicenseCategory.PERMISSIVE,
        "BSL-1.0" to LicenseCategory.PERMISSIVE,

        // Public domain
        "Unlicense" to LicenseCategory.PUBLIC_DOMAIN,
        "CC0-1.0" to LicenseCategory.PUBLIC_DOMAIN,
        "WTFPL" to LicenseCategory.PUBLIC_DOMAIN,

        // Weak copyleft
        "LGPL-2.1-only" to LicenseCategory.WEAK_COPYLEFT,
        "LGPL-3.0-only" to LicenseCategory.WEAK_COPYLEFT,
        "LGPL-2.1-or-later" to LicenseCategory.WEAK_COPYLEFT,
        "LGPL-3.0-or-later" to LicenseCategory.WEAK_COPYLEFT,
        "MPL-2.0" to LicenseCategory.WEAK_COPYLEFT,
        "EPL-1.0" to LicenseCategory.WEAK_COPYLEFT,
        "EPL-2.0" to LicenseCategory.WEAK_COPYLEFT,
        "CDDL-1.0" to LicenseCategory.WEAK_COPYLEFT,

        // Strong copyleft
        "GPL-2.0-only" to LicenseCategory.STRONG_COPYLEFT,
        "GPL-3.0-only" to LicenseCategory.STRONG_COPYLEFT,
        "GPL-2.0-or-later" to LicenseCategory.STRONG_COPYLEFT,
        "GPL-3.0-or-later" to LicenseCategory.STRONG_COPYLEFT,
        "AGPL-3.0-only" to LicenseCategory.STRONG_COPYLEFT,
        "AGPL-3.0-or-later" to LicenseCategory.STRONG_COPYLEFT,
    )

    private val SPDX_IDS: Set<String> = CATEGORY_MAP.keys
}
