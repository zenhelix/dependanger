package io.github.zenhelix.dependanger.integration.support

import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.api.compatibilityIssues
import io.github.zenhelix.dependanger.api.licenseViolations
import io.github.zenhelix.dependanger.api.transitives
import io.github.zenhelix.dependanger.api.updates
import io.github.zenhelix.dependanger.api.versionConflicts
import io.github.zenhelix.dependanger.api.vulnerabilities
import org.assertj.core.api.AbstractAssert

// --- Entry points ---

fun assertResult(result: DependangerResult): ResultAssert = ResultAssert(result)

fun assertToml(content: String): TomlAssert = TomlAssert(content)

fun assertBom(content: String): BomAssert = BomAssert(content)

// --- ResultAssert ---

class ResultAssert(actual: DependangerResult) :
    AbstractAssert<ResultAssert, DependangerResult>(actual, ResultAssert::class.java) {

    fun isSuccessful(): ResultAssert = apply {
        isNotNull
        if (!actual.isSuccess) {
            failWithMessage(
                "Expected result to be successful, but it failed. Errors: %s",
                actual.diagnostics.errors.map { "${it.code}: ${it.message}" },
            )
        }
    }

    fun isFailed(): ResultAssert = apply {
        isNotNull
        if (actual.isSuccess) {
            failWithMessage("Expected result to be failed, but it was successful")
        }
    }

    fun hasLibraryCount(expected: Int): ResultAssert = apply {
        isNotNull
        val count = actual.effectiveOrNull()?.libraries?.size ?: 0
        if (count != expected) {
            failWithMessage("Expected %d libraries, but found %d", expected, count)
        }
    }

    fun hasVersion(alias: String, value: String): ResultAssert = apply {
        isNotNull
        val versions = actual.effectiveOrNull()?.versions ?: emptyMap()
        val resolved = versions[alias]
        if (resolved == null) {
            failWithMessage("Expected version '%s' to exist, but it was not found. Available: %s", alias, versions.keys)
        } else if (resolved.value != value) {
            failWithMessage("Expected version '%s' = '%s', but was '%s'", alias, value, resolved.value)
        }
    }

    fun hasLibrary(alias: String): ResultAssert = apply {
        isNotNull
        val libraries = actual.effectiveOrNull()?.libraries ?: emptyMap()
        if (alias !in libraries) {
            failWithMessage("Expected library '%s' to exist, but it was not found. Available: %s", alias, libraries.keys)
        }
    }

    fun hasPlugin(alias: String): ResultAssert = apply {
        isNotNull
        val plugins = actual.effectiveOrNull()?.plugins ?: emptyMap()
        if (alias !in plugins) {
            failWithMessage("Expected plugin '%s' to exist, but it was not found. Available: %s", alias, plugins.keys)
        }
    }

    fun hasBundle(alias: String): ResultAssert = apply {
        isNotNull
        val bundles = actual.effectiveOrNull()?.bundles ?: emptyMap()
        if (alias !in bundles) {
            failWithMessage("Expected bundle '%s' to exist, but it was not found. Available: %s", alias, bundles.keys)
        }
    }

    fun hasNoErrors(): ResultAssert = apply {
        isNotNull
        if (actual.diagnostics.hasErrors) {
            failWithMessage(
                "Expected no errors, but found: %s",
                actual.diagnostics.errors.map { "${it.code}: ${it.message}" },
            )
        }
    }

    fun hasErrorWithCode(code: String): ResultAssert = apply {
        isNotNull
        val found = actual.diagnostics.errors.any { it.code == code }
        if (!found) {
            failWithMessage(
                "Expected error with code '%s', but errors were: %s",
                code,
                actual.diagnostics.errors.map { it.code },
            )
        }
    }

    fun hasWarningWithCode(code: String): ResultAssert = apply {
        isNotNull
        val found = actual.diagnostics.warnings.any { it.code == code }
        if (!found) {
            failWithMessage(
                "Expected warning with code '%s', but warnings were: %s",
                code,
                actual.diagnostics.warnings.map { it.code },
            )
        }
    }

    fun hasUpdatesCount(expected: Int): ResultAssert = apply {
        isNotNull
        val count = actual.updates.size
        if (count != expected) {
            failWithMessage("Expected %d updates, but found %d", expected, count)
        }
    }

    fun hasVulnerabilitiesCount(expected: Int): ResultAssert = apply {
        isNotNull
        val count = actual.vulnerabilities.size
        if (count != expected) {
            failWithMessage("Expected %d vulnerabilities, but found %d", expected, count)
        }
    }

    fun hasLicenseViolationsCount(expected: Int): ResultAssert = apply {
        isNotNull
        val count = actual.licenseViolations.size
        if (count != expected) {
            failWithMessage("Expected %d license violations, but found %d", expected, count)
        }
    }

    fun hasTransitivesCount(expected: Int): ResultAssert = apply {
        isNotNull
        val count = actual.transitives.size
        if (count != expected) {
            failWithMessage("Expected %d transitives, but found %d", expected, count)
        }
    }

    fun hasCompatibilityIssuesCount(expected: Int): ResultAssert = apply {
        isNotNull
        val count = actual.compatibilityIssues.size
        if (count != expected) {
            failWithMessage("Expected %d compatibility issues, but found %d", expected, count)
        }
    }

    fun hasVersionConflictsCount(expected: Int): ResultAssert = apply {
        isNotNull
        val count = actual.versionConflicts.size
        if (count != expected) {
            failWithMessage("Expected %d version conflicts, but found %d", expected, count)
        }
    }
}

