package io.github.zenhelix.dependanger.features.license

import io.github.zenhelix.dependanger.effective.spi.StrictOnlyPresetContributor

public class LicenseCheckPresetContributor : StrictOnlyPresetContributor(LicenseCheckProcessor.PROCESSOR_ID)
