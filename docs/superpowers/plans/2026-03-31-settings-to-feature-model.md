# Extract Feature Settings to feature-model Module

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to
> implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move all `*Settings`, `*SettingsKey`, `*SettingsDsl`, `*SettingsProvider` classes from feature implementation modules to
`dependanger-feature-model`, so CLI and other consumers can depend on settings contracts without pulling in feature implementations.

**Architecture:** Each feature module currently bundles settings (data contracts) together with processor implementation. We extract
settings into `feature-model` (the existing shared contracts module), keeping the same package structure under
`io.github.zenhelix.dependanger.feature.model.settings.*`. Feature modules and CLI both depend on `feature-model` for settings. Feature
modules keep only processors and SPI runtime. ServiceLoader registrations for `FeatureSettingsProvider` move to `feature-model` since
providers are pure deserializers with no implementation dependencies.

**Tech Stack:** Kotlin, kotlinx-serialization, Gradle (TYPESAFE_PROJECT_ACCESSORS)

---

## File Structure

### New files in `dependanger-feature-model`

```
components/shared/dependanger-feature-model/src/main/kotlin/io/github/zenhelix/dependanger/feature/model/settings/
├── updates/
│   └── UpdateCheckSettings.kt          (Settings + Key + Dsl + Provider)
├── security/
│   └── SecurityCheckSettings.kt        (Settings + Key + Dsl + Provider)
├── license/
│   └── LicenseCheckSettings.kt         (Settings + Key + Dsl + DualLicensePolicy + Provider)
├── transitive/
│   └── TransitiveResolutionSettings.kt (Settings + Key + Dsl + Provider)
└── analysis/
    └── CompatibilityAnalysisSettings.kt(Settings + Key + Dsl + Provider)

components/shared/dependanger-feature-model/src/main/resources/META-INF/services/
└── io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider  (all 5 providers)
```

### Deleted files from feature modules

```
components/features/dependanger-updates/src/main/kotlin/.../updates/UpdateCheckSettings.kt
components/features/dependanger-updates/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider

components/features/dependanger-security/src/main/kotlin/.../security/SecurityCheckSettings.kt
components/features/dependanger-security/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider

components/features/dependanger-license/src/main/kotlin/.../license/LicenseCheckSettings.kt
components/features/dependanger-license/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider

components/features/dependanger-transitive/src/main/kotlin/.../transitive/TransitiveResolutionSettings.kt
components/features/dependanger-transitive/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider

components/features/dependanger-analysis/src/main/kotlin/.../analysis/CompatibilityAnalysisSettings.kt
components/features/dependanger-analysis/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider
```

### Modified files

```
# Build files
components/shared/dependanger-feature-model/build.gradle.kts          (add kotlinx-serialization-json)
dependanger-cli/build.gradle.kts                                       (remove 5 feature implementation deps, keep runtimeOnly)

# Feature modules — update imports in processors
components/features/dependanger-updates/src/main/kotlin/.../updates/UpdateCheckProcessor.kt
components/features/dependanger-security/src/main/kotlin/.../security/SecurityCheckProcessor.kt
components/features/dependanger-license/src/main/kotlin/.../license/LicenseCheckProcessor.kt
components/features/dependanger-license/src/main/kotlin/.../license/LicensePolicy.kt
components/features/dependanger-license/src/main/kotlin/.../license/LicenseCheckContext.kt
components/features/dependanger-transitive/src/main/kotlin/.../transitive/TransitiveResolverProcessor.kt
components/features/dependanger-analysis/src/main/kotlin/.../analysis/CompatibilityAnalysisProcessor.kt

# CLI — update imports in commands
dependanger-cli/src/main/kotlin/.../cli/commands/CheckUpdatesCommand.kt
dependanger-cli/src/main/kotlin/.../cli/commands/SecurityCheckCommand.kt
dependanger-cli/src/main/kotlin/.../cli/commands/LicenseCheckCommand.kt
dependanger-cli/src/main/kotlin/.../cli/commands/ResolveTransitivesCommand.kt
dependanger-cli/src/main/kotlin/.../cli/commands/ReportCommand.kt
dependanger-cli/src/main/kotlin/.../cli/commands/AnalyzeCommand.kt
```

---

### Task 1: Create settings files in feature-model (updates, security, analysis)

**Files:**

- Create:
  `components/shared/dependanger-feature-model/src/main/kotlin/io/github/zenhelix/dependanger/feature/model/settings/updates/UpdateCheckSettings.kt`
