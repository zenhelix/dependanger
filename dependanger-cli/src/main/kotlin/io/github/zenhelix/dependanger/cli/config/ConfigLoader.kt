package io.github.zenhelix.dependanger.cli.config

import com.charleskorn.kaml.Yaml
import io.github.zenhelix.dependanger.cli.CliException
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

public class ConfigLoader {

    private val yaml: Yaml = Yaml.default

    public fun load(configPath: Path? = null): CliConfig {
        val path = configPath ?: try {
            findConfigFile()
        } catch (_: CliException.FileNotFound) {
            return CliConfig()
        }

        val content = path.readText()
        return try {
            yaml.decodeFromString(CliConfig.serializer(), content)
        } catch (e: Exception) {
            throw CliException.ParseError("Failed to parse config file '$path': ${e.message}", e)
        }
    }

    public fun findConfigFile(): Path {
        var current: Path? = Path.of("").toAbsolutePath()
        while (current != null) {
            val candidate = current.resolve(DEFAULT_CONFIG_FILENAME)
            if (candidate.exists()) {
                return candidate
            }
            current = current.parent
        }

        val homeCandidate = Path.of(System.getProperty("user.home")).resolve(DEFAULT_CONFIG_FILENAME)
        if (homeCandidate.exists()) {
            return homeCandidate
        }

        throw CliException.FileNotFound(
            "Config file '$DEFAULT_CONFIG_FILENAME' not found in current directory hierarchy or user home directory"
        )
    }

    public companion object {
        public const val DEFAULT_CONFIG_FILENAME: String = ".dependanger.yaml"
    }
}
