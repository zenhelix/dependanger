package io.github.zenhelix.dependanger.effective.model

import io.github.zenhelix.dependanger.core.model.Constraint
import io.github.zenhelix.dependanger.core.model.DeprecationInfo
import io.github.zenhelix.dependanger.core.model.LicenseInfo
import io.github.zenhelix.dependanger.core.model.Requirements
import kotlinx.serialization.Serializable

@Serializable
public data class EffectiveLibrary(
    val alias: String,
    val group: String,
    val artifact: String,
    val version: ResolvedVersion?,
    val description: String?,
    val tags: Set<String>,
    val requires: Requirements?,
    /** Derived from [deprecation] != null. Kept as a constructor parameter to preserve JSON serialization round-trip. */
    val isDeprecated: Boolean,
    val deprecation: DeprecationInfo?,
    val license: LicenseInfo?,
    val constraints: List<Constraint>,
    val isPlatform: Boolean,
    val ignoreUpdates: Boolean = false,
)
