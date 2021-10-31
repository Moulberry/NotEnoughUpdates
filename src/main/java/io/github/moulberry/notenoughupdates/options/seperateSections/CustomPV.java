package io.github.moulberry.notenoughupdates.options.seperateSections;

import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigAccordionId;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigEditorAccordion;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigEditorText;
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigOption;

public class CustomPV {
    @ConfigOption(
            name = "First Box",
            desc = ""
    )
    @ConfigEditorAccordion(id = 0)
    public boolean firstBoxAccordion = false;

    @Expose
    @ConfigOption(
            name = "First Api Path",
            desc = ""
    )
    @ConfigEditorText
    @ConfigAccordionId(id = 0)
    public String pathPV1 = "essence_wither";

    @Expose
    @ConfigOption(
            name = "First Stat Name",
            desc = ""
    )
    @ConfigEditorText
    @ConfigAccordionId(id = 0)
    public String namePV1 = "Wither Essence";

    @Expose
    @ConfigOption(
            name = "Second Api Path",
            desc = ""
    )
    @ConfigEditorText
    @ConfigAccordionId(id = 0)
    public String pathPV2 = "essence_undead";

    @Expose
    @ConfigOption(
            name = "Second Stat Name",
            desc = ""
    )
    @ConfigEditorText
    @ConfigAccordionId(id = 0)
    public String namePV2 = "Undead Essence";

    @Expose
    @ConfigOption(
            name = "Third Api Path",
            desc = ""
    )
    @ConfigEditorText
    @ConfigAccordionId(id = 0)
    public String pathPV3 = "essence_diamond";

    @Expose
    @ConfigOption(
            name = "Third Stat Name",
            desc = ""
    )
    @ConfigEditorText
    @ConfigAccordionId(id = 0)
    public String namePV3 = "Diamond Essence";

    @Expose
    @ConfigOption(
            name = "Fourth Api Path",
            desc = ""
    )
    @ConfigEditorText
    @ConfigAccordionId(id = 0)
    public String pathPV4 = "essence_dragon";

    @Expose
    @ConfigOption(
            name = "Fourth Stat Name",
            desc = ""
    )
    @ConfigEditorText
    @ConfigAccordionId(id = 0)
    public String namePV4 = "Dragon Essence";

    @Expose
    @ConfigOption(
            name = "Fifth Api Path",
            desc = ""
    )
    @ConfigEditorText
    @ConfigAccordionId(id = 0)
    public String pathPV5 = "essence_spider";

    @Expose
    @ConfigOption(
            name = "Fifth Stat Name",
            desc = ""
    )
    @ConfigEditorText
    @ConfigAccordionId(id = 0)
    public String namePV5 = "Spider Essence";

    @Expose
    @ConfigOption(
            name = "Sixth Api Path",
            desc = ""
    )
    @ConfigEditorText
    @ConfigAccordionId(id = 0)
    public String pathPV6 = "essence_gold";

    @Expose
    @ConfigOption(
            name = "Sixth Stat Name",
            desc = ""
    )
    @ConfigEditorText
    @ConfigAccordionId(id = 0)
    public String namePV6 = "Gold Essence";

    @Expose
    @ConfigOption(
            name = "Seventh Api Path",
            desc = ""
    )
    @ConfigEditorText
    @ConfigAccordionId(id = 0)
    public String pathPV7 = "essence_ice";

    @Expose
    @ConfigOption(
            name = "Seventh Stat Name",
            desc = ""
    )
    @ConfigEditorText
    @ConfigAccordionId(id = 0)
    public String namePV7 = "Ice Essence";
}