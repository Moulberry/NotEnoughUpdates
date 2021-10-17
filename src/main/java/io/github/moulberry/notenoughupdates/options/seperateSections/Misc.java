package io.github.moulberry.notenoughupdates.options.seperateSections;

import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.core.config.annotations.*;

public class Misc {
    @Expose
    @ConfigOption(
            name = "Only Show on Skyblock",
            desc = "The item list and some other GUI elements will only show on skyblock"
    )
    @ConfigEditorBoolean
    public boolean onlyShowOnSkyblock = true;

    @Expose
    @ConfigOption(
            name = "Hide Potion Effects",
            desc = "Hide the potion effects inside your inventory while on skyblock"
    )
    @ConfigEditorBoolean
    public boolean hidePotionEffect = true;

    @Expose
    @ConfigOption(
            name = "Streamer Mode",
            desc = "Randomize lobby names in the scoreboard and chat messages to help prevent stream sniping"
    )
    @ConfigEditorBoolean
    public boolean streamerMode = false;

    @Expose
    @ConfigOption(
            name = "GUI Click Sounds",
            desc = "Play click sounds in various NEU-related GUIs when pressing buttons"
    )
    @ConfigEditorBoolean
    public boolean guiButtonClicks = true;

    @Expose
    @ConfigOption(
            name = "Replace Chat Social Options",
            desc = "Replace Hypixel's chat social options with NEU's profile viewer."
    )
    @ConfigEditorBoolean
    public boolean replaceSocialOptions = true;

    @Expose
    @ConfigOption(
            name = "Damage Indicator Style",
            desc = "Change the style of Skyblock damage indicators to be easier to read\n" +
                    "\u00A7cSome old animations mods breaks this feature"
    )
    @ConfigEditorDropdown(
            values = {"Off", "Commas", "Shortened"}
    )
    public int damageIndicatorStyle = 1;

    @Expose
    @ConfigOption(
            name = "Profile Viewer",
            desc = "Brings up the profile viewer (/pv)\n" +
                    "Shows stats and networth of players"
    )
    @ConfigEditorButton(runnableId = 13, buttonText = "Open")
    public boolean openPV = true;

    @Expose
    @ConfigOption(
            name = "Edit Enchant Colours",
            desc = "Change the colours of certain skyblock enchants (/neuec)"
    )
    @ConfigEditorButton(runnableId = 8, buttonText = "Open")
    public boolean editEnchantColoursButton = true;

    @Expose
    @ConfigOption(
            name = "Chroma Text Speed",
            desc = "Change the speed of chroma text for items names (/neucustomize) and enchant colours (/neuec) with the chroma colour code (&z)"
    )
    @ConfigEditorSlider(
            minValue = 10,
            maxValue = 500,
            minStep = 10
    )
    public int chromaSpeed = 100;

    @Expose
    @ConfigOption(
            name = "Disable Skull retexturing",
            desc = "Disables the skull retexturing."
    )
    @ConfigEditorBoolean
    public boolean disableSkullRetexturing = false;

}