# Settings Decoupling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Decouple feature-specific settings from core `Settings` class into their respective feature modules, using `ProcessingContext.properties` for runtime access and `customSettings: Map<String, JsonElement>` for JSON serialization.

**Architecture:** Feature settings classes move from `dependanger-core` to their feature modules. Each feature module defines a `ProcessingContextKey`, a DSL extension function on `SettingsDsl`, and a `FeatureSettingsProvider` SPI implementation for automatic deserialization. Core `Settings` retains only shared fields (`defaultDistribution`, `strictVersionResolution`, `repositories`, `validation`, `customSettings`). Dead settings (`TomlSettings`, `BomSettings`, `ReportSettings`) are deleted entirely.

**Tech Stack:** Kotlin 2.1.20, kotlinx-serialization, ServiceLoader SPI, `explicitApi()` mode

---

## File Structure

### New files to create

| File | Responsibility |
|------|---------------|
| `effective/.../spi/FeatureSettingsProvider.kt` | SPI interface for deserializing feature settings from `customSettings` |
| `updates/.../UpdateCheckSettingsKey.kt` | ProcessingContextKey + FeatureSettingsProvider + DSL extension |
| `security/.../SecurityCheckSettingsKey.kt` | ProcessingContextKey + FeatureSettingsProvider + DSL extension |
| `license/.../LicenseCheckSettingsKey.kt` | ProcessingContextKey + FeatureSettingsProvider + DSL extension |
| `transitive/.../TransitiveResolutionSettingsKey.kt` | ProcessingContextKey + FeatureSettingsProvider + DSL extension |
| `maven-resolver/.../BomCacheSettingsKey.kt` | ProcessingContextKey + FeatureSettingsProvider + DSL extension |
| `analysis/.../CompatibilityAnalysisSettingsKey.kt` | ProcessingContextKey + FeatureSettingsProvider + DSL extension |
| Each feature module: `META-INF/services/...FeatureSettingsProvider` | ServiceLoader registration |

### Files to modify

| File | Change |
|------|--------|
| `core/.../model/Settings.kt` | Remove 9 feature fields, keep core-only |
| `core/.../model/Enums.kt` | Remove feature-specific enums |
| `core/.../dsl/SettingsDsl.kt` | Remove feature blocks, add `putCustomSetting()` |
| `effective/.../pipeline/ProcessingContext.kt` | Add `require()` helper |
| `api/.../DependangerBuilder.kt` | Add `withContextProperty()`, resolve properties via SPI |
| `api/.../Dependanger.kt` | Pass resolved properties to `baseContext()` |
| All 5 feature processors | Read from `context[key]` instead of `context.settings.xxx` |
| All 6 CLI commands | Use `builder.withContextProperty()` |
| `gradle/.../DependangerExtension.kt` | No change (delegates to DSL, extensions come from classpath) |

### Files to delete

| File | Reason |
|------|--------|
| `core/.../model/Settings.kt` entries: `TomlSettings`, `BomSettings`, `ReportSettings` | Dead — not consumed by any processor |
| `core/.../dsl/SettingsDsl.kt` entries: `TomlSettingsDsl`, `BomSettingsDsl`, `ReportSettingsDsl` | Dead DSLs for deleted settings |

---

### Task 1: Add FeatureSettingsProvider SPI and SettingsDsl infrastructure

**Files:**
- Create: `components/core/dependanger-effective/src/main/kotlin/io/github/zenhelix/dependanger/effective/spi/FeatureSettingsProvider.kt`
- Modify: `components/core/dependanger-core/src/main/kotlin/io/github/zenhelix/dependanger/core/dsl/SettingsDsl.kt`
- Modify: `components/core/dependanger-effective/src/main/kotlin/io/github/zenhelix/dependanger/effective/pipeline/ProcessingContext.kt`

- [ ] **Step 1: Create FeatureSettingsProvider SPI interface**

```kotlin
// components/core/dependanger-effective/src/main/kotlin/io/github/zenhelix/dependanger/effective/spi/FeatureSettingsProvider.kt
package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContextKey
import kotlinx.serialization.json.JsonElement

public interface FeatureSettingsProvider {
    public val settingsKey: String
    public fun deserialize(json: JsonElement): Pair<ProcessingContextKey<*>, Any>
}
```

- [ ] **Step 2: Add `putCustomSetting()` to SettingsDsl**

Add to `SettingsDsl` class:

