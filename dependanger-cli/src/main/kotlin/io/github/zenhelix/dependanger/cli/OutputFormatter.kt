package io.github.zenhelix.dependanger.cli

public class OutputFormatter(
    public val useColor: Boolean = true,
    public val jsonOutput: Boolean = false,
) {
    public fun formatSuccess(message: String): String = TODO()
    public fun formatError(message: String): String = TODO()
    public fun formatWarning(message: String): String = TODO()
    public fun formatInfo(message: String): String = TODO()
    public fun formatTable(headers: List<String>, rows: List<List<String>>): String = TODO()
}
