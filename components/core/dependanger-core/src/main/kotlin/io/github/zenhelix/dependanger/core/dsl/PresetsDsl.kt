package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.Preset

@DependangerDslMarker
public class PresetsDsl {
    public val presets: MutableList<Preset> = mutableListOf()

    public fun preset(name: String, block: PresetDsl.() -> Unit) {
        val dsl = PresetDsl().apply(block)
        presets.add(
            Preset(
                name = name,
                bundles = dsl.bundles.toList(),
                distributions = dsl.distributions.toList(),
                settings = dsl.settingsDsl?.toSettings(),
            )
        )
    }
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
