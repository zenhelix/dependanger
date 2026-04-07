# Refactoring: Design Patterns & Duplication Cleanup

**Date:** 2026-04-07
**Status:** Approved
**Scope:** 4 refactoring items across feature-support, gradle-plugin, cli modules

---

## 1. Split AbstractParallelNetworkProcessor Hierarchy

### Problem

`AbstractParallelNetworkProcessor<S>` resolves both `HttpClientFactory` and Maven-specific infrastructure (repositories, credentials) via
`NetworkProcessorInfrastructure`. `SecurityCheckProcessor` doesn't use Maven repositories (it calls the OSV API), so it bypasses the
abstract base entirely and implements `ParallelMetadataProcessor` directly. This creates inconsistency.

### Design

Split into two-level hierarchy in `dependanger-feature-support`:

```
ParallelMetadataProcessor (existing interface)
    └── AbstractParallelFeatureProcessor<S>     — settings + httpClientFactory
            └── AbstractParallelMavenProcessor<S>   — + repositories + credentials
```

**`AbstractParallelFeatureProcessor<S : Any>`** (new):

- Abstract `settingsKey: ProcessingContextKey<S>`
- Resolves `HttpClientFactory` from context (fallback `DefaultHttpClientFactory`)
- Abstract `executeWithInfrastructure(metadata, context, settings, httpClientFactory): ParallelResult`
- Final `processParallel()` — extracts settings, resolves httpClientFactory, delegates

**`AbstractParallelMavenProcessor<S : Any>`** (renamed from current `AbstractParallelNetworkProcessor`):

- Extends `AbstractParallelFeatureProcessor<S>`
- Abstract `featureRepositories(settings): List<Repository>` (existing)
- Resolves `NetworkProcessorInfrastructure` (repositories + credentials + httpClientFactory)
- Abstract `executeWithMavenInfrastructure(metadata, context, settings, infrastructure): ParallelResult`
- Implements `executeWithInfrastructure()` by building `NetworkProcessorInfrastructure` and delegating

### Migrations

| Processor                | Current base                         | New base                                               |
|--------------------------|--------------------------------------|--------------------------------------------------------|
| `UpdateCheckProcessor`   | `AbstractParallelNetworkProcessor`   | `AbstractParallelMavenProcessor`                       |
| `LicenseCheckProcessor`  | `AbstractParallelNetworkProcessor`   | `AbstractParallelMavenProcessor` (if uses Maven repos) |
| `SecurityCheckProcessor` | `ParallelMetadataProcessor` (direct) | `AbstractParallelFeatureProcessor`                     |

### Files affected

- `components/shared/dependanger-feature-support/src/main/kotlin/.../AbstractParallelNetworkProcessor.kt` — rename to
  `AbstractParallelMavenProcessor`, extract parent
- New: `AbstractParallelFeatureProcessor.kt`
- `components/features/dependanger-updates/.../UpdateCheckProcessor.kt` — rename method
- `components/features/dependanger-security/.../SecurityCheckProcessor.kt` — extend new base
- `components/features/dependanger-license/.../LicenseCheckProcessor.kt` — rename method (if applicable)

---

## 2. AnalyticalTaskRunner for Gradle Tasks

### Problem

`CheckUpdatesTask`, `SecurityCheckTask`, `LicenseCheckTask`, `ResolveTransitivesTask`, `AnalyzeTask` repeat identical boilerplate: read
metadata from extension, get failOnError, runWithErrorHandling, build Dependanger, runBlocking process, handle errors.

### Design

New class `AnalyticalTaskRunner` (separate file in gradle-plugin module):

```kotlin
class AnalyticalTaskRunner(
    private val extension: DependangerExtension,
    private val logger: Logger,
) {
    fun run(
        configure: DependangerBuilder.() -> Unit = {},
        handle: AnalyticalTaskContext.(DependangerResult) -> Unit,
    ) {
        val metadata = extension.toMetadata()
        val failOnError = extension.failOnError.get()
        AbstractDependangerTask.runWithErrorHandling(failOnError, logger) {
            val dependanger = Dependanger.fromMetadata(metadata)
                .apply(configure)
                .build()
            val result = runBlocking { dependanger.process() }
            if (!result.isSuccess) {
                DependangerTaskHelper.handleProcessingErrors(result, failOnError, logger)
                return@runWithErrorHandling
            }
            AnalyticalTaskContext(logger, failOnError).handle(result)
        }
    }
}

class AnalyticalTaskContext(
    val logger: Logger,
    val failOnError: Boolean,
)
```

