package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Library
import io.github.zenhelix.dependanger.core.model.Plugin
import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.VersionReference
import io.github.zenhelix.dependanger.core.model.VersionReference.VersionRange
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.EffectiveVersion
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MetadataConversionProcessorTest {

    private val processor = MetadataConversionProcessor()

    private fun emptyMetadata(
        libraries: List<Library> = emptyList(),
        plugins: List<Plugin> = emptyList(),
    ): DependangerMetadata = DependangerMetadata(
        schemaVersion = DependangerMetadata.SCHEMA_VERSION,
        versions = emptyList(),
        libraries = libraries,
        plugins = plugins,
        bundles = emptyList(),
        bomImports = emptyList(),
        constraints = emptyList(),
        targetPlatforms = emptyList(),
        distributions = emptyList(),
        compatibility = emptyList(),
        settings = Settings.DEFAULT,
        presets = emptyList(),
    )

    private fun contextFor(metadata: DependangerMetadata): ProcessingContext = ProcessingContext(
        originalMetadata = metadata,
        settings = Settings.DEFAULT,
        environment = ProcessingEnvironment.DEFAULT,
        activeDistribution = null,
        callback = null,
        properties = emptyMap(),
    )

    private fun emptyEffective(): EffectiveMetadata = EffectiveMetadata(
        schemaVersion = DependangerMetadata.SCHEMA_VERSION,
        distribution = null,
        versions = emptyMap(),
        libraries = emptyMap(),
        plugins = emptyMap(),
        bundles = emptyMap(),
        diagnostics = Diagnostics.EMPTY,
        processingInfo = null,
    )

    @Nested
    inner class LibraryRangeConversion {

        @Test
        fun `library with simple range produces EffectiveVersion Range`() = runTest {
            val range = VersionRange.Simple("[1.0,2.0)")
            val lib = Library(
                alias = "mylib",
                group = "com.example",
                artifact = "mylib",
                version = VersionReference.Range(range),
                description = null,
                tags = emptySet(),
                requires = null,
                deprecation = null,
                license = null,
                constraints = emptyList(),
                isPlatform = false,
            )

            val metadata = emptyMetadata(libraries = listOf(lib))
            val result = processor.process(emptyEffective(), contextFor(metadata))

            val effectiveLib = result.libraries["mylib"]
            assertThat(effectiveLib).isNotNull
            assertThat(effectiveLib!!.version).isInstanceOf(EffectiveVersion.Range::class.java)
            val effectiveRange = effectiveLib.version as EffectiveVersion.Range
            assertThat(effectiveRange.range).isEqualTo(range)
        }

        @Test
        fun `library with rich range produces EffectiveVersion Range`() = runTest {
            val range = VersionRange.Rich(
                require = "1.0",
                strictly = null,
                prefer = "1.5",
                reject = listOf("1.3"),
            )
            val lib = Library(
                alias = "richlib",
                group = "com.example",
                artifact = "richlib",
                version = VersionReference.Range(range),
                description = null,
                tags = emptySet(),
                requires = null,
                deprecation = null,
                license = null,
                constraints = emptyList(),
                isPlatform = false,
            )

            val metadata = emptyMetadata(libraries = listOf(lib))
            val result = processor.process(emptyEffective(), contextFor(metadata))

            val effectiveLib = result.libraries["richlib"]
            assertThat(effectiveLib).isNotNull
            assertThat(effectiveLib!!.version).isInstanceOf(EffectiveVersion.Range::class.java)
            val effectiveRange = effectiveLib.version as EffectiveVersion.Range
            assertThat(effectiveRange.range).isEqualTo(range)
        }
    }

    @Nested
    inner class PluginRangeConversion {

        @Test
        fun `plugin with simple range produces EffectiveVersion Range`() = runTest {
            val range = VersionRange.Simple("[2.0,3.0)")
            val plugin = Plugin(
                alias = "myplugin",
                id = "com.example.plugin",
                version = VersionReference.Range(range),
                tags = emptySet(),
            )

            val metadata = emptyMetadata(plugins = listOf(plugin))
            val result = processor.process(emptyEffective(), contextFor(metadata))

            val effectivePlugin = result.plugins["myplugin"]
            assertThat(effectivePlugin).isNotNull
            assertThat(effectivePlugin!!.version).isInstanceOf(EffectiveVersion.Range::class.java)
            val effectiveRange = effectivePlugin.version as EffectiveVersion.Range
            assertThat(effectiveRange.range).isEqualTo(range)
        }
    }
}
