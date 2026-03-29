package io.github.zenhelix.dependanger.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class DistributionManagementTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var metadataFile: Path

    @BeforeEach
    fun setUp() {
        metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
    }

    @Nested
    inner class `Adding distributions` {

        @Test
        fun `adds distribution with include tags`() {
            val result = CliTestSupport.runCli(
                "add-distribution", "android", "--include-tags", "android,core",
                "-i", metadataFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            val dist = CliTestSupport.readMetadata(metadataFile).distributions.find { it.name == "android" }
            assertThat(dist).isNotNull
            assertThat(dist!!.spec).isNotNull
            assertThat(dist.spec!!.byTags).isNotNull
        }

        @Test
        fun `adds distribution with exclude tags`() {
            val result = CliTestSupport.runCli(
                "add-distribution", "server", "--exclude-tags", "android",
                "-i", metadataFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            val dist = CliTestSupport.readMetadata(metadataFile).distributions.find { it.name == "server" }
            assertThat(dist!!.spec!!.byTags!!.excludes).isNotEmpty
        }

        @Test
        fun `adds distribution with include bundles`() {
            val result = CliTestSupport.runCli(
                "add-distribution", "minimal", "--include-bundles", "kotlin-essentials",
                "-i", metadataFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            val dist = CliTestSupport.readMetadata(metadataFile).distributions.find { it.name == "minimal" }
            assertThat(dist!!.spec!!.byBundles!!.includes).containsExactly("kotlin-essentials")
        }

        @Test
        fun `adds distribution without filters`() {
            val result = CliTestSupport.runCli(
                "add-distribution", "all", "-i", metadataFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            val dist = CliTestSupport.readMetadata(metadataFile).distributions.find { it.name == "all" }
            assertThat(dist!!.spec).isNull()
        }

        @Test
        fun `rejects duplicate distribution`() {
            CliTestSupport.runCli("add-distribution", "android", "-i", metadataFile.toString())
            val result = CliTestSupport.runCli("add-distribution", "android", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(1)
        }
    }

    @Nested
    inner class `Removing distributions` {

        @Test
        fun `removes existing distribution`() {
            CliTestSupport.runCli("add-distribution", "android", "-i", metadataFile.toString())
            val result = CliTestSupport.runCli("remove-distribution", "android", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(CliTestSupport.readMetadata(metadataFile).distributions).isEmpty()
        }

        @Test
        fun `fails for nonexistent distribution`() {
            val result = CliTestSupport.runCli("remove-distribution", "nonexistent", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(1)
        }
    }
}
