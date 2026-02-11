package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.Preset

@DependangerDslMarker
public class PresetsDsl {
    private val _presets: MutableList<Preset> = mutableListOf()
    public val presets: List<Preset> get() = _presets.toList()
    private val presetDsls: MutableMap<String, PresetDsl> = mutableMapOf()

    public fun preset(name: String, block: PresetDsl.() -> Unit = {}) {
        val dsl = PresetDsl().apply(block)
        presetDsls[name] = dsl
        _presets.add(
            Preset(
                name = name,
                bundles = dsl.bundles.toList(),
                distributions = dsl.distributions.toList(),
                settings = dsl.settingsDsl?.toSettings(),
            )
        )
    }

    public fun findByName(name: String): Preset? = _presets.find { it.name == name }

    public fun findDslByName(name: String): PresetDsl? = presetDsls[name]

    public fun availableNames(): List<String> = _presets.map { it.name }
}

@DependangerDslMarker
public class PresetDsl {
    private val _bundles: MutableList<String> = mutableListOf()
    public val bundles: List<String> get() = _bundles.toList()
    private val _distributions: MutableList<String> = mutableListOf()
    public val distributions: List<String> get() = _distributions.toList()
    public var settingsDsl: SettingsDsl? = null

    public fun bundles(vararg names: String) {
        _bundles.addAll(names)
    }

    public fun distributions(vararg names: String) {
        _distributions.addAll(names)
    }

    public fun settings(block: SettingsDsl.() -> Unit) {
        settingsDsl = SettingsDsl().apply(block)
    }
}
