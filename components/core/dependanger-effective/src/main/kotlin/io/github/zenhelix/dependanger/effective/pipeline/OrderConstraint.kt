package io.github.zenhelix.dependanger.effective.pipeline

public sealed class OrderConstraint {
    public data class RunsAfter(val processorId: String) : OrderConstraint()
    public data class RunsBefore(val processorId: String) : OrderConstraint()

    public companion object {
        public fun runsAfter(processorId: String): OrderConstraint = RunsAfter(processorId)
        public fun runsBefore(processorId: String): OrderConstraint = RunsBefore(processorId)
    }
}
