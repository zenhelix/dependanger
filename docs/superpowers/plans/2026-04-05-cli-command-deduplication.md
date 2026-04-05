# CLI Command Deduplication via OptionGroups + Runners

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate ~30 instances of duplicated CLI infrastructure code by introducing Clikt OptionGroups for shared options and Runner composition objects for shared execution flows.

**Architecture:** Two Clikt `OptionGroup` classes (`MetadataOptions`, `PipelineOptions`) encapsulate repeated option declarations. Two Runner classes (`MetadataRunner`, `PipelineRunner`) encapsulate the read-transform-write and read-process-handle flows respectively. A `parseEnum<E>()` utility replaces 3+ duplicated try/catch blocks. Commands delegate to these components via composition, remaining flat (single-level inheritance from `CliktCommand`).

**Tech Stack:** Kotlin 2.1.20, Clikt 5.0, Mordant, kotlinx-serialization, JUnit 5, AssertJ

---

## File Structure

### New files to create:
- `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/options/MetadataOptions.kt` - OptionGroup for `-i`/`-o` shared across mutation commands
- `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/options/PipelineOptions.kt` - OptionGroup for `-i`/`-d`/`--format` shared across pipeline commands
- `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/runner/MetadataRunner.kt` - Composition helper for read-transform-write flow
- `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/runner/PipelineRunner.kt` - Composition helper for read-process-handle flow

### Files to modify:
- `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/CliParsers.kt` - Add `parseEnum<E>()` utility
- All 15 mutation commands (Add*, Remove*, Update*, MigrateDeprecated) - Use MetadataOptions + MetadataRunner
- All 9 pipeline commands (Process, Validate, Generate, Analyze, Report, ResolveTransitives, CheckUpdates, SecurityCheck, LicenseCheck) - Use PipelineOptions + PipelineRunner

### Test files:
- `dependanger-cli/src/test/kotlin/io/github/zenhelix/dependanger/cli/MetadataRunnerTest.kt` - Unit tests for MetadataRunner
- `dependanger-cli/src/test/kotlin/io/github/zenhelix/dependanger/cli/PipelineRunnerTest.kt` - Unit tests for PipelineRunner
- `dependanger-cli/src/test/kotlin/io/github/zenhelix/dependanger/cli/ParseEnumTest.kt` - Unit tests for parseEnum
- All existing tests must continue passing unchanged

---

### Task 1: Add `parseEnum<E>()` utility

