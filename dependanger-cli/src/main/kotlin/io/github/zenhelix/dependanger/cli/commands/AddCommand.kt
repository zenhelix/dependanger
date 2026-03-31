package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

public class AddCommand : CliktCommand(name = "add") {
    override fun help(context: Context): String = "Add entities to metadata"
    override fun run(): Unit = Unit
}