```kotlin
public fun putCustomSetting(key: String, value: JsonElement) {
    customSettings = customSettings + (key to value)
}
```

- [ ] **Step 3: Add `require()` helper to ProcessingContext**

Add to `ProcessingContext`:

```kotlin
public fun <T : Any> require(key: ProcessingContextKey<T>): T =
    this[key] ?: error("Required context property '${key.name}' is not set")
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :components:core:dependanger-effective:compileKotlin :components:core:dependanger-core:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add components/core/dependanger-effective/src/main/kotlin/io/github/zenhelix/dependanger/effective/spi/FeatureSettingsProvider.kt
git add components/core/dependanger-core/src/main/kotlin/io/github/zenhelix/dependanger/core/dsl/SettingsDsl.kt
git add components/core/dependanger-effective/src/main/kotlin/io/github/zenhelix/dependanger/effective/pipeline/ProcessingContext.kt
git commit -m "feat: add FeatureSettingsProvider SPI and SettingsDsl.putCustomSetting()"
```

---

### Task 2: Add DependangerBuilder.withContextProperty() and property resolution

**Files:**
- Modify: `components/api/dependanger-api/src/main/kotlin/io/github/zenhelix/dependanger/api/DependangerBuilder.kt`
- Modify: `components/api/dependanger-api/src/main/kotlin/io/github/zenhelix/dependanger/api/Dependanger.kt`

- [ ] **Step 1: Add withContextProperty to DependangerBuilder**

Add fields and method to `DependangerBuilder`:

```kotlin
private val contextProperties: MutableMap<ProcessingContextKey<*>, Any> = mutableMapOf()

public fun <T : Any> withContextProperty(key: ProcessingContextKey<T>, value: T): DependangerBuilder = apply {
    contextProperties[key] = value
}
```

Pass to `Dependanger` constructor:

```kotlin
public fun build(): Dependanger {
    val resolvedMetadata = resolveMetadata()
    return Dependanger(
        metadata = resolvedMetadata,
        preset = preset,
        environment = environment,
        coreProcessors = coreProcessors(),
        discoveredProcessors = ServiceLoader.load(EffectiveMetadataProcessor::class.java).toList(),
        additionalProcessors = additionalProcessors.toList(),
        disabledProcessorIds = disabledProcessorIds.toSet(),
        pipelineCustomizer = pipelineCustomizer,
        contextProperties = contextProperties.toMap(),
    )
}
```

- [ ] **Step 2: Update Dependanger constructor and baseContext()**

Add `contextProperties` parameter to `Dependanger` constructor:

```kotlin
public class Dependanger internal constructor(
    private val metadata: DependangerMetadata,
    private val preset: ProcessingPreset,
    private val environment: ProcessingEnvironment,
    private val coreProcessors: List<EffectiveMetadataProcessor>,
    private val discoveredProcessors: List<EffectiveMetadataProcessor>,
    private val additionalProcessors: List<EffectiveMetadataProcessor>,
    private val disabledProcessorIds: Set<String>,
    private val pipelineCustomizer: (PipelineBuilder.() -> Unit)?,
    private val contextProperties: Map<ProcessingContextKey<*>, Any>,
)
```

Update `baseContext()` to resolve properties from `customSettings` + explicit overrides:

```kotlin
private fun baseContext(
    distribution: String? = null,
    callback: ProcessingCallback? = null,
): ProcessingContext {
    val settingsProviders = ServiceLoader.load(FeatureSettingsProvider::class.java)
    val resolvedProperties = buildMap<ProcessingContextKey<*>, Any> {
        for (provider in settingsProviders) {
            val json = metadata.settings.customSettings[provider.settingsKey]
            if (json != null) {
                val (key, value) = provider.deserialize(json)
                put(key, value)
            }
        }
        putAll(contextProperties)
    }

    return ProcessingContext(
        originalMetadata = metadata,
        settings = metadata.settings,
        environment = environment,
        activeDistribution = distribution ?: metadata.settings.defaultDistribution,
        callback = callback,
        properties = resolvedProperties,
    )
}
```

Add import:
```kotlin
import io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :components:api:dependanger-api:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add components/api/dependanger-api/src/main/kotlin/io/github/zenhelix/dependanger/api/DependangerBuilder.kt
git add components/api/dependanger-api/src/main/kotlin/io/github/zenhelix/dependanger/api/Dependanger.kt
git commit -m "feat: add DependangerBuilder.withContextProperty() and SPI-based property resolution"
```

