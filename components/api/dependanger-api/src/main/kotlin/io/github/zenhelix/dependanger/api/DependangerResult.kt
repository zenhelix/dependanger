package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.generators.bom.BomConfig
import io.github.zenhelix.dependanger.generators.toml.TomlConfig
import java.nio.file.Path

public data class DependangerResult(
    val effective: EffectiveMetadata?,
    val diagnostics: Diagnostics,
) {
    public val isSuccess: Boolean get() = effective != null && !diagnostics.hasErrors

    public fun toToml(config: TomlConfig = TomlConfig()): String = TODO()
    public fun toBom(config: BomConfig): String = TODO()
    public fun writeTomlTo(path: Path, config: TomlConfig = TomlConfig()): Path = TODO()
    public fun writeBomTo(path: Path, config: BomConfig): Path = TODO()
}
