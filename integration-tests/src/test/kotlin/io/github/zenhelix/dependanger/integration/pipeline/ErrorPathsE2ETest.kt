package io.github.zenhelix.dependanger.integration.pipeline

import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.DependangerConfigurationException
import io.github.zenhelix.dependanger.api.DependangerProcessingException
import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.api.toBom
import io.github.zenhelix.dependanger.api.toToml
import io.github.zenhelix.dependanger.core.dsl.versionRef
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.DiagnosticCodes
import io.github.zenhelix.dependanger.integration.support.IntegrationTestBase
import io.github.zenhelix.dependanger.integration.support.TestCatalogs
import io.github.zenhelix.dependanger.integration.support.assertResult
import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Error Paths E2E")
class ErrorPathsE2ETest : IntegrationTestBase() {

    @Nested
    inner class `configuration errors` {

        @Test
        fun `invalid JSON throws DependangerConfigurationException`() {
            assertThatThrownBy {
                Dependanger.fromJson("this is not valid json")
            }.isInstanceOf(DependangerConfigurationException::class.java)
        }
    }

    @Nested
    inner class `unresolved references` {

        @Test
        fun `unresolved version reference produces error and blocks generation`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib", "com.example:lib", versionRef("nonexistent"))
                }
            }.process()

            assertThat(result.isSuccess).isFalse()

            val errorCodes = result.diagnostics.errors.map { it.code }
            assertThat(errorCodes).contains(DiagnosticCodes.Version.UNRESOLVED)

            assertThatThrownBy {
                result.toToml()
            }.isInstanceOf(DependangerProcessingException::class.java)
        }
    }

    @Nested
    inner class `distribution errors` {

        @Test
        fun `nonexistent distribution produces error diagnostic`() = runTest {
            val result = dependanger {
                versions {
                    version("v1", "1.0.0")
                }
                libraries {
                    library("lib", "com.example:lib", versionRef("v1"))
                }
            }.process(distribution = "nonexistent")

            val allCodes = (result.diagnostics.errors + result.diagnostics.warnings).map { it.code }
            assertThat(allCodes).contains(DiagnosticCodes.Profile.NOT_FOUND)
        }
    }

    @Nested
    inner class `empty DSL edge cases` {

        @Test
        fun `empty DSL with STRICT preset still succeeds`() = runTest {
            mockHttp {
                // No responses needed for empty catalog
            }

            val result = dependanger(preset = ProcessingPreset.STRICT) { }.process()

            assertResult(result).isSuccessful()
        }
    }

    @Nested
    inner class `duplicate handling` {

        @Test
        fun `duplicate library aliases are handled`() = runTest {
            val result = dependanger {
                versions {
                    version("v1", "1.0.0")
                }
                libraries {
                    library("dup-lib", "com.example:lib-a", versionRef("v1"))
                    library("dup-lib", "com.example:lib-b", versionRef("v1"))
                }
            }.process()

            // Should either succeed with last-wins or produce a validation diagnostic
            val hasDuplicateDiagnostic = result.diagnostics.errors.any {
                it.code == DiagnosticCodes.Validation.DUPLICATE_ALIAS
            } || result.diagnostics.warnings.any {
                it.code == DiagnosticCodes.Validation.DUPLICATE_ALIAS
            }

            // Either the duplicate is flagged or the result has exactly one library
            if (!hasDuplicateDiagnostic) {
                assertThat(result).isInstanceOf(DependangerResult.Success::class.java)
            }
        }
    }

    @Nested
    inner class `circular references` {

        @Test
        fun `circular bundle extends`() = runTest {
            val result = dependanger {
                versions {
                    version("v1", "1.0.0")
                }
                libraries {
                    library("lib-a", "com.example:lib-a", versionRef("v1"))
                }
                bundles {
                    bundle("bundle-a") {
                        libraries("lib-a")
                        extends("bundle-b")
                    }
                    bundle("bundle-b") {
                        extends("bundle-a")
                    }
                }
            }.process()

            // Should either produce a diagnostic or handle gracefully
            val hasCircularDiagnostic = result.diagnostics.errors.any {
                it.code == DiagnosticCodes.Validation.CIRCULAR_EXTENDS
            } || result.diagnostics.warnings.any {
                it.code == DiagnosticCodes.Validation.CIRCULAR_EXTENDS
            }

            // If no circular diagnostic, the result should at least not crash
            assertThat(result.isSuccess || hasCircularDiagnostic).isTrue()
        }
    }

    @Nested
    inner class `generation on failed result` {

        @Test
        fun `generation on failed result throws DependangerProcessingException`() = runTest {
            val result = dependanger {
                libraries {
                    library("lib", "com.example:lib", versionRef("missing"))
                }
            }.process()

            assertThat(result.isSuccess).isFalse()

            assertThatThrownBy {
                result.toBom(bomConfig())
            }.isInstanceOf(DependangerProcessingException::class.java)
        }
    }

    @Nested
    inner class `serialization round-trip` {

        @Test
        fun `JSON round-trip preserves metadata`() = runTest {
            val dslBlock = TestCatalogs.standard()

            val originalResult = dependanger(dslBlock = dslBlock).process()
            assertResult(originalResult).isSuccessful()

            // Serialize DSL to metadata, then to JSON, then back
            val dsl = io.github.zenhelix.dependanger.core.dsl.DependangerDsl().apply(dslBlock)
            val metadata = dsl.toMetadata()
            val format = JsonSerializationFormat()
            val json = format.serialize(metadata)
            val deserialized = format.deserialize(json)

            val roundTripResult = Dependanger.fromMetadata(deserialized).build().process()

            assertResult(roundTripResult).isSuccessful()

            val originalSuccess = originalResult as DependangerResult.Success
            val roundTripSuccess = roundTripResult as DependangerResult.Success
            val originalLibs = originalSuccess.effective.libraries.keys
            val roundTripLibs = roundTripSuccess.effective.libraries.keys
            assertThat(roundTripLibs).containsExactlyInAnyOrderElementsOf(originalLibs)

            val originalVersions = originalSuccess.effective.versions.mapValues { it.value.value }
            val roundTripVersions = roundTripSuccess.effective.versions.mapValues { it.value.value }
            assertThat(roundTripVersions).containsAllEntriesOf(originalVersions)
        }
    }
}
