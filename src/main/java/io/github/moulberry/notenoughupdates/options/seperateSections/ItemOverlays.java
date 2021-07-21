package io.github.moulberry.notenoughupdates.options.seperateSections;

import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.core.config.annotations.*;

public class ItemOverlays {
    @ConfigOption(
            name = "Treecapitator Overlay",
            desc = ""
    )
    @ConfigEditorAccordion(id = 0)
    public boolean treecapAccordion = false;

    @Expose
    @ConfigOption(
            name = "Enable Treecap Overlay",
            desc = "Show which blocks will be broken when using a Jungle Axe or Treecapitator"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean enableTreecapOverlay = true;

    @Expose
    @ConfigOption(
            name = "Overlay Colour",
            desc = "Change the colour of the overlay"
    )
    @ConfigEditorColour
    @ConfigAccordionId(id = 0)
    public String treecapOverlayColour = "00:50:64:224:208";

    @Expose
    @ConfigOption(
            name = "Enable Monkey Pet Check",
            desc = "Will check use the API to check what pet you're using\nto determine the cooldown based off of if you have monkey pet."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean enableMonkeyCheck = true;

    @ConfigOption(
            name = "Builder's Wand Overlay",
            desc = ""
    )
    @ConfigEditorAccordion(id = 1)
    public boolean wandAccordion = false;

    @Expose
    @ConfigOption(
            name = "Enable Wand Overlay",
            desc = "Show which blocks will be placed when using the Builder's Wand"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean enableWandOverlay = true;

    @Expose
    @ConfigOption(
            name = "Wand Block Count",
            desc = "Shows the total count of a block in your inventory"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean wandBlockCount = true;

    @Expose
    @ConfigOption(
            name = "Overlay Colour",
            desc = "Change the colour of the ghost block outline"
    )
    @ConfigEditorColour
    @ConfigAccordionId(id = 1)
    public String wandOverlayColour = "00:50:64:224:208";

    @ConfigOption(
            name = "Block Zapper Overlay",
            desc = ""
    )
    @ConfigEditorAccordion(id = 6)
    public boolean zapperAccordion = false;

    @Expose
    @ConfigOption(
            name = "Enable Zapper Overlay",
            desc = "Show which blocks will be destroyed when using the Block Zapper"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 6)
    public boolean enableZapperOverlay = true;

    @Expose
    @ConfigOption(
            name = "Overlay Colour",
            desc = "Change the colour of the ghost block outline"
    )
    @ConfigEditorColour
    @ConfigAccordionId(id = 6)
    public String zapperOverlayColour = "0:102:171:5:0";

    @ConfigOption(
            name = "Smooth AOTE",
            desc = ""
    )
    @ConfigEditorAccordion(id = 2)
    public boolean aoteAccordion = false;

    @Expose
    @ConfigOption(
            name = "Enable Smooth AOTE",
            desc = "Teleport smoothly to your destination when using AOTE"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean enableSmoothAOTE = true;

    @Expose
    @ConfigOption(
            name = "Enable Smooth Hyperion",
            desc = "Teleport smoothly to your destination when using Hyperion"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean enableSmoothHyperion = true;

    @Expose
    @ConfigOption(
            name = "Smooth TP Time",
            desc = "Change the amount of time (milliseconds) taken to teleport"
    )
    @ConfigEditorSlider(
            minValue = 0,
            maxValue = 500,
            minStep = 25
    )
    @ConfigAccordionId(id = 2)
    public int smoothTpMillis = 175;

    @Expose
    @ConfigOption(
            name = "Disable Hyperion Particles",
            desc = "Remove the explosion effect when using a hyperion"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean disableHyperionParticles = true;

    @ConfigOption(
            name = "Bonemerang Overlay",
            desc = ""
    )
    @ConfigEditorAccordion(id = 3)
    public boolean bonemerangAccordion = false;

    @Expose
    @ConfigOption(
            name = "Highlight Targeted Entities",
            desc = "Highlight entities that will be hit by your bonemerang"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean highlightTargeted = true;

    @Expose
    @ConfigOption(
            name = "Break Warning",
            desc = "Show a warning below your crosshair if the bonemerang will break on a block"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean showBreak = true;

    @ConfigOption(
            name = "Minion Crystal Radius Overlay",
            desc = ""
    )
    @ConfigEditorAccordion(id = 5)
    public boolean crystalAccordion = false;

    @Expose
    @ConfigOption(
            name = "Enable Crystal Overlay",
            desc = "Show a block overlay for the effective radius of minion crystals (farming, mining, etc)"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    public boolean enableCrystalOverlay = true;

    @Expose
    @ConfigOption(
            name = "Always Show Crystal Overlay",
            desc = "Show the crystal overlay, even when a minion crystal is not being held"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    public boolean alwaysShowCrystal = false;
}
