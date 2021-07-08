package io.github.moulberry.notenoughupdates.options.seperateSections;

import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigEditorBoolean;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigEditorDropdown;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigOption;

public class EnchSolvers {
    @Expose
    @ConfigOption(
            name = "Enable Solvers",
            desc = "Turn on solvers for the experimentation table"
    )
    @ConfigEditorBoolean
    public boolean enableEnchantingSolvers = true;

    /*@Expose
        @ConfigOption(
                name = "Prevent Misclicks",
                desc = "Prevent accidentally failing the Chronomatron and Ultrasequencer experiments"
        )
        @ConfigEditorBoolean
        public boolean preventMisclicks = true;*/
	
    @Expose
    @ConfigOption(
            name = "Hide Tooltips",
            desc = "Hide the tooltip of items in the Chronomatron and Ultrasequencer experiments"
    )
    @ConfigEditorBoolean
    public boolean hideTooltips = true;

    @Expose
    @ConfigOption(
            name = "Ultrasequencer Numbers",
            desc = "Replace the items in the supersequencer with only numbers"
    )
    @ConfigEditorBoolean
    public boolean seqNumbers = false;

    @Expose
    @ConfigOption(
            name = "Ultrasequencer Next",
            desc = "Set the colour of the glass pane shown behind the element in the ultrasequencer which is next"
    )
    @ConfigEditorDropdown(
            values = {"None", "White", "Orange", "Light Purple", "Light Blue", "Yellow", "Light Green", "Pink",
                    "Gray", "Light Gray", "Cyan", "Dark Purple", "Dark Blue", "Brown", "Dark Green", "Red", "Black"}
    )
    public int seqNext = 6;

    @Expose
    @ConfigOption(
            name = "Ultrasequencer Upcoming",
            desc = "Set the colour of the glass pane shown behind the element in the ultrasequencer which is coming after \"next\""
    )
    @ConfigEditorDropdown(
            values = {"None", "White", "Orange", "Light Purple", "Light Blue", "Yellow", "Light Green", "Pink",
                    "Gray", "Light Gray", "Cyan", "Dark Purple", "Dark Blue", "Brown", "Dark Green", "Red", "Black"}
    )
    public int seqUpcoming = 5;

    @Expose
    @ConfigOption(
            name = "Superpairs Matched",
            desc = "Set the colour of the glass pane shown behind successfully matched pairs"
    )
    @ConfigEditorDropdown(
            values = {"None", "White", "Orange", "Light Purple", "Light Blue", "Yellow", "Light Green", "Pink",
                    "Gray", "Light Gray", "Cyan", "Dark Purple", "Dark Blue", "Brown", "Dark Green", "Red", "Black"}
    )
    public int supMatched = 6;

    @Expose
    @ConfigOption(
            name = "Superpairs Possible",
            desc = "Set the colour of the glass pane shown behind pairs which can be matched, but have not yet"
    )
    @ConfigEditorDropdown(
            values = {"None", "White", "Orange", "Light Purple", "Light Blue", "Yellow", "Light Green", "Pink",
                    "Gray", "Light Gray", "Cyan", "Dark Purple", "Dark Blue", "Brown", "Dark Green", "Red", "Black"}
    )
    public int supPossible = 2;

    @Expose
    @ConfigOption(
            name = "Superpairs Unmatched",
            desc = "Set the colour of the glass pane shown behind pairs which have been previously uncovered"
    )
    @ConfigEditorDropdown(
            values = {"None", "White", "Orange", "Light Purple", "Light Blue", "Yellow", "Light Green", "Pink",
                    "Gray", "Light Gray", "Cyan", "Dark Purple", "Dark Blue", "Brown", "Dark Green", "Red", "Black"}
    )
    public int supUnmatched = 5;

    @Expose
    @ConfigOption(
            name = "Superpairs Powerups",
            desc = "Set the colour of the glass pane shown behind powerups"
    )
    @ConfigEditorDropdown(
            values = {"None", "White", "Orange", "Light Purple", "Light Blue", "Yellow", "Light Green", "Pink",
                    "Gray", "Light Gray", "Cyan", "Dark Purple", "Dark Blue", "Brown", "Dark Green", "Red", "Black"}
    )
    public int supPower = 11;
}