---

### Task 3: Move UpdateCheckSettings to dependanger-updates

**Files:**
- Create: `components/features/dependanger-updates/src/main/kotlin/io/github/zenhelix/dependanger/features/updates/UpdateCheckSettings.kt`
- Create: `components/features/dependanger-updates/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider`
- Modify: `components/features/dependanger-updates/src/main/kotlin/io/github/zenhelix/dependanger/features/updates/UpdateCheckProcessor.kt`

- [ ] **Step 1: Create UpdateCheckSettings with key, provider, and DSL extension**

```kotlin
// components/features/dependanger-updates/src/main/kotlin/io/github/zenhelix/dependanger/features/updates/UpdateCheckSettings.kt
package io.github.zenhelix.dependanger.features.updates

import io.github.zenhelix.dependanger.core.dsl.DependangerDslMarker
import io.github.zenhelix.dependanger.core.dsl.SettingsDsl
import io.github.zenhelix.dependanger.core.model.Repository
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

public val UpdateCheckSettingsKey: ProcessingContextKey<UpdateCheckSettings> =
    ProcessingContextKey("updateCheck")

@Serializable
public data class UpdateCheckSettings(
    val enabled: Boolean,
    val excludePatterns: List<String>,
    val includePrerelease: Boolean,
    val repositories: List<Repository>,
    val timeout: Long,
    val parallelism: Int,
    val cacheDirectory: String?,
    val cacheTtlHours: Long,
) {
    public companion object {
        public const val DEFAULT_TIMEOUT_MS: Long = 30_000
        public const val DEFAULT_PARALLELISM: Int = 10
        public const val DEFAULT_CACHE_TTL_HOURS: Long = 1

        public val DEFAULT: UpdateCheckSettings = UpdateCheckSettings(
            enabled = false,
            excludePatterns = emptyList(),
            includePrerelease = false,
            repositories = emptyList(),
            timeout = DEFAULT_TIMEOUT_MS,
            parallelism = DEFAULT_PARALLELISM,
            cacheDirectory = null,
            cacheTtlHours = DEFAULT_CACHE_TTL_HOURS,
        )
    }
}

public class UpdateCheckSettingsProvider : FeatureSettingsProvider {
    override val settingsKey: String = "updateCheck"

    override fun deserialize(json: JsonElement): Pair<ProcessingContextKey<*>, Any> =
        UpdateCheckSettingsKey to Json.decodeFromJsonElement(UpdateCheckSettings.serializer(), json)
}

@DependangerDslMarker
public class UpdateCheckSettingsDsl {
    public var enabled: Boolean = UpdateCheckSettings.DEFAULT.enabled
    public var excludePatterns: List<String> = UpdateCheckSettings.DEFAULT.excludePatterns
    public var includePrerelease: Boolean = UpdateCheckSettings.DEFAULT.includePrerelease
    public var repositories: List<Repository> = UpdateCheckSettings.DEFAULT.repositories
    public var timeout: Long = UpdateCheckSettings.DEFAULT.timeout
    public var parallelism: Int = UpdateCheckSettings.DEFAULT.parallelism
    public var cacheDirectory: String? = UpdateCheckSettings.DEFAULT.cacheDirectory
    public var cacheTtlHours: Long = UpdateCheckSettings.DEFAULT.cacheTtlHours

    public fun toSettings(): UpdateCheckSettings = UpdateCheckSettings(
        enabled = enabled,
        excludePatterns = excludePatterns,
        includePrerelease = includePrerelease,
        repositories = repositories,
        timeout = timeout,
        parallelism = parallelism,
        cacheDirectory = cacheDirectory,
        cacheTtlHours = cacheTtlHours,
    )
}

public fun SettingsDsl.updateCheck(block: UpdateCheckSettingsDsl.() -> Unit) {
    val settings = UpdateCheckSettingsDsl().apply(block).toSettings()
    putCustomSetting("updateCheck", Json.encodeToJsonElement(UpdateCheckSettings.serializer(), settings))
}
```

- [ ] **Step 2: Register ServiceLoader**

```
// components/features/dependanger-updates/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider
io.github.zenhelix.dependanger.features.updates.UpdateCheckSettingsProvider
```

- [ ] **Step 3: Update UpdateCheckProcessor to read from context[key]**

