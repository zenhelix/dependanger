package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.util.UpdateType
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.effective.model.withExtension
import io.github.zenhelix.dependanger.effective.pipeline.EffectiveMetadataProcessor
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingPhase
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitiesExtensionKey
import io.github.zenhelix.dependanger.features.security.model.VulnerabilityInfo
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitySeverity
import io.github.zenhelix.dependanger.features.updates.model.UpdateAvailableInfo
import io.github.zenhelix.dependanger.features.updates.model.UpdatesExtensionKey
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FeatureProcessorIntegrationTest {

    private class FakeProcessor<T : Any>(
        override val id: String,
        override val phase: ProcessingPhase,
        override val order: Int,
        private val extensionKey: ExtensionKey<T>,
        private val provider: (EffectiveMetadata) -> T,
    ) : EffectiveMetadataProcessor {
        override val isOptional: Boolean = false
        override val description: String = "Fake $id for tests"
        override fun supports(context: ProcessingContext): Boolean = true

        override suspend fun process(metadata: EffectiveMetadata, context: ProcessingContext): EffectiveMetadata =
            metadata.withExtension(extensionKey, provider(metadata))
    }

    private fun fakeUpdateCheck(provider: (EffectiveMetadata) -> List<UpdateAvailableInfo>): FakeProcessor<List<UpdateAvailableInfo>> =
        FakeProcessor("fake-update-check", ProcessingPhase.UPDATE_CHECK, 100, UpdatesExtensionKey, provider)

    private fun fakeSecurityCheck(provider: (EffectiveMetadata) -> List<VulnerabilityInfo>): FakeProcessor<List<VulnerabilityInfo>> =
        FakeProcessor("fake-security-check", ProcessingPhase.SECURITY_CHECK, 120, VulnerabilitiesExtensionKey, provider)

    @Nested
    inner class UpdateCheckFlow {

        @Test
        fun `updates from processor are accessible via result extensions`() = runTest {
            val fakeUpdates = listOf(
                UpdateAvailableInfo(
                    alias = "ktor-core",
                    group = "io.ktor",
                    artifact = "ktor-client-core",
                    currentVersion = "3.1.0",
                    latestVersion = "3.1.1",
                    latestStable = "3.1.1",
                    latestAny = "3.2.0-beta1",
                    updateType = UpdateType.PATCH,
                    repository = "https://repo1.maven.org/maven2",
                ),
            )

            val result = Dependanger({
                                         versions { version("ktor", "3.1.0") }
                                         libraries { library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor")) }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeUpdateCheck { fakeUpdates })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.updates).hasSize(1)
            assertThat(result.updates[0].alias).isEqualTo("ktor-core")
            assertThat(result.updates[0].currentVersion).isEqualTo("3.1.0")
            assertThat(result.updates[0].latestVersion).isEqualTo("3.1.1")
            assertThat(result.updates[0].updateType).isEqualTo(UpdateType.PATCH)
        }

        @Test
        fun `updates based on effective metadata libraries`() = runTest {
            val result = Dependanger({
                                         versions { version("ktor", "3.0.0") }
                                         libraries {
                                             library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor"))
                                             library("ktor-cio", "io.ktor:ktor-client-cio", versionRef("ktor"))
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeUpdateCheck { metadata ->
                    metadata.libraries.values.map { lib ->
                        UpdateAvailableInfo(
                            alias = lib.alias,
                            group = lib.group,
                            artifact = lib.artifact,
                            currentVersion = lib.version?.value ?: "unknown",
                            latestVersion = "3.1.1",
                            updateType = UpdateType.MINOR,
                        )
                    }
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.updates).hasSize(2)
            assertThat(result.updates.map { it.alias }).containsExactlyInAnyOrder("ktor-core", "ktor-cio")
            assertThat(result.updates).allMatch { it.latestVersion == "3.1.1" }
        }

        @Test
        fun `no updates when all libraries are current`() = runTest {
            val result = Dependanger({
                                         libraries { library("lib", "com.example:lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeUpdateCheck { emptyList() })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.updates).isEmpty()
        }

        @Test
        fun `distribution filtering happens before update check`() = runTest {
            val result = Dependanger({
                                         libraries {
                                             library("android-lib", "com.android:lib:1.0") { tags("android") }
                                             library("server-lib", "com.server:lib:2.0") { tags("server") }
                                         }
                                         distributions {
                                             distribution("android") {
                                                 spec { byTags { include { anyOf("android") } } }
                                             }
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeUpdateCheck { metadata ->
                    metadata.libraries.values.map { lib ->
                        UpdateAvailableInfo(
                            alias = lib.alias,
                            group = lib.group,
                            artifact = lib.artifact,
                            currentVersion = lib.version?.value ?: "unknown",
                            latestVersion = "9.9.9",
                            updateType = UpdateType.MAJOR,
                        )
                    }
                })
            }.process(distribution = "android")

            assertThat(result.isSuccess).isTrue()
            assertThat(result.updates).hasSize(1)
            assertThat(result.updates[0].alias).isEqualTo("android-lib")
        }
    }

    @Nested
    inner class SecurityCheckFlow {

        @Test
        fun `vulnerabilities from processor are accessible via result extensions`() = runTest {
            val fakeVulns = listOf(
                VulnerabilityInfo(
                    id = "GHSA-abc-123",
                    aliases = listOf("CVE-2024-1234"),
                    summary = "Remote code execution in example-lib",
                    severity = VulnerabilitySeverity.CRITICAL,
                    cvssScore = 9.8,
                    cvssVersion = "3.1",
                    fixedVersion = "2.0.0",
                    url = "https://github.com/advisories/GHSA-abc-123",
                    affectedGroup = "com.example",
                    affectedArtifact = "example-lib",
                    affectedVersion = "1.0.0",
                ),
            )

            val result = Dependanger({
                                         libraries { library("example-lib", "com.example:example-lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeSecurityCheck { fakeVulns })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.vulnerabilities).hasSize(1)
            assertThat(result.vulnerabilities[0].id).isEqualTo("GHSA-abc-123")
            assertThat(result.vulnerabilities[0].severity).isEqualTo(VulnerabilitySeverity.CRITICAL)
            assertThat(result.vulnerabilities[0].cvssScore).isEqualTo(9.8)
            assertThat(result.vulnerabilities[0].affectedArtifact).isEqualTo("example-lib")
        }

        @Test
        fun `multiple vulnerabilities for different libraries`() = runTest {
            val result = Dependanger({
                                         libraries {
                                             library("lib-a", "com.a:lib-a:1.0")
                                             library("lib-b", "com.b:lib-b:2.0")
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeSecurityCheck { metadata ->
                    metadata.libraries.values.flatMap { lib ->
                        listOf(
                            VulnerabilityInfo(
                                id = "VULN-${lib.alias}",
                                aliases = emptyList(),
                                summary = "Vulnerability in ${lib.alias}",
                                severity = VulnerabilitySeverity.HIGH,
                                cvssScore = 7.5,
                                cvssVersion = "3.1",
                                fixedVersion = null,
                                url = null,
                                affectedGroup = lib.group,
                                affectedArtifact = lib.artifact,
                                affectedVersion = lib.version?.value ?: "",
                            )
                        )
                    }
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.vulnerabilities).hasSize(2)
            assertThat(result.vulnerabilities.map { it.id })
                .containsExactlyInAnyOrder("VULN-lib-a", "VULN-lib-b")
        }

        @Test
        fun `no vulnerabilities when libraries are clean`() = runTest {
            val result = Dependanger({
                                         libraries { library("safe-lib", "com.safe:lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeSecurityCheck { emptyList() })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.vulnerabilities).isEmpty()
        }
    }

    @Nested
    inner class CombinedFeatureProcessors {

        @Test
        fun `updates and vulnerabilities coexist in result`() = runTest {
            val result = Dependanger({
                                         libraries { library("lib", "com.example:lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeUpdateCheck {
                    listOf(
                        UpdateAvailableInfo(
                            alias = "lib", group = "com.example", artifact = "lib",
                            currentVersion = "1.0.0", latestVersion = "2.0.0",
                            updateType = UpdateType.MAJOR,
                        )
                    )
                })
                addProcessor(fakeSecurityCheck {
                    listOf(
                        VulnerabilityInfo(
                            id = "CVE-2024-9999", aliases = emptyList(),
                            summary = "Critical vuln", severity = VulnerabilitySeverity.CRITICAL,
                            cvssScore = 9.0, cvssVersion = "3.1", fixedVersion = "2.0.0",
                            url = null, affectedGroup = "com.example",
                            affectedArtifact = "lib", affectedVersion = "1.0.0",
                        )
                    )
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.updates).hasSize(1)
            assertThat(result.vulnerabilities).hasSize(1)
            assertThat(result.updates[0].latestVersion).isEqualTo("2.0.0")
            assertThat(result.vulnerabilities[0].fixedVersion).isEqualTo("2.0.0")
        }

        @Test
        fun `full pipeline DSL to TOML with feature data`() = runTest {
            val result = Dependanger({
                                         versions { version("kotlin", "2.1.20") }
                                         libraries {
                                             library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib", versionRef("kotlin"))
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeUpdateCheck { metadata ->
                    metadata.libraries.values.map { lib ->
                        UpdateAvailableInfo(
                            alias = lib.alias, group = lib.group, artifact = lib.artifact,
                            currentVersion = lib.version?.value ?: "", latestVersion = "2.2.0",
                            updateType = UpdateType.MINOR,
                        )
                    }
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            val toml = result.toToml()
            assertThat(toml).contains("[versions]")
            assertThat(toml).contains("kotlin")
            assertThat(result.updates).hasSize(1)
            assertThat(result.updates[0].latestVersion).isEqualTo("2.2.0")
        }
    }
}
