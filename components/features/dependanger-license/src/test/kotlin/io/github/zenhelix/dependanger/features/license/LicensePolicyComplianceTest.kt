package io.github.zenhelix.dependanger.features.license

import io.github.zenhelix.dependanger.features.license.DualLicensePolicy
import io.github.zenhelix.dependanger.features.license.LicenseCheckSettings
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.model.ResolvedVersion
import io.github.zenhelix.dependanger.effective.model.VersionSource
import io.github.zenhelix.dependanger.features.license.model.LicenseCategory
import io.github.zenhelix.dependanger.features.license.model.LicenseResult
import io.github.zenhelix.dependanger.features.license.model.LicenseSource
import io.github.zenhelix.dependanger.features.license.model.LicenseViolationType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LicensePolicyComplianceTest {

    private fun testLibrary(
        alias: String = "test-lib",
        group: String = "com.example",
        artifact: String = "test",
        version: String = "1.0.0",
    ): EffectiveLibrary = EffectiveLibrary(
        alias = alias,
        group = group,
        artifact = artifact,
        version = ResolvedVersion(
            alias = "test-version",
            value = version,
            source = VersionSource.DECLARED,
            originalRef = null,
        ),
        description = null,
        tags = emptySet(),
        requires = null,
        isDeprecated = false,
        deprecation = null,
        license = null,
        constraints = emptyList(),
        isPlatform = false,
    )

    private fun licenseResult(
        spdxId: String?,
        category: LicenseCategory,
        licenseName: String? = null,
        source: LicenseSource = LicenseSource.MAVEN_POM,
    ): LicenseResult = LicenseResult(
        spdxId = spdxId,
        licenseName = licenseName,
        source = source,
        category = category,
    )

    private fun settings(
        deniedLicenses: List<String> = emptyList(),
        allowedLicenses: List<String> = emptyList(),
        dualLicensePolicy: DualLicensePolicy = DualLicensePolicy.OR,
        warnOnCopyleft: Boolean = false,
        warnOnUnknown: Boolean = false,
    ): LicenseCheckSettings = LicenseCheckSettings.DEFAULT.copy(
        deniedLicenses = deniedLicenses,
        allowedLicenses = allowedLicenses,
        dualLicensePolicy = dualLicensePolicy,
        warnOnCopyleft = warnOnCopyleft,
        warnOnUnknown = warnOnUnknown,
    )

    @Nested
    inner class `denied license check` {

        @Test
        fun `single denied license with no alternative produces DENIED violation`() {
            val lib = testLibrary()
            val licenses = listOf(licenseResult("GPL-3.0-only", LicenseCategory.STRONG_COPYLEFT))
            val cfg = settings(deniedLicenses = listOf("GPL-3.0-only"))

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.violations).hasSize(1)
            assertThat(result.violations[0].violationType).isEqualTo(LicenseViolationType.DENIED)
        }

        @Test
        fun `denied license with permitted OR alternative produces no violation`() {
            val lib = testLibrary()
            val licenses = listOf(
                licenseResult("GPL-3.0-only", LicenseCategory.STRONG_COPYLEFT),
                licenseResult("MIT", LicenseCategory.PERMISSIVE),
            )
            val cfg = settings(
                deniedLicenses = listOf("GPL-3.0-only"),
                dualLicensePolicy = DualLicensePolicy.OR,
            )

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.violations).isEmpty()
        }

        @Test
        fun `all licenses denied under OR policy produces violation`() {
            val lib = testLibrary()
            val licenses = listOf(
                licenseResult("GPL-3.0-only", LicenseCategory.STRONG_COPYLEFT),
                licenseResult("AGPL-3.0-only", LicenseCategory.STRONG_COPYLEFT),
            )
            val cfg = settings(
                deniedLicenses = listOf("GPL-3.0-only", "AGPL-3.0-only"),
                dualLicensePolicy = DualLicensePolicy.OR,
            )

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.violations).hasSize(1)
            assertThat(result.violations[0].violationType).isEqualTo(LicenseViolationType.DENIED)
        }

        @Test
        fun `any license denied under AND policy produces violation`() {
            val lib = testLibrary()
            val licenses = listOf(
                licenseResult("GPL-3.0-only", LicenseCategory.STRONG_COPYLEFT),
                licenseResult("MIT", LicenseCategory.PERMISSIVE),
            )
            val cfg = settings(
                deniedLicenses = listOf("GPL-3.0-only"),
                dualLicensePolicy = DualLicensePolicy.AND,
            )

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.violations).hasSize(1)
            assertThat(result.violations[0].violationType).isEqualTo(LicenseViolationType.DENIED)
        }

        @Test
        fun `no violation when no licenses are in denied list`() {
            val lib = testLibrary()
            val licenses = listOf(licenseResult("MIT", LicenseCategory.PERMISSIVE))
            val cfg = settings(deniedLicenses = listOf("GPL-3.0-only"))

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.violations).isEmpty()
        }

        @Test
        fun `empty denied list produces no denied violations`() {
            val lib = testLibrary()
            val licenses = listOf(licenseResult("GPL-3.0-only", LicenseCategory.STRONG_COPYLEFT))
            val cfg = settings(deniedLicenses = emptyList())

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.violations.filter { it.violationType == LicenseViolationType.DENIED }).isEmpty()
        }
    }

    @Nested
    inner class `allowed license check` {

        @Test
        fun `license not in allowed list produces NOT_ALLOWED violation`() {
            val lib = testLibrary()
            val licenses = listOf(licenseResult("GPL-3.0-only", LicenseCategory.STRONG_COPYLEFT))
            val cfg = settings(allowedLicenses = listOf("MIT", "Apache-2.0"))

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.violations).hasSize(1)
            assertThat(result.violations[0].violationType).isEqualTo(LicenseViolationType.NOT_ALLOWED)
        }

        @Test
        fun `at least one license in allowed list under OR policy produces no violation`() {
            val lib = testLibrary()
            val licenses = listOf(
                licenseResult("GPL-3.0-only", LicenseCategory.STRONG_COPYLEFT),
                licenseResult("MIT", LicenseCategory.PERMISSIVE),
            )
            val cfg = settings(
                allowedLicenses = listOf("MIT"),
                dualLicensePolicy = DualLicensePolicy.OR,
            )

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.violations.filter { it.violationType == LicenseViolationType.NOT_ALLOWED }).isEmpty()
        }

        @Test
        fun `not all licenses in allowed list under AND policy produces violation`() {
            val lib = testLibrary()
            val licenses = listOf(
                licenseResult("MIT", LicenseCategory.PERMISSIVE),
                licenseResult("GPL-3.0-only", LicenseCategory.STRONG_COPYLEFT),
            )
            val cfg = settings(
                allowedLicenses = listOf("MIT"),
                dualLicensePolicy = DualLicensePolicy.AND,
            )

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.violations).anyMatch { it.violationType == LicenseViolationType.NOT_ALLOWED }
        }

        @Test
        fun `all licenses in allowed list under AND policy produces no violation`() {
            val lib = testLibrary()
            val licenses = listOf(
                licenseResult("MIT", LicenseCategory.PERMISSIVE),
                licenseResult("Apache-2.0", LicenseCategory.PERMISSIVE),
            )
            val cfg = settings(
                allowedLicenses = listOf("MIT", "Apache-2.0"),
                dualLicensePolicy = DualLicensePolicy.AND,
            )

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.violations.filter { it.violationType == LicenseViolationType.NOT_ALLOWED }).isEmpty()
        }

        @Test
        fun `empty allowed list produces no NOT_ALLOWED violations`() {
            val lib = testLibrary()
            val licenses = listOf(licenseResult("GPL-3.0-only", LicenseCategory.STRONG_COPYLEFT))
            val cfg = settings(allowedLicenses = emptyList())

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.violations.filter { it.violationType == LicenseViolationType.NOT_ALLOWED }).isEmpty()
        }
    }

    @Nested
    inner class `copyleft warning` {

        @Test
        fun `emits warning when warnOnCopyleft enabled and copyleft license present`() {
            val lib = testLibrary()
            val licenses = listOf(licenseResult("GPL-3.0-only", LicenseCategory.STRONG_COPYLEFT))
            val cfg = settings(warnOnCopyleft = true)

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.diagnostics.warnings).hasSize(1)
            assertThat(result.diagnostics.warnings[0].message).contains("copyleft")
        }

        @Test
        fun `emits warning for weak copyleft when warnOnCopyleft enabled`() {
            val lib = testLibrary()
            val licenses = listOf(licenseResult("LGPL-2.1-only", LicenseCategory.WEAK_COPYLEFT))
            val cfg = settings(warnOnCopyleft = true)

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.diagnostics.warnings).hasSize(1)
        }

        @Test
        fun `no warning when warnOnCopyleft disabled`() {
            val lib = testLibrary()
            val licenses = listOf(licenseResult("GPL-3.0-only", LicenseCategory.STRONG_COPYLEFT))
            val cfg = settings(warnOnCopyleft = false)

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.diagnostics.warnings).isEmpty()
        }

        @Test
        fun `no warning for permissive license when warnOnCopyleft enabled`() {
            val lib = testLibrary()
            val licenses = listOf(licenseResult("MIT", LicenseCategory.PERMISSIVE))
            val cfg = settings(warnOnCopyleft = true)

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.diagnostics.warnings).isEmpty()
        }
    }

    @Nested
    inner class `unknown license warning` {

        @Test
        fun `emits warning when warnOnUnknown enabled and all licenses are unknown`() {
            val lib = testLibrary()
            val licenses = listOf(licenseResult(null, LicenseCategory.UNKNOWN))
            val cfg = settings(warnOnUnknown = true)

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.diagnostics.warnings).hasSize(1)
            assertThat(result.diagnostics.warnings[0].message).contains("unknown")
        }

        @Test
        fun `no warning when at least one license is known`() {
            val lib = testLibrary()
            val licenses = listOf(
                licenseResult(null, LicenseCategory.UNKNOWN),
                licenseResult("MIT", LicenseCategory.PERMISSIVE),
            )
            val cfg = settings(warnOnUnknown = true)

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.diagnostics.warnings).isEmpty()
        }

        @Test
        fun `no warning when warnOnUnknown disabled`() {
            val lib = testLibrary()
            val licenses = listOf(licenseResult(null, LicenseCategory.UNKNOWN))
            val cfg = settings(warnOnUnknown = false)

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.diagnostics.warnings).isEmpty()
        }
    }

    @Nested
    inner class `dual license policy differences` {

        @Test
        fun `OR policy - denied only when ALL are denied, AND policy - denied when ANY is denied`() {
            val lib = testLibrary()
            val licenses = listOf(
                licenseResult("GPL-3.0-only", LicenseCategory.STRONG_COPYLEFT),
                licenseResult("MIT", LicenseCategory.PERMISSIVE),
            )

            val orResult = LicensePolicy.checkCompliance(
                lib, licenses, settings(deniedLicenses = listOf("GPL-3.0-only"), dualLicensePolicy = DualLicensePolicy.OR)
            )
            val andResult = LicensePolicy.checkCompliance(
                lib, licenses, settings(deniedLicenses = listOf("GPL-3.0-only"), dualLicensePolicy = DualLicensePolicy.AND)
            )

            assertThat(orResult.violations).isEmpty()
            assertThat(andResult.violations).hasSize(1)
        }

        @Test
        fun `OR policy - allowed if ANY is allowed, AND policy - allowed only if ALL are allowed`() {
            val lib = testLibrary()
            val licenses = listOf(
                licenseResult("MIT", LicenseCategory.PERMISSIVE),
                licenseResult("GPL-3.0-only", LicenseCategory.STRONG_COPYLEFT),
            )

            val orResult = LicensePolicy.checkCompliance(
                lib, licenses, settings(allowedLicenses = listOf("MIT"), dualLicensePolicy = DualLicensePolicy.OR)
            )
            val andResult = LicensePolicy.checkCompliance(
                lib, licenses, settings(allowedLicenses = listOf("MIT"), dualLicensePolicy = DualLicensePolicy.AND)
            )

            assertThat(orResult.violations.filter { it.violationType == LicenseViolationType.NOT_ALLOWED }).isEmpty()
            assertThat(andResult.violations.filter { it.violationType == LicenseViolationType.NOT_ALLOWED }).hasSize(1)
        }
    }

    @Nested
    inner class `violation metadata` {

        @Test
        fun `violation contains library coordinates and detected license`() {
            val lib = testLibrary(alias = "my-lib", group = "org.example", artifact = "core")
            val licenses = listOf(licenseResult("GPL-3.0-only", LicenseCategory.STRONG_COPYLEFT))
            val cfg = settings(deniedLicenses = listOf("GPL-3.0-only"))

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            val violation = result.violations[0]
            assertThat(violation.alias).isEqualTo("my-lib")
            assertThat(violation.group).isEqualTo("org.example")
            assertThat(violation.artifact).isEqualTo("core")
            assertThat(violation.detectedLicense).isEqualTo("GPL-3.0-only")
            assertThat(violation.category).isEqualTo(LicenseCategory.STRONG_COPYLEFT)
        }

        @Test
        fun `no violations and no diagnostics when settings are permissive`() {
            val lib = testLibrary()
            val licenses = listOf(licenseResult("MIT", LicenseCategory.PERMISSIVE))
            val cfg = settings()

            val result = LicensePolicy.checkCompliance(lib, licenses, cfg)

            assertThat(result.violations).isEmpty()
            assertThat(result.diagnostics.warnings).isEmpty()
            assertThat(result.diagnostics.errors).isEmpty()
        }
    }
}
