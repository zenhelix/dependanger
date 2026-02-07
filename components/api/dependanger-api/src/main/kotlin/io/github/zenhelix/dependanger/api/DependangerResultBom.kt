package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.generators.bom.BomConfig
import io.github.zenhelix.dependanger.generators.bom.BomGenerator
import java.nio.file.Path

public fun DependangerResult.toBom(config: BomConfig): String =
    generate(BomGenerator(config))

public fun DependangerResult.writeBomTo(path: Path, config: BomConfig): Path =
    writeTo(BomGenerator(config), path)
