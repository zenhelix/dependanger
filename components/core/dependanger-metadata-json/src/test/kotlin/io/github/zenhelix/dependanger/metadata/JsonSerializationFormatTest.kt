package io.github.zenhelix.dependanger.metadata

import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.Version
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class JsonSerializationFormatTest {

    private val format = JsonSerializationFormat()

    private fun emptyMetadata() = DependangerMetadata(
        schemaVersion = "1.0",
        versions = emptyList(),
        libraries = emptyList(),
        plugins = emptyList(),
        bundles = emptyList(),
        bomImports = emptyList(),
        constraints = emptyList(),
        targetPlatforms = emptyList(),
        distributions = emptyList(),
        compatibility = emptyList(),
        settings = Settings.DEFAULT,
        presets = emptyList(),
        extensions = emptyMap(),
    )

    private fun metadataWithVersions() = emptyMetadata().copy(
        versions = listOf(
            Version(name = "kotlin", value = "2.1.20", fallbacks = emptyList()),
            Version(name = "spring-boot", value = "3.4.0", fallbacks = emptyList()),
        ),
    )

    @Nested
    inner class Serialization {

        @Test
        fun `serialize empty metadata produces valid JSON`() {
            val result = format.serialize(emptyMetadata())

            assertThat(result).isNotBlank()
            assertThat(result).contains("\"schemaVersion\"")
            assertThat(result).contains("\"1.0\"")
        }

        @Test
        fun `serialize then deserialize round-trip preserves data`() {
            val original = emptyMetadata()

            val json = format.serialize(original)
            val restored = format.deserialize(json)

            assertThat(restored).isEqualTo(original)
        }

        @Test
        fun `formatId is json`() {
            assertThat(format.formatId).isEqualTo("json")
        }

        @Test
        fun `fileExtension is dot json`() {
            assertThat(format.fileExtension).isEqualTo(".json")
        }
    }

    @Nested
    inner class Deserialization {

        @Test
        fun `blank input throws IllegalArgumentException`() {
            assertThatThrownBy { format.deserialize("   ") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("blank")
        }

        @Test
        fun `empty input throws IllegalArgumentException`() {
            assertThatThrownBy { format.deserialize("") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("blank")
        }

        @Test
        fun `invalid JSON throws MetadataDeserializationException`() {
            assertThatThrownBy { format.deserialize("not a json") }
                .isInstanceOf(MetadataDeserializationException::class.java)
        }

        @Test
        fun `JSON with unknown keys is tolerated`() {
            val original = emptyMetadata()
            val json = format.serialize(original)
            val jsonWithExtra = json.replaceFirst("{", """{"unknownField": "value",""")

            val result = format.deserialize(jsonWithExtra)

            assertThat(result).isEqualTo(original)
        }
    }

    @Nested
    inner class FileIO {

        @Test
        fun `write creates file with JSON content`(@TempDir tempDir: Path) {
            val file = tempDir.resolve("metadata.json")
            val metadata = emptyMetadata()

            format.write(metadata, file)

            val content = file.readText()
            assertThat(content).isNotBlank()
            assertThat(content).contains("\"schemaVersion\"")
        }

        @Test
        fun `write creates parent directories`(@TempDir tempDir: Path) {
            val file = tempDir.resolve("nested/dir/metadata.json")
            val metadata = emptyMetadata()

            format.write(metadata, file)

            assertThat(file).exists()
            val content = file.readText()
            assertThat(content).contains("\"schemaVersion\"")
        }

        @Test
        fun `read returns deserialized metadata`(@TempDir tempDir: Path) {
            val file = tempDir.resolve("metadata.json")
            val original = emptyMetadata()
            format.write(original, file)

            val result = format.read(file)

            assertThat(result).isEqualTo(original)
        }

        @Test
        fun `read non-existent file throws MetadataReadException`(@TempDir tempDir: Path) {
            val file = tempDir.resolve("does-not-exist.json")

            assertThatThrownBy { format.read(file) }
                .isInstanceOf(MetadataReadException::class.java)
                .hasMessageContaining("not found")
        }

        @Test
        fun `write then read round-trip preserves data`(@TempDir tempDir: Path) {
            val file = tempDir.resolve("roundtrip.json")
            val original = emptyMetadata()

            format.write(original, file)
            val restored = format.read(file)

            assertThat(restored).isEqualTo(original)
        }
    }

    @Nested
    inner class WithData {

        @Test
        fun `metadata with versions serializes and deserializes correctly`() {
            val original = metadataWithVersions()

            val json = format.serialize(original)
            val restored = format.deserialize(json)

            assertThat(restored).isEqualTo(original)
        }
    }
}