Replace:
```kotlin
override fun supports(context: ProcessingContext): Boolean =
    context.settings.updateCheck.enabled
```
With:
```kotlin
override fun supports(context: ProcessingContext): Boolean =
    context[UpdateCheckSettingsKey]?.enabled == true
```

Replace:
```kotlin
val settings = context.settings.updateCheck
```
With:
```kotlin
val settings = context.require(UpdateCheckSettingsKey)
```

Replace:
```kotlin
context.settings.repositories
```
With:
```kotlin
context.settings.repositories
```
(This stays the same — `repositories` remains in core Settings.)

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :components:features:dependanger-updates:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add components/features/dependanger-updates/
git commit -m "feat: move UpdateCheckSettings to dependanger-updates module"
```

---

### Task 4: Move SecurityCheckSettings to dependanger-security

Same pattern as Task 3. **Files:**
- Create: `components/features/dependanger-security/src/main/kotlin/io/github/zenhelix/dependanger/features/security/SecurityCheckSettings.kt`
- Create: `components/features/dependanger-security/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider`
- Modify: `components/features/dependanger-security/src/main/kotlin/io/github/zenhelix/dependanger/features/security/SecurityCheckProcessor.kt`

- [ ] **Step 1: Create SecurityCheckSettings with key, provider, and DSL extension**

```kotlin
package io.github.zenhelix.dependanger.features.security

import io.github.zenhelix.dependanger.core.dsl.DependangerDslMarker
import io.github.zenhelix.dependanger.core.dsl.SettingsDsl
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

public val SecurityCheckSettingsKey: ProcessingContextKey<SecurityCheckSettings> =
    ProcessingContextKey("securityCheck")

@Serializable
public data class SecurityCheckSettings(
    val enabled: Boolean,
    val failOnVulnerability: Severity,
    val minSeverity: String,
    val ignoreVulnerabilities: List<String>,
    val timeout: Long,
    val parallelism: Int,
    val cacheDirectory: String?,
    val cacheTtlHours: Long,
) {
    public companion object {
        public const val DEFAULT_TIMEOUT_MS: Long = 30_000
        public const val DEFAULT_PARALLELISM: Int = 10
        public const val DEFAULT_CACHE_TTL_HOURS: Long = 24

        public val DEFAULT: SecurityCheckSettings = SecurityCheckSettings(
            enabled = false,
            failOnVulnerability = Severity.ERROR,
            minSeverity = "HIGH",
            ignoreVulnerabilities = emptyList(),
            timeout = DEFAULT_TIMEOUT_MS,
            parallelism = DEFAULT_PARALLELISM,
            cacheDirectory = null,
            cacheTtlHours = DEFAULT_CACHE_TTL_HOURS,
        )
    }
}

public class SecurityCheckSettingsProvider : FeatureSettingsProvider {
    override val settingsKey: String = "securityCheck"

    override fun deserialize(json: JsonElement): Pair<ProcessingContextKey<*>, Any> =
        SecurityCheckSettingsKey to Json.decodeFromJsonElement(SecurityCheckSettings.serializer(), json)
}

@DependangerDslMarker
public class SecurityCheckSettingsDsl {
    public var enabled: Boolean = SecurityCheckSettings.DEFAULT.enabled
    public var failOnVulnerability: Severity = SecurityCheckSettings.DEFAULT.failOnVulnerability
    public var minSeverity: String = SecurityCheckSettings.DEFAULT.minSeverity
    public var ignoreVulnerabilities: List<String> = SecurityCheckSettings.DEFAULT.ignoreVulnerabilities
    public var timeout: Long = SecurityCheckSettings.DEFAULT.timeout
    public var parallelism: Int = SecurityCheckSettings.DEFAULT.parallelism
    public var cacheDirectory: String? = SecurityCheckSettings.DEFAULT.cacheDirectory
    public var cacheTtlHours: Long = SecurityCheckSettings.DEFAULT.cacheTtlHours

    public fun toSettings(): SecurityCheckSettings = SecurityCheckSettings(
        enabled = enabled,
        failOnVulnerability = failOnVulnerability,
        minSeverity = minSeverity,
        ignoreVulnerabilities = ignoreVulnerabilities,
        timeout = timeout,
        parallelism = parallelism,
        cacheDirectory = cacheDirectory,
        cacheTtlHours = cacheTtlHours,
    )
}

