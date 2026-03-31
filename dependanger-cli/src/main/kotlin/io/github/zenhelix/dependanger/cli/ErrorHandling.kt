package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.ProgramResult
import io.github.zenhelix.dependanger.api.DependangerException

public fun withErrorHandling(formatter: OutputFormatter, block: () -> Unit) {
    try {
        block()
    } catch (e: CliException.ValidationFailed) {
        formatter.renderDiagnostics(e.diagnostics)
        throw ProgramResult(1)
    } catch (e: CliException) {
        formatter.error(e.message ?: "Unknown error")
        throw ProgramResult(1)
    } catch (e: DependangerException) {
        formatter.error(e.message ?: "Processing error")
        if (e.cause != null) {
            formatter.error("Caused by: ${e.cause?.message}")
        }
        throw ProgramResult(1)
    } catch (e: Exception) {
        formatter.error("Unexpected error: ${e.message ?: "Unknown error"}")
        if (e.cause != null) {
            formatter.error("Caused by: ${e.cause?.message}")
        }
        throw ProgramResult(1)
    }
}