Note: `runWithErrorHandling` is currently an inline method on `AbstractDependangerTask`. It may need to be extracted to a standalone
function or to `DependangerTaskHelper` so `AnalyticalTaskRunner` can use it.

### Migrations

Each analytical task's `execute()` body becomes ~5-10 lines: construct runner, call `run(configure, handle)` with only feature-specific
configuration and result rendering.

### Files affected

- New: `AnalyticalTaskRunner.kt`
- `AbstractDependangerTask.kt` — extract `runWithErrorHandling` to be reusable
- `CheckUpdatesTask.kt` — simplify
- `SecurityCheckTask.kt` — simplify
- `LicenseCheckTask.kt` — simplify
- `ResolveTransitivesTask.kt` — simplify
- `AnalyzeTask.kt` — simplify (note: uses `handleFilteredDiagnostics`, may need variant)

---

## 3. Migrate SecurityCheckCommand to PipelineRunner

### Problem

`SecurityCheckCommand` (120 lines) manually creates `MetadataService`, `OutputFormatter`, and calls `CoroutineRunner.run {}` — the "old
style" before `PipelineRunner` was introduced. All other analytical commands (`CheckUpdatesCommand`, `LicenseCheckCommand`,
`ResolveTransitivesCommand`) already use `PipelineRunner`.

### Design

Rewrite `SecurityCheckCommand` to use `PipelineRunner(this, opts).run(configure, handle)`.

**SARIF handling (variant B):** Keep `--format` as a command-specific option (not in `PipelineOptions`), since SARIF is security-specific.
The command will use `PipelineOptions` only for `--input` and `--distribution`.

The `configure` block will set `SecurityCheckSettings` into context (same as current). The `handle` block will branch on format (
text/json/sarif) and render accordingly.

### Files affected

- `dependanger-cli/.../commands/SecurityCheckCommand.kt` — rewrite

---

## 4. Extract writeOutputIfRequested to PipelineHandlerContext

### Problem

`CheckUpdatesCommand`, `LicenseCheckCommand`, `ResolveTransitivesCommand` repeat identical output-to-file logic:

```kotlin
output?.let { outputFile ->
    val outputPath = Path.of(outputFile)
    val jsonString = CliDefaults.CLI_JSON.encodeToString(serializer, data)
    outputPath.writeText(jsonString)
    formatter.success("Report written to $outputPath")
}
```

### Design

Add two methods to `PipelineHandlerContext`:

```kotlin
// For typed serializable data
fun <T> writeOutputIfRequested(output: String?, data: T, serializer: KSerializer<T>) { ... }

// For pre-formatted string content (SARIF, custom formats)
fun writeOutputIfRequested(output: String?, content: String) { ... }
```

### Files affected

- `dependanger-cli/.../runner/PipelineRunner.kt` — add methods to `PipelineHandlerContext`
- `dependanger-cli/.../commands/CheckUpdatesCommand.kt` — use new method
- `dependanger-cli/.../commands/LicenseCheckCommand.kt` — use new method
- `dependanger-cli/.../commands/ResolveTransitivesCommand.kt` — use new method
- `dependanger-cli/.../commands/SecurityCheckCommand.kt` — use new method (after migration)

---

## Dependency Order

Items 1 and 2 are independent of each other and can be done in parallel.

Item 3 depends on item 4 (SecurityCheckCommand migration should use `writeOutputIfRequested`).

Item 4 can be done first or together with item 3.

Recommended execution order:

1. Items 1 + 2 in parallel
2. Item 4 (add `writeOutputIfRequested`)
3. Item 3 (migrate SecurityCheckCommand, using the new method)
4. Update existing commands to use `writeOutputIfRequested`