public fun SettingsDsl.securityCheck(block: SecurityCheckSettingsDsl.() -> Unit) {
    val settings = SecurityCheckSettingsDsl().apply(block).toSettings()
    putCustomSetting("securityCheck", Json.encodeToJsonElement(SecurityCheckSettings.serializer(), settings))
}
```

- [ ] **Step 2: Register ServiceLoader**

```
io.github.zenhelix.dependanger.features.security.SecurityCheckSettingsProvider
```

- [ ] **Step 3: Update SecurityCheckProcessor**

Replace `context.settings.securityCheck` with `context[SecurityCheckSettingsKey]` / `context.require(SecurityCheckSettingsKey)`.

- [ ] **Step 4: Verify and commit**

```bash
./gradlew :components:features:dependanger-security:compileKotlin
git add components/features/dependanger-security/
git commit -m "feat: move SecurityCheckSettings to dependanger-security module"
```

---

### Task 5: Move LicenseCheckSettings to dependanger-license

Same pattern. Settings class includes `DualLicensePolicy` enum — move it here from core `Enums.kt`.

- [ ] **Step 1: Create LicenseCheckSettings.kt**

Include `DualLicensePolicy` enum in this file (or a separate file in the same package):

```kotlin
@Serializable
public enum class DualLicensePolicy {
    OR, AND
}
```

Settings fields: `enabled`, `allowedLicenses`, `deniedLicenses`, `dualLicensePolicy`, `failOnDenied`, `failOnUnknown`, `failOnCopyleft`, `warnOnCopyleft`, `warnOnUnknown`, `ignoreLibraries`, `includeTransitives`, `timeout`, `parallelism`, `cacheDirectory`, `cacheTtlHours`.

Constants: `DEFAULT_TIMEOUT_MS = 30_000`, `DEFAULT_PARALLELISM = 10`, `DEFAULT_CACHE_TTL_HOURS = 168`.

- [ ] **Step 2: Register ServiceLoader, update processor, verify, commit**

---

### Task 6: Move TransitiveResolutionSettings to dependanger-transitive

Same pattern. Move `ConflictResolutionStrategy` enum here from core `Enums.kt`.

```kotlin
@Serializable
public enum class ConflictResolutionStrategy {
    HIGHEST, FIRST, FAIL, CONSTRAINT
}
```

Settings fields: `enabled`, `repositories`, `maxDepth`, `maxTransitives`, `conflictResolution`, `includeOptional`, `scopes`, `cacheDirectory`, `cacheTtlHours`.

Constants: `DEFAULT_CACHE_TTL_HOURS = 24`.

- [ ] **Step 1: Create, register, update processor, verify, commit**

---

### Task 7: Move BomCacheSettings to dependanger-maven-resolver

Same pattern. Settings fields: `enabled`, `directory`, `ttlHours`, `ttlSnapshotHours`.

Constants: `DEFAULT_CACHE_TTL_HOURS = 24`, `SNAPSHOT_CACHE_TTL_HOURS = 1`.

**Note:** BomImportProcessor reads `context.settings.bomCache` and `context.settings.repositories`. After refactoring, it reads `context[BomCacheSettingsKey]` for cache config and `context.settings.repositories` for repos (repos stay in core).

- [ ] **Step 1: Create, register, update BomImportProcessor, verify, commit**

---

### Task 8: Move CompatibilityAnalysisSettings to dependanger-analysis

Same pattern. Settings fields: `enabled`, `targetJdk`, `targetKotlin`, `minSeverity`, `failOnErrors`.

**Note:** `CompatibilityCheckProcessor` does NOT read `context.settings.compatibilityAnalysis`. It works purely with `context.originalMetadata.compatibility`. However, `AnalyzeCommand` in CLI sets `compatibilityAnalysis.enabled` and `targetJdk`. The processor `supports()` always returns `true` — enablement is controlled by `PipelineBuilder.enableOptional()` in presets.

The settings should still move for consistency, but the processor won't read them directly. CLI will use `withContextProperty()`.

- [ ] **Step 1: Create, register, verify, commit**

---

### Task 9: Slim core Settings and SettingsDsl

**Files:**
- Modify: `components/core/dependanger-core/src/main/kotlin/io/github/zenhelix/dependanger/core/model/Settings.kt`
- Modify: `components/core/dependanger-core/src/main/kotlin/io/github/zenhelix/dependanger/core/dsl/SettingsDsl.kt`
- Modify: `components/core/dependanger-core/src/main/kotlin/io/github/zenhelix/dependanger/core/model/Enums.kt`
- Modify: `components/core/dependanger-core/src/main/kotlin/io/github/zenhelix/dependanger/core/model/Preset.kt`

- [ ] **Step 1: Slim Settings.kt**

Remove: `SettingsDefaults` object, `TomlSettings`, `BomSettings`, `BomCacheSettings`, `UpdateCheckSettings`, `CompatibilityAnalysisSettings`, `SecurityCheckSettings`, `LicenseCheckSettings`, `TransitiveResolutionSettings`, `ReportSettings`.

Final `Settings`:

```kotlin
@Serializable
public data class Settings(
    val defaultDistribution: String?,
    val strictVersionResolution: Boolean,
    val repositories: List<Repository>,
    val validation: ValidationSettings,
    val customSettings: Map<String, JsonElement>,
) {
    public companion object {
        public val DEFAULT: Settings = Settings(
            defaultDistribution = null,
            strictVersionResolution = false,
            repositories = emptyList(),
            validation = ValidationSettings.DEFAULT,
            customSettings = emptyMap(),
        )
    }
}
```

Keep `ValidationSettings` (used by core `ValidationProcessor`).

- [ ] **Step 2: Slim SettingsDsl.kt**

Remove all feature Trackable fields, feature block methods, feature DSL classes (`TomlSettingsDsl`, `BomSettingsDsl`, `BomCacheSettingsDsl`, `UpdateCheckSettingsDsl`, `CompatibilityAnalysisSettingsDsl`, `SecurityCheckSettingsDsl`, `LicenseCheckSettingsDsl`, `TransitiveResolutionSettingsDsl`, `ReportSettingsDsl`).

Update `applyTo()` — only apply core fields + merge `customSettings` maps.
Update `mergeFrom()` — only merge core fields + merge `customSettings`.
Update `toSettings()` — only core fields.

Final `SettingsDsl`:

```kotlin
@DependangerDslMarker
public class SettingsDsl {
    private val _defaultDistribution = Trackable<String?>(null)
    public var defaultDistribution: String? by _defaultDistribution

