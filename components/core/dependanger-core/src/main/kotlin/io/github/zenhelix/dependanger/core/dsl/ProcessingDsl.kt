package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.ProcessingPreset

@DependangerDslMarker
public class ProcessingDsl {
    public var preset: ProcessingPreset = ProcessingPreset.DEFAULT
    public var disabledProcessors: List<String> = emptyList()

    public fun disableProcessor(id: String) {
        disabledProcessors = disabledProcessors + id
    }
}
