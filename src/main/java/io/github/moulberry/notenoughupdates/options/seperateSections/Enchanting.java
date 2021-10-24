package io.github.moulberry.notenoughupdates.options.seperateSections;

import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.core.config.annotations.*;

public class Enchanting {
    @ConfigOption(
            name = "Enchant Table GUI",
            desc = ""
    )
    @ConfigEditorAccordion(id = 1)
    public boolean tableGUIAccordion = false;

    @Expose
    @ConfigOption(
            name = "Enable Enchant Table GUI",
            desc = "Show a custom GUI when using the Enchant Table"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean enableTableGUI = true;

    @Expose
    @ConfigOption(
            name = "Incompatible Enchants",
            desc = "How to display enchants that are incompatible with your current item, eg. Smite on a sword with Sharpness"
    )
    @ConfigEditorDropdown(
            values = {"Highlight", "Hide"}
    )
    @ConfigAccordionId(id = 1)
    public int incompatibleEnchants = 0;

    @Expose
    @ConfigOption(
            name = "Enchant Sorting",
            desc = "Change the method of sorting enchants in the GUI"
    )
    @ConfigEditorDropdown(
            values = {"By Cost", "Alphabetical"}
    )
    @ConfigAccordionId(id = 1)
    public int enchantSorting = 0;

    @Expose
    @ConfigOption(
            name = "Enchant Ordering",
            desc = "Change the method of ordering used by the sort"
    )
    @ConfigEditorDropdown(
            values = {"Ascending", "Descending"}
    )
    @ConfigAccordionId(id = 1)
    public int enchantOrdering = 0;


    @ConfigOption(
            name = "Enchanting Solvers",
            desc = ""
    )
    @ConfigEditorAccordion(id = 0)
    public boolean enchantingSolversAccordion = false;


    @Expose
    @ConfigOption(
            name = "Enable Solvers",
            desc = "Turn on solvers for the experimentation table"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean enableEnchantingSolvers = true;

    /*@Expose
        @ConfigOption(
                name = "Prevent Misclicks",
                desc = "Prevent accidentally failing the Chronomatron and Ultrasequencer experiments"
        )
        @ConfigEditorBoolean
        @ConfigAccordionId(id = 0)
        public boolean preventMisclicks = true;*/
	
    @Expose
    @ConfigOption(
            name = "Hide Tooltips",
            desc = "Hide the tooltip of items in the Chronomatron and Ultrasequencer experiments"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean hideTooltips = true;

    @Expose
    @ConfigOption(
            name = "Ultrasequencer Numbers",
            desc = "Replace the items in the supersequencer with only numbers"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
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
    @ConfigAccordionId(id = 0)
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
    @ConfigAccordionId(id = 0)
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
    @ConfigAccordionId(id = 0)
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
    @ConfigAccordionId(id = 0)
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
    @ConfigAccordionId(id = 0)
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
    @ConfigAccordionId(id = 0)
    public int supPower = 11;
}