    private val _strictVersionResolution = Trackable(false)
    public var strictVersionResolution: Boolean by _strictVersionResolution

    private val _repositories = Trackable<List<Repository>>(emptyList())
    public var repositories: List<Repository> by _repositories

    private val _validationSettings = Trackable(ValidationSettings.DEFAULT)
    public var validationSettings: ValidationSettings by _validationSettings

    private val _customSettings = Trackable<Map<String, JsonElement>>(emptyMap())
    public var customSettings: Map<String, JsonElement> by _customSettings

    public fun validation(block: ValidationSettingsDsl.() -> Unit) {
        val dsl = ValidationSettingsDsl().apply(block)
        validationSettings = dsl.toSettings()
    }

    public fun putCustomSetting(key: String, value: JsonElement) {
        customSettings = customSettings + (key to value)
    }

    public fun applyTo(target: SettingsDsl) {
        if (_defaultDistribution.isSet) target.defaultDistribution = defaultDistribution
        if (_strictVersionResolution.isSet) target.strictVersionResolution = strictVersionResolution
        if (_repositories.isSet) target.repositories = repositories
        if (_validationSettings.isSet) target.validationSettings = validationSettings
        if (_customSettings.isSet) target.customSettings = target.customSettings + customSettings
    }

    public fun mergeFrom(settings: Settings) {
        val defaults = Settings.DEFAULT
        if (settings.defaultDistribution != defaults.defaultDistribution) defaultDistribution = settings.defaultDistribution
        if (settings.strictVersionResolution != defaults.strictVersionResolution) strictVersionResolution = settings.strictVersionResolution
        if (settings.repositories != defaults.repositories) repositories = settings.repositories
        if (settings.validation != defaults.validation) validationSettings = settings.validation
        if (settings.customSettings != defaults.customSettings) customSettings = customSettings + settings.customSettings
    }