- Create:
  `components/shared/dependanger-feature-model/src/main/kotlin/io/github/zenhelix/dependanger/feature/model/settings/security/SecurityCheckSettings.kt`
- Create:
  `components/shared/dependanger-feature-model/src/main/kotlin/io/github/zenhelix/dependanger/feature/model/settings/analysis/CompatibilityAnalysisSettings.kt`
- Modify: `components/shared/dependanger-feature-model/build.gradle.kts`

- [ ] **Step 1: Add kotlinx-serialization-json dependency to feature-model**

In `components/shared/dependanger-feature-model/build.gradle.kts`, add `implementation(libs.kotlinx.serialization.json)` to dependencies.
The `SettingsDsl` extension functions use `Json.encodeToJsonElement(...)` which requires the json module (not just core).

```kotlin
dependencies {
    api(projects.components.core.dependangerCore)
    api(projects.components.core.dependangerEffective)
    api(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
}
```

- [ ] **Step 2: Create UpdateCheckSettings.kt in feature-model**

Create file
`components/shared/dependanger-feature-model/src/main/kotlin/io/github/zenhelix/dependanger/feature/model/settings/updates/UpdateCheckSettings.kt`.

Copy the full content from
`components/features/dependanger-updates/src/main/kotlin/io/github/zenhelix/dependanger/features/updates/UpdateCheckSettings.kt` but change
the package to `io.github.zenhelix.dependanger.feature.model.settings.updates`.

The file contains: `UpdateCheckSettingsKey`, `UpdateCheckSettings` data class, `UpdateCheckSettingsProvider`, `UpdateCheckSettingsDsl`, and
`SettingsDsl.updateCheck()` extension.

- [ ] **Step 3: Create SecurityCheckSettings.kt in feature-model**

Create file
`components/shared/dependanger-feature-model/src/main/kotlin/io/github/zenhelix/dependanger/feature/model/settings/security/SecurityCheckSettings.kt`.

Copy from
`components/features/dependanger-security/src/main/kotlin/io/github/zenhelix/dependanger/features/security/SecurityCheckSettings.kt`, change
package to `io.github.zenhelix.dependanger.feature.model.settings.security`.

Contains: `SecurityCheckSettingsKey`, `SecurityCheckSettings`, `SecurityCheckSettingsProvider`, `SecurityCheckSettingsDsl`,
`SettingsDsl.securityCheck()`.

- [ ] **Step 4: Create CompatibilityAnalysisSettings.kt in feature-model**

Create file
`components/shared/dependanger-feature-model/src/main/kotlin/io/github/zenhelix/dependanger/feature/model/settings/analysis/CompatibilityAnalysisSettings.kt`.

Copy from
`components/features/dependanger-analysis/src/main/kotlin/io/github/zenhelix/dependanger/features/analysis/CompatibilityAnalysisSettings.kt`,
change package to `io.github.zenhelix.dependanger.feature.model.settings.analysis`.

Contains: `CompatibilityAnalysisSettingsKey`, `CompatibilityAnalysisSettings`, `CompatibilityAnalysisSettingsProvider`,
`CompatibilityAnalysisSettingsDsl`, `SettingsDsl.compatibilityAnalysis()`.

- [ ] **Step 5: Verify feature-model compiles**

Run: `./gradlew :components:shared:dependanger-feature-model:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add components/shared/dependanger-feature-model/
git commit -m "refactor: add settings contracts for updates, security, analysis to feature-model"
```

---

### Task 2: Create settings files in feature-model (license, transitive)

**Files:**

- Create:
  `components/shared/dependanger-feature-model/src/main/kotlin/io/github/zenhelix/dependanger/feature/model/settings/license/LicenseCheckSettings.kt`
- Create:
  `components/shared/dependanger-feature-model/src/main/kotlin/io/github/zenhelix/dependanger/feature/model/settings/transitive/TransitiveResolutionSettings.kt`

- [ ] **Step 1: Create LicenseCheckSettings.kt in feature-model**

Copy from `components/features/dependanger-license/src/main/kotlin/io/github/zenhelix/dependanger/features/license/LicenseCheckSettings.kt`,
change package to `io.github.zenhelix.dependanger.feature.model.settings.license`.

**Important:** This file also defines `DualLicensePolicy` enum. Include it in the new file. Both `LicenseCheckSettings` and the feature
module's `LicensePolicy.kt` use this enum — after migration, both will import from feature-model.

Contains: `LicenseCheckSettingsKey`, `DualLicensePolicy`, `LicenseCheckSettings`, `LicenseCheckSettingsProvider`, `LicenseCheckSettingsDsl`,
`SettingsDsl.licenseCheck()`.

