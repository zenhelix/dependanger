package io.github.zenhelix.dependanger.effective.model

import io.github.zenhelix.dependanger.core.model.DiagnosticMessage
import io.github.zenhelix.dependanger.core.model.Diagnostics

@Deprecated("Use io.github.zenhelix.dependanger.core.model.Diagnostics", ReplaceWith("Diagnostics", "io.github.zenhelix.dependanger.core.model.Diagnostics"))
public typealias ProcessingDiagnostics = Diagnostics

@Deprecated(
    "Use io.github.zenhelix.dependanger.core.model.DiagnosticMessage",
    ReplaceWith("DiagnosticMessage", "io.github.zenhelix.dependanger.core.model.DiagnosticMessage")
)
public typealias ProcessingDiagnosticMessage = DiagnosticMessage
