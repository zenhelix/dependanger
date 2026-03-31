package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

public class RemoveCommand : CliktCommand(name = "remove") {
    override fun help(context: Context): String = "Remove entities from metadata"
    override fun run(): Unit = Unit
}