- [ ] **Step 2: Create TransitiveResolutionSettings.kt in feature-model**

Copy from
`components/features/dependanger-transitive/src/main/kotlin/io/github/zenhelix/dependanger/features/transitive/TransitiveResolutionSettings.kt`,
change package to `io.github.zenhelix.dependanger.feature.model.settings.transitive`.

**Note:** This settings class imports `ConflictResolutionStrategy` which is already in `feature-model` at
`io.github.zenhelix.dependanger.feature.model.transitive.ConflictResolutionStrategy`. The import stays the same.

Contains: `TransitiveResolutionSettingsKey`, `TransitiveResolutionSettings`, `TransitiveResolutionSettingsProvider`,
`TransitiveResolutionSettingsDsl`, `SettingsDsl.transitiveResolution()`.

- [ ] **Step 3: Verify feature-model compiles**

Run: `./gradlew :components:shared:dependanger-feature-model:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add components/shared/dependanger-feature-model/
git commit -m "refactor: add settings contracts for license, transitive to feature-model"
```

---

### Task 3: Create unified ServiceLoader registration in feature-model

**Files:**

- Create:
  `components/shared/dependanger-feature-model/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider`

- [ ] **Step 1: Create META-INF/services file**

Create file
`components/shared/dependanger-feature-model/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider`
with content:

```
io.github.zenhelix.dependanger.feature.model.settings.updates.UpdateCheckSettingsProvider
io.github.zenhelix.dependanger.feature.model.settings.security.SecurityCheckSettingsProvider
io.github.zenhelix.dependanger.feature.model.settings.license.LicenseCheckSettingsProvider
io.github.zenhelix.dependanger.feature.model.settings.transitive.TransitiveResolutionSettingsProvider
io.github.zenhelix.dependanger.feature.model.settings.analysis.CompatibilityAnalysisSettingsProvider
```

- [ ] **Step 2: Verify feature-model builds cleanly**

Run: `./gradlew :components:shared:dependanger-feature-model:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add components/shared/dependanger-feature-model/
git commit -m "refactor: add unified ServiceLoader registration for feature settings providers"
```

---

### Task 4: Migrate feature modules to use settings from feature-model

**Files:**

- Delete: `components/features/dependanger-updates/src/main/kotlin/.../updates/UpdateCheckSettings.kt`
- Delete:
  `components/features/dependanger-updates/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider`
- Delete: `components/features/dependanger-security/src/main/kotlin/.../security/SecurityCheckSettings.kt`
- Delete:
  `components/features/dependanger-security/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider`
- Delete: `components/features/dependanger-analysis/src/main/kotlin/.../analysis/CompatibilityAnalysisSettings.kt`
- Delete:
  `components/features/dependanger-analysis/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider`
- Delete: `components/features/dependanger-license/src/main/kotlin/.../license/LicenseCheckSettings.kt`
- Delete:
  `components/features/dependanger-license/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider`
- Delete: `components/features/dependanger-transitive/src/main/kotlin/.../transitive/TransitiveResolutionSettings.kt`
- Delete:
  `components/features/dependanger-transitive/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider`
- Modify: All processor .kt files that import Settings/SettingsKey

- [ ] **Step 1: Delete old settings files and SPI registrations from all 5 feature modules**

Delete the files listed above. Each feature module already depends on `dependanger-feature-model` via
`api(projects.components.shared.dependangerFeatureModel)`.

- [ ] **Step 2: Update imports in dependanger-updates processor**

In `components/features/dependanger-updates/src/main/kotlin/.../updates/UpdateCheckProcessor.kt`, replace:

```
import io.github.zenhelix.dependanger.features.updates.UpdateCheckSettings
import io.github.zenhelix.dependanger.features.updates.UpdateCheckSettingsKey
```

with:

```
import io.github.zenhelix.dependanger.feature.model.settings.updates.UpdateCheckSettings
import io.github.zenhelix.dependanger.feature.model.settings.updates.UpdateCheckSettingsKey
```

Search all .kt files in `dependanger-updates/src/main/` for any other imports of `UpdateCheckSettings` or `UpdateCheckSettingsKey` and
update them.

- [ ] **Step 3: Update imports in dependanger-security processor**

Same pattern in `SecurityCheckProcessor.kt` and any other files in `dependanger-security/src/main/`:

```
import io.github.zenhelix.dependanger.features.security.SecurityCheckSettings -> import io.github.zenhelix.dependanger.feature.model.settings.security.SecurityCheckSettings
import io.github.zenhelix.dependanger.features.security.SecurityCheckSettingsKey -> import io.github.zenhelix.dependanger.feature.model.settings.security.SecurityCheckSettingsKey
```

