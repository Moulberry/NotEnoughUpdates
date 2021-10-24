package io.github.moulberry.notenoughupdates.options.seperateSections;

import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.core.config.annotations.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MiscOverlays {
    @ConfigOption(
            name = "Todo Overlay",
            desc = ""
    )
    @ConfigEditorAccordion(id = 0)
    public boolean todoAccordion = true;

    @Expose
    @ConfigOption(
            name = "Enable Todo Overlay",
            desc = "Show an overlay that reminds you to do important tasks"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean todoOverlay2 = false;

    @Expose
    @ConfigOption(
            name = "Todo Text",
            desc = "\u00a7eDrag text to change the appearance of the overlay\n" +
                    "\u00a7rIf you want to see the time until something is available, click \"Add\" and then the respective timer"
    )
    @ConfigEditorDraggableList(
            exampleText = {
                    "\u00a73Cakes: \u00a7e1d21h",
                    "\u00a73Cookie Buff: \u00a7e2d23h",
                    "\u00a73Godpot: \u00a7e19h",
                    "\u00a73Puzzler: \u00a7e13h",
                    "\u00a73Fetchur: \u00a7e3h38m",
                    "\u00a73Commissions: \u00a7e3h38m",
                    "\u00a73Experiments: \u00a7e3h38m"}
    )
    @ConfigAccordionId(id = 0)
    public List<Integer> todoText2 = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6));

    @ConfigOption(
            name = "Show Only If Soon",
            desc = ""
    )
    @ConfigAccordionId(id = 0)
    @ConfigEditorAccordion(id = 1)
    public boolean TodoAccordion = false;

    @Expose
    @ConfigOption(
            name = "Experimentation Display",
            desc = "Change the way the experimentation timer displays\n" +
                    "Only when ready, When very Soon, When soon, When kinda soon or always."
    )
    @ConfigAccordionId(id =1)
    @ConfigEditorDropdown(
            values = {"Only when ready", "When very Soon", "When soon", "When Kinda Soon", "Always"}
    )
    public int experimentationDisplay = 0;


    @Expose
    @ConfigOption(
            name = "Puzzler Reset Display",
            desc = "Change the way the puzzler reset timer displays\n" +
                    "Only when ready, When very Soon, When soon, When kinda soon or always."
    )
    @ConfigAccordionId(id =1)
    @ConfigEditorDropdown(
            values = {"Only when ready", "When very Soon", "When soon", "When Kinda Soon", "Always"}
    )
    public int puzzlerDisplay = 0;

    @Expose
    @ConfigOption(
            name = "Fetchur Reset Display",
            desc = "Change the way the fetchur reset timer displays\n" +
                    "Only when ready, When very Soon, When soon, When kinda soon or always."
    )
    @ConfigAccordionId(id =1)
    @ConfigEditorDropdown(
            values = {"Only when ready", "When very Soon", "When soon", "When Kinda Soon", "Always"}
    )
    public int fetchurDisplay = 0;

    @Expose
    @ConfigOption(
            name = "Commission timer Display",
            desc = "Change the way the Commission timer displays\n" +
                    "Only when ready, When very Soon, When soon, When kinda soon or always."
    )
    @ConfigAccordionId(id =1)
    @ConfigEditorDropdown(
            values = {"Only when ready", "When very Soon", "When soon", "When Kinda Soon", "Always"}
    )
    public int commissionDisplay = 0;

    @Expose
    @ConfigOption(
            name = "Cake Buff Display",
            desc = "Change the way the cake buff timer displays\n" +
                    "Only when ready, When very Soon, When soon, When kinda soon or always."
    )
    @ConfigAccordionId(id =1)
    @ConfigEditorDropdown(
            values = {"Only when ready", "When very Soon", "When soon", "When Kinda Soon", "Always"}
    )
    public int cakesDisplay = 0;

    @Expose
    @ConfigOption(
            name = "Cookie Buff Display",
            desc = "Change the way the cookie buff displays\n" +
                    "Only when ready, When very Soon, When soon, When kinda soon or always."
    )
    @ConfigAccordionId(id =1)
    @ConfigEditorDropdown(
            values = {"Only when ready", "When very Soon", "When soon", "When Kinda Soon", "Always"}
    )
    public int cookieBuffDisplay = 0;

    @Expose
    @ConfigOption(
            name = "God Pot Display",
            desc = "Change the way the god pot displays\n" +
                    "Only when ready, When very Soon, When soon, When kinda soon or always."
    )
    @ConfigAccordionId(id =1)
    @ConfigEditorDropdown(
            values = {"Only when ready", "When very Soon", "When soon", "When Kinda Soon", "Always"}
    )
    public int godpotDisplay = 0;

    @ConfigOption(
            name = "Colours",
            desc = ""
    )

    @ConfigEditorAccordion(id = 2)
    @ConfigAccordionId(id = 0)
    public boolean TodoColourAccordion = false;

    @Expose
    @ConfigOption(
            name = "Ready colour",
            desc = "Change the colour of when the timer is ready"

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 2)
    public int readyColour = 10;

    @Expose
    @ConfigOption(
            name = "Gone colour",
            desc = "Change the colour of when the timer is gone"

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 2)
    public int goneColour = 12;

    @Expose
    @ConfigOption(
            name = "Very soon colour",
            desc = "Change the colour of when the timer is almost ready/gone"

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 2)
    public int verySoonColour = 11;

    @Expose
    @ConfigOption(
            name = "Soon Colour",
            desc = "Change the colour of when the timer is soon ready/gone"

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 2)
    public int soonColour = 9;

    @Expose
    @ConfigOption(
            name = "Kinda Soon Colour",
            desc = "Change the colour of when the timer is kinda soon ready/gone"

    )
    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 2)
    public int kindaSoonColour = 1;


    @Expose
    @ConfigOption(
            name = "Default Colour",
            desc = "Change the default colour of the timers"

    )

    @ConfigEditorDropdown(
            values = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
    )
    @ConfigAccordionId(id = 2)
    public int defaultColour = 15;


    @Expose
    @ConfigOption(
            name = "Todo Position",
            desc = "Change the position of the Todo overlay"
    )
    @ConfigEditorButton(
            runnableId = 5,
            buttonText = "Edit"
    )
    @ConfigAccordionId(id = 0)
    public Position todoPosition = new Position(100, 0);


    @Expose
    @ConfigOption(
            name = "Todo Style",
            desc = "Change the style of the todo overlay"
    )
    @ConfigEditorDropdown(
            values = {"Background", "No Shadow", "Shadow", "Full Shadow"}
    )
    @ConfigAccordionId(id = 0)
    public int todoStyle = 0;

    @Expose
    @ConfigOption(
            name = "Todo Icons",
            desc = "Add little item icons next to the lines in the todo overlay"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean todoIcons = true;
}
