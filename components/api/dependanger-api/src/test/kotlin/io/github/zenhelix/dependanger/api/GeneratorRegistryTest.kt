package io.github.zenhelix.dependanger.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GeneratorRegistryTest {

    @Test
    fun `discovers generators via ServiceLoader`() {
        val generators = GeneratorRegistry.generators

        assertThat(generators).isNotEmpty()
        assertThat(generators.map { it.generatorId }).contains("toml-version-catalog", "maven-bom")
    }

    @Test
    fun `findById returns correct generator`() {
        val toml = GeneratorRegistry.findById("toml-version-catalog")
        assertThat(toml).isNotNull
        assertThat(toml!!.generatorId).isEqualTo("toml-version-catalog")
        assertThat(toml.fileExtension).isEqualTo("toml")

        val bom = GeneratorRegistry.findById("maven-bom")
        assertThat(bom).isNotNull
        assertThat(bom!!.generatorId).isEqualTo("maven-bom")
        assertThat(bom.fileExtension).isEqualTo("xml")
    }

    @Test
    fun `findById returns null for unknown id`() {
        assertThat(GeneratorRegistry.findById("unknown-generator")).isNull()
    }

    @Test
    fun `ids returns all registered generator ids`() {
        val ids = GeneratorRegistry.ids()
        assertThat(ids).contains("toml-version-catalog", "maven-bom")
    }
}
