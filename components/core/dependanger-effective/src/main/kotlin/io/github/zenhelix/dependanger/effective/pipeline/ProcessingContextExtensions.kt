package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.MavenRepository
import io.github.zenhelix.dependanger.core.model.Repository

private val DEFAULT_MAVEN_CENTRAL: List<MavenRepository> = listOf(
    MavenRepository(url = "https://repo.maven.apache.org/maven2", name = "Maven Central")
)

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
