package io.github.zenhelix.dependanger.features.security

import io.github.zenhelix.dependanger.features.security.model.VulnerabilityInfo

public data class PackageQuery(
    val group: String,
    val artifact: String,
    val version: String,
)

public class OsvApiClient(
    public val apiUrl: String = "https://api.osv.dev",
) {
    public suspend fun queryBatch(packages: List<PackageQuery>): List<List<VulnerabilityInfo>> = TODO()
}
