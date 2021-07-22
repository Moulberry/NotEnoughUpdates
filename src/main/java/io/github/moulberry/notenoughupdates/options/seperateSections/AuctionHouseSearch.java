package io.github.moulberry.notenoughupdates.options.seperateSections;

import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigEditorBoolean;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigOption;

public class AuctionHouseSearch {
    @Expose
    @ConfigOption(
            name = "Enable Search GUI",
            desc = "Use the advanced search GUI with autocomplete and history instead of the normal sign GUI\n\u00a7eStar Selection Texture: Johnny#4567"
    )
    @ConfigEditorBoolean
    public boolean enableSearchOverlay = true;

    @Expose
    @ConfigOption(
            name = "Keep Previous Search",
            desc = "Don't clear the search bar after closing the GUI"
    )
    @ConfigEditorBoolean
    public boolean keepPreviousSearch = false;

    @Expose
    @ConfigOption(
            name = "Past Searches",
            desc = "Show past searches below the autocomplete box"
    )
    @ConfigEditorBoolean
    public boolean showPastSearches = true;

    @Expose
    @ConfigOption(
            name = "ESC to Full Close",
            desc = "Make pressing ESCAPE close the search GUI without opening up the AH again\n" +
                    "ENTER can still be used to search"
    )
    @ConfigEditorBoolean
    public boolean escFullClose = true;
}
