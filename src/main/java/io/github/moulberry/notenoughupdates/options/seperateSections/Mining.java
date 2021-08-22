package io.github.moulberry.notenoughupdates.options.seperateSections;

import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.core.config.annotations.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Mining {
    @ConfigOption(
            name = "Waypoints",
            desc = ""
    )
    @ConfigEditorAccordion(id = 0)
    public boolean waypointsAccordion = false;

    @Expose
    @ConfigOption(
            name = "Mines Waypoints",
            desc = "Show waypoints in the Dwarven mines to the various locations\n" +
                    "Use \"Commissions Only\" to only show active commission locations"
    )
    @ConfigEditorDropdown(
            values = {"Hide", "Commissions Only", "Always"},
            initialIndex = 1
    )
    @ConfigAccordionId(id = 0)
    public int locWaypoints = 1;

    @Expose
    @ConfigOption(
            name = "Emissary Waypoints",
            desc = "Show waypoints in the Dwarven mines to emissaries\n" +
                    "Use \"Commission End\" to only show after finishing commissions"
    )
    @ConfigEditorDropdown(
            values = {"Hide", "Commission End", "Always"},
            initialIndex = 1
    )
    @ConfigAccordionId(id = 0)
    public int emissaryWaypoints = 1;

    @ConfigOption(
            name = "Drill Fuel Bar",
            desc = ""
    )
    @ConfigEditorAccordion(id = 1)
    public boolean drillAccordion = false;

    @Expose
    @ConfigOption(
            name = "Drill Fuel Bar",
            desc = "Show a fancy drill fuel bar when holding a drill in mining areas"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean drillFuelBar = true;

    @Expose
    @ConfigOption(
            name = "Fuel Bar Width",
            desc = "Change the width of the drill fuel bar"
    )
    @ConfigEditorSlider(
            minValue = 50,
            maxValue = 400,
            minStep = 10
    )
    @ConfigAccordionId(id = 1)
    public int drillFuelBarWidth = 200;

    @Expose
    @ConfigOption(
            name = "Fuel Bar Position",
            desc = "Set the position of the drill fuel bar"
    )
    @ConfigEditorButton(
            runnableId = 2,
            buttonText = "Edit"
    )
    @ConfigAccordionId(id = 1)
    public Position drillFuelBarPosition = new Position(0, -100, true, false);

    @ConfigOption(
            name = "Dwarven Overlay",
            desc = ""
    )
    @ConfigEditorAccordion(id = 2)
    public boolean overlayAccordion = false;

    @Expose
    @ConfigOption(
            name = "Dwarven Overlay",
            desc = "Show an Overlay with useful information on the screen while in Dwarven Mines"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean dwarvenOverlay = true;

    @Expose
    @ConfigOption(
            name = "Dwarven Text",
            desc = "\u00a7eDrag text to change the appearance of the Overlay\n" +
                    "\u00a7rGo to the Dwarven Mines to show this Overlay with useful information"
    )
    @ConfigEditorDraggableList(
            exampleText = {"\u00a73Goblin Slayer: \u00a7626.5%\n\u00a73Lucky Raffle: \u00a7c0.0%",
                    "\u00a73Mithril Powder: \u00a726,243",
                    "\u00a73Gemstone Powder: \u00a7d6,243",
                    "\u00a73Forge 1) \u00a79Diamonite\u00a77: \u00a7aReady!",
                    "\u00a73Forge 2) \u00a77EMPTY\n\u00a73Forge 3) \u00a77EMPTY\n\u00a73Forge 4) \u00a77EMPTY",
                    "\u00a73Pickaxe CD: \u00a7a78s"}
    )
    @ConfigAccordionId(id = 2)
    public List<Integer> dwarvenText = new ArrayList<>(Arrays.asList(0, 1, 4, 2, 3, 5));

    @Expose
    @ConfigOption(
            name = "Overlay Position",
            desc = "Change the position of the Dwarven Mines information Overlay (commisions, powder & forge statuses)"
    )
    @ConfigEditorButton(
            runnableId = 1,
            buttonText = "Edit"
    )
    @ConfigAccordionId(id = 2)
    public Position overlayPosition = new Position(10, 100);

    @Expose
    @ConfigOption(
            name = "Overlay Style",
            desc = "Change the style of the Dwarven Mines information Overlay"
    )
    @ConfigEditorDropdown(
            values = {"Background", "No Shadow", "Shadow", "Full Shadow"}
    )
    @ConfigAccordionId(id = 2)
    public int overlayStyle = 0;

    @ConfigOption(
            name = "Metal Detector Solver",
            desc = ""
    )
    @ConfigEditorAccordion(id = 3)
    public boolean metalDetectorSolverAccordion = false;

    @Expose
    @ConfigOption(
            name = "Enable Waypoints",
            desc = "Enabled the metal detector solver for Mines of Divan, to use this stand still to calculate possible blocks and then if required stand" +
                    " still on another block."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean metalDetectorEnabled = true;

    @Expose
    @ConfigOption(
            name = "Show Possible Blocks",
            desc = "Show waypoints on possible locations when NEU isn't sure about what block the treasure is."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean metalDetectorShowPossible = false;

    @ConfigOption(
            name = "Automaton Parts Overlay",
            desc = ""
    )
    @ConfigEditorAccordion(id = 4)
    public boolean automatonOverlayAccordion = false;

    @Expose
    @ConfigOption(
            name = "Enable Overlay",
            desc = "Enables the Automaton parts Overlay."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean automatonOverlay = false;

    @Expose
    @ConfigOption(
            name = "Overlay Position",
            desc = "Change the position of the Automaton parts Overlay."
    )
    @ConfigEditorButton(
            runnableId = 10,
            buttonText = "Edit"
    )
    @ConfigAccordionId(id = 4)
    public Position automatonOverlayPosition = new Position(200, 0);

    @Expose
    @ConfigOption(
            name = "Style",
            desc = "Change the style of the Automaton parts Overlay."
    )
    @ConfigEditorDropdown(
            values = {"Background", "No Shadow", "Shadow", "Full Shadow"}
    )
    @ConfigAccordionId(id = 4)
    public int automatonOverlayStyle = 0;

    @Expose
    @ConfigOption(
            name = "Show Icons",
            desc = "Show icons in the Overlay that represent the part."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean automatonIcons = true;


    @Expose
    @ConfigOption(
            name = "Hide Done",
            desc = "Don't show parts you've given to Professor Robot."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean automatonHideDone = false;

    @ConfigOption(
            name = "Colours",
            desc = ""
    )
    @ConfigEditorAccordion(id = 5)
    @ConfigAccordionId(id = 4)
    public boolean AutomatonColourAccordion = false;

    @Expose
    @ConfigOption(
            name = "Part colour",
            desc = "Change the colour of the part text."

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 5)
    public int automatonPartColor = 3;

    @Expose
    @ConfigOption(
            name = "Ready colour",
            desc = "Change the colour when the part is given to Professor Robot."

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 5)
    public int automatonDoneColor = 10;

    @Expose
    @ConfigOption(
            name = "In Inventory Color",
            desc = "Change the colour when the part is in the inventory."

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 5)
    public int automatonInventoryColor = 14;

    @Expose
    @ConfigOption(
            name = "In Storage Color",
            desc = "Change the colour when the part is in the storage."

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 5)
    public int automatonStorageColor = 14;

    @Expose
    @ConfigOption(
            name = "Missing Color",
            desc = "Change the colour when the part is missing."

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 5)
    public int automatonMissingColor = 12;

    @ConfigOption(
            name = "Mines of Divan Overlay",
            desc = ""
    )
    @ConfigEditorAccordion(id = 6)
    public boolean divanMinesOverlayAccordion = false;

    @Expose
    @ConfigOption(
            name = "Enable Overlay",
            desc = "Enables the Mines of Divan Overlay."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 6)
    public boolean divanMinesOverlay = false;

    @Expose
    @ConfigOption(
            name = "Overlay Position",
            desc = "Change the position of the Mines of Divan Overlay."
    )
    @ConfigEditorButton(
            runnableId = 11,
            buttonText = "Edit"
    )
    @ConfigAccordionId(id = 6)
    public Position divanMinesOverlayPosition = new Position(200, 0);

    @Expose
    @ConfigOption(
            name = "Style",
            desc = "Change the style of the Mines of Divan Overlay."
    )
    @ConfigEditorDropdown(
            values = {"Background", "No Shadow", "Shadow", "Full Shadow"}
    )
    @ConfigAccordionId(id = 6)
    public int divanMinesOverlayStyle = 0;

    @Expose
    @ConfigOption(
            name = "Show Icons",
            desc = "Show icons in the Overlay that represent the part."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 6)
    public boolean divanMinesIcons = true;


    @Expose
    @ConfigOption(
            name = "Hide Done",
            desc = "Don't show parts you've given to the Keepers."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 6)
    public boolean divanMinesHideDone = false;

    @ConfigOption(
            name = "Colours",
            desc = ""
    )
    @ConfigEditorAccordion(id = 7)
    @ConfigAccordionId(id = 6)
    public boolean divanMinesColourAccordion = false;

    @Expose
    @ConfigOption(
            name = "Part colour",
            desc = "Change the colour of the part text."

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 7)
    public int divanMinesPartColor = 3;

    @Expose
    @ConfigOption(
            name = "Ready colour",
            desc = "Change the colour when the part is given to the Keepers."

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 7)
    public int divanMinesDoneColor = 10;

    @Expose
    @ConfigOption(
            name = "In Inventory Color",
            desc = "Change the colour when the part is in the inventory."

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 7)
    public int divanMinesInventoryColor = 14;

    @Expose
    @ConfigOption(
            name = "In Storage Color",
            desc = "Change the colour when the part is in the storage."

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 7)
    public int divanMinesStorageColor = 14;

    @Expose
    @ConfigOption(
            name = "Missing Color",
            desc = "Change the colour when the part is missing."

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 7)
    public int divanMinesMissingColor = 12;

    @Expose
    @ConfigOption(
            name = "Puzzler Solver",
            desc = "Show the correct block to mine for the puzzler puzzle in Dwarven Mines"
    )
    @ConfigEditorBoolean
    public boolean puzzlerSolver = true;

    @Expose
    @ConfigOption(
            name = "Titanium Alert",
            desc = "Show an alert whenever titanium appears nearby"
    )
    @ConfigEditorBoolean
    public boolean titaniumAlert = true;

    @Expose
    @ConfigOption(
            name = "Titanium must touch air",
            desc = "Only show an alert if the Titanium touches air. (kinda sus)"
    )
    @ConfigEditorBoolean
    public boolean titaniumAlertMustBeVisible = false;


    @Expose
    @ConfigOption(
            name = "Dwarven Mines Textures",
            desc = "Allows texture packs to retexture blocks in the Dwarven Mines. If you don't have a texturepack that does this, you should leave this off"
    )
    @ConfigEditorBoolean
    public boolean dwarvenTextures = false;

        /*@Expose
        @ConfigOption(
                name = "Don't Mine Stone",
                desc = "Prevent mining stone blocks in mining areas"
        )
        @ConfigEditorBoolean
        public boolean dontMineStone = true;

        @Expose
        @ConfigOption(
                name = "Reveal Mist Creepers",
                desc = "Make the creepers in the Dwarven Mines mist visible"
        )
        @ConfigEditorBoolean
        public boolean revealMistCreepers = true;*/
}
