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
    val name: String,
    val version: ResolvedVersion? = null,
    val tags: Set<String> = emptySet(),
    val requires: Requirements? = null,
    val deprecation: DeprecationInfo? = null,
    val license: LicenseInfo? = null,
    val constraints: List<Constraint> = emptyList(),
    val isPlatform: Boolean = false,
)
