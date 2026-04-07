package io.github.zenhelix.dependanger.effective.model

public const val DEFAULT_DEPRECATION_MESSAGE: String = "DEPRECATED"

public fun EffectiveLibrary.deprecationMessage(): String =
    deprecationSummary ?: DEFAULT_DEPRECATION_MESSAGE
