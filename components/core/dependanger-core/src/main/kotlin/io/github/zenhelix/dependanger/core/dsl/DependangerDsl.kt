package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.TypedKey
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

public class DslExtensionKey<T : Any>(name: String, serializer: KSerializer<T>) : TypedKey<T>(name, serializer)

@DependangerDslMarker
public class DependangerDsl : DependangerDslApi {
    private val versionsDsl: VersionsDsl = VersionsDsl()
    private val librariesDsl: LibrariesDsl = LibrariesDsl()
    private val pluginsDsl: PluginsDsl = PluginsDsl()
    private val bundlesDsl: BundlesDsl = BundlesDsl()
    private val bomImportsDsl: BomImportsDsl = BomImportsDsl()
    private val targetPlatformsDsl: TargetPlatformsDsl = TargetPlatformsDsl()
    private val distributionsDsl: DistributionsDsl = DistributionsDsl()
    private val constraintsDsl: ConstraintsDsl = ConstraintsDsl()
    private val compatibilityDsl: CompatibilityDsl = CompatibilityDsl()
    private val settingsDsl: SettingsDsl = SettingsDsl()
    private val presetsDsl: PresetsDsl = PresetsDsl()
    private val processingDsl: ProcessingDsl = ProcessingDsl()
    private val extensions: MutableMap<DslExtensionKey<*>, Any> = mutableMapOf()

    public override fun versions(block: VersionsDsl.() -> Unit) {
        versionsDsl.apply(block)
    }

    public override fun libraries(block: LibrariesDsl.() -> Unit) {
        librariesDsl.apply(block)
    }

    public override fun plugins(block: PluginsDsl.() -> Unit) {
        pluginsDsl.apply(block)
    }

    public override fun bundles(block: BundlesDsl.() -> Unit) {
        bundlesDsl.apply(block)
    }

    public override fun bomImports(block: BomImportsDsl.() -> Unit) {
        bomImportsDsl.apply(block)
    }

    public override fun targetPlatforms(block: TargetPlatformsDsl.() -> Unit) {
        targetPlatformsDsl.apply(block)
    }

    public override fun distributions(block: DistributionsDsl.() -> Unit) {
        distributionsDsl.apply(block)
    }

    public override fun constraints(block: ConstraintsDsl.() -> Unit) {
        constraintsDsl.apply(block)
    }

    public override fun compatibility(block: CompatibilityDsl.() -> Unit) {
        compatibilityDsl.apply(block)
    }

    public override fun settings(block: SettingsDsl.() -> Unit) {
        settingsDsl.apply(block)
    }

    public override fun presets(block: PresetsDsl.() -> Unit) {
        presetsDsl.apply(block)
    }

    public override fun processing(block: ProcessingDsl.() -> Unit) {
        processingDsl.apply(block)
    }

    public fun <T : Any> registerExtension(key: DslExtensionKey<T>, extension: T) {
        extensions[key] = extension
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> extension(key: DslExtensionKey<T>): T? = extensions[key] as? T

    public fun allExtensions(): Map<DslExtensionKey<*>, Any> = extensions.toMap()

    public fun applyPreset(name: String) {
        // bundles и distributions — строковые ссылки на определённые в DSL сущности.
        // Доступны через metadata.presets для потребителей (API, CLI, Gradle Plugin),
        // которые используют их для определения scope обработки.

        val presetDsl = presetsDsl.findDslByName(name)
        if (presetDsl != null) {
            // DSL-путь: точный merge через Trackable — применяются только явно установленные поля
            presetDsl.settingsDsl?.applyTo(settingsDsl)
            return
        }

        // JSON-путь (fallback): preset загружен из файла, DSL-объекта нет
        val preset = presetsDsl.findByName(name)
            ?: throw IllegalArgumentException(
                "Preset '$name' not found. Available presets: ${presetsDsl.availableNames()}"
            )
        preset.settings?.let { presetSettings ->
            settingsDsl.mergeFrom(presetSettings)
        }
    }

    public fun toMetadata(): DependangerMetadata = DependangerMetadata(
        schemaVersion = DependangerMetadata.SCHEMA_VERSION,
        versions = versionsDsl.versions.toList(),
        libraries = librariesDsl.libraries.toList(),
        plugins = pluginsDsl.plugins.toList(),
        bundles = bundlesDsl.bundles.toList(),
        bomImports = bomImportsDsl.boms.toList(),
        constraints = constraintsDsl.constraints.toList(),
        targetPlatforms = targetPlatformsDsl.platforms.toList(),
        distributions = distributionsDsl.distributions.toList(),
        compatibility = compatibilityDsl.rules.toList(),
        settings = settingsDsl.toSettings(),
        presets = presetsDsl.presets.toList(),
        extensions = buildExtensionsMap(),
    )

    @Suppress("UNCHECKED_CAST")
    private fun buildExtensionsMap(): Map<String, JsonElement> {
        val json = Json { encodeDefaults = true }
        return buildMap {
            for ((key, value) in extensions) {
                val serializer = (key as DslExtensionKey<Any>).serializer
                put(key.name, json.encodeToJsonElement(serializer, value))
            }
        }
    }
}

public fun <T : Any> DependangerDsl.configure(key: DslExtensionKey<T>, block: T.() -> Unit) {
    val ext = extension(key) ?: error("DSL extension '${key.name}' is not registered. Call registerExtension() first.")
    ext.block()
}

public fun dependanger(block: DependangerDsl.() -> Unit): DependangerDsl =
    DependangerDsl().apply(block)
