package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.Distribution
import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.model.Repository

private val DEFAULT_MAVEN_CENTRAL: List<MavenRepository> = listOf(
    MavenRepository(url = "https://repo.maven.apache.org/maven2", name = "Maven Central")
)

/**
 * Resolves the effective list of Maven repositories using a three-tier fallback:
 * 1. [featureRepositories] — feature-specific repos (e.g. from UpdateCheckSettings.repositories)
 * 2. Global [ProcessingContext.settings] repositories
 * 3. Maven Central as a last resort
 */
/**
 * Resolves the active distribution by name from originalMetadata.
 */
public fun ProcessingContext.resolveDistribution(distributionName: String?): Distribution? =
    distributionName?.let { name -> originalMetadata.distributions.find { it.name == name } }

public fun ProcessingContext.resolveMavenRepositories(
    featureRepositories: List<Repository> = emptyList(),
): List<MavenRepository> =
    featureRepositories
        .filterIsInstance<MavenRepository>()
        .ifEmpty {
            settings.repositories
                .filterIsInstance<MavenRepository>()
                .ifEmpty { DEFAULT_MAVEN_CENTRAL }
        }
