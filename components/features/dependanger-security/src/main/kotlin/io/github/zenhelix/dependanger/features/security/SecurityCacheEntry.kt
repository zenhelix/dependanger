package io.github.zenhelix.dependanger.features.security

import io.github.zenhelix.dependanger.feature.model.security.VulnerabilityInfo
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
internal data class SecurityCacheEntry(
    val vulnerabilities: List<VulnerabilityInfo>,
    val fetchedAt: Instant,
)
