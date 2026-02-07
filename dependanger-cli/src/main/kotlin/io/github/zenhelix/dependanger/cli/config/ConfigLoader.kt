package io.github.zenhelix.dependanger.cli.config

import java.nio.file.Path

public class ConfigLoader {
    public fun load(configPath: Path? = null): CliConfig = TODO()
    public fun findConfigFile(): Path = TODO()

    public companion object {
        public const val DEFAULT_CONFIG_FILENAME: String = ".dependanger.yaml"
    }
}
