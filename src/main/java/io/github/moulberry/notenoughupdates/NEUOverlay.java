package io.github.moulberry.notenoughupdates;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.realmsclient.gui.ChatFormatting;
import info.bliki.htmlcleaner.TagNode;
import info.bliki.wiki.filter.Encoder;
import info.bliki.wiki.model.Configuration;
import info.bliki.wiki.model.ImageFormat;
import info.bliki.wiki.model.WikiModel;
import info.bliki.wiki.tags.HTMLBlockTag;
import info.bliki.wiki.tags.HTMLTag;
import info.bliki.wiki.tags.IgnoreTag;
import info.bliki.wiki.tags.MathTag;
import io.github.moulberry.notenoughupdates.itemeditor.GuiElementTextField;
import io.github.moulberry.notenoughupdates.itemeditor.NEUItemEditor;
import io.github.moulberry.notenoughupdates.options.Options;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderEnderman;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NEUOverlay extends Gui {

    private NEUManager manager;

    private ResourceLocation itemPaneTabArrow = new ResourceLocation("notenoughupdates:item_pane_tab_arrow.png");
    private ResourceLocation prev = new ResourceLocation("notenoughupdates:prev.png");
    private ResourceLocation next = new ResourceLocation("notenoughupdates:next.png");
    private ResourceLocation item_edit = new ResourceLocation("notenoughupdates:item_edit.png");
    private ResourceLocation close = new ResourceLocation("notenoughupdates:close.png");
    private ResourceLocation settings = new ResourceLocation("notenoughupdates:settings.png");
    private ResourceLocation off = new ResourceLocation("notenoughupdates:off.png");
    private ResourceLocation on = new ResourceLocation("notenoughupdates:on.png");
    private ResourceLocation help = new ResourceLocation("notenoughupdates:help.png");

    private ResourceLocation sort_all = new ResourceLocation("notenoughupdates:sort_all.png");
    private ResourceLocation sort_mob = new ResourceLocation("notenoughupdates:sort_mob.png");
    private ResourceLocation sort_pet = new ResourceLocation("notenoughupdates:sort_pet.png");
    private ResourceLocation sort_tool = new ResourceLocation("notenoughupdates:sort_weapon.png");
    private ResourceLocation sort_armor = new ResourceLocation("notenoughupdates:sort_armor.png");
    private ResourceLocation sort_accessory = new ResourceLocation("notenoughupdates:sort_accessory.png");
    private ResourceLocation sort_all_active = new ResourceLocation("notenoughupdates:sort_all_active.png");
    private ResourceLocation sort_mob_active = new ResourceLocation("notenoughupdates:sort_mob_active.png");
    private ResourceLocation sort_pet_active = new ResourceLocation("notenoughupdates:sort_pet_active.png");
    private ResourceLocation sort_tool_active = new ResourceLocation("notenoughupdates:sort_weapon_active.png");
    private ResourceLocation sort_armor_active = new ResourceLocation("notenoughupdates:sort_armor_active.png");
    private ResourceLocation sort_accessory_active = new ResourceLocation("notenoughupdates:sort_accessory_active.png");


    private ResourceLocation order_alphabetical = new ResourceLocation("notenoughupdates:order_alphabetical.png");
    private ResourceLocation order_rarity = new ResourceLocation("notenoughupdates:order_rarity.png");
    private ResourceLocation order_alphabetical_active = new ResourceLocation("notenoughupdates:order_alphabetical_active.png");
    private ResourceLocation order_rarity_active = new ResourceLocation("notenoughupdates:order_rarity_active.png");
    private ResourceLocation ascending_overlay = new ResourceLocation("notenoughupdates:ascending_overlay.png");
    private ResourceLocation descending_overlay = new ResourceLocation("notenoughupdates:descending_overlay.png");



    private String mobRegex = ".*?((_MONSTER)|(_ANIMAL)|(_MINIBOSS)|(_BOSS)|(_SC))$";
    private String petRegex = ".*?;[0-4]$";

    private ResourceLocation[] sortIcons = new ResourceLocation[] {
        sort_all, sort_mob, sort_pet, sort_tool, sort_armor, sort_accessory
    };
    private ResourceLocation[] sortIconsActive = new ResourceLocation[] {
            sort_all_active, sort_mob_active, sort_pet_active, sort_tool_active, sort_armor_active, sort_accessory_active
    };

    private ResourceLocation[] orderIcons = new ResourceLocation[] {
            order_alphabetical, order_rarity
    };
    private ResourceLocation[] orderIconsActive = new ResourceLocation[] {
            order_alphabetical_active, order_rarity_active
    };

    private final WikiModel wikiModel;

    private final int searchBarXSize = 200;
    private final int searchBarYOffset = 10;
    private final int searchBarYSize = 40;
    private final int searchBarPadding = 2;

    private final int ZOOM_FACTOR = 2;
    private final int IMAGE_WIDTH = 400;

    private final int boxPadding = 15;
    private final int itemPadding = 4;
    private final int itemSize = 16;

    private Color bg = new Color(90, 90, 140, 50);
    private Color fg = new Color(100,100,100, 255);

    private String informationPaneTitle;
    private ResourceLocation informationPaneImage = null;
    private BufferedImage webpageLoadTemp = null;
    private int informationPaneImageHeight = 0;
    private String[] informationPane;
    private AtomicInteger webpageAwaitID = new AtomicInteger(-1);
    private boolean configOpen = false;

    private static final int SCROLL_AMOUNT = 50;
    private LerpingInteger scrollHeight = new LerpingInteger(0);

    private TreeSet<JsonObject> searchedItems = null;
    private JsonObject[] searchedItemsArr = null;

    private boolean itemPaneOpen = false;

    private int page = 0;

    private LerpingFloat itemPaneOffsetFactor = new LerpingFloat(1);
    private LerpingInteger itemPaneTabOffset = new LerpingInteger(20, 50);
    private LerpingFloat infoPaneOffsetFactor = new LerpingFloat(0);

    private boolean searchMode = false;
    private long millisLastLeftClick = 0;

    boolean mouseDown = false;

    private boolean searchBarHasFocus = false;
    GuiTextField textField = new GuiTextField(0, null, 0, 0, 0, 0);

    Map<Options.Option<?>, GuiElementTextField> textConfigMap = new HashMap<>();

    private static final int COMPARE_MODE_ALPHABETICAL = 0;
    private static final int COMPARE_MODE_RARITY = 1;

    private static final int SORT_MODE_ALL = 0;
    private static final int SORT_MODE_MOB = 1;
    private static final int SORT_MODE_PET = 2;
    private static final int SORT_MODE_TOOL = 3;
    private static final int SORT_MODE_ARMOR = 4;
    private static final int SORT_MODE_ACCESSORY = 5;

    private ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());

    public NEUOverlay(NEUManager manager) {
        this.manager = manager;
        textField.setFocused(true);
        textField.setCanLoseFocus(false);

        Configuration conf = new Configuration();
        conf.addTokenTag("img", new HTMLTag("img"));
        conf.addTokenTag("code", new HTMLTag("code"));
        conf.addTokenTag("span", new AllowEmptyHTMLTag("span"));
        conf.addTokenTag("table", new HTMLBlockTag("table", Configuration.SPECIAL_BLOCK_TAGS+"span|"));
        conf.addTokenTag("infobox", new IgnoreTag("infobox"));
        conf.addTokenTag("tabber", new IgnoreTag("tabber"));
        conf.addTokenTag("kbd", new HTMLTag("kbd"));
        wikiModel = new WikiModel(conf,"https://hypixel-skyblock.fandom.com/wiki/Special:Filepath/${image}",
                "https://hypixel-skyblock.fandom.com/wiki/${title}") {
            {
                TagNode.addAllowedAttribute("style");
                TagNode.addAllowedAttribute("src");
            }

            protected String createImageName(ImageFormat imageFormat) {
                String imageName = imageFormat.getFilename();
                /*String sizeStr = imageFormat.getWidthStr();
                if (sizeStr != null) {
                    imageName = sizeStr + '-' + imageName;
                }*/
                if (imageName.endsWith(".svg")) {
                    imageName += ".png";
                }
                imageName = Encoder.encodeUrl(imageName);
                if (replaceColon()) {
                    imageName = imageName.replace(':', '/');
                }
                return imageName;
            }

            public void parseInternalImageLink(String imageNamespace, String rawImageLink) {
                rawImageLink = rawImageLink.replaceFirst("\\|x([0-9]+)px", "\\|$1x$1px");
                if(!rawImageLink.split("\\|")[0].toLowerCase().endsWith(".jpg")) {
                    super.parseInternalImageLink(imageNamespace, rawImageLink);
                }
            }
        };

    }

    public void reset() {
        searchBarHasFocus = false;
        if(!(searchMode || (manager.config.keepopen.value && itemPaneOpen))) {
            itemPaneOpen = false;
            itemPaneOffsetFactor.setValue(1);
            itemPaneTabOffset.setValue(20);
        }
    }

    public void showInfo(JsonObject item) {
        if(item.has("info") && item.has("infoType")) {
            JsonArray lore = item.get("info").getAsJsonArray();
            String[] loreA = new String[lore.size()];
            for (int i = 0; i < lore.size(); i++) loreA[i] = lore.get(i).getAsString();
            displayInformationPane(item.get("displayname").getAsString(), item.get("infoType").getAsString(), loreA);
        }
    }

    /**
     * Handles the mouse input, cancelling the forge event if a NEU gui element is clicked.
     */
    public boolean mouseInput() {
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        int mouseX = Mouse.getX() / scaledresolution.getScaleFactor();
        int mouseY = height - Mouse.getY() / scaledresolution.getScaleFactor();

        if(Mouse.getEventButtonState()) {
            mouseDown = true;
        } else if(Mouse.getEventButton() != -1) {
            mouseDown = false;
        }

        //Unfocuses the search bar by default. Search bar is focused if the click is on the bar itself.
        if(Mouse.getEventButtonState()) setSearchBarFocus(false);

        //Item selection (right) gui
        if(mouseX > width*getItemPaneOffsetFactor()) {
            if(!Mouse.getEventButtonState()) return true; //End early if the mouse isn't pressed, but still cancel event.

            AtomicBoolean clickedItem = new AtomicBoolean(false);
            iterateItemSlots((x, y) -> {
                if(mouseX >= x-1 && mouseX <= x+itemSize+1) {
                    if(mouseY >= y-1 && mouseY <= y+itemSize+1) {
                        clickedItem.set(true);
                        //TODO: Do something when clicking on items :)
                        int id = getSlotId(x, y);
                        JsonObject item = getSearchedItemPage(id);
                        if (item != null) {
                            if(Mouse.getEventButton() == 0) {
                                manager.showRecipe(item);
                            } else if(Mouse.getEventButton() == 1) {
                                showInfo(item);
                            } else if(Mouse.getEventButton() == 2) {
                                textField.setText("id:"+item.get("internalname").getAsString());
                                updateSearch();
                                searchMode = true;
                            }
                        }
                    }
                }
            });
            if(!clickedItem.get()) {
                int paneWidth = (int)(width/3*getWidthMult());
                int leftSide = (int)(width*getItemPaneOffsetFactor());
                int rightSide = leftSide+paneWidth-boxPadding-getItemBoxXPadding();

                int scaledYSize = searchBarYSize/scaledresolution.getScaleFactor();

                if(mouseY >= boxPadding && mouseY <= boxPadding+getSearchBarYSize()) {
                    int leftPrev = leftSide+boxPadding+getItemBoxXPadding();
                    if(mouseX > leftPrev && mouseX < leftPrev+scaledYSize*400/160) { //"Previous" button
                        page--;
                    }
                    int rightNext = leftSide+paneWidth-boxPadding-getItemBoxXPadding();
                    if(mouseX > rightNext-scaledYSize*400/160 && mouseX < rightNext) { //"Next" button
                        page++;
                    }
                }

                float sortIconsMinX = (sortIcons.length+orderIcons.length)*(itemSize+itemPadding)+itemSize;
                float availableX = rightSide-(leftSide+boxPadding+getItemBoxXPadding());
                float sortOrderScaleFactor = Math.min(1, availableX / sortIconsMinX);

                int scaledItemSize = (int)(itemSize*sortOrderScaleFactor);
                int scaledItemPaddedSize = (int)((itemSize+itemPadding)*sortOrderScaleFactor);
                int iconTop = height-boxPadding-(itemSize+scaledItemSize)/2-1;

                if(mouseY >= iconTop && mouseY <= iconTop+scaledItemSize) {
                    for(int i=0; i<orderIcons.length; i++) {
                        int orderIconX = leftSide+boxPadding+getItemBoxXPadding()+i*scaledItemPaddedSize;
                        if(mouseX >= orderIconX && mouseX <= orderIconX+scaledItemSize) {
                            if(Mouse.getEventButton() == 0) {
                                manager.config.compareMode.value = new Double(i);
                                updateSearch();
                            } else if(Mouse.getEventButton() == 1) {
                                manager.config.compareAscending.value.set(i, !manager.config.compareAscending.value.get(i));
                                updateSearch();
                            }
                        }
                    }

                    for(int i=0; i<sortIcons.length; i++) {
                        int sortIconX = rightSide-scaledItemSize-i*scaledItemPaddedSize;
                        if(mouseX >= sortIconX && mouseX <= sortIconX+scaledItemSize) {
                            manager.config.sortMode.value = new Double(i);
                            updateSearch();
                        }
                    }
                }
            }
            return true;
        }

        if(Mouse.getEventButton() == 2) {
            Slot slot = ((GuiContainer)Minecraft.getMinecraft().currentScreen).getSlotUnderMouse();
            if(slot != null) {
                ItemStack hover = slot.getStack();
                if(hover != null) {
                    textField.setText("id:"+manager.getInternalNameForItem(hover));
                    updateSearch();
                    searchMode = true;
                }
            }
        }

        //Left gui
        if(mouseX < width*infoPaneOffsetFactor.getValue()) {
            int dWheel = Mouse.getEventDWheel();

            if(dWheel < 0) {
                scrollHeight.setTarget(scrollHeight.getTarget()+SCROLL_AMOUNT);
                scrollHeight.resetTimer();
                return true;
            } else if(dWheel > 0) {
                scrollHeight.setTarget(scrollHeight.getTarget()-SCROLL_AMOUNT);
                scrollHeight.resetTimer();
                return true;
            }
        }

        if(mouseX > width*getInfoPaneOffsetFactor()-22 && mouseX < width*getInfoPaneOffsetFactor()-6) {
            if(mouseY > 7 && mouseY < 23) {
                if(Mouse.getEventButtonState() && Mouse.getEventButton() < 2) { //Left or right click up
                    informationPaneTitle = null;
                    informationPaneImage = null;
                    informationPane = new String[0];
                    configOpen = false;
                }
            }
        }

        //Search bar
        if(mouseX >= width/2 - searchBarXSize/2 && mouseX <= width/2 + searchBarXSize/2) {
            if(mouseY >= height - searchBarYOffset - getSearchBarYSize() &&
                    mouseY <= height - searchBarYOffset) {
                if(Mouse.getEventButtonState()) {
                    setSearchBarFocus(true);
                    if(Mouse.getEventButton() == 1) { //Right mouse button down
                        textField.setText("");
                        updateSearch();
                    } else {
                        if(System.currentTimeMillis() - millisLastLeftClick < 300) {
                            searchMode = !searchMode;
                        }
                        textField.setCursorPosition(getClickedIndex(mouseX, mouseY));
                        millisLastLeftClick = System.currentTimeMillis();
                    }
                }
                return true;
            }
        }

        int paddingUnscaled = searchBarPadding/scaledresolution.getScaleFactor();
        int topTextBox = height - searchBarYOffset - getSearchBarYSize();
        int iconSize = getSearchBarYSize()+paddingUnscaled*2;
        if(paddingUnscaled < 1) paddingUnscaled = 1;

        if(mouseY > topTextBox - paddingUnscaled && mouseY < topTextBox - paddingUnscaled + iconSize) {
            if(mouseX > width/2 + searchBarXSize/2 + paddingUnscaled*6 &&
                    mouseX < width/2 + searchBarXSize/2 + paddingUnscaled*6 + iconSize) {
                if(Mouse.getEventButtonState()) {
                    manager.config.enableItemEditing.value = !manager.config.enableItemEditing.value;
                }
            } else if(mouseX > width/2 - searchBarXSize/2 - paddingUnscaled*6 - iconSize &&
                    mouseX < width/2 - searchBarXSize/2 - paddingUnscaled*6) {
                if(Mouse.getEventButtonState()) {
                    configOpen = !configOpen;
                    infoPaneOffsetFactor.setTarget(1/3f);
                    infoPaneOffsetFactor.resetTimer();

                }
            }
        }

        iterateSettingTile(new SettingsTileConsumer() {
            @Override
            public void consume(int x, int y, int tileWidth, int tileHeight, Options.Option<?> option) {
                float mult = tileWidth/90f;
                if(option.value instanceof Boolean) {
                    if(Mouse.getEventButtonState()) {
                        if(mouseX > x+tileWidth/2-(int)(32*mult) && mouseX < x+tileWidth/2-(int)(32*mult)+(int)(48*mult)) {
                            if(mouseY > y+tileHeight-(int)(20*mult) && mouseY < y+tileHeight-(int)(20*mult)+(int)(16*mult)) {
                                ((Options.Option<Boolean>)option).value = !((Boolean)option.value);
                            }
                        }
                    }
                } else {
                    if(!textConfigMap.containsKey(option)) {
                        textConfigMap.put(option, new GuiElementTextField(String.valueOf(option.value), 0));
                    }
                    GuiElementTextField tf = textConfigMap.get(option);
                    if(mouseX > x+(int)(10*mult) && mouseX < x+(int)(10*mult)+tileWidth-(int)(20*mult)) {
                        if(mouseY > y+tileHeight-(int)(20*mult) && mouseY < y+tileHeight-(int)(20*mult)+(int)(16*mult)) {
                            if(Mouse.getEventButtonState()) {
                                tf.mouseClicked(mouseX, mouseY, Mouse.getEventButton());
                            } else if(Mouse.getEventButton() == -1 && mouseDown) {
                                tf.mouseClickMove(mouseX, mouseY, 0, 0); //last 2 values are unused
                            }
                            return;
                        }
                    }
                    if(Mouse.getEventButtonState()) tf.otherComponentClick();
                }
            }
        });

        return false;
    }

    //From https://stackoverflow.com/questions/1760766/how-to-convert-non-supported-character-to-html-entity-in-java
    public String encodeNonAscii(String c) {
        StringBuilder buf = new StringBuilder(c.length());
        CharsetEncoder enc = StandardCharsets.US_ASCII.newEncoder();
        for (int idx = 0; idx < c.length(); ++idx) {
            char ch = c.charAt(idx);
            if (enc.canEncode(ch))
                buf.append(ch);
            else {
                buf.append("&#");
                buf.append((int) ch);
                buf.append(';');
            }
        }
        return buf.toString();
    }

    ExecutorService ste = Executors.newSingleThreadExecutor();

    private void processAndSetWebpageImage(String html, String name) {
        File cssFile = new File(manager.configLocation, "wikia.css");
        File wkHtmlToImage = new File(manager.configLocation, "wkhtmltox/bin/wkhtmltoimage");
        File input = new File(manager.configLocation, "tmp/input.html");
        File output = new File(manager.configLocation, "tmp/"+
                name.replaceAll("(?i)\\u00A7.", "")
                    .replaceAll("[^a-zA-Z0-9_\\-]", "_")+".png");
        input.deleteOnExit();
        output.deleteOnExit();

        if(output.exists()) {
            try {
                webpageLoadTemp = ImageIO.read(output);
                informationPane = new String[] { EnumChatFormatting.RED+"Failed to set informationPaneImage." };
            } catch(IOException e) {
                e.printStackTrace();
                informationPaneImage = null;
                informationPane = new String[] { EnumChatFormatting.RED+"ERROR" };
                return;
            }
        } else {
            html = "<div id=\"mw-content-text\" lang=\"en\" dir=\"ltr\" class=\"mw-content-ltr mw-content-text\">"+html+"</div>";
            html = "<div id=\"WikiaArticle\" class=\"WikiaArticle\">"+html+"</div>";
            html = "<link rel=\"stylesheet\" href=\"file:///"+cssFile.getAbsolutePath()+"\">\n"+html;

            try(PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(input), StandardCharsets.UTF_8)), false)) {

                out.println(encodeNonAscii(html));
            } catch(IOException e) {}

            try {
                final int awaitID = webpageAwaitID.get();

                informationPane = new String[] { EnumChatFormatting.GRAY+"Rendering webpage, please wait... (AwaitID: "+awaitID+")" };

                Runtime runtime = Runtime.getRuntime();
                Process p = runtime.exec("\""+wkHtmlToImage.getAbsolutePath() + "\" --width "+ IMAGE_WIDTH*ZOOM_FACTOR+" --transparent --zoom "+ZOOM_FACTOR+" \"" + input.getAbsolutePath() + "\" \"" + output.getAbsolutePath() + "\"");
                ste.submit(() -> {
                    try {
                        if(p.waitFor(15, TimeUnit.SECONDS)) {
                            if(awaitID != webpageAwaitID.get()) return;

                            try {
                                webpageLoadTemp = ImageIO.read(output);
                                informationPane = new String[] { EnumChatFormatting.RED+"Failed to set informationPaneImage." };
                            } catch(IOException e) {
                                e.printStackTrace();
                                informationPaneImage = null;
                                informationPane = new String[] { EnumChatFormatting.RED+"ERROR" };
                                return;
                            }
                        } else {
                            if(awaitID != webpageAwaitID.get()) return;

                            informationPane = new String[] { EnumChatFormatting.RED+"Webpage render timed out (>15sec). Maybe it's too large?" };
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch(Exception e) {
                e.printStackTrace();
                informationPaneImage = null;
                informationPane = new String[] { EnumChatFormatting.RED+"ERROR" };
                return;
            }
        }
    }

    public void displayInformationPane(String title, String infoType, String[] info) {
        scrollHeight.setValue(0);
        informationPaneTitle = title;
        informationPaneImage = null;
        informationPane = null;

        configOpen = false;

        infoPaneOffsetFactor.setTarget(1/3f);
        infoPaneOffsetFactor.resetTimer();

        webpageAwaitID.incrementAndGet();

        if(info == null || info.length == 0) {
            informationPane = new String[]{"\u00A77No additional information."};
        } else {
            String joined = StringUtils.join(info, "\n");
            String wiki = null;
            String html = null;
            if(infoType.equals("TEXT")) {
                informationPane = info;
                return;
            } else if(infoType.equals("WIKI_URL")) {
                File f = manager.getWebFile(joined);
                if(f == null) {
                    informationPane = new String[] { EnumChatFormatting.RED+"Failed to load wiki url: "+joined };
                    return;
                };

                StringBuilder sb = new StringBuilder();
                try(BufferedReader br = new BufferedReader(new InputStreamReader(
                        new FileInputStream(f), StandardCharsets.UTF_8))) {
                    String l;
                    while((l = br.readLine()) != null){
                        sb.append(l).append("\n");
                    }
                } catch(IOException e) {
                    informationPane = new String[] { EnumChatFormatting.RED+"Failed to load wiki url: "+joined };
                    return;
                }
                wiki = sb.toString();
            }

            if(infoType.equals("WIKI") || wiki != null) {
                if(wiki == null) wiki = joined;
                try {
                    String[] split = wiki.split("</infobox>");
                    wiki = split[split.length - 1]; //Remove everything before infobox
                    wiki = wiki.split("<span class=\"navbox-vde\">")[0]; //Remove navbox
                    wiki = wiki.split("<table class=\"navbox mw-collapsible\"")[0];
                    wiki = "__NOTOC__\n" + wiki; //Remove TOC
                    try (PrintWriter out = new PrintWriter(new File(manager.configLocation, "debug/parsed.txt"))) {
                        out.println(wiki);
                    } catch (IOException e) {
                    }
                    html = wikiModel.render(wiki);
                    try (PrintWriter out = new PrintWriter(new File(manager.configLocation, "debug/html.txt"))) {
                        out.println(html);
                    } catch (IOException e) {
                    }
                } catch (Exception e) {
                    informationPane = new String[]{EnumChatFormatting.RED + "Failed to parse wiki: " + joined};
                    return;
                }
            }

            if(infoType.equals("HTML") || html != null) {
                if(html == null) html = joined;
                processAndSetWebpageImage(html, title);
            }
        }
    }

    public int getClickedIndex(int mouseX, int mouseY) {
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        int xComp = mouseX - (width/2 - searchBarXSize/2 + 5);

        String trimmed = Minecraft.getMinecraft().fontRendererObj.trimStringToWidth(textField.getText(), xComp);
        int linePos = trimmed.length();
        if(linePos != textField.getText().length()) {
            char after = textField.getText().charAt(linePos);
            int trimmedWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(trimmed);
            int charWidth = Minecraft.getMinecraft().fontRendererObj.getCharWidth(after);
            if(trimmedWidth + charWidth/2 < xComp-5) {
                linePos++;
            }
        }
        return linePos;
    }

    public void setSearchBarFocus(boolean focus) {
        if(focus) {
            itemPaneOpen = true;
        }
        searchBarHasFocus = focus;
    }

    /**
     * Handles the keyboard input, cancelling the forge event if the search bar has focus.
     */
    public boolean keyboardInput() {
        if(Minecraft.getMinecraft().currentScreen == null) return false;

        if(Keyboard.getEventKeyState()) {
            if(searchBarHasFocus) {
                if(Keyboard.getEventKey() == 1) {
                    searchBarHasFocus = false;
                } else {
                    if(textField.textboxKeyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey())) {
                        updateSearch();
                    }
                }
            } else {
                if(Keyboard.getEventKey() == Keyboard.KEY_LEFT) yaw--;
                if(Keyboard.getEventKey() == Keyboard.KEY_RIGHT) yaw++;
                if(Keyboard.getEventKey() == Keyboard.KEY_UP) pitch--;
                if(Keyboard.getEventKey() == Keyboard.KEY_DOWN) pitch++;

                iterateSettingTile(new SettingsTileConsumer() {
                    @Override
                    public void consume(int x, int y, int tileWidth, int tileHeight, Options.Option<?> option) {
                        if(!textConfigMap.containsKey(option)) {
                            textConfigMap.put(option, new GuiElementTextField(String.valueOf(option.value), 0));
                        }
                        GuiElementTextField tf = textConfigMap.get(option);

                        if(!(option.value instanceof Boolean)) {
                            tf.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
                        }
                    }
                });
            }
        }

        if(Keyboard.getEventKeyState()) {
            AtomicReference<String> internalname = new AtomicReference<>(null);
            Slot slot = ((GuiContainer)Minecraft.getMinecraft().currentScreen).getSlotUnderMouse();
            if(slot != null) {
                ItemStack hover = slot.getStack();
                if(hover != null) {
                    internalname.set(manager.getInternalNameForItem(hover));
                }
            } else {
                int height = scaledresolution.getScaledHeight();

                int mouseX = Mouse.getX() / scaledresolution.getScaleFactor();
                int mouseY = height - Mouse.getY() / scaledresolution.getScaleFactor();

                iterateItemSlots((x, y) -> {
                    if (mouseX >= x - 1 && mouseX <= x + itemSize + 1) {
                        if (mouseY >= y - 1 && mouseY <= y + itemSize + 1) {
                            int id = getSlotId(x, y);
                            JsonObject json = getSearchedItemPage(id);
                            if(json != null) internalname.set(json.get("internalname").getAsString());
                            return;
                        }
                    }
                });
            }
            if(internalname.get() != null) {
                JsonObject item = manager.getItemInformation().get(internalname.get());
                if(item != null) {
                    if(manager.config.enableItemEditing.value && Keyboard.getEventCharacter() == 'k') {
                        Minecraft.getMinecraft().displayGuiScreen(new NEUItemEditor(manager,
                                item.get("internalname").getAsString(), item));
                        return true;
                    } else if(Keyboard.getEventCharacter() == 'f') {
                        toggleRarity(item.get("internalname").getAsString());
                        return true;
                    } else if(Keyboard.getEventCharacter() == 'r') {
                        manager.showRecipe(item);
                        return true;
                    }
                }
            }
        }

        return searchBarHasFocus; //Cancels keyboard events if the search bar has focus
    }

    public void toggleRarity(String internalname) {
        if(getFavourites().contains(internalname)) {
            getFavourites().remove(internalname);
        } else {
            getFavourites().add(internalname);
        }
        updateSearch();
    }

    String[] rarityArr = new String[] {
        EnumChatFormatting.WHITE+EnumChatFormatting.BOLD.toString()+"COMMON",
        EnumChatFormatting.GREEN+EnumChatFormatting.BOLD.toString()+"UNCOMMON",
        EnumChatFormatting.BLUE+EnumChatFormatting.BOLD.toString()+"RARE",
        EnumChatFormatting.DARK_PURPLE+EnumChatFormatting.BOLD.toString()+"EPIC",
        EnumChatFormatting.GOLD+EnumChatFormatting.BOLD.toString()+"LEGENDARY",
        EnumChatFormatting.LIGHT_PURPLE+EnumChatFormatting.BOLD.toString()+"SPECIAL",
    };
    public int getRarity(JsonArray lore) {
        for(int i=lore.size()-1; i>=0; i--) {
            String line = lore.get(i).getAsString();

            for(int j=0; j<rarityArr.length; j++) {
                if(line.startsWith(rarityArr[j])) {
                    return j;
                }
            }
        }
        return -1;
    }

    private int getCompareMode() {
        return manager.config.compareMode.value.intValue();
    }
    private int getSortMode() {
        return manager.config.sortMode.value.intValue();
    }
    private List<Boolean> getCompareAscending() {
        return manager.config.compareAscending.value;
    }
    private List<String> getFavourites() {
        return manager.config.favourites.value;
    }

    private Comparator<JsonObject> getItemComparator() {
        return (o1, o2) -> {
            //1 (mult) if o1 should appear after o2
            //-1 (-mult) if o2 should appear after o1
            if(getFavourites().contains(o1.get("internalname").getAsString()) && !getFavourites().contains(o2.get("internalname").getAsString())) {
                return -1;
            }
            if(!getFavourites().contains(o1.get("internalname").getAsString()) && getFavourites().contains(o2.get("internalname").getAsString())) {
                return 1;
            }

            int mult = getCompareAscending().get(getCompareMode()) ? 1 : -1;
            if(getCompareMode() == COMPARE_MODE_RARITY) {
                int rarity1 = getRarity(o1.get("lore").getAsJsonArray());
                int rarity2 = getRarity(o2.get("lore").getAsJsonArray());

                if(rarity1 < rarity2) return mult;
                if(rarity1 > rarity2) return -mult;
            }

            String i1 = o1.get("internalname").getAsString();
            String[] split1 = i1.split("_");
            String last1 = split1[split1.length-1];
            String start1 = i1.substring(0, i1.length()-last1.length());

            String i2 = o2.get("internalname").getAsString();
            String[] split2 = i2.split("_");
            String last2 = split2[split2.length-1];
            String start2 = i2.substring(0, i2.length()-last2.length());

            mult = getCompareAscending().get(COMPARE_MODE_ALPHABETICAL) ? 1 : -1;
            if(start1.equals(start2)) {
                String[] order = new String[]{"HELMET","CHESTPLATE","LEGGINGS","BOOTS"};
                int type1 = checkItemType(o1.get("lore").getAsJsonArray(), order);
                int type2 = checkItemType(o2.get("lore").getAsJsonArray(), order);


                if(type1 < type2) return -mult;
                if(type1 > type2) return mult;
            }

            int nameComp = mult*o1.get("displayname").getAsString().replaceAll("(?i)\\u00A7.", "")
                     .compareTo(o2.get("displayname").getAsString().replaceAll("(?i)\\u00A7.", ""));
            if(nameComp != 0) {
                return nameComp;
            }
            return mult*o1.get("internalname").getAsString().compareTo(o2.get("internalname").getAsString());
        };
    }

    public int checkItemType(JsonArray lore, String... typeMatches) {
        for(int i=lore.size()-1; i>=0; i--) {
            String line = lore.get(i).getAsString();

            for(String rarity : rarityArr) {
                for(int j=0; j<typeMatches.length; j++) {
                    if(line.trim().equals(rarity + " " + typeMatches[j])) {
                        return j;
                    }
                }
            }
        }
        return -1;
    }

    public boolean checkMatchesSort(String internalname, JsonObject item) {
        if(getSortMode() == SORT_MODE_ALL) {
            return !internalname.matches(mobRegex);
        } else if(getSortMode() == SORT_MODE_MOB) {
            return internalname.matches(mobRegex);
        } else if(getSortMode() == SORT_MODE_PET) {
            return internalname.matches(petRegex);
        } else if(getSortMode() == SORT_MODE_TOOL) {
            return checkItemType(item.get("lore").getAsJsonArray(),
                    "SWORD", "BOW", "AXE", "PICKAXE", "FISHING ROD", "WAND", "SHOVEL", "HOE") >= 0;
        } else if(getSortMode() == SORT_MODE_ARMOR) {
            return checkItemType(item.get("lore").getAsJsonArray(), "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS") >= 0;
        } else if(getSortMode() == SORT_MODE_ACCESSORY) {
            return checkItemType(item.get("lore").getAsJsonArray(), "ACCESSORY") >= 0;
        }
        return true;
    }

    public void updateSearch() {
        if(searchedItems==null) searchedItems = new TreeSet<>(getItemComparator());
        searchedItems.clear();
        searchedItemsArr = null;
        Set<String> itemsMatch = manager.search(textField.getText());
        for(String itemname : itemsMatch) {
            JsonObject item = manager.getItemInformation().get(itemname);
            if(checkMatchesSort(itemname, item)) {
                searchedItems.add(item);
            }
        }
    }

    public JsonObject[] getSearchedItems() {
        if(searchedItems==null) {
            updateSearch();
        }
        if(searchedItemsArr==null) {
            searchedItemsArr = new JsonObject[searchedItems.size()];
            int i=0;
            for(JsonObject item : searchedItems) {
                searchedItemsArr[i] = item;
                i++;
            }
        }
        return searchedItemsArr;
    }

    public JsonObject getSearchedItemPage(int index) {
        if(index < getSlotsXSize()*getSlotsYSize()) {
            int actualIndex = index + getSlotsXSize()*getSlotsYSize()*page;
            if(actualIndex < getSearchedItems().length) {
                return getSearchedItems()[actualIndex];
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public int getItemBoxXPadding() {
        int width = scaledresolution.getScaledWidth();
        return (((int)(width-width*getItemPaneOffsetFactor())-2*boxPadding)%(itemSize+itemPadding)+itemPadding)/2;
    }

    /**
     * Iterates through all the item slots in the right panel and calls a biconsumer for each slot with
     * arguments equal to the slot's x and y position respectively. This is used in order to prevent
     * code duplication issues.
     */
    public void iterateItemSlots(BiConsumer<Integer, Integer> itemSlotConsumer) {
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        int paneWidth = (int)(width/3*getWidthMult());
        int itemBoxXPadding = getItemBoxXPadding();
        int itemBoxYPadding = ((height-getSearchBarYSize()-2*boxPadding-itemSize-2)%(itemSize+itemPadding)+itemPadding)/2;

        int xStart = (int)(width*getItemPaneOffsetFactor())+boxPadding+itemBoxXPadding;
        int yStart = boxPadding+getSearchBarYSize()+itemBoxYPadding;
        int xEnd = (int)(width*getItemPaneOffsetFactor())+paneWidth-boxPadding-itemSize;
        int yEnd = height-boxPadding-itemSize-2-itemBoxYPadding;

        //Render the items, displaying the tooltip if the cursor is over the item
        for(int y = yStart; y < yEnd; y+=itemSize+itemPadding) {
            for(int x = xStart; x < xEnd; x+=itemSize+itemPadding) {
                itemSlotConsumer.accept(x, y);
            }
        }
    }

    public float getWidthMult() {
        float scaleFMult = 1;
        if(scaledresolution.getScaleFactor()==4) scaleFMult = 0.9f;
        return (float)Math.min(1.5, Math.max(0.5, manager.config.paneWidthMult.value.floatValue()))*scaleFMult;
    }

    /**
     * Calculates the number of horizontal item slots.
     */
    public int getSlotsXSize() {
        int width = scaledresolution.getScaledWidth();

        int paneWidth = (int)(width/3*getWidthMult());
        int itemBoxXPadding = (((int)(width-width*getItemPaneOffsetFactor())-2*boxPadding)%(itemSize+itemPadding)+itemPadding)/2;
        int xStart = (int)(width*getItemPaneOffsetFactor())+boxPadding+itemBoxXPadding;
        int xEnd = (int)(width*getItemPaneOffsetFactor())+paneWidth-boxPadding-itemSize;

        return (int)Math.ceil((xEnd - xStart)/((float)(itemSize+itemPadding)));
    }

    /**
     * Calculates the number of vertical item slots.
     */
    public int getSlotsYSize() {
        int height = scaledresolution.getScaledHeight();

        int itemBoxYPadding = ((height-getSearchBarYSize()-2*boxPadding-itemSize-2)%(itemSize+itemPadding)+itemPadding)/2;
        int yStart = boxPadding+getSearchBarYSize()+itemBoxYPadding;
        int yEnd = height-boxPadding-itemSize-2-itemBoxYPadding;

        return (int)Math.ceil((yEnd - yStart)/((float)(itemSize+itemPadding)));
    }

    public int getMaxPages() {
        if(getSearchedItems().length == 0) return 1;
        return (int)Math.ceil(getSearchedItems().length/(float)getSlotsYSize()/getSlotsXSize());
    }

    /**
     * Takes in the x and y coordinates of a slot and returns the id of that slot.
     */
    public int getSlotId(int x, int y) {
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        int itemBoxXPadding = (((int)(width-width*getItemPaneOffsetFactor())-2*boxPadding)%(itemSize+itemPadding)+itemPadding)/2;
        int itemBoxYPadding = ((height-getSearchBarYSize()-2*boxPadding-itemSize-2)%(itemSize+itemPadding)+itemPadding)/2;

        int xStart = (int)(width*getItemPaneOffsetFactor())+boxPadding+itemBoxXPadding;
        int yStart = boxPadding+getSearchBarYSize()+itemBoxYPadding;

        int xIndex = (x-xStart)/(itemSize+itemPadding);
        int yIndex = (y-yStart)/(itemSize+itemPadding);
        return xIndex + yIndex*getSlotsXSize();
    }

    private int getSearchBarYSize() {
        return Math.max(searchBarYSize/scaledresolution.getScaleFactor(), itemSize);
    }

    public void renderNavElement(int leftSide, int rightSide, int maxPages, String name) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

        drawRect(leftSide-1, boxPadding,
                rightSide+1,
                boxPadding+getSearchBarYSize(), fg.getRGB());

        float scaledYSize = searchBarYSize/scaledresolution.getScaleFactor();

        Minecraft.getMinecraft().getTextureManager().bindTexture(prev);
        GlStateManager.color(1f, 1f, 1f, 1f);
        drawTexturedRect(leftSide, boxPadding+(getSearchBarYSize()-scaledYSize)/2f,
                scaledYSize*400/160, scaledYSize);
        GlStateManager.bindTexture(0);

        Minecraft.getMinecraft().getTextureManager().bindTexture(next);
        GlStateManager.color(1f, 1f, 1f, 1f);
        drawTexturedRect(rightSide-scaledYSize*400/160, boxPadding+(getSearchBarYSize()-scaledYSize)/2f,
                scaledYSize*400/160, scaledYSize);
        GlStateManager.bindTexture(0);

        String pageText = EnumChatFormatting.BOLD+name + (page+1) + "/" + maxPages;
        fr.drawString(pageText, (leftSide+rightSide)/2-fr.getStringWidth(pageText)/2,
                boxPadding+(getSearchBarYSize()-8)/2+1, Color.BLACK.getRGB());
    }

    private int limCol(int col) {
        return Math.min(255, Math.max(0, col));
    }

    public float yaw = 0;
    public float pitch = 20;

    private void renderEntity(float posX, float posY, float scale, String name, Class<? extends EntityLivingBase>... classes) {
        EntityLivingBase[] entities = new EntityLivingBase[classes.length];
        try {
            EntityLivingBase last = null;
            for(int i=0; i<classes.length; i++) {
                Class<? extends EntityLivingBase> clazz = classes[i];
                if(clazz == null) continue;

                EntityLivingBase newEnt = clazz.getConstructor(new Class[] {World.class}).newInstance(Minecraft.getMinecraft().theWorld);

                //newEnt.renderYawOffset = yaw;
                //newEnt.rotationYaw = yaw;
                newEnt.rotationPitch = pitch;
                //newEnt.rotationYawHead = yaw;
                //newEnt.prevRotationYawHead = yaw-1;

                newEnt.setCustomNameTag(name);

                if(last != null) {
                    last.riddenByEntity = newEnt;
                    newEnt.ridingEntity = last;
                    last.updateRiderPosition();
                }
                last = newEnt;

                entities[i] = newEnt;
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return;
        }


        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(posX, posY, 50.0F);
        GlStateManager.scale(-scale, scale, scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);

        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);

        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);

        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        rendermanager.setPlayerViewY(180.0F);
        rendermanager.setRenderShadow(false);
        for(EntityLivingBase ent : entities) {
            GL11.glColor4f(1,1,1,1);
            if(ent != null) rendermanager.renderEntityWithPosYaw(ent, ent.posX, ent.posY, ent.posZ, 0.0F, 1.0F);
        }
        rendermanager.setRenderShadow(true);

        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void renderOverlay(int mouseX, int mouseY) {
        if(searchMode && textField.getText().length() > 0) {
            int width = scaledresolution.getScaledWidth();
            int height = scaledresolution.getScaledHeight();
            try {
                GuiContainer inv = (GuiContainer) Minecraft.getMinecraft().currentScreen;
                Field guiLeft = GuiContainer.class.getDeclaredField("field_147003_i");//guiLeft
                Field guiTop = GuiContainer.class.getDeclaredField("field_147009_r");//guiTop
                guiLeft.setAccessible(true);
                guiTop.setAccessible(true);
                int guiLeftI = (int) guiLeft.get(inv);
                int guiTopI = (int) guiTop.get(inv);

                GL11.glTranslatef(0, 0, 300);
                int overlay = new Color(0, 0, 0, 100).getRGB();
                for(Slot slot : inv.inventorySlots.inventorySlots) {
                    if(slot.getStack() == null || !manager.doesStackMatchSearch(slot.getStack(), textField.getText())) {
                        drawRect(guiLeftI+slot.xDisplayPosition, guiTopI+slot.yDisplayPosition,
                                guiLeftI+slot.xDisplayPosition+16, guiTopI+slot.yDisplayPosition+16,
                                overlay);
                    }
                }
                if(inv.getSlotUnderMouse() != null) {
                    ItemStack stack = inv.getSlotUnderMouse().getStack();
                    if(stack != null) {
                        List<String> list = stack.getTooltip(Minecraft.getMinecraft().thePlayer,
                                Minecraft.getMinecraft().gameSettings.advancedItemTooltips);

                        for (int i = 0; i < list.size(); ++i){
                            if (i == 0){
                                list.set(i, stack.getRarity().rarityColor + list.get(i));
                            } else {
                                list.set(i, EnumChatFormatting.GRAY + list.get(i));
                            }
                        }

                        Utils.drawHoveringText(list, mouseX, mouseY, width, height, -1, Minecraft.getMinecraft().fontRendererObj);
                    }
                }
                GL11.glTranslatef(0, 0, -300);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Renders the search bar, item selection (right) and item info (left) gui elements.
     */
    public void render(int mouseX, int mouseY) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        yaw++;
        yaw %= 360;

        scrollHeight.tick();
        manager.updatePrices();

        int opacity = Math.min(255, Math.max(0, manager.config.bgOpacity.value.intValue()));
        bg = new Color((bg.getRGB() & 0x00ffffff) | opacity << 24, true);

        opacity = Math.min(255, Math.max(0, manager.config.fgOpacity.value.intValue()));
        Color fgCustomOpacity = new Color((fg.getRGB() & 0x00ffffff) | opacity << 24, true);
        Color fgFavourite = new Color(limCol(fg.getRed()+20), limCol(fg.getGreen()+10), limCol(fg.getBlue()-10), opacity);
        Color fgFavourite2 = new Color(limCol(fg.getRed()+100), limCol(fg.getGreen()+50), limCol(fg.getBlue()-50), opacity);

        if(webpageLoadTemp != null && informationPaneImage == null) {
            DynamicTexture tex = new DynamicTexture(webpageLoadTemp);
            informationPaneImage = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(
                    "notenoughupdates/informationPaneImage", tex);
            informationPaneImageHeight = webpageLoadTemp.getHeight();

            webpageLoadTemp = null;
        }

        if(itemPaneOpen) {
            if(itemPaneTabOffset.getValue() == 0) {
                if(itemPaneOffsetFactor.getTarget() != 2/3f) {
                    itemPaneOffsetFactor.setTarget(2/3f);
                    itemPaneOffsetFactor.resetTimer();
                }
            } else {
                if(itemPaneTabOffset.getTarget() != 0) {
                    itemPaneTabOffset.setTarget(0);
                    itemPaneTabOffset.resetTimer();
                }
            }
        } else {
            if(itemPaneOffsetFactor.getValue() == 1) {
                if(itemPaneTabOffset.getTarget() != 20) {
                    itemPaneTabOffset.setTarget(20);
                    itemPaneTabOffset.resetTimer();
                }
            } else {
                if(itemPaneOffsetFactor.getTarget() != 1f) {
                    itemPaneOffsetFactor.setTarget(1f);
                    itemPaneOffsetFactor.resetTimer();
                }
            }
        }

        itemPaneOffsetFactor.tick();
        itemPaneTabOffset.tick();
        infoPaneOffsetFactor.tick();

        if(page > getMaxPages()-1) page = getMaxPages()-1;
        if(page < 0) page = 0;

        GlStateManager.disableLighting();

        /**
         * Search bar
         */
        int paddingUnscaled = searchBarPadding/scaledresolution.getScaleFactor();
        if(paddingUnscaled < 1) paddingUnscaled = 1;

        int topTextBox = height - searchBarYOffset - getSearchBarYSize();

        //Search bar background
        drawRect(width/2 - searchBarXSize/2 - paddingUnscaled,
                topTextBox - paddingUnscaled,
                width/2 + searchBarXSize/2 + paddingUnscaled,
                height - searchBarYOffset + paddingUnscaled, searchMode ? Color.YELLOW.getRGB() : Color.WHITE.getRGB());
        drawRect(width/2 - searchBarXSize/2,
                topTextBox,
                width/2 + searchBarXSize/2,
                height - searchBarYOffset, Color.BLACK.getRGB());

        //Settings
        int iconSize = getSearchBarYSize()+paddingUnscaled*2;
        Minecraft.getMinecraft().getTextureManager().bindTexture(settings);
        drawRect(width/2 - searchBarXSize/2 - paddingUnscaled*6 - iconSize,
                topTextBox - paddingUnscaled,
                width/2 - searchBarXSize/2 - paddingUnscaled*6,
                topTextBox - paddingUnscaled + iconSize, Color.WHITE.getRGB());

        drawRect(width/2 - searchBarXSize/2 - paddingUnscaled*5 - iconSize,
                topTextBox,
                width/2 - searchBarXSize/2 - paddingUnscaled*7,
                topTextBox - paddingUnscaled*2 + iconSize, Color.GRAY.getRGB());
        GlStateManager.color(1f, 1f, 1f, 1f);
        drawTexturedRect(width/2 - searchBarXSize/2 - paddingUnscaled*6 - iconSize, topTextBox - paddingUnscaled, iconSize, iconSize);
        GlStateManager.bindTexture(0);

        //Search bar text
        fr.drawString(textField.getText(), width/2 - searchBarXSize/2 + 5,
                topTextBox+(getSearchBarYSize()-8)/2, Color.WHITE.getRGB());

        //Determines position of cursor. Cursor blinks on and off every 500ms.
        if(searchBarHasFocus && System.currentTimeMillis()%1000>500) {
            String textBeforeCursor = textField.getText().substring(0, textField.getCursorPosition());
            int textBeforeCursorWidth = fr.getStringWidth(textBeforeCursor);
            drawRect(width/2 - searchBarXSize/2 + 5 + textBeforeCursorWidth,
                    topTextBox+(getSearchBarYSize()-8)/2-1,
                    width/2 - searchBarXSize/2 + 5 + textBeforeCursorWidth+1,
                    topTextBox+(getSearchBarYSize()-8)/2+9, Color.WHITE.getRGB());
        }

        String selectedText = textField.getSelectedText();
        if(!selectedText.isEmpty()) {
            int selectionWidth = fr.getStringWidth(selectedText);

            int leftIndex = Math.min(textField.getCursorPosition(), textField.getSelectionEnd());
            String textBeforeSelection = textField.getText().substring(0, leftIndex);
            int textBeforeSelectionWidth = fr.getStringWidth(textBeforeSelection);

            drawRect(width/2 - searchBarXSize/2 + 5 + textBeforeSelectionWidth,
                    topTextBox+(getSearchBarYSize()-8)/2-1,
                    width/2 - searchBarXSize/2 + 5 + textBeforeSelectionWidth + selectionWidth,
                    topTextBox+(getSearchBarYSize()-8)/2+9, Color.LIGHT_GRAY.getRGB());

            fr.drawString(selectedText,
                    width/2 - searchBarXSize/2 + 5 + textBeforeSelectionWidth,
                    topTextBox+(getSearchBarYSize()-8)/2, Color.BLACK.getRGB());
        }


        /**
         * Item selection (right) gui element rendering
         */
        int paneWidth = (int)(width/3*getWidthMult());
        int leftSide = (int)(width*getItemPaneOffsetFactor());
        int rightSide = leftSide+paneWidth-boxPadding-getItemBoxXPadding();

        //Tab
        Minecraft.getMinecraft().getTextureManager().bindTexture(itemPaneTabArrow);
        GlStateManager.color(1f, 1f, 1f, 0.3f);
        drawTexturedRect(width-itemPaneTabOffset.getValue(), height/2 - 50, 20, 100);
        GlStateManager.bindTexture(0);

        if(mouseX > width-itemPaneTabOffset.getValue() && mouseY > height/2 - 50
                && mouseY < height/2 + 50) {
            itemPaneOpen = true;
        }

        //Atomic reference used so that below lambda doesn't complain about non-effectively-final variable
        AtomicReference<JsonObject> tooltipToDisplay = new AtomicReference<>(null);

        if(itemPaneOffsetFactor.getValue() < 1) {
            drawRect(leftSide+boxPadding-5, boxPadding-5,
                    leftSide+paneWidth-boxPadding+5, height-boxPadding+5, bg.getRGB());

            renderNavElement(leftSide+boxPadding+getItemBoxXPadding(), rightSide, getMaxPages(),
                    scaledresolution.getScaleFactor()<4?"Page: ":"");

            //Sort bar
            drawRect(leftSide+boxPadding+getItemBoxXPadding()-1,
                    height-boxPadding-itemSize-2,
                    rightSide+1,
                    height-boxPadding, fgCustomOpacity.getRGB());

            float sortIconsMinX = (sortIcons.length+orderIcons.length)*(itemSize+itemPadding)+itemSize;
            float availableX = rightSide-(leftSide+boxPadding+getItemBoxXPadding());
            float sortOrderScaleFactor = Math.min(1, availableX / sortIconsMinX);

            int scaledItemSize = (int)(itemSize*sortOrderScaleFactor);
            int scaledItemPaddedSize = (int)((itemSize+itemPadding)*sortOrderScaleFactor);
            int iconTop = height-boxPadding-(itemSize+scaledItemSize)/2-1;

            for(int i=0; i<orderIcons.length; i++) {
                int orderIconX = leftSide+boxPadding+getItemBoxXPadding()+i*scaledItemPaddedSize;
                drawRect(orderIconX, iconTop,scaledItemSize+orderIconX,iconTop+scaledItemSize, fg.getRGB());

                Minecraft.getMinecraft().getTextureManager().bindTexture(getCompareMode() == i ? orderIconsActive[i] : orderIcons[i]);
                GlStateManager.color(1f, 1f, 1f, 1f);
                drawTexturedRect(orderIconX, iconTop, scaledItemSize, scaledItemSize,0, 1, 0, 1, GL11.GL_NEAREST);

                Minecraft.getMinecraft().getTextureManager().bindTexture(getCompareAscending().get(i) ? ascending_overlay : descending_overlay);
                GlStateManager.color(1f, 1f, 1f, 1f);
                drawTexturedRect(orderIconX, iconTop, scaledItemSize, scaledItemSize,0, 1, 0, 1, GL11.GL_NEAREST);
                GlStateManager.bindTexture(0);
            }

            for(int i=0; i<sortIcons.length; i++) {
                int sortIconX = rightSide-scaledItemSize-i*scaledItemPaddedSize;
                drawRect(sortIconX, iconTop,scaledItemSize+sortIconX,iconTop+scaledItemSize, fg.getRGB());
                Minecraft.getMinecraft().getTextureManager().bindTexture(getSortMode() == i ? sortIconsActive[i] : sortIcons[i]);
                GlStateManager.color(1f, 1f, 1f, 1f);
                drawTexturedRect(sortIconX, iconTop, scaledItemSize, scaledItemSize, 0, 1, 0, 1, GL11.GL_NEAREST);
                GlStateManager.bindTexture(0);
            }

            //Iterate through all item slots and display the appropriate item
            iterateItemSlots((x, y) -> {
                int id = getSlotId(x, y);
                JsonObject json = getSearchedItemPage(id);
                if(json == null) {
                    return;
                }

                if(getFavourites().contains(json.get("internalname").getAsString())) {
                    drawRect(x-1, y-1, x+itemSize+1, y+itemSize+1, fgFavourite2.getRGB());
                    drawRect(x, y, x+itemSize, y+itemSize, fgFavourite.getRGB());
                } else {
                    drawRect(x-1, y-1, x+itemSize+1, y+itemSize+1, fgCustomOpacity.getRGB());
                }

                if(json.has("entityrender")) {
                    String name = json.get("displayname").getAsString();
                    String[] split = name.split(" \\(");
                    name = name.substring(0, name.length()-split[split.length-1].length()-2);

                    Class<? extends EntityLivingBase>[] entities = new Class[1];
                    if(json.get("entityrender").isJsonArray()) {
                        JsonArray entityrender = json.get("entityrender").getAsJsonArray();
                        entities = new Class[entityrender.size()];
                        for(int i=0; i<entityrender.size(); i++) {
                            Class<? extends Entity> clazz = EntityList.stringToClassMapping.get(entityrender.get(i).getAsString());
                            if(clazz != null && EntityLivingBase.class.isAssignableFrom(clazz)) {
                                entities[i] = (Class<? extends EntityLivingBase>)clazz;
                            }
                        }
                    } else if(json.get("entityrender").isJsonPrimitive()) {
                        Class<? extends Entity> clazz = EntityList.stringToClassMapping.get(json.get("entityrender").getAsString());
                        if(clazz != null && EntityLivingBase.class.isAssignableFrom(clazz)) {
                            entities[0] = (Class<? extends EntityLivingBase>)clazz;
                        }
                    }

                    float scale = 8;
                    if(json.has("entityscale")) {
                        scale *= json.get("entityscale").getAsFloat();
                    }

                    renderEntity(x+itemSize/2, y+itemSize, scale, name, entities);
                } else {
                    drawItemStack(manager.jsonToStack(json), x, y, null);
                }


                if(mouseX > x-1 && mouseX < x+itemSize+1) {
                    if(mouseY > y-1 && mouseY < y+itemSize+1) {
                        tooltipToDisplay.set(json);
                    }
                }
            });
        }

        /**
         * Item info (left) gui element rendering
         */

        rightSide = (int)(width*getInfoPaneOffsetFactor());
        leftSide = rightSide - paneWidth;

        if(scrollHeight.getValue() < 0) scrollHeight.setValue(0);

        if(informationPaneTitle != null || configOpen) {
            if(configOpen) {
                int boxLeft = leftSide+boxPadding-5;
                int boxRight = rightSide-boxPadding+5;
                drawRect(boxLeft, boxPadding-5, boxRight,
                        height-boxPadding+5, bg.getRGB());

                renderNavElement(leftSide+boxPadding, rightSide-boxPadding, 1,"Settings: ");

                AtomicReference<List<String>> textToDisplay = new AtomicReference<>(null);
                iterateSettingTile(new SettingsTileConsumer() {
                    public void consume(int x, int y, int tileWidth, int tileHeight, Options.Option<?> option) {
                        float mult = tileWidth/90f;

                        drawRect(x, y, x+tileWidth, y+tileHeight, fg.getRGB());
                        if(scaledresolution.getScaleFactor()==4) {
                            GL11.glScalef(0.5f,0.5f,1);
                            renderStringTrimWidth(option.displayName, fr, true, (x+(int)(8*mult))*2, (y+(int)(8*mult))*2,
                                    (tileWidth-(int)(16*mult))*2, new Color(100,255,150).getRGB(), 3);
                            GL11.glScalef(2,2,1);
                        } else {
                            renderStringTrimWidth(option.displayName, fr, true, x+(int)(8*mult), y+(int)(8*mult),
                                    tileWidth-(int)(16*mult), new Color(100,255,150).getRGB(), 3);
                        }

                        if(option.value instanceof Boolean) {
                            GlStateManager.color(1f, 1f, 1f, 1f);
                            Minecraft.getMinecraft().getTextureManager().bindTexture(((Boolean)option.value) ? on : off);
                            drawTexturedRect(x+tileWidth/2-(int)(32*mult), y+tileHeight-(int)(20*mult), (int)(48*mult), (int)(16*mult));

                            Minecraft.getMinecraft().getTextureManager().bindTexture(help);
                            drawTexturedRect(x+tileWidth/2+(int)(19*mult), y+tileHeight-(int)(19*mult), (int)(14*mult), (int)(14*mult));
                            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

                            if(mouseX > x+tileWidth/2+(int)(19*mult) && mouseX < x+tileWidth/2+(int)(19*mult)+(int)(14*mult)) {
                                if(mouseY > y+tileHeight-(int)(19*mult) && mouseY < y+tileHeight-(int)(19*mult)+(int)(14*mult)) {
                                    List<String> textLines = new ArrayList<>();
                                    textLines.add(option.displayName);
                                    textLines.add(EnumChatFormatting.GRAY+option.desc);
                                    textToDisplay.set(textLines);
                                }
                            }
                        } else {
                            if(!textConfigMap.containsKey(option)) {
                                textConfigMap.put(option, new GuiElementTextField(String.valueOf(option.value), 0));
                            }
                            GuiElementTextField tf = textConfigMap.get(option);
                            tf.setSize(tileWidth-(int)(20*mult), (int)(16*mult));
                            tf.render(x+(int)(10*mult), y+tileHeight-(int)(20*mult));

                            try {
                                tf.setCustomBorderColour(-1);
                                option.setValue(tf.getText());
                            } catch(Exception e) {
                                tf.setCustomBorderColour(Color.RED.getRGB());
                            }
                        }
                    }
                });
                if(textToDisplay.get() != null) {
                    Utils.drawHoveringText(textToDisplay.get(), mouseX, mouseY, width, height, 200, fr);
                }
            } else if(informationPaneImage != null) {
                int titleLen = fr.getStringWidth(informationPaneTitle);
                fr.drawString(informationPaneTitle, (leftSide+rightSide-titleLen)/2, +boxPadding + 5, Color.WHITE.getRGB());

                drawRect(leftSide+boxPadding-5, boxPadding-5, rightSide-boxPadding+5,
                        height-boxPadding+5, bg.getRGB());

                int imageW = paneWidth - boxPadding*2;
                float scaleF = IMAGE_WIDTH*ZOOM_FACTOR/(float)imageW;

                Minecraft.getMinecraft().getTextureManager().bindTexture(informationPaneImage);
                GlStateManager.color(1f, 1f, 1f, 1f);
                if(height-boxPadding*3 < informationPaneImageHeight/scaleF) {
                    if(scrollHeight.getValue() > informationPaneImageHeight/scaleF-height+boxPadding*3) {
                        scrollHeight.setValue((int)(informationPaneImageHeight/scaleF-height+boxPadding*3));
                    }
                    int yScroll = scrollHeight.getValue();

                    float vMin = yScroll/(informationPaneImageHeight/scaleF);
                    float vMax = (yScroll+height-boxPadding*3)/(informationPaneImageHeight/scaleF);
                    drawTexturedRect(leftSide+boxPadding, boxPadding*2, imageW, height-boxPadding*3,
                            0, 1, vMin, vMax);
                } else {
                    scrollHeight.setValue(0);

                    drawTexturedRect(leftSide+boxPadding, boxPadding*2, imageW, (int)(informationPaneImageHeight/scaleF));
                }
                GlStateManager.bindTexture(0);
            } else if(informationPane != null) {
                int titleLen = fr.getStringWidth(informationPaneTitle);
                int yScroll = -scrollHeight.getValue();
                fr.drawString(informationPaneTitle, (leftSide+rightSide-titleLen)/2, yScroll+boxPadding + 5, Color.WHITE.getRGB());

                int yOff = 20;
                for(int i=0; i<informationPane.length; i++) {
                    String line = informationPane[i];

                    yOff += renderStringTrimWidth(line, fr, false,leftSide+boxPadding + 5, yScroll+boxPadding + 10 + yOff,
                            width*1/3-boxPadding*2-10, Color.WHITE.getRGB(), -1);
                    yOff += 16;
                }

                int top = boxPadding - 5;
                int totalBoxHeight = yOff+14;
                int bottom = Math.max(top+totalBoxHeight, height-boxPadding+5);

                if(scrollHeight.getValue() > top+totalBoxHeight-(height-boxPadding+5)) {
                    scrollHeight.setValue(top+totalBoxHeight-(height-boxPadding+5));
                }
                drawRect(leftSide+boxPadding-5, yScroll+boxPadding-5, rightSide-boxPadding+5, yScroll+bottom, bg.getRGB());
            }
            if(rightSide > 0) {
                GlStateManager.color(1f, 1f, 1f, 1f);
                Minecraft.getMinecraft().getTextureManager().bindTexture(close);
                drawTexturedRect(rightSide-22, 7, 16, 16);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            }
        }

        //Render tooltip
        JsonObject json = tooltipToDisplay.get();
        if(json != null) {
            List<String> text = new ArrayList<>();
            text.add(json.get("displayname").getAsString());
            JsonArray lore = json.get("lore").getAsJsonArray();
            for(int i=0; i<lore.size(); i++) {
                text.add(lore.get(i).getAsString());
            }

            Float auctionPrice = manager.getAuctionPrices().get(json.get("internalname").getAsString());
            Pair<Float, Float> bazaarBuySellPrice = manager.getBazaarBuySellPrices().get(json.get("internalname").getAsString());

            boolean hasAuctionPrice = auctionPrice != null;
            boolean hasBazaarPrice = bazaarBuySellPrice != null;

            if(hasAuctionPrice || hasBazaarPrice) text.add("");
            if(hasBazaarPrice) text.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Bazaar Buy: "+
                    EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+bazaarBuySellPrice.getLeft()+" coins");
            if(hasBazaarPrice) text.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Bazaar Sell: "+
                    EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+bazaarBuySellPrice.getRight()+" coins");
            if(hasAuctionPrice) text.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Auction House Buy/Sell: "+
                    EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+auctionPrice+" coins");

            boolean hasClick = false;
            boolean hasInfo = false;
            if(json.has("clickcommand") && !json.get("clickcommand").getAsString().isEmpty()) {
                hasClick = true;
            }
            if(json.has("info") && json.get("info").getAsJsonArray().size() > 0) {
                hasInfo = true;
            }

            if(hasClick || hasInfo) text.add("");
            if(hasClick) text.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"LMB/R : View recipe!");
            if(hasInfo) text.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"RMB : View additional information!");

            Utils.drawHoveringText(text, mouseX, mouseY, width, height, -1, fr);
        }
    }

    public float getItemPaneOffsetFactor() {
        return itemPaneOffsetFactor.getValue() * getWidthMult() + (1-getWidthMult());
    }

    public float getInfoPaneOffsetFactor() {
        if(configOpen) return infoPaneOffsetFactor.getValue() * getWidthMult();
        if(itemPaneOffsetFactor.getValue() < 1 && itemPaneOffsetFactor.getValue() == itemPaneOffsetFactor.getTarget()) {
            return infoPaneOffsetFactor.getValue() * getWidthMult();
        } else {
            return Math.min(infoPaneOffsetFactor.getValue(), 1-getItemPaneOffsetFactor());
        }
    }

    public int renderStringTrimWidth(String str, FontRenderer fr, boolean shadow, int x, int y, int len, int colour, int maxLines) {
        int yOff = 0;
        String excess;
        String trimmed = trimToWidth(str, len);

        String colourCodes = "";
        Pattern pattern = Pattern.compile("\\u00A7.");
        Matcher matcher = pattern.matcher(trimmed);
        while(matcher.find()) {
            colourCodes += matcher.group();
        }

        boolean firstLine = true;
        int trimmedCharacters = trimmed.length();
        int lines = 0;
        while((lines++<maxLines) || maxLines<0) {
            if(trimmed.length() == str.length()) {
                fr.drawString(trimmed, x, y + yOff, colour, shadow);
                break;
            } else if(trimmed.isEmpty()) {
                yOff -= 12;
                break;
            } else {
                if(firstLine) {
                    fr.drawString(trimmed, x, y + yOff, colour, shadow);
                    firstLine = false;
                } else {
                    if(trimmed.startsWith(" ")) {
                        trimmed = trimmed.substring(1);
                    }
                    fr.drawString(colourCodes + trimmed, x, y + yOff, colour, shadow);
                }

                excess = str.substring(trimmedCharacters);
                trimmed = trimToWidth(excess, len);
                trimmedCharacters += trimmed.length();
                yOff += 12;
            }
        }
        return yOff;
    }

    public String trimToWidth(String str, int len) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        String trim = fr.trimStringToWidth(str, len);

        if(str.length() != trim.length() && !trim.endsWith(" ")) {
            char next = str.charAt(trim.length());
            if(next != ' ') {
                String[] split = trim.split(" ");
                String last = split[split.length-1];
                if(last.length() < 8) {
                    trim = trim.substring(0, trim.length()-last.length());
                }
            }
        }

        return trim;
    }

    private abstract class SettingsTileConsumer {
        public abstract void consume(int x, int y, int tileWidth, int tileHeight, Options.Option<?> option);
    }

    public int getTotalOptions() {
        return manager.config.getOptions().size();
    }

    public void iterateSettingTile(SettingsTileConsumer settingsTileConsumer) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        int numHorz = scaledresolution.getScaleFactor() >= 3 ? 2 : 3;

        int paneWidth = (int)(width/3*getWidthMult());
        int rightSide = (int)(width*getInfoPaneOffsetFactor());
        int leftSide = rightSide - paneWidth;

        int boxLeft = leftSide+boxPadding-5;
        int boxRight = rightSide-boxPadding+5;

        int boxWidth = boxRight-boxLeft;
        int tilePadding = 7;
        int tileWidth = (boxWidth-tilePadding*4)/numHorz;
        int tileHeight = tileWidth*3/4;

        int x=0;
        int y=tilePadding+boxPadding+getSearchBarYSize();
        for(int i=0; i<getTotalOptions(); i++) {
            if(i!=0 && i%numHorz==0) {
                x = 0;
                y += tileHeight+tilePadding;
            }
            x+=tilePadding;

            settingsTileConsumer.consume(boxLeft+x, y, tileWidth, tileHeight, manager.config.getOptions().get(i));

            x+=tileWidth;
        }
    }

    private void drawItemStack(ItemStack stack, int x, int y, String altText) {
        RenderItem itemRender = Minecraft.getMinecraft().getRenderItem();

        RenderHelper.enableGUIStandardItemLighting();
        itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();
    }

    private void drawTexturedRect(float x, float y, float width, float height) {
        drawTexturedRect(x, y, width, height, 0, 1, 0 , 1);
    }

    private void drawTexturedRect(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax) {
        drawTexturedRect(x, y, width, height, uMin, uMax, vMin , vMax, GL11.GL_LINEAR);
    }

    private void drawTexturedRect(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax, int filter) {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glTexCoord2f(uMin, vMin);
        GL11.glVertex3f(x, y, 0.0F);
        GL11.glTexCoord2f(uMin, vMax);
        GL11.glVertex3f(x, y+height, 0.0F);
        GL11.glTexCoord2f(uMax, vMin);
        GL11.glVertex3f(x+width, y, 0.0F);
        GL11.glTexCoord2f(uMax, vMax);
        GL11.glVertex3f(x+width, y+height, 0.0F);
        GL11.glEnd();

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        GlStateManager.disableBlend();

        /*worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + height + offset, this.zLevel).tex(uMin, vMax).endVertex();
        worldrenderer.pos(x + width + offset, y + height + offset, this.zLevel).tex(uMax, vMax).endVertex();
        worldrenderer.pos(x + width + offset, y, this.zLevel).tex(uMax, vMin).endVertex();
        worldrenderer.pos(x, y, this.zLevel).tex(uMin, vMin).endVertex();
        tessellator.draw();*/

    }
}