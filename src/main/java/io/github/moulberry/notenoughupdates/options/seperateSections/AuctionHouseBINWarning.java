package io.github.moulberry.notenoughupdates.options.seperateSections;

import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigEditorBoolean;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigEditorSlider;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigOption;

public class AuctionHouseBINWarning {
    @Expose
    @ConfigOption(
            name = "Enable BIN Warning",
            desc = "Ask for confirmation when BINing an item for below X% of lowestbin"
    )
    @ConfigEditorBoolean
    public boolean enableBINWarning = true;

    @Expose
    @ConfigOption(
            name = "Warning Threshold",
            desc = "Threshold for BIN warning\nExample: 10% means warn if sell price is 10% lower than lowestbin"
    )
    @ConfigEditorSlider(
            minValue = 0.0f,
            maxValue = 100.0f,
            minStep = 5f
    )
    public float warningThreshold = 10f;

}
