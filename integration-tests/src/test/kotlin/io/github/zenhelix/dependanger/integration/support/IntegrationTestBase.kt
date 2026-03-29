package io.github.zenhelix.dependanger.integration.support

import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.DependangerBuilder
import io.github.zenhelix.dependanger.core.dsl.DependangerDsl
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.generators.bom.BomConfig
import io.github.zenhelix.dependanger.http.client.HttpClientFactory
import io.ktor.client.HttpClient
import io.ktor.serialization.kotlinx.json.json
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Abstract base class for all integration tests.
 *
 * Sets up and tears down HTTP mocking via MockK + Ktor MockEngine.
 * Provides helpers for configuring mock HTTP responses and creating Dependanger instances.
 */
abstract class IntegrationTestBase {

    @TempDir
    lateinit var tempDir: Path

    protected lateinit var router: MockHttpRouter
    private val originalSystemProperties: MutableMap<String, String?> = mutableMapOf()

    @BeforeEach
    fun setUpBase() {
        mockkObject(HttpClientFactory)
        setCacheSystemProperties()
    }

    @AfterEach
    fun tearDownBase() {
        unmockkObject(HttpClientFactory)
        restoreSystemProperties()
    }

    /**
     * Configure mock HTTP responses for this test.
     * Must be called before [dependanger] and `process()`.
     */
    protected fun mockHttp(block: MockHttpRouter.Builder.() -> Unit) {
        router = mockHttpRouter(block)

        every { HttpClientFactory.create(any()) } answers {
            // Create a fresh MockEngine per HttpClient so that closing one client
            // does not invalidate engines used by other clients
            val engine = router.createMockEngine()
            HttpClient(engine) {
                // MockEngine client does not need CIO-specific config,
                // but we keep ContentNegotiation for JSON parsing if needed
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json(
                        kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    )
                }
            }
        }
    }

    /**
     * Create a Dependanger instance from DSL with optional preset and JDK version.
     */
    protected fun dependanger(
        preset: ProcessingPreset = ProcessingPreset.DEFAULT,
        jdk: Int? = null,
        dslBlock: DependangerDsl.() -> Unit,
    ): Dependanger {
        val builder = DependangerBuilder(dslBlock)
            .preset(preset)

        if (jdk != null) {
            builder.jdkVersion(jdk)
        }

        return builder.build()
    }

    /**
     * Cache directory for this test, isolated via @TempDir.
     */
    protected val cacheDir: Path
        get() = tempDir.resolve("cache")

    /**
     * Returns the isolated cache directory path as String, for use in settings DSL.
     */
    protected fun cacheDirFor(feature: String): String =
        cacheDir.resolve(feature).toAbsolutePath().toString()

    protected fun bomConfig(
        groupId: String = "io.test",
        artifactId: String = "test-bom",
        version: String = "1.0.0",
    ): BomConfig = BomConfig(
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        name = null,
        description = null,
        filename = BomConfig.DEFAULT_FILENAME,
        includeOptionalDependencies = false,
        prettyPrint = true,
        includeDeprecationComments = false,
    )

    private fun setCacheSystemProperties() {
        val cacheRoot = cacheDir.toAbsolutePath().toString()
        setSystemProperty("dependanger.cache.bom", "$cacheRoot/bom")
        setSystemProperty("dependanger.cache.versions", "$cacheRoot/versions")
        setSystemProperty("dependanger.cache.security", "$cacheRoot/security")
        setSystemProperty("dependanger.cache.transitives", "$cacheRoot/transitives")
        setSystemProperty("dependanger.cache.licenses", "$cacheRoot/licenses")
    }

    private fun setSystemProperty(key: String, value: String) {
        originalSystemProperties[key] = System.getProperty(key)
        System.setProperty(key, value)
    }

    private fun restoreSystemProperties() {
        for ((key, originalValue) in originalSystemProperties) {
            if (originalValue != null) {
                System.setProperty(key, originalValue)
            } else {
                System.clearProperty(key)
            }
        }
        originalSystemProperties.clear()
    }
}
