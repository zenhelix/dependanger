package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.core.model.Settings
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingEnvironment
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private fun emptyMetadata(): DependangerMetadata = DependangerMetadata(
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

private fun contextWith(properties: Map<ProcessingContextKey<*>, Any> = emptyMap()): ProcessingContext =
    ProcessingContext(
        originalMetadata = emptyMetadata(),
        settings = Settings.DEFAULT,
        environment = ProcessingEnvironment.DEFAULT,
        activeDistribution = null,
        callback = null,
        properties = properties,
    )

class ContextContributorTest {

    @Nested
    inner class CoreSpiContextContributorContribute {

        @Test
        fun `contribute returns map with all three expected keys`() {
            val contributor = CoreSpiContextContributor()

            val result = contributor.contribute()

            assertThat(result).containsKey(LibraryFiltersKey)
            assertThat(result).containsKey(PluginFiltersKey)
            assertThat(result).containsKey(CustomRuleHandlersKey)
            assertThat(result).hasSize(3)
        }

        @Test
        fun `library filters value is a list`() {
            val contributor = CoreSpiContextContributor()

            val result = contributor.contribute()

            assertThat(result[LibraryFiltersKey]).isInstanceOf(List::class.java)
        }

        @Test
        fun `plugin filters value is a list`() {
            val contributor = CoreSpiContextContributor()

            val result = contributor.contribute()

            assertThat(result[PluginFiltersKey]).isInstanceOf(List::class.java)
        }

        @Test
        fun `custom rule handlers value is a map`() {
            val contributor = CoreSpiContextContributor()

            val result = contributor.contribute()

            assertThat(result[CustomRuleHandlersKey]).isInstanceOf(Map::class.java)
        }

        @Test
        fun `without SPI implementations library filters list is empty`() {
            val contributor = CoreSpiContextContributor()

            val result = contributor.contribute()
            val filters = result[LibraryFiltersKey] as List<*>

            assertThat(filters).isEmpty()
        }

        @Test
        fun `without SPI implementations plugin filters list is empty`() {
            val contributor = CoreSpiContextContributor()

            val result = contributor.contribute()
            val filters = result[PluginFiltersKey] as List<*>

            assertThat(filters).isEmpty()
        }

        @Test
        fun `without SPI implementations custom rule handlers map is empty`() {
            val contributor = CoreSpiContextContributor()

            val result = contributor.contribute()
            val handlers = result[CustomRuleHandlersKey] as Map<*, *>

            assertThat(handlers).isEmpty()
        }
    }

    @Nested
    inner class ContributionsInProcessingContext {

        @Test
        fun `contributed library filters are readable from context`() {
            val contributor = CoreSpiContextContributor()
            val contributions = contributor.contribute()
            val context = contextWith(contributions)

            val filters: List<LibraryFilter>? = context[LibraryFiltersKey]

            assertThat(filters).isNotNull
            assertThat(filters).isEmpty()
        }

        @Test
        fun `contributed plugin filters are readable from context`() {
            val contributor = CoreSpiContextContributor()
            val contributions = contributor.contribute()
            val context = contextWith(contributions)

            val filters: List<PluginFilter>? = context[PluginFiltersKey]

            assertThat(filters).isNotNull
            assertThat(filters).isEmpty()
        }

        @Test
        fun `contributed custom rule handlers are readable from context`() {
            val contributor = CoreSpiContextContributor()
            val contributions = contributor.contribute()
            val context = contextWith(contributions)

            val handlers: Map<String, CustomRuleHandler>? = context[CustomRuleHandlersKey]

            assertThat(handlers).isNotNull
            assertThat(handlers).isEmpty()
        }

        @Test
        fun `context without contributions returns null for keys`() {
            val context = contextWith()

            assertThat(context[LibraryFiltersKey]).isNull()
            assertThat(context[PluginFiltersKey]).isNull()
            assertThat(context[CustomRuleHandlersKey]).isNull()
        }
    }

    @Nested
    inner class ContextContributorInterface {

        @Test
        fun `CoreSpiContextContributor implements ContextContributor`() {
            val contributor = CoreSpiContextContributor()

            assertThat(contributor).isInstanceOf(ContextContributor::class.java)
        }
    }
}