**Files:**
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/CliParsers.kt`
- Create: `dependanger-cli/src/test/kotlin/io/github/zenhelix/dependanger/cli/ParseEnumTest.kt`

- [ ] **Step 1: Write the failing test**

Create `dependanger-cli/src/test/kotlin/io/github/zenhelix/dependanger/cli/ParseEnumTest.kt`:

```kotlin
package io.github.zenhelix.dependanger.cli

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ParseEnumTest {

    private enum class TestEnum { FOO, BAR_BAZ }

    @Test
    fun `parses exact match`() {
        assertThat(parseEnum<TestEnum>("FOO", "test")).isEqualTo(TestEnum.FOO)
    }

    @Test
    fun `parses case-insensitive`() {
        assertThat(parseEnum<TestEnum>("foo", "test")).isEqualTo(TestEnum.FOO)
        assertThat(parseEnum<TestEnum>("bar_baz", "test")).isEqualTo(TestEnum.BAR_BAZ)
    }

    @Test
    fun `throws CliException for unknown value`() {
        assertThatThrownBy { parseEnum<TestEnum>("UNKNOWN", "test enum") }
            .isInstanceOf(CliException.InvalidArgument::class.java)
            .hasMessageContaining("Unknown test enum 'UNKNOWN'")
            .hasMessageContaining("FOO")
            .hasMessageContaining("BAR_BAZ")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew :dependanger-cli:test --tests "io.github.zenhelix.dependanger.cli.ParseEnumTest" --no-daemon`

Expected: FAIL — `parseEnum` function does not exist yet.

- [ ] **Step 3: Implement parseEnum**

Add to the end of `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/CliParsers.kt`:

```kotlin
public inline fun <reified E : Enum<E>> parseEnum(value: String, label: String): E = try {
    enumValueOf<E>(value.uppercase())
} catch (_: IllegalArgumentException) {
    throw CliException.InvalidArgument(
        "Unknown $label '$value'. Available: ${enumValues<E>().joinToString { it.name }}"
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew :dependanger-cli:test --tests "io.github.zenhelix.dependanger.cli.ParseEnumTest" --no-daemon`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2
git add dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/CliParsers.kt dependanger-cli/src/test/kotlin/io/github/zenhelix/dependanger/cli/ParseEnumTest.kt
git commit -m "feat(cli): add parseEnum utility for type-safe enum parsing"
```

---

### Task 2: Create MetadataOptions OptionGroup

**Files:**
- Create: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/options/MetadataOptions.kt`

- [ ] **Step 1: Create the OptionGroup**

Create `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/options/MetadataOptions.kt`:

```kotlin
package io.github.zenhelix.dependanger.cli.options

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.CliDefaults

public class MetadataOptions : OptionGroup("Metadata file options") {
    public val input: String by option("-i", "--input", help = "Input metadata file")
        .default(CliDefaults.METADATA_FILE)
    public val output: String? by option("-o", "--output", help = "Output file (defaults to input)")
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew :dependanger-cli:compileKotlin --no-daemon`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2
git add dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/options/MetadataOptions.kt
git commit -m "feat(cli): add MetadataOptions OptionGroup for shared input/output options"
```

---

### Task 3: Create MetadataRunner

**Files:**
- Create: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/runner/MetadataRunner.kt`
- Create: `dependanger-cli/src/test/kotlin/io/github/zenhelix/dependanger/cli/MetadataRunnerTest.kt`

- [ ] **Step 1: Write the failing test**

Create `dependanger-cli/src/test/kotlin/io/github/zenhelix/dependanger/cli/MetadataRunnerTest.kt`:

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.testing.test
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.Version
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class MetadataRunnerTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `runs read-transform-write cycle`() {
        val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())

        val command = object : CliktCommand(name = "test") {
            val opts by MetadataOptions()

            override fun run() {
                MetadataRunner(this, opts).run { metadata ->
                    val newVersion = Version(name = "test-version", value = "1.0.0", fallbacks = emptyList())
                    metadata.copy(versions = metadata.versions + newVersion) to "Added test-version"
                }
            }
        }

        val result = command.test(listOf("-i", metadataFile.toString()))

        assertThat(result.statusCode).isEqualTo(0)
        assertThat(result.output).contains("[OK]")
        val updated = CliTestSupport.readMetadata(metadataFile)
        assertThat(updated.versions.any { it.name == "test-version" }).isTrue()
    }

    @Test
    fun `writes to separate output file when specified`() {
        val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())
        val outputFile = tempDir.resolve("output.json")

        val command = object : CliktCommand(name = "test") {
            val opts by MetadataOptions()

            override fun run() {
                MetadataRunner(this, opts).run { metadata ->
                    val newVersion = Version(name = "out-version", value = "2.0.0", fallbacks = emptyList())
                    metadata.copy(versions = metadata.versions + newVersion) to "Added out-version"
                }
            }
        }

        val result = command.test(listOf("-i", metadataFile.toString(), "-o", outputFile.toString()))

        assertThat(result.statusCode).isEqualTo(0)
        val updated = CliTestSupport.readMetadata(outputFile)
        assertThat(updated.versions.any { it.name == "out-version" }).isTrue()
        // Original file should be unchanged
        val original = CliTestSupport.readMetadata(metadataFile)
        assertThat(original.versions.none { it.name == "out-version" }).isTrue()
    }

    @Test
    fun `handles CliException with error output`() {
        val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())

        val command = object : CliktCommand(name = "test") {
            val opts by MetadataOptions()

            override fun run() {
                MetadataRunner(this, opts).run { _ ->
                    throw CliException.AliasNotFound("Test", "missing")
                }
            }
        }

        val result = command.test(listOf("-i", metadataFile.toString()))

        assertThat(result.statusCode).isEqualTo(1)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew :dependanger-cli:test --tests "io.github.zenhelix.dependanger.cli.MetadataRunnerTest" --no-daemon`

Expected: FAIL — `MetadataRunner` does not exist.

- [ ] **Step 3: Implement MetadataRunner**

Create `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/runner/MetadataRunner.kt`:

```kotlin
package io.github.zenhelix.dependanger.cli.runner

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import io.github.zenhelix.dependanger.cli.MetadataService
import io.github.zenhelix.dependanger.cli.OutputFormatter
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.withErrorHandling
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import java.nio.file.Path

public class MetadataRunner(
    command: CliktCommand,
    private val opts: MetadataOptions,
) {
    public val formatter: OutputFormatter = OutputFormatter(terminal = command.terminal)
    private val metadataService: MetadataService = MetadataService()

    public fun run(transform: (DependangerMetadata) -> Pair<DependangerMetadata, String>) {
        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(opts.input))
            val (updated, message) = transform(metadata)
            metadataService.write(updated, Path.of(opts.output ?: opts.input))
            formatter.success(message)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew :dependanger-cli:test --tests "io.github.zenhelix.dependanger.cli.MetadataRunnerTest" --no-daemon`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2
git add dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/runner/MetadataRunner.kt dependanger-cli/src/test/kotlin/io/github/zenhelix/dependanger/cli/MetadataRunnerTest.kt
git commit -m "feat(cli): add MetadataRunner for read-transform-write composition"
```

---

### Task 4: Create PipelineOptions OptionGroup

**Files:**
- Create: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/options/PipelineOptions.kt`

- [ ] **Step 1: Create the OptionGroup**

Create `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/options/PipelineOptions.kt`:

```kotlin
package io.github.zenhelix.dependanger.cli.options

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.CliDefaults

public class PipelineOptions : OptionGroup("Pipeline options") {
    public val input: String by option("-i", "--input", help = "Input metadata file")
        .default(CliDefaults.METADATA_FILE)
    public val distribution: String? by option("-d", "--distribution", help = "Active distribution")
    public val format: String by option("--format", help = "Output format: text, json")
        .default(CliDefaults.OUTPUT_FORMAT_TEXT)
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew :dependanger-cli:compileKotlin --no-daemon`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2
git add dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/options/PipelineOptions.kt
git commit -m "feat(cli): add PipelineOptions OptionGroup for shared pipeline options"
```

---

### Task 5: Create PipelineRunner

**Files:**
- Create: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/runner/PipelineRunner.kt`
- Create: `dependanger-cli/src/test/kotlin/io/github/zenhelix/dependanger/cli/PipelineRunnerTest.kt`

- [ ] **Step 1: Write the failing test**

Create `dependanger-cli/src/test/kotlin/io/github/zenhelix/dependanger/cli/PipelineRunnerTest.kt`:

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.testing.test
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.runner.PipelineRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PipelineRunnerTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `runs pipeline and invokes handler`() {
        val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())

        CliTestSupport.mockDependangerResult().use {
            val command = object : CliktCommand(name = "test") {
                val opts by PipelineOptions()

                override fun run() {
                    val runner = PipelineRunner(this, opts)
                    runner.run(
                        handle = { result ->
                            runner.formatter.success("Pipeline completed: ${result.isSuccess}")
                        }
                    )
                }
            }

            val result = command.test(listOf("-i", metadataFile.toString()))

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).contains("Pipeline completed: true")
        }
    }

    @Test
    fun `passes distribution to pipeline`() {
        val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())

        CliTestSupport.mockDependangerResult().use {
            val command = object : CliktCommand(name = "test") {
                val opts by PipelineOptions()

                override fun run() {
                    val runner = PipelineRunner(this, opts)
                    runner.run(
                        handle = { result ->
                            runner.formatter.success("OK")
                        }
                    )
                }
            }

            val result = command.test(listOf("-i", metadataFile.toString(), "-d", "mobile"))

            assertThat(result.statusCode).isEqualTo(0)
        }
    }

    @Test
    fun `creates formatter with json mode based on format option`() {
        val metadataFile = CliTestSupport.writeMetadata(tempDir, CliTestSupport.minimalMetadata())

        CliTestSupport.mockDependangerResult().use {
            val command = object : CliktCommand(name = "test") {
                val opts by PipelineOptions()

                override fun run() {
                    val runner = PipelineRunner(this, opts)
                    runner.run(
                        handle = { _ ->
                            // In JSON mode, success() is suppressed — no output
                            runner.formatter.success("This should be suppressed")
                        }
                    )
                }
            }

            val result = command.test(listOf("-i", metadataFile.toString(), "--format", "json"))

            assertThat(result.statusCode).isEqualTo(0)
            assertThat(result.output).doesNotContain("This should be suppressed")
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew :dependanger-cli:test --tests "io.github.zenhelix.dependanger.cli.PipelineRunnerTest" --no-daemon`

Expected: FAIL — `PipelineRunner` does not exist.

- [ ] **Step 3: Implement PipelineRunner**

Create `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/runner/PipelineRunner.kt`:

```kotlin
package io.github.zenhelix.dependanger.cli.runner

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.DependangerBuilder
import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.cli.CliDefaults
import io.github.zenhelix.dependanger.cli.CoroutineRunner
import io.github.zenhelix.dependanger.cli.MetadataService
import io.github.zenhelix.dependanger.cli.OutputFormatter
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.withErrorHandling
import java.nio.file.Path

public class PipelineRunner(
    command: CliktCommand,
    private val opts: PipelineOptions,
) {
    public val formatter: OutputFormatter = OutputFormatter(
        jsonMode = opts.format == CliDefaults.OUTPUT_FORMAT_JSON,
        terminal = command.terminal,
    )
    private val metadataService: MetadataService = MetadataService()

    public fun run(
        configure: DependangerBuilder.() -> Unit = {},
        handle: (DependangerResult) -> Unit,
    ) {
        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(opts.input))
            val builder = Dependanger.fromMetadata(metadata)
            builder.configure()
            val dependanger = builder.build()
            val result = CoroutineRunner.run {
                dependanger.process(opts.distribution)
            }
            handle(result)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew :dependanger-cli:test --tests "io.github.zenhelix.dependanger.cli.PipelineRunnerTest" --no-daemon`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2
git add dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/runner/PipelineRunner.kt dependanger-cli/src/test/kotlin/io/github/zenhelix/dependanger/cli/PipelineRunnerTest.kt
git commit -m "feat(cli): add PipelineRunner for read-process-handle composition"
```

---

### Task 6: Migrate simple mutation commands to MetadataRunner

Migrate these commands: `AddVersionCommand`, `AddPluginCommand`, `AddBundleCommand`, `AddBomCommand`, `RemovePluginCommand`, `RemoveBomCommand`. These are the simplest mutation commands — no reference checks, no force flags (except AddBom has validation).

**Files:**
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/AddVersionCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/AddPluginCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/AddBundleCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/AddBomCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/RemovePluginCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/RemoveBomCommand.kt`

- [ ] **Step 1: Migrate AddVersionCommand**

Replace `AddVersionCommand` content with:

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.Version

public class AddVersionCommand : CliktCommand(name = "version") {
    override fun help(context: Context): String = "Add a version alias to metadata.json"

    private val opts by MetadataOptions()
    public val alias: String by argument(help = "Version alias name")
    public val value: String by argument(help = "Version value")
    public val fallback: String? by option("--fallback", help = "Fallback condition=value (e.g. jdkBelow(17)=2.7.18)")

    override fun run() = MetadataRunner(this, opts).run { metadata ->
        if (metadata.versions.any { it.name == alias }) {
            throw CliException.DuplicateAlias("Version", alias)
        }

        val newVersion = Version(name = alias, value = value, fallbacks = emptyList())
        metadata.copy(versions = metadata.versions + newVersion) to "Added version '$alias'"
    }
}
```

- [ ] **Step 2: Migrate AddPluginCommand**

Replace `AddPluginCommand` content with:

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.Plugin

public class AddPluginCommand : CliktCommand(name = "plugin") {
    override fun help(context: Context): String = "Add a Gradle plugin to metadata.json"

    private val opts by MetadataOptions()
    public val alias: String by argument(help = "Plugin alias")
    public val pluginId: String by argument(help = "Gradle plugin ID")
    public val version: String? by option("-v", "--version", help = "Plugin version or ref:alias")
    public val tags: String? by option("-t", "--tags", help = "Tags (comma-separated)")

    override fun run() = MetadataRunner(this, opts).run { metadata ->
        if (metadata.plugins.any { it.alias == alias }) {
            throw CliException.DuplicateAlias("Plugin", alias)
        }

        val resolvedVersion = parseVersionRef(version)

        val newPlugin = Plugin(
            alias = alias,
            id = pluginId,
            version = resolvedVersion,
            tags = parseCommaSeparated(tags).toSet(),
        )
        metadata.copy(plugins = metadata.plugins + newPlugin) to "Added plugin '$alias'"
    }
}
```

- [ ] **Step 3: Migrate AddBundleCommand**

Replace `AddBundleCommand` content with:

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.Bundle

public class AddBundleCommand : CliktCommand(name = "bundle") {
    override fun help(context: Context): String = "Add a bundle to metadata.json"

    private val opts by MetadataOptions()
    public val name: String by argument(help = "Bundle name")
    public val libraries: String? by option("--libraries", help = "Library aliases (comma-separated)")
    public val extends: String? by option("--extends", help = "Bundle names to extend (comma-separated)")

    override fun run() = MetadataRunner(this, opts).run { metadata ->
        if (metadata.bundles.any { it.alias == name }) {
            throw CliException.DuplicateAlias("Bundle", name)
        }

        val newBundle = Bundle(
            alias = name,
            libraries = parseCommaSeparated(libraries),
            extends = parseCommaSeparated(extends),
        )
        metadata.copy(bundles = metadata.bundles + newBundle) to "Added bundle '$name'"
    }
}
```

- [ ] **Step 4: Migrate AddBomCommand**

Replace `AddBomCommand` content with:

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.BomImport
import io.github.zenhelix.dependanger.core.model.VersionReference

public class AddBomCommand : CliktCommand(name = "bom") {
    override fun help(context: Context): String = "Add a BOM import to metadata.json"

    private val opts by MetadataOptions()
    public val coordinates: String by argument(help = "Maven BOM coordinates (group:artifact[:version])")
    public val alias: String? by option("--alias", help = "BOM alias (defaults to artifactId)")
    public val version: String? by option("-v", "--version", help = "BOM version or ref:alias")

    override fun run() = MetadataRunner(this, opts).run { metadata ->
        val coords = parseMavenCoordinates(coordinates)
        val resolvedAlias = alias ?: coords.artifact
        val resolvedVersion = version?.let { parseVersionRef(it) }
            ?: coords.version?.let { VersionReference.Literal(version = it) }
            ?: throw CliException.InvalidArgument("BOM version is required")

        if (metadata.bomImports.any { it.alias == resolvedAlias }) {
            throw CliException.DuplicateAlias("BOM import", resolvedAlias)
        }

        val newBom = BomImport(
            alias = resolvedAlias,
            group = coords.group,
            artifact = coords.artifact,
            version = resolvedVersion,
        )
        metadata.copy(bomImports = metadata.bomImports + newBom) to "Added BOM import '$resolvedAlias'"
    }
}
```

- [ ] **Step 5: Migrate RemovePluginCommand**

Replace `RemovePluginCommand` content with:

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner

public class RemovePluginCommand : CliktCommand(name = "plugin") {
    override fun help(context: Context): String = "Remove a plugin from metadata.json"

    private val opts by MetadataOptions()
    public val alias: String by argument(help = "Plugin alias to remove")

    override fun run() = MetadataRunner(this, opts).run { metadata ->
        if (metadata.plugins.none { it.alias == alias }) {
            throw CliException.AliasNotFound("Plugin", alias)
        }

        metadata.copy(plugins = metadata.plugins.filter { it.alias != alias }) to "Removed plugin '$alias'"
    }
}
```

- [ ] **Step 6: Migrate RemoveBomCommand**

Replace `RemoveBomCommand` content with:

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner

public class RemoveBomCommand : CliktCommand(name = "bom") {
    override fun help(context: Context): String = "Remove a BOM import from metadata.json"

    private val opts by MetadataOptions()
    public val alias: String by argument(help = "BOM alias to remove")

    override fun run() = MetadataRunner(this, opts).run { metadata ->
        if (metadata.bomImports.none { it.alias == alias }) {
            throw CliException.AliasNotFound("BOM", alias)
        }

        metadata.copy(bomImports = metadata.bomImports.filter { it.alias != alias }) to "Removed BOM '$alias'"
    }
}
```

- [ ] **Step 7: Run all existing tests to verify no regressions**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew :dependanger-cli:test --no-daemon`

Expected: ALL PASS

- [ ] **Step 8: Commit**

```bash
cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2
git add dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/AddVersionCommand.kt dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/AddPluginCommand.kt dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/AddBundleCommand.kt dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/AddBomCommand.kt dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/RemovePluginCommand.kt dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/RemoveBomCommand.kt
git commit -m "refactor(cli): migrate simple mutation commands to MetadataRunner"
```

---

### Task 7: Migrate remaining mutation commands to MetadataRunner

Migrate: `AddLibraryCommand`, `AddDistributionCommand`, `RemoveVersionCommand`, `RemoveLibraryCommand`, `RemoveBundleCommand`, `RemoveDistributionCommand`, `UpdateVersionCommand`, `UpdateLibraryCommand`.

These are more complex — they have `--force` flags, reference checks, or multi-field updates.

**Files:**
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/AddLibraryCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/AddDistributionCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/RemoveVersionCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/RemoveLibraryCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/RemoveBundleCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/RemoveDistributionCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/UpdateVersionCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/UpdateLibraryCommand.kt`

- [ ] **Step 1: Migrate AddLibraryCommand**

Replace `AddLibraryCommand` content with:

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.Library
import io.github.zenhelix.dependanger.core.model.VersionReference

public class AddLibraryCommand : CliktCommand(name = "library") {
    override fun help(context: Context): String = "Add a library to metadata.json"

    private val opts by MetadataOptions()
    public val alias: String by argument(help = "Library alias")
    public val coordinates: String by argument(help = "Maven coordinates (group:artifact[:version])")
    public val version: String? by option("-v", "--version", help = "Library version")
    public val versionRef: String? by option("--version-ref", help = "Named version reference")
    public val tags: String? by option("-t", "--tags", help = "Tags (comma-separated)")

    override fun run() = MetadataRunner(this, opts).run { metadata ->
        val coords = parseMavenCoordinates(coordinates)

        val resolvedVersion: VersionReference? = when {
            versionRef != null -> VersionReference.Reference(name = versionRef!!)
            version != null    -> VersionReference.Literal(version = version!!)
            else               -> coords.version?.let { VersionReference.Literal(version = it) }
        }

        if (metadata.libraries.any { it.alias == alias }) {
            throw CliException.DuplicateAlias("Library", alias)
        }

        val newLibrary = Library(
            alias = alias,
            group = coords.group,
            artifact = coords.artifact,
            version = resolvedVersion,
            description = null,
            tags = parseCommaSeparated(tags).toSet(),
            requires = null,
            deprecation = null,
            license = null,
            constraints = emptyList(),
            isPlatform = false,
        )
        metadata.copy(libraries = metadata.libraries + newLibrary) to "Added library '$alias'"
    }
}
```

- [ ] **Step 2: Migrate AddDistributionCommand**

Replace `AddDistributionCommand` content with:

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.Distribution
import io.github.zenhelix.dependanger.core.model.filter.BundleFilter
import io.github.zenhelix.dependanger.core.model.filter.LibraryFilterSpec
import io.github.zenhelix.dependanger.core.model.filter.TagExclude
import io.github.zenhelix.dependanger.core.model.filter.TagFilter
import io.github.zenhelix.dependanger.core.model.filter.TagInclude

public class AddDistributionCommand : CliktCommand(name = "distribution") {
    override fun help(context: Context): String = "Add a distribution to metadata.json"

    private val opts by MetadataOptions()
    public val name: String by argument(help = "Distribution name")
    public val includeTags: String? by option("--include-tags", help = "Tags to include (comma-separated)")
    public val excludeTags: String? by option("--exclude-tags", help = "Tags to exclude (comma-separated)")
    public val includeBundles: String? by option("--include-bundles", help = "Bundles to include (comma-separated)")

    override fun run() = MetadataRunner(this, opts).run { metadata ->
        if (metadata.distributions.any { it.name == name }) {
            throw CliException.DuplicateAlias("Distribution", name)
        }

        val tagFilter = if (includeTags != null || excludeTags != null) {
            TagFilter(
                includes = if (includeTags != null) {
                    listOf(TagInclude(anyOf = parseCommaSeparated(includeTags).toSet(), allOf = emptySet()))
                } else {
                    emptyList()
                },
                excludes = if (excludeTags != null) {
                    listOf(TagExclude(anyOf = parseCommaSeparated(excludeTags).toSet()))
                } else {
                    emptyList()
                },
            )
        } else {
            null
        }

        val bundleFilter = if (includeBundles != null) {
            BundleFilter(
                includes = parseCommaSeparated(includeBundles).toSet(),
                excludes = emptySet(),
            )
        } else {
            null
        }

        val librarySpec = if (tagFilter != null || bundleFilter != null) {
            LibraryFilterSpec(
                byTags = tagFilter,
                byGroups = null,
                byAliases = null,
                byBundles = bundleFilter,
                byDeprecated = null,
                customFilters = emptyMap(),
            )
        } else {
            null
        }

        val newDistribution = Distribution(name = name, librarySpec = librarySpec, pluginSpec = null)
        metadata.copy(distributions = metadata.distributions + newDistribution) to "Added distribution '$name'"
    }
}
```

- [ ] **Step 3: Migrate RemoveVersionCommand**

Replace `RemoveVersionCommand` content with:

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.VersionReference

public class RemoveVersionCommand : CliktCommand(name = "version") {
    override fun help(context: Context): String = "Remove a version alias from metadata.json"

    private val opts by MetadataOptions()
    public val alias: String by argument(help = "Version alias to remove")
    public val force: Boolean by option("-f", "--force", help = "Skip reference checks").flag()

    override fun run() = MetadataRunner(this, opts).run { metadata ->
        if (metadata.versions.none { it.name == alias }) {
            throw CliException.AliasNotFound("Version", alias)
        }

        if (!force) {
            val referencingLibraries = metadata.libraries
                .filter { it.version is VersionReference.Reference && (it.version as VersionReference.Reference).name == alias }
                .map { it.alias }
            val referencingPlugins = metadata.plugins
                .filter { it.version is VersionReference.Reference && (it.version as VersionReference.Reference).name == alias }
                .map { it.alias }
            val refs = referencingLibraries + referencingPlugins
            if (refs.isNotEmpty()) {
                throw CliException.ReferenceConflict(alias, refs)
            }
        }

        metadata.copy(versions = metadata.versions.filter { it.name != alias }) to "Removed version '$alias'"
    }
}
```

- [ ] **Step 4: Migrate RemoveLibraryCommand, RemoveBundleCommand, RemoveDistributionCommand**

Apply same pattern: replace `val input`/`val output` with `private val opts by MetadataOptions()`, wrap `run()` body in `MetadataRunner(this, opts).run { metadata -> ... }`.

RemoveLibraryCommand:
```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner

public class RemoveLibraryCommand : CliktCommand(name = "library") {
    override fun help(context: Context): String = "Remove a library from metadata.json"

    private val opts by MetadataOptions()
    public val alias: String by argument(help = "Library alias to remove")
    public val force: Boolean by option("-f", "--force", help = "Skip dependency checks").flag()

    override fun run() = MetadataRunner(this, opts).run { metadata ->
        if (metadata.libraries.none { it.alias == alias }) {
            throw CliException.AliasNotFound("Library", alias)
        }

        if (!force) {
            val referencingBundles = metadata.bundles
                .filter { alias in it.libraries }
                .map { it.alias }
            if (referencingBundles.isNotEmpty()) {
                throw CliException.ReferenceConflict(alias, referencingBundles)
            }
        }

        metadata.copy(libraries = metadata.libraries.filter { it.alias != alias }) to "Removed library '$alias'"
    }
}
```

RemoveBundleCommand:
```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner

public class RemoveBundleCommand : CliktCommand(name = "bundle") {
    override fun help(context: Context): String = "Remove a bundle from metadata.json"

    private val opts by MetadataOptions()
    public val name: String by argument(help = "Bundle name to remove")
    public val force: Boolean by option("-f", "--force", help = "Skip dependency checks").flag()

    override fun run() = MetadataRunner(this, opts).run { metadata ->
        if (metadata.bundles.none { it.alias == name }) {
            throw CliException.AliasNotFound("Bundle", name)
        }

        if (!force) {
            val referencingBundles = metadata.bundles
                .filter { it.alias != name && name in it.extends }
                .map { it.alias }
            if (referencingBundles.isNotEmpty()) {
                throw CliException.ReferenceConflict(name, referencingBundles)
            }
        }

        metadata.copy(bundles = metadata.bundles.filter { it.alias != name }) to "Removed bundle '$name'"
    }
}
```

RemoveDistributionCommand:
```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner

public class RemoveDistributionCommand : CliktCommand(name = "distribution") {
    override fun help(context: Context): String = "Remove a distribution from metadata.json"

    private val opts by MetadataOptions()
    public val name: String by argument(help = "Distribution name to remove")
    public val force: Boolean by option("-f", "--force", help = "Skip dependency checks").flag()

    override fun run() = MetadataRunner(this, opts).run { metadata ->
        if (metadata.distributions.none { it.name == name }) {
            throw CliException.AliasNotFound("Distribution", name)
        }

        if (!force) {
            if (metadata.settings.defaultDistribution == name) {
                throw CliException.ReferenceConflict(name, listOf("settings.defaultDistribution"))
            }
        }

        metadata.copy(distributions = metadata.distributions.filter { it.name != name }) to "Removed distribution '$name'"
    }
}
```

- [ ] **Step 5: Migrate UpdateVersionCommand, UpdateLibraryCommand**

UpdateVersionCommand:
```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.VersionReference

public class UpdateVersionCommand : CliktCommand(name = "version") {
    override fun help(context: Context): String = "Update a version in metadata.json"

    private val opts by MetadataOptions()
    public val alias: String by argument(help = "Version or library alias")
    public val version: String by argument(help = "New version value")
    public val library: Boolean by option("-l", "--library", help = "Update library version").flag()

    override fun run() = MetadataRunner(this, opts).run { metadata ->
        val entity: String
        val updated = if (library) {
            entity = "library"
            if (metadata.libraries.none { it.alias == alias }) {
                throw CliException.AliasNotFound("Library", alias)
            }
            metadata.copy(
                libraries = metadata.libraries.map { lib ->
                    if (lib.alias == alias) lib.copy(version = VersionReference.Literal(version)) else lib
                }
            )
        } else {
            entity = "version"
            if (metadata.versions.none { it.name == alias }) {
                throw CliException.AliasNotFound("Version", alias)
            }
            metadata.copy(
                versions = metadata.versions.map { ver ->
                    if (ver.name == alias) ver.copy(value = version) else ver
                }
            )
        }

        updated to "Updated $entity '$alias' to '$version'"
    }
}
```

UpdateLibraryCommand:
```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import io.github.zenhelix.dependanger.core.model.JdkConstraints
import io.github.zenhelix.dependanger.core.model.Requirements

public class UpdateLibraryCommand : CliktCommand(name = "library") {
    override fun help(context: Context): String = "Update a library in metadata.json"

    private val opts by MetadataOptions()
    public val alias: String by argument(help = "Library alias to update")
    public val version: String? by option("-v", "--version", help = "New version or ref:alias")
    public val tags: String? by option("-t", "--tags", help = "New tags (comma-separated)")
    public val requiresJdk: String? by option("--requires-jdk", help = "Minimum JDK version")

    override fun run() = MetadataRunner(this, opts).run { metadata ->
        val existing = metadata.libraries.find { it.alias == alias }
            ?: throw CliException.AliasNotFound("Library", alias)

        val updatedLib = existing.copy(
            version = version?.let { parseVersionRef(it) } ?: existing.version,
            tags = tags?.let { parseCommaSeparated(it).toSet() } ?: existing.tags,
            requires = requiresJdk?.let {
                Requirements(
                    jdk = JdkConstraints(min = it.toIntOrNull(), max = null),
                    kotlin = null,
                )
            } ?: existing.requires,
        )

        metadata.copy(
            libraries = metadata.libraries.map { if (it.alias == alias) updatedLib else it }
        ) to "Updated library '$alias'"
    }
}
```

- [ ] **Step 6: Run all existing tests**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew :dependanger-cli:test --no-daemon`

Expected: ALL PASS

- [ ] **Step 7: Commit**

```bash
cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2
git add dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/
git commit -m "refactor(cli): migrate remaining mutation commands to MetadataRunner"
```

---

### Task 8: Migrate MigrateDeprecatedCommand

MigrateDeprecatedCommand is the most complex mutation command. It needs `MetadataRunner` for the outer read-write loop, but also has `--dry-run` and `--backup` paths that need the formatter directly. Use `MetadataRunner` but with a custom flow — the `run` lambda can use the runner's `formatter` for intermediate output.

**Files:**
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/MigrateDeprecatedCommand.kt`

- [ ] **Step 1: Refactor MigrateDeprecatedCommand**

The dry-run path doesn't write, so it needs to short-circuit. We'll use `MetadataRunner` but keep the `formatter` accessible for table rendering and info messages. The trick: throw `ProgramResult(0)` from the dry-run path to exit cleanly without writing.

Actually, since the dry-run path doesn't transform metadata, we need to handle it differently. We'll use MetadataRunner's `formatter` property but handle dry-run via an early return pattern. MetadataRunner.run's lambda must return `Pair<DependangerMetadata, String>`, but dry-run doesn't produce updated metadata.

Better approach: For commands that need early-return or non-standard flow, use `MetadataRunner`'s individual pieces but not the `run()` method. However, since `MetadataRunner` exposes `formatter`, we can add a `readAndHandle` method.

Simplest approach: Add a second method to `MetadataRunner` — `readAndHandle` that gives full control.

Add to `MetadataRunner.kt`:

```kotlin
public fun readAndHandle(block: MetadataCommandContext.() -> Unit) {
    withErrorHandling(formatter) {
        val metadata = metadataService.read(Path.of(opts.input))
        MetadataCommandContext(metadata, metadataService, formatter, opts).block()
    }
}

public class MetadataCommandContext(
    public val metadata: DependangerMetadata,
    private val metadataService: MetadataService,
    public val formatter: OutputFormatter,
    private val opts: MetadataOptions,
) {
    public fun write(updated: DependangerMetadata, path: Path? = null) {
        metadataService.write(updated, path ?: Path.of(opts.output ?: opts.input))
    }
}
```

Then MigrateDeprecatedCommand uses `readAndHandle`:

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import java.nio.file.Path

public class MigrateDeprecatedCommand : CliktCommand(name = "migrate-deprecated") {
    override fun help(context: Context): String = "Migrate deprecated libraries to their replacements"

    private val opts by MetadataOptions()
    public val dryRun: Boolean by option("--dry-run", help = "Show migration plan without executing").flag()
    public val replace: Boolean by option("--replace", help = "Replace deprecated with replacedBy in bundles").flag(default = true)
    public val remove: Boolean by option("--remove", help = "Remove deprecated libraries from metadata").flag()
    public val removeFromBundles: Boolean by option("--remove-from-bundles", help = "Remove deprecated from bundles instead of replacing").flag()
    public val backup: Boolean by option("--backup", help = "Create backup before modifying").flag()

    override fun run() {
        val runner = MetadataRunner(this, opts)
        runner.readAndHandle {
            val deprecated = metadata.libraries.filter { it.deprecation != null }

            if (deprecated.isEmpty()) {
                formatter.info("No deprecated libraries found")
                return@readAndHandle
            }

            val headers = listOf("Alias", "Replaced By", "Message")
            val rows = deprecated.map { lib ->
                listOf(
                    lib.alias,
                    lib.deprecation?.replacedBy ?: "-",
                    lib.deprecation?.message ?: "-",
                )
            }

            if (dryRun) {
                formatter.info("Migration plan (dry run):")
                formatter.renderTable(headers, rows)
                return@readAndHandle
            }

            formatter.renderTable(headers, rows)

            if (backup) {
                val backupPath = Path.of("${opts.input}.bak")
                write(metadata, backupPath)
                formatter.info("Backup saved to '$backupPath'")
            }

            val deprecatedAliases = deprecated.map { it.alias }.toSet()
            val replacementMap = deprecated
                .filter { it.deprecation?.replacedBy != null }
                .associate { it.alias to it.deprecation!!.replacedBy!! }

            val updatedBundles = metadata.bundles.map { bundle ->
                val updatedLibraries = bundle.libraries.flatMap { libAlias ->
                    when {
                        removeFromBundles && libAlias in deprecatedAliases -> emptyList()
                        replace && libAlias in replacementMap              -> listOf(replacementMap.getValue(libAlias))
                        else                                               -> listOf(libAlias)
                    }
                }
                bundle.copy(libraries = updatedLibraries)
            }

            val updatedLibraries = if (remove) {
                metadata.libraries.filter { it.deprecation == null }
            } else {
                metadata.libraries
            }

            val updated = metadata.copy(
                libraries = updatedLibraries,
                bundles = updatedBundles,
            )

            write(updated)

            val removedCount = if (remove) deprecated.size else 0
            val replacedCount = replacementMap.size
            formatter.success("Migration complete: $replacedCount replaced, $removedCount removed")
        }
    }
}
```

- [ ] **Step 2: Update MetadataRunner with readAndHandle method**

Update `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/runner/MetadataRunner.kt`:

```kotlin
package io.github.zenhelix.dependanger.cli.runner

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import io.github.zenhelix.dependanger.cli.MetadataService
import io.github.zenhelix.dependanger.cli.OutputFormatter
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.withErrorHandling
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import java.nio.file.Path

public class MetadataRunner(
    command: CliktCommand,
    private val opts: MetadataOptions,
) {
    public val formatter: OutputFormatter = OutputFormatter(terminal = command.terminal)
    private val metadataService: MetadataService = MetadataService()

    public fun run(transform: (DependangerMetadata) -> Pair<DependangerMetadata, String>) {
        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(opts.input))
            val (updated, message) = transform(metadata)
            metadataService.write(updated, Path.of(opts.output ?: opts.input))
            formatter.success(message)
        }
    }

    public fun readAndHandle(block: MetadataCommandContext.() -> Unit) {
        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(opts.input))
            MetadataCommandContext(metadata, metadataService, formatter, opts).block()
        }
    }
}

public class MetadataCommandContext(
    public val metadata: DependangerMetadata,
    private val metadataService: MetadataService,
    public val formatter: OutputFormatter,
    private val opts: MetadataOptions,
) {
    public fun write(updated: DependangerMetadata, path: Path? = null) {
        metadataService.write(updated, path ?: Path.of(opts.output ?: opts.input))
    }
}
```

- [ ] **Step 3: Run all tests**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew :dependanger-cli:test --no-daemon`

Expected: ALL PASS

- [ ] **Step 4: Commit**

```bash
cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2
git add dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/runner/MetadataRunner.kt dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/MigrateDeprecatedCommand.kt
git commit -m "refactor(cli): migrate MigrateDeprecatedCommand to MetadataRunner.readAndHandle"
```

---

### Task 9: Migrate pipeline commands to PipelineRunner

Migrate: `ValidateCommand`, `CheckUpdatesCommand`, `AnalyzeCommand`, `LicenseCheckCommand`, `ResolveTransitivesCommand`. These all follow the pattern: read metadata → configure Dependanger → process → render results.

Also apply `parseEnum<E>()` where enum parsing is duplicated.

**Files:**
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/ValidateCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/CheckUpdatesCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/AnalyzeCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/LicenseCheckCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/ResolveTransitivesCommand.kt`

Each command should:
1. Replace `val input`, `val format`, `val distribution` options with `private val opts by PipelineOptions()`
2. Replace `val formatter = OutputFormatter(...)` / `val metadataService = MetadataService()` / `withErrorHandling` with `PipelineRunner(this, opts).run(configure = { ... }, handle = { ... })`
3. Replace manual `try { Enum.valueOf(...) }` with `parseEnum<E>()`

- [ ] **Step 1: Migrate ValidateCommand**

Note: ValidateCommand calls `dependanger.validate()` not `dependanger.process()`. It needs a slightly different flow. It also uses `--strict` which isn't in PipelineOptions. We'll use PipelineRunner's formatter but handle the validate-specific flow manually.

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.runner.PipelineRunner
import io.github.zenhelix.dependanger.core.model.Diagnostics

public class ValidateCommand : CliktCommand(name = "validate") {
    override fun help(context: Context): String = "Validate metadata.json or DSL configuration"

    private val opts by PipelineOptions()
    public val strict: Boolean by option("--strict", help = "Fail on warnings").flag()

    override fun run() {
        val runner = PipelineRunner(this, opts)
        val jsonMode = opts.format == CliDefaults.OUTPUT_FORMAT_JSON

        withErrorHandling(runner.formatter) {
            val metadata = MetadataService().read(java.nio.file.Path.of(opts.input))

            val result = CoroutineRunner.run {
                Dependanger.fromMetadata(metadata).build().validate()
            }

            val diagnostics = result.diagnostics

            if (jsonMode) {
                runner.formatter.renderJson(diagnostics, Diagnostics.serializer())
            } else {
                runner.formatter.renderDiagnostics(diagnostics)
            }

            if (diagnostics.hasErrors || (strict && diagnostics.warnings.isNotEmpty())) {
                throw CliException.ValidationFailed(diagnostics)
            }

            runner.formatter.success("Validation passed")
        }
    }
}
```

- [ ] **Step 2: Migrate CheckUpdatesCommand**

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.updates
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.runner.PipelineRunner
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.feature.model.settings.updates.UpdateCheckSettings
import io.github.zenhelix.dependanger.feature.model.settings.updates.UpdateCheckSettingsKey
import io.github.zenhelix.dependanger.feature.model.updates.UpdateAvailableInfo
import kotlinx.serialization.builtins.ListSerializer
import java.nio.file.Path
import kotlin.io.path.writeText

public class CheckUpdatesCommand : CliktCommand(name = "updates") {
    override fun help(context: Context): String = "Check for available library updates"

    private val opts by PipelineOptions()
    public val output: String? by option("-o", "--output", help = "Output report file")
    public val includePrerelease: Boolean by option("--include-prerelease", help = "Include prerelease").flag()
    public val exclude: List<String> by option("--exclude", help = "Exclude patterns").multiple()
    public val type: String? by option("--type", help = "Update types: PATCH,MINOR,MAJOR")
    public val repositories: String? by option("--repositories", help = "Maven repos (comma-separated)")
    public val offline: Boolean by option("--offline", help = "Use cache only").flag()
    public val failOnUpdates: Boolean by option("--fail-on-updates", help = "Fail if updates found").flag()

    override fun run() = PipelineRunner(this, opts).run(
        configure = {
            preset(ProcessingPreset.STRICT)
            withContextProperty(UpdateCheckSettingsKey, UpdateCheckSettings(
                enabled = true,
                includePrerelease = includePrerelease,
                excludePatterns = exclude,
                repositories = parseMavenRepositories(repositories) ?: emptyList(),
                cacheTtlHours = if (offline) Long.MAX_VALUE else UpdateCheckSettings.DEFAULT_CACHE_TTL_HOURS,
                timeout = UpdateCheckSettings.DEFAULT_TIMEOUT_MS,
                parallelism = UpdateCheckSettings.DEFAULT_PARALLELISM,
                cacheDirectory = null,
            ))
        },
        handle = { result ->
            val updates = result.updates

            val filteredUpdates = type?.let { typeFilter ->
                val allowedTypes = parseCommaSeparated(typeFilter).map { it.uppercase() }.toSet()
                updates.filter { it.updateType.name in allowedTypes }
            } ?: updates

            val jsonMode = opts.format == CliDefaults.OUTPUT_FORMAT_JSON
            if (jsonMode) {
                formatter.renderJson(filteredUpdates, ListSerializer(UpdateAvailableInfo.serializer()))
            } else {
                if (filteredUpdates.isEmpty()) {
                    formatter.success("All libraries are up to date")
                } else {
                    formatter.renderTable(
                        headers = listOf("Library", "Current", "Available", "Type"),
                        rows = filteredUpdates.map { update ->
                            listOf(
                                "${update.group}:${update.artifact}",
                                update.currentVersion,
                                update.latestVersion,
                                update.updateType.name,
                            )
                        }
                    )
                    formatter.info("${filteredUpdates.size} update(s) available")
                }
            }

            output?.let { outputFile ->
                val outputPath = Path.of(outputFile)
                val jsonString = CliDefaults.CLI_JSON.encodeToString(ListSerializer(UpdateAvailableInfo.serializer()), filteredUpdates)
                outputPath.writeText(jsonString)
                formatter.success("Report written to $outputPath")
            }

            if (failOnUpdates && filteredUpdates.isNotEmpty()) {
                throw ProgramResult(1)
            }
        }
    )
}
```

- [ ] **Step 3: Migrate AnalyzeCommand, LicenseCheckCommand, ResolveTransitivesCommand**

Apply same pattern as CheckUpdatesCommand. Replace manual enum parsing with `parseEnum<E>()`.

For ResolveTransitivesCommand, replace:
```kotlin
val strategy = try {
    ConflictResolutionStrategy.valueOf(conflictResolution.uppercase())
} catch (_: IllegalArgumentException) {
    throw CliException.InvalidArgument(...)
}
```
with:
```kotlin
val strategy = parseEnum<ConflictResolutionStrategy>(conflictResolution, "conflict resolution strategy")
```

- [ ] **Step 4: Run all tests**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew :dependanger-cli:test --no-daemon`

Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2
git add dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/
git commit -m "refactor(cli): migrate pipeline commands to PipelineRunner"
```

---

### Task 10: Migrate special pipeline commands

Migrate: `ProcessCommand`, `GenerateCommand`, `SecurityCheckCommand`, `ReportCommand`. These have non-standard flows.

**ProcessCommand** — uses `--preset`, `--disable-processor`, writes effective JSON. It needs custom configure and custom output. PipelineRunner works here with `configure` and `handle`.

**GenerateCommand** — no `--format`, no `--distribution` inherited from PipelineOptions (actually it has `-d`). It uses `--output-dir` not `-o`. It doesn't need PipelineRunner's `formatter` json mode. Use PipelineOptions for `-i`/`-d` but override `--format` default or don't use it.

**SecurityCheckCommand** — has SARIF mode, uses `echo()` for SARIF output. Unique enough that PipelineRunner is still useful for the core flow.

**ReportCommand** — calls `writeReportTo()`, doesn't use `--format` from PipelineOptions (uses its own for report format).

For commands that don't perfectly fit `PipelineOptions`, the command can still declare additional options alongside the option group. The `--format` default can be overridden per-command by declaring it directly and not using the one from PipelineOptions.

Alternative: Since ProcessCommand, GenerateCommand, and ReportCommand each have unique option needs, they can use PipelineRunner directly but with `PipelineOptions` only for the common `-i`/`-d` subset. The `--format` in PipelineOptions won't conflict — commands that need a different default just ignore it.

For these complex commands, use PipelineRunner's `run(configure, handle)` but keep their own additional options. Apply `parseEnum<E>()` where relevant.

**Files:**
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/ProcessCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/GenerateCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/SecurityCheckCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/ReportCommand.kt`

- [ ] **Step 1: Migrate ProcessCommand**

ProcessCommand uses `--preset` and `--disable-processor` which are unique. It writes effective JSON to `--output` (different semantics from MetadataOptions). Use PipelineOptions for `-i`/`-d`. Add `--output`, `--preset`, `--disable-processor` as local options.

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.runner.PipelineRunner
import io.github.zenhelix.dependanger.core.model.ProcessingPreset
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import java.nio.file.Path
import kotlin.io.path.writeText

public class ProcessCommand : CliktCommand(name = "process") {
    override fun help(context: Context): String = "Run processing pipeline"

    private val opts by PipelineOptions()
    public val output: String by option("-o", "--output", help = "Output effective file").default(CliDefaults.EFFECTIVE_OUTPUT_FILE)
    public val preset: String by option("--preset", help = "Processing preset").default(CliDefaults.PROCESSING_PRESET_DEFAULT)
    public val disableProcessor: List<String> by option("--disable-processor", help = "Disable a processor by ID").multiple()

    override fun run() = PipelineRunner(this, opts).run(
        configure = {
            val resolvedPreset = parseEnum<ProcessingPreset>(preset, "preset")
            preset(resolvedPreset)
            disableProcessor.forEach { id -> disableProcessor(id) }
        },
        handle = { result ->
            val effective = result.effectiveOrNull()
            if (effective == null) {
                formatter.renderDiagnostics(result.diagnostics)
                throw CliException.ValidationFailed(result.diagnostics)
            }

            val outputPath = Path.of(output)
            val jsonString = CliDefaults.CLI_JSON.encodeToString(EffectiveMetadata.serializer(), effective)
            outputPath.writeText(jsonString)

            formatter.renderDiagnostics(result.diagnostics)
            if (result.isSuccess) {
                formatter.success("Processed metadata written to $outputPath")
            } else {
                formatter.warning("Processed metadata written to $outputPath (with errors)")
            }
        }
    )
}
```

- [ ] **Step 2: Migrate GenerateCommand**

GenerateCommand doesn't use `--format`. Use PipelineOptions for `-i`/`-d` only. The `--format` from PipelineOptions defaults to "text" and isn't used — harmless.

```kotlin
package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.api.writeBomTo
import io.github.zenhelix.dependanger.api.writeTomlTo
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.runner.PipelineRunner
import io.github.zenhelix.dependanger.generators.bom.BomConfig
import io.github.zenhelix.dependanger.generators.toml.TomlConfig
import java.nio.file.Path

public class GenerateCommand : CliktCommand(name = "generate") {
    override fun help(context: Context): String = "Generate artifacts from metadata"

    private val opts by PipelineOptions()
    public val outputDir: String by option("--output-dir", help = "Output directory").default(".")
    public val toml: Boolean by option("--toml", help = "Generate TOML version catalog").flag()
    public val bom: Boolean by option("--bom", help = "Generate Maven BOM").flag()
    public val tomlFilename: String by option("--toml-filename", help = "TOML filename").default(CliDefaults.TOML_FILENAME)
    public val tomlComments: Boolean by option("--toml-comments", help = "Include comments").flag()
    public val tomlSort: Boolean by option("--toml-sort", help = "Sort sections").flag(default = true)
    public val tomlInlineVersions: Boolean by option("--toml-inline-versions", help = "Inline versions").flag()
    public val bomGroup: String? by option("--bom-group", help = "BOM groupId")
    public val bomArtifact: String? by option("--bom-artifact", help = "BOM artifactId")
    public val bomVersion: String? by option("--bom-version", help = "BOM version")
    public val bomIncludeOptional: Boolean by option("--bom-include-optional", help = "Include optional").flag()

    override fun run() = PipelineRunner(this, opts).run(
        handle = { result ->
            if (!result.isSuccess) {
                formatter.renderDiagnostics(result.diagnostics)
                throw CliException.ValidationFailed(result.diagnostics)
            }

            val generateToml = toml || !bom
            val generateBom = bom
            val outDir = Path.of(outputDir)

            if (generateToml) {
                val tomlConfig = TomlConfig(
                    filename = tomlFilename,
                    includeComments = tomlComments,
                    sortSections = tomlSort,
                    useInlineVersions = tomlInlineVersions,
                    includeDeprecationComments = true,
                )
                result.writeTomlTo(outDir, tomlConfig)
                formatter.success("Generated TOML version catalog: ${outDir.resolve(tomlConfig.filename)}")
            }

            if (generateBom) {
                val resolvedGroup = bomGroup
                    ?: throw CliException.InvalidArgument("--bom-group is required for BOM generation")
                val resolvedArtifact = bomArtifact
                    ?: throw CliException.InvalidArgument("--bom-artifact is required for BOM generation")
                val resolvedVersion = bomVersion
                    ?: throw CliException.InvalidArgument("--bom-version is required for BOM generation")

                val bomConfig = BomConfig(
                    groupId = resolvedGroup,
                    artifactId = resolvedArtifact,
                    version = resolvedVersion,
                    name = null,
                    description = null,
                    filename = BomConfig.DEFAULT_FILENAME,
                    includeOptionalDependencies = bomIncludeOptional,
                    prettyPrint = true,
                    includeDeprecationComments = true,
                )
                result.writeBomTo(outDir, bomConfig)
                formatter.success("Generated Maven BOM: ${outDir.resolve(bomConfig.filename)}")
            }
        }
    )
}
```

- [ ] **Step 3: Migrate SecurityCheckCommand and ReportCommand**

SecurityCheckCommand has SARIF and uses `echo()`. Keep the SARIF-specific path via Clikt's `echo()` — the PipelineRunner handles the common flow.

ReportCommand has its own format enum (ReportFormat) and doesn't use PipelineOptions' `--format`. Use `parseEnum<ReportFormat>()`.

- [ ] **Step 4: Run all tests**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew :dependanger-cli:test --no-daemon`

Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2
git add dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/
git commit -m "refactor(cli): migrate special pipeline commands to PipelineRunner"
```

---

### Task 11: Migrate InitCommand

InitCommand is unique — it doesn't read metadata, it creates it. It can't use MetadataRunner. Keep it as-is but use the formatter pattern consistently.

**Files:**
- Modify: `dependanger-cli/src/main/kotlin/io/github/zenhelix/dependanger/cli/commands/InitCommand.kt`

- [ ] **Step 1: Minimal cleanup only**

InitCommand doesn't fit MetadataRunner or PipelineRunner — it's a creation command. Leave it as-is. No changes needed.

- [ ] **Step 2: Verify all tests pass**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew :dependanger-cli:test --no-daemon`

Expected: ALL PASS

---

### Task 12: Final cleanup and full test run

- [ ] **Step 1: Run full project test suite**

Run: `cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2 && ./gradlew test --no-daemon`

Expected: ALL PASS

- [ ] **Step 2: Verify no unused imports or dead code**

Check that old import patterns (`import com.github.ajalt.clikt.core.terminal` used directly, `import java.nio.file.Path` for input path) are cleaned up in migrated commands.

- [ ] **Step 3: Final commit if any cleanup needed**

```bash
cd /Users/dmitriimedakin/IdeaProjects/zenhelix/dependager2
git add -A
git commit -m "refactor(cli): cleanup unused imports after command migration"
```