- [ ] **Step 4: Update imports in dependanger-analysis processor**

Same pattern in `CompatibilityAnalysisProcessor.kt`:

```
import io.github.zenhelix.dependanger.features.analysis.CompatibilityAnalysisSettings -> import io.github.zenhelix.dependanger.feature.model.settings.analysis.CompatibilityAnalysisSettings
import io.github.zenhelix.dependanger.features.analysis.CompatibilityAnalysisSettingsKey -> import io.github.zenhelix.dependanger.feature.model.settings.analysis.CompatibilityAnalysisSettingsKey
```

- [ ] **Step 5: Update imports in dependanger-license**

Files to update: `LicenseCheckProcessor.kt`, `LicensePolicy.kt`, `LicenseCheckContext.kt`, and any test files.

Replace all of:

```
import io.github.zenhelix.dependanger.features.license.LicenseCheckSettings
import io.github.zenhelix.dependanger.features.license.LicenseCheckSettingsKey
import io.github.zenhelix.dependanger.features.license.DualLicensePolicy
```

with:

```
import io.github.zenhelix.dependanger.feature.model.settings.license.LicenseCheckSettings
import io.github.zenhelix.dependanger.feature.model.settings.license.LicenseCheckSettingsKey
import io.github.zenhelix.dependanger.feature.model.settings.license.DualLicensePolicy
```

- [ ] **Step 6: Update imports in dependanger-transitive**

Files to update: `TransitiveResolverProcessor.kt`, `TransitiveResolverContext.kt`, and any test files.

Replace:

```
import io.github.zenhelix.dependanger.features.transitive.TransitiveResolutionSettings
import io.github.zenhelix.dependanger.features.transitive.TransitiveResolutionSettingsKey
```

with:

```
import io.github.zenhelix.dependanger.feature.model.settings.transitive.TransitiveResolutionSettings
import io.github.zenhelix.dependanger.feature.model.settings.transitive.TransitiveResolutionSettingsKey
```

- [ ] **Step 7: Compile all feature modules**

Run:
`./gradlew :components:features:dependanger-updates:compileKotlin :components:features:dependanger-security:compileKotlin :components:features:dependanger-license:compileKotlin :components:features:dependanger-transitive:compileKotlin :components:features:dependanger-analysis:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor: migrate feature modules to use settings from feature-model"
```

---

### Task 5: Update CLI to use settings from feature-model and slim dependencies

**Files:**

- Modify: `dependanger-cli/build.gradle.kts`
- Modify: `dependanger-cli/src/main/kotlin/.../cli/commands/CheckUpdatesCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/.../cli/commands/SecurityCheckCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/.../cli/commands/LicenseCheckCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/.../cli/commands/ResolveTransitivesCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/.../cli/commands/ReportCommand.kt`
- Modify: `dependanger-cli/src/main/kotlin/.../cli/commands/AnalyzeCommand.kt`

- [ ] **Step 1: Update imports in all CLI command files**

In `CheckUpdatesCommand.kt`:

```
import io.github.zenhelix.dependanger.features.updates.UpdateCheckSettings -> import io.github.zenhelix.dependanger.feature.model.settings.updates.UpdateCheckSettings
import io.github.zenhelix.dependanger.features.updates.UpdateCheckSettingsKey -> import io.github.zenhelix.dependanger.feature.model.settings.updates.UpdateCheckSettingsKey
```

In `SecurityCheckCommand.kt`:

```
import io.github.zenhelix.dependanger.features.security.SecurityCheckSettings -> import io.github.zenhelix.dependanger.feature.model.settings.security.SecurityCheckSettings
import io.github.zenhelix.dependanger.features.security.SecurityCheckSettingsKey -> import io.github.zenhelix.dependanger.feature.model.settings.security.SecurityCheckSettingsKey
```

In `LicenseCheckCommand.kt`:

```
import io.github.zenhelix.dependanger.features.license.LicenseCheckSettings -> import io.github.zenhelix.dependanger.feature.model.settings.license.LicenseCheckSettings
import io.github.zenhelix.dependanger.features.license.LicenseCheckSettingsKey -> import io.github.zenhelix.dependanger.feature.model.settings.license.LicenseCheckSettingsKey
```

In `ResolveTransitivesCommand.kt` and `ReportCommand.kt`:

