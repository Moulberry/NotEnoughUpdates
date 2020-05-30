package io.github.moulberry.notenoughupdates;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.infopanes.*;
import io.github.moulberry.notenoughupdates.itemeditor.NEUItemEditor;
import io.github.moulberry.notenoughupdates.util.LerpingFloat;
import io.github.moulberry.notenoughupdates.util.LerpingInteger;
import io.github.moulberry.notenoughupdates.util.TexLoc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Matrix4f;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.moulberry.notenoughupdates.GuiTextures.*;

public class NEUOverlay extends Gui {

    private NEUManager manager;

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

    private int searchBarXSize = 200;
    private final int searchBarYOffset = 10;
    private final int searchBarYSize = 40;
    private final int searchBarPadding = 2;

    public static final int BOX_PADDING = 15;
    public static  final int ITEM_PADDING = 4;
    public static  final int ITEM_SIZE = 16;

    private Color bg = new Color(90, 90, 140, 50);
    private Color fg = new Color(100,100,100, 255);

    //private String informationPaneTitle;
    //private ResourceLocation informationPaneImage = null;
    //private String[] informationPane;
    //private AtomicInteger webpageAwaitID = new AtomicInteger(-1);
    //private boolean configOpen = false;
    private InfoPane activeInfoPane = null;

    private TreeSet<JsonObject> searchedItems = null;
    private JsonObject[] searchedItemsArr = null;

    private boolean itemPaneOpen = false;
    private boolean hoveringItemPaneToggle = false;

    private int page = 0;

    private LerpingFloat itemPaneOffsetFactor = new LerpingFloat(1);
    private LerpingInteger itemPaneTabOffset = new LerpingInteger(20, 50);
    private LerpingFloat infoPaneOffsetFactor = new LerpingFloat(0);

    private boolean searchMode = false;
    private long millisLastLeftClick = 0;

    boolean mouseDown = false;

    private boolean redrawItems = false;

    private boolean searchBarHasFocus = false;
    GuiTextField textField = new GuiTextField(0, null, 0, 0, 0, 0);

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
            String loreS = StringUtils.join(loreA, "\n");

