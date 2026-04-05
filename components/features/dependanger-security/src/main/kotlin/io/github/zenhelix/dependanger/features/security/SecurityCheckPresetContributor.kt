package io.github.zenhelix.dependanger.features.security

import io.github.zenhelix.dependanger.effective.spi.StrictOnlyPresetContributor

public class SecurityCheckPresetContributor : StrictOnlyPresetContributor(SecurityCheckProcessor.PROCESSOR_ID)
