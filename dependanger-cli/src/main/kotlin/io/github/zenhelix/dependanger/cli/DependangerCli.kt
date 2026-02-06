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
            AddLibraryCommand(),
            RemoveLibraryCommand(),
            UpdateVersionCommand(),
            ValidateCommand(),
            ProcessCommand(),
            GenerateCommand(),
            CheckUpdatesCommand(),
            AnalyzeCommand(),
            SecurityCheckCommand(),
            LicenseCheckCommand(),
            ResolveTransitivesCommand(),
            ReportCommand(),
        )
        .main(args)
}
