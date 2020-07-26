package io.github.moulberry.notenoughupdates.options;

import com.google.gson.*;
import io.github.moulberry.notenoughupdates.GuiEnchantColour;
import io.github.moulberry.notenoughupdates.NEUOverlayPlacements;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.mbgui.MBAnchorPoint;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.util.vector.Vector2f;

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

    public Option<Boolean> enableItemEditing = new Option(
            false,
            "Enable Item Editing",
            true,
            "Dev Feature.");
    public Option<Boolean> onlyShowOnSkyblock = new Option(
            true,
            "Only Show On Skyblock",
            false,
            "NEU Overlay only appears when you are playing Skyblock.");
    public Option<Boolean> hidePotionEffect = new Option(
            true,
            "Hide Potion Effects",
            false,
            "Potion effects are hidden in the inventory GUI. Contrib: All the gamers that play on GUI AUTO");
    public Option<Boolean> showQuickCommands = new Option(
            true,
            "Quick Commands",
            false,
            "Shows QuickCommands™ above search bar.");
    public Option<Boolean> showUpdateMsg = new Option(
            true,
            "Show Update Notifs",
            false,
            "Shows update messages if NEU is out-of-date.");
    public Option<Boolean> tooltipBorderColours = new Option(
            true,
            "Coloured Tooltip Borders",
            false,
            "Makes the border of tooltips coloured. (Only NEU Tooltips)");
    public Option<Boolean> advancedPriceInfo = new Option(
            false,
            "Adv. Item Price Info",
            false,
            "Shows some extra information about item prices.");
    public Option<Boolean> cacheRenderedItempane = new Option(
            true,
            "Cache Itempane",
            false,
            "Caches the drawn itempane, drastically improving performance. Animated textures will not work.");
    public Option<Boolean> streamerMode = new Option(
            false,
            "Streamer Mode",
            false,
            "Hides or randomises some stuff on your screen to prevent sniping.");
    public Option<Boolean> hideApiKey = new Option(
            false,
            "Hide Apikey Setting",
            false,
            "Hides the Apikey setting (please try not to leak Apikey if you're recording)");
    public Option<Boolean> quickAHUpdate = new Option(
            false,
            "NeuAH Quick Update",
            false,
            "Will instantly update the whole AH when an api update is detected (aka as fast as possible). Warning: Uses lots of data.");
    public Option<Double> bgBlurFactor = new Option(
            5.0,
            "Background Blur",
            false,
            "Changes the strength of pane background blur. 0-50.", 0, 50);
    public Option<String> apiKey = new Option(
            "",
            "Api Key",
            false,
            "Type /api new to receive key and put it here.");
    public Option<Boolean> autoupdate = new Option(
            true,
            "Automatically Update Items",
            false,
            "If true, updated items will automatically download from the remote repository when you start the game. \nHIGHLY RECOMMENDED.");
    public Option<Boolean> quickcommandMousePress = new Option(
            false,
            "QuickCommand on Mouse Press",
            false,
            "If true, quickcommands will trigger on mouse down instead of mouse up.");
    public Option<Boolean> disableItemTabOpen = new Option(
            false,
            "No Tab Open",
            false,
            "If True, moving your mouse to the item tab on the right side won't open the itempane.");
    public Option<Boolean> keepopen = new Option(
            false,
            "Keep Itempane Open",
            false,
            "If true, the itempane will stay open after the gui is closed.");
    public Option<Boolean> itemStyle = new Option(
            true,
            "Circular Item Style",
            false,
            "Uses the circular item background style instead of the square style. Contrib: Calyps0");
    public Option<Boolean> hideEmptyPanes = new Option(
            true,
            "Hide GUI Filler Tooltips",
            false,
            "Hides the tooltip of glass panes in skyblock GUIs. Contrib: ThatGravyBoat");
    public Option<Double> paneWidthMult = new Option(
            1.0,
            "Pane Width",
            false,
            "Changes how wide the item and info panes are. Value between 0.5-1.5.", 0.5, 1.5);
    public Option<Double> bgOpacity = new Option(
            30.0,
            "Pane Background Opacity",
            false,
            "Changes the background colour opacity of item and info panes. Value between 0-255.", 0, 255);
    public Option<Double> fgOpacity = new Option(
            255.0,
            "Item Background Opacity",
            false,
            "Changes the opacity of item background. Value between 0-255.", 0, 255);
    public Option<Double> itemHighlightOpacity = new Option(
            178.0,
            "Item Highlight Opacity",
            false,
            "Changes the opacity of item highlights. Value between 0-255.", 0, 255);
    public Option<Double> panePadding = new Option(
            10.0,
            "Pane Padding",
            false,
            "Changes the padding of the panes. Value between 0-20.", 0, 20);
    public Option<Double> ahNotification = new Option(
            2.0,
            "AH Notification (Mins, 0 = off)",
            false,
            "Minutes before AH ends to notify. 0-10.", 0, 10);
    public Option<Double> tooltipBorderOpacity = new Option(
            200.0,
            "Coloured Tooltip Border Opacity",
            false,
            "Coloured tooltips only apply to tooltips in my GUIs. Value between 0-255.", 0, 255);

    /**
     * OPTIONS THAT DON'T SHOW IN GUI
     */
    public Option<Boolean> dev = new Option(
            false,
            "Show Dev Options",
            true,
            "Dev Feature. Please don't use.");
    public Option<String> selectedCape = new Option(
            "",
            "Selected Cape",
            true,
            "Selected Cape");
    public Option<Double> compareMode = new Option(
            0.0,
            "Compare Mode",
            false,
            "Compare Mode");
    public Option<Double> sortMode = new Option(
            0.0,
            "Sort Mode",
            false,
            "Sort Mode");
    public Option<ArrayList<Boolean>> compareAscending = new Option(
            Utils.createList(true, true, true),
            "Compare Ascending",
            false,
            "Compare Ascending");
    public Option<ArrayList<String>> favourites = new Option(
            new ArrayList<String>(),
            "Favourites",
            false,
            "Favourites");
    public Option<Map<String, ArrayList<String>>> collectionLog = new Option(
            new HashMap<String, ArrayList<String>>(),
            "CollectionLog",
            false,
            "CollectionLog");
    public Option<ArrayList<String>> quickCommands = new Option(
            createDefaultQuickCommands(),
            "Quick Commands",
            false,
            "Quick Commands");
    public Option<String> overlaySearchBar = new Option(
            "",
            "OverlaySearchBar",
            false,
            "OverlaySearchBar");
    public Option<String> overlayQuickCommand = new Option(
            "",
            "OverlaySearchBar",
            false,
            "OverlaySearchBar");
    public Option<List<String>> enchantColours = new Option(
            Utils.createList("[a-zA-Z ]+:\u003e:9:6",
                                    "[a-zA-Z ]+:\u003e:6:c",
                                    "[a-zA-Z ]+:\u003e:5:5",
                                    "Experience:\u003e:3:5",
                                    "Life Steal:\u003e:3:5",
                                    "Scavenger:\u003e:3:5",
                                    "Looting:\u003e:3:5"),
            "enchantColours",
            false,
            "enchantColours");

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
            if(Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if(NotEnoughUpdates.INSTANCE.manager.configFile.getParentFile().exists()) {
                    try {
                        desktop.open(NotEnoughUpdates.INSTANCE.manager.configFile.getParentFile());
                    } catch(IOException ignored) {}
                }
            }
        }));

        buttons.add(new Button("Edit Gui Positions", "Allows you to change the position of the search bar, etc.", () -> {
            Minecraft.getMinecraft().displayGuiScreen(new NEUOverlayPlacements());
        }));


        buttons.add(new Button("Edit Enchant Colours", "Allows you to change the colour of any enchant at any level.", () -> {
            Minecraft.getMinecraft().displayGuiScreen(new GuiEnchantColour());
        }));
    }

    public List<Button> getButtons() {
        return buttons;
    }

    public List<Option> getOptions() {
        List<Option> options = new ArrayList<>();

        //Buttons
        tryAddOption(enableItemEditing, options);
        tryAddOption(onlyShowOnSkyblock, options);
        tryAddOption(showQuickCommands, options);
        tryAddOption(hidePotionEffect, options);
        tryAddOption(hideEmptyPanes, options);
        //tryAddOption(advancedPriceInfo, options);
        tryAddOption(showUpdateMsg, options);
        tryAddOption(tooltipBorderColours, options);
        tryAddOption(hideApiKey, options);
        tryAddOption(streamerMode, options);
        tryAddOption(quickAHUpdate, options);
        tryAddOption(autoupdate, options);
        tryAddOption(cacheRenderedItempane, options);
        tryAddOption(itemStyle, options);
        tryAddOption(keepopen, options);
        tryAddOption(disableItemTabOpen, options);
        //Sliders
        tryAddOption(bgBlurFactor, options);
        tryAddOption(paneWidthMult, options);
        tryAddOption(ahNotification, options);
        tryAddOption(bgOpacity, options);
        tryAddOption(fgOpacity, options);
        tryAddOption(itemHighlightOpacity, options);
        tryAddOption(panePadding, options);
        tryAddOption(tooltipBorderOpacity, options);
        //Text
        tryAddOption(apiKey, options);

        return options;
    }

    private void tryAddOption(Option<?> option, List<Option> list) {
        if(!option.secret) {// || dev.value) {
            list.add(option);
        }
    }

    public static class Option<T> implements Serializable {
        public T value;
        public final transient T defaultValue;
        public final transient String displayName;
        public final transient boolean secret;
        public final transient String desc;
        public final transient double minValue;
        public final transient double maxValue;

        public Option(T defaultValue, String displayName, boolean secret, String desc) {
            this(defaultValue, displayName, secret, desc, 0, 100);
        }

        public Option(T defaultValue, String displayName, boolean secret, String desc, double minValue, double maxValue) {
            this.value = defaultValue;
            this.defaultValue = defaultValue;
            this.displayName = displayName;
            this.secret = secret;
            this.desc = desc;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        public void setValue(String value) {
            if(this.value instanceof Boolean) {
                ((Option<Boolean>) this).value = Boolean.valueOf(value);
            } else if(this.value instanceof Double) {
                ((Option<Double>)this).value = Double.valueOf(value);
            } else if(this.value instanceof String) {
                ((Option<String>)this).value = value;
            }
        }
    }

    public static JsonSerializer<Option<?>> createSerializer() {
        return (src, typeOfSrc, context) -> {
            if(src.secret && src.defaultValue.equals(src.value)) {
                return null;
            }
            return context.serialize(src.value);
        };
    }

    public static JsonDeserializer<Option<?>> createDeserializer() {
        return (json, typeOfT, context) -> {
            try {
                return new Option(context.deserialize(json, Object.class), "unknown", false, "unknown");
            } catch(Exception e) {
                return null;
            }
        };
    }

    public static Options loadFromFile(Gson gson, File file) throws IOException {
        InputStream in = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        Options oLoad = gson.fromJson(reader, Options.class);
        Options oDefault = new Options();
        if(oLoad == null) return oDefault;

        for(Field f : Options.class.getDeclaredFields()) {
            try {
                if(((Option)f.get(oDefault)).value instanceof List) {
                    //If the default size of the list is greater than the loaded size, use the default value.
                    //if(((List<?>)((Option)f.get(oDefault)).value).size() > ((List<?>)((Option)f.get(oLoad)).value).size()) {
                    //    continue;
                    //}
                }
                ((Option)f.get(oDefault)).value = ((Option)f.get(oLoad)).value;
            } catch (Exception e) { }
        }
        return oDefault;
    }

    public void saveToFile(Gson gson, File file) throws IOException {
        file.createNewFile();

        try(BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(this));
        }
    }


}