    public fun toSettings(): Settings = Settings(
        defaultDistribution = defaultDistribution,
        strictVersionResolution = strictVersionResolution,
        repositories = repositories,
        validation = validationSettings,
        customSettings = customSettings,
    )
}
```

- [ ] **Step 3: Slim Enums.kt**

Remove `DualLicensePolicy`, `ConflictResolutionStrategy`, `ReportFormat`, `ReportSection` (moved to feature modules or deleted with dead settings).

Remaining in `Enums.kt`: `ValidationAction`, `Severity`, `VersionConstraintType`, `KmpTarget`.

- [ ] **Step 4: Update Preset model**

`Preset.settings` field references `Settings`. Since `Settings` is now slim, presets that contain feature settings need to store them in `Settings.customSettings`. This works automatically if DSL extensions serialize into `customSettings`. No code change needed — `Preset.settings: Settings?` still works.

- [ ] **Step 5: Verify core compilation**

Run: `./gradlew :components:core:dependanger-core:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add components/core/dependanger-core/
git commit -m "refactor: slim Settings to core-only fields, remove dead settings"
```

---

### Task 10: Update CLI commands

**Files:**
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/CheckUpdatesCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/SecurityCheckCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/LicenseCheckCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/ResolveTransitivesCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/AnalyzeCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/ReportCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/MetadataService.kt`

- [ ] **Step 1: Update CheckUpdatesCommand**

Replace:
```kotlin
val updatedSettings = metadata.settings.copy(
    updateCheck = metadata.settings.updateCheck.copy(
        enabled = true,
        includePrerelease = includePrerelease,
        excludePatterns = exclude,
        repositories = parseMavenRepositories(repositories) ?: metadata.settings.updateCheck.repositories,
        cacheTtlHours = if (offline) Long.MAX_VALUE else metadata.settings.updateCheck.cacheTtlHours,
    )
)
val updatedMetadata = metadata.copy(settings = updatedSettings)
val dependanger = Dependanger.fromMetadata(updatedMetadata).build()
```

With:
```kotlin
val dependanger = Dependanger.fromMetadata(metadata)
    .withContextProperty(UpdateCheckSettingsKey, UpdateCheckSettings(
        enabled = true,
        includePrerelease = includePrerelease,
        excludePatterns = exclude,
        repositories = parseMavenRepositories(repositories) ?: emptyList(),
        cacheTtlHours = if (offline) Long.MAX_VALUE else UpdateCheckSettings.DEFAULT_CACHE_TTL_HOURS,
        timeout = UpdateCheckSettings.DEFAULT_TIMEOUT_MS,
        parallelism = UpdateCheckSettings.DEFAULT_PARALLELISM,
        cacheDirectory = null,
    ))
    .build()
```

Import: `import io.github.zenhelix.dependanger.features.updates.UpdateCheckSettings` and `import io.github.zenhelix.dependanger.features.updates.UpdateCheckSettingsKey`.

- [ ] **Step 2: Update SecurityCheckCommand**

Same pattern — replace `metadata.settings.copy(securityCheck = ...)` with `builder.withContextProperty(SecurityCheckSettingsKey, SecurityCheckSettings(...))`.

- [ ] **Step 3: Update LicenseCheckCommand**

Same pattern with `LicenseCheckSettingsKey`.

- [ ] **Step 4: Update ResolveTransitivesCommand**

Same pattern with `TransitiveResolutionSettingsKey`.

- [ ] **Step 5: Update AnalyzeCommand**

Same pattern with `CompatibilityAnalysisSettingsKey`.

- [ ] **Step 6: Update ReportCommand**

`ReportCommand` conditionally enables `transitiveResolution.enabled`. Replace with `builder.withContextProperty(TransitiveResolutionSettingsKey, TransitiveResolutionSettings.DEFAULT.copy(enabled = true))` when `--include-transitives` is set.

- [ ] **Step 7: Update MetadataService.emptyMetadata()**

Remove all feature settings from `emptyMetadata()`. The `Settings.DEFAULT` now has only core fields:

```kotlin
public fun emptyMetadata(): DependangerMetadata = DependangerMetadata(
    schemaVersion = DependangerMetadata.SCHEMA_VERSION,
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
```

- [ ] **Step 8: Verify CLI compilation**

Run: `./gradlew :dependanger-cli:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add dependanger-cli/
git commit -m "refactor: update CLI commands to use withContextProperty()"
```

---

### Task 11: Update EffectiveMetadataBuilder

**Files:**
- Modify: `components/core/dependanger-effective/src/main/kotlin/io/github/zenhelix/dependanger/effective/builder/EffectiveMetadataBuilder.kt`

- [ ] **Step 1: Add property support to EffectiveMetadataBuilder**

`EffectiveMetadataBuilder` creates `ProcessingContext` without feature properties. Add a `contextProperties` map:

