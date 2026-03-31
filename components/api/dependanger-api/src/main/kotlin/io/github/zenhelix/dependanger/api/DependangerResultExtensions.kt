package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.effective.model.CompatibilityIssue
import io.github.zenhelix.dependanger.effective.model.CompatibilityIssuesExtensionKey
import io.github.zenhelix.dependanger.effective.model.getExtension
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolation
import io.github.zenhelix.dependanger.feature.model.license.LicenseViolationsExtensionKey
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitiesExtensionKey
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilityInfo
import io.github.zenhelix.dependanger.feature.model.transitive.TransitiveTree
import io.github.zenhelix.dependanger.feature.model.transitive.TransitivesExtensionKey
import io.github.zenhelix.dependanger.feature.model.transitive.VersionConflict
import io.github.zenhelix.dependanger.feature.model.transitive.VersionConflictsExtensionKey
import io.github.zenhelix.dependanger.feature.model.updates.UpdateAvailableInfo
import io.github.zenhelix.dependanger.feature.model.updates.UpdatesExtensionKey

public val DependangerResult.updates: List<UpdateAvailableInfo>
    get() = effectiveOrNull()?.getExtension(UpdatesExtensionKey) ?: emptyList()

public val DependangerResult.vulnerabilities: List<VulnerabilityInfo>
    get() = effectiveOrNull()?.getExtension(VulnerabilitiesExtensionKey) ?: emptyList()

public val DependangerResult.licenseViolations: List<LicenseViolation>
    get() = effectiveOrNull()?.getExtension(LicenseViolationsExtensionKey) ?: emptyList()

public val DependangerResult.transitives: List<TransitiveTree>
    get() = effectiveOrNull()?.getExtension(TransitivesExtensionKey) ?: emptyList()

public val DependangerResult.compatibilityIssues: List<CompatibilityIssue>
    get() = effectiveOrNull()?.getExtension(CompatibilityIssuesExtensionKey) ?: emptyList()

public val DependangerResult.versionConflicts: List<VersionConflict>
    get() = effectiveOrNull()?.getExtension(VersionConflictsExtensionKey) ?: emptyList()
