package io.github.zenhelix.dependanger.core.dsl

@DependangerDslMarker
public class ProcessingDsl {
    public var preset: String = "DEFAULT"
    public var disabledProcessors: List<String> = emptyList()

    public fun disableProcessor(id: String) {
        disabledProcessors = disabledProcessors + id
    }
}