```kotlin
public class EffectiveMetadataBuilder {
    public var preset: ProcessingPreset = ProcessingPreset.DEFAULT
    public var distribution: String? = null
    public var environment: ProcessingEnvironment = ProcessingEnvironment.DEFAULT
    private val contextProperties: MutableMap<ProcessingContextKey<*>, Any> = mutableMapOf()

    // ... existing methods ...

    public fun <T : Any> withProperty(key: ProcessingContextKey<T>, value: T) {
        contextProperties[key] = value
    }

    public suspend fun build(metadata: DependangerMetadata): EffectiveMetadata {
        val pipeline = ProcessingPipeline {
            addProcessors(coreProcessors())
            this@EffectiveMetadataBuilder.preset.configure(this)
        }

        val activeDistribution = distribution ?: metadata.settings.defaultDistribution

        val context = ProcessingContext(
            originalMetadata = metadata,
            settings = metadata.settings,
            environment = environment,
            activeDistribution = activeDistribution,
            callback = null,
            properties = contextProperties.toMap(),
        )

        return pipeline.process(context)
    }
}
```

- [ ] **Step 2: Verify and commit**

```bash
./gradlew :components:core:dependanger-effective:compileKotlin
git add components/core/dependanger-effective/
git commit -m "refactor: add property support to EffectiveMetadataBuilder"
```

---

### Task 12: Update ReportFormat and ReportSection usage

**Files:**
- Modify: `components/features/dependanger-report/src/main/kotlin/io/github/zenhelix/dependanger/features/report/` (move enums here)
- Modify: `components/core/dependanger-core/src/main/kotlin/io/github/zenhelix/dependanger/core/model/Enums.kt` (remove)
- Modify: CLI and test files that import these enums

- [ ] **Step 1: Move ReportFormat and ReportSection to dependanger-report**

Create or add to existing file in `dependanger-report`:

```kotlin
package io.github.zenhelix.dependanger.features.report

import kotlinx.serialization.Serializable

@Serializable
public enum class ReportFormat {
    JSON, YAML, MARKDOWN, HTML
}

@Serializable
public enum class ReportSection {
    SUMMARY, LIBRARIES, PLUGINS, BUNDLES, VERSIONS, UPDATES, COMPATIBILITY,
    VULNERABILITIES, DEPRECATED, LICENSES, TRANSITIVES, CONSTRAINTS, VALIDATION
}
```

- [ ] **Step 2: Update imports across codebase, verify, commit**

---

### Task 13: Full build verification and test fixes

- [ ] **Step 1: Run full build**

Run: `./gradlew build`

- [ ] **Step 2: Fix compilation errors**

Fix any remaining import issues across test files. Tests that create `Settings(...)` with feature fields need updating to use `Settings.DEFAULT` or `Settings(... customSettings = mapOf("updateCheck" to ...))`.

- [ ] **Step 3: Run all tests**

Run: `./gradlew test`

- [ ] **Step 4: Fix failing tests**

Update test assertions that reference removed fields. Tests in `dependanger-api/src/test/` that configure feature settings via DSL need the feature module extensions imported.

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "fix: update tests for settings decoupling"
```

---

## Parallelization Guide

| Tasks | Can run in parallel? | Notes |
|-------|---------------------|-------|
| Tasks 3-8 | Yes | Each feature module is independent |
| Task 9 | No | Depends on 3-8 completing |
| Tasks 10-12 | Yes | Independent concerns after core is slimmed |
| Task 13 | No | Final verification, depends on everything |

## Risk Areas

1. **Preset merge via `applyTo()`/`mergeFrom()`** — feature settings now merge via `customSettings` map. Per-field tracking is lost (entire map is one `Trackable`). This is acceptable since presets rarely override individual feature settings fields.

2. **`ReportFormat`/`ReportSection` enums** are referenced by `ReportProvider` SPI in `dependanger-effective`. If the SPI uses these types, the effective module would need a dependency on report module, creating a cycle. Check `ReportProvider` interface — if it uses these enums, define abstract equivalents in effective and let report module implement them.

3. **Test fixtures in CLI** (`testFixtures/`) reference feature settings types. Imports need updating.

4. **`ProcessingPresetConfigurator`** references `ProcessorIds` for feature processors (UPDATE_CHECK, SECURITY_CHECK, etc.). These IDs stay in `ProcessorIds` in effective module — no change needed.
