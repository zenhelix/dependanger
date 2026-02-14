package io.github.zenhelix.dependanger.features.license

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.DualLicensePolicy
import io.github.zenhelix.dependanger.core.model.LicenseCheckSettings
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.features.license.model.LicenseCategory
import io.github.zenhelix.dependanger.features.license.model.LicenseResult
import io.github.zenhelix.dependanger.features.license.model.LicenseViolation
import io.github.zenhelix.dependanger.features.license.model.LicenseViolationType
import io.github.zenhelix.dependanger.features.license.model.isCopyleft

/**
 * Result of policy compliance check for a single library.
 */
internal data class PolicyCheckResult(
    val violations: List<LicenseViolation>,
    val diagnostics: Diagnostics,
)

/**
 * Checks license compliance against configured policies.
 * Handles dual-license semantics (OR vs AND policy).
 */
internal object LicensePolicy {

    private const val PROCESSOR_ID: String = "license-check"

    /**
     * Checks all licenses for a library against the configured policies.
     *
     * Algorithm:
     * 1. Build the license expression string for display
     * 2. Denied check (depends on dualLicensePolicy):
     *    - OR policy: violation only if ALL licenses are in denied list (no permitted alternative)
     *    - AND policy: violation if ANY license is in denied list
     * 3. Allowed check (depends on dualLicensePolicy):
     *    - OR policy: OK if at least ONE license is in allowed list; violation if NONE are
     *    - AND policy: OK only if ALL licenses are in allowed list
     * 4. Copyleft warning: if warnOnCopyleft and ANY license is copyleft
     * 5. Unknown warning: if warnOnUnknown and ALL licenses are UNKNOWN
     */
    fun checkCompliance(
        lib: EffectiveLibrary,
        licenses: List<LicenseResult>,
        settings: LicenseCheckSettings,
    ): PolicyCheckResult {
        val violations = mutableListOf<LicenseViolation>()
        var diagnostics = Diagnostics.EMPTY

        val spdxIds = licenses.mapNotNull { it.spdxId }
        val separator = if (settings.dualLicensePolicy == DualLicensePolicy.AND) " AND " else " OR "
        val licenseExpression = spdxIds.joinToString(separator).ifEmpty { "UNKNOWN" }
        val worstCategory = worstCategory(licenses)

        // 1. Denied list check
        if (settings.deniedLicenses.isNotEmpty() && spdxIds.isNotEmpty()) {
            val hasDeniedViolation = when (settings.dualLicensePolicy) {
                DualLicensePolicy.OR  -> spdxIds.all { it in settings.deniedLicenses }
                DualLicensePolicy.AND -> spdxIds.any { it in settings.deniedLicenses }
            }
            if (hasDeniedViolation) {
                val message = when (settings.dualLicensePolicy) {
                    DualLicensePolicy.OR  -> "All licenses are in denied list (no permitted alternative): $licenseExpression"
                    DualLicensePolicy.AND -> "License in denied list (AND-policy, all apply): $licenseExpression"
                }
                violations.add(
                    LicenseViolation(
                        alias = lib.alias,
                        group = lib.group,
                        artifact = lib.artifact,
                        detectedLicense = licenseExpression,
                        category = worstCategory,
                        violationType = LicenseViolationType.DENIED,
                        message = message,
                    )
                )
            }
        }

        // 2. Allowed list check
        if (settings.allowedLicenses.isNotEmpty() && spdxIds.isNotEmpty()) {
            val isAllowed = when (settings.dualLicensePolicy) {
                DualLicensePolicy.OR  -> spdxIds.any { it in settings.allowedLicenses }
                DualLicensePolicy.AND -> spdxIds.all { it in settings.allowedLicenses }
            }
            if (!isAllowed) {
                val message = when (settings.dualLicensePolicy) {
                    DualLicensePolicy.OR  -> "None of the licenses are in allowed list: $licenseExpression"
                    DualLicensePolicy.AND -> "Not all licenses are in allowed list (AND-policy): $licenseExpression"
                }
                violations.add(
                    LicenseViolation(
                        alias = lib.alias,
                        group = lib.group,
                        artifact = lib.artifact,
                        detectedLicense = licenseExpression,
                        category = worstCategory,
                        violationType = LicenseViolationType.NOT_ALLOWED,
                        message = message,
                    )
                )
            }
        }

        // 3. Copyleft warning
        val copyleftLicenses = licenses.filter { it.category.isCopyleft }
        if (settings.warnOnCopyleft && copyleftLicenses.isNotEmpty()) {
            val copyleftIds = copyleftLicenses.mapNotNull { it.spdxId }.joinToString(", ")
            diagnostics = diagnostics + Diagnostics.warning(
                DiagnosticCodes.License.COPYLEFT_WARNING,
                "Library '${lib.alias}' has copyleft license(s): $copyleftIds",
                PROCESSOR_ID,
                mapOf("library" to lib.alias, "licenses" to copyleftIds),
            )
        }

        // 4. Unknown warning
        if (settings.warnOnUnknown && licenses.all { it.category == LicenseCategory.UNKNOWN }) {
            diagnostics = diagnostics + Diagnostics.warning(
                DiagnosticCodes.License.UNKNOWN_WARNING,
                "License for library '${lib.alias}' (${lib.group}:${lib.artifact}:${lib.version?.value}) is unknown",
                PROCESSOR_ID,
                mapOf("library" to lib.alias, "coordinate" to "${lib.group}:${lib.artifact}:${lib.version?.value}"),
            )
        }

        return PolicyCheckResult(
            violations = violations,
            diagnostics = diagnostics,
        )
    }

    private val CATEGORY_SEVERITY: Map<LicenseCategory, Int> = mapOf(
        LicenseCategory.PROPRIETARY to 5,
        LicenseCategory.UNKNOWN to 4,
        LicenseCategory.STRONG_COPYLEFT to 3,
        LicenseCategory.WEAK_COPYLEFT to 2,
        LicenseCategory.PUBLIC_DOMAIN to 1,
        LicenseCategory.PERMISSIVE to 0,
    )

    private fun worstCategory(licenses: List<LicenseResult>): LicenseCategory =
        licenses.maxByOrNull { CATEGORY_SEVERITY[it.category] ?: 0 }?.category ?: LicenseCategory.UNKNOWN
}
