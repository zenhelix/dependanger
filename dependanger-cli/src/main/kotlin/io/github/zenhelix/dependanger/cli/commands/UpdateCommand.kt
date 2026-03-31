package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

public class UpdateCommand : CliktCommand(name = "update") {
    override fun help(context: Context): String = "Update entities in metadata"
    override fun run(): Unit = Unit
}