// --- TomlAssert ---

class TomlAssert(actual: String) :
    AbstractAssert<TomlAssert, String>(actual, TomlAssert::class.java) {

    fun hasVersionsSection(): TomlAssert = apply {
        isNotNull
        if ("[versions]" !in actual) {
            failWithMessage("Expected TOML to contain [versions] section")
        }
    }

    fun hasLibrariesSection(): TomlAssert = apply {
        isNotNull
        if ("[libraries]" !in actual) {
            failWithMessage("Expected TOML to contain [libraries] section")
        }
    }

    fun hasPluginsSection(): TomlAssert = apply {
        isNotNull
        if ("[plugins]" !in actual) {
            failWithMessage("Expected TOML to contain [plugins] section")
        }
    }

    fun hasBundlesSection(): TomlAssert = apply {
        isNotNull
        if ("[bundles]" !in actual) {
            failWithMessage("Expected TOML to contain [bundles] section")
        }
    }

    fun containsVersion(alias: String, value: String): TomlAssert = apply {
        isNotNull
        val pattern = """$alias\s*=\s*"$value""""
        if (!Regex(pattern).containsMatchIn(actual)) {
            failWithMessage("Expected TOML to contain version %s = \"%s\"", alias, value)
        }
    }

    fun containsLibrary(alias: String): TomlAssert = apply {
        isNotNull
        val pattern = """(?m)^$alias\s*="""
        val librariesSection = extractSection("[libraries]")
        if (librariesSection == null || !Regex(pattern).containsMatchIn(librariesSection)) {
            failWithMessage("Expected TOML [libraries] section to contain '%s'", alias)
        }
    }

    fun doesNotContainLibrary(alias: String): TomlAssert = apply {
        isNotNull
        val pattern = """(?m)^$alias\s*="""
        val librariesSection = extractSection("[libraries]")
        if (librariesSection != null && Regex(pattern).containsMatchIn(librariesSection)) {
            failWithMessage("Expected TOML [libraries] section NOT to contain '%s'", alias)
        }
    }

    fun containsBundle(alias: String, vararg libs: String): TomlAssert = apply {
        isNotNull
        val bundlesSection = extractSection("[bundles]")
        if (bundlesSection == null) {
            failWithMessage("Expected TOML to contain [bundles] section with bundle '%s'", alias)
            return@apply
        }
        val pattern = """(?m)^$alias\s*="""
        if (!Regex(pattern).containsMatchIn(bundlesSection)) {
            failWithMessage("Expected TOML [bundles] section to contain '%s'", alias)
            return@apply
        }
        for (lib in libs) {
            if (lib !in bundlesSection) {
                failWithMessage("Expected bundle '%s' to reference library '%s'", alias, lib)
            }
        }
    }

    fun containsPlugin(alias: String): TomlAssert = apply {
        isNotNull
        val pattern = """(?m)^$alias\s*="""
        val pluginsSection = extractSection("[plugins]")
        if (pluginsSection == null || !Regex(pattern).containsMatchIn(pluginsSection)) {
            failWithMessage("Expected TOML [plugins] section to contain '%s'", alias)
        }
    }

    private fun extractSection(header: String): String? {
        val idx = actual.indexOf(header)
        if (idx == -1) return null
        val afterHeader = actual.substring(idx + header.length)
        val nextSection = afterHeader.indexOf("\n[")
        return if (nextSection == -1) afterHeader else afterHeader.substring(0, nextSection)
    }
}

// --- BomAssert ---

class BomAssert(actual: String) :
    AbstractAssert<BomAssert, String>(actual, BomAssert::class.java) {

    fun containsDependency(group: String, artifact: String, version: String): BomAssert = apply {
        isNotNull
        val depPattern = Regex(
            """<dependency>\s*<groupId>\Q$group\E</groupId>\s*<artifactId>\Q$artifact\E</artifactId>\s*<version>\Q$version\E</version>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        if (!depPattern.containsMatchIn(actual)) {
            failWithMessage(
                "Expected BOM to contain dependency %s:%s:%s",
                group, artifact, version,
            )
        }
    }

    fun doesNotContainDependency(group: String, artifact: String): BomAssert = apply {
        isNotNull
        // Check that group+artifact don't appear together in the same <dependency> block
        val depPattern = Regex(
            """<dependency>\s*<groupId>\Q$group\E</groupId>\s*<artifactId>\Q$artifact\E</artifactId>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        if (depPattern.containsMatchIn(actual)) {
            failWithMessage("Expected BOM NOT to contain dependency %s:%s", group, artifact)
        }
    }

    fun hasGroupId(groupId: String): BomAssert = apply {
        isNotNull
        // Match project-level groupId (not inside <dependency>)
        val pattern = Regex("""<project[^>]*>.*?<groupId>\Q$groupId\E</groupId>""", RegexOption.DOT_MATCHES_ALL)
        if (!pattern.containsMatchIn(actual)) {
            failWithMessage("Expected BOM project groupId to be '%s'", groupId)
        }
    }

    fun hasArtifactId(artifactId: String): BomAssert = apply {
        isNotNull
        val pattern = Regex("""<project[^>]*>.*?<artifactId>\Q$artifactId\E</artifactId>""", RegexOption.DOT_MATCHES_ALL)
        if (!pattern.containsMatchIn(actual)) {
            failWithMessage("Expected BOM project artifactId to be '%s'", artifactId)
        }
    }
}
