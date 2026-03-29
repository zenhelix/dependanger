package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.testing.CliktCommandTestResult
import com.github.ajalt.clikt.testing.test
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.DependangerBuilder
import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.core.model.Bundle
import io.github.zenhelix.dependanger.features.transitive.ConflictResolutionStrategy
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Library
import io.github.zenhelix.dependanger.core.model.Plugin
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.model.Version
import io.github.zenhelix.dependanger.core.model.VersionReference
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import io.github.zenhelix.dependanger.core.util.UpdateType
import io.github.zenhelix.dependanger.effective.model.CompatibilityIssue
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import io.github.zenhelix.dependanger.effective.model.ExtensionKey
import io.github.zenhelix.dependanger.features.license.model.LicenseCategory
import io.github.zenhelix.dependanger.features.license.model.LicenseViolation
import io.github.zenhelix.dependanger.features.license.model.LicenseViolationType
import io.github.zenhelix.dependanger.features.security.model.VulnerabilityInfo
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitySeverity
import io.github.zenhelix.dependanger.features.transitive.model.TransitiveTree
import io.github.zenhelix.dependanger.features.transitive.model.VersionConflict
import io.github.zenhelix.dependanger.features.updates.model.UpdateAvailableInfo
import io.github.zenhelix.dependanger.metadata.JsonSerializationFormat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.nio.file.Path

object CliTestSupport {

    fun runCli(vararg args: String): CliktCommandTestResult {
        val cli = DependangerCli().subcommands(
            InitCommand(),
            AddVersionCommand(),
            AddLibraryCommand(),
            AddPluginCommand(),
            AddBundleCommand(),
            AddBomCommand(),
            AddDistributionCommand(),
            RemoveVersionCommand(),
            RemoveLibraryCommand(),
            RemovePluginCommand(),
            RemoveBundleCommand(),
            RemoveBomCommand(),
            RemoveDistributionCommand(),
            UpdateVersionCommand(),
            UpdateLibraryCommand(),
            MigrateDeprecatedCommand(),
            ValidateCommand(),
            ProcessCommand(),
            GenerateCommand(),
            CheckUpdatesCommand(),
            AnalyzeCommand(),
            SecurityCheckCommand(),
            LicenseCheckCommand(),
            ResolveTransitivesCommand(),
            ReportCommand(),
        )
        return cli.test(argv = args.toList())
    }

    private val format = JsonSerializationFormat()

    fun writeMetadata(dir: Path, metadata: DependangerMetadata): Path {
        val file = dir.resolve(CliDefaults.METADATA_FILE)
        format.write(metadata, file)
        return file
    }

    fun readMetadata(path: Path): DependangerMetadata = format.read(path)

    fun emptyMetadata(): DependangerMetadata = MetadataService().emptyMetadata()

    fun minimalMetadata(): DependangerMetadata = emptyMetadata().copy(
        versions = listOf(
            Version(name = "kotlin", value = "2.1.20", fallbacks = emptyList()),
            Version(name = "coroutines", value = "1.10.1", fallbacks = emptyList()),
        ),
        libraries = listOf(
            Library(
                alias = "stdlib",
                group = "org.jetbrains.kotlin",
                artifact = "kotlin-stdlib",
                version = VersionReference.Reference(name = "kotlin"),
                description = null,
                tags = setOf("kotlin", "core"),
                requires = null,
                deprecation = null,
                license = null,
                constraints = emptyList(),
                isPlatform = false,
            ),
            Library(
                alias = "coroutines",
                group = "org.jetbrains.kotlinx",
                artifact = "kotlinx-coroutines-core",
                version = VersionReference.Reference(name = "coroutines"),
                description = null,
                tags = setOf("kotlin"),
                requires = null,
                deprecation = null,
                license = null,
                constraints = emptyList(),
                isPlatform = false,
            ),
        ),
        plugins = listOf(
            Plugin(
                alias = "kotlin-jvm",
                id = "org.jetbrains.kotlin.jvm",
                version = VersionReference.Reference(name = "kotlin"),
                tags = emptySet(),
            ),
        ),
        bundles = listOf(
            Bundle(alias = "kotlin-essentials", libraries = listOf("stdlib", "coroutines"), extends = emptyList()),
        ),
    )

