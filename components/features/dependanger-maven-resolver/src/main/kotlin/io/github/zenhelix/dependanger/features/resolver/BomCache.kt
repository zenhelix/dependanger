package io.github.zenhelix.dependanger.features.resolver

public class BomCache(
    public val cacheDirectory: String,
    public val ttlHours: Long = 24,
    public val ttlSnapshotHours: Long = 1,
) {
    public fun get(group: String, artifact: String, version: String): BomContent = TODO()
    public fun put(group: String, artifact: String, version: String, content: BomContent): Unit = TODO()
    public fun invalidate(group: String, artifact: String, version: String): Unit = TODO()
    public fun clear(): Unit = TODO()
}
