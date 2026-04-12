package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.zenhelix.dependanger.cli.options.MetadataOptions
import io.github.zenhelix.dependanger.cli.runner.MetadataRunner
import java.nio.file.Path

public class MigrateDeprecatedCommand : CliktCommand(name = "migrate-deprecated") {
    override fun help(context: Context): String = "Migrate deprecated libraries to their replacements"

    public val dryRun: Boolean by option("--dry-run", help = "Show migration plan without executing").flag()
    public val replace: Boolean by option("--replace", help = "Replace deprecated with replacedBy in bundles").flag(default = true)
    public val remove: Boolean by option("--remove", help = "Remove deprecated libraries from metadata").flag()
    public val removeFromBundles: Boolean by option("--remove-from-bundles", help = "Remove deprecated from bundles instead of replacing").flag()
    public val backup: Boolean by option("--backup", help = "Create backup before modifying").flag()

    private val opts by MetadataOptions()

    override fun run() {
        val runner = MetadataRunner(this, opts)
        runner.readAndHandle {
            val deprecated = metadata.libraries.filter { it.deprecation != null }

            if (deprecated.isEmpty()) {
                formatter.info("No deprecated libraries found")
                return@readAndHandle
            }

            val headers = listOf("Alias", "Replaced By", "Message")
            val rows = deprecated.map { lib ->
                listOf(
                    lib.alias,
                    lib.deprecation?.replacedBy ?: "-",
                    lib.deprecation?.message ?: "-",
                )
            }

            if (dryRun) {
                formatter.info("Migration plan (dry run):")
                formatter.renderTable(headers, rows)
                return@readAndHandle
            }

            formatter.renderTable(headers, rows)

            if (backup) {
                val backupPath = Path.of("${opts.input}.bak")
                write(metadata, backupPath)
                formatter.info("Backup saved to '$backupPath'")
            }

            val deprecatedAliases = deprecated.map { it.alias }.toSet()
            val replacementMap = deprecated
                .mapNotNull { lib -> lib.deprecation?.replacedBy?.let { lib.alias to it } }
                .toMap()

            val updatedBundles = metadata.bundles.map { bundle ->
                val updatedLibraries = bundle.libraries.flatMap { libAlias ->
                    when {
                        removeFromBundles && libAlias in deprecatedAliases -> emptyList()
                        replace && libAlias in replacementMap              -> listOf(replacementMap.getValue(libAlias))
                        else                                               -> listOf(libAlias)
                    }
                }
                bundle.copy(libraries = updatedLibraries)
            }

            val updatedLibraries = if (remove) {
                metadata.libraries.filter { it.deprecation == null }
            } else {
                metadata.libraries
            }

            val updated = metadata.copy(
                libraries = updatedLibraries,
                bundles = updatedBundles,
            )

            write(updated)

            val removedCount = if (remove) deprecated.size else 0
            val replacedCount = replacementMap.size
            formatter.success("Migration complete: $replacedCount replaced, $removedCount removed")
        }
    }
}
