package io.github.moulberry.notenoughupdates;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.infopanes.*;
import io.github.moulberry.notenoughupdates.itemeditor.NEUItemEditor;
import io.github.moulberry.notenoughupdates.util.LerpingFloat;
import io.github.moulberry.notenoughupdates.util.LerpingInteger;
import io.github.moulberry.notenoughupdates.util.Utils;
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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Matrix4f;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.awt.*;
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
            order_alphabetical, order_rarity, order_value
    };
    private ResourceLocation[] orderIconsActive = new ResourceLocation[] {
            order_alphabetical_active, order_rarity_active, order_value_active
    };

    //Various constants used for GUI structure
    private int searchBarXSize = 200;
    private final int searchBarYOffset = 10;
    private final int searchBarYSize = 40;
    private final int searchBarPadding = 2;

    private float oldWidthMult = 0;

    public static final int ITEM_PADDING = 4;
    public static final int ITEM_SIZE = 16;

    private Color bg = new Color(90, 90, 140, 50);
    private Color fg = new Color(100,100,100, 255);

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
    private long millisLastMouseMove = 0;
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    boolean mouseDown = false;

    private boolean redrawItems = false;

    private boolean searchBarHasFocus = false;
    GuiTextField textField = new GuiTextField(0, null, 0, 0, 0, 0);

    private static final int COMPARE_MODE_ALPHABETICAL = 0;
    private static final int COMPARE_MODE_RARITY = 1;
    private static final int COMPARE_MODE_VALUE = 2;

    private static final int SORT_MODE_ALL = 0;
    private static final int SORT_MODE_MOB = 1;
    private static final int SORT_MODE_PET = 2;
    private static final int SORT_MODE_TOOL = 3;
    private static final int SORT_MODE_ARMOR = 4;
    private static final int SORT_MODE_ACCESSORY = 5;

    private ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());

    private boolean disabled = false;

    public NEUOverlay(NEUManager manager) {
        this.manager = manager;
        textField.setFocused(true);
        textField.setCanLoseFocus(false);
    }

    /**
     * Disables searchBarFocus and resets the item pane position. Called whenever NEUOverlay is opened.
     */
    public void reset() {
        searchBarHasFocus = false;
        if(!(searchMode || (manager.config.keepopen.value && itemPaneOpen))) {
            itemPaneOpen = false;
            itemPaneOffsetFactor.setValue(1);
            itemPaneTabOffset.setValue(20);
        }
    }

    /**
     * Calls #displayInformationPane with a HTMLInfoPane created from item.info and item.infoType.
     */
    public void showInfo(JsonObject item) {
        if(item.has("info") && item.has("infoType")) {
            JsonArray lore = item.get("info").getAsJsonArray();
            String[] loreA = new String[lore.size()];
            for (int i = 0; i < lore.size(); i++) loreA[i] = lore.get(i).getAsString();
            String loreS = StringUtils.join(loreA, "\n");

            String internalname = item.get("internalname").getAsString();
            String name = item.get("displayname").getAsString();
            switch(item.get("infoType").getAsString()) {
                case "WIKI_URL":
                    displayInformationPane(HTMLInfoPane.createFromWikiUrl(this, manager, name, loreS));
                    return;
                case "WIKI":
                    displayInformationPane(HTMLInfoPane.createFromWiki(this, manager, name, internalname, loreS));
                    return;
                case "HTML":
                    displayInformationPane(new HTMLInfoPane(this, manager, name, internalname, loreS));
                    return;
            }
            displayInformationPane(new TextInfoPane(this, manager, name, loreS));
        }
    }

    /**
     * Handles the mouse input, cancelling the forge event if a NEU gui element is clicked.
     */
    public boolean mouseInput() {
        if(disabled) {
            return false;
        }

        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        int mouseX = Mouse.getX() / scaledresolution.getScaleFactor();
        int mouseY = height - Mouse.getY() / scaledresolution.getScaleFactor();

        //if(lastMouseX != mouseX || lastMouseY != mouseY) {
        //    millisLastMouseMove = System.currentTimeMillis();
        //}

        lastMouseX = mouseX;
        lastMouseY = mouseY;

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
                int rightSide = leftSide+paneWidth-getBoxPadding()-getItemBoxXPadding();
                leftSide = leftSide+getBoxPadding()+getItemBoxXPadding();

                FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
                int maxPages = getMaxPages();
                String name = scaledresolution.getScaleFactor()<4?"Page: ":"";
                float maxStrLen = fr.getStringWidth(EnumChatFormatting.BOLD+name + maxPages + "/" + maxPages);
                float maxButtonXSize = (rightSide-leftSide+2 - maxStrLen*0.5f - 10)/2f;
                int buttonXSize = (int)Math.min(maxButtonXSize, getSearchBarYSize()*480/160f);
                int ySize = (int)(buttonXSize/480f*160);
                int yOffset = (int)((getSearchBarYSize()-ySize)/2f);
                int top = getBoxPadding()+yOffset;

                if(mouseY >= top && mouseY <= top+ySize) {
                    int leftPrev = leftSide-1;
                    if(mouseX > leftPrev && mouseX < leftPrev+buttonXSize) { //"Previous" button
                        setPage(page-1);
                        Utils.playPressSound();
                    }
                    int leftNext = rightSide+1-buttonXSize;
                    if(mouseX > leftNext && mouseX < leftNext+buttonXSize) { //"Next" button
                        setPage(page+1);
                        Utils.playPressSound();
                    }
                }

                float sortIconsMinX = (sortIcons.length+orderIcons.length)*(ITEM_SIZE+ITEM_PADDING)+ITEM_SIZE;
                float availableX = rightSide-leftSide;
                float sortOrderScaleFactor = Math.min(1, availableX / sortIconsMinX);

                int scaledITEM_SIZE = (int)(ITEM_SIZE*sortOrderScaleFactor);
                int scaledItemPaddedSize = (int)((ITEM_SIZE+ITEM_PADDING)*sortOrderScaleFactor);
                int iconTop = height-getBoxPadding()-(ITEM_SIZE+scaledITEM_SIZE)/2-1;

                if(mouseY >= iconTop && mouseY <= iconTop+scaledITEM_SIZE) {
                    for(int i=0; i<orderIcons.length; i++) {
                        int orderIconX = leftSide+i*scaledItemPaddedSize;
                        if(mouseX >= orderIconX && mouseX <= orderIconX+scaledITEM_SIZE) {
                            if(Mouse.getEventButton() == 0) {
                                manager.config.compareMode.value = new Double(i);
                                updateSearch();
                                Utils.playPressSound();
                            } else if(Mouse.getEventButton() == 1) {
                                manager.config.compareAscending.value.set(i, !manager.config.compareAscending.value.get(i));
                                updateSearch();
                                Utils.playPressSound();
                            }
                        }
                    }

                    for(int i=0; i<sortIcons.length; i++) {
                        int sortIconX = rightSide-scaledITEM_SIZE-i*scaledItemPaddedSize;
                        if(mouseX >= sortIconX && mouseX <= sortIconX+scaledITEM_SIZE) {
                            manager.config.sortMode.value = new Double(i);
                            updateSearch();
                            Utils.playPressSound();
                        }
                    }
                }
            }
            return true;
        }

        if(Minecraft.getMinecraft().currentScreen instanceof GuiContainer) {
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
        }

        //Clicking on "close info pane" button
        if(mouseX > width*getInfoPaneOffsetFactor()-getBoxPadding()-8 && mouseX < width*getInfoPaneOffsetFactor()-getBoxPadding()+8) {
            if(mouseY > getBoxPadding()-8 && mouseY < getBoxPadding()+8) {
                if(Mouse.getEventButtonState() && Mouse.getEventButton() < 2) { //Left or right click up
                    displayInformationPane(null);
                    return true;
                }
            }
        }


        //Quickcommands
        int paddingUnscaled = searchBarPadding/scaledresolution.getScaleFactor();
        if(paddingUnscaled < 1) paddingUnscaled = 1;
        int topTextBox = height - searchBarYOffset - getSearchBarYSize();

        if(Mouse.getEventButtonState() && manager.config.showQuickCommands.value) {
            ArrayList<String> quickCommands = manager.config.quickCommands.value;
            int bigItemSize = getSearchBarYSize();
            int bigItemPadding = paddingUnscaled*4;
            int xStart = width/2 + bigItemPadding/2 - (bigItemSize+bigItemPadding)*quickCommands.size()/2;
            int xEnd = width/2 - bigItemPadding/2 + (bigItemSize+bigItemPadding)*quickCommands.size()/2;
            int y = topTextBox - bigItemSize - bigItemPadding - paddingUnscaled*2;

            if(mouseY >= y && mouseY <= topTextBox-paddingUnscaled*2) {
                if(mouseX > xStart && mouseX < xEnd) {
                    if((mouseX - xStart)%(bigItemSize+bigItemPadding) < bigItemSize) {
                        int index = (mouseX - xStart)/(bigItemSize+bigItemPadding);
                        if(index >= 0 && index < quickCommands.size()) {
                            String quickCommand = quickCommands.get(index);
                            if(quickCommand.contains(":")) {
                                String command = quickCommand.split(":")[0].trim();
                                if(command.startsWith("/")) {
                                    NotEnoughUpdates.INSTANCE.sendChatMessage(command);
                                    Utils.playPressSound();
                                    return true;
                                } else {
                                    ClientCommandHandler.instance.executeCommand(Minecraft.getMinecraft().thePlayer, command);
                                    Utils.playPressSound();
                                    return true;
                                }
                            }
                        }
                    }
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

        int iconSize = getSearchBarYSize()+paddingUnscaled*2;

        if(mouseY > topTextBox - paddingUnscaled && mouseY < topTextBox - paddingUnscaled + iconSize) {
            if(mouseX > width/2 + getSearchBarXSize()/2 + paddingUnscaled*6 &&
                    mouseX < width/2 + getSearchBarXSize()/2 + paddingUnscaled*6 + iconSize) {
                if(Mouse.getEventButtonState()) {
                    displayInformationPane(HTMLInfoPane.createFromWikiUrl(this, manager, "Help",
                            "https://moulberry.github.io/files/neu_help.html"));
                    Utils.playPressSound();
                }
            } else if(mouseX > width/2 - getSearchBarXSize()/2 - paddingUnscaled*6 - iconSize &&
                    mouseX < width/2 - getSearchBarXSize()/2 - paddingUnscaled*6) {
                if(Mouse.getEventButtonState()) {
                    if(activeInfoPane instanceof SettingsInfoPane) {
                        displayInformationPane(null);
                    } else {
                        displayInformationPane(new SettingsInfoPane(this, manager));
                    }
                    Utils.playPressSound();
                }
            }
        }

        if(activeInfoPane != null) {
            if(mouseX < width*getInfoPaneOffsetFactor()) {
                activeInfoPane.mouseInput(width, height, mouseX, mouseY, mouseDown);
                return true;
            } else if(Mouse.getEventButton() <= 1 && Mouse.getEventButtonState()) { //Left or right click
                activeInfoPane.mouseInputOutside();
            }
        }

        return false;
    }

    /**
     * Returns searchBarXSize, scaled by 0.8 if gui scale == AUTO.
     */
    public int getSearchBarXSize() {
        if(scaledresolution.getScaleFactor()==4) return (int)(searchBarXSize*0.8);
        return (int)(searchBarXSize);
    }

    /**
     * Sets the activeInfoPane and sets the target of the infoPaneOffsetFactor to make the infoPane "slide" out.
     */
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

    /**
     * Finds the index of the character inside the search bar that was clicked, used to set the caret.
     */
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

        int keyPressed = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter()+256 : Keyboard.getEventKey();

        if(disabled) {
            if(Keyboard.getEventKeyState() && keyPressed == manager.keybindToggleDisplay.getKeyCode()) {
                disabled = !disabled;
            }
            return false;
        }

        if(Keyboard.isKeyDown(Keyboard.KEY_Y) && manager.config.dev.value) {
            displayInformationPane(new DevInfoPane(this, manager));
            //displayInformationPane(new QOLInfoPane(this, manager));
        }

        if(Keyboard.getEventKeyState()) {
            if(searchBarHasFocus) {
                if(keyPressed == 1) {
                    searchBarHasFocus = false;
                } else {
                    if(textField.textboxKeyTyped(Keyboard.getEventCharacter(), keyPressed)) {
                        updateSearch();
                    }
                }
            } else {
                if(activeInfoPane != null) {
                    if(activeInfoPane.keyboardInput()) {
                        return true;
                    }
                }

                if(keyPressed == manager.keybindClosePanes.getKeyCode()) {
                    itemPaneOffsetFactor.setValue(1);
                    itemPaneTabOffset.setValue(20);
                    itemPaneOpen = false;
                    displayInformationPane(null);
                }

                if(keyPressed == manager.keybindToggleDisplay.getKeyCode()) {
                    disabled = !disabled;
                    return true;
                }

                AtomicReference<String> internalname = new AtomicReference<>(null);
                AtomicReference<ItemStack> itemstack = new AtomicReference<>(null);
                if(Minecraft.getMinecraft().currentScreen instanceof GuiContainer &&
                        Utils.getSlotUnderMouse((GuiContainer)Minecraft.getMinecraft().currentScreen) != null) {
                    Slot slot = Utils.getSlotUnderMouse((GuiContainer)Minecraft.getMinecraft().currentScreen);
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
                        if(keyPressed == manager.keybindViewUsages.getKeyCode()) {
                            manager.displayGuiItemUsages(internalname.get(), "");
                            return true;
                        } else if(keyPressed == manager.keybindFavourite.getKeyCode()) {
                            toggleFavourite(item.get("internalname").getAsString());
                            return true;
                        } else if(keyPressed == manager.keybindViewRecipe.getKeyCode()) {
                            manager.showRecipe(item);
                            return true;
                        } else if(keyPressed == manager.keybindGive.getKeyCode()) {
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

    public void toggleFavourite(String internalname) {
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

    /**
     * Finds the rarity from the lore of an item.
     * -1 = UNKNOWN
     * 0 = COMMON
     * 1 = UNCOMMON
     * 2 = RARE
     * 3 = EPIC
     * 4 = LEGENDARY
     * 5 = SPECIAL
     */
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

    /**
     * Convenience functions that get various compare/sort modes from the config.
     */
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

    /**
     * Creates an item comparator used to sort the list of items according to the favourite set then compare mode.
     * Defaults to alphabetical sorting if the above factors cannot distinguish between two items.
     */
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
            } else if(getCompareMode() == COMPARE_MODE_VALUE) {
                float cost1 = manager.getCraftCost(o1.get("internalname").getAsString()).craftCost;
                float cost2 = manager.getCraftCost(o2.get("internalname").getAsString()).craftCost;

                if(cost1 < cost2) return mult;
                if(cost1 > cost2) return -mult;
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

    /**
     * Checks whether an item matches a certain type, i.e. whether the item lore ends in "{rarity} {item type}"
     * eg. "SHOVEL" will return >0 for "COMMON SHOVEL", "EPIC SHOVEL", etc.
     * @return the index of the type that matched, or -1 otherwise.
     */
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

    /**
     * Checks whether an item matches the current sort mode.
     */
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

    /**
     * Clears the current item list, creating a new TreeSet if necessary.
     * Adds all items that match the search AND match the sort mode to the current item list.
     * Also adds some easter egg items. (Also I'm very upset if you came here to find them :'( )
     */
    public void updateSearch() {
        if(searchedItems==null) searchedItems = new TreeSet<>(getItemComparator());
        searchedItems.clear();
        searchedItemsArr = null;
        redrawItems = true;
        Set<String> itemsMatch = manager.search(textField.getText(), true);
        for(String itemname : itemsMatch) {
            JsonObject item = manager.getItemInformation().get(itemname);
            if(checkMatchesSort(itemname, item)) {
                searchedItems.add(item);
            }
        }
        switch(textField.getText().toLowerCase().trim()) {
            case "nullzee":
                searchedItems.add(CustomItems.NULLZEE);
                break;
            case "rune":
                searchedItems.add(CustomItems.RUNE);
                break;
            case "2b2t":
                searchedItems.add(CustomItems.TWOBEETWOTEE);
                break;
            case "ducttape":
            case "ducttapedigger":
                searchedItems.add(CustomItems.DUCTTAPE);
                break;
        }
    }

    /**
     * Returns an index-able array containing the elements in searchedItems.
     * Whenever searchedItems is updated via the above method, the array is recreated here.
     */
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

    /**
     * Gets the item in searchedItemArr corresponding to the certain index on the current page.
     * @return item, if the item exists. null, otherwise.
     */
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
        return (((int)(width/3*getWidthMult())-2*getBoxPadding())%(ITEM_SIZE+ITEM_PADDING)+ITEM_PADDING)/2;
    }

    public int getBoxPadding() {
        double panePadding = Math.max(0, Math.min(20, manager.config.panePadding.value));
        return (int)(panePadding*2/scaledresolution.getScaleFactor()+5);
    }

    private abstract class ItemSlotConsumer {
        public abstract void consume(int x, int y, int id);
    }

    public void iterateItemSlots(ItemSlotConsumer itemSlotConsumer) {
        int width = scaledresolution.getScaledWidth();
        int itemBoxXPadding = getItemBoxXPadding();
        iterateItemSlots(itemSlotConsumer, (int)(width*getItemPaneOffsetFactor())+getBoxPadding()+itemBoxXPadding);
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
        int itemBoxYPadding = ((height-getSearchBarYSize()-2*getBoxPadding()-ITEM_SIZE-2)%(ITEM_SIZE+ITEM_PADDING)+ITEM_PADDING)/2;

        int yStart = getBoxPadding()+getSearchBarYSize()+itemBoxYPadding;
        int itemBoxXPadding = getItemBoxXPadding();
        int xEnd = xStart+paneWidth-getBoxPadding()*2-ITEM_SIZE-itemBoxXPadding;
        int yEnd = height-getBoxPadding()-ITEM_SIZE-2-itemBoxYPadding;

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
        if(scaledresolution.getScaleFactor()==4) scaleFMult *= 0.9f;
        if(manager.auctionManager.customAH.isRenderOverAuctionView()) scaleFMult *= 0.8f;
        return (float)Math.max(0.5, Math.min(1.5, manager.config.paneWidthMult.value.floatValue()))*scaleFMult;
    }

    /**
     * Calculates the number of horizontal item slots.
     */
    public int getSlotsXSize() {
        int width = scaledresolution.getScaledWidth();

        int paneWidth = (int)(width/3*getWidthMult());
        int itemBoxXPadding = (((int)(width-width*getItemPaneOffsetFactor())-2*getBoxPadding())%(ITEM_SIZE+ITEM_PADDING)+ITEM_PADDING)/2;
        int xStart = (int)(width*getItemPaneOffsetFactor())+getBoxPadding()+itemBoxXPadding;
        int xEnd = (int)(width*getItemPaneOffsetFactor())+paneWidth-getBoxPadding()-ITEM_SIZE;

        return (int)Math.ceil((xEnd - xStart)/((float)(ITEM_SIZE+ITEM_PADDING)));
    }

    /**
     * Calculates the number of vertical item slots.
     */
    public int getSlotsYSize() {
        int height = scaledresolution.getScaledHeight();

        int itemBoxYPadding = ((height-getSearchBarYSize()-2*getBoxPadding()-ITEM_SIZE-2)%(ITEM_SIZE+ITEM_PADDING)+ITEM_PADDING)/2;
        int yStart = getBoxPadding()+getSearchBarYSize()+itemBoxYPadding;
        int yEnd = height-getBoxPadding()-ITEM_SIZE-2-itemBoxYPadding;

        return (int)Math.ceil((yEnd - yStart)/((float)(ITEM_SIZE+ITEM_PADDING)));
    }

    public int getMaxPages() {
        if(getSearchedItems().length == 0) return 1;
        return (int)Math.ceil(getSearchedItems().length/(float)getSlotsYSize()/getSlotsXSize());
    }

    public int getSearchBarYSize() {
        return Math.max(searchBarYSize/scaledresolution.getScaleFactor(), ITEM_SIZE);
    }

    /**
     * Renders the top navigation bar, can be used by InfoPane implementations (such as SettingsInfoPane).
     * Renders "prev" button, index/maxIndex string, "next" button.
     */
    public void renderNavElement(int leftSide, int rightSide, int maxPages, int page, String name) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

        String pageText = EnumChatFormatting.BOLD+name + page + "/" + maxPages;

        float maxStrLen = fr.getStringWidth(EnumChatFormatting.BOLD+name + maxPages + "/" + maxPages);
        float maxButtonXSize = (rightSide-leftSide+2 - maxStrLen*0.5f - 10)/2f;
        int buttonXSize = (int)Math.min(maxButtonXSize, getSearchBarYSize()*480/160f);
        int ySize = (int)(buttonXSize/480f*160);
        int yOffset = (int)((getSearchBarYSize()-ySize)/2f);
        int top = getBoxPadding()+yOffset;

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

    public boolean isUsingMobsFilter() {
        return getSortMode() == SORT_MODE_MOB;
    }

    public float yaw = 0;
    public float pitch = 20;

    /**
     * Renders an entity onto the GUI at a certain x and y position.
     */
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

    /**
     * Renders black squares over the inventory to indicate items that do not match a specific search. (When searchMode
     * is enabled)
     */
    public void renderOverlay(int mouseX, int mouseY) {
        if(searchMode && textField.getText().length() > 0) {
            if(Minecraft.getMinecraft().currentScreen instanceof GuiContainer) {
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
    }

    Shader blurShaderHorz = null;
    Framebuffer blurOutputHorz = null;
    Shader blurShaderVert = null;
    Framebuffer blurOutputVert = null;

    /**
     * Creates a projection matrix that projects from our coordinate space [0->width; 0->height] to OpenGL coordinate
     * space [-1 -> 1; 1 -> -1] (Note: flipped y-axis).
     *
     * This is so that we can render to and from the framebuffer in a way that is familiar to us, instead of needing to
     * apply scales and translations manually.
     */
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

    /**
     * Renders whatever is currently in the Minecraft framebuffer to our two framebuffers, applying a horizontal
     * and vertical blur separately in order to significantly save computation time.
     * This is only possible if framebuffers are supported by the system, so this method will exit prematurely
     * if framebuffers are not available. (Apple machines, for example, have poor framebuffer support).
     */
    private double lastBgBlurFactor = 5;
    private void blurBackground() {
        int width = Minecraft.getMinecraft().displayWidth;
        int height = Minecraft.getMinecraft().displayHeight;

        if(manager.config.bgBlurFactor.value <= 0 || !OpenGlHelper.isFramebufferEnabled()) return;

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

    /**
     * Renders a subsection of the blurred framebuffer on to the corresponding section of the screen.
     * Essentially, this method will "blur" the background inside the bounds specified by [x->x+blurWidth, y->y+blurHeight]
     */
    public void renderBlurredBackground(int width, int height, int x, int y, int blurWidth, int blurHeight) {
        if(manager.config.bgBlurFactor.value <= 0 || !OpenGlHelper.isFramebufferEnabled()) return;

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
     * Renders the search bar, quick commands, item selection (right) and item info (left) gui elements.
     */
    public void render(int mouseX, int mouseY, boolean hoverInv) {
        if(disabled) {
            return;
        }
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        if(guiScaleLast != scaledresolution.getScaleFactor()) {
            guiScaleLast = scaledresolution.getScaleFactor();
            redrawItems = true;
        }

        if(oldWidthMult != getWidthMult()) {
            oldWidthMult = getWidthMult();
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
         * Item selection (right) gui element rendering
         */
        int paneWidth = (int)(width/3*getWidthMult());
        int leftSide = (int)(width*getItemPaneOffsetFactor());
        int rightSide = leftSide+paneWidth-getBoxPadding()-getItemBoxXPadding();

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
        List<String> textToDisplay = null;
        if(itemPaneOffsetFactor.getValue() < 1) {
            renderBlurredBackground(width, height,
                    leftSide+getBoxPadding()-5, getBoxPadding()-5,
                    paneWidth-getBoxPadding()*2+10, height-getBoxPadding()*2+10);

            drawRect(leftSide+getBoxPadding()-5, getBoxPadding()-5,
                    leftSide+paneWidth-getBoxPadding()+5, height-getBoxPadding()+5, bg.getRGB());

            renderNavElement(leftSide+getBoxPadding()+getItemBoxXPadding(), rightSide, getMaxPages(), page+1,
                    scaledresolution.getScaleFactor()<4?"Page: ":"");

            //Sort bar
            drawRect(leftSide+getBoxPadding()+getItemBoxXPadding()-1,
                    height-getBoxPadding()-ITEM_SIZE-2,
                    rightSide+1,
                    height-getBoxPadding(), fgCustomOpacity.getRGB());

            float sortIconsMinX = (sortIcons.length+orderIcons.length)*(ITEM_SIZE+ITEM_PADDING)+ITEM_SIZE;
            float availableX = rightSide-(leftSide+getBoxPadding()+getItemBoxXPadding());
            float sortOrderScaleFactor = Math.min(1, availableX / sortIconsMinX);

            int scaledITEM_SIZE = (int)(ITEM_SIZE*sortOrderScaleFactor);
            int scaledItemPaddedSize = (int)((ITEM_SIZE+ITEM_PADDING)*sortOrderScaleFactor);
            int iconTop = height-getBoxPadding()-(ITEM_SIZE+scaledITEM_SIZE)/2-1;

            boolean hoveredOverControl = false;
            for(int i=0; i<orderIcons.length; i++) {
                int orderIconX = leftSide+getBoxPadding()+getItemBoxXPadding()+i*scaledItemPaddedSize;
                drawRect(orderIconX, iconTop,scaledITEM_SIZE+orderIconX,iconTop+scaledITEM_SIZE, fg.getRGB());

                Minecraft.getMinecraft().getTextureManager().bindTexture(getCompareMode() == i ? orderIconsActive[i] : orderIcons[i]);
                GlStateManager.color(1f, 1f, 1f, 1f);
                Utils.drawTexturedRect(orderIconX, iconTop, scaledITEM_SIZE, scaledITEM_SIZE,0, 1, 0, 1, GL11.GL_NEAREST);

                Minecraft.getMinecraft().getTextureManager().bindTexture(getCompareAscending().get(i) ? ascending_overlay : descending_overlay);
                GlStateManager.color(1f, 1f, 1f, 1f);
                Utils.drawTexturedRect(orderIconX, iconTop, scaledITEM_SIZE, scaledITEM_SIZE,0, 1, 0, 1, GL11.GL_NEAREST);
                GlStateManager.bindTexture(0);

                if(mouseY > iconTop && mouseY < iconTop+scaledITEM_SIZE) {
                    if(mouseX > orderIconX && mouseX < orderIconX+scaledITEM_SIZE) {
                        hoveredOverControl = true;
                        if(System.currentTimeMillis() - millisLastMouseMove > 400) {
                            String text = EnumChatFormatting.GRAY+"Order ";
                            if(i == COMPARE_MODE_ALPHABETICAL) text += "Alphabetically";
                            else if(i == COMPARE_MODE_RARITY) text += "by Rarity";
                            else if(i == COMPARE_MODE_VALUE) text += "by Item Worth";
                            else text = null;
                            if(text != null) textToDisplay = Utils.createList(text);
                        }
                    }
                }
            }

            for(int i=0; i<sortIcons.length; i++) {
                int sortIconX = rightSide-scaledITEM_SIZE-i*scaledItemPaddedSize;
                drawRect(sortIconX, iconTop,scaledITEM_SIZE+sortIconX,iconTop+scaledITEM_SIZE, fg.getRGB());
                Minecraft.getMinecraft().getTextureManager().bindTexture(getSortMode() == i ? sortIconsActive[i] : sortIcons[i]);
                GlStateManager.color(1f, 1f, 1f, 1f);
                Utils.drawTexturedRect(sortIconX, iconTop, scaledITEM_SIZE, scaledITEM_SIZE, 0, 1, 0, 1, GL11.GL_NEAREST);
                GlStateManager.bindTexture(0);

                if(mouseY > iconTop && mouseY < iconTop+scaledITEM_SIZE) {
                    if(mouseX > sortIconX && mouseX < sortIconX+scaledITEM_SIZE) {
                        hoveredOverControl = true;
                        if(System.currentTimeMillis() - millisLastMouseMove > 400) {
                            String text = EnumChatFormatting.GRAY+"Filter ";
                            if(i == SORT_MODE_ALL) text = EnumChatFormatting.GRAY+"No Filter";
                            else if(i == SORT_MODE_MOB) text += "Mobs";
                            else if(i == SORT_MODE_PET) text += "Pets";
                            else if(i == SORT_MODE_TOOL) text += "Tools";
                            else if(i == SORT_MODE_ARMOR) text += "Armor";
                            else if(i == SORT_MODE_ACCESSORY) text += "Accessories";
                            else text = null;
                            if(text != null) textToDisplay = Utils.createList(text);
                        }
                    }
                }
            }

            if(!hoveredOverControl) {
                millisLastMouseMove = System.currentTimeMillis();
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
            int xStart = (int)(width*getItemPaneOffsetFactor())+getBoxPadding()+itemBoxXPadding;

            if(OpenGlHelper.isFramebufferEnabled()) {
                renderItemsFromImage(xStart, width, height);
                renderEnchOverlay();

                checkFramebufferSizes(width, height);

                if(redrawItems || !manager.config.cacheRenderedItempane.value) {
                    renderItemsToImage(width, height, fgFavourite2, fgFavourite, fgCustomOpacity, true, true);
                    redrawItems = false;
                }
            } else {
                renderItems(xStart, true, true, true);
            }

            GlStateManager.enableBlend();
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.1F);
            GlStateManager.disableLighting();
        }

        /**
         * Search bar
         */
        int paddingUnscaled = searchBarPadding/scaledresolution.getScaleFactor();
        if(paddingUnscaled < 1) paddingUnscaled = 1;

        int topTextBox = height - searchBarYOffset - getSearchBarYSize();

        //Search bar background
        drawRect(width/2 - getSearchBarXSize()/2 - paddingUnscaled,
                topTextBox - paddingUnscaled,
                width/2 + getSearchBarXSize()/2 + paddingUnscaled,
                height - searchBarYOffset + paddingUnscaled, searchMode ? Color.YELLOW.getRGB() : Color.WHITE.getRGB());
        drawRect(width/2 - getSearchBarXSize()/2,
                topTextBox,
                width/2 + getSearchBarXSize()/2,
                height - searchBarYOffset, Color.BLACK.getRGB());

        //Quickcommands
        if(manager.config.showQuickCommands.value) {
            ArrayList<String> quickCommands = manager.config.quickCommands.value;
            int bigItemSize = getSearchBarYSize();
            int bigItemPadding = paddingUnscaled*4;
            int x = width/2 + bigItemPadding/2 - (bigItemSize+bigItemPadding)*quickCommands.size()/2;
            int y = topTextBox - bigItemSize - bigItemPadding - paddingUnscaled*2;

            for(String quickCommand : quickCommands) {
                if(quickCommand.split(":").length!=3) {
                    continue;
                }
                String display = quickCommand.split(":")[2];
                ItemStack render = null;
                float extraScale = 1;
                if(display.length() > 20) { //Custom head
                    render = new ItemStack(Items.skull, 1, 3);
                    NBTTagCompound nbt = new NBTTagCompound();
                    NBTTagCompound skullOwner = new NBTTagCompound();
                    NBTTagCompound properties = new NBTTagCompound();
                    NBTTagList textures = new NBTTagList();
                    NBTTagCompound textures_0 = new NBTTagCompound();


                    String uuid = UUID.nameUUIDFromBytes(display.getBytes()).toString();
                    skullOwner.setString("Id", uuid);
                    skullOwner.setString("Name", uuid);

                    textures_0.setString("Value", display);
                    textures.appendTag(textures_0);

                    properties.setTag("textures", textures);
                    skullOwner.setTag("Properties", properties);
                    nbt.setTag("SkullOwner", skullOwner);
                    render.setTagCompound(nbt);

                    extraScale = 1.3f;
                } else if(manager.getItemInformation().containsKey(display)) {
                    render = manager.jsonToStack(manager.getItemInformation().get(display));
                } else {
                    Item item = Item.itemRegistry.getObject(new ResourceLocation(display.toLowerCase()));
                    if(item != null) {
                        render = new ItemStack(item);
                    }
                }
                if(render != null) {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(item_mask);
                    GlStateManager.color(1, 1, 1, 1);
                    Utils.drawTexturedRect(x - paddingUnscaled, y - paddingUnscaled,
                            bigItemSize + paddingUnscaled*2, bigItemSize + paddingUnscaled*2, GL11.GL_NEAREST);
                    GlStateManager.color(fg.getRed() / 255f,fg.getGreen() / 255f,
                            fg.getBlue() / 255f, fg.getAlpha() / 255f);
                    Utils.drawTexturedRect(x, y, bigItemSize, bigItemSize, GL11.GL_NEAREST);

                    if(mouseX > x && mouseX < x+bigItemSize) {
                        if(mouseY > y && mouseY < y+bigItemSize) {
                            textToDisplay = new ArrayList<>();
                            textToDisplay.add(EnumChatFormatting.GRAY+quickCommand.split(":")[1]);
                        }
                    }

                    float itemScale = bigItemSize/(float)ITEM_SIZE*extraScale;
                    GlStateManager.pushMatrix();
                    GlStateManager.scale(itemScale, itemScale, 1);
                    GlStateManager.translate((x-(extraScale-1)*bigItemSize/2) /itemScale,
                            (y-(extraScale-1)*bigItemSize/2)/itemScale, 0f);
                    Utils.drawItemStack(render, 0, 0);
                    GlStateManager.popMatrix();
                }
                x += bigItemSize + bigItemPadding;
            }
        }

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
         * Item info (left) gui element rendering
         */

        rightSide = (int)(width*getInfoPaneOffsetFactor());
        leftSide = rightSide - paneWidth;

        if(activeInfoPane != null) {
            activeInfoPane.tick();
            activeInfoPane.render(width, height, bg, fg, scaledresolution, mouseX, mouseY);

            GlStateManager.color(1f, 1f, 1f, 1f);
            Minecraft.getMinecraft().getTextureManager().bindTexture(close);
            Utils.drawTexturedRect(rightSide-getBoxPadding()-8, getBoxPadding()-8, 16, 16);
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

            textToDisplay = text;
        }
        if(textToDisplay != null) {
            Utils.drawHoveringText(textToDisplay, mouseX, mouseY, width, height, -1, fr);
        }

        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.disableLighting();
    }

    /**
     * Used in SettingsInfoPane to redraw the items when a setting changes.
     */
    public void redrawItems() {
        redrawItems = true;
    }

    /**
     * Sets the current page and marks that the itemsPane should be redrawn
     * @param page
     */
    public void setPage(int page) {
        this.page = page;
        redrawItems = true;
    }

    private Framebuffer[] itemFramebuffers = new Framebuffer[2];

    /**
     * Checks whether the screen size has changed, if so it reconstructs the itemPane framebuffer and marks that the
     * itemPane should be redrawn.
     */
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

    /**
     * Renders all items to a framebuffer so that it can be reused later, drastically improving performance.
     * Unfortunately using this feature will mean that animated textures will not work, but oh well.
     * Mojang please optimize item rendering thanks.
     */
    private void renderItemsToImage(int width, int height, Color fgFavourite2,
                                    Color fgFavourite, Color fgCustomOpacity, boolean items, boolean entities) {
        int sw = width*scaledresolution.getScaleFactor();
        int sh = height*scaledresolution.getScaleFactor();

        GL11.glPushMatrix();
        prepareFramebuffer(itemFramebuffers[0], sw, sh);
        renderItems(10, items, entities, false);
        cleanupFramebuffer(itemFramebuffers[0], sw, sh);
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        prepareFramebuffer(itemFramebuffers[1], sw, sh);
        renderItemBackgrounds(fgFavourite2, fgFavourite, fgCustomOpacity);
        cleanupFramebuffer(itemFramebuffers[1], sw, sh);
        GL11.glPopMatrix();
    }

    private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

    /**
     * Renders the framebuffer created by #renderItemsToImage to the screen.
     * itemRenderOffset is a magic number that makes the z-level of the rendered items equal to the z-level of
     * the item glint overlay model, meaning that a depthFunc of GL_EQUAL can correctly render on to the item.
     */
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

    /**
     * Renders the enchant overlay, since only the items have the specific z-offset of 7.5001, this will only apply
     * the enchant overlay to the actual items and not anything else.
     *
     * (I tried very hard to replicate the enchant rendering overlay code from vanilla, but I couldn't get it to
     * work without rendering with the "ITEM" vertex model like in vanilla, so I choose to render an arbitrary 2D
     * item. If a texture pack sets a custom 3D model for an apple, this will probably break.)
     */
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

    /**
     * Renders all the item backgrounds, either squares or squircles.
     */
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

    private void renderItems(int xStart, boolean items, boolean entities, boolean glint) {
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
                    if(glint) {
                        Utils.drawItemStack(stack, x, y);
                    } else {
                        Utils.drawItemStackWithoutGlint(stack, x, y);
                    }
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