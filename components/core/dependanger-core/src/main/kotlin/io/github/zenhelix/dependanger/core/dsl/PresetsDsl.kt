package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.Preset

@DependangerDslMarker
public class PresetsDsl {
    public val presets: MutableList<Preset> = mutableListOf()
    private val presetDsls: MutableMap<String, PresetDsl> = mutableMapOf()

    public fun preset(name: String, block: PresetDsl.() -> Unit) {
        val dsl = PresetDsl().apply(block)
        presetDsls[name] = dsl
        presets.add(
            Preset(
                name = name,
                bundles = dsl.bundles.toList(),
                distributions = dsl.distributions.toList(),
                settings = dsl.settingsDsl?.toSettings(),
            )
        )
    }

    public fun findByName(name: String): Preset? = presets.find { it.name == name }

    public fun findDslByName(name: String): PresetDsl? = presetDsls[name]

    public fun availableNames(): List<String> = presets.map { it.name }
}

@DependangerDslMarker
public class PresetDsl {
    public val bundles: MutableList<String> = mutableListOf()
    public val distributions: MutableList<String> = mutableListOf()
    public var settingsDsl: SettingsDsl? = null

    public fun bundles(vararg names: String) {
        bundles.addAll(names)
    }

    public fun distributions(vararg names: String) {
        distributions.addAll(names)
    }

    public fun settings(block: SettingsDsl.() -> Unit) {
        settingsDsl = SettingsDsl().apply(block)
    }
}
