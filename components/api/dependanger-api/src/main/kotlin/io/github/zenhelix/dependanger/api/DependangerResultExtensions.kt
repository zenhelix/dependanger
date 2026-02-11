package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.effective.model.CompatibilityIssue
import io.github.zenhelix.dependanger.effective.model.CompatibilityIssuesExtensionKey
import io.github.zenhelix.dependanger.effective.model.getExtension
import io.github.zenhelix.dependanger.features.license.model.LicenseViolation
import io.github.zenhelix.dependanger.features.license.model.LicenseViolationsExtensionKey
import io.github.zenhelix.dependanger.features.security.model.VulnerabilitiesExtensionKey
import io.github.zenhelix.dependanger.features.security.model.VulnerabilityInfo
import io.github.zenhelix.dependanger.features.transitive.model.TransitiveTree
import io.github.zenhelix.dependanger.features.transitive.model.TransitivesExtensionKey
import io.github.zenhelix.dependanger.features.transitive.model.VersionConflict
import io.github.zenhelix.dependanger.features.transitive.model.VersionConflictsExtensionKey
import io.github.zenhelix.dependanger.features.updates.model.UpdateAvailableInfo
import io.github.zenhelix.dependanger.features.updates.model.UpdatesExtensionKey

public val DependangerResult.updates: List<UpdateAvailableInfo>
    get() = effective?.getExtension(UpdatesExtensionKey) ?: emptyList()

public val DependangerResult.vulnerabilities: List<VulnerabilityInfo>
    get() = effective?.getExtension(VulnerabilitiesExtensionKey) ?: emptyList()

public val DependangerResult.licenseViolations: List<LicenseViolation>
    get() = effective?.getExtension(LicenseViolationsExtensionKey) ?: emptyList()

public val DependangerResult.transitives: List<TransitiveTree>
    get() = effective?.getExtension(TransitivesExtensionKey) ?: emptyList()

public val DependangerResult.compatibilityIssues: List<CompatibilityIssue>
    get() = effective?.getExtension(CompatibilityIssuesExtensionKey) ?: emptyList()

public val DependangerResult.versionConflicts: List<VersionConflict>
    get() = effective?.getExtension(VersionConflictsExtensionKey) ?: emptyList()
