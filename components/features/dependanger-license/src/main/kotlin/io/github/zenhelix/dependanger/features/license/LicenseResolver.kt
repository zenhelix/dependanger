package io.github.zenhelix.dependanger.features.license

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.zenhelix.dependanger.cache.CacheResult
import io.github.zenhelix.dependanger.cache.DirBasedCache
import io.github.zenhelix.dependanger.clearlydefined.client.ClearlyDefinedClient
import io.github.zenhelix.dependanger.clearlydefined.client.model.ClearlyDefinedResult
import io.github.zenhelix.dependanger.core.model.MavenCoordinate
import io.github.zenhelix.dependanger.core.model.MavenGAV
import io.github.zenhelix.dependanger.feature.model.license.LicenseCategory
import io.github.zenhelix.dependanger.features.license.model.LicenseResult
import io.github.zenhelix.dependanger.features.license.model.LicenseSource
import io.github.zenhelix.dependanger.features.license.spi.LicenseSourceProvider
import io.github.zenhelix.dependanger.maven.client.MavenPomService
import io.github.zenhelix.dependanger.maven.client.model.DownloadResult
import io.github.zenhelix.dependanger.maven.pom.parser.PomParser

private val logger = KotlinLogging.logger {}

public class LicenseResolver(
    private val cache: DirBasedCache<List<LicenseResult>>,
    private val pomService: MavenPomService,
    private val clearlyDefinedClient: ClearlyDefinedClient,
    private val customProviders: List<LicenseSourceProvider> = emptyList(),
) {

    public suspend fun resolve(
        coordinate: MavenCoordinate,
        version: String,
        declaredLicenseId: String?,
    ): List<LicenseResult> {
        val gav = MavenGAV(coordinate, version)

        when (val cacheResult = cache.get(coordinate, version)) {
            is CacheResult.Hit       -> {
                logger.debug { "Cache hit for $coordinate:$version" }
                return cacheResult.data
            }

            is CacheResult.Corrupted -> {
                logger.warn { "Corrupted cache for $coordinate:$version: ${cacheResult.error}" }
            }

            is CacheResult.Miss      -> {
                logger.debug { "Cache miss for $coordinate:$version" }
            }
        }

        if (!declaredLicenseId.isNullOrBlank()) {
            val results = resolveDeclared(declaredLicenseId)
            cache.put(coordinate, version, results)
            logger.info { "Resolved $coordinate:$version from declared license: $declaredLicenseId" }
            return results
        }

        val customResults = resolveFromCustom(gav)
        if (customResults != null) {
            cache.put(coordinate, version, customResults)
            logger.info { "Resolved $coordinate:$version from custom source (${customResults.size} license(s))" }
            return customResults
        }

        val pomResults = resolveFromPom(gav)
        if (pomResults != null) {
            cache.put(coordinate, version, pomResults)
            logger.info { "Resolved $coordinate:$version from Maven POM (${pomResults.size} license(s))" }
            return pomResults
        }

        val clearlyDefinedResults = resolveFromClearlyDefined(gav)
        if (clearlyDefinedResults != null) {
            cache.put(coordinate, version, clearlyDefinedResults)
            logger.info { "Resolved $coordinate:$version from ClearlyDefined (${clearlyDefinedResults.size} license(s))" }
            return clearlyDefinedResults
        }

        val stale = cache.getStale(coordinate, version)
        if (stale != null) {
            logger.info { "Using stale cache for $coordinate:$version" }
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
        cache.put(coordinate, version, unknown)
        logger.warn { "Could not resolve license for $coordinate:$version, returning UNKNOWN" }
        return unknown
    }

    private fun resolveDeclared(declaredLicenseId: String): List<LicenseResult> {
        val spdxId = SpdxLicenseMapper.normalize(declaredLicenseId) ?: declaredLicenseId
        val category = SpdxLicenseMapper.categorize(spdxId)
        return listOf(LicenseResult(spdxId = spdxId, licenseName = declaredLicenseId, source = LicenseSource.DECLARED, category = category))
    }

    private suspend fun resolveFromCustom(gav: MavenGAV): List<LicenseResult>? {
        if (customProviders.isEmpty()) return null

        val sorted = customProviders.sortedBy { it.priority }

        for (provider in sorted) {
            val resolved = try {
                provider.resolve(gav.coordinate.group, gav.coordinate.artifact, gav.version)
            } catch (e: Exception) {
                logger.warn(e) { "Custom provider '${provider.sourceId}' failed for $gav" }
                continue
            }

            if (resolved != null) {
                logger.debug { "Custom provider '${provider.sourceId}' resolved $gav (${resolved.size} license(s))" }
                return resolved.map { rl ->
                    val spdxId = if (rl.spdxId != null) SpdxLicenseMapper.normalize(rl.spdxId) ?: rl.spdxId else null
                    val category = SpdxLicenseMapper.categorize(spdxId, rl.name)
                    LicenseResult(spdxId = spdxId, licenseName = rl.name, source = LicenseSource.CUSTOM, category = category)
                }
            }
        }

        return null
    }

    private suspend fun resolveFromPom(gav: MavenGAV): List<LicenseResult>? {
        return when (val downloadResult = pomService.downloadPom(gav.coordinate.group, gav.coordinate.artifact, gav.version)) {
            is DownloadResult.Success      -> {
                val pomProject = try {
                    PomParser.parse(downloadResult.content)
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to parse POM for $gav" }
                    return null
                }

                val pomLicenses = pomProject.licenses
                if (pomLicenses.isEmpty()) {
                    logger.debug { "No licenses found in POM for $gav" }
                    return null
                }

                pomLicenses.map { pomLicense ->
                    val licenseName = pomLicense.name
                    val spdxId = if (licenseName != null) SpdxLicenseMapper.normalize(licenseName) else null
                    val category = SpdxLicenseMapper.categorize(spdxId, licenseName)
                    LicenseResult(spdxId = spdxId, licenseName = licenseName, source = LicenseSource.MAVEN_POM, category = category)
                }
            }

            is DownloadResult.NotFound     -> {
                logger.debug { "POM not found for $gav" }
                null
            }

            is DownloadResult.AuthRequired -> {
                logger.warn { "Authentication required to download POM for $gav (${downloadResult.url})" }
                null
            }

            is DownloadResult.Failed       -> {
                logger.warn { "Failed to download POM for $gav: ${downloadResult.error}" }
                null
            }
        }
    }

    private suspend fun resolveFromClearlyDefined(gav: MavenGAV): List<LicenseResult>? {
        return when (val result = clearlyDefinedClient.fetchLicense(gav.coordinate.group, gav.coordinate.artifact, gav.version)) {
            is ClearlyDefinedResult.Found -> {
                val licenseIds = SpdxExpressionParser.parse(result.declaredExpression)
                if (licenseIds.isEmpty()) return null
                licenseIds.map { licenseId ->
                    val spdxId = SpdxLicenseMapper.normalize(licenseId) ?: licenseId
                    val category = SpdxLicenseMapper.categorize(spdxId)
                    LicenseResult(spdxId = spdxId, licenseName = licenseId, source = LicenseSource.CLEARLY_DEFINED, category = category)
                }
            }

            is ClearlyDefinedResult.NotFound -> {
                logger.debug { "No ClearlyDefined data for $gav" }
                null
            }

            is ClearlyDefinedResult.Failed   -> {
                logger.warn { "ClearlyDefined lookup failed for $gav: ${result.error}" }
                null
            }
        }
    }
}
