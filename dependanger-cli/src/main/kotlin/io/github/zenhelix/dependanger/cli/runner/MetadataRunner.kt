package io.github.zenhelix.dependanger.cli.runner

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import io.github.zenhelix.dependanger.cli.MetadataService
import io.github.zenhelix.dependanger.cli.OutputFormatter
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.withErrorHandling
import io.github.zenhelix.dependanger.core.model.metadata.DependangerMetadata
import java.nio.file.Path

public class MetadataRunner(
    command: CliktCommand,
    private val opts: MetadataOptions,
) {
    public val formatter: OutputFormatter = OutputFormatter(terminal = command.terminal)
    private val metadataService: MetadataService = MetadataService()

    public fun run(transform: (DependangerMetadata) -> Pair<DependangerMetadata, String>) {
        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(opts.input))
            val (updated, message) = transform(metadata)
            metadataService.write(updated, Path.of(opts.output ?: opts.input))
            formatter.success(message)
        }
    }

    public fun readAndHandle(block: MetadataCommandContext.() -> Unit) {
        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(opts.input))
            MetadataCommandContext(metadata, metadataService, formatter, opts).block()
        }
    }
}

public class MetadataCommandContext(
    public val metadata: DependangerMetadata,
    private val metadataService: MetadataService,
    public val formatter: OutputFormatter,
    private val opts: MetadataOptions,
) {
    public fun write(updated: DependangerMetadata, path: Path? = null) {
        metadataService.write(updated, path ?: Path.of(opts.output ?: opts.input))
    }
}
