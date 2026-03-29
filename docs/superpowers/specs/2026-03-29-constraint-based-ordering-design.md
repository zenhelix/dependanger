# Constraint-Based Processor Ordering

## Goal

Replace integer-based processor ordering with declarative constraint-based ordering. Processors declare semantic dependencies (`runsAfter`, `runsBefore`) and the pipeline resolves execution order via topological sort.

## Models

### OrderConstraint (sealed class)
- `RunsAfter(processorId: String)` — this processor must execute after the referenced one
- `RunsBefore(processorId: String)` — this processor must execute before the referenced one

### ProcessingPhase changes
- Remove `order: Int` field
- Keep `name: String` and `executionMode: ExecutionMode`

### EffectiveMetadataProcessor changes
- Remove `val order: Int`
- Add `val constraints: Set<OrderConstraint>` (default: `emptySet()`)

### ParallelMetadataProcessor
- Inherits `constraints` from EffectiveMetadataProcessor, no changes

## Pipeline Resolution (PipelineBuilder)

1. Collect active processors (after enable/disable filtering)
2. Build DAG from constraints (RunsAfter → edge from dependency to processor, RunsBefore → edge from processor to target)
3. Topological sort via Kahn's algorithm (deterministic: break ties by processor id alphabetically)
4. Processors with no constraints AND no incoming edges → append at end (sorted by id)
5. Exception: `ProfileProcessor` (id="profile") is always first when it has no constraints — convention for the bootstrap processor
6. Cycle detection → `PipelineConfigurationException("Circular dependency detected: A → B → C → A")`
7. Unknown processor id in constraint → `PipelineConfigurationException("Processor 'X' referenced in constraint of 'Y' is not registered")`
8. Group sorted processors by consecutive execution mode (same as current logic)

## Built-in Processor Constraints

| Processor | id | constraints |
|---|---|---|
| ProfileProcessor | profile | `emptySet()` |
| MetadataConversionProcessor | metadata-conversion | `runsAfter(profile)` |
| ExtractedVersionsProcessor | extracted-versions | `runsAfter(metadata-conversion)` |
| BomImportProcessor | bom-import | `runsAfter(metadata-conversion)` |
| LibraryFilterProcessor | library-filter | `runsAfter(metadata-conversion)` |
| VersionFallbackProcessor | version-fallback | `runsAfter(library-filter)` |
| VersionResolverProcessor | version-resolver | `runsAfter(version-fallback), runsAfter(extracted-versions), runsAfter(bom-import)` |
| BundleFilterProcessor | bundle-filter | `runsAfter(library-filter)` |
| PluginFilterProcessor | plugin-filter | `runsAfter(metadata-conversion)` |
| PluginProcessor | plugin | `runsAfter(plugin-filter)` |
| UsedVersionsProcessor | used-versions | `runsAfter(version-resolver), runsAfter(plugin)` |
| ValidationProcessor | validation | `runsAfter(version-resolver)` |
| CompatRulesProcessor | compat-rules | `runsAfter(validation)` |
| UpdateCheckProcessor | update-check | `runsAfter(version-resolver)` |
| SecurityCheckProcessor | security-check | `runsAfter(version-resolver)` |
| LicenseCheckProcessor | license-check | `runsAfter(version-resolver)` |
| TransitiveResolverProcessor | transitive-resolver | `runsAfter(version-resolver)` |
| CompatibilityCheckProcessor | compatibility-analysis | `runsAfter(version-resolver)` |

## Third-party Example

```kotlin
class MyProcessor : EffectiveMetadataProcessor {
    override val id = "my-custom"
    override val phase = ProcessingPhase("MY_CUSTOM", ExecutionMode.SEQUENTIAL)
    override val constraints = setOf(
        OrderConstraint.RunsAfter(ProcessorIds.VERSION_RESOLVER),
        OrderConstraint.RunsBefore(ProcessorIds.VALIDATION),
    )
}
```

## No Constraints Default

Processors with `constraints = emptySet()` and no incoming edges from other processors are placed at the end of the pipeline (after all constrained processors), sorted alphabetically by id. Exception: `profile` processor is always first.
