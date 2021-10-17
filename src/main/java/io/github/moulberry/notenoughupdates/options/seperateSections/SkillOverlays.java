package io.github.moulberry.notenoughupdates.options.seperateSections;

import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.core.config.annotations.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkillOverlays {
    @ConfigOption(
            name = "Farming",
            desc = ""
    )
    @ConfigEditorAccordion(id = 0)
    public boolean farmingAccordion = false;
    @Expose
    @ConfigOption(
            name = "Enable Farming Overlay",
            desc = "Show an overlay while farming with useful information"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean farmingOverlay = true;

    @Expose
    @ConfigOption(
            name = "Farming Text",
            desc = "\u00a7eDrag text to change the appearance of the overlay\n" +
                    "\u00a7rHold a mathematical hoe or use an axe with cultivating enchantment while gaining farming xp to show the overlay"
    )
    @ConfigEditorDraggableList(
            exampleText = {"\u00a7bCounter: \u00a7e37,547,860",
                    "\u00a7bCrops/m: \u00a7e38.29",
                    "\u00a7bFarm: \u00a7e12\u00a77 [\u00a7e|||||||||||||||||\u00a78||||||||\u00a77] \u00a7e67%",
                    "\u00a7bCurrent XP: \u00a7e6,734",
                    "\u00a7bRemaining XP: \u00a7e3,265",
                    "\u00a7bXP/h: \u00a7e238,129",
                    "\u00a7bYaw: \u00a7e68.25\u00a7l\u1D52",
                    "\u00a7bETA: \u00a7e13h12m"}
    )
    @ConfigAccordionId(id = 0)
    public List<Integer> farmingText = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 7, 6));

    @Expose
    @ConfigOption(
            name = "Farming Position",
            desc = "Change the position of the Farming overlay"
    )
    @ConfigEditorButton(
            runnableId = 3,
            buttonText = "Edit"
    )
    @ConfigAccordionId(id = 0)
    public Position farmingPosition = new Position(10, 200);

    @Expose
    @ConfigOption(
            name = "Farming Style",
            desc = "Change the style of the Farming overlay"
    )
    @ConfigEditorDropdown(
            values = {"Background", "No Shadow", "Shadow", "Full Shadow"}
    )
    @ConfigAccordionId(id = 0)
    public int farmingStyle = 0;
    @ConfigOption(
            name = "Mining",
            desc = ""
    )
    @ConfigEditorAccordion(id = 1)
    public boolean miningAccordion = false;
    @Expose
    @ConfigOption(
            name = "Enable Mining Overlay",
            desc = "Show an overlay while Mining with useful information"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean miningSkillOverlay = true;

    @Expose
    @ConfigOption(
            name = "Mining Text",
            desc = "\u00a7eDrag text to change the appearance of the overlay\n" +
                    "\u00a7rHold a pickaxe while gaining mining xp to show the overlay"
    )
    @ConfigEditorDraggableList(
            exampleText = {"\u00a7bCompact: \u00a7e547,860",
                    "\u00a7bBlocks/m: \u00a7e38.29",
                    "\u00a7bMine: \u00a7e12\u00a77 [\u00a7e|||||||||||||||||\u00a78||||||||\u00a77] \u00a7e67%",
                    "\u00a7bCurrent XP: \u00a7e6,734",
                    "\u00a7bRemaining XP: \u00a7e3,265",
                    "\u00a7bXP/h: \u00a7e238,129",
                    "\u00a7bYaw: \u00a7e68.25\u00a7l\u1D52",
                    "\u00a7bETA: \u00a7e13h12m"}
    )
    @ConfigAccordionId(id = 1)
    public List<Integer> miningText = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 7));

    @Expose
    @ConfigOption(
            name = "Mining Position",
            desc = "Change the position of the Mining overlay"
    )
    @ConfigEditorButton(
            runnableId = 11,
            buttonText = "Edit"
    )
    @ConfigAccordionId(id = 1)
    public Position miningPosition = new Position(10, 200);

    @Expose
    @ConfigOption(
            name = "Mining Style",
            desc = "Change the style of the Mining overlay"
    )
    @ConfigEditorDropdown(
            values = {"Background", "No Shadow", "Shadow", "Full Shadow"}
    )
    @ConfigAccordionId(id = 1)
    public int miningStyle = 0;
}
