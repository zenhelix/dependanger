package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

public class CheckCommand : CliktCommand(name = "check") {
    override fun help(context: Context): String = "Run checks on dependencies"
    override fun run(): Unit = Unit
}
