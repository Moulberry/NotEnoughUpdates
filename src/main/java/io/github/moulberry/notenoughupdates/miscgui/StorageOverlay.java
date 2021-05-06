package io.github.moulberry.notenoughupdates.miscgui;

import com.google.common.collect.Lists;
import com.google.gson.stream.JsonWriter;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.BackgroundBlur;
import io.github.moulberry.notenoughupdates.core.GlScissorStack;
import io.github.moulberry.notenoughupdates.core.GuiElement;
import io.github.moulberry.notenoughupdates.core.GuiElementTextField;
import io.github.moulberry.notenoughupdates.core.util.lerp.LerpingInteger;
import io.github.moulberry.notenoughupdates.core.util.render.RenderUtils;
import io.github.moulberry.notenoughupdates.miscfeatures.StorageManager;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLSync;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class StorageOverlay extends GuiElement {

    private static final int CHEST_TOP_OFFSET = 17;
    private static final int CHEST_SLOT_SIZE = 18;
    private static final int CHEST_BOTTOM_OFFSET = 215;

    public static final ResourceLocation STORAGE_PREVIEW_TEXTURES[] = new ResourceLocation[4];
    private static final ResourceLocation STORAGE_TEXTURES[] = new ResourceLocation[4];
    private static final ResourceLocation STORAGE_ICONS_TEXTURE = new ResourceLocation("notenoughupdates:storage_gui/storage_icons.png");
    private static final ResourceLocation[] LOAD_CIRCLE_SEQ = new ResourceLocation[11];
    static {
        for(int i=0; i<STORAGE_TEXTURES.length; i++) {
            STORAGE_TEXTURES[i] = new ResourceLocation("notenoughupdates:storage_gui/storage_gui_"+i+".png");
        }
        for(int i=0; i<STORAGE_PREVIEW_TEXTURES.length; i++) {
            STORAGE_PREVIEW_TEXTURES[i] = new ResourceLocation("notenoughupdates:storage_gui/storage_preview_"+i+".png");
        }

        LOAD_CIRCLE_SEQ[0] = new ResourceLocation("notenoughupdates:loading_circle_seq/1.png");
        LOAD_CIRCLE_SEQ[1] = new ResourceLocation("notenoughupdates:loading_circle_seq/1.png");
        LOAD_CIRCLE_SEQ[2] = new ResourceLocation("notenoughupdates:loading_circle_seq/2.png");
        for(int i=2; i<=7; i++) {
            LOAD_CIRCLE_SEQ[i+1] = new ResourceLocation("notenoughupdates:loading_circle_seq/"+i+".png");
        }
        LOAD_CIRCLE_SEQ[9] = new ResourceLocation("notenoughupdates:loading_circle_seq/7.png");
        LOAD_CIRCLE_SEQ[10] = new ResourceLocation("notenoughupdates:loading_circle_seq/1.png");
    }

    private static final StorageOverlay INSTANCE = new StorageOverlay();
    public static StorageOverlay getInstance() {
        return INSTANCE;
    }

    private GuiElementTextField searchBar = new GuiElementTextField("", 88, 10,
                    GuiElementTextField.SCALE_TEXT | GuiElementTextField.DISABLE_BG);

    private int guiLeft;
    private int guiTop;

    private int loadCircleIndex = 0;
    private int loadCircleRotation = 0;

    private long millisAccumIndex = 0;
    private long millisAccumRotation = 0;

    private long lastMillis = 0;

    private int scrollVelocity = 0;
    private long lastScroll = 0;

    private int desiredHeightSwitch = -1;
    private int desiredHeightMX = -1;
    private int desiredHeightMY = -1;

    private int scrollGrabOffset = -1;

    private LerpingInteger scroll = new LerpingInteger(0, 200);

    private int getMaximumScroll() {
        synchronized(StorageManager.getInstance().storageConfig.displayToStorageIdMap) {
            int lastDisplayId = StorageManager.getInstance().storageConfig.displayToStorageIdMap.size()-1;
            int coords = (int)Math.ceil(lastDisplayId/3f)*3+3;

            return getPageCoords(coords).y+scroll.getValue()-getStorageViewSize()-14;
        }
    }

    private void scrollToY(int y) {
        int target = y;
        if(target < 0) target = 0;

        int maxY = getMaximumScroll();
        if(target > maxY) target = maxY;

        float factor = (scroll.getValue()-target)/(float)(scroll.getValue()-y+1E-5);

        scroll.setTarget(target);
        scroll.setTimeToReachTarget(Math.min(200, Math.max(20, (int)(200*factor))));
        scroll.resetTimer();
    }

    public void scrollToStorage(int displayId, boolean forceScroll) {
        if(displayId < 0) return;

        int y = getPageCoords(displayId).y-17;
        if(y < 3) {
            scrollToY(y + scroll.getValue());
        } else {
            int storageViewSize = getStorageViewSize();
            int y2 = getPageCoords(displayId+3).y-17-storageViewSize;
            if(y2 > 3) {
                if(forceScroll) {
                    scrollToY(y + scroll.getValue());
                } else {
                    scrollToY(y2+scroll.getValue());
                }
            }
        }
    }

    private int getStorageViewSize() {
        return NotEnoughUpdates.INSTANCE.config.storageGUI.storageHeight;
    }

    private int getScrollBarHeight() {
        return getStorageViewSize() - 21;
    }

    @Override
    public void render() {
        if(!(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) return;

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();
        int mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
        int mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;
        FontRenderer fontRendererObj = Minecraft.getMinecraft().fontRendererObj;

        scroll.tick();

        int displayStyle = NotEnoughUpdates.INSTANCE.config.storageGUI.displayStyle;
        ResourceLocation storageTexture = STORAGE_TEXTURES[displayStyle];
        ResourceLocation storagePreviewTexture = STORAGE_PREVIEW_TEXTURES[displayStyle];
        int textColour = 0x404040;
        int searchTextColour = 0xe0e0e0;
        if(displayStyle == 2) {
            textColour = 0x000000;
            searchTextColour = 0xa0a0a0;
        } else if(displayStyle == 3) {
            textColour = 0xFBCC6C;
        } else if(displayStyle == 0) {
            textColour = 0x909090;
            searchTextColour = 0xa0a0a0;
        }

        long currentTime = System.currentTimeMillis();
        if(lastMillis > 0) {
            long deltaTime = currentTime - lastMillis;
            millisAccumIndex += deltaTime;
            loadCircleIndex += millisAccumIndex / (1000/15);
            millisAccumIndex %= (1000/15);

            millisAccumRotation += deltaTime;
            loadCircleRotation += millisAccumRotation / (1000/107);
            millisAccumRotation %= (1000/107);
        }

        lastMillis = currentTime;
        loadCircleIndex %= LOAD_CIRCLE_SEQ.length;
        loadCircleRotation %= 360;

        Color loadCircleColour = Color.getHSBColor(loadCircleRotation/360f, 0.3f, 0.9f);
        ItemStack stackOnMouse = Minecraft.getMinecraft().thePlayer.inventory.getItemStack();
        if(stackOnMouse != null) {
            String stackDisplay = Utils.cleanColour(stackOnMouse.getDisplayName());
            if(stackDisplay.startsWith("Backpack Slot ") || stackDisplay.startsWith("Empty Backpack Slot ") ||
                    stackDisplay.startsWith("Ender Chest Page ")) {
                stackOnMouse = null;
            }
        }

        List<String> tooltipToDisplay = null;
        int slotPreview = -1;

        int storageViewSize = getStorageViewSize();

        int sizeX = 540;
        int sizeY = 100+storageViewSize;
        int searchNobX = 18;

        int itemHoverX = -1;
        int itemHoverY = -1;

        guiLeft = width/2 - (sizeX-searchNobX)/2;
        guiTop = height/2 - sizeY/2;

        if(displayStyle == 0) {
            BackgroundBlur.renderBlurredBackground(7, width, height, guiLeft, guiTop, sizeX, storageViewSize);
            BackgroundBlur.renderBlurredBackground(7, width, height, guiLeft+5, guiTop+storageViewSize, sizeX-searchNobX-10, sizeY-storageViewSize-4);
        }

        Utils.drawGradientRect(0, 0, width, height, 0xc0101010, 0xd0101010);

        GL11.glPushMatrix();
        GlStateManager.translate(guiLeft, guiTop, 0);

        boolean hoveringOtherBackpack = false;

        //Gui
        Minecraft.getMinecraft().getTextureManager().bindTexture(storageTexture);
        GlStateManager.color(1, 1, 1, 1);
        Utils.drawTexturedRect(0, 0, sizeX, 10, 0, sizeX/600f, 0, 10/400f, GL11.GL_NEAREST);
        Utils.drawTexturedRect(0, 10, sizeX, storageViewSize-20, 0, sizeX/600f, 10/400f, 94/400f, GL11.GL_NEAREST);
        Utils.drawTexturedRect(0, storageViewSize-10, sizeX, 110, 0, sizeX/600f, 94/400f, 204/400f, GL11.GL_NEAREST);

        int maxScroll = getMaximumScroll();
        if(scroll.getValue() > maxScroll) {
            scroll.setValue(maxScroll);
        }
        if(scroll.getValue() < 0) {
            scroll.setValue(0);
        }

        //Scroll bar
        int scrollBarY = Math.round(getScrollBarHeight()*scroll.getValue()/(float)maxScroll);
        float uMin = scrollGrabOffset >= 0 ? 12/600f : 0;
        Utils.drawTexturedRect(520, 8+scrollBarY, 12, 15, uMin, uMin+12/600f, 250/400f, 265/400f, GL11.GL_NEAREST);

        int currentPage = StorageManager.getInstance().getCurrentPageId();

        boolean mouseInsideStorages = mouseY > guiTop+3 && mouseY < guiTop+3+storageViewSize;

        //Storages
        GlScissorStack.push(0, guiTop+3, width, guiTop+3+storageViewSize, scaledResolution);
        for(Map.Entry<Integer, Integer> entry : StorageManager.getInstance().storageConfig.displayToStorageIdMap.entrySet()) {
            int displayId = entry.getKey();
            int storageId = entry.getValue();

            IntPair coords = getPageCoords(displayId);
            int storageX = coords.x;
            int storageY = coords.y;

            if(coords.y-11 > 3+storageViewSize || coords.y+90 < 3) continue;

            if(storageId < 9) {
                fontRendererObj.drawString("Ender Chest Page "+(storageId+1), storageX, storageY-11, textColour);
            } else {
                fontRendererObj.drawString("Backpack Slot "+(storageId-8), storageX, storageY-11, textColour);
            }

            StorageManager.StoragePage page = StorageManager.getInstance().getPage(storageId, false);
            if(page == null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(storageTexture);
                GlStateManager.color(1, 1, 1, 1);
                int h = 18*3;

                Utils.drawTexturedRect(storageX, storageY, 162, h, 0, 162 / 600f, 265 / 400f, (265+h) / 400f, GL11.GL_NEAREST);

                Gui.drawRect(storageX, storageY, storageX + 162, storageY + h, 0x80000000);

                if(storageId < 9) {
                    Utils.drawStringCenteredScaledMaxWidth("Locked Page", fontRendererObj,
                            storageX+81, storageY+h/2, true, 150, 0xd94c00);
                } else {
                    Utils.drawStringCenteredScaledMaxWidth("Empty Backpack Slot", fontRendererObj,
                            storageX+81, storageY+h/2, true, 150, 0xd94c00);
                }
            } else if(page.rows <= 0) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(storageTexture);
                GlStateManager.color(1, 1, 1, 1);
                int h = 18*3;

                Utils.drawTexturedRect(storageX, storageY, 162, h, 0, 162 / 600f, 265 / 400f, (265 + h) / 400f, GL11.GL_NEAREST);

                Gui.drawRect(storageX, storageY, storageX + 162, storageY + h, 0x80000000);

                Utils.drawStringCenteredScaledMaxWidth("Click to load items", fontRendererObj,
                        storageX+81, storageY+h/2, true, 150, 0xffdf00);
            } else {
                int rows = page.rows;

                int storageW = 162;
                int storageH = 18*rows;

                Minecraft.getMinecraft().getTextureManager().bindTexture(storageTexture);
                GlStateManager.color(1, 1, 1, 1);
                Utils.drawTexturedRect(storageX, storageY, storageW, storageH, 0, 162/600f, 265/400f, (265+storageH)/400f, GL11.GL_NEAREST);

                boolean whiteOverlay = false;

                GlStateManager.enableDepth();
                for(int k=0; k<rows*9; k++) {
                    ItemStack stack = page.items[k];
                    int itemX = storageX+1+18*(k%9);
                    int itemY = storageY+1+18*(k/9);
                    Utils.drawItemStack(stack, itemX, itemY);

                    if(!searchBar.getText().isEmpty()) {
                        if(stack == null || !NotEnoughUpdates.INSTANCE.manager.doesStackMatchSearch(stack, searchBar.getText())) {
                            GlStateManager.disableDepth();
                            Gui.drawRect(itemX, itemY, itemX+16, itemY+16, 0x80000000);
                            GlStateManager.enableDepth();
                        }
                    }

                    GlStateManager.disableLighting();

                    if(mouseInsideStorages && mouseX >= guiLeft+itemX && mouseX < guiLeft+itemX+18 && mouseY >= guiTop+itemY && mouseY < guiTop+itemY+18) {
                        if(storageId != StorageManager.getInstance().getCurrentPageId()) {
                            hoveringOtherBackpack = true;
                            whiteOverlay = stackOnMouse == null;
                        } else {
                            itemHoverX = itemX;
                            itemHoverY = itemY;
                        }

                        if(stack != null) {
                            tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
                        }
                    }
                }

                GlStateManager.disableDepth();
                if(storageId == currentPage) {
                    Gui.drawRect(storageX+1, storageY, storageX, storageY+storageH, 0xffffdf00);
                    Gui.drawRect(storageX+storageW-1, storageY, storageX+storageW, storageY+storageH, 0xffffdf00);
                    Gui.drawRect(storageX, storageY, storageX+storageW, storageY+1, 0xffffdf00);
                    Gui.drawRect(storageX, storageY+storageH-1, storageX+storageW, storageY+storageH, 0xffffdf00);
                } else if(currentTime - StorageManager.getInstance().storageOpenSwitchMillis < 1000 &&
                        StorageManager.getInstance().desiredStoragePage == storageId &&
                        StorageManager.getInstance().getCurrentPageId() != storageId) {
                    Gui.drawRect(storageX, storageY, storageX+storageW, storageY+storageH, 0x30000000);

                    Minecraft.getMinecraft().getTextureManager().bindTexture(LOAD_CIRCLE_SEQ[loadCircleIndex]);
                    GlStateManager.color(loadCircleColour.getRed()/255f, loadCircleColour.getGreen()/255f,
                            loadCircleColour.getBlue()/255f, 1);

                    GlStateManager.pushMatrix();
                    GlStateManager.translate(storageX+storageW/2, storageY+storageH/2, 0);
                    GlStateManager.rotate(loadCircleRotation, 0, 0, 1);
                    Utils.drawTexturedRect(-10, -10, 20, 20, GL11.GL_LINEAR);
                    GlStateManager.popMatrix();
                } else if(whiteOverlay) {
                    Gui.drawRect(storageX, storageY, storageX+storageW, storageY+storageH, 0x80ffffff);
                } else {
                    Gui.drawRect(storageX, storageY, storageX+storageW, storageY+storageH, 0x30000000);
                }
                GlStateManager.enableDepth();
            }
        }
        GlScissorStack.pop(scaledResolution);

        //Inventory Text
        fontRendererObj.drawString("Inventory", 180, storageViewSize+6, textColour);
        searchBar.setCustomTextColour(searchTextColour);
        searchBar.render(252, storageViewSize+5);

        //Player Inventory
        GuiChest guiChest = (GuiChest) Minecraft.getMinecraft().currentScreen;
        ContainerChest containerChest = (ContainerChest) guiChest.inventorySlots;
        ItemStack[] playerItems = Minecraft.getMinecraft().thePlayer.inventory.mainInventory;
        int inventoryStartIndex = containerChest.getLowerChestInventory().getSizeInventory();
        GlStateManager.enableDepth();
        for(int i=0; i<9; i++) {
            int itemX = 181+18*i;
            int itemY = storageViewSize+76;

            //Utils.drawItemStack(playerItems[i], itemX, itemY);
            GlStateManager.pushMatrix();
            GlStateManager.translate(181-8, storageViewSize+18-(inventoryStartIndex/9*18+31), 0);
            guiChest.drawSlot(containerChest.inventorySlots.get(inventoryStartIndex+i));
            GlStateManager.popMatrix();

            if(!searchBar.getText().isEmpty()) {
                if(playerItems[i] == null || !NotEnoughUpdates.INSTANCE.manager.doesStackMatchSearch(playerItems[i], searchBar.getText())) {
                    GlStateManager.disableDepth();
                    Gui.drawRect(itemX, itemY, itemX+16, itemY+16, 0x80000000);
                    GlStateManager.enableDepth();
                }
            }

            if(mouseX >= guiLeft+itemX && mouseX < guiLeft+itemX+18 && mouseY >= guiTop+itemY && mouseY < guiTop+itemY+18) {
                itemHoverX = itemX;
                itemHoverY = itemY;

                if(playerItems[i] != null) {
                    tooltipToDisplay = playerItems[i].getTooltip(Minecraft.getMinecraft().thePlayer, Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
                }
            }
        }
        for(int i=0; i<27; i++) {
            int itemX = 181+18*(i%9);
            int itemY = storageViewSize+18+18*(i/9);

            //Utils.drawItemStack(playerItems[i+9], itemX, itemY);
            GlStateManager.pushMatrix();
            GlStateManager.translate(181-8, storageViewSize+18-(inventoryStartIndex/9*18+31), 0);
            guiChest.drawSlot(containerChest.inventorySlots.get(inventoryStartIndex+9+i));
            GlStateManager.popMatrix();

            if(!searchBar.getText().isEmpty()) {
                if(playerItems[i+9] == null || !NotEnoughUpdates.INSTANCE.manager.doesStackMatchSearch(playerItems[i+9], searchBar.getText())) {
                    GlStateManager.disableDepth();
                    Gui.drawRect(itemX, itemY, itemX+16, itemY+16, 0x80000000);
                    GlStateManager.enableDepth();
                }
            }

            if(mouseX >= guiLeft+itemX && mouseX < guiLeft+itemX+18 && mouseY >= guiTop+itemY && mouseY < guiTop+itemY+18) {
                itemHoverX = itemX;
                itemHoverY = itemY;

                if(playerItems[i+9] != null) {
                    tooltipToDisplay = playerItems[i+9].getTooltip(Minecraft.getMinecraft().thePlayer, Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
                }
            }
        }

        //Backpack Selector
        fontRendererObj.drawString("Ender Chest Pages", 9, storageViewSize+12, textColour);
        fontRendererObj.drawString("Storage Pages", 9, storageViewSize+44, textColour);
        if(StorageManager.getInstance().onStorageMenu) {
            for(int i=0; i<9; i++) {
                int itemX = 10+i*18;
                int itemY = storageViewSize+24;
                ItemStack stack = containerChest.getLowerChestInventory().getStackInSlot(i+9);
                Utils.drawItemStack(stack, itemX, itemY);

                if(mouseX >= guiLeft+itemX && mouseX < guiLeft+itemX+18 && mouseY >= guiTop+itemY && mouseY < guiTop+itemY+18) {
                    itemHoverX = itemX;
                    itemHoverY = itemY;

                    if(stack != null) {
                        if(NotEnoughUpdates.INSTANCE.config.storageGUI.enderchestPreview) slotPreview = i;
                        tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
                    }
                }
            }
            for(int i=0; i<18; i++) {
                int itemX = 10+18*(i%9);
                int itemY = storageViewSize+56+18*(i/9);
                ItemStack stack = containerChest.getLowerChestInventory().getStackInSlot(i+27);
                Utils.drawItemStack(stack, itemX, itemY);

                if(mouseX >= guiLeft+itemX && mouseX < guiLeft+itemX+18 && mouseY >= guiTop+itemY && mouseY < guiTop+itemY+18) {
                    itemHoverX = itemX;
                    itemHoverY = itemY;

                    if(stack != null) {
                        if(NotEnoughUpdates.INSTANCE.config.storageGUI.backpackPreview) slotPreview = i+StorageManager.MAX_ENDER_CHEST_PAGES;
                        tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
                    }
                }
            }
        } else {
            for(int i=0; i<9; i++) {
                StorageManager.StoragePage page = StorageManager.getInstance().getPage(i, false);
                int itemX = 10+(i%9)*18;
                int itemY = storageViewSize+24+(i/9)*18;

                ItemStack stack;
                if(page != null && page.backpackDisplayStack != null) {
                    stack = page.backpackDisplayStack;
                } else {
                    stack = StorageManager.LOCKED_ENDERCHEST_STACK;
                }

                if(stack != null) {
                    Utils.drawItemStack(stack, itemX, itemY);

                    if(mouseX >= guiLeft+itemX && mouseX < guiLeft+itemX+18 && mouseY >= guiTop+itemY && mouseY < guiTop+itemY+18) {
                        itemHoverX = itemX;
                        itemHoverY = itemY;
                        if(NotEnoughUpdates.INSTANCE.config.storageGUI.enderchestPreview) slotPreview = i;
                        tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
                    }
                }
            }
            for(int i=0; i<18; i++) {
                StorageManager.StoragePage page = StorageManager.getInstance().getPage(i+StorageManager.MAX_ENDER_CHEST_PAGES, false);
                int itemX = 10+(i%9)*18;
                int itemY = storageViewSize+56+(i/9)*18;

                ItemStack stack;
                if(page != null && page.backpackDisplayStack != null) {
                    stack = page.backpackDisplayStack;
                } else {
                    stack = StorageManager.getInstance().getMissingBackpackStack(i);
                }

                if(stack != null) {
                    Utils.drawItemStack(stack, itemX, itemY);

                    if(mouseX >= guiLeft+itemX && mouseX < guiLeft+itemX+18 && mouseY >= guiTop+itemY && mouseY < guiTop+itemY+18) {
                        itemHoverX = itemX;
                        itemHoverY = itemY;
                        if(NotEnoughUpdates.INSTANCE.config.storageGUI.backpackPreview) slotPreview = i+StorageManager.MAX_ENDER_CHEST_PAGES;
                        tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
                    }
                }
            }
        }

        //Buttons
        Minecraft.getMinecraft().getTextureManager().bindTexture(STORAGE_ICONS_TEXTURE);
        GlStateManager.color(1, 1, 1, 1);
        for(int i=0; i<10; i++) {
            int buttonX = 388+(i%5)*18;
            int buttonY = getStorageViewSize()+35+(i/5)*18;

            float minU = (i*16)/256f;
            float maxU = (i*16+16)/256f;

            int vIndex = 0;

            switch(i) {
                case 2:
                    vIndex = NotEnoughUpdates.INSTANCE.config.storageGUI.displayStyle; break;
                case 3:
                    vIndex = NotEnoughUpdates.INSTANCE.config.storageGUI.backpackPreview ? 1 : 0; break;
                case 4:
                    vIndex = NotEnoughUpdates.INSTANCE.config.storageGUI.enderchestPreview ? 1 : 0; break;
            }

            Utils.drawTexturedRect(buttonX, buttonY, 16, 16, minU, maxU, (vIndex*16)/256f, (vIndex*16+16)/256f, GL11.GL_NEAREST);

            if(mouseX >= guiLeft+buttonX && mouseX < guiLeft+buttonX+18 &&
                    mouseY >= guiTop+buttonY && mouseY < guiTop+buttonY+18) {
                switch(i) {
                    case 0:
                        tooltipToDisplay = createTooltip(
                                "Enable GUI",
                                0,
                                "On",
                                "Off"
                        ); break;
                    case 1:
                        int tooltipStorageHeight = desiredHeightSwitch != -1 ? desiredHeightSwitch :
                                NotEnoughUpdates.INSTANCE.config.storageGUI.storageHeight;
                        tooltipToDisplay = createTooltip(
                                "Storage View Height",
                                Math.round((tooltipStorageHeight-104)/52f),
                                "Tiny",
                                "Small",
                                "Medium",
                                "Large",
                                "Huge"
                        );
                        if(desiredHeightSwitch != -1) {
                            tooltipToDisplay.add("");
                            tooltipToDisplay.add(EnumChatFormatting.YELLOW+"* Move mouse to apply changes *");
                        }
                        break;
                    case 2:
                        tooltipToDisplay = createTooltip(
                                "Overlay Style",
                                NotEnoughUpdates.INSTANCE.config.storageGUI.displayStyle,
                                "Transparent",
                                "Minecraft",
                                "Dark",
                                "Custom"
                        );
                        break;
                    case 3:
                        tooltipToDisplay = createTooltip(
                                "Backpack Preview",
                                NotEnoughUpdates.INSTANCE.config.storageGUI.backpackPreview ? 0 : 1,
                                "On",
                                "Off"
                        );
                        break;
                    case 4:
                        tooltipToDisplay = createTooltip(
                                "Enderchest Preview",
                                NotEnoughUpdates.INSTANCE.config.storageGUI.enderchestPreview ? 0 : 1,
                                "On",
                                "Off"
                        );
                        break;
                }
            }
        }

        if(itemHoverX >= 0 && itemHoverY >= 0) {
            GlStateManager.disableDepth();
            GlStateManager.colorMask(true, true, true, false);
            Gui.drawRect(itemHoverX, itemHoverY, itemHoverX + 16, itemHoverY + 16, 0x80ffffff);
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.enableDepth();
        }

        GlStateManager.popMatrix();
        GlStateManager.translate(0, 0, 100);
        if(stackOnMouse != null) {
            if(hoveringOtherBackpack) {
                Utils.drawItemStack(new ItemStack(Item.getItemFromBlock(Blocks.barrier)), mouseX - 8, mouseY - 8);
            } else {
                Utils.drawItemStack(stackOnMouse, mouseX - 8, mouseY - 8);
            }
        } else if(slotPreview >= 0) {
            StorageManager.StoragePage page = StorageManager.getInstance().getPage(slotPreview, false);

            if(page != null && page.rows > 0) {
                int rows = page.rows;

                BackgroundBlur.renderBlurredBackground(7, width, height, mouseX+2, mouseY+2, 172, 10+18*rows);
                Utils.drawGradientRect(mouseX+2, mouseY+2, mouseX+174, mouseY+12+18*rows, 0xc0101010, 0xd0101010);

                Minecraft.getMinecraft().getTextureManager().bindTexture(storagePreviewTexture);
                GlStateManager.translate(0, 0, 100);
                GlStateManager.color(1, 1, 1, 1);
                Utils.drawTexturedRect(mouseX, mouseY, 176, 7, 0, 1, 0, 7/32f, GL11.GL_NEAREST);
                for(int i=0; i<rows; i++) {
                    Utils.drawTexturedRect(mouseX, mouseY+7+18*i, 176, 18, 0, 1, 7/32f, 25/32f, GL11.GL_NEAREST);
                }
                Utils.drawTexturedRect(mouseX, mouseY+7+18*rows, 176, 7, 0, 1, 25/32f, 1, GL11.GL_NEAREST);

                for(int i=0; i<rows*9; i++) {
                    ItemStack stack = page.items[i];
                    if(stack != null) {
                        Utils.drawItemStack(stack, mouseX+8+18*(i%9), mouseY+8+18*(i/9));
                    }
                }
                GlStateManager.translate(0, 0, -100);
            } else {
                Utils.drawHoveringText(tooltipToDisplay, mouseX, mouseY,  width, height, -1, Minecraft.getMinecraft().fontRendererObj);
            }
        } else if(tooltipToDisplay != null) {
            Utils.drawHoveringText(tooltipToDisplay, mouseX, mouseY,  width, height, -1, Minecraft.getMinecraft().fontRendererObj);
        }
        GlStateManager.translate(0, 0, -100);
    }

    private List<String> createTooltip(String title, int selectedOption, String... options) {
        String selPrefix = EnumChatFormatting.DARK_AQUA + " \u25b6 ";
        String unselPrefix = EnumChatFormatting.GRAY.toString();

        for(int i=0; i<options.length; i++) {
            if(i == selectedOption) {
                options[i] = selPrefix + options[i];
            } else {
                options[i] = unselPrefix + options[i];
            }
        }

        List list = Lists.newArrayList(options);
        list.add(0, "");
        list.add(0, EnumChatFormatting.GREEN+title);
        return list;
    }

    private static class IntPair {
        int x;
        int y;

        public IntPair(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public IntPair getPageCoords(int displayId) {
        if(displayId < 0) displayId = 0;

        int y = -scroll.getValue()+17+104*(displayId/3);
        for(int i=0; i<=displayId-3; i += 3) {
            int maxRows = 1;
            for(int j=i; j<i+3; j++) {
                if(!StorageManager.getInstance().storageConfig.displayToStorageIdMap.containsKey(j)) {
                    continue;
                }
                int storageId = StorageManager.getInstance().storageConfig.displayToStorageIdMap.get(j);
                StorageManager.StoragePage page = StorageManager.getInstance().getPage(storageId, false);
                if(page == null || page.rows <= 0) {
                    maxRows = Math.max(maxRows, 3);
                } else {
                    maxRows = Math.max(maxRows, page.rows);
                }
            }
            y -= (5-maxRows)*18;
        }

        return new IntPair(8+172*(displayId%3), y);
    }

    @Override
    public boolean mouseInput(int mouseX, int mouseY) {
        if(!(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) return false;

        int dWheel = Mouse.getEventDWheel();
        if(dWheel != 0) {
            if(dWheel < 0) {
                dWheel = -1;
                if(scrollVelocity > 0) scrollVelocity = 0;
            }
            if(dWheel > 0) {
                dWheel = 1;
                if(scrollVelocity < 0) scrollVelocity = 0;
            }

            long currentTime = System.currentTimeMillis();
            if(currentTime - lastScroll > 200) {
                scrollVelocity = 0;
            } else {
                scrollVelocity = (int)(scrollVelocity / 1.3f);
            }
            lastScroll = currentTime;

            scrollVelocity += dWheel*10;
            scrollToY(scroll.getTarget()-scrollVelocity);

            return true;
        }

        if(Mouse.getEventButton() == 0) {
            if(!Mouse.getEventButtonState()) {
                scrollGrabOffset = -1;
            } else if(mouseX >= guiLeft+519 && mouseX <= guiLeft+519+14 &&
                        mouseY >= guiTop+8 && mouseY <= guiTop+106) {
                int scrollMouseY = mouseY - (guiTop+8);
                int scrollBarY = Math.round(getScrollBarHeight()*scroll.getValue()/(float)getMaximumScroll());

                if(scrollMouseY >= scrollBarY && scrollMouseY < scrollBarY+12) {
                    scrollGrabOffset = scrollMouseY - scrollBarY;
                }
            }
        }
        if(scrollGrabOffset >= 0 && Mouse.getEventButton() == -1 && !Mouse.getEventButtonState()) {
            int scrollMouseY = mouseY - (guiTop+8);
            int scrollBarY = scrollMouseY - scrollGrabOffset;

            scrollToY(Math.round(scrollBarY*getMaximumScroll()/(float)getScrollBarHeight()));
            scroll.setTimeToReachTarget(10);
        }

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();

        int storageViewSize = getStorageViewSize();

        int sizeX = 540;
        int sizeY = 100+storageViewSize;
        int searchNobX = 18;

        guiLeft = width/2 - (sizeX-searchNobX)/2;
        guiTop = height/2 - sizeY/2;

        if(mouseX > guiLeft+181 && mouseX < guiLeft+181+162 &&
                mouseY > guiTop+storageViewSize+18 && mouseY < guiTop+storageViewSize+94) {
            return false;
        }

        if(mouseY > guiTop+3 && mouseY < guiTop+storageViewSize+3) {
            int currentPage = StorageManager.getInstance().getCurrentPageId();

            for(Map.Entry<Integer, Integer> entry : StorageManager.getInstance().storageConfig.displayToStorageIdMap.entrySet()) {
                IntPair pageCoords = getPageCoords(entry.getKey());

                if(pageCoords.y > storageViewSize+3 || pageCoords.y+90 < 3) continue;

                StorageManager.StoragePage page = StorageManager.getInstance().getPage(entry.getValue(), false);
                int rows = page == null ? 3 : page.rows <= 0 ? 3 : page.rows;
                if(mouseX > guiLeft+pageCoords.x && mouseX < guiLeft+pageCoords.x+162 &&
                        mouseY > guiTop+pageCoords.y && mouseY < guiTop+pageCoords.y+rows*18) {
                    if(currentPage >= 0 && entry.getValue() == currentPage) {
                        return false;
                    } else {
                        if(Mouse.getEventButtonState() && Mouse.getEventButton() == 0 &&
                                Minecraft.getMinecraft().thePlayer.inventory.getItemStack() == null &&
                                page != null) {
                            scrollToStorage(entry.getKey(), false);
                            StorageManager.getInstance().sendToPage(entry.getValue());
                            return true;
                        }
                    }
                }
            }
        }

        for(int i=0; i<10; i++) {
            int buttonX = 388+(i%5)*18;
            int buttonY = getStorageViewSize()+35+(i/5)*18;

            float minU = (i*16)/256f;
            float maxU = (i*16+16)/256f;

            int vIndex = 0;

            switch(i) {
                case 2:
                    vIndex = NotEnoughUpdates.INSTANCE.config.storageGUI.displayStyle; break;
                /*case 3:
                    vIndex = */
            }

            Utils.drawTexturedRect(buttonX, buttonY, 16, 16, minU, maxU, (vIndex*16)/256f, (vIndex*16+16)/256f, GL11.GL_NEAREST);
        }
        if(desiredHeightSwitch != -1 && Mouse.getEventButton() == -1 && !Mouse.getEventButtonState()) {
            int delta = Math.abs(desiredHeightMX - mouseX) + Math.abs(desiredHeightMY - mouseY);
            if(delta > 3) {
                NotEnoughUpdates.INSTANCE.config.storageGUI.storageHeight = desiredHeightSwitch;
                desiredHeightSwitch = -1;
            }
        }
        if(Mouse.getEventButtonState() && mouseX >= guiLeft+388 && mouseX < guiLeft+388+90 &&
                mouseY >= guiTop+storageViewSize+35 && mouseY < guiTop+storageViewSize+35+36) {
            int xN = mouseX - (guiLeft+388);
            int yN = mouseY - (guiTop+storageViewSize+35);

            int xIndex = xN/18;
            int yIndex = yN/18;

            int buttonIndex = xIndex + 5*yIndex;

            switch(buttonIndex) {
                case 0:
                    NotEnoughUpdates.INSTANCE.config.storageGUI.enableStorageGUI = false; break;
                case 1:
                    int size = desiredHeightSwitch != -1 ? desiredHeightSwitch : NotEnoughUpdates.INSTANCE.config.storageGUI.storageHeight;
                    int sizeIndex = Math.round((size-104)/54f);
                    if(Mouse.getEventButton() == 0) {
                        sizeIndex--;
                    } else {
                        sizeIndex++;
                    }
                    size = sizeIndex*54+104;
                    if(size < 104) size = 104;
                    if(size > 312) size = 312;
                    desiredHeightMX = mouseX;
                    desiredHeightMY = mouseY;
                    desiredHeightSwitch = size; break;
                case 2:
                    int displayStyle = NotEnoughUpdates.INSTANCE.config.storageGUI.displayStyle;
                    if(Mouse.getEventButton() == 0) {
                        displayStyle--;
                    } else {
                        displayStyle++;
                    }
                    if(displayStyle < 0) displayStyle = STORAGE_TEXTURES.length-1;
                    if(displayStyle >= STORAGE_TEXTURES.length) displayStyle = 0;

                    NotEnoughUpdates.INSTANCE.config.storageGUI.displayStyle = displayStyle; break;
                case 3:
                    NotEnoughUpdates.INSTANCE.config.storageGUI.backpackPreview =
                            !NotEnoughUpdates.INSTANCE.config.storageGUI.backpackPreview; break;
                case 4:
                    NotEnoughUpdates.INSTANCE.config.storageGUI.enderchestPreview =
                            !NotEnoughUpdates.INSTANCE.config.storageGUI.enderchestPreview; break;
            }
        }

        if(mouseX >= guiLeft+10 && mouseX <= guiLeft+171 &&
                mouseY >= guiTop+storageViewSize+23 && mouseY <= guiTop+storageViewSize+91) {
            if(StorageManager.getInstance().onStorageMenu) {
                return false;
            } else if(Mouse.getEventButtonState() && Mouse.getEventButton() == 0) {
                for(int i=0; i<9; i++) {
                    int storageId = i;
                    int displayId = StorageManager.getInstance().getDisplayIdForStorageId(i);

                    StorageManager.StoragePage page = StorageManager.getInstance().getPage(storageId, false);
                    if(page != null) {
                        int itemX = 10+(i%9)*18;
                        int itemY = storageViewSize+24+(i/9)*18;

                        if(mouseX >= guiLeft+itemX && mouseX < guiLeft+itemX+18 &&
                                mouseY >= guiTop+itemY && mouseY < guiTop+itemY+18) {
                            StorageManager.getInstance().sendToPage(storageId);
                            scrollToStorage(displayId, true);
                            return true;
                        }
                    }
                }
                for(int i=0; i<18; i++) {
                    int storageId = i+StorageManager.MAX_ENDER_CHEST_PAGES;
                    int displayId = StorageManager.getInstance().getDisplayIdForStorageId(i);

                    StorageManager.StoragePage page = StorageManager.getInstance().getPage(storageId, false);
                    if(page != null) {
                        int itemX = 10+(i%9)*18;
                        int itemY = storageViewSize+56+(i/9)*18;

                        if(mouseX >= guiLeft+itemX && mouseX < guiLeft+itemX+18 &&
                                mouseY >= guiTop+itemY && mouseY < guiTop+itemY+18) {
                            StorageManager.getInstance().sendToPage(storageId);
                            scrollToStorage(displayId, true);
                            return true;
                        }
                    }
                }
            }
        }

        return true;
    }

    public void overrideIsMouseOverSlot(Slot slot, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
        if(StorageManager.getInstance().shouldRenderStorageOverlayFast()) {
            boolean playerInv = slot.inventory == Minecraft.getMinecraft().thePlayer.inventory;

            int slotId = slot.getSlotIndex();
            int storageViewSize = getStorageViewSize();

            if(playerInv) {
                if(slotId < 9) {
                    if(mouseY >= guiTop+storageViewSize+76 && mouseY <= guiTop+storageViewSize+92) {
                        int xN = mouseX-(guiLeft+181);

                        int xClicked = xN/18;

                        if(xClicked == slotId) {
                            cir.setReturnValue(true);
                            return;
                        }
                    }
                } else {
                    int xN = mouseX-(guiLeft+181);
                    int yN = mouseY-(guiTop+storageViewSize+18);

                    int xClicked = xN/18;
                    int yClicked = yN/18;

                    if(xClicked >= 0 && xClicked <= 8 &&
                            yClicked >= 0 && yClicked <= 2) {
                        if(xClicked + yClicked*9 + 9 == slotId) {
                            cir.setReturnValue(true);
                            return;
                        }
                    }
                }
            } else {
                if(StorageManager.getInstance().onStorageMenu) {
                    if(slotId >= 9 && slotId < 18) {
                        if(mouseY >= guiTop+storageViewSize+24 && mouseY < guiTop+storageViewSize+24+18) {
                            int xN = mouseX-(guiLeft+10);

                            int xClicked = xN/18;

                            if(xClicked == slotId%9) {
                                cir.setReturnValue(true);
                                return;
                            }
                        }
                    } else if(slotId >= 27 && slotId < 45) {
                        int xN = mouseX-(guiLeft+10);
                        int yN = mouseY-(guiTop+storageViewSize+56);

                        int xClicked = xN/18;
                        int yClicked = yN/18;

                        if(xClicked == slotId%9 &&
                                yClicked >= 0 && yClicked == slotId/9-3) {
                            cir.setReturnValue(true);
                            return;
                        }
                    }
                } else {
                    int currentPage = StorageManager.getInstance().getCurrentPageId();
                    int displayId = StorageManager.getInstance().getDisplayIdForStorageId(currentPage);
                    if(displayId >= 0) {
                        IntPair pageCoords = getPageCoords(displayId);

                        int xN = mouseX-(guiLeft+pageCoords.x);
                        int yN = mouseY-(guiTop+pageCoords.y);

                        int xClicked = xN/18;
                        int yClicked = yN/18;

                        if(xClicked >= 0 && xClicked <= 8 &&
                                yClicked >= 0 && yClicked <= 5) {
                            if(xClicked + yClicked*9 + 9 == slotId) {
                                cir.setReturnValue(true);
                                return;
                            }
                        }
                    }
                }
            }
            cir.setReturnValue(false);
        }
    }

    public void clearSearch() {
        searchBar.setText("");
        StorageManager.getInstance().searchDisplay(searchBar.getText());
    }

    @Override
    public boolean keyboardInput() {
        if(Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
            Minecraft.getMinecraft().thePlayer.closeScreen();
            return false;
        }

        if(Keyboard.getEventKeyState()) {
            String prevText = searchBar.getText();
            searchBar.setFocus(true);
            searchBar.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
            if(!prevText.equals(searchBar.getText())) {
                StorageManager.getInstance().searchDisplay(searchBar.getText());
            }
        }

        return false;
    }
}
