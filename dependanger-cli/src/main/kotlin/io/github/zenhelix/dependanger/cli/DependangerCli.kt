package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

public class DependangerCli : CliktCommand(name = "dependanger") {
    override fun help(context: Context): String = "Dependanger - Centralized dependency management tool"

    override fun run(): Unit = Unit
}

public fun main(args: Array<String>) {
    DependangerCli()
        .subcommands(
            InitCommand(),
            AddCommand().subcommands(
                AddVersionCommand(),
                AddLibraryCommand(),
                AddPluginCommand(),
                AddBundleCommand(),
                AddBomCommand(),
                AddDistributionCommand(),
            ),
            RemoveCommand().subcommands(
                RemoveVersionCommand(),
                RemoveLibraryCommand(),
                RemovePluginCommand(),
                RemoveBundleCommand(),
                RemoveBomCommand(),
                RemoveDistributionCommand(),
            ),
            UpdateCommand().subcommands(
                UpdateVersionCommand(),
                UpdateLibraryCommand(),
            ),
            MigrateDeprecatedCommand(),
            ValidateCommand(),
            ProcessCommand(),
            GenerateCommand(),
            CheckCommand().subcommands(
                CheckUpdatesCommand(),
                SecurityCheckCommand(),
                LicenseCheckCommand(),
            ),
            AnalyzeCommand(),
            ResolveTransitivesCommand(),
            ReportCommand(),
        )
        .main(args)
}
