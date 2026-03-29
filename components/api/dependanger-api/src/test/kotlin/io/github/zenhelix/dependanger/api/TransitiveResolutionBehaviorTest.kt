package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.core.util.UpdateType
import io.github.zenhelix.dependanger.feature.model.license.LicenseCategory
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolation
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolationType
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilityInfo
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitySeverity
import io.github.zenhelix.dependanger.feature.model.transitive.ConflictResolutionStrategy
import io.github.zenhelix.dependanger.feature.model.transitive.TransitiveTree
import io.github.zenhelix.dependanger.feature.model.transitive.VersionConflict
import io.github.zenhelix.dependanger.feature.model.updates.UpdateAvailableInfo
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TransitiveResolutionBehaviorTest {

    private fun dependanger(
        preset: ProcessingPreset = ProcessingPreset.DEFAULT,
        dslBlock: DependangerDsl.() -> Unit,
    ): Dependanger = Dependanger(dslBlock) { preset(preset) }


    @Nested
    inner class `Transitive tree via result extensions` {

        @Test
        fun `transitives accessible via result transitives`() = runTest {
            val fakeTree = listOf(
                TransitiveTree(
                    group = "io.ktor",
                    artifact = "ktor-client-core",
                    version = "3.1.0",
                    scope = "implementation",
                    children = emptyList(),
                    isDuplicate = false,
                    isCycle = false,
                ),
            )

            val result = Dependanger({
                                         versions { version("ktor", "3.1.0") }
                                         libraries { library("ktor-core", "io.ktor:ktor-client-core", versionRef("ktor")) }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeTransitiveResolver { fakeTree })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.transitives).hasSize(1)
            assertThat(result.transitives[0].group).isEqualTo("io.ktor")
            assertThat(result.transitives[0].artifact).isEqualTo("ktor-client-core")
            assertThat(result.transitives[0].version).isEqualTo("3.1.0")
            assertThat(result.transitives[0].scope).isEqualTo("implementation")
            assertThat(result.transitives[0].isDuplicate).isFalse()
            assertThat(result.transitives[0].isCycle).isFalse()
        }

        @Test
        fun `nested transitive tree with children renders correctly`() = runTest {
            val nestedTree = listOf(
                TransitiveTree(
                    group = "com.example",
                    artifact = "parent-lib",
                    version = "1.0.0",
                    scope = "implementation",
                    children = listOf(
                        TransitiveTree(
                            group = "com.example",
                            artifact = "child-lib",
                            version = "2.0.0",
                            scope = "runtime",
                            children = listOf(
                                TransitiveTree(
                                    group = "com.example",
                                    artifact = "grandchild-lib",
                                    version = "3.0.0",
                                    scope = "runtime",
                                    children = emptyList(),
                                    isDuplicate = false,
                                    isCycle = false,
                                ),
                            ),
                            isDuplicate = false,
                            isCycle = false,
                        ),
                        TransitiveTree(
                            group = "com.example",
                            artifact = "duplicate-lib",
                            version = "1.5.0",
                            scope = "runtime",
                            children = emptyList(),
                            isDuplicate = true,
                            isCycle = false,
                        ),
                    ),
                    isDuplicate = false,
                    isCycle = false,
                ),
            )

            val result = Dependanger({
                                         libraries { library("parent", "com.example:parent-lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeTransitiveResolver { nestedTree })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.transitives).hasSize(1)

            val root = result.transitives[0]
            assertThat(root.artifact).isEqualTo("parent-lib")
            assertThat(root.children).hasSize(2)

            val child = root.children[0]
            assertThat(child.artifact).isEqualTo("child-lib")
            assertThat(child.children).hasSize(1)
            assertThat(child.children[0].artifact).isEqualTo("grandchild-lib")

            val duplicate = root.children[1]
            assertThat(duplicate.artifact).isEqualTo("duplicate-lib")
            assertThat(duplicate.isDuplicate).isTrue()
        }

        @Test
        fun `empty transitives when no processor data`() = runTest {
            val result = dependanger {
                libraries { library("lib", "com.example:lib:1.0.0") }
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.transitives).isEmpty()
        }
    }

    @Nested
    inner class `Version conflicts` {

        @Test
        fun `version conflicts accessible via result versionConflicts`() = runTest {
            val fakeConflicts = listOf(
                VersionConflict(
                    group = "com.example",
                    artifact = "shared-lib",
                    requestedVersions = listOf("1.0.0", "2.0.0"),
                    resolvedVersion = "2.0.0",
                    resolution = ConflictResolutionStrategy.HIGHEST,
                ),
            )

            val result = Dependanger({
                                         libraries { library("shared", "com.example:shared-lib:2.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeConflictDetector { fakeConflicts })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.versionConflicts).hasSize(1)
            assertThat(result.versionConflicts[0].group).isEqualTo("com.example")
            assertThat(result.versionConflicts[0].artifact).isEqualTo("shared-lib")
            assertThat(result.versionConflicts[0].requestedVersions).containsExactly("1.0.0", "2.0.0")
            assertThat(result.versionConflicts[0].resolvedVersion).isEqualTo("2.0.0")
            assertThat(result.versionConflicts[0].resolution).isEqualTo(ConflictResolutionStrategy.HIGHEST)
        }

        @Test
        fun `conflict with multiple requested versions`() = runTest {
            val fakeConflicts = listOf(
                VersionConflict(
                    group = "org.slf4j",
                    artifact = "slf4j-api",
                    requestedVersions = listOf("1.7.36", "2.0.0", "2.0.9"),
                    resolvedVersion = "2.0.9",
                    resolution = ConflictResolutionStrategy.HIGHEST,
                ),
                VersionConflict(
                    group = "com.google.guava",
                    artifact = "guava",
                    requestedVersions = listOf("31.0-jre", "32.1-jre"),
                    resolvedVersion = "31.0-jre",
                    resolution = ConflictResolutionStrategy.FIRST,
                ),
            )

            val result = Dependanger({
                                         libraries {
                                             library("slf4j", "org.slf4j:slf4j-api:2.0.9")
                                             library("guava", "com.google.guava:guava:31.0-jre")
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeConflictDetector { fakeConflicts })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.versionConflicts).hasSize(2)

            val slf4jConflict = result.versionConflicts.first { it.artifact == "slf4j-api" }
            assertThat(slf4jConflict.requestedVersions).hasSize(3)
            assertThat(slf4jConflict.resolution).isEqualTo(ConflictResolutionStrategy.HIGHEST)

            val guavaConflict = result.versionConflicts.first { it.artifact == "guava" }
            assertThat(guavaConflict.requestedVersions).hasSize(2)
            assertThat(guavaConflict.resolution).isEqualTo(ConflictResolutionStrategy.FIRST)
        }

        @Test
        fun `no conflicts when versions aligned`() = runTest {
            val result = Dependanger({
                                         libraries { library("lib", "com.example:lib:1.0.0") }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeConflictDetector { emptyList() })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.versionConflicts).isEmpty()
        }
    }

    @Nested
    inner class `Transitive data with distribution` {

        @Test
        fun `transitive data reflects libraries from filtered distribution`() = runTest {
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
                addProcessor(fakeTransitiveResolver { metadata ->
                    metadata.libraries.values.map { lib ->
                        TransitiveTree(
                            group = lib.group,
                            artifact = lib.artifact,
                            version = lib.version?.value,
                            scope = "implementation",
                            children = emptyList(),
                            isDuplicate = false,
                            isCycle = false,
                        )
                    }
                })
            }.process(distribution = "android")

            assertThat(result.isSuccess).isTrue()
            assertThat(result.transitives).hasSize(1)
            assertThat(result.transitives[0].group).isEqualTo("com.android")
            assertThat(result.transitives[0].artifact).isEqualTo("lib")
        }
    }

    @Nested
    inner class `All feature data combined` {

        @Test
        fun `updates + vulnerabilities + licenses + transitives + conflicts all coexist in single result`() = runTest {
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
                        ),
                    )
                })
                addProcessor(fakeSecurityCheck {
                    listOf(
                        VulnerabilityInfo(
                            id = "CVE-2024-1111", aliases = emptyList(),
                            summary = "Test vulnerability", severity = VulnerabilitySeverity.HIGH,
                            cvssScore = 7.5, cvssVersion = "3.1", fixedVersion = "2.0.0",
                            url = null, affectedGroup = "com.example",
                            affectedArtifact = "lib", affectedVersion = "1.0.0",
                        ),
                    )
                })
                addProcessor(fakeLicenseCheck {
                    listOf(
                        LicenseViolation(
                            alias = "lib", group = "com.example", artifact = "lib",
                            detectedLicense = "GPL-3.0",
                            category = LicenseCategory.STRONG_COPYLEFT,
                            violationType = LicenseViolationType.DENIED,
                            message = "GPL-3.0 is not allowed",
                        ),
                    )
                })
                addProcessor(fakeTransitiveResolver {
                    listOf(
                        TransitiveTree(
                            group = "com.example", artifact = "lib", version = "1.0.0",
                            scope = "implementation", children = emptyList(),
                            isDuplicate = false, isCycle = false,
                        ),
                    )
                })
                addProcessor(fakeConflictDetector {
                    listOf(
                        VersionConflict(
                            group = "com.example", artifact = "lib",
                            requestedVersions = listOf("1.0.0", "1.1.0"),
                            resolvedVersion = "1.1.0",
                            resolution = ConflictResolutionStrategy.HIGHEST,
                        ),
                    )
                })
            }.process()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.updates).hasSize(1)
            assertThat(result.vulnerabilities).hasSize(1)
            assertThat(result.licenseViolations).hasSize(1)
            assertThat(result.transitives).hasSize(1)
            assertThat(result.versionConflicts).hasSize(1)
        }

        @Test
        fun `each feature data is independently accessible`() = runTest {
            val result = Dependanger({
                                         libraries {
                                             library("lib-a", "com.a:lib:1.0")
                                             library("lib-b", "com.b:lib:2.0")
                                         }
                                     }) {
                preset(ProcessingPreset.DEFAULT)
                addProcessor(fakeUpdateCheck {
                    listOf(
                        UpdateAvailableInfo(
                            alias = "lib-a", group = "com.a", artifact = "lib",
                            currentVersion = "1.0", latestVersion = "1.1",
                            updateType = UpdateType.MINOR,
                        ),
                    )
                })
                addProcessor(fakeTransitiveResolver { metadata ->
                    metadata.libraries.values.map { lib ->
                        TransitiveTree(
                            group = lib.group, artifact = lib.artifact,
                            version = lib.version?.value, scope = "implementation",
                            children = emptyList(), isDuplicate = false, isCycle = false,
                        )
                    }
                })
            }.process()

            assertThat(result.isSuccess).isTrue()

            assertThat(result.updates).hasSize(1)
            assertThat(result.updates[0].alias).isEqualTo("lib-a")

            assertThat(result.transitives).hasSize(2)
            assertThat(result.transitives.map { it.group }).containsExactlyInAnyOrder("com.a", "com.b")

            assertThat(result.vulnerabilities).isEmpty()
            assertThat(result.licenseViolations).isEmpty()
            assertThat(result.versionConflicts).isEmpty()
        }
    }
}
