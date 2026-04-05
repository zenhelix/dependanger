package io.github.zenhelix.dependanger.clearlydefined.client.model

/**
 * Result of a ClearlyDefined API query.
 * Returns raw declared SPDX expression without parsing.
 */
public sealed interface ClearlyDefinedResult {

    /** License expression successfully retrieved from ClearlyDefined. */
    public data class Found(val declaredExpression: String) : ClearlyDefinedResult

    /** No license information found for this artifact. */
    public data object NotFound : ClearlyDefinedResult

    /** Request failed due to network or API error. */
    public data class Failed(val error: String) : ClearlyDefinedResult
}
