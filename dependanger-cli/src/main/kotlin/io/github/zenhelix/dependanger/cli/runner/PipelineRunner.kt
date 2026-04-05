package io.github.zenhelix.dependanger.cli.runner

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import io.github.zenhelix.dependanger.api.Dependanger
import io.github.zenhelix.dependanger.api.DependangerBuilder
import io.github.zenhelix.dependanger.api.DependangerResult
import io.github.zenhelix.dependanger.cli.CliDefaults
import io.github.zenhelix.dependanger.cli.CliException
import io.github.zenhelix.dependanger.cli.CoroutineRunner
import io.github.zenhelix.dependanger.cli.MetadataService
import io.github.zenhelix.dependanger.cli.OutputFormatter
import io.github.zenhelix.dependanger.cli.options.PipelineOptions
import io.github.zenhelix.dependanger.cli.withErrorHandling
import java.nio.file.Path

public class PipelineRunner(
    command: CliktCommand,
    private val opts: PipelineOptions,
) {
    public val formatter: OutputFormatter = OutputFormatter(
        jsonMode = opts.format == CliDefaults.OUTPUT_FORMAT_JSON,
        terminal = command.terminal,
    )
    private val metadataService: MetadataService = MetadataService()

    public fun run(
        configure: DependangerBuilder.() -> Unit = {},
        handle: PipelineHandlerContext.(DependangerResult) -> Unit,
    ) {
        withErrorHandling(formatter) {
            val metadata = metadataService.read(Path.of(opts.input))
            val builder = Dependanger.fromMetadata(metadata)
            builder.configure()
            val dependanger = builder.build()
            val result = CoroutineRunner.run {
                dependanger.process(opts.distribution)
            }
            if (result is DependangerResult.Failure) {
                formatter.renderDiagnostics(result.diagnostics)
                throw CliException.ProcessingFailed("Processing pipeline failed")
            }
            PipelineHandlerContext(formatter, opts).handle(result)
        }
    }
}

public class PipelineHandlerContext(
    public val formatter: OutputFormatter,
    public val opts: PipelineOptions,
)