            String name = item.get("displayname").getAsString();
            switch(item.get("infoType").getAsString()) {
                case "WIKI_URL":
                    displayInformationPane(HTMLInfoPane.createFromWikiUrl(this, manager, name, loreS));
                    return;
                case "WIKI":
                    displayInformationPane(HTMLInfoPane.createFromWiki(this, manager, name, loreS));
                    return;
                case "HTML":
                    displayInformationPane(new HTMLInfoPane(this, manager, name, loreS));
                    return;
            }
            displayInformationPane(new TextInfoPane(this, manager, name, loreS));
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
            iterateItemSlots(new ItemSlotConsumer() {
                public void consume(int x, int y, int id) {
                    if(mouseX >= x-1 && mouseX <= x+ITEM_SIZE+1) {
                        if(mouseY >= y-1 && mouseY <= y+ITEM_SIZE+1) {
                            clickedItem.set(true);

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
                }
            });
            if(!clickedItem.get()) {
                int paneWidth = (int)(width/3*getWidthMult());
                int leftSide = (int)(width*getItemPaneOffsetFactor());
                int rightSide = leftSide+paneWidth-BOX_PADDING-getItemBoxXPadding();
                leftSide = leftSide+BOX_PADDING+getItemBoxXPadding();

                FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
                int maxPages = getMaxPages();
                String name = scaledresolution.getScaleFactor()<4?"Page: ":"";
                float maxStrLen = fr.getStringWidth(EnumChatFormatting.BOLD+name + maxPages + "/" + maxPages);
                float maxButtonXSize = (rightSide-leftSide+2 - maxStrLen*0.5f - 10)/2f;
                int buttonXSize = (int)Math.min(maxButtonXSize, getSearchBarYSize()*480/160f);
                int ySize = (int)(buttonXSize/480f*160);
                int yOffset = (int)((getSearchBarYSize()-ySize)/2f);
                int top = BOX_PADDING+yOffset;

                if(mouseY >= top && mouseY <= top+ySize) {
                    int leftPrev = leftSide-1;
                    if(mouseX > leftPrev && mouseX < leftPrev+buttonXSize) { //"Previous" button
                        setPage(page-1);
                    }
                    int leftNext = rightSide+1-buttonXSize;
                    if(mouseX > leftNext && mouseX < leftNext+buttonXSize) { //"Next" button
                        setPage(page+1);
                    }
                }

                float sortIconsMinX = (sortIcons.length+orderIcons.length)*(ITEM_SIZE+ITEM_PADDING)+ITEM_SIZE;
                float availableX = rightSide-(leftSide+BOX_PADDING+getItemBoxXPadding());
                float sortOrderScaleFactor = Math.min(1, availableX / sortIconsMinX);

                int scaledITEM_SIZE = (int)(ITEM_SIZE*sortOrderScaleFactor);
                int scaledItemPaddedSize = (int)((ITEM_SIZE+ITEM_PADDING)*sortOrderScaleFactor);
                int iconTop = height-BOX_PADDING-(ITEM_SIZE+scaledITEM_SIZE)/2-1;

                if(mouseY >= iconTop && mouseY <= iconTop+scaledITEM_SIZE) {
                    for(int i=0; i<orderIcons.length; i++) {
                        int orderIconX = leftSide+BOX_PADDING+getItemBoxXPadding()+i*scaledItemPaddedSize;
                        if(mouseX >= orderIconX && mouseX <= orderIconX+scaledITEM_SIZE) {
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
                        int sortIconX = rightSide-scaledITEM_SIZE-i*scaledItemPaddedSize;
                        if(mouseX >= sortIconX && mouseX <= sortIconX+scaledITEM_SIZE) {
                            manager.config.sortMode.value = new Double(i);
                            updateSearch();
                        }
                    }
                }
            }
            return true;
        }

        if(Mouse.getEventButton() == 2) {
            Slot slot = Utils.getSlotUnderMouse((GuiContainer)Minecraft.getMinecraft().currentScreen);
            if(slot != null) {
                ItemStack hover = slot.getStack();
                if(hover != null) {
                    textField.setText("id:"+manager.getInternalNameForItem(hover));
                    updateSearch();
                    searchMode = true;
                    return true;
                }
            }
        }

        //Clicking on "close info pane" button
        if(mouseX > width*getInfoPaneOffsetFactor()-22 && mouseX < width*getInfoPaneOffsetFactor()-6) {
            if(mouseY > 7 && mouseY < 23) {
                if(Mouse.getEventButtonState() && Mouse.getEventButton() < 2) { //Left or right click up
                    displayInformationPane(null);
                    return true;
                }
            }
        }

        //Search bar
        if(mouseX >= width/2 - getSearchBarXSize()/2 && mouseX <= width/2 + getSearchBarXSize()/2) {
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
            if(mouseX > width/2 + getSearchBarXSize()/2 + paddingUnscaled*6 &&
                    mouseX < width/2 + getSearchBarXSize()/2 + paddingUnscaled*6 + iconSize) {
                if(Mouse.getEventButtonState()) {
                    displayInformationPane(HTMLInfoPane.createFromWikiUrl(this, manager, "Help",
                            "https://moulberry.github.io/files/neu_help.html"));
                }
            } else if(mouseX > width/2 - getSearchBarXSize()/2 - paddingUnscaled*6 - iconSize &&
                    mouseX < width/2 - getSearchBarXSize()/2 - paddingUnscaled*6) {
                if(Mouse.getEventButtonState()) {
                    if(activeInfoPane instanceof SettingsInfoPane) {
                        displayInformationPane(null);
                    } else {
                        displayInformationPane(new SettingsInfoPane(this, manager));
                    }
                }
            }
        }

        if(activeInfoPane != null) {
            if(mouseX < width*getInfoPaneOffsetFactor()) {
                activeInfoPane.mouseInput(width, height, mouseX, mouseY, mouseDown);
                return true;
            }
        }

        return false;
    }

    public int getSearchBarXSize() {
        if(scaledresolution.getScaleFactor()==4) return (int)(searchBarXSize*0.8);
        return searchBarXSize;
    }

    public void displayInformationPane(InfoPane pane) {
        if(pane == null) {
            infoPaneOffsetFactor.setTarget(0);
        } else {
            infoPaneOffsetFactor.setTarget(1/3f);
        }
        infoPaneOffsetFactor.resetTimer();
        this.activeInfoPane = pane;
    }

    public InfoPane getActiveInfoPane() {
        return activeInfoPane;
    }

    /*public void displayInformationPane(String title, String infoType, String[] info) {
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
    }*/

    public int getClickedIndex(int mouseX, int mouseY) {
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        int xComp = mouseX - (width/2 - getSearchBarXSize()/2 + 5);

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
    public boolean keyboardInput(boolean hoverInv) {
        if(Minecraft.getMinecraft().currentScreen == null) return false;
        Keyboard.enableRepeatEvents(true);

        if(Keyboard.isKeyDown(Keyboard.KEY_Y)) {
            displayInformationPane(new DevInfoPane(this, manager));
            //displayInformationPane(new QOLInfoPane(this, manager));
        }

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
                if(activeInfoPane != null) {
                    activeInfoPane.keyboardInput();
                }

                AtomicReference<String> internalname = new AtomicReference<>(null);
                AtomicReference<ItemStack> itemstack = new AtomicReference<>(null);
                Slot slot = Utils.getSlotUnderMouse((GuiContainer)Minecraft.getMinecraft().currentScreen);
                if(slot != null) {
                    ItemStack hover = slot.getStack();
                    if(hover != null) {
                        internalname.set(manager.getInternalNameForItem(hover));
                        itemstack.set(hover);
                    }
                } else if(!hoverInv) {
                    int height = scaledresolution.getScaledHeight();

                    int mouseX = Mouse.getX() / scaledresolution.getScaleFactor();
                    int mouseY = height - Mouse.getY() / scaledresolution.getScaleFactor();

                    iterateItemSlots(new ItemSlotConsumer() {
                        public void consume(int x, int y, int id) {
                            if (mouseX >= x - 1 && mouseX <= x + ITEM_SIZE + 1) {
                                if (mouseY >= y - 1 && mouseY <= y + ITEM_SIZE + 1) {
                                    JsonObject json = getSearchedItemPage(id);
                                    if (json != null) internalname.set(json.get("internalname").getAsString());
                                }
                            }
                        }
                    });
                }
                if(internalname.get() != null) {
                    if(itemstack.get() != null) {
                        if(manager.config.enableItemEditing.value && Keyboard.getEventCharacter() == 'k') {
                            Minecraft.getMinecraft().displayGuiScreen(new NEUItemEditor(manager,
                                    internalname.get(), manager.getJsonForItem(itemstack.get())));
                            return true;
                        }
                    }
                    JsonObject item = manager.getItemInformation().get(internalname.get());
                    if(item != null) {
                        if(Keyboard.getEventCharacter() == 'u') {
                            manager.displayGuiItemUsages(internalname.get(), "");
                            return true;
                        } else if(Keyboard.getEventCharacter() == 'f') {
                            toggleRarity(item.get("internalname").getAsString());
                            return true;
                        } else if(Keyboard.getEventCharacter() == 'r') {
                            manager.showRecipe(item);
                            return true;
                        } else if(Keyboard.getEventCharacter() == 'l') {
                            if(Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode) {
                                Minecraft.getMinecraft().thePlayer.inventory.addItemStackToInventory(
                                        manager.jsonToStack(item));
                            }
                        } else if(manager.config.enableItemEditing.value && Keyboard.getEventCharacter() == 'k') {
                            Minecraft.getMinecraft().displayGuiScreen(new NEUItemEditor(manager,
                                    internalname.get(), item));
                            return true;
                        }
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
        redrawItems = true;
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
        return (((int)(width/3*getWidthMult())-2*BOX_PADDING)%(ITEM_SIZE+ITEM_PADDING)+ITEM_PADDING)/2;
    }

    private abstract class ItemSlotConsumer {
        public abstract void consume(int x, int y, int id);
    }

    public void iterateItemSlots(ItemSlotConsumer itemSlotConsumer) {
        int width = scaledresolution.getScaledWidth();
        int itemBoxXPadding = getItemBoxXPadding();
        iterateItemSlots(itemSlotConsumer, (int)(width*getItemPaneOffsetFactor())+BOX_PADDING+itemBoxXPadding);
    }

    /**
     * Iterates through all the item slots in the right panel and calls a ItemSlotConsumer for each slot with
     * arguments equal to the slot's x and y position respectively. This is used in order to prevent
     * code duplication issues.
     */
    public void iterateItemSlots(ItemSlotConsumer itemSlotConsumer, int xStart) {
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        int paneWidth = (int)(width/3*getWidthMult());
        int itemBoxYPadding = ((height-getSearchBarYSize()-2*BOX_PADDING-ITEM_SIZE-2)%(ITEM_SIZE+ITEM_PADDING)+ITEM_PADDING)/2;

        int yStart = BOX_PADDING+getSearchBarYSize()+itemBoxYPadding;
        int itemBoxXPadding = getItemBoxXPadding();
        int xEnd = xStart+paneWidth-BOX_PADDING*2-ITEM_SIZE-itemBoxXPadding;
        int yEnd = height-BOX_PADDING-ITEM_SIZE-2-itemBoxYPadding;

        //Render the items, displaying the tooltip if the cursor is over the item
        int id = 0;
        for(int y = yStart; y < yEnd; y+=ITEM_SIZE+ITEM_PADDING) {
            for(int x = xStart; x < xEnd; x+=ITEM_SIZE+ITEM_PADDING) {
                itemSlotConsumer.consume(x, y, id++);
            }
        }
    }

    public float getWidthMult() {
        float scaleFMult = 1;
        if(scaledresolution.getScaleFactor()==4) scaleFMult = 0.9f;
        return (float)Math.max(0.5, Math.min(1.5, manager.config.paneWidthMult.value.floatValue()))*scaleFMult;
    }

    /**
     * Calculates the number of horizontal item slots.
     */
    public int getSlotsXSize() {
        int width = scaledresolution.getScaledWidth();

        int paneWidth = (int)(width/3*getWidthMult());
        int itemBoxXPadding = (((int)(width-width*getItemPaneOffsetFactor())-2*BOX_PADDING)%(ITEM_SIZE+ITEM_PADDING)+ITEM_PADDING)/2;
        int xStart = (int)(width*getItemPaneOffsetFactor())+BOX_PADDING+itemBoxXPadding;
        int xEnd = (int)(width*getItemPaneOffsetFactor())+paneWidth-BOX_PADDING-ITEM_SIZE;

        return (int)Math.ceil((xEnd - xStart)/((float)(ITEM_SIZE+ITEM_PADDING)));
    }

    /**
     * Calculates the number of vertical item slots.
     */
    public int getSlotsYSize() {
        int height = scaledresolution.getScaledHeight();

        int itemBoxYPadding = ((height-getSearchBarYSize()-2*BOX_PADDING-ITEM_SIZE-2)%(ITEM_SIZE+ITEM_PADDING)+ITEM_PADDING)/2;
        int yStart = BOX_PADDING+getSearchBarYSize()+itemBoxYPadding;
        int yEnd = height-BOX_PADDING-ITEM_SIZE-2-itemBoxYPadding;

        return (int)Math.ceil((yEnd - yStart)/((float)(ITEM_SIZE+ITEM_PADDING)));
    }

    public int getMaxPages() {
        if(getSearchedItems().length == 0) return 1;
        return (int)Math.ceil(getSearchedItems().length/(float)getSlotsYSize()/getSlotsXSize());
    }

    /**
     * Takes in the x and y coordinates of a slot and returns the id of that slot.
     */
    /*public int getSlotId(int x, int y) {
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        int itemBoxXPadding = (((int)(width-width*getItemPaneOffsetFactor())-2*BOX_PADDING)%(ITEM_SIZE+ITEM_PADDING)+ITEM_PADDING)/2;
        int itemBoxYPadding = ((height-getSearchBarYSize()-2*BOX_PADDING-ITEM_SIZE-2)%(ITEM_SIZE+ITEM_PADDING)+ITEM_PADDING)/2;

        int xStart = (int)(width*getItemPaneOffsetFactor())+BOX_PADDING+itemBoxXPadding;
        int yStart = BOX_PADDING+getSearchBarYSize()+itemBoxYPadding;

        int xIndex = (x-xStart)/(ITEM_SIZE+ITEM_PADDING);
        int yIndex = (y-yStart)/(ITEM_SIZE+ITEM_PADDING);
        return xIndex + yIndex*getSlotsXSize();
    }*/

    public int getSearchBarYSize() {
        return Math.max(searchBarYSize/scaledresolution.getScaleFactor(), ITEM_SIZE);
    }

    public void renderNavElement(int leftSide, int rightSide, int maxPages, int page, String name) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

        String pageText = EnumChatFormatting.BOLD+name + page + "/" + maxPages;

        float maxStrLen = fr.getStringWidth(EnumChatFormatting.BOLD+name + maxPages + "/" + maxPages);
        float maxButtonXSize = (rightSide-leftSide+2 - maxStrLen*0.5f - 10)/2f;
        int buttonXSize = (int)Math.min(maxButtonXSize, getSearchBarYSize()*480/160f);
        int ySize = (int)(buttonXSize/480f*160);
        int yOffset = (int)((getSearchBarYSize()-ySize)/2f);
        int top = BOX_PADDING+yOffset;

        /*drawRect(leftSide-1, top,
                rightSide+1,
                top+ySize, fg.getRGB());*/

        int leftPressed = 0;
        int rightPressed = 0;

        if(Mouse.isButtonDown(0) || Mouse.isButtonDown(1)) {
            int height = scaledresolution.getScaledHeight();

            int mouseX = Mouse.getX() / scaledresolution.getScaleFactor();
            int mouseY = height - Mouse.getY() / scaledresolution.getScaleFactor();

            if(mouseY >= top && mouseY <= top+ySize) {
                int leftPrev = leftSide-1;
                if(mouseX > leftPrev && mouseX < leftPrev+buttonXSize) { //"Previous" button
                    leftPressed = 1;
                }
                int leftNext = rightSide+1-buttonXSize;
                if(mouseX > leftNext && mouseX < leftNext+buttonXSize) { //"Next" button
                    rightPressed = 1;
                }
            }
        }

        drawRect(leftSide-1, top, leftSide-1+buttonXSize, top+ySize, fg.getRGB());
        GlStateManager.color(1f, 1f, 1f, 1f);
        Minecraft.getMinecraft().getTextureManager().bindTexture(rightarrow);
        Utils.drawTexturedRect(leftSide-1+leftPressed,
                top+leftPressed,
                buttonXSize, ySize, 1, 0, 0, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(rightarrow_overlay);
        Utils.drawTexturedRect(leftSide-1,
                top,
                buttonXSize, ySize, 1-leftPressed, leftPressed, 1-leftPressed, leftPressed);
        GlStateManager.bindTexture(0);
        Utils.drawStringCenteredScaled(EnumChatFormatting.BOLD+"Prev", fr,
                leftSide-1+buttonXSize*300/480f+leftPressed,
                top+ySize/2f+leftPressed, false,
                (int)(buttonXSize*240/480f), Color.BLACK.getRGB());

        drawRect(rightSide+1-buttonXSize, top, rightSide+1, top+ySize, fg.getRGB());
        GlStateManager.color(1f, 1f, 1f, 1f);
        Minecraft.getMinecraft().getTextureManager().bindTexture(rightarrow);
        Utils.drawTexturedRect(rightSide+1-buttonXSize+rightPressed,
                top+rightPressed,
                buttonXSize, ySize);
        Minecraft.getMinecraft().getTextureManager().bindTexture(rightarrow_overlay);
        Utils.drawTexturedRect(rightSide+1-buttonXSize,
                top,
                buttonXSize, ySize, 1-rightPressed, rightPressed, 1-rightPressed, rightPressed);
        GlStateManager.bindTexture(0);
        Utils.drawStringCenteredScaled(EnumChatFormatting.BOLD+"Next", fr,
                rightSide+1-buttonXSize*300/480f+rightPressed,
                top+ySize/2f+rightPressed, false,
                (int)(buttonXSize*240/480f), Color.BLACK.getRGB());

        int strMaxLen = rightSide-leftSide-2*buttonXSize-10;

        drawRect(leftSide-1+buttonXSize+3, top, rightSide+1-buttonXSize-3, top+ySize,
                new Color(177,177,177).getRGB());
        drawRect(leftSide+buttonXSize+3, top+1, rightSide+1-buttonXSize-3, top+ySize,
                new Color(50,50,50).getRGB());
        drawRect(leftSide+buttonXSize+3, top+1, rightSide-buttonXSize-3, top+ySize-1, fg.getRGB());
        Utils.drawStringCenteredScaledMaxWidth(pageText, fr,(leftSide+rightSide)/2,
                top+ySize/2f, false, strMaxLen, Color.BLACK.getRGB());
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
            GuiContainer inv = (GuiContainer) Minecraft.getMinecraft().currentScreen;
            int guiLeftI = (int)Utils.getField(GuiContainer.class, inv, "guiLeft", "field_147003_i");
            int guiTopI = (int)Utils.getField(GuiContainer.class, inv, "guiTop", "field_147009_r");

            GL11.glTranslatef(0, 0, 260);
            int overlay = new Color(0, 0, 0, 100).getRGB();
            for(Slot slot : inv.inventorySlots.inventorySlots) {
                if(slot.getStack() == null || !manager.doesStackMatchSearch(slot.getStack(), textField.getText())) {
                    drawRect(guiLeftI+slot.xDisplayPosition, guiTopI+slot.yDisplayPosition,
                            guiLeftI+slot.xDisplayPosition+16, guiTopI+slot.yDisplayPosition+16,
                            overlay);
                }
            }
            if(Utils.getSlotUnderMouse(inv) != null) {
                ItemStack stack = Utils.getSlotUnderMouse(inv).getStack();
                //Minecraft.getMinecraft().currentScreen.renderToolTip(stack, mouseX, mouseY);
                Class<?>[] params = new Class[]{ItemStack.class, int.class, int.class};
                Method renderToolTip = Utils.getMethod(GuiScreen.class, params, "renderToolTip", "func_146285_a");
                if(renderToolTip != null) {
                    renderToolTip.setAccessible(true);
                    try {
                        renderToolTip.invoke(Minecraft.getMinecraft().currentScreen, stack, mouseX, mouseY);
                    } catch(Exception e) {}
                }
            }
            GL11.glTranslatef(0, 0, -260);
        }
    }

    Shader blurShaderHorz = null;
    Framebuffer blurOutputHorz = null;
    Shader blurShaderVert = null;
    Framebuffer blurOutputVert = null;

    private Matrix4f createProjectionMatrix(int width, int height) {
        Matrix4f projMatrix  = new Matrix4f();
        projMatrix.setIdentity();
        projMatrix.m00 = 2.0F / (float)width;
        projMatrix.m11 = 2.0F / (float)(-height);
        projMatrix.m22 = -0.0020001999F;
        projMatrix.m33 = 1.0F;
        projMatrix.m03 = -1.0F;
        projMatrix.m13 = 1.0F;
        projMatrix.m23 = -1.0001999F;
        return projMatrix;
    }

    private double lastBgBlurFactor = 5;
    private void blurBackground() {
        int width = Minecraft.getMinecraft().displayWidth;
        int height = Minecraft.getMinecraft().displayHeight;

        if(manager.config.bgBlurFactor.value <= 0) return;

        if(blurOutputHorz == null) {
            blurOutputHorz = new Framebuffer(width, height, false);
            blurOutputHorz.setFramebufferFilter(GL11.GL_NEAREST);
        }
        if(blurOutputVert == null) {
            blurOutputVert = new Framebuffer(width, height, false);
            blurOutputVert.setFramebufferFilter(GL11.GL_NEAREST);
        }
        if(blurOutputHorz.framebufferWidth != width || blurOutputHorz.framebufferHeight != height) {
            blurOutputHorz.createBindFramebuffer(width, height);
            blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }
        if(blurOutputVert.framebufferWidth != width || blurOutputVert.framebufferHeight != height) {
            blurOutputVert.createBindFramebuffer(width, height);
            blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }

        if(blurShaderHorz == null) {
            try {
                blurShaderHorz = new Shader(Minecraft.getMinecraft().getResourceManager(), "blur",
                        Minecraft.getMinecraft().getFramebuffer(), blurOutputHorz);
                blurShaderHorz.getShaderManager().getShaderUniform("BlurDir").set(1, 0);
                blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
            } catch(Exception e) { }
        }
        if(blurShaderVert == null) {
            try {
                blurShaderVert = new Shader(Minecraft.getMinecraft().getResourceManager(), "blur",
                        blurOutputHorz, blurOutputVert);
                blurShaderVert.getShaderManager().getShaderUniform("BlurDir").set(0, 1);
                blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
            } catch(Exception e) { }
        }
        if(blurShaderHorz != null && blurShaderVert != null) {
            if(manager.config.bgBlurFactor.value != lastBgBlurFactor) {
                lastBgBlurFactor = Math.max(0, Math.min(50, manager.config.bgBlurFactor.value));
                blurShaderHorz.getShaderManager().getShaderUniform("Radius").set((float)lastBgBlurFactor);
                blurShaderVert.getShaderManager().getShaderUniform("Radius").set((float)lastBgBlurFactor);
            }
            GL11.glPushMatrix();
            blurShaderHorz.loadShader(0);
            blurShaderVert.loadShader(0);
            GlStateManager.enableDepth();
            GL11.glPopMatrix();

            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }
    }

    public void renderBlurredBackground(int width, int height, int x, int y, int blurWidth, int blurHeight) {
        if(manager.config.bgBlurFactor.value <= 0) return;

        int f = scaledresolution.getScaleFactor();
        float uMin = x/(float)width;
        float uMax = (x+blurWidth)/(float)width;
        float vMin = y/(float)height;
        float vMax = (y+blurHeight)/(float)height;

        blurOutputVert.bindFramebufferTexture();
        GlStateManager.color(1f, 1f, 1f, 1f);
        //Utils.setScreen(width*f, height*f, f);
        Utils.drawTexturedRect(x, y, blurWidth, blurHeight, uMin, uMax, vMax, vMin);
        //Utils.setScreen(width, height, f);
        blurOutputVert.unbindFramebufferTexture();
    }

    int guiScaleLast = 0;

    /**
     * Renders the search bar, item selection (right) and item info (left) gui elements.
     */
    public void render(int mouseX, int mouseY, boolean hoverInv) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        if(guiScaleLast != scaledresolution.getScaleFactor()) {
            guiScaleLast = scaledresolution.getScaleFactor();
            redrawItems = true;
        }

        blurBackground();

        yaw++;
        yaw %= 360;

        manager.updatePrices();

        int opacity = Math.min(255, Math.max(0, manager.config.bgOpacity.value.intValue()));
        bg = new Color((bg.getRGB() & 0x00ffffff) | opacity << 24, true);

        opacity = Math.min(255, Math.max(0, manager.config.fgOpacity.value.intValue()));
        Color fgCustomOpacity = new Color((fg.getRGB() & 0x00ffffff) | opacity << 24, true);
        Color fgFavourite = new Color(limCol(fg.getRed()+20), limCol(fg.getGreen()+10), limCol(fg.getBlue()-10), opacity);
        Color fgFavourite2 = new Color(limCol(fg.getRed()+100), limCol(fg.getGreen()+50), limCol(fg.getBlue()-50), opacity);

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

        if(page > getMaxPages()-1) setPage(getMaxPages()-1);
        if(page < 0) setPage(0);

        GlStateManager.disableLighting();

        /**
         * Search bar
         */
        int paddingUnscaled = searchBarPadding/scaledresolution.getScaleFactor();
        if(paddingUnscaled < 1) paddingUnscaled = 1;

        int topTextBox = height - searchBarYOffset - getSearchBarYSize();

        /*Minecraft.getMinecraft().getTextureManager().bindTexture(logo_bg);
        GlStateManager.color(1f, 1f, 1f, 1f);
        Utils.drawTexturedRect((width)/2-37,
                height - searchBarYOffset - getSearchBarYSize()-30,
                74, 54);
        GlStateManager.bindTexture(0);*/

        //Search bar background
        drawRect(width/2 - getSearchBarXSize()/2 - paddingUnscaled,
                topTextBox - paddingUnscaled,
                width/2 + getSearchBarXSize()/2 + paddingUnscaled,
                height - searchBarYOffset + paddingUnscaled, searchMode ? Color.YELLOW.getRGB() : Color.WHITE.getRGB());
        drawRect(width/2 - getSearchBarXSize()/2,
                topTextBox,
                width/2 + getSearchBarXSize()/2,
                height - searchBarYOffset, Color.BLACK.getRGB());

        /*Minecraft.getMinecraft().getTextureManager().bindTexture(logo_fg);
        GlStateManager.color(1f, 1f, 1f, 1f);
        Utils.drawTexturedRect((width)/2-37,
                height - searchBarYOffset - getSearchBarYSize()-27,
                74, 54);
        GlStateManager.bindTexture(0);*/

        //Settings
        int iconSize = getSearchBarYSize()+paddingUnscaled*2;
        Minecraft.getMinecraft().getTextureManager().bindTexture(settings);
        drawRect(width/2 - getSearchBarXSize()/2 - paddingUnscaled*6 - iconSize,
                topTextBox - paddingUnscaled,
                width/2 - getSearchBarXSize()/2 - paddingUnscaled*6,
                topTextBox - paddingUnscaled + iconSize, Color.WHITE.getRGB());

        drawRect(width/2 - getSearchBarXSize()/2 - paddingUnscaled*5 - iconSize,
                topTextBox,
                width/2 - getSearchBarXSize()/2 - paddingUnscaled*7,
                topTextBox - paddingUnscaled*2 + iconSize, Color.GRAY.getRGB());
        GlStateManager.color(1f, 1f, 1f, 1f);
        Utils.drawTexturedRect(width/2 - getSearchBarXSize()/2 - paddingUnscaled*6 - iconSize, topTextBox - paddingUnscaled, iconSize, iconSize);
        GlStateManager.bindTexture(0);

        //Help
        Minecraft.getMinecraft().getTextureManager().bindTexture(help);
        drawRect(width/2 + getSearchBarXSize()/2 + paddingUnscaled*6,
                topTextBox - paddingUnscaled,
                width/2 + getSearchBarXSize()/2 + paddingUnscaled*6 + iconSize,
                topTextBox - paddingUnscaled + iconSize, Color.WHITE.getRGB());

        drawRect(width/2 + getSearchBarXSize()/2 + paddingUnscaled*7,
                topTextBox,
                width/2 + getSearchBarXSize()/2 + paddingUnscaled*5 + iconSize,
                topTextBox - paddingUnscaled*2 + iconSize, Color.GRAY.getRGB());
        GlStateManager.color(1f, 1f, 1f, 1f);
        Utils.drawTexturedRect(width/2 + getSearchBarXSize()/2 + paddingUnscaled*7, topTextBox,
                iconSize-paddingUnscaled*2, iconSize-paddingUnscaled*2);
        GlStateManager.bindTexture(0);

        //Search bar text
        fr.drawString(textField.getText(), width/2 - getSearchBarXSize()/2 + 5,
                topTextBox+(getSearchBarYSize()-8)/2, Color.WHITE.getRGB());

        //Determines position of cursor. Cursor blinks on and off every 500ms.
        if(searchBarHasFocus && System.currentTimeMillis()%1000>500) {
            String textBeforeCursor = textField.getText().substring(0, textField.getCursorPosition());
            int textBeforeCursorWidth = fr.getStringWidth(textBeforeCursor);
            drawRect(width/2 - getSearchBarXSize()/2 + 5 + textBeforeCursorWidth,
                    topTextBox+(getSearchBarYSize()-8)/2-1,
                    width/2 - getSearchBarXSize()/2 + 5 + textBeforeCursorWidth+1,
                    topTextBox+(getSearchBarYSize()-8)/2+9, Color.WHITE.getRGB());
        }

        String selectedText = textField.getSelectedText();
        if(!selectedText.isEmpty()) {
            int selectionWidth = fr.getStringWidth(selectedText);

            int leftIndex = Math.min(textField.getCursorPosition(), textField.getSelectionEnd());
            String textBeforeSelection = textField.getText().substring(0, leftIndex);
            int textBeforeSelectionWidth = fr.getStringWidth(textBeforeSelection);

            drawRect(width/2 - getSearchBarXSize()/2 + 5 + textBeforeSelectionWidth,
                    topTextBox+(getSearchBarYSize()-8)/2-1,
                    width/2 - getSearchBarXSize()/2 + 5 + textBeforeSelectionWidth + selectionWidth,
                    topTextBox+(getSearchBarYSize()-8)/2+9, Color.LIGHT_GRAY.getRGB());

            fr.drawString(selectedText,
                    width/2 - getSearchBarXSize()/2 + 5 + textBeforeSelectionWidth,
                    topTextBox+(getSearchBarYSize()-8)/2, Color.BLACK.getRGB());
        }


        /**
         * Item selection (right) gui element rendering
         */
        int paneWidth = (int)(width/3*getWidthMult());
        int leftSide = (int)(width*getItemPaneOffsetFactor());
        int rightSide = leftSide+paneWidth-BOX_PADDING-getItemBoxXPadding();

        //Tab

        Minecraft.getMinecraft().getTextureManager().bindTexture(itemPaneTabArrow);
        GlStateManager.color(1f, 1f, 1f, 0.3f);
        Utils.drawTexturedRect(width-itemPaneTabOffset.getValue(), height/2 - 50, 20, 100);
        GlStateManager.bindTexture(0);

        if(mouseX > width-itemPaneTabOffset.getValue() && mouseY > height/2 - 50
                && mouseY < height/2 + 50) {
            if(!hoveringItemPaneToggle) {
                itemPaneOpen = !itemPaneOpen;
                hoveringItemPaneToggle = true;
            }
        } else {
            hoveringItemPaneToggle = false;
        }

        //Atomic reference used so that below lambda doesn't complain about non-effectively-final variable
        AtomicReference<JsonObject> tooltipToDisplay = new AtomicReference<>(null);

        if(itemPaneOffsetFactor.getValue() < 1) {
            renderBlurredBackground(width, height,
                    leftSide+BOX_PADDING-5, BOX_PADDING-5,
                    paneWidth-BOX_PADDING*2+10, height-BOX_PADDING*2+10);

            drawRect(leftSide+BOX_PADDING-5, BOX_PADDING-5,
                    leftSide+paneWidth-BOX_PADDING+5, height-BOX_PADDING+5, bg.getRGB());

            renderNavElement(leftSide+BOX_PADDING+getItemBoxXPadding(), rightSide, getMaxPages(), page+1,
                    scaledresolution.getScaleFactor()<4?"Page: ":"");

            //Sort bar
            drawRect(leftSide+BOX_PADDING+getItemBoxXPadding()-1,
                    height-BOX_PADDING-ITEM_SIZE-2,
                    rightSide+1,
                    height-BOX_PADDING, fgCustomOpacity.getRGB());

            float sortIconsMinX = (sortIcons.length+orderIcons.length)*(ITEM_SIZE+ITEM_PADDING)+ITEM_SIZE;
            float availableX = rightSide-(leftSide+BOX_PADDING+getItemBoxXPadding());
            float sortOrderScaleFactor = Math.min(1, availableX / sortIconsMinX);

            int scaledITEM_SIZE = (int)(ITEM_SIZE*sortOrderScaleFactor);
            int scaledItemPaddedSize = (int)((ITEM_SIZE+ITEM_PADDING)*sortOrderScaleFactor);
            int iconTop = height-BOX_PADDING-(ITEM_SIZE+scaledITEM_SIZE)/2-1;

            for(int i=0; i<orderIcons.length; i++) {
                int orderIconX = leftSide+BOX_PADDING+getItemBoxXPadding()+i*scaledItemPaddedSize;
                drawRect(orderIconX, iconTop,scaledITEM_SIZE+orderIconX,iconTop+scaledITEM_SIZE, fg.getRGB());

                Minecraft.getMinecraft().getTextureManager().bindTexture(getCompareMode() == i ? orderIconsActive[i] : orderIcons[i]);
                GlStateManager.color(1f, 1f, 1f, 1f);
                Utils.drawTexturedRect(orderIconX, iconTop, scaledITEM_SIZE, scaledITEM_SIZE,0, 1, 0, 1, GL11.GL_NEAREST);

                Minecraft.getMinecraft().getTextureManager().bindTexture(getCompareAscending().get(i) ? ascending_overlay : descending_overlay);
                GlStateManager.color(1f, 1f, 1f, 1f);
                Utils.drawTexturedRect(orderIconX, iconTop, scaledITEM_SIZE, scaledITEM_SIZE,0, 1, 0, 1, GL11.GL_NEAREST);
                GlStateManager.bindTexture(0);
            }

            for(int i=0; i<sortIcons.length; i++) {
                int sortIconX = rightSide-scaledITEM_SIZE-i*scaledItemPaddedSize;
                drawRect(sortIconX, iconTop,scaledITEM_SIZE+sortIconX,iconTop+scaledITEM_SIZE, fg.getRGB());
                Minecraft.getMinecraft().getTextureManager().bindTexture(getSortMode() == i ? sortIconsActive[i] : sortIcons[i]);
                GlStateManager.color(1f, 1f, 1f, 1f);
                Utils.drawTexturedRect(sortIconX, iconTop, scaledITEM_SIZE, scaledITEM_SIZE, 0, 1, 0, 1, GL11.GL_NEAREST);
                GlStateManager.bindTexture(0);
            }

            if(!hoverInv) {
                iterateItemSlots(new ItemSlotConsumer() {
                    public void consume(int x, int y, int id) {
                        JsonObject json = getSearchedItemPage(id);
                        if (json == null) {
                            return;
                        }
                        if (mouseX > x - 1 && mouseX < x + ITEM_SIZE + 1) {
                            if (mouseY > y - 1 && mouseY < y + ITEM_SIZE + 1) {
                                tooltipToDisplay.set(json);
                            }
                        }
                    }
                });
            }

            //Iterate through all item slots and display the appropriate item
            int itemBoxXPadding = getItemBoxXPadding();
            int xStart = (int)(width*getItemPaneOffsetFactor())+BOX_PADDING+itemBoxXPadding;

            renderItemsFromImage(xStart, width, height);
            renderEnchOverlay();

            checkFramebufferSizes(width, height);

            if(redrawItems || !manager.config.cacheRenderedItempane.value) {
                renderItemsToImage(width, height, fgFavourite2, fgFavourite, fgCustomOpacity, true, true);
                redrawItems = false;
            }
        }

        /**
         * Item info (left) gui element rendering
         */

        rightSide = (int)(width*getInfoPaneOffsetFactor());
        leftSide = rightSide - paneWidth;

        if(activeInfoPane != null) {
            activeInfoPane.tick();
            activeInfoPane.render(width, height, bg, fg, scaledresolution, mouseX, mouseY);

            GlStateManager.color(1f, 1f, 1f, 1f);
            Minecraft.getMinecraft().getTextureManager().bindTexture(close);
            Utils.drawTexturedRect(rightSide-22, 7, 16, 16);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
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

            JsonObject auctionInfo = manager.getItemAuctionInfo(json.get("internalname").getAsString());
            JsonObject bazaarInfo = manager.getBazaarInfo(json.get("internalname").getAsString());

            boolean hasAuctionPrice = auctionInfo != null;
            boolean hasBazaarPrice = bazaarInfo != null;

            NumberFormat format = NumberFormat.getInstance(Locale.US);

            NEUManager.CraftInfo craftCost = manager.getCraftCost(json.get("internalname").getAsString());

            if(hasAuctionPrice || hasBazaarPrice || craftCost.fromRecipe) text.add("");
            if(hasBazaarPrice) {
                int bazaarBuyPrice = (int)bazaarInfo.get("avg_buy").getAsFloat();
                text.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Bazaar Buy: "+
                        EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+format.format(bazaarBuyPrice)+" coins");
                int bazaarSellPrice = (int)bazaarInfo.get("avg_sell").getAsFloat();
                text.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Bazaar Sell: "+
                        EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+format.format(bazaarSellPrice)+" coins");
                if(manager.config.advancedPriceInfo.value) {
                    int bazaarInstantBuyPrice = (int)bazaarInfo.get("curr_buy").getAsFloat();
                    text.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Bazaar Insta-Buy: "+
                            EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+format.format(bazaarInstantBuyPrice)+" coins");
                    int bazaarInstantSellPrice = (int)bazaarInfo.get("curr_sell").getAsFloat();
                    text.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Bazaar Insta-Sell: "+
                            EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+format.format(bazaarInstantSellPrice)+" coins");
                }
            }
            if(hasAuctionPrice) {
                int auctionPrice = (int)(auctionInfo.get("price").getAsFloat() / auctionInfo.get("count").getAsFloat());
                text.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"AH Price: "+
                        EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+format.format(auctionPrice)+" coins");
            }
            if(craftCost.fromRecipe) {
                text.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Raw Craft Cost: "+
                        EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+format.format((int)craftCost.craftCost)+" coins");
            }

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

    public void redrawItems() {
        redrawItems = true;
    }

    public void setPage(int page) {
        this.page = page;
        redrawItems = true;
    }

    private Framebuffer[] itemFramebuffers = new Framebuffer[2];

    private void checkFramebufferSizes(int width, int height) {
        int sw = width*scaledresolution.getScaleFactor();
        int sh = height*scaledresolution.getScaleFactor();
        for(int i=0; i<itemFramebuffers.length; i++) {
            if(itemFramebuffers[i] == null || itemFramebuffers[i].framebufferWidth != sw || itemFramebuffers[i].framebufferHeight != sh) {
                if(itemFramebuffers[i] == null) {
                    itemFramebuffers[i] = new Framebuffer(sw, sh, true);
                } else {
                    itemFramebuffers[i].createBindFramebuffer(sw, sh);
                }
                itemFramebuffers[i].setFramebufferFilter(GL11.GL_NEAREST);
                redrawItems = true;
            }
        }
    }

    private void prepareFramebuffer(Framebuffer buffer, int sw, int sh) {
        buffer.framebufferClear();
        buffer.bindFramebuffer(false);
        GL11.glViewport(0, 0, sw, sh);
    }
    private void cleanupFramebuffer(Framebuffer buffer, int sw, int sh) {
        buffer.unbindFramebuffer();
        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
    }


    private void renderItemsToImage(int width, int height, Color fgFavourite2,
                                    Color fgFavourite, Color fgCustomOpacity, boolean items, boolean entities) {
        int sw = width*scaledresolution.getScaleFactor();
        int sh = height*scaledresolution.getScaleFactor();

        GL11.glPushMatrix();
        prepareFramebuffer(itemFramebuffers[0], sw, sh);
        renderItems(10, items, entities);
        cleanupFramebuffer(itemFramebuffers[0], sw, sh);
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        prepareFramebuffer(itemFramebuffers[1], sw, sh);
        renderItemBackgrounds(fgFavourite2, fgFavourite, fgCustomOpacity);
        cleanupFramebuffer(itemFramebuffers[1], sw, sh);
        GL11.glPopMatrix();
    }

    private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

    float itemRenderOffset = 7.5001f;
    private void renderItemsFromImage(int xOffset, int width, int height) {
        if(itemFramebuffers[0] != null && itemFramebuffers[1] != null) {
            itemFramebuffers[1].bindFramebufferTexture();
            GlStateManager.color(1f, 1f, 1f, 1f);
            Utils.drawTexturedRect(xOffset-10, 0, width, height, 0, 1, 1, 0);
            itemFramebuffers[1].unbindFramebufferTexture();

            GL11.glTranslatef(0, 0, itemRenderOffset);
            itemFramebuffers[0].bindFramebufferTexture();
            GlStateManager.color(1f, 1f, 1f, 1f);
            Utils.drawTexturedRect(xOffset-10, 0, width, height, 0, 1, 1, 0);
            itemFramebuffers[0].unbindFramebufferTexture();
            GL11.glTranslatef(0, 0, -itemRenderOffset);
        }
    }

    private void renderEnchOverlay() {
        ItemStack stack = new ItemStack(Items.apple);
        IBakedModel model = Minecraft.getMinecraft().getRenderItem().getItemModelMesher()
                .getItemModel(stack);
        float f = (float)(Minecraft.getSystemTime() % 3000L) / 3000.0F / 8.0F;
        float f1 = (float)(Minecraft.getSystemTime() % 4873L) / 4873.0F / 8.0F;
        Minecraft.getMinecraft().getTextureManager().bindTexture(RES_ITEM_GLINT);

        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0, -7.5001f+itemRenderOffset);
        iterateItemSlots(new ItemSlotConsumer() {
            public void consume(int x, int y, int id) {
                JsonObject json = getSearchedItemPage(id);
                if (json == null || !manager.jsonToStack(json).hasEffect()) {
                    return;
                }

                GlStateManager.pushMatrix();
                GlStateManager.enableRescaleNormal();
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(516, 0.1F);
                GlStateManager.enableBlend();

                GlStateManager.disableLighting();

                GlStateManager.translate(x, y, 0);
                GlStateManager.scale(16f, 16f, 16f);

                GlStateManager.depthMask(false);
                GlStateManager.depthFunc(GL11.GL_EQUAL);
                GlStateManager.blendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
                GlStateManager.matrixMode(5890);
                GlStateManager.pushMatrix();
                GlStateManager.scale(8.0F, 8.0F, 8.0F);
                GlStateManager.translate(f, 0.0F, 0.0F);
                GlStateManager.rotate(-50.0F, 0.0F, 0.0F, 1.0F);

                renderModel(model, -8372020, null);

                GlStateManager.popMatrix();
                GlStateManager.pushMatrix();
                GlStateManager.scale(8.0F, 8.0F, 8.0F);
                GlStateManager.translate(-f1, 0.0F, 0.0F);
                GlStateManager.rotate(10.0F, 0.0F, 0.0F, 1.0F);

                renderModel(model, -8372020, null);

                GlStateManager.popMatrix();
                GlStateManager.matrixMode(5888);
                GlStateManager.blendFunc(770, 771);
                GlStateManager.depthFunc(515);
                GlStateManager.depthMask(true);

                GlStateManager.popMatrix();

            }
        });
        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableRescaleNormal();
        GL11.glTranslatef(0, 0, 7.5001f-itemRenderOffset);
        GL11.glPopMatrix();

        GlStateManager.bindTexture(0);
    }

    private void renderModel(IBakedModel model, int color, ItemStack stack) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.ITEM);

        for (EnumFacing enumfacing : EnumFacing.values()) {
            this.renderQuads(worldrenderer, model.getFaceQuads(enumfacing), color);
        }

        this.renderQuads(worldrenderer, model.getGeneralQuads(), color);

        tessellator.draw();
    }

    private void renderQuads(WorldRenderer renderer, List<BakedQuad> quads, int color) {
        if(quads == null) return;

        for(BakedQuad quad : quads) {
            renderer.addVertexData(quad.getVertexData());
            renderer.putColor4(color);
        }
    }

    private void renderItemBackgrounds(Color fgFavourite2, Color fgFavourite, Color fgCustomOpacity) {
        if(fgCustomOpacity.getAlpha() == 0) return;
        iterateItemSlots(new ItemSlotConsumer() {
            public void consume(int x, int y, int id) {
                JsonObject json = getSearchedItemPage(id);
                if (json == null) {
                    return;
                }

                Minecraft.getMinecraft().getTextureManager().bindTexture(item_mask);
                if (getFavourites().contains(json.get("internalname").getAsString())) {
                    if(manager.config.itemStyle.value) {
                        GlStateManager.color(fgFavourite2.getRed() / 255f, fgFavourite2.getGreen() / 255f,
                                fgFavourite2.getBlue() / 255f, fgFavourite2.getAlpha() / 255f);
                        Utils.drawTexturedRect(x - 1, y - 1, ITEM_SIZE + 2, ITEM_SIZE + 2, GL11.GL_NEAREST);

                        GlStateManager.color(fgFavourite.getRed() / 255f, fgFavourite.getGreen() / 255f,
                                fgFavourite.getBlue() / 255f, fgFavourite.getAlpha() / 255f);
                        Utils.drawTexturedRect(x, y, ITEM_SIZE, ITEM_SIZE, GL11.GL_NEAREST);
                    } else {
                        drawRect(x-1, y-1, x+ITEM_SIZE+1, y+ITEM_SIZE+1, fgFavourite2.getRGB());
                        drawRect(x, y, x+ITEM_SIZE, y+ITEM_SIZE, fgFavourite.getRGB());
                    }
                } else {
                    if(manager.config.itemStyle.value) {
                        GlStateManager.color(fgCustomOpacity.getRed() / 255f, fgCustomOpacity.getGreen() / 255f,
                                fgCustomOpacity.getBlue() / 255f, fgCustomOpacity.getAlpha() / 255f);
                        Utils.drawTexturedRect(x - 1, y - 1, ITEM_SIZE + 2, ITEM_SIZE + 2, GL11.GL_NEAREST);
                    } else {
                        drawRect(x-1, y-1, x+ITEM_SIZE+1, y+ITEM_SIZE+1, fgCustomOpacity.getRGB());
                    }
                }
                GlStateManager.bindTexture(0);
            }
        }, 10);
    }

    private void renderItems(int xStart, boolean items, boolean entities) {
        iterateItemSlots(new ItemSlotConsumer() {
            public void consume(int x, int y, int id) {
                JsonObject json = getSearchedItemPage(id);
                if (json == null) {
                    return;
                }

                if (json.has("entityrender")) {
                    if(!entities) return;
                    String name = json.get("displayname").getAsString();
                    String[] split = name.split(" \\(");
                    name = name.substring(0, name.length() - split[split.length - 1].length() - 2);

                    Class<? extends EntityLivingBase>[] entities = new Class[1];
                    if (json.get("entityrender").isJsonArray()) {
                        JsonArray entityrender = json.get("entityrender").getAsJsonArray();
                        entities = new Class[entityrender.size()];
                        for (int i = 0; i < entityrender.size(); i++) {
                            Class<? extends Entity> clazz = EntityList.stringToClassMapping.get(entityrender.get(i).getAsString());
                            if (clazz != null && EntityLivingBase.class.isAssignableFrom(clazz)) {
                                entities[i] = (Class<? extends EntityLivingBase>) clazz;
                            }
                        }
                    } else if (json.get("entityrender").isJsonPrimitive()) {
                        Class<? extends Entity> clazz = EntityList.stringToClassMapping.get(json.get("entityrender").getAsString());
                        if (clazz != null && EntityLivingBase.class.isAssignableFrom(clazz)) {
                            entities[0] = (Class<? extends EntityLivingBase>) clazz;
                        }
                    }

                    float scale = 8;
                    if (json.has("entityscale")) {
                        scale *= json.get("entityscale").getAsFloat();
                    }

                    renderEntity(x + ITEM_SIZE / 2, y + ITEM_SIZE, scale, name, entities);
                } else {
                    if(!items) return;
                    ItemStack stack = manager.jsonToStack(json);
                    Utils.drawItemStackWithoutGlint(stack, x, y);
                }
            }
        }, xStart);
    }

    public float getItemPaneOffsetFactor() {
        return itemPaneOffsetFactor.getValue() * getWidthMult() + (1-getWidthMult());
    }

    public float getInfoPaneOffsetFactor() {
        return infoPaneOffsetFactor.getValue() * getWidthMult();
    }

}