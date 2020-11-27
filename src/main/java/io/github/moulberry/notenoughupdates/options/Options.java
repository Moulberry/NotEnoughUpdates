package io.github.moulberry.notenoughupdates.options;

import com.google.gson.*;
import io.github.moulberry.notenoughupdates.dungeons.GuiDungeonMapEditor;
import io.github.moulberry.notenoughupdates.GuiEnchantColour;
import io.github.moulberry.notenoughupdates.NEUOverlayPlacements;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Options {

    /**
     * Ok, I'll be honest. I wrote this class without looking too much in to how to make proper serializable
     * variables with defaults values/etc. It works. I'm happy.
     */

    public static final transient int FLAG_COLOUR = 0b1;
    public static final transient int FLAG_INT = 0b10;

    public static final int CAT_ALL = 0;
    public static final int CAT_MISC = 1;
    public static final int CAT_FEATURES = 2;
    public static final int CAT_SLIDERS = 3;
    public static final int CAT_COLOURS = 4;

    public Option<Boolean> enableItemEditing = new Option(
            false,
            "Enable Item Editing",
            true,
            "Dev Feature.", CAT_ALL);
    public Option<Boolean> onlyShowOnSkyblock = new Option(
            true,
            "Only Show On Skyblock",
            false,
            "NEU Overlay only appears when you are playing Skyblock.", CAT_MISC);
    public Option<Boolean> showVanillaItems = new Option(
            true,
            "Show Vanilla Items",
            false,
            "Shows vanilla items in the itemlist.", CAT_MISC);
    public Option<Boolean> hidePotionEffect = new Option(
            true,
            "Hide Potion Effects",
            false,
            "Potion effects are hidden in the inventory GUI. Contrib: All the gamers that play on GUI AUTO", CAT_MISC);
    public Option<Boolean> showQuickCommands = new Option(
            true,
            "Quick Commands",
            false,
            "Shows QuickCommands\u2122 above search bar.", CAT_FEATURES);
    public Option<Boolean> showUpdateMsg = new Option(
            true,
            "Show Update Notifs",
            false,
            "Shows update messages if NEU is out-of-date.", CAT_MISC);
    public Option<Boolean> tooltipBorderColours = new Option(
            true,
            "Coloured Tooltip Borders",
            false,
            "Makes the border of tooltips coloured. (Only NEU Tooltips)", CAT_MISC);
    public Option<Boolean> disableAhScroll = new Option(
            false,
            "No NeuAH Scroll",
            false,
            "Disables Scrolling in NeuAH", CAT_MISC);
    public Option<Boolean> advancedPriceInfo = new Option(
            false,
            "Adv. Item Price Info",
            false,
            "Shows some extra information about item prices.", CAT_MISC);
    public Option<Boolean> cacheRenderedItempane = new Option(
            true,
            "Cache Itempane",
            false,
            "Caches the drawn itempane, drastically improving performance. Animated textures will not work.", CAT_MISC);
    public Option<Boolean> streamerMode = new Option(
            false,
            "Streamer Mode",
            false,
            "Hides or randomises some stuff on your screen to prevent sniping", CAT_MISC);
    public Option<Boolean> disableTreecapOverlay = new Option(
            false,
            "Disable Treecap Overlay",
            false,
            "Disables the treecapitator overlay effect", CAT_FEATURES);
    public Option<Boolean> disableWandOverlay = new Option(
            false,
            "Disable Builder's Wand Overlay",
            false,
            "Disables the builder's wand overlay effect", CAT_FEATURES);
    public Option<Boolean> wandBlockCount = new Option(
            true,
            "Builder's Wand Block Count",
            false,
            "If true, will show how many blocks you have remaining when holding a builder's wand.", CAT_MISC);
    public Option<Boolean> hideApiKey = new Option(
            false,
            "Hide Apikey Setting",
            false,
            "Hides the Apikey setting (please try not to leak Apikey if you're recording)", CAT_MISC);
    public Option<Double> bgBlurFactor = new Option(
            5.0,
            "Background Blur",
            false,
            "Changes the strength of pane background blur. 0-50.", 0, 50, CAT_SLIDERS);
    public Option<String> apiKey = new Option(
            "",
            "Api Key",
            false,
            "Type /api new to receive key and put it here.", CAT_MISC);
    public Option<Boolean> autoupdate = new Option(
            true,
            "Automatically Update Items",
            false,
            "If true, updated items will automatically download from the remote repository when you start the game. \nHIGHLY RECOMMENDED.", CAT_MISC);
    public Option<Boolean> quickcommandMousePress = new Option(
            false,
            "QuickCommand on Mouse Press",
            false,
            "If true, quickcommands will trigger on mouse down instead of mouse up.", CAT_MISC);
    public Option<Boolean> disableItemTabOpen = new Option(
            false,
            "No Tab Open",
            false,
            "If True, moving your mouse to the item tab on the right side won't open the itempane.", CAT_MISC);
    public Option<Boolean> keepopen = new Option(
            false,
            "Keep Itempane Open",
            false,
            "If true, the itempane will stay open after the gui is closed.", CAT_MISC);
    public Option<Boolean> itemStyle = new Option(
            true,
            "Circular Item Style",
            false,
            "Uses the circular item background style instead of the square style. Contrib: Calyps0", CAT_MISC);
    public Option<Boolean> hideEmptyPanes = new Option(
            true,
            "Hide GUI Filler Tooltips",
            false,
            "Hides the tooltip of glass panes in skyblock GUIs. Contrib: ThatGravyBoat", CAT_MISC);
    public Option<Boolean> guiButtonClicks = new Option(
            true,
            "Button Click Sounds",
            false,
            "Plays a click sound whenever various NEU GUIs are interacted with", CAT_MISC);
    public Option<Boolean> dungeonProfitLore = new Option(
            false,
            "Dungeon Profit in Lore",
            false,
            "If true, will show the dungeon profit on the tooltip of the 'reward chest' instead of as a GUI.", CAT_MISC);
    public Option<Boolean> auctionPriceInfo = new Option(
            true,
            "Price Info in Auction Lore",
            false,
            "If true, will show price information about an item inside the auction house item tooltip.", CAT_MISC);
    public Option<Boolean> useCustomTrade = new Option(
            true,
            "Custom Trade",
            false,
            "If true, uses the custom trade window for skyblock trades.", CAT_FEATURES);
    public Option<Boolean> invBazaarPrice = new Option(
            false,
            "Show Bazaar Price In Inventory",
            false,
            "If true, shows the bazaar price for the item you hover in your inventory.", CAT_MISC);
    public Option<Boolean> invAuctionPrice = new Option(
            false,
            "Show Auction Price In Inventory",
            false,
            "If true, shows the auction price for the item you hover in your inventory.", CAT_MISC);
    public Option<Boolean> dungeonBlocksEverywhere = new Option(
            false,
            "Show Dungeon Block Overlay Everywhere",
            false,
            "If true, will show the overlay for cracked bricks, etc. even when not in dungeons.", CAT_MISC);
    public Option<Boolean> disableDungeonBlocks = new Option(
            true,
            "Disable the dungeon blocks feature",
            false,
            "If true, the dungeon block overlay will be disabled. WARNING: May cause memory/fps issues on some machines", CAT_FEATURES);
    public Option<Boolean> slowDungeonBlocks = new Option(
            false,
            "Slowly Update Dungeon Block Textures",
            false,
            "If true, dungeon blocks will only update once every second.\n" +
                    "Use this option if you are having performance\n" +
                    "issues relating to the dungeon blocks.", CAT_MISC);
    public Option<Boolean> missingEnchantList = new Option(
            true,
            "Missing Enchant List",
            false,
            "If true, will show enchants that are missing on an enchanted item when LSHIFT is pressed.", CAT_FEATURES);
    public Option<Boolean> neuAuctionHouse = new Option(
            false,
            "NEU Auction House",
            false,
            "Enables the auction house which can be found using /neuah.\n" +
                    "Don't enable this option unless you use /neuah\n" +
                    "You *may* need to restart after enabling this for the auctions to download properly", CAT_FEATURES);
    public Option<Boolean> eventNotifications = new Option(
            true,
            "Skyblock Event Notifications",
            false,
            "Notifies you 5m (default) before and when favourited events (/neucalendar) start.", CAT_FEATURES);
    public Option<Boolean> showEventTimerInInventory = new Option(
            true,
            "Event Timer In Inventory",
            false,
            "Will show how long until the next event starts at the top of your inventory", CAT_FEATURES);
    public Option<Boolean> eventNotificationSounds = new Option(
            true,
            "Skyblock Event Notification Sounds",
            false,
            "Will play a sounds whenever a favourited event starts.", CAT_MISC);
    public Option<Boolean> spookyMorningNotification = new Option(
            true,
            "Spooky Festival Morning Notification",
            false,
            "During a spooky festival, will notify the player whenever it hits 7am", CAT_MISC);

    public Option<Boolean> accessoryBagOverlay = new Option(
            true,
            "Accessory Bag Overlay",
            false,
            "If true, will an overlay with useful information in your accessory bag.", CAT_FEATURES);
    public Option<Boolean> rodColours = new Option(
            true,
            "Custom Rod Line Colours",
            false,
            "If true, will use custom colours for fishing line rods in skyblock.", CAT_FEATURES);
    public Option<Double> paneGuiScale = new Option(
            0.0,
            "Pane GUI Scale",
            false,
            "Changes the GUI scale of the item pane. 0 = use game default. 1-4 = scale", FLAG_INT, 0, 4, CAT_SLIDERS);
    public Option<Double> paneWidthMult = new Option(
            1.0,
            "Pane Width",
            false,
            "Changes how wide the item and info panes are. Value between 0.5-1.5.", 0.5, 1.5, CAT_SLIDERS);
    public Option<Double> smoothAoteMillis = new Option(
            175.0,
            "Smooth AOTE Milliseconds",
            false,
            "How long teleporting with the AOTE takes. 0 = disable.", 0, 300, CAT_SLIDERS);
    public Option<Double> itemHighlightOpacity = new Option(
            178.0,
            "Item Highlight Opacity",
            false,
            "Changes the opacity of item highlights. Value between 0-255.", 0, 255, CAT_SLIDERS);
    public Option<Double> panePadding = new Option(
            10.0,
            "Pane Padding",
            false,
            "Changes the padding of the panes. Value between 0-20.", 0, 20, CAT_SLIDERS);
    public Option<Double> ahNotification = new Option(
            2.0,
            "AH Notification (Mins, 0 = off)",
            false,
            "Minutes before AH ends to notify. 0-10.", 0, 10, CAT_SLIDERS);
    public Option<Double> tooltipBorderOpacity = new Option(
            200.0,
            "Coloured Tooltip Border Opacity",
            false,
            "Coloured tooltips only apply to tooltips in my GUIs. Value between 0-255.", 0, 255, CAT_SLIDERS);
    public Option<Double> dynamicMenuBackgroundStyle = new Option(
            1.0,
            "SBMenu Background Style",
            false,
            "Style of the background used for the skyblock menu.", 0, 10, CAT_FEATURES);
    public Option<Double> dynamicMenuButtonStyle = new Option(
            1.0,
            "SBMenu Button Style",
            false,
            "Style of the buttons used for the skyblock menu.", 0, 10, CAT_FEATURES);
    public Option<Double> dungeonWinMillis = new Option(
            5000.0,
            "Dungeon Victory Screen Millis",
            false,
            "Changes how long the victory screen at the end of dungeons appears for. 0 = off", FLAG_INT, 0, 15000, CAT_SLIDERS);
    public Option<Double> eventNotificationBeforeSeconds = new Option(
            300.0,
            "Event Notification Before Seconds",
            false,
            "Changes how long before skyblock events will the 'starting in' notification show. 0 = off", FLAG_INT, 0, 1800, CAT_SLIDERS);

    public Option<String> itemBackgroundColour = new Option(
            "00:255:100:100:100",
            "Item BG Colour",
            false,
            "Item BG Colour",
            FLAG_COLOUR, CAT_COLOURS);
    public Option<String> itemFavouriteColour = new Option(
            "00:255:200:150:50",
            "Item BG Favourite Colour",
            false,
            "Item BG Favourite Colour",
            FLAG_COLOUR, CAT_COLOURS);
    public Option<String> paneBackgroundColour = new Option(
            "15:6:0:0:255",
            "Pane Background Colour",
            false,
            "Pane Background Colour",
            FLAG_COLOUR, CAT_COLOURS);
    public Option<String> treecapOverlayColour = new Option(
            "00:50:64:224:208",
            "Treecapitator Overlay Colour",
            false,
            "Treecapitator Overlay Colour",
            FLAG_COLOUR, CAT_COLOURS);
    public Option<String> wandOverlayColour = new Option(
            "00:50:64:224:208",
            "Builder's Wand Overlay Colour",
            false,
            "Builder's Wand Overlay Colour",
            FLAG_COLOUR, CAT_COLOURS);

    public Option<String> dungCrackedColour = new Option(
            "0:252:7:255:217",
            "Dungeon Cracked Brick Colour",
            false,
            "Dungeon Cracked Brick Colour",
            FLAG_COLOUR, CAT_COLOURS);
    public Option<String> dungDispenserColour = new Option(
            "0:255:255:76:0",
            "Dungeon Dispenser Colour",
            false,
            "Dungeon Dispenser Colour",
            FLAG_COLOUR, CAT_COLOURS);
    public Option<String> dungLeverColour = new Option(
            "0:252:24:249:255",
            "Dungeon Lever Colour",
            false,
            "Dungeon Lever Colour",
            FLAG_COLOUR, CAT_COLOURS);
    public Option<String> dungTripWireColour = new Option(
            "0:255:255:0:0",
            "Dungeon Trip Wire Colour",
            false,
            "Dungeon Trip Wire Colour",
            FLAG_COLOUR, CAT_COLOURS);
    public Option<String> dungChestColour = new Option(
            "0:255:0:163:36",
            "Dungeon Chest Colour",
            false,
            "Dungeon Chest Colour",
            FLAG_COLOUR, CAT_COLOURS);
    public Option<String> dungTrappedChestColour = new Option(
            "0:255:0:163:36",
            "Dungeon Trapped Chest Colour",
            false,
            "Dungeon Trapped Chest Colour",
            FLAG_COLOUR, CAT_COLOURS);
    public Option<String> dungBatColour = new Option(
            "0:255:12:255:0",
            "Dungeon Bat Colour",
            false,
            "Dungeon Bat Colour",
            FLAG_COLOUR, CAT_COLOURS);
    public Option<String> selfRodLineColour = new Option(
            "0:255:0:0:0",
            "Your Rod Line Colour",
            false,
            "Changes the colour of your rod's fishing line.\nContrib: ThatGravyBoat",
            FLAG_COLOUR, CAT_COLOURS);
    public Option<String> otherRodLineColour = new Option(
            "0:255:0:0:0",
            "Other Rod Line Colour",
            false,
            "Changes the colour of other players' rod's fishing line.\nContrib: ThatGravyBoat",
            FLAG_COLOUR, CAT_COLOURS);

    /**
     * OPTIONS THAT DON'T SHOW IN GUI
     */
    public Option<Boolean> dev = new Option(
            false,
            "Show Dev Options",
            true,
            "Dev Feature. Please don't use.", CAT_ALL);
    public Option<Boolean> loadedModBefore = new Option(
            false,
            "loadedModBefore",
            true,
            "loadedModBefore", CAT_ALL);
    public Option<Boolean> doRamNotif = new Option(
            true,
            "doRamNotif",
            false,
            "doRamNotif", CAT_ALL);
    public Option<Boolean> customTradePrices = new Option(
            true,
            "Trade Item Values",
            true,
            "If true, shows a window with the total item value of either side", CAT_ALL);
    public Option<Boolean> customTradePriceStyle = new Option(
            true,
            "Trade Prices Style",
            true,
            "Changes the style of the top item prices", CAT_ALL);
    public Option<String> selectedCape = new Option(
            "",
            "Selected Cape",
            true,
            "Selected Cape", CAT_ALL);
    public Option<Double> compareMode = new Option(
            0.0,
            "Compare Mode",
            false,
            "Compare Mode", CAT_ALL);
    public Option<Double> sortMode = new Option(
            0.0,
            "Sort Mode",
            false,
            "Sort Mode", CAT_ALL);
    public Option<ArrayList<Boolean>> compareAscending = new Option(
            Utils.createList(true, true, true),
            "Compare Ascending",
            false,
            "Compare Ascending", CAT_ALL);
    public Option<ArrayList<String>> favourites = new Option(
            new ArrayList<String>(),
            "Favourites",
            false,
            "Favourites", CAT_ALL);
    public Option<ArrayList<String>> eventFavourites = new Option(
            new ArrayList<String>(),
            "Event Favourites",
            false,
            "Event Favourites", CAT_ALL);
    public Option<Map<String, ArrayList<String>>> collectionLog = new Option(
            new HashMap<String, ArrayList<String>>(),
            "CollectionLog",
            false,
            "CollectionLog", CAT_ALL);
    public Option<ArrayList<String>> quickCommands = new Option(
            createDefaultQuickCommands(),
            "Quick Commands",
            false,
            "Quick Commands", CAT_ALL);
    public Option<String> overlaySearchBar = new Option(
            "",
            "OverlaySearchBar",
            false,
            "OverlaySearchBar", CAT_ALL);
    public Option<String> overlayQuickCommand = new Option(
            "",
            "OverlaySearchBar",
            false,
            "OverlaySearchBar", CAT_ALL);
    public Option<List<String>> enchantColours = new Option(
            Utils.createList("[a-zA-Z\\- ]+:\u003e:9:6",
                    "[a-zA-Z\\- ]+:\u003e:6:c",
                    "[a-zA-Z\\- ]+:\u003e:5:5",
                    "Experience:\u003e:3:5",
                    "Life Steal:\u003e:3:5",
                    "Scavenger:\u003e:3:5",
                    "Looting:\u003e:3:5"),
            "enchantColours",
            false,
            "enchantColours", CAT_ALL);

    //Dungeon Map Options
    public Option<Double> dmBorderSize = new Option(
            1.0,
            "Border Size",
            false,
            "Changes the size of the map border, without changing the size of the contents", 0, 5, CAT_ALL);
    public Option<Double> dmRoomSize = new Option(
            1.0,
            "Room Size",
            false,
            "Changes the size of rooms. Useful for higher dungeons with larger maps", 0, 5, CAT_ALL);
    public Option<Double> dmIconScale = new Option(
            1.0,
            "Icon Size",
            false,
            "Changes the scale of room indicators and player icons", 0.5, 3, CAT_ALL);
    public Option<Double> dmBorderStyle = new Option(
            0.0,
            "Border Style",
            false,
            "Various custom borders from various talented artists.\nUse 'custom' if your texture pack has a custom border", CAT_ALL);
    public Option<Boolean> dmEnable = new Option(
            true,
            "Show Dungeon Map",
            false,
            "Show/hide the NEU dungeon map", CAT_ALL);
    public Option<Boolean> dmCenterPlayer = new Option(
            false,
            "Map Center",
            false,
            "Center on rooms, or center on your player", CAT_ALL);
    public Option<Boolean> dmRotatePlayer = new Option(
            true,
            "Rotate with Player",
            false,
            "Rotate the map to face the same direction as your player", CAT_ALL);
    public Option<Boolean> dmOrientCheck = new Option(
            true,
            "Orient Checkmarks",
            false,
            "Checkmarks will always show vertically, regardless of rotation", CAT_ALL);
    public Option<Boolean> dmCenterCheck = new Option(
            false,
            "Center Checkmarks",
            false,
            "Checkmarks will show closer to the center of rooms", CAT_ALL);
    public Option<Double> dmPlayerHeads = new Option(
            0.0,
            "Player Icon Style",
            false,
            "Various player icon styles", CAT_ALL);
    public Option<Boolean> dmPlayerInterp = new Option(
            true,
            "Interpolate Far Players",
            false,
            "Will make players far away move smoothly", CAT_ALL);
    public Option<Double> dmCompat = new Option(
            0.0,
            "OpenGL Compatibility",
            false,
            "Compatiblity options for people with bad computers. ONLY use this if you know what you are doing, otherwise the map will look worse", CAT_ALL);
    public Option<String> dmBackgroundColour = new Option(
            "00:170:75:75:75",
            "Background Colour",
            false,
            "Colour of the map background. Supports opacity & chroma", FLAG_COLOUR, CAT_ALL);
    public Option<String> dmBorderColour = new Option(
            "00:0:0:0:0",
            "Border Colour",
            false,
            "Colour of the map border. Supports opacity & chroma. Turn off custom borders to see", FLAG_COLOUR, CAT_ALL);
    public Option<Boolean> dmChromaBorder = new Option(
            false,
            "Chroma Border Mode",
            false,
            "Applies a hue offset around the map border", CAT_ALL);
    public Option<Double> dmBackgroundBlur = new Option(
            3.0,
            "Background Blur Factor",
            false,
            "Changes the blur factor behind the map. Set to 0 to disable blur", CAT_ALL);
    public Option<Double> dmCenterX = new Option(
            8.5,
            "Center X (%)",
            false,
            "The horizontal position of the map", CAT_ALL);
    public Option<Double> dmCenterY = new Option(
            15.0,
            "Center Y (%)",
            false,
            "The vertical position of the map", CAT_ALL);

    private ArrayList<String> createDefaultQuickCommands() {
        ArrayList<String> arr = new ArrayList<>();
        arr.add("/warp home:Warp Home:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzljODg4MWU0MjkxNWE5ZDI5YmI2MWExNmZiMjZkMDU5OTEzMjA0ZDI2NWRmNWI0MzliM2Q3OTJhY2Q1NiJ9fX0=");
        arr.add("/warp hub:Warp Hub:eyJ0aW1lc3RhbXAiOjE1NTkyMTU0MTY5MDksInByb2ZpbGVJZCI6IjQxZDNhYmMyZDc0OTQwMGM5MDkwZDU0MzRkMDM4MzFiIiwicHJvZmlsZU5hbWUiOiJNZWdha2xvb24iLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Q3Y2M2Njg3NDIzZDA1NzBkNTU2YWM1M2UwNjc2Y2I1NjNiYmRkOTcxN2NkODI2OWJkZWJlZDZmNmQ0ZTdiZjgifX19");
        arr.add("/craft:Crafting Table:CRAFTING_TABLE");
        arr.add("/enderchest:Ender Chest:ENDER_CHEST");
        arr.add("/wardrobe:Wardrobe:LEATHER_CHESTPLATE");
        arr.add("/pets:Pets:BONE");
        arr.add("neucl:Collection Log:MAP");
        arr.add("neuah:NEU Auction House:GOLD_BLOCK");
        return arr;
    }

    public class Button {
        public String displayName;
        public String desc;
        public Runnable click;

        public Button(String displayName, String desc, Runnable click) {
            this.displayName = displayName;
            this.desc = desc;
            this.click = click;
        }
    }

    private transient List<Button> buttons = new ArrayList<>();

    {
        buttons.add(new Button("Open Config Folder", "Opens the config folder. Be careful.", () -> {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (NotEnoughUpdates.INSTANCE.manager.configFile.getParentFile().exists()) {
                    try {
                        desktop.open(NotEnoughUpdates.INSTANCE.manager.configFile.getParentFile());
                    } catch (IOException ignored) {
                    }
                }
            }
        }));

        buttons.add(new Button("Edit Gui Positions", "Allows you to change the position of the search bar, etc.", () -> {
            Minecraft.getMinecraft().displayGuiScreen(new NEUOverlayPlacements());
        }));

        buttons.add(new Button("Edit Enchant Colours", "Allows you to change the colour of any enchant at any level.", () -> {
            Minecraft.getMinecraft().displayGuiScreen(new GuiEnchantColour());
        }));


        buttons.add(new Button("Edit Dungeon Map", "Allows you to configure the NEU dungeon map.", () -> {
            Minecraft.getMinecraft().displayGuiScreen(new GuiDungeonMapEditor());
        }));
    }

    public List<Button> getButtons() {
        return buttons;
    }

    public List<Option> getOptions() {
        List<Option> options = new ArrayList<>();

        //Pane width near top so less scuffed
        tryAddOption(paneWidthMult, options);
        //Buttons
        tryAddOption(enableItemEditing, options);
        tryAddOption(onlyShowOnSkyblock, options);
        tryAddOption(showVanillaItems, options);
        tryAddOption(showQuickCommands, options);
        tryAddOption(hidePotionEffect, options);
        tryAddOption(hideEmptyPanes, options);
        tryAddOption(guiButtonClicks, options);
        tryAddOption(advancedPriceInfo, options);
        tryAddOption(showUpdateMsg, options);
        tryAddOption(tooltipBorderColours, options);
        tryAddOption(disableAhScroll, options);
        tryAddOption(hideApiKey, options);
        tryAddOption(streamerMode, options);
        tryAddOption(disableTreecapOverlay, options);
        tryAddOption(disableWandOverlay, options);
        tryAddOption(wandBlockCount, options);
        tryAddOption(autoupdate, options);
        tryAddOption(cacheRenderedItempane, options);
        tryAddOption(itemStyle, options);
        tryAddOption(keepopen, options);
        tryAddOption(disableItemTabOpen, options);
        tryAddOption(dungeonProfitLore, options);
        tryAddOption(auctionPriceInfo, options);
        tryAddOption(useCustomTrade, options);
        tryAddOption(customTradePrices, options);
        tryAddOption(customTradePriceStyle, options);
        tryAddOption(invBazaarPrice, options);
        tryAddOption(invAuctionPrice, options);
        tryAddOption(dungeonBlocksEverywhere, options);
        tryAddOption(disableDungeonBlocks, options);
        tryAddOption(slowDungeonBlocks, options);
        tryAddOption(missingEnchantList, options);
        tryAddOption(accessoryBagOverlay, options);
        tryAddOption(rodColours, options);
        tryAddOption(neuAuctionHouse, options);
        tryAddOption(eventNotifications, options);
        tryAddOption(showEventTimerInInventory, options);
        tryAddOption(spookyMorningNotification, options);
        //Sliders
        tryAddOption(paneGuiScale, options);
        tryAddOption(smoothAoteMillis, options);
        tryAddOption(bgBlurFactor, options);
        tryAddOption(ahNotification, options);
        tryAddOption(panePadding, options);
        tryAddOption(tooltipBorderOpacity, options);
        tryAddOption(dynamicMenuBackgroundStyle, options);
        tryAddOption(dynamicMenuButtonStyle, options);
        tryAddOption(dungeonWinMillis, options);
        tryAddOption(eventNotificationBeforeSeconds, options);
        //Text
        tryAddOption(apiKey, options);
        //Colour
        tryAddOption(paneBackgroundColour, options);
        tryAddOption(itemBackgroundColour, options);
        tryAddOption(itemFavouriteColour, options);
        tryAddOption(treecapOverlayColour, options);
        tryAddOption(wandOverlayColour, options);
        tryAddOption(selfRodLineColour, options);
        tryAddOption(otherRodLineColour, options);

        tryAddOption(dungCrackedColour, options);
        tryAddOption(dungDispenserColour, options);
        tryAddOption(dungLeverColour, options);
        tryAddOption(dungTripWireColour, options);
        tryAddOption(dungChestColour, options);
        tryAddOption(dungTrappedChestColour, options);
        tryAddOption(dungBatColour, options);

        return options;
    }

    private void tryAddOption(Option<?> option, List<Option> list) {
        if (!option.secret) {// || dev.value) {
            list.add(option);
        }
    }

    public static class Option<T> implements Serializable {
        public T value;
        public final transient T defaultValue;
        public final transient String displayName;
        public final transient boolean secret;
        public final transient String desc;
        public final transient int flags;
        public final transient double minValue;
        public final transient double maxValue;
        public final transient int category;
        public final transient ArrayList<String> tags;

        public Option(T defaultValue, String displayName, boolean secret, String desc, int category) {
            this(defaultValue, displayName, secret, desc, 0, 0, 100, category);
        }

        public Option(T defaultValue, String displayName, boolean secret, String desc, int flags, int category) {
            this(defaultValue, displayName, secret, desc, flags, 0, 100, category);
        }

        public Option(T defaultValue, String displayName, boolean secret, String desc, double minValue, double maxValue, int category) {
            this(defaultValue, displayName, secret, desc, 0, minValue, maxValue, category);
        }

        public Option(T defaultValue, String displayName, boolean secret, String desc, int flags, double minValue, double maxValue, int category) {
            this.value = defaultValue;
            this.defaultValue = defaultValue;
            this.displayName = displayName;
            this.secret = secret;
            this.desc = desc;
            this.flags = flags;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.category = category;

            this.tags = new ArrayList<>();
            for(String s : displayName.split(" ")) {
                s = s.replaceAll("[^A-Za-z]", "").toLowerCase();
                if(s.length() > 0) {
                    tags.add(s);
                }
            }
            for(String s : desc.split(" ")) {
                s = s.replaceAll("[^A-Za-z]", "").toLowerCase();
                if(s.length() >= 4) {
                    tags.add(s);
                }
            }
        }

        public void setValue(String value) {
            if (this.value instanceof Boolean) {
                ((Option<Boolean>) this).value = Boolean.valueOf(value);
            } else if (this.value instanceof Double) {
                ((Option<Double>) this).value = Double.valueOf(value);
            } else if (this.value instanceof String) {
                ((Option<String>) this).value = value;
            }
        }
    }

    public static JsonSerializer<Option<?>> createSerializer() {
        return (src, typeOfSrc, context) -> {
            if (src.secret && src.defaultValue.equals(src.value)) {
                return null;
            }
            return context.serialize(src.value);
        };
    }

    public static JsonDeserializer<Option<?>> createDeserializer() {
        return (json, typeOfT, context) -> {
            try {
                return new Option(context.deserialize(json, Object.class), "unknown", false, "unknown", CAT_ALL);
            } catch (Exception e) {
                return null;
            }
        };
    }

    public static Options loadFromFile(Gson gson, File file) throws IOException {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            Options oLoad = gson.fromJson(reader, Options.class);
            Options oDefault = new Options();
            if (oLoad == null) return oDefault;

            for (Field f : Options.class.getDeclaredFields()) {
                try {
                    if (((Option<?>) f.get(oDefault)).value instanceof List) {
                        //If the default size of the list is greater than the loaded size, use the default value.
                        //if(((List<?>)((Option)f.get(oDefault)).value).size() > ((List<?>)((Option)f.get(oLoad)).value).size()) {
                        //    continue;
                        //}
                    }
                    if(((Option<?>) f.get(oDefault)).value.getClass().isAssignableFrom(((Option<?>) f.get(oLoad)).value.getClass())) {
                        ((Option) f.get(oDefault)).value = ((Option) f.get(oLoad)).value;
                    }
                } catch (Exception e) {
                }
            }
            return oDefault;
        }
    }


    public void saveToFile(Gson gson, File file) throws IOException {
        file.createNewFile();

        try(BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(this));
        }
    }
}