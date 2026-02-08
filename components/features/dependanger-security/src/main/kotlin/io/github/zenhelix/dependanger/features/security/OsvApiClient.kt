package io.github.zenhelix.dependanger.features.security

import io.github.zenhelix.dependanger.features.security.model.VulnerabilityInfo

public class OsvApiClient(
    public val apiUrl: String = "https://api.osv.dev",
) {
    public suspend fun queryVulnerabilities(group: String, artifact: String, version: String): List<VulnerabilityInfo> = TODO()
}
