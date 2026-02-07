package io.github.zenhelix.dependanger.effective.pipeline

public fun interface ProcessingCallback {
    public fun onEvent(event: ProcessingEvent)
}
