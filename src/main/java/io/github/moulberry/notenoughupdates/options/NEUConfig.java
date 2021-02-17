package io.github.moulberry.notenoughupdates.options;

import com.google.common.collect.Lists;
import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.GuiScreenElementWrapper;
import io.github.moulberry.notenoughupdates.core.config.Config;
import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.core.config.annotations.*;
import io.github.moulberry.notenoughupdates.core.config.gui.GuiPositionEditor;
import io.github.moulberry.notenoughupdates.overlays.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NEUConfig extends Config {

    private void editOverlay(String activeConfig, TextOverlay overlay, Position position) {
        Vector2f size = overlay.getDummySize();
        int width = (int)size.x;
        int height = (int)size.y;
        Minecraft.getMinecraft().displayGuiScreen(new GuiPositionEditor(position, width, height, () -> {
            overlay.renderDummy();
            OverlayManager.dontRenderOverlay = overlay.getClass();
        }, () -> {
        }, () -> NotEnoughUpdates.INSTANCE.openGui = new GuiScreenElementWrapper(
                new NEUConfigEditor(NotEnoughUpdates.INSTANCE.config, activeConfig))
        ));
    }

    @Override
    public void executeRunnable(int runnableId) {
        String activeConfigCategory = null;
        if(Minecraft.getMinecraft().currentScreen instanceof GuiScreenElementWrapper) {
            GuiScreenElementWrapper wrapper = (GuiScreenElementWrapper) Minecraft.getMinecraft().currentScreen;
            if(wrapper.element instanceof NEUConfigEditor) {
                activeConfigCategory = ((NEUConfigEditor)wrapper.element).getSelectedCategory();
            }
        }
        final String activeConfigCategoryF = activeConfigCategory;

        switch (runnableId) {
            case 0:
                ClientCommandHandler.instance.executeCommand(Minecraft.getMinecraft().thePlayer, "/neumap");
                return;
            case 1:
                editOverlay(activeConfigCategory, OverlayManager.miningOverlay, mining.overlayPosition);
                return;
            case 2:
                Minecraft.getMinecraft().displayGuiScreen(new GuiPositionEditor(
                        NotEnoughUpdates.INSTANCE.config.mining.drillFuelBarPosition,
                        NotEnoughUpdates.INSTANCE.config.mining.drillFuelBarWidth, 12, () -> {
                }, () -> {
                }, () -> NotEnoughUpdates.INSTANCE.openGui = new GuiScreenElementWrapper(
                        new NEUConfigEditor(NotEnoughUpdates.INSTANCE.config, activeConfigCategoryF))
                ));
                return;
            case 3:
                editOverlay(activeConfigCategory, OverlayManager.farmingOverlay, skillOverlays.farmingPosition);
                return;
            case 4:
                editOverlay(activeConfigCategory, OverlayManager.petInfoOverlay, petOverlay.petInfoPosition);
                /*Minecraft.getMinecraft().displayGuiScreen(new GuiPositionEditor(
                        NotEnoughUpdates.INSTANCE.config.petOverlay.petInfoPosition,
                        150, 22, () -> {
                }, () -> {
                }, () -> NotEnoughUpdates.INSTANCE.openGui = new GuiScreenElementWrapper(
                        new NEUConfigEditor(NotEnoughUpdates.INSTANCE.config, activeConfigCategoryF))
                ));*/
                return;
        }
    }

    @Expose
    @Category(
            name = "Misc",
            desc = "Miscellaneous options which don't fit into any other category"
    )
    public Misc misc = new Misc();

    @Expose
    @Category(
            name = "Notifications",
            desc = "Notifications"
    )
    public Notifications notifications = new Notifications();

    @Expose
    @Category(
            name = "Item List",
            desc = "Modify the item list which shows when opening an inventory"
    )
    public Itemlist itemlist = new Itemlist();

    @Expose
    @Category(
            name = "Toolbar",
            desc = "Toolbar"
    )
    public Toolbar toolbar = new Toolbar();

    @Expose
    @Category(
            name = "Tooltip Tweaks",
            desc = "Tooltip Tweaks"
    )
    public TooltipTweaks tooltipTweaks = new TooltipTweaks();

    @Expose
    @Category(
            name = "Price Info (Auction)",
            desc = "Price Info (Auction)"
    )
    public PriceInfoAuc priceInfoAuc = new PriceInfoAuc();

    @Expose
    @Category(
            name = "Price Info (Bazaar)",
            desc = "Price Info (Bazaar)"
    )
    public PriceInfoBaz priceInfoBaz = new PriceInfoBaz();

    @Expose
    @Category(
            name = "Skill Overlays",
            desc = "Skill Overlays"
    )
    public SkillOverlays skillOverlays = new SkillOverlays();

    @Expose
    @Category(
            name = "Dungeon Profit",
            desc = "Dungeon Profit"
    )
    public DungeonProfit dungeonProfit = new DungeonProfit();


    @Expose
    @Category(
            name = "Dungeon Solvers",
            desc = "Dungeon Solvers"
    )
    public DungeonSolvers dungeonSolvers = new DungeonSolvers();


    @Expose
    @Category(
            name = "Enchanting Solvers",
            desc = "Enchanting Solvers"
    )
    public EnchSolvers enchantingSolvers = new EnchSolvers();

    @Expose
    @Category(
            name = "Mining",
            desc = "Mining"
    )
    public Mining mining = new Mining();

    @Expose
    @Category(
            name = "NEU Auction House",
            desc = "NEU Auction House"
    )
    public NeuAuctionHouse neuAuctionHouse = new NeuAuctionHouse();

    @Expose
    @Category(
            name = "Improved SB Menus",
            desc = "Improved SB Menus"
    )
    public ImprovedSBMenu improvedSBMenu = new ImprovedSBMenu();

    @Expose
    @Category(
            name = "Calendar",
            desc = "Calendar"
    )
    public Calendar calendar = new Calendar();

    @Expose
    @Category(
            name = "Trade Menu",
            desc = "Trade Menu"
    )
    public TradeMenu tradeMenu = new TradeMenu();

    @Expose
    @Category(
            name = "Treecap Overlay",
            desc = "Treecap Overlay"
    )
    public Treecap treecap = new Treecap();

    @Expose
    @Category(
            name = "Pet Overlay",
            desc = "Pet Overlay"
    )
    public PetOverlay petOverlay = new PetOverlay();

    @Expose
    @Category(
            name = "Builders Wand Overlay",
            desc = "Builders Wand Overlay"
    )
    public BuilderWand builderWand = new BuilderWand();


    @Expose
    @Category(
            name = "Dungeon Map",
            desc = "Dungeon Map"
    )
    public DungeonMapOpen dungeonMapOpen = new DungeonMapOpen();

    @Expose
    @Category(
            name = "Smooth AOTE",
            desc = "Smooth AOTE"
    )
    public SmoothAOTE smoothAOTE = new SmoothAOTE();


    @Expose
    @Category(
            name = "AH Search GUI",
            desc = "AH Search GUI"
    )
    public AuctionHouseSearch auctionHouseSearch = new AuctionHouseSearch();

    @Expose
    @Category(
            name = "Dungeon Block Overlay",
            desc = "Dungeon Block Overlay"
    )
    public DungeonBlock dungeonBlock = new DungeonBlock();

    @Expose
    @Category(
            name = "Bonemerang Overlay",
            desc = "Bonemerang Overlay"
    )
    public BonemerangOverlay bonemerangOverlay = new BonemerangOverlay();

    @Expose
    @Category(
            name = "Accessory Bag Overlay",
            desc = "Accessory Bag Overlay"
    )
    public AccessoryBag accessoryBag = new AccessoryBag();

    @Expose
    @Category(
            name = "Custom Rod Colours",
            desc = "Custom Rod Colours"
    )
    public RodColours rodColours = new RodColours();

    @Expose
    @Category(
            name = "Dungeon Win Overlay",
            desc = "Dungeon Win Overlay"
    )
    public DungeonWin dungeonWin = new DungeonWin();

    @Expose
    @Category(
            name = "Api Key",
            desc = "Api Key"
    )
    public ApiKey apiKey = new ApiKey();

    @Expose
    public Hidden hidden = new Hidden();

    @Expose
    public DungeonMap dungeonMap = new DungeonMap();

    public static class Misc {
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
                name = "Gui Click Sounds",
                desc = "Play click sounds in various NEU-related GUIs when pressing buttons"
        )
        @ConfigEditorBoolean
        public boolean guiButtonClicks = true;

        @Expose
        @ConfigOption(
                name = "Damage Indicator Style",
                desc = "Change the style of Skyblock damage indicators to be easier to read"
        )
        @ConfigEditorDropdown(
                values = {"Off", "Commas", "Shortened"}
        )
        public int damageIndicatorStyle = 1;
    }

    public static class Notifications {
        @Expose
        @ConfigOption(
                name = "Update Messages",
                desc = "Give a notification in chat whenever a new version of NEU is released"
        )
        @ConfigEditorBoolean
        public boolean showUpdateMsg = true;

        @Expose
        @ConfigOption(
                name = "Wrong Pet",
                desc = "Gives a notification in chat whenever you're using a pet that doesnt match the same xp you're gathering."
        )
        @ConfigEditorBoolean
        public boolean showWrongPetMsg = false;
    }

    public static class Itemlist {
        @Expose
        @ConfigOption(
                name = "Show Vanilla Items",
                desc = "Vanilla items are included in the item list"
        )
        @ConfigEditorBoolean
        public boolean showVanillaItems = true;

        @Expose
        @ConfigOption(
                name = "Open Itemlist Arrow",
                desc = "Creates an arrow on the right-side to open the item list when hovered"
        )
        @ConfigEditorBoolean
        public boolean tabOpen = true;

        @Expose
        @ConfigOption(
                name = "Keep Open",
                desc = "Keeps the Itemlist open after the inventory is closed"
        )
        @ConfigEditorBoolean
        public boolean keepopen = false;

        @Expose
        @ConfigOption(
                name = "Item Style",
                desc = "Sets the style of the background behind items"
        )
        @ConfigEditorDropdown(
                values = {"Round", "Square"}
        )
        public int itemStyle = 0;

        @Expose
        @ConfigOption(
                name = "Pane Gui Scale",
                desc = "Change the gui scale of the Itemlist"
        )
        @ConfigEditorDropdown(
                values = {"Default", "Small", "Medium", "Large", "Auto"}
        )
        public int paneGuiScale = 0;

        @Expose
        @ConfigOption(
                name = "Background Blur",
                desc = "Change the blur amount behind the Itemlist. 0 = off"
        )
        @ConfigEditorSlider(
                minValue = 0,
                maxValue = 20,
                minStep = 1
        )
        public int bgBlurFactor = 5;

        @Expose
        @ConfigOption(
                name = "Pane Width Multiplier",
                desc = "Change the width of the Itemlist"
        )
        @ConfigEditorSlider(
                minValue = 0.5f,
                maxValue = 1.5f,
                minStep = 0.1f
        )
        public float paneWidthMult = 1.0f;

        @Expose
        @ConfigOption(
                name = "Pane Padding",
                desc = "Change the padding around the Itemlist"
        )
        @ConfigEditorSlider(
                minValue = 0f,
                maxValue = 20f,
                minStep = 1f
        )
        public int panePadding = 10;

        @Expose
        @ConfigOption(
                name = "Foreground Colour",
                desc = "Change the colour of foreground elements in the Itemlist"
        )
        @ConfigEditorColour
        public String foregroundColour = "00:255:100:100:100";

        @Expose
        @ConfigOption(
                name = "Favourite Colour",
                desc = "Change the colour of favourited elements in the Itemlist"
        )
        @ConfigEditorColour
        public String favouriteColour = "00:255:200:150:50";

        @Expose
        @ConfigOption(
                name = "Pane Background Colour",
                desc = "Change the colour of the Itemlist background"
        )
        @ConfigEditorColour
        public String backgroundColour = "15:6:0:0:255";
    }

    public static class Toolbar {
        @Expose
        @ConfigOption(
                name = "Show Quick Commands",
                desc = "Show QuickCommands\u2122 in the NEU toolbar"
        )
        @ConfigEditorBoolean
        public boolean quickCommands = true;

        @Expose
        @ConfigOption(
                name = "Show Search Bar",
                desc = "Show Itemlist search bar in the NEU toolbar"
        )
        @ConfigEditorBoolean
        public boolean searchBar = true;

        @Expose
        @ConfigOption(
                name = "Search Bar Width",
                desc = "Change the width of the search bar"
        )
        @ConfigEditorSlider(
                minValue = 50f,
                maxValue = 300f,
                minStep = 10f
        )
        public int searchBarWidth = 200;

        @Expose
        @ConfigOption(
                name = "Search Bar Height",
                desc = "Change the height of the search bar"
        )
        @ConfigEditorSlider(
                minValue = 15f,
                maxValue = 50f,
                minStep = 1f
        )
        public int searchBarHeight = 40;

        @Expose
        @ConfigOption(
                name = "Quick Commands Click Type",
                desc = "Change the click type needed to trigger quick commands"
        )
        @ConfigEditorDropdown(
                values = {"Mouse Up", "Mouse Down"}
        )
        public int quickCommandsClickType = 0;
    }

    public static class TooltipTweaks {
        @Expose
        @ConfigOption(
                name = "Price Info (Inv)",
                desc = "Show price information for items in your inventory"
        )
        @ConfigEditorBoolean
        public boolean showPriceInfoInvItem = true;

        @Expose
        @ConfigOption(
                name = "Price Info (AH)",
                desc = "Show price information for auctioned items"
        )
        @ConfigEditorBoolean
        public boolean showPriceInfoAucItem = true;

        @Expose
        @ConfigOption(
                name = "Missing Enchant List",
                desc = "Show which enchants are missing on an item when pressing LSHIFT"
        )
        @ConfigEditorBoolean
        public boolean missingEnchantList = true;

        @Expose
        @ConfigOption(
                name = "Tooltip Border Colours",
                desc = "Make the borders of tooltips match the rarity of the item (NEU Tooltips Only)"
        )
        @ConfigEditorBoolean
        public boolean tooltipBorderColours = true;

        @Expose
        @ConfigOption(
                name = "Tooltip Border Opacity",
                desc = "Change the opacity of the rarity highlight (NEU Tooltips Only)"
        )
        @ConfigEditorSlider(
                minValue = 0f,
                maxValue = 255f,
                minStep = 1f
        )
        public int tooltipBorderOpacity = 200;
    }

    public static class PriceInfoAuc {
        @Expose
        @ConfigOption(
                name = "Line 1",
                desc = "Set the price information displayed on Line #1"
        )
        @ConfigEditorDropdown(
                values = {"", "Lowest BIN", "AH Price", "AH Sales", "Raw Craft Cost", "AVG Lowest BIN", "Dungeon Costs"}
        )
        public int line1 = 1;

        @Expose
        @ConfigOption(
                name = "Line 2",
                desc = "Set the price information displayed on Line #2"
        )
        @ConfigEditorDropdown(
                values = {"", "Lowest BIN", "AH Price", "AH Sales", "Raw Craft Cost", "AVG Lowest BIN", "Dungeon Costs"}
        )
        public int line2 = 2;

        @Expose
        @ConfigOption(
                name = "Line 3",
                desc = "Set the price information displayed on Line #3"
        )
        @ConfigEditorDropdown(
                values = {"", "Lowest BIN", "AH Price", "AH Sales", "Raw Craft Cost", "AVG Lowest BIN", "Dungeon Costs"}
        )
        public int line3 = 3;

        @Expose
        @ConfigOption(
                name = "Line 4",
                desc = "Set the price information displayed on Line #4"
        )
        @ConfigEditorDropdown(
                values = {"", "Lowest BIN", "AH Price", "AH Sales", "Raw Craft Cost", "AVG Lowest BIN", "Dungeon Costs"}
        )
        public int line4 = 4;

        @Expose
        @ConfigOption(
                name = "Line 5",
                desc = "Set the price information displayed on Line #5"
        )
        @ConfigEditorDropdown(
                values = {"", "Lowest BIN", "AH Price", "AH Sales", "Raw Craft Cost", "AVG Lowest BIN", "Dungeon Costs"}
        )
        public int line5 = 6;

        @Expose
        @ConfigOption(
                name = "Line 6",
                desc = "Set the price information displayed on Line #6"
        )
        @ConfigEditorDropdown(
                values = {"", "Lowest BIN", "AH Price", "AH Sales", "Raw Craft Cost", "AVG Lowest BIN", "Dungeon Costs"}
        )
        public int line6 = 0;
    }

    public static class PriceInfoBaz {
        @Expose
        @ConfigOption(
                name = "Line 1",
                desc = "Set the price information displayed on Line #1"
        )
        @ConfigEditorDropdown(
                values = {"", "Buy", "Sell", "Buy (Insta)", "Sell (Insta)", "Raw Craft Cost"}
        )
        public int line1 = 1;

        @Expose
        @ConfigOption(
                name = "Line 2",
                desc = "Set the price information displayed on Line #2"
        )
        @ConfigEditorDropdown(
                values = {"", "Buy", "Sell", "Buy (Insta)", "Sell (Insta)", "Raw Craft Cost"}
        )
        public int line2 = 2;

        @Expose
        @ConfigOption(
                name = "Line 3",
                desc = "Set the price information displayed on Line #3"
        )
        @ConfigEditorDropdown(
                values = {"", "Buy", "Sell", "Buy (Insta)", "Sell (Insta)", "Raw Craft Cost"}
        )
        public int line3 = 3;

        @Expose
        @ConfigOption(
                name = "Line 4",
                desc = "Set the price information displayed on Line #4"
        )
        @ConfigEditorDropdown(
                values = {"", "Buy", "Sell", "Buy (Insta)", "Sell (Insta)", "Raw Craft Cost"}
        )
        public int line4 = 4;

        @Expose
        @ConfigOption(
                name = "Line 5",
                desc = "Set the price information displayed on Line #5"
        )
        @ConfigEditorDropdown(
                values = {"", "Buy", "Sell", "Buy (Insta)", "Sell (Insta)", "Raw Craft Cost"}
        )
        public int line5 = 5;

        @Expose
        @ConfigOption(
                name = "Line 6",
                desc = "Set the price information displayed on Line #6"
        )
        @ConfigEditorDropdown(
                values = {"", "Buy", "Sell", "Buy (Insta)", "Sell (Insta)", "Raw Craft Cost"}
        )
        public int line6 = 0;
    }

    public static class SkillOverlays {
        @Expose
        @ConfigOption(
                name = "Enable Farming Overlay",
                desc = "Show an overlay while farming with useful information"
        )
        @ConfigEditorBoolean
        public boolean farmingOverlay = true;

        @Expose
        @ConfigOption(
                name = "Farming Text",
                desc = "\u00a7eDrag text to change the appearance of the overlay\n" +
                        "\u00a7rHold a mathematical hoe or use an axe while gaining farming xp to show the overlay"
        )
        @ConfigEditorDraggableList(
                exampleText = {"\u00a7bCounter: \u00a7e37,547,860",
                        "\u00a7bCrops/m: \u00a7e38.29",
                        "\u00a7bFarm: \u00a7e12\u00a77 [\u00a7e|||||||||||||||||\u00a78||||||||\u00a77] \u00a7e67%",
                        "\u00a7bCurrent XP: \u00a7e6,734",
                        "\u00a7bRemaining XP: \u00a7e3,265",
                        "\u00a7bXP/h: \u00a7e238,129",
                        "\u00a7bYaw: \u00a7e68.25\u00a7l\u1D52",
                        "\u00a7bETA: 13h12m"}
        )
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
        public Position farmingPosition = new Position(10, 200);

        @Expose
        @ConfigOption(
                name = "Farming Style",
                desc = "Change the style of the Farming overlay"
        )
        @ConfigEditorDropdown(
                values = {"Background", "No Shadow", "Shadow", "Full Shadow"}
        )
        public int farmingStyle = 0;
    }

    public static class DungeonProfit {
        @Expose
        @ConfigOption(
                name = "Profit Type",
                desc = "Set the price dataset used for calculating profit"
        )
        @ConfigEditorDropdown(
                values = {"Lowest BIN", "24 AVG Lowest Bin", "Auction AVG"}
        )
        public int profitType = 0;

        @Expose
        @ConfigOption(
                name = "Profit Display Location",
                desc = "Set where the profit information is displayed\n" +
                        "Overlay = Overlay on right side of inventory\n" +
                        "GUI Title = Text displayed next to the inventory title\n" +
                        "Lore = Inside the \"Open Reward Chest\" item"
        )
        @ConfigEditorDropdown(
                values = {"Overlay", "GUI Title", "Lore", "Off"}
        )
        public int profitDisplayLoc = 0;
    }
    public static class DungeonSolvers {

    }

    public static class EnchSolvers {
        @Expose
        @ConfigOption(
                name = "Enable Solvers",
                desc = "Turn on solvers for the experimentation table"
        )
        @ConfigEditorBoolean
        public boolean enableEnchantingSolvers = true;

        @Expose
        @ConfigOption(
                name = "Prevent Misclicks",
                desc = "Prevent accidentally failing the Chronomatron and Ultrasequencer experiments"
        )
        @ConfigEditorBoolean
        public boolean preventMisclicks = true;

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

    public static class Mining {
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
        public boolean revealMistCreepers = true;

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
        public int emissaryWaypoints = 1;

        @Expose
        @ConfigOption(
                name = "Drill Fuel Bar",
                desc = "Show a fancy drill fuel bar when holding a drill in mining areas"
        )
        @ConfigEditorBoolean
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
        public Position drillFuelBarPosition = new Position(0, -100, true, false);


        @Expose
        @ConfigOption(
                name = "Dwarven Overlay",
                desc = "Show an overlay with useful information on the screen while in Dwarven Mines"
        )
        @ConfigEditorBoolean
        public boolean dwarvenOverlay = true;

        @Expose
        @ConfigOption(
                name = "Dwarven Text",
                desc = "\u00a7eDrag text to change the appearance of the overlay\n" +
                        "\u00a7rGo to the Dwarven Mines to show this overlay with useful information"
        )
        @ConfigEditorDraggableList(
                exampleText = {"\u00a73Goblin Slayer: \u00a7626.5%\n\u00a73Lucky Raffle: \u00a7c0.0%",
                "\u00a73Mithril Powder: \u00a726,243",
                "\u00a73Forge 1) \u00a79Diamonite\u00a77: \u00a7aReady!",
                "\u00a73Forge 2) \u00a77EMPTY\n\u00a73Forge 3) \u00a77EMPTY\n\u00a73Forge 4) \u00a77EMPTY"}
        )
        public List<Integer> dwarvenText = new ArrayList<>(Arrays.asList(0, 1, 2, 3));

        @Expose
        @ConfigOption(
                name = "Overlay Position",
                desc = "Change the position of the Dwarven Mines information overlay (commisions, powder & forge statuses)"
        )
        @ConfigEditorButton(
                runnableId = 1,
                buttonText = "Edit"
        )
        public Position overlayPosition = new Position(10, 100);

        @Expose
        @ConfigOption(
                name = "Overlay Style",
                desc = "Change the style of the Dwarven Mines information overlay"
        )
        @ConfigEditorDropdown(
                values = {"Background", "No Shadow", "Shadow", "Full Shadow"}
        )
        public int overlayStyle = 0;
    }

    public static class NeuAuctionHouse {
        @Expose
        @ConfigOption(
                name = "Enable NeuAH",
                desc = "Turn on the NEU Auction House. \u00A7cWARNING: May negatively impact performance on low-end machines"
        )
        @ConfigEditorBoolean
        public boolean enableNeuAuctionHouse = false;

        @Expose
        @ConfigOption(
                name = "Disable AH Scroll",
                desc = "Disable scrolling using the scroll wheel inside NeuAH.\n" +
                        "This should be used if you want to be able to scroll through tooltips"
        )
        @ConfigEditorBoolean
        public boolean disableAhScroll = false;

        @Expose
        @ConfigOption(
                name = "AH Notification (Mins)",
                desc = "Change the amount of time (in minutes) before the \"Ending Soon\" notification for an auction you have bid on"
        )
        @ConfigEditorSlider(
                minValue = 1f,
                maxValue = 10f,
                minStep = 1f
        )
        public int ahNotification = 5;
    }

    public static class ImprovedSBMenu {
        @Expose
        @ConfigOption(
                name = "Enable Improved SB Menus",
                desc = "Change the way that skyblock menus (eg. /sbmenu) look"
        )
        @ConfigEditorBoolean
        public boolean enableSbMenus = true;

        @Expose
        @ConfigOption(
                name = "Menu Background Style",
                desc = "Change the style of the background of skyblock menus"
        )
        @ConfigEditorDropdown(
                values = {"Dark 1", "Dark 2", "Transparent", "Light 1", "Light 2", "Light 3",
                          "Unused 1", "Unused 2", "Unused 3", "Unused 4"}
        )
        public int backgroundStyle = 0;

        @Expose
        @ConfigOption(
                name = "Button Background Style",
                desc = "Change the style of the foreground elements in skyblock menus"
        )
        @ConfigEditorDropdown(
                values = {"Dark 1", "Dark 2", "Transparent", "Light 1", "Light 2", "Light 3",
                          "Unused 1", "Unused 2", "Unused 3", "Unused 4"}
        )
        public int buttonStyle = 0;

        @Expose
        @ConfigOption(
                name = "Hide Empty Tooltips",
                desc = "Hide the tooltips of glass panes with no text"
        )
        @ConfigEditorBoolean
        public boolean hideEmptyPanes = true;
    }

    public static class Calendar {
        @Expose
        @ConfigOption(
                name = "Event Notifications",
                desc = "Display notifications for skyblock calendar events"
        )
        @ConfigEditorBoolean
        public boolean eventNotifications = true;

        @Expose
        @ConfigOption(
                name = "Starting Soon Time",
                desc = "Display a notification before events start, time in seconds.\n" +
                        "0 = No prior notification"
        )
        @ConfigEditorSlider(
                minValue = 0f,
                maxValue = 600f,
                minStep = 30f
        )
        public int startingSoonTime = 300;

        @Expose
        @ConfigOption(
                name = "Timer In Inventory",
                desc = "Displays the time until the next event at the top of your screen when in inventories"
        )
        @ConfigEditorBoolean
        public boolean showEventTimerInInventory = true;

        @Expose
        @ConfigOption(
                name = "Notification Sounds",
                desc = "Play a sound whenever events start"
        )
        @ConfigEditorBoolean
        public boolean eventNotificationSounds = true;

        @Expose
        @ConfigOption(
                name = "Spooky Night Notification",
                desc = "Send a notification during spooky event when the time reaches 7pm"
        )
        @ConfigEditorBoolean
        public boolean spookyNightNotification = true;
    }

    public static class TradeMenu {
        @Expose
        @ConfigOption(
                name = "Enable Custom Trade Menu",
                desc = "When trading with other players in skyblock, display a special GUI designed to prevent scamming"
        )
        @ConfigEditorBoolean
        public boolean enableCustomTrade = true;


        @Expose
        @ConfigOption(
                name = "Price Information",
                desc = "Show the price of items in the trade window on both sides"
        )
        @ConfigEditorBoolean
        public boolean customTradePrices = true;

        @Expose
        public boolean customTradePriceStyle = true;
    }

    public static class Treecap {
        @Expose
        @ConfigOption(
                name = "Enable Treecap Overlay",
                desc = "Show which blocks will be broken when using a Jungle Axe or Treecapitator"
        )
        @ConfigEditorBoolean
        public boolean enableTreecapOverlay = true;

        @Expose
        @ConfigOption(
                name = "Overlay Colour",
                desc = "Change the colour of the overlay"
        )
        @ConfigEditorColour
        public String treecapOverlayColour = "00:50:64:224:208";

        @Expose
        @ConfigOption(
                name = "Enable Monkey Pet Check",
                desc = "Will check use the API to check what pet you're using\nto determine the cooldown based off of if you have monkey pet."
        )
        @ConfigEditorBoolean

        public boolean enableMonkeyCheck = true;
    }

    public static class PetOverlay {
        @Expose
        @ConfigOption(
                name = "Enable Pet Info Overlay",
                desc = "Shows current active pet and pet exp on screen."
        )
        @ConfigEditorBoolean
        public boolean enablePetInfo = false;

        @Expose
        @ConfigOption(
                name = "Pet Info Position",
                desc = "The position of the pet info."
        )
        @ConfigEditorButton(
                runnableId = 4,
                buttonText = "Edit"
        )
        public Position petInfoPosition = new Position(-1, -1);

        @Expose
        @ConfigOption(
                name = "Pet Overlay Text",
                desc = "\u00a7eDrag text to change the appearance of the overlay\n" +
                        "\u00a7rEquip a pet to show the overlay"
        )
        @ConfigEditorDraggableList(
                exampleText = {"\u00a7a[Lvl 37] \u00a7fRock",
                        "\u00a7b2,312.9/2,700\u00a7e (85.7%)",
                        "\u00a7b2.3k/2.7k\u00a7e (85.7%)",
                        "\u00a7bXP/h: \u00a7e27,209",
                        "\u00a7bTotal XP: \u00a7e30,597.9",
                        "\u00a7bHeld Item: \u00a7fMining Exp Boost",
                        "\u00a7bUntil L38: \u00a7e5m13s",
                        "\u00a7bUntil L100: \u00a7e2d13h"}
        )
        public List<Integer> petOverlayText = new ArrayList<>(Arrays.asList(0, 2, 3, 6, 4));

        @Expose
        @ConfigOption(
                name = "Pet Overlay Icon",
                desc = "Show the icon of the pet you have equiped in the overlay"
        )
        @ConfigEditorBoolean
        public boolean petOverlayIcon = true;

        @Expose
        @ConfigOption(
                name = "Pet Info Overlay Style",
                desc = "Change the style of the Pet Info overlay"
        )
        @ConfigEditorDropdown(
                values = {"Background", "No Shadow", "Shadow Only", "Full Shadow"}
        )
        public int petInfoOverlayStyle = 0;
    }

    public static class BuilderWand {
        @Expose
        @ConfigOption(
                name = "Enable Wand Overlay",
                desc = "Show which blocks will be placed when using the Builder's Wand"
        )
        @ConfigEditorBoolean
        public boolean enableWandOverlay = true;

        @Expose
        @ConfigOption(
                name = "Wand Block Count",
                desc = "Shows the total count of a block in your inventory"
        )
        @ConfigEditorBoolean
        public boolean wandBlockCount = true;

        @Expose
        @ConfigOption(
                name = "Overlay Colour",
                desc = "Change the colour of the ghost block outline"
        )
        @ConfigEditorColour
        public String wandOverlayColour = "00:50:64:224:208";
    }

    public static class DungeonMapOpen {
        @Expose
        @ConfigOption(
                name = "Edit Dungeon Map",
                desc = "The NEU dungeon map has it's own editor (/neumap).\n" +
                        "Click the button on the left to open it"
        )
        @ConfigEditorButton(
                runnableId = 0,
                buttonText = "Edit"
        )
        public int editDungeonMap = 0;

        @Expose
        @ConfigOption(
                name = "Show Own Head As Marker",
                desc = "If you have the \"Head\" icon style selected, don't replace your green marker with a head"
        )
        @ConfigEditorBoolean
        public boolean showOwnHeadAsMarker = false;
    }

    public static class AuctionHouseSearch {
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

    public static class DungeonBlock {
        @Expose
        @ConfigOption(
                name = "Enable Block Overlay",
                desc = "Change the colour of certain blocks / entities while inside dungeons, but keeps the normal texture outside of dungeons"
        )
        @ConfigEditorBoolean
        public boolean enableDungBlockOverlay = true;

        @Expose
        @ConfigOption(
                name = "Show Overlay Everywhere",
                desc = "Show the dungeon block overlay even when not inside dungeons. Should only be used for testing."
        )
        @ConfigEditorBoolean
        public boolean dungeonBlocksEverywhere = false;

        @Expose
        @ConfigOption(
                name = "Slow Update",
                desc = "Updates the colour every second instead of every tick.\n" +
                        "\u00A7cWARNING: This will cause all texture animations (eg. flowing water) to update slowly.\n" +
                        "This should only be used on low-end machines"
        )
        @ConfigEditorBoolean
        public boolean slowDungeonBlocks = false;

        @Expose
        @ConfigOption(
                name = "Cracked Bricks",
                desc = "Change the colour of: Cracked Bricks"
        )
        @ConfigEditorColour
        public String dungCrackedColour = "0:255:7:255:217";

        @Expose
        @ConfigOption(
                name = "Dispensers",
                desc = "Change the colour of: Dispensers"
        )
        @ConfigEditorColour
        public String dungDispenserColour = "0:255:255:76:0";

        @Expose
        @ConfigOption(
                name = "Levers",
                desc = "Change the colour of: Levers"
        )
        @ConfigEditorColour
        public String dungLeverColour = "0:252:24:249:255";

        @Expose
        @ConfigOption(
                name = "Tripwire String",
                desc = "Change the colour of: Tripwire String"
        )
        @ConfigEditorColour
        public String dungTripWireColour = "0:255:255:0:0";

        @Expose
        @ConfigOption(
                name = "Normal Chests",
                desc = "Change the colour of: Normal Chests"
        )
        @ConfigEditorColour
        public String dungChestColour = "0:255:0:163:36";

        @Expose
        @ConfigOption(
                name = "Trapped Chests",
                desc = "Change the colour of: Trapped Chests"
        )
        @ConfigEditorColour
        public String dungTrappedChestColour = "0:255:0:163:36";

        @Expose
        @ConfigOption(
                name = "Bats",
                desc = "Change the colour of: Bats"
        )
        @ConfigEditorColour
        public String dungBatColour = "0:255:12:255:0";
    }

    public static class BonemerangOverlay {
        @Expose
        @ConfigOption(
                name = "Highlight Targeted Entities",
                desc = "Highlight entities that will be hit by your bonemerang"
        )
        @ConfigEditorBoolean
        public boolean highlightTargeted = true;

        @Expose
        @ConfigOption(
                name = "Break Warning",
                desc = "Show a warning below your crosshair if the bonemerang will break on a block"
        )
        @ConfigEditorBoolean
        public boolean showBreak = true;
    }

    public static class AccessoryBag {
        @Expose
        @ConfigOption(
                name = "Enable Accessory Bag Overlay",
                desc = "Show an overlay on the accessory bag screen which gives useful information about your accessories"
        )
        @ConfigEditorBoolean
        public boolean enableOverlay = true;
    }

    public static class SmoothAOTE {
        @Expose
        @ConfigOption(
                name = "Enable Smooth AOTE",
                desc = "Teleport smoothly to your destination when using AOTE"
        )
        @ConfigEditorBoolean
        public boolean enableSmoothAOTE = true;

        @Expose
        @ConfigOption(
                name = "Enable Smooth Hyperion",
                desc = "Teleport smoothly to your destination when using Hyperion"
        )
        @ConfigEditorBoolean
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
        public int smoothTpMillis = 175;


        @Expose
        @ConfigOption(
                name = "Disable Hyperion Particles",
                desc = "Remove the explosion effect when using a hyperion"
        )
        @ConfigEditorBoolean
        public boolean disableHyperionParticles = true;
    }

    public static class RodColours {
        @Expose
        @ConfigOption(
                name = "Enable Rod Colours",
                desc = "Change the colour of your and other players' rod lines\n" +
                        "Also fixes the position of the rod line"
        )
        @ConfigEditorBoolean
        public boolean enableRodColours = true;

        @Expose
        @ConfigOption(
                name = "Own Rod Colour",
                desc = "Change the colour of your own rod lines"
        )
        @ConfigEditorColour
        public String ownRodColour = "0:255:0:0:0";


        @Expose
        @ConfigOption(
                name = "Other Rod Colour",
                desc = "Change the colour of other players' rod lines"
        )
        @ConfigEditorColour
        public String otherRodColour = "0:255:0:0:0";
    }

    public static class DungeonWin {
        @Expose
        @ConfigOption(
                name = "Enable Dungeon Win",
                desc = "Show a fancy win screen and stats when completing a dungeon"
        )
        @ConfigEditorBoolean
        public boolean enableDungeonWin = true;

        @Expose
        @ConfigOption(
                name = "Dungeon Win Time",
                desc = "Change the amount of time (milliseconds) that the win screen shows for"
        )
        @ConfigEditorSlider(
                minValue = 0,
                maxValue = 20000,
                minStep = 500
        )
        public int dungeonWinMillis = 8000;
    }

    public static class ApiKey {
        @Expose
        @ConfigOption(
                name = "Api Key",
                desc = "Hypixel api key"
        )
        @ConfigEditorText
        public String apiKey = "";
    }

    private static ArrayList<String> createDefaultQuickCommands() {
        ArrayList<String> arr = new ArrayList<>();
        arr.add("/warp home:Warp Home:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzljODg4MWU0MjkxNWE5ZDI5YmI2MWExNmZiMjZkMDU5OTEzMjA0ZDI2NWRmNWI0MzliM2Q3OTJhY2Q1NiJ9fX0=");
        arr.add("/warp hub:Warp Hub:eyJ0aW1lc3RhbXAiOjE1NTkyMTU0MTY5MDksInByb2ZpbGVJZCI6IjQxZDNhYmMyZDc0OTQwMGM5MDkwZDU0MzRkMDM4MzFiIiwicHJvZmlsZU5hbWUiOiJNZWdha2xvb24iLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Q3Y2M2Njg3NDIzZDA1NzBkNTU2YWM1M2UwNjc2Y2I1NjNiYmRkOTcxN2NkODI2OWJkZWJlZDZmNmQ0ZTdiZjgifX19");
        arr.add("/warp dungeon_hub:Dungeon Hub:eyJ0aW1lc3RhbXAiOjE1Nzg0MDk0MTMxNjksInByb2ZpbGVJZCI6IjQxZDNhYmMyZDc0OTQwMGM5MDkwZDU0MzRkMDM4MzFiIiwicHJvZmlsZU5hbWUiOiJNZWdha2xvb24iLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzliNTY4OTViOTY1OTg5NmFkNjQ3ZjU4NTk5MjM4YWY1MzJkNDZkYjljMWIwMzg5YjhiYmViNzA5OTlkYWIzM2QiLCJtZXRhZGF0YSI6eyJtb2RlbCI6InNsaW0ifX19fQ==");
        arr.add("/craft:Crafting Table:CRAFTING_TABLE");
        arr.add("/enderchest:Ender Chest:ENDER_CHEST");
        arr.add("/wardrobe:Wardrobe:LEATHER_CHESTPLATE");
        arr.add("/pets:Pets:BONE");
        arr.add("neuah:NEU Auction House:GOLD_BLOCK");
        arr.add("/bz:Bazaar:GOLD_BARDING");
        return arr;
    }

    public static class Hidden {
        @Expose public int commissionMilestone = 0;
        @Expose public boolean enableItemEditing = false;
        @Expose public boolean cacheRenderedItempane = true;
        @Expose public boolean autoupdate = true;
        @Expose public String overlaySearchBar = "";
        @Expose public String overlayQuickCommand = "";
        @Expose public boolean dev = false;
        @Expose public boolean loadedModBefore = false;
        @Expose public boolean doRamNotif = true;
        @Expose public String selectedCape = null;
        @Expose public int compareMode = 0;
        @Expose public int sortMode = 0;
        @Expose public ArrayList<Boolean> compareAscending = Lists.newArrayList(true, true, true);
        @Expose public ArrayList<String> favourites = new ArrayList<>();
        @Expose public ArrayList<String> previousAuctionSearches = new ArrayList<>();
        @Expose public ArrayList<String> eventFavourites = new ArrayList<>();
        @Expose public ArrayList<String> quickCommands = createDefaultQuickCommands();
        @Expose public ArrayList<String> enchantColours = Lists.newArrayList(
                      "[a-zA-Z\\- ]+:\u003e:9:6:0",
                                "[a-zA-Z\\- ]+:\u003e:6:c:0",
                                "[a-zA-Z\\- ]+:\u003e:5:5:0",
                                "Experience:\u003e:3:5:0",
                                "Life Steal:\u003e:3:5:0",
                                "Scavenger:\u003e:3:5:0",
                                "Looting:\u003e:3:5:0");
    }

    public static class DungeonMap {
        @Expose
        @ConfigOption(
                name = "Border Size",
                desc = "Changes the size of the map border, without changing the size of the contents"
        )
        @ConfigEditorSlider(
                minValue = 0,
                maxValue = 5,
                minStep = 0.25f
        )
        public float dmBorderSize = 1;

        @Expose
        @ConfigOption(
                name = "Room Size",
                desc = "Changes the size of rooms. Useful for higher dungeons with larger maps"
        )
        @ConfigEditorSlider(
                minValue = 0,
                maxValue = 5,
                minStep = 0.25f
        )
        public float dmRoomSize = 1;

        @Expose
        @ConfigOption(
                name = "Icon Size",
                desc = "Changes the scale of room indicators and player icons"
        )
        @ConfigEditorSlider(
                minValue = 0.5f,
                maxValue = 3f,
                minStep = 0.25f
        )
        public float dmIconScale = 1.0f;

        @Expose
        @ConfigOption(
                name = "Border Style",
                desc = "Various custom borders from various talented artists.\nUse 'custom' if your texture pack has a custom border"
        )
        public int dmBorderStyle = 0;

        @Expose
        @ConfigOption(
                name = "Show Dungeon Map",
                desc = "Show/hide the NEU dungeon map"
        )
        public boolean dmEnable = true;

        @Expose
        @ConfigOption(
                name = "Map Center",
                desc = "Center on rooms, or center on your player"
        )
        public boolean dmCenterPlayer = true;

        @Expose
        @ConfigOption(
                name = "Rotate with Player",
                desc = "Rotate the map to face the same direction as your player"
        )
        public boolean dmRotatePlayer = true;
        
        @Expose
        @ConfigOption(
                name = "Orient Checkmarks",
                desc = "Checkmarks will always show vertically, regardless of rotation"
        )
        public boolean dmOrientCheck = true;
        
        @Expose
        @ConfigOption(
                name = "Center Checkmarks",
                desc = "Checkmarks will show closer to the center of rooms"
        )
        public boolean dmCenterCheck = false;
        
        @Expose
        @ConfigOption(
                name = "Player Icon Style",
                desc = "Various player icon styles"
        )
        public int dmPlayerHeads = 0;
        
        @Expose
        @ConfigOption(
                name = "Interpolate Far Players",
                desc = "Will make players far away move smoothly"
        )
        public boolean dmPlayerInterp = true;
        
        @Expose
        @ConfigOption(
                name = "OpenGL Compatibility",
                desc = "Compatiblity options for people with bad computers. ONLY use this if you know what you are doing, otherwise the map will look worse"
        )
        public int dmCompat = 0;
        
        @Expose
        @ConfigOption(
                name = "Background Colour",
                desc = "Colour of the map background. Supports opacity & chroma"
        )
        public String dmBackgroundColour = "00:170:75:75:75";
        
        @Expose
        @ConfigOption(
                name = "Border Colour",
                desc = "Colour of the map border. Supports opacity & chroma. Turn off custom borders to see"
        )
        public String dmBorderColour = "00:0:0:0:0";
        
        @Expose
        @ConfigOption(
                name = "Chroma Border Mode",
                desc = "Applies a hue offset around the map border"
        )
        public boolean dmChromaBorder = false;
        
        @Expose
        @ConfigOption(
                name = "Background Blur Factor",
                desc = "Changes the blur factor behind the map. Set to 0 to disable blur"
        )
        public float dmBackgroundBlur = 0;
        
        @Expose
        @ConfigOption(
                name = "Position",
                desc = "The position of the map"
        )
        public Position dmPosition = new Position(10, 10);
    }

}
