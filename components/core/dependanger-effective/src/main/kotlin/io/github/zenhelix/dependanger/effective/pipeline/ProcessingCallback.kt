package io.github.zenhelix.dependanger.effective.pipeline

public interface ProcessingCallback {
    public fun onPhaseStarted(phase: ProcessingPhase) {}
    public fun onPhaseCompleted(phase: ProcessingPhase) {}
    public fun onProgress(message: String) {}
    public fun onWarning(message: String) {}
    public fun onError(message: String) {}
}
