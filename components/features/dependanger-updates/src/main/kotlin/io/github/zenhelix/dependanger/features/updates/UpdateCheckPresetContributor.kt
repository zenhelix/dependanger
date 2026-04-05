package io.github.zenhelix.dependanger.features.updates

import io.github.zenhelix.dependanger.effective.spi.StrictOnlyPresetContributor

public class UpdateCheckPresetContributor : StrictOnlyPresetContributor(UpdateCheckProcessor.PROCESSOR_ID)
