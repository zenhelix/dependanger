package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.util.UpdateType
import io.github.zenhelix.dependanger.features.license.model.LicenseCategory
import io.github.zenhelix.dependanger.features.license.model.LicenseViolation
import io.github.zenhelix.dependanger.features.license.model.LicenseViolationType
import io.github.zenhelix.dependanger.features.security.model.VulnerabilityInfo
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitySeverity
import io.github.zenhelix.dependanger.features.updates.model.UpdateAvailableInfo
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LicenseComplianceBehaviorTest {

    private fun dependanger(
        preset: ProcessingPreset = ProcessingPreset.DEFAULT,
        dslBlock: DependangerDsl.() -> Unit,
    ): Dependanger = Dependanger(dslBlock) { preset(preset) }


    @Nested
    inner class `License violations via result extensions` {

        @Test
        fun `denied license violation accessible via result licenseViolations`() = runTest {
            val violation = LicenseViolation(
                alias = "gpl-lib",
                group = "com.example",
                artifact = "gpl-lib",
                detectedLicense = "GPL-3.0",
                category = LicenseCategory.STRONG_COPYLEFT,
                violationType = LicenseViolationType.DENIED,
                message = "GPL-3.0 is not allowed in this project",
            )

            val result = Dependanger({
                                         libraries { library("gpl-lib", "com.example:gpl-lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeLicenseCheck { listOf(violation) })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).hasSize(1)
            assertThat(result.licenseViolations[0].alias).isEqualTo("gpl-lib")
            assertThat(result.licenseViolations[0].detectedLicense).isEqualTo("GPL-3.0")
            assertThat(result.licenseViolations[0].violationType).isEqualTo(LicenseViolationType.DENIED)
        }

        @Test
        fun `multiple violations from different libraries`() = runTest {
            val result = Dependanger({
                                         libraries {
                                             library("lib-a", "com.a:lib-a:1.0")
                                             library("lib-b", "com.b:lib-b:2.0")
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeLicenseCheck { metadata ->
                    metadata.libraries.values.map { lib ->
                        LicenseViolation(
                            alias = lib.alias,
                            group = lib.group,
                            artifact = lib.artifact,
                            detectedLicense = "AGPL-3.0",
                            category = LicenseCategory.STRONG_COPYLEFT,
                            violationType = LicenseViolationType.DENIED,
                            message = "AGPL-3.0 is denied",
                        )
                    }
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).hasSize(2)
            assertThat(result.licenseViolations.map { it.alias })
                .containsExactlyInAnyOrder("lib-a", "lib-b")
        }

        @Test
        fun `no violations when FakeProcessor returns empty list`() = runTest {
            val result = Dependanger({
                                         libraries { library("clean-lib", "com.clean:lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeLicenseCheck { emptyList() })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).isEmpty()
        }
    }

    @Nested
    inner class `License categories` {

        @Test
        fun `PERMISSIVE category violation has correct category`() = runTest {
            val result = Dependanger({
                                         libraries { library("mit-lib", "com.example:mit-lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeLicenseCheck {
                    listOf(
                        LicenseViolation(
                            alias = "mit-lib",
                            group = "com.example",
                            artifact = "mit-lib",
                            detectedLicense = "MIT",
                            category = LicenseCategory.PERMISSIVE,
                            violationType = LicenseViolationType.NOT_ALLOWED,
                            message = "MIT is not allowed in this distribution",
                        )
                    )
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).hasSize(1)
            assertThat(result.licenseViolations[0].category).isEqualTo(LicenseCategory.PERMISSIVE)
        }

        @Test
        fun `STRONG_COPYLEFT category violation has correct category`() = runTest {
            val result = Dependanger({
                                         libraries { library("gpl-lib", "com.example:gpl-lib:2.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeLicenseCheck {
                    listOf(
                        LicenseViolation(
                            alias = "gpl-lib",
                            group = "com.example",
                            artifact = "gpl-lib",
                            detectedLicense = "GPL-3.0-only",
                            category = LicenseCategory.STRONG_COPYLEFT,
                            violationType = LicenseViolationType.DENIED,
                            message = "GPL-3.0-only is denied",
                        )
                    )
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).hasSize(1)
            assertThat(result.licenseViolations[0].category).isEqualTo(LicenseCategory.STRONG_COPYLEFT)
        }

        @Test
        fun `UNKNOWN category violation has correct category`() = runTest {
            val result = Dependanger({
                                         libraries { library("mystery-lib", "com.example:mystery-lib:0.1.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeLicenseCheck {
                    listOf(
                        LicenseViolation(
                            alias = "mystery-lib",
                            group = "com.example",
                            artifact = "mystery-lib",
                            detectedLicense = null,
                            category = LicenseCategory.UNKNOWN,
                            violationType = LicenseViolationType.NOT_ALLOWED,
                            message = "Unknown license is not allowed",
                        )
                    )
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).hasSize(1)
            assertThat(result.licenseViolations[0].category).isEqualTo(LicenseCategory.UNKNOWN)
            assertThat(result.licenseViolations[0].detectedLicense).isNull()
        }
    }

    @Nested
    inner class `Violation types` {

        @Test
        fun `DENIED type violation`() = runTest {
            val result = Dependanger({
                                         libraries { library("denied-lib", "com.denied:lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeLicenseCheck {
                    listOf(
                        LicenseViolation(
                            alias = "denied-lib",
                            group = "com.denied",
                            artifact = "lib",
                            detectedLicense = "SSPL-1.0",
                            category = LicenseCategory.PROPRIETARY,
                            violationType = LicenseViolationType.DENIED,
                            message = "SSPL-1.0 is explicitly denied",
                        )
                    )
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).hasSize(1)
            assertThat(result.licenseViolations[0].violationType).isEqualTo(LicenseViolationType.DENIED)
            assertThat(result.licenseViolations[0].category).isEqualTo(LicenseCategory.PROPRIETARY)
        }

        @Test
        fun `NOT_ALLOWED type violation`() = runTest {
            val result = Dependanger({
                                         libraries { library("restricted-lib", "com.restricted:lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeLicenseCheck {
                    listOf(
                        LicenseViolation(
                            alias = "restricted-lib",
                            group = "com.restricted",
                            artifact = "lib",
                            detectedLicense = "LGPL-2.1",
                            category = LicenseCategory.WEAK_COPYLEFT,
                            violationType = LicenseViolationType.NOT_ALLOWED,
                            message = "LGPL-2.1 is not in the allowed list",
                        )
                    )
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).hasSize(1)
            assertThat(result.licenseViolations[0].violationType).isEqualTo(LicenseViolationType.NOT_ALLOWED)
            assertThat(result.licenseViolations[0].category).isEqualTo(LicenseCategory.WEAK_COPYLEFT)
        }
    }

    @Nested
    inner class `Combined with other features` {

        @Test
        fun `license violations and updates coexist in result`() = runTest {
            val result = Dependanger({
                                         libraries { library("lib", "com.example:lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeLicenseCheck {
                    listOf(
                        LicenseViolation(
                            alias = "lib",
                            group = "com.example",
                            artifact = "lib",
                            detectedLicense = "GPL-2.0",
                            category = LicenseCategory.STRONG_COPYLEFT,
                            violationType = LicenseViolationType.DENIED,
                            message = "GPL-2.0 is denied",
                        )
                    )
                })
                addProcessor(fakeUpdateCheck {
                    listOf(
                        UpdateAvailableInfo(
                            alias = "lib",
                            group = "com.example",
                            artifact = "lib",
                            currentVersion = "1.0.0",
                            latestVersion = "2.0.0",
                            updateType = UpdateType.MAJOR,
                        )
                    )
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).hasSize(1)
            assertThat(result.updates).hasSize(1)
            assertThat(result.licenseViolations[0].alias).isEqualTo("lib")
            assertThat(result.updates[0].latestVersion).isEqualTo("2.0.0")
        }

        @Test
        fun `license violations and vulnerabilities coexist`() = runTest {
            val result = Dependanger({
                                         libraries { library("vuln-lib", "com.vuln:lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeLicenseCheck {
                    listOf(
                        LicenseViolation(
                            alias = "vuln-lib",
                            group = "com.vuln",
                            artifact = "lib",
                            detectedLicense = "AGPL-3.0",
                            category = LicenseCategory.STRONG_COPYLEFT,
                            violationType = LicenseViolationType.DENIED,
                            message = "AGPL-3.0 is denied",
                        )
                    )
                })
                addProcessor(fakeSecurityCheck {
                    listOf(
                        VulnerabilityInfo(
                            id = "CVE-2024-5678",
                            aliases = emptyList(),
                            summary = "SQL injection in vuln-lib",
                            severity = VulnerabilitySeverity.HIGH,
                            cvssScore = 8.1,
                            cvssVersion = "3.1",
                            fixedVersion = "1.1.0",
                            url = null,
                            affectedGroup = "com.vuln",
                            affectedArtifact = "lib",
                            affectedVersion = "1.0.0",
                        )
                    )
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).hasSize(1)
            assertThat(result.vulnerabilities).hasSize(1)
            assertThat(result.licenseViolations[0].detectedLicense).isEqualTo("AGPL-3.0")
            assertThat(result.vulnerabilities[0].id).isEqualTo("CVE-2024-5678")
        }

        @Test
        fun `all three features coexist in result`() = runTest {
            val result = Dependanger({
                                         libraries { library("multi-lib", "com.multi:lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeLicenseCheck {
                    listOf(
                        LicenseViolation(
                            alias = "multi-lib",
                            group = "com.multi",
                            artifact = "lib",
                            detectedLicense = "BSD-3-Clause",
                            category = LicenseCategory.PERMISSIVE,
                            violationType = LicenseViolationType.NOT_ALLOWED,
                            message = "BSD-3-Clause not in allowed list",
                        )
                    )
                })
                addProcessor(fakeUpdateCheck {
                    listOf(
                        UpdateAvailableInfo(
                            alias = "multi-lib",
                            group = "com.multi",
                            artifact = "lib",
                            currentVersion = "1.0.0",
                            latestVersion = "1.2.0",
                            updateType = UpdateType.MINOR,
                        )
                    )
                })
                addProcessor(fakeSecurityCheck {
                    listOf(
                        VulnerabilityInfo(
                            id = "GHSA-xyz-789",
                            aliases = listOf("CVE-2025-0001"),
                            summary = "Path traversal in multi-lib",
                            severity = VulnerabilitySeverity.MEDIUM,
                            cvssScore = 5.3,
                            cvssVersion = "3.1",
                            fixedVersion = "1.1.0",
                            url = "https://github.com/advisories/GHSA-xyz-789",
                            affectedGroup = "com.multi",
                            affectedArtifact = "lib",
                            affectedVersion = "1.0.0",
                        )
                    )
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.licenseViolations).hasSize(1)
            assertThat(result.updates).hasSize(1)
            assertThat(result.vulnerabilities).hasSize(1)
            assertThat(result.licenseViolations[0].category).isEqualTo(LicenseCategory.PERMISSIVE)
            assertThat(result.updates[0].updateType).isEqualTo(UpdateType.MINOR)
            assertThat(result.vulnerabilities[0].severity).isEqualTo(VulnerabilitySeverity.MEDIUM)
        }
    }
}
