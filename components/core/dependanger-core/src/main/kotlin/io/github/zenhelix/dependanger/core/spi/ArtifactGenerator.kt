package io.github.zenhelix.dependanger.core.spi

public interface ArtifactGenerator<T> {
    public val generatorId: String
    public fun generate(config: T): String
}