```
import io.github.zenhelix.dependanger.features.transitive.TransitiveResolutionSettings -> import io.github.zenhelix.dependanger.feature.model.settings.transitive.TransitiveResolutionSettings
import io.github.zenhelix.dependanger.features.transitive.TransitiveResolutionSettingsKey -> import io.github.zenhelix.dependanger.feature.model.settings.transitive.TransitiveResolutionSettingsKey
```

In `AnalyzeCommand.kt`:

```
import io.github.zenhelix.dependanger.features.analysis.CompatibilityAnalysisSettings -> import io.github.zenhelix.dependanger.feature.model.settings.analysis.CompatibilityAnalysisSettings
import io.github.zenhelix.dependanger.features.analysis.CompatibilityAnalysisSettingsKey -> import io.github.zenhelix.dependanger.feature.model.settings.analysis.CompatibilityAnalysisSettingsKey
```

- [ ] **Step 2: Update CLI build.gradle.kts**

Change feature `implementation` dependencies to `runtimeOnly` (needed only for ServiceLoader processor discovery):

```kotlin
dependencies {
    implementation(projects.components.shared.dependangerFeatureModel)  // already there — provides settings
    implementation(projects.components.api.dependangerApi)

    runtimeOnly(projects.components.features.dependangerUpdates)
    runtimeOnly(projects.components.features.dependangerSecurity)
    runtimeOnly(projects.components.features.dependangerLicense)
    runtimeOnly(projects.components.features.dependangerTransitive)
    runtimeOnly(projects.components.features.dependangerAnalysis)
    runtimeOnly(projects.components.features.dependangerReport)

    // ... rest unchanged
}
```

- [ ] **Step 3: Compile CLI**

Run: `./gradlew :dependanger-cli:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add dependanger-cli/
git commit -m "refactor: CLI uses settings from feature-model, features now runtimeOnly"
```

---

### Task 6: Update tests and verify full build

**Files:**

- Modify: Any test files importing old settings packages
- Modify: `dependanger-cli/src/testFixtures/kotlin/.../cli/CliTestSupport.kt` (if it imports settings)
- Modify: `integration-tests/` test files (if they import settings)

- [ ] **Step 1: Search and fix all remaining old imports in test files**

Search all `.kt` files in the entire project for:

```
import io.github.zenhelix.dependanger.features.updates.UpdateCheckSettings
import io.github.zenhelix.dependanger.features.security.SecurityCheckSettings
import io.github.zenhelix.dependanger.features.license.LicenseCheckSettings
import io.github.zenhelix.dependanger.features.license.DualLicensePolicy
import io.github.zenhelix.dependanger.features.transitive.TransitiveResolutionSettings
import io.github.zenhelix.dependanger.features.analysis.CompatibilityAnalysisSettings
```

Replace each with the new `feature.model.settings.*` package. Pay special attention to:

- Feature module test files (e.g., `LicensePolicyComplianceTest.kt` imports `DualLicensePolicy` and `LicenseCheckSettings`)
- CLI test fixtures
- Integration tests

- [ ] **Step 2: Full build with all tests**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL with all tests passing

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "refactor: update all test imports for settings extraction to feature-model"
```

---

### Task 7: Cleanup — remove BomCacheSettings SPI from maven-resolver if applicable

**Files:**

- Check:
  `components/features/dependanger-maven-resolver/src/main/resources/META-INF/services/io.github.zenhelix.dependanger.effective.spi.FeatureSettingsProvider`

- [ ] **Step 1: Check if BomCacheSettings should also move**

`BomCacheSettings` + `BomCacheSettingsProvider` are in `dependanger-maven-resolver`. CLI does NOT import these. They are only used
internally by `BomImportProcessor`.

Decision: **Leave BomCacheSettings in maven-resolver** — it is not needed by any external consumer. Only move settings that have external
consumers (CLI commands).

- [ ] **Step 2: Verify final dependency graph**

Run: `./gradlew build` one final time.
Expected: BUILD SUCCESSFUL

The final dependency graph for CLI:

```
dependanger-cli
  ├── implementation: dependanger-api (facade)
  ├── implementation: dependanger-feature-model (settings contracts + extension keys)
  ├── runtimeOnly: dependanger-updates (ServiceLoader: processor)
  ├── runtimeOnly: dependanger-security (ServiceLoader: processor)
  ├── runtimeOnly: dependanger-license (ServiceLoader: processor)
  ├── runtimeOnly: dependanger-transitive (ServiceLoader: processor)
  ├── runtimeOnly: dependanger-analysis (ServiceLoader: processor)
  └── runtimeOnly: dependanger-report (ServiceLoader: report provider)
```

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "refactor: complete settings extraction — CLI feature deps now runtimeOnly"
```