    fun mockDependangerResult(
        extensions: Map<ExtensionKey<*>, Any> = emptyMap(),
        diagnostics: Diagnostics = Diagnostics.EMPTY,
    ): AutoCloseable {
        val effective = EffectiveMetadata(
            schemaVersion = "1.0",
            distribution = null,
            versions = emptyMap(),
            libraries = emptyMap(),
            plugins = emptyMap(),
            bundles = emptyMap(),
            diagnostics = diagnostics,
            processingInfo = null,
            extensions = extensions,
        )
        val result = DependangerResult(effective = effective, diagnostics = diagnostics)

        val mockDependanger = mockk<Dependanger>()
        coEvery { mockDependanger.process(any(), any()) } returns result
        coEvery { mockDependanger.validate() } returns result

        val mockBuilder = mockk<DependangerBuilder>()
        every { mockBuilder.preset(any()) } returns mockBuilder
        every { mockBuilder.disableProcessor(any()) } returns mockBuilder
        every { mockBuilder.withContextProperty(any(), any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockDependanger

        mockkObject(Dependanger.Companion)
        every { Dependanger.fromMetadata(any()) } returns mockBuilder

        return AutoCloseable { unmockkObject(Dependanger.Companion) }
    }

    fun sampleUpdate(
        alias: String = "stdlib",
        group: String = "org.jetbrains.kotlin",
        artifact: String = "kotlin-stdlib",
        currentVersion: String = "2.1.20",
        latestVersion: String = "2.2.0",
        updateType: UpdateType = UpdateType.MINOR,
    ): UpdateAvailableInfo = UpdateAvailableInfo(
        alias = alias,
        group = group,
        artifact = artifact,
        currentVersion = currentVersion,
        latestVersion = latestVersion,
        latestStable = latestVersion,
        latestAny = latestVersion,
        updateType = updateType,
        repository = null,
    )

    fun sampleVulnerability(
        id: String = "GHSA-test-001",
        severity: VulnerabilitySeverity = VulnerabilitySeverity.HIGH,
        group: String = "org.example",
        artifact: String = "vulnerable-lib",
        version: String = "1.0.0",
        summary: String = "Test vulnerability",
        cvssScore: Double? = 7.5,
    ): VulnerabilityInfo = VulnerabilityInfo(
        id = id,
        aliases = listOf("CVE-2024-0001"),
        summary = summary,
        severity = severity,
        cvssScore = cvssScore,
        cvssVersion = "3.1",
        fixedVersion = "2.0.0",
        url = "https://example.com/advisory",
        affectedGroup = group,
        affectedArtifact = artifact,
        affectedVersion = version,
    )

    fun sampleLicenseViolation(
        alias: String = "gpl-lib",
        group: String = "org.example",
        artifact: String = "gpl-library",
        license: String? = "GPL-3.0",
        category: LicenseCategory = LicenseCategory.STRONG_COPYLEFT,
        violationType: LicenseViolationType = LicenseViolationType.DENIED,
    ): LicenseViolation = LicenseViolation(
        alias = alias,
        group = group,
        artifact = artifact,
        detectedLicense = license,
        category = category,
        violationType = violationType,
        message = "License $license is not allowed by policy",
    )

    fun sampleCompatibilityIssue(
        ruleId: String = "JDK_COMPAT",
        severity: Severity = Severity.WARNING,
        message: String = "Library requires JDK 21 but target is JDK 17",
    ): CompatibilityIssue = CompatibilityIssue(
        ruleId = ruleId,
        message = message,
        severity = severity,
        affectedLibraries = listOf("stdlib", "coroutines"),
        suggestion = "Upgrade JDK to 21",
    )

    fun sampleTransitiveTree(
        group: String = "org.jetbrains.kotlin",
        artifact: String = "kotlin-stdlib",
        version: String = "2.1.20",
    ): TransitiveTree = TransitiveTree(
        group = group,
        artifact = artifact,
        version = version,
        scope = "compile",
        children = listOf(
            TransitiveTree(
                group = "org.jetbrains",
                artifact = "annotations",
                version = "24.0.0",
                scope = "compile",
                children = emptyList(),
                isDuplicate = false,
                isCycle = false,
            )
        ),
        isDuplicate = false,
        isCycle = false,
    )

    fun sampleVersionConflict(): VersionConflict = VersionConflict(
        group = "org.jetbrains",
        artifact = "annotations",
        requestedVersions = listOf("23.0.0", "24.0.0"),
        resolvedVersion = "24.0.0",
        resolution = ConflictResolutionStrategy.HIGHEST,
    )
}
