package io.github.zenhelix.dependanger.features.license.spi

public interface LicenseSourceProvider {
    public val sourceId: String
    public val priority: Int

    public suspend fun resolve(
        group: String,
        artifact: String,
        version: String,
    ): List<ResolvedLicense>?
}

public data class ResolvedLicense(
    val spdxId: String?,
    val name: String?,
)
