package io.github.zenhelix.dependanger.effective.model

import io.github.zenhelix.dependanger.core.model.Constraint
import io.github.zenhelix.dependanger.core.model.DeprecationInfo
import io.github.zenhelix.dependanger.core.model.LicenseInfo
import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import io.github.zenhelix.dependanger.core.model.Requirements
import kotlinx.serialization.Serializable

@Serializable
public data class EffectiveLibrary(
    val alias: String,
    val coordinate: MavenCoordinate,
    val version: EffectiveVersion,
    val description: String?,
    val tags: Set<String>,
    val requires: Requirements?,
    val deprecation: DeprecationInfo?,
    val license: LicenseInfo?,
    val constraints: List<Constraint>,
    val isPlatform: Boolean,
    val ignoreUpdates: Boolean = false,
) {
    val isDeprecated: Boolean get() = deprecation != null

    /** Human-readable deprecation summary, e.g. "DEPRECATED: Use x instead. Removal: 2.0". Null when not deprecated. */
    val deprecationSummary: String?
        get() = deprecation?.toCommentParts()?.joinToString(". ")
}
