package io.github.zenhelix.dependanger.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class BundleManagementTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var metadataFile: Path

    @BeforeEach
    fun setUp() {
        metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
    }

    @Nested
    inner class `Adding bundles` {

        @Test
        fun `adds bundle with libraries`() {
            val result = CliTestSupport.runCli(
                "add", "bundle", "networking", "--libraries", "stdlib",
                "-i", metadataFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            val bundle = CliTestSupport.readMetadata(metadataFile).bundles.find { it.alias == "networking" }
            assertThat(bundle).isNotNull
            assertThat(bundle!!.libraries).containsExactly("stdlib")
        }

        @Test
        fun `adds bundle with extends`() {
            val result = CliTestSupport.runCli(
                "add", "bundle", "kotlin-full", "--extends", "kotlin-essentials",
                "-i", metadataFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(0)
            val bundle = CliTestSupport.readMetadata(metadataFile).bundles.find { it.alias == "kotlin-full" }
            assertThat(bundle!!.extends).containsExactly("kotlin-essentials")
        }

        @Test
        fun `rejects duplicate bundle name`() {
            val result = CliTestSupport.runCli(
                "add", "bundle", "kotlin-essentials", "--libraries", "stdlib",
                "-i", metadataFile.toString(),
            )
            assertThat(result.statusCode).isEqualTo(1)
        }
    }

    @Nested
    inner class `Removing bundles` {

        @Test
        fun `removes bundle not extended by others`() {
            val result = CliTestSupport.runCli("remove", "bundle", "kotlin-essentials", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(0)
            assertThat(CliTestSupport.readMetadata(metadataFile).bundles).isEmpty()
        }

        @Test
        fun `fails to remove bundle extended by another`() {
            CliTestSupport.runCli("add", "bundle", "kotlin-full", "--extends", "kotlin-essentials", "-i", metadataFile.toString())
            val result = CliTestSupport.runCli("remove", "bundle", "kotlin-essentials", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(1)
        }

        @Test
        fun `force removes extended bundle`() {
            CliTestSupport.runCli("add", "bundle", "kotlin-full", "--extends", "kotlin-essentials", "-i", metadataFile.toString())
            val result = CliTestSupport.runCli("remove", "bundle", "kotlin-essentials", "--force", "-i", metadataFile.toString())
            assertThat(result.statusCode).isEqualTo(0)
        }
    }
}
