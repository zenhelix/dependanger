package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

public interface ArtifactGenerator<T> {
    public val generatorId: String
    public val description: String
    public val fileExtension: String
    public fun generate(effective: EffectiveMetadata): T
    public fun write(artifact: T, output: Path)
}

/** Writes a string artifact to [filename] inside [output] directory, creating parent dirs as needed. */
public fun writeStringArtifact(artifact: String, output: Path, filename: String) {
    val targetFile = output.resolve(filename)
    Files.createDirectories(targetFile.parent)
    Files.writeString(targetFile, artifact, StandardCharsets.UTF_8)
}
