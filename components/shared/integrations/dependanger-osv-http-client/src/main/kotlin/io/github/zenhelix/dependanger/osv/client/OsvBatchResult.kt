package io.github.zenhelix.dependanger.osv.client

public sealed interface OsvBatchResult {
    public data class Success(val vulnerabilities: List<List<OsvVulnerabilityData>>) : OsvBatchResult
    public data class PartialSuccess(
        val vulnerabilities: List<List<OsvVulnerabilityData>>,
        val failedPackageCount: Int,
        val error: String,
        val isTimeout: Boolean,
    ) : OsvBatchResult

    public data class Timeout(val error: String) : OsvBatchResult
    public data class Failed(val error: String) : OsvBatchResult
}
