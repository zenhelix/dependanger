package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.generators.toml.TomlConfig
import io.github.zenhelix.dependanger.generators.toml.TomlGenerator
import java.nio.file.Path

public fun DependangerResult.toToml(config: TomlConfig = TomlConfig.DEFAULT): String =
    generate(TomlGenerator(config))

public fun DependangerResult.writeTomlTo(path: Path, config: TomlConfig = TomlConfig.DEFAULT): Path =
    writeTo(TomlGenerator(config), path)
