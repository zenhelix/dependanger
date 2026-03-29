package io.github.zenhelix.dependanger.cli

import com.github.ajalt.mordant.rendering.TextColors.brightGreen
import com.github.ajalt.mordant.rendering.TextColors.brightRed
import com.github.ajalt.mordant.rendering.TextColors.brightYellow
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.core.model.Severity
import kotlinx.serialization.KSerializer

public class OutputFormatter(
    public val jsonMode: Boolean = false,
    terminal: Terminal? = null,
) {
    private val terminal: Terminal = terminal ?: Terminal()

    public fun success(message: String) {
        if (!jsonMode) {
            terminal.println(brightGreen("[OK] $message"))
        }
    }

    public fun error(message: String) {
        terminal.println(brightRed("[ERROR] $message"), stderr = true)
    }

    public fun warning(message: String) {
        if (!jsonMode) {
            terminal.println(brightYellow("[WARN] $message"))
        }
    }

    public fun info(message: String) {
        if (!jsonMode) {
            terminal.println(gray("[INFO] $message"))
        }
    }

    public fun println(message: String) {
        terminal.println(message)
    }

    public fun renderDiagnostics(diagnostics: Diagnostics) {
        val allMessages = diagnostics.errors + diagnostics.warnings + diagnostics.infos

        if (allMessages.isEmpty()) return

        val rendered = table {
            header {
                row("Severity", "Code", "Message", "Processor")
            }
            body {
                allMessages.forEach { msg ->
                    row(
                        when (msg.severity) {
                            Severity.ERROR   -> brightRed("ERROR")
                            Severity.WARNING -> brightYellow("WARN")
                            Severity.INFO    -> gray("INFO")
                        },
                        msg.code,
                        msg.message,
                        msg.processorId ?: "-",
                    )
                }
            }
        }
        terminal.println(rendered)
    }

    public fun renderTable(headers: List<String>, rows: List<List<String>>) {
        if (rows.isEmpty()) {
            info("No results")
            return
        }
        val rendered = table {
            header {
                row(headers)
            }
            body {
                rows.forEach { row(it) }
            }
        }
        terminal.println(rendered)
    }

    public fun <T> renderJson(value: T, serializer: KSerializer<T>) {
        terminal.println(CliDefaults.CLI_JSON.encodeToString(serializer, value))
    }

    public fun printRaw(text: String) {
        terminal.print(text)
    }
}
