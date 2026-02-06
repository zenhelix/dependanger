package io.github.zenhelix.dependanger.effective.builder

public sealed class DistributionSelection {
    public data object Default : DistributionSelection()
    public data class Named(val name: String) : DistributionSelection()
}
