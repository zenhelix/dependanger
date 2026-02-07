package io.github.zenhelix.dependanger.effective.pipeline

import io.github.zenhelix.dependanger.core.model.DiagnosticMessage
import kotlin.time.Duration

public sealed class ProcessingEvent {
    public data class PhaseStarted(val phase: ProcessingPhase) : ProcessingEvent()
    public data class PhaseCompleted(val phase: ProcessingPhase, val duration: Duration) : ProcessingEvent()
    public data class PhaseError(val phase: ProcessingPhase, val error: Throwable) : ProcessingEvent()
    public data class DiagnosticAdded(val message: DiagnosticMessage) : ProcessingEvent()
    public data class Progress(val phase: ProcessingPhase, val current: Int, val total: Int) : ProcessingEvent()
}
