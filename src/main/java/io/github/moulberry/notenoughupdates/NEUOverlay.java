package io.github.moulberry.notenoughupdates;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.realmsclient.gui.ChatFormatting;
import io.github.moulberry.notenoughupdates.itemeditor.NEUItemEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NEUOverlay extends Gui {

    private NEUManager manager;

    private ResourceLocation itemPaneTabArrow = new ResourceLocation("notenoughupdates:item_pane_tab_arrow.png");
    private ResourceLocation prev = new ResourceLocation("notenoughupdates:prev_pow2.png");
    private ResourceLocation next = new ResourceLocation("notenoughupdates:next_pow2.png");
    private ResourceLocation item_edit = new ResourceLocation("notenoughupdates:item_edit.png");

    private final int searchBarXSize = 200;
    private final int searchBarYOffset = 10;
    private final int searchBarYSize = 40;
    private final int searchBarPadding = 2;

    private final int boxPadding = 15;
    private final int itemPadding = 4;
    private final int itemSize = 16;

    private String informationPaneTitle;
    private String[] informationPane;

    private boolean allowItemEditing;

    private LinkedHashMap<String, JsonObject> searchedItems = null;

    private boolean itemPaneOpen = false;

    private int page = 0;

    private LerpingFloat itemPaneOffsetFactor = new LerpingFloat(1);
    private LerpingInteger itemPaneTabOffset = new LerpingInteger(20, 50);
    private LerpingFloat infoPaneOffsetFactor = new LerpingFloat(0);

    private boolean searchMode = false;
    private long millisLastLeftClick = 0;

    private boolean searchBarHasFocus = false;
    GuiTextField textField = new GuiTextField(0, null, 0, 0, 0, 0);

    public NEUOverlay(NEUManager manager) {
        this.manager = manager;
        textField.setFocused(true);
        textField.setCanLoseFocus(false);

        allowItemEditing = manager.getAllowEditing();
    }

    public void reset() {
        searchBarHasFocus = false;
        itemPaneOpen = searchMode;
        itemPaneOffsetFactor.setValue(1);
        itemPaneTabOffset.setValue(20);
    }

    /**
     * Handles the mouse input, cancelling the forge event if a NEU gui element is clicked.
     */
    public boolean mouseInput() {
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        int mouseX = Mouse.getX() / scaledresolution.getScaleFactor();
        int mouseY = height - Mouse.getY() / scaledresolution.getScaleFactor();

        //Unfocuses the search bar by default. Search bar is focused if the click is on the bar itself.
        if(Mouse.getEventButtonState()) setSearchBarFocus(false);

        //Item selection (right) gui
        if(mouseX > width*itemPaneOffsetFactor.getValue()) {
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
                            if(item.has("clickcommand") && Mouse.getEventButton() == 0) {
                                String clickcommand = item.get("clickcommand").getAsString();

                                if(clickcommand.equals("viewrecipe")) {
                                    Minecraft.getMinecraft().thePlayer.sendChatMessage(
                                            "/" + clickcommand + " " + item.get("internalname").getAsString().toUpperCase());
                                } else if(clickcommand.equals("viewpotion")) {

                                    Minecraft.getMinecraft().thePlayer.sendChatMessage(
                                            "/" + clickcommand + " " + item.get("internalname").getAsString().toLowerCase());
                                }
                            } else if(item.has("info") && Mouse.getEventButton() == 1) {
                                JsonArray lore = item.get("info").getAsJsonArray();
                                String[] loreA = new String[lore.size()];
                                for(int i=0; i<lore.size(); i++) loreA[i] = lore.get(i).getAsString();
                                displayInformationPane(item.get("displayname").getAsString(), loreA);
                            } else if(Mouse.getEventButton() == 3) {
                                textField.setText("itemid:"+item.get("internalname").getAsString());
                            }
                        }
                    }
                }
            });
            if(!clickedItem.get()) {
                int leftSide = (int)(width*itemPaneOffsetFactor.getValue());

                if(mouseY > boxPadding && mouseY < boxPadding+searchBarYSize/scaledresolution.getScaleFactor()) {
                    int leftPrev = leftSide+boxPadding+getItemBoxXPadding();
                    if(mouseX > leftPrev && mouseX < leftPrev+120/scaledresolution.getScaleFactor()) { //"Previous" button
                        page--;
                    }
                    int rightNext = leftSide+width/3-boxPadding-getItemBoxXPadding();
                    if(mouseX > rightNext-120/scaledresolution.getScaleFactor() && mouseX < rightNext) {
                        page++;
                    }
                }
            }
            return true;
        }

        //Search bar
        if(mouseX >= width/2 - searchBarXSize/2 && mouseX <= width/2 + searchBarXSize/2) {
            if(mouseY >= height - searchBarYOffset - searchBarYSize/scaledresolution.getScaleFactor() &&
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
        int topTextBox = height - searchBarYOffset - searchBarYSize/scaledresolution.getScaleFactor();
        int iconSize = searchBarYSize/scaledresolution.getScaleFactor()+paddingUnscaled*2;
        if(paddingUnscaled < 1) paddingUnscaled = 1;
        if(mouseX > width/2 + searchBarXSize/2 + paddingUnscaled*6 &&
                mouseX < width/2 + searchBarXSize/2 + paddingUnscaled*6 + iconSize) {
            if(mouseY > topTextBox - paddingUnscaled && mouseY < topTextBox - paddingUnscaled + iconSize) {
                if(Mouse.getEventButtonState()) {
                    allowItemEditing = !allowItemEditing;
                    manager.setAllowEditing(allowItemEditing);
                }
            }
        }

        return false;
    }

    public void displayInformationPane(String title, String[] info) {
        informationPaneTitle = title;

        if(info == null || info.length == 0) {
            informationPane = new String[]{"\u00A77No additional information."};
        } else {
            informationPane = info;
        }
        infoPaneOffsetFactor.setTarget(1/3f);
        infoPaneOffsetFactor.resetTimer();
    }

    public int getClickedIndex(int mouseX, int mouseY) {
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
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
        if(searchBarHasFocus && Keyboard.getEventKey() == 1 && Keyboard.getEventKeyState()) {
            searchBarHasFocus = false;
            if(!textField.getText().isEmpty()) {
                return true;
            }
        }

        if(searchBarHasFocus && Keyboard.getEventKeyState()) {
            if(textField.textboxKeyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey())) {
                if(textField.getText().isEmpty()) {
                    searchedItems = null;
                } else {
                    updateSearch();
                }
            }
        }

        if(allowItemEditing) {
            if(Keyboard.getEventCharacter() == 'k' && Keyboard.getEventKeyState()) {
                Slot slot = ((GuiContainer)Minecraft.getMinecraft().currentScreen).getSlotUnderMouse();
                if(slot != null) {
                    ItemStack hover = slot.getStack();
                    if(hover != null) {
                        Minecraft.getMinecraft().displayGuiScreen(new NEUItemEditor(manager,
                                manager.getInternalNameForItem(hover), manager.getJsonForItem(hover)));
                        //manager.writeItemToFile(hover);
                    }
                } else {
                    ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
                    int height = scaledresolution.getScaledHeight();

                    int mouseX = Mouse.getX() / scaledresolution.getScaleFactor();
                    int mouseY = height - Mouse.getY() / scaledresolution.getScaleFactor();

                    iterateItemSlots((x, y) -> {
                        if (mouseX >= x - 1 && mouseX <= x + itemSize + 1) {
                            if (mouseY >= y - 1 && mouseY <= y + itemSize + 1) {
                                int id = getSlotId(x, y);
                                JsonObject item = getSearchedItemPage(id);

                                if(item != null) {
                                    Minecraft.getMinecraft().displayGuiScreen(new NEUItemEditor(manager,
                                            item.get("internalname").getAsString(), item));
                                }
                            }
                        }
                    });
                }
            }
        }

        return searchBarHasFocus; //Cancels keyboard events if the search bar has focus
    }

    public void updateSearch() {
        if(searchedItems==null) searchedItems = new LinkedHashMap<>();
        searchedItems.clear();
        Set<String> itemsMatch = manager.search(textField.getText());
        for(String item : itemsMatch) {
            searchedItems.put(item, manager.getItemInformation().get(item));
        }
    }

    public Collection<JsonObject> getSearchedItems() {
        if(searchedItems==null) {
            return manager.getItemInformation().values();
        } else {
            return searchedItems.values();
        }
    }

    public JsonObject getSearchedItemPage(int index) {
        if(index < getSlotsXSize()*getSlotsYSize()) {
            int actualIndex = index + getSlotsXSize()*getSlotsYSize()*page;
            if(actualIndex < getSearchedItems().size()) {
                return (JsonObject) (getSearchedItems().toArray()[actualIndex]);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public int getItemBoxXPadding() {
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledresolution.getScaledWidth();
        return (((int)(width-width*itemPaneOffsetFactor.getValue())-2*boxPadding)%(itemSize+itemPadding)+itemPadding)/2;
    }

    /**
     * Iterates through all the item slots in the right panel and calls a biconsumer for each slot with
     * arguments equal to the slot's x and y position respectively. This is used in order to prevent
     * code duplication issues.
     */
    public void iterateItemSlots(BiConsumer<Integer, Integer> itemSlotConsumer) {
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        int itemBoxXPadding = getItemBoxXPadding();
        int itemBoxYPadding = ((height-2*boxPadding)%(itemSize+itemPadding)+itemPadding)/2;

        int xStart = (int)(width*itemPaneOffsetFactor.getValue())+boxPadding+itemBoxXPadding;
        int yStart = boxPadding+searchBarYSize/scaledresolution.getScaleFactor()+itemBoxYPadding;
        int xEnd = (int)(width*itemPaneOffsetFactor.getValue())+width/3-boxPadding-itemSize;
        int yEnd = height-boxPadding-itemSize;

        //Render the items, displaying the tooltip if the cursor is over the item
        for(int y = yStart; y < yEnd; y+=itemSize+itemPadding) {
            for(int x = xStart; x < xEnd; x+=itemSize+itemPadding) {
                itemSlotConsumer.accept(x, y);
            }
        }
    }

    /**
     * Calculates the number of horizontal item slots.
     */
    public int getSlotsXSize() {
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledresolution.getScaledWidth();

        int itemBoxXPadding = (((int)(width-width*itemPaneOffsetFactor.getValue())-2*boxPadding)%(itemSize+itemPadding)+itemPadding)/2;
        int xStart = (int)(width*itemPaneOffsetFactor.getValue())+boxPadding+itemBoxXPadding;
        int xEnd = (int)(width*itemPaneOffsetFactor.getValue())+width/3-boxPadding-itemSize;

        return (int)Math.ceil((xEnd - xStart)/((float)(itemSize+itemPadding)));
    }

    /**
     * Calculates the number of vertical item slots.
     */
    public int getSlotsYSize() {
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int height = scaledresolution.getScaledHeight();

        int itemBoxYPadding = ((height-2*boxPadding)%(itemSize+itemPadding)+itemPadding)/2;
        int yStart = boxPadding+searchBarYSize/scaledresolution.getScaleFactor()+itemBoxYPadding;
        int yEnd = height-boxPadding-itemSize;

        return (int)Math.ceil((yEnd - yStart)/((float)(itemSize+itemPadding)));
    }

    public int getMaxPages() {
        if(getSearchedItems().size() == 0) return 1;
        return (int)Math.ceil(getSearchedItems().size()/(float)getSlotsYSize()/getSlotsXSize());
    }

    /**
     * Takes in the x and y coordinates of a slot and returns the id of that slot.
     */
    public int getSlotId(int x, int y) {
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        int itemBoxXPadding = (((int)(width-width*itemPaneOffsetFactor.getValue())-2*boxPadding)%(itemSize+itemPadding)+itemPadding)/2;
        int itemBoxYPadding = ((height-2*boxPadding)%(itemSize+itemPadding)+itemPadding)/2;

        int xStart = (int)(width*itemPaneOffsetFactor.getValue())+boxPadding+itemBoxXPadding;
        int yStart = boxPadding+searchBarYSize/scaledresolution.getScaleFactor()+itemBoxYPadding;

        int xIndex = (x-xStart)/(itemSize+itemPadding);
        int yIndex = (y-yStart)/(itemSize+itemPadding);
        return xIndex + yIndex*getSlotsXSize();
    }

    /**
     * Renders the search bar, item selection (right) and item info (left) gui elements.
     */
    public void render(float partialTicks, int mouseX, int mouseY) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        if(searchMode && textField.getText().length() > 0) {
            try {
                GuiContainer inv = (GuiContainer) Minecraft.getMinecraft().currentScreen;
                Field guiLeft = GuiContainer.class.getDeclaredField("field_147003_i");
                Field guiTop = GuiContainer.class.getDeclaredField("field_147009_r");
                guiLeft.setAccessible(true);
                guiTop.setAccessible(true);
                int guiLeftI = (int) guiLeft.get(inv);
                int guiTopI = (int) guiTop.get(inv);

                GL11.glPushMatrix();
                GL11.glTranslatef(0, 0, 300);
                int overlay = new Color(50, 50, 50, 150).getRGB();
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
                                list.set(i, stack.getRarity().rarityColor + (String)list.get(i));
                            } else {
                                list.set(i, EnumChatFormatting.GRAY + (String)list.get(i));
                            }
                        }

                        GuiUtils.drawHoveringText(list, mouseX, mouseY, width, height, -1, Minecraft.getMinecraft().fontRendererObj);
                    }
                }
                GL11.glPopMatrix();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
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

        int topTextBox = height - searchBarYOffset - searchBarYSize/scaledresolution.getScaleFactor();

        //Search bar background
        drawRect(width/2 - searchBarXSize/2 - paddingUnscaled,
                topTextBox - paddingUnscaled,
                width/2 + searchBarXSize/2 + paddingUnscaled,
                height - searchBarYOffset + paddingUnscaled, searchMode ? Color.YELLOW.getRGB() : Color.WHITE.getRGB());
        drawRect(width/2 - searchBarXSize/2,
                topTextBox,
                width/2 + searchBarXSize/2,
                height - searchBarYOffset, Color.BLACK.getRGB());

        //Item editor enable button
        int iconSize = searchBarYSize/scaledresolution.getScaleFactor()+paddingUnscaled*2;
        Minecraft.getMinecraft().getTextureManager().bindTexture(item_edit);
        drawRect(width/2 + searchBarXSize/2 + paddingUnscaled*6,
                topTextBox - paddingUnscaled,
                width/2 + searchBarXSize/2 + paddingUnscaled*6 + iconSize,
                topTextBox - paddingUnscaled + iconSize, Color.WHITE.getRGB());

        drawRect(width/2 + searchBarXSize/2 + paddingUnscaled*7,
                topTextBox,
                width/2 + searchBarXSize/2 + paddingUnscaled*5 + iconSize,
                topTextBox - paddingUnscaled*2 + iconSize, allowItemEditing ? Color.GREEN.getRGB() : Color.RED.getRGB());
        GlStateManager.color(1f, 1f, 1f, 1f);
        drawTexturedRect(width/2 + searchBarXSize/2 + paddingUnscaled*6, topTextBox - paddingUnscaled, iconSize, iconSize);
        GlStateManager.bindTexture(0);

        if(mouseX > width/2 + searchBarXSize/2 + paddingUnscaled*6 &&
                mouseX < width/2 + searchBarXSize/2 + paddingUnscaled*6 + iconSize) {
            if(mouseY > topTextBox - paddingUnscaled && mouseY < topTextBox - paddingUnscaled + iconSize) {
                GuiUtils.drawHoveringText(Arrays.asList(
                        EnumChatFormatting.GOLD.toString()+EnumChatFormatting.BOLD+"Enable item editing",
                        EnumChatFormatting.RED+"Warning: "+EnumChatFormatting.YELLOW+"Uploading fake, " +
                                "misinformative or misleading information using the item editor may result in your " +
                                "account being banned from using this mod.",
                        EnumChatFormatting.GREEN.toString()+EnumChatFormatting.BOLD+"Press k on an any item to use the item editor."),
                        mouseX, mouseY, width, height, 250, Minecraft.getMinecraft().fontRendererObj);
                GlStateManager.disableLighting();
            }
        }

        //Search bar text
        fr.drawString(textField.getText(), width/2 - searchBarXSize/2 + 5,
                topTextBox+(searchBarYSize/scaledresolution.getScaleFactor()-8)/2, Color.WHITE.getRGB());

        //Determines position of cursor. Cursor blinks on and off every 500ms.
        if(searchBarHasFocus && System.currentTimeMillis()%1000>500) {
            String textBeforeCursor = textField.getText().substring(0, textField.getCursorPosition());
            int textBeforeCursorWidth = fr.getStringWidth(textBeforeCursor);
            drawRect(width/2 - searchBarXSize/2 + 5 + textBeforeCursorWidth,
                    topTextBox+(searchBarYSize/scaledresolution.getScaleFactor()-8)/2-1,
                    width/2 - searchBarXSize/2 + 5 + textBeforeCursorWidth+1,
                    topTextBox+(searchBarYSize/scaledresolution.getScaleFactor()-8)/2+9, Color.WHITE.getRGB());
        }

        String selectedText = textField.getSelectedText();
        if(!selectedText.isEmpty()) {
            int selectionWidth = fr.getStringWidth(selectedText);

            int leftIndex = textField.getCursorPosition() < textField.getSelectionEnd() ?
                    textField.getCursorPosition() : textField.getSelectionEnd();
            String textBeforeSelection = textField.getText().substring(0, leftIndex);
            int textBeforeSelectionWidth = fr.getStringWidth(textBeforeSelection);

            drawRect(width/2 - searchBarXSize/2 + 5 + textBeforeSelectionWidth,
                    topTextBox+(searchBarYSize/scaledresolution.getScaleFactor()-8)/2-1,
                    width/2 - searchBarXSize/2 + 5 + textBeforeSelectionWidth + selectionWidth,
                    topTextBox+(searchBarYSize/scaledresolution.getScaleFactor()-8)/2+9, Color.LIGHT_GRAY.getRGB());

            fr.drawString(selectedText,
                    width/2 - searchBarXSize/2 + 5 + textBeforeSelectionWidth,
                    topTextBox+(searchBarYSize/scaledresolution.getScaleFactor()-8)/2, Color.BLACK.getRGB());
        }


        /**
         * Item selection (right) gui element rendering
         */
        Color bg = new Color(128, 128, 128, 50);
        int leftSide = (int)(width*itemPaneOffsetFactor.getValue());
        int rightSide = leftSide+width/3-boxPadding-getItemBoxXPadding();
        drawRect(leftSide+boxPadding-5, boxPadding-5,
                leftSide+width/3-boxPadding+5, height-boxPadding+5, bg.getRGB());

        drawRect(leftSide+boxPadding+getItemBoxXPadding()-1, boxPadding,
                rightSide+1,
                boxPadding+searchBarYSize/scaledresolution.getScaleFactor(), Color.GRAY.getRGB());

        Minecraft.getMinecraft().getTextureManager().bindTexture(prev);
        GlStateManager.color(1f, 1f, 1f, 1f);
        drawTexturedRect(leftSide+boxPadding+getItemBoxXPadding(), boxPadding,
                120/scaledresolution.getScaleFactor(), searchBarYSize/scaledresolution.getScaleFactor());
        GlStateManager.bindTexture(0);


        Minecraft.getMinecraft().getTextureManager().bindTexture(next);
        GlStateManager.color(1f, 1f, 1f, 1f);
        drawTexturedRect(rightSide-120/scaledresolution.getScaleFactor(), boxPadding,
                120/scaledresolution.getScaleFactor(), searchBarYSize/scaledresolution.getScaleFactor());
        GlStateManager.bindTexture(0);

        int offset = 1;
        if(scaledresolution.getScaleFactor()==4) offset = 0;

        String pageText = EnumChatFormatting.BOLD+"Page: " + (page+1) + "/" + getMaxPages();
        fr.drawString(pageText, leftSide+width/6-fr.getStringWidth(pageText)/2-1,
                boxPadding+(searchBarYSize/scaledresolution.getScaleFactor())/2-4+offset, Color.BLACK.getRGB());

        //Tab
        Minecraft.getMinecraft().getTextureManager().bindTexture(itemPaneTabArrow);
        GlStateManager.color(1f, 1f, 1f, 0.3f);
        drawTexturedRect(width-itemPaneTabOffset.getValue(), height/2 - 50, 20, 100);
        GlStateManager.bindTexture(0);

        if(mouseX > width-itemPaneTabOffset.getValue() && mouseY > height/2 - 50
                    && mouseY < height/2 + 50) {
            itemPaneOpen = true;
        }

        //Atomic integer used so that below lambda doesn't complain about non-effectively-final variable
        AtomicReference<JsonObject> tooltipToDisplay = new AtomicReference<>(null);

        //Iterate through all item slots and display the appropriate item
        iterateItemSlots((x, y) -> {
            int id = getSlotId(x, y);
            JsonObject json = getSearchedItemPage(id);
            if(json == null) {
                return;
            }

            drawRect(x-1, y-1, x+itemSize+1, y+itemSize+1, Color.GRAY.getRGB());
            ItemStack stack = new ItemStack(Item.itemRegistry.getObject(
                    new ResourceLocation(json.get("itemid").getAsString())));

            if(stack.getItem() == null) {
                stack = new ItemStack(Items.diamond, 1, 10); //Purple broken texture item
            } else {
                if(json.has("damage")) {
                    stack.setItemDamage(json.get("damage").getAsInt());
                }

                if(json.has("nbttag")) {
                    try {
                        NBTTagCompound tag = JsonToNBT.getTagFromJson(json.get("nbttag").getAsString());
                        stack.setTagCompound(tag);
                    } catch(NBTException e) {
                    }
                }
            }

            //Integer.toString(getSlotId(x, y))
            drawItemStack(stack, x, y, null);

            if(mouseX > x-1 && mouseX < x+itemSize+1) {
                if(mouseY > y-1 && mouseY < y+itemSize+1) {
                    tooltipToDisplay.set(json);
                }
            }
        });

        //Render tooltip
        JsonObject json = tooltipToDisplay.get();
        if(json != null) {
            List<String> text = new ArrayList<>();
            text.add(json.get("displayname").getAsString());
            JsonArray lore = json.get("lore").getAsJsonArray();
            for(int i=0; i<lore.size(); i++) {
                text.add(lore.get(i).getAsString());
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
            if(hasClick) text.add("\u00A7e\u00A7lLeft click to view recipe!");
            if(hasInfo) text.add("\u00A7e\u00A7lRight click to view additional information!");

            GuiUtils.drawHoveringText(text, mouseX, mouseY, width, height, -1, fr);
            GlStateManager.disableLighting();
        }

        /**
         * Item info (left) gui element rendering
         */

        rightSide = (int)(width*getInfoPaneOffsetFactor());
        leftSide = rightSide - width/3;

        drawRect(leftSide+boxPadding-5, boxPadding-5, rightSide-boxPadding+5, height-boxPadding+5, bg.getRGB());
        if(informationPane != null && informationPaneTitle != null) {
            int titleLen = fr.getStringWidth(informationPaneTitle);
            fr.drawString(informationPaneTitle, (leftSide+rightSide-titleLen)/2, boxPadding + 5, Color.WHITE.getRGB());

            int yOff = 20;
            for(int i=0; i<informationPane.length; i++) {
                String line = informationPane[i];

                String excess;
                String trimmed = trimToWidth(line, width*1/3-boxPadding*2-10);

                String colourCodes = "";
                Pattern pattern = Pattern.compile("\\u00A7.");
                Matcher matcher = pattern.matcher(trimmed);
                while(matcher.find()) {
                    colourCodes += matcher.group();
                }

                boolean firstLine = true;
                int trimmedCharacters = trimmed.length();
                while(true) {
                    if(trimmed.length() == line.length()) {
                        fr.drawString(trimmed, leftSide+boxPadding + 5, boxPadding + 10 + yOff, Color.WHITE.getRGB());
                        break;
                    } else if(trimmed.isEmpty()) {
                        yOff -= 12;
                        break;
                    } else {
                        if(firstLine) {
                            fr.drawString(trimmed, leftSide+boxPadding + 5, boxPadding + 10 + yOff, Color.WHITE.getRGB());
                            firstLine = false;
                        } else {
                            if(trimmed.startsWith(" ")) {
                                trimmed = trimmed.substring(1);
                            }
                            fr.drawString(colourCodes + trimmed, leftSide+boxPadding + 5, boxPadding + 10 + yOff, Color.WHITE.getRGB());
                        }

                        excess = line.substring(trimmedCharacters);
                        trimmed = trimToWidth(excess, width * 1 / 3 - boxPadding * 2 - 10);
                        trimmedCharacters += trimmed.length();
                        yOff += 12;
                    }
                }

                yOff += 16;
            }
        }
    }

    public float getInfoPaneOffsetFactor() {
        if(itemPaneOffsetFactor.getValue() == 2/3f) {
            return infoPaneOffsetFactor.getValue();
        } else {
            return Math.min(infoPaneOffsetFactor.getValue(), 1-itemPaneOffsetFactor.getValue());
        }
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

    private void drawItemStack(ItemStack stack, int x, int y, String altText) {
        RenderItem itemRender = Minecraft.getMinecraft().getRenderItem();
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;

        RenderHelper.enableGUIStandardItemLighting();
        itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();

        itemRender.renderItemOverlayIntoGUI(font, stack, x, y, altText);
    }

    private void drawTexturedRect(int x, int y, int width, int height) {
        drawTexturedRect(x, y, width, height, 0, 1, 0 , 1);
    }

    private void drawTexturedRect(int x, int y, int width, int height, float uMin, float uMax, float vMin, float vMax) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.alphaFunc(516, 0.003921569F);

        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x + 0, y + height, this.zLevel).tex(uMin, vMax).endVertex();
        worldrenderer.pos(x + width, y + height, this.zLevel).tex(uMax, vMax).endVertex();
        worldrenderer.pos(x + width, y + 0, this.zLevel).tex(uMax, vMin).endVertex();
        worldrenderer.pos(x + 0, y + 0, this.zLevel).tex(uMin, vMin).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
    }
}