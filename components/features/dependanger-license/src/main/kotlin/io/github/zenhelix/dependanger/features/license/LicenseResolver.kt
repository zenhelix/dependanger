package io.github.zenhelix.dependanger.features.license

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.cache.CacheResult
import io.github.zenhelix.dependanger.cache.DirBasedCache
import io.github.zenhelix.dependanger.feature.model.license.LicenseCategory
import io.github.zenhelix.dependanger.features.license.model.LicenseResult
import io.github.zenhelix.dependanger.features.license.model.LicenseSource
import io.github.zenhelix.dependanger.features.license.spi.LicenseSourceProvider
import io.github.zenhelix.dependanger.features.resolver.DownloadResult
import io.github.zenhelix.dependanger.features.resolver.MavenPomDownloader
import io.github.zenhelix.dependanger.maven.pom.parser.PomParser

private val logger = KotlinLogging.logger {}

public class LicenseResolver(
    private val cache: DirBasedCache<List<LicenseResult>>,
    private val pomDownloader: MavenPomDownloader,
    private val clearlyDefinedClient: ClearlyDefinedClient,
    private val customProviders: List<LicenseSourceProvider> = emptyList(),
) {

    public suspend fun resolve(
        group: String,
        artifact: String,
        version: String,
        declaredLicenseId: String?,
    ): List<LicenseResult> {
        val coordinate = "$group:$artifact:$version"

        when (val cacheResult = cache.get(group, artifact, version)) {
            is CacheResult.Hit       -> {
                logger.debug { "Cache hit for $coordinate" }
                return cacheResult.data
            }

            is CacheResult.Corrupted -> {
                logger.warn { "Corrupted cache for $coordinate: ${cacheResult.error}" }
            }

            is CacheResult.Miss      -> {
                logger.debug { "Cache miss for $coordinate" }
            }
        }

        if (!declaredLicenseId.isNullOrBlank()) {
            val results = resolveDeclared(declaredLicenseId)
            cache.put(group, artifact, version, results)
            logger.info { "Resolved $coordinate from declared license: $declaredLicenseId" }
            return results
        }

        val customResults = resolveFromCustom(group, artifact, version)
        if (customResults != null) {
            cache.put(group, artifact, version, customResults)
            logger.info { "Resolved $coordinate from custom source (${customResults.size} license(s))" }
            return customResults
        }

        val pomResults = resolveFromPom(group, artifact, version)
        if (pomResults != null) {
            cache.put(group, artifact, version, pomResults)
            logger.info { "Resolved $coordinate from Maven POM (${pomResults.size} license(s))" }
            return pomResults
        }

        val clearlyDefinedResults = resolveFromClearlyDefined(group, artifact, version)
        if (clearlyDefinedResults != null) {
            cache.put(group, artifact, version, clearlyDefinedResults)
            logger.info { "Resolved $coordinate from ClearlyDefined (${clearlyDefinedResults.size} license(s))" }
            return clearlyDefinedResults
        }

        val stale = cache.getStale(group, artifact, version)
        if (stale != null) {
            logger.info { "Using stale cache for $coordinate" }
            return stale
        }

        val unknown = listOf(
            LicenseResult(
                spdxId = null,
                licenseName = null,
                source = LicenseSource.UNKNOWN,
                category = LicenseCategory.UNKNOWN,
            )
        )
        cache.put(group, artifact, version, unknown)
        logger.warn { "Could not resolve license for $coordinate, returning UNKNOWN" }
        return unknown
    }

    private fun resolveDeclared(declaredLicenseId: String): List<LicenseResult> {
        val spdxId = SpdxLicenseMapper.normalize(declaredLicenseId) ?: declaredLicenseId
        val category = SpdxLicenseMapper.categorize(spdxId)
        return listOf(createLicenseResult(spdxId = spdxId, licenseName = declaredLicenseId, source = LicenseSource.DECLARED, category = category))
    }

    private suspend fun resolveFromCustom(
        group: String,
        artifact: String,
        version: String,
    ): List<LicenseResult>? {
        if (customProviders.isEmpty()) return null

        val coordinate = "$group:$artifact:$version"
        val sorted = customProviders.sortedBy { it.priority }

        for (provider in sorted) {
            val resolved = try {
                provider.resolve(group, artifact, version)
            } catch (e: Exception) {
                logger.warn(e) { "Custom provider '${provider.sourceId}' failed for $coordinate" }
                continue
            }

            if (resolved != null) {
                logger.debug { "Custom provider '${provider.sourceId}' resolved $coordinate (${resolved.size} license(s))" }
                return resolved.map { rl ->
                    val spdxId = if (rl.spdxId != null) SpdxLicenseMapper.normalize(rl.spdxId) ?: rl.spdxId else null
                    val category = SpdxLicenseMapper.categorize(spdxId, rl.name)
                    createLicenseResult(spdxId = spdxId, licenseName = rl.name, source = LicenseSource.CUSTOM, category = category)
                }
            }
        }

        return null
    }

    private suspend fun resolveFromPom(
        group: String,
        artifact: String,
        version: String,
    ): List<LicenseResult>? {
        val coordinate = "$group:$artifact:$version"

        return when (val downloadResult = pomDownloader.downloadPom(group, artifact, version)) {
            is DownloadResult.Success      -> {
                val pomProject = try {
                    PomParser.parse(downloadResult.content)
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to parse POM for $coordinate" }
                    return null
                }

                val pomLicenses = pomProject.licenses
                if (pomLicenses.isEmpty()) {
                    logger.debug { "No licenses found in POM for $coordinate" }
                    return null
                }

                pomLicenses.map { pomLicense ->
                    val licenseName = pomLicense.name
                    val spdxId = if (licenseName != null) SpdxLicenseMapper.normalize(licenseName) else null
                    val category = SpdxLicenseMapper.categorize(spdxId, licenseName)
                    createLicenseResult(spdxId = spdxId, licenseName = licenseName, source = LicenseSource.MAVEN_POM, category = category)
                }
            }

            is DownloadResult.NotFound     -> {
                logger.debug { "POM not found for $coordinate" }
                null
            }

            is DownloadResult.AuthRequired -> {
                logger.warn { "Authentication required to download POM for $coordinate (${downloadResult.url})" }
                null
            }

            is DownloadResult.Failed       -> {
                logger.warn { "Failed to download POM for $coordinate: ${downloadResult.error}" }
                null
            }
        }
    }

    private suspend fun resolveFromClearlyDefined(
        group: String,
        artifact: String,
        version: String,
    ): List<LicenseResult>? {
        val coordinate = "$group:$artifact:$version"

        return when (val result = clearlyDefinedClient.fetchLicenses(group, artifact, version)) {
            is ClearlyDefinedResult.Success  -> {
                result.licenseIds.map { licenseId ->
                    val spdxId = SpdxLicenseMapper.normalize(licenseId) ?: licenseId
                    val category = SpdxLicenseMapper.categorize(spdxId)
                    createLicenseResult(spdxId = spdxId, licenseName = licenseId, source = LicenseSource.CLEARLY_DEFINED, category = category)
                }
            }

            is ClearlyDefinedResult.NotFound -> {
                logger.debug { "No ClearlyDefined data for $coordinate" }
                null
            }

            is ClearlyDefinedResult.Failed   -> {
                logger.warn { "ClearlyDefined lookup failed for $coordinate: ${result.error}" }
                null
            }
        }
    }

    private fun createLicenseResult(
        spdxId: String?,
        licenseName: String?,
        source: LicenseSource,
        category: LicenseCategory,
    ): LicenseResult = LicenseResult(
        spdxId = spdxId,
        licenseName = licenseName,
        source = source,
        category = category,
    )
}
