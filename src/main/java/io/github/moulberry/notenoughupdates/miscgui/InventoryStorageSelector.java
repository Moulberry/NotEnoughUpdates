package io.github.moulberry.notenoughupdates.miscgui;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.config.KeybindHelper;
import io.github.moulberry.notenoughupdates.miscfeatures.StorageManager;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class InventoryStorageSelector {

    private static final InventoryStorageSelector INSTANCE = new InventoryStorageSelector();

    private static final ResourceLocation ICONS = new ResourceLocation("notenoughupdates:storage_gui/hotbar_icons.png");

    public boolean isOverridingSlot = false;
    public int selectedIndex = 0;

    public static InventoryStorageSelector getInstance() {
        return INSTANCE;
    }

    public boolean isSlotSelected() {
        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() || !NotEnoughUpdates.INSTANCE.config.storageGUI.showInvBackpack) {
            isOverridingSlot = false;
            return false;
        }
        if(Minecraft.getMinecraft().currentScreen != null) {
            return false;
        }
        if(Minecraft.getMinecraft().thePlayer == null) {
            isOverridingSlot = false;
            return false;
        }
        if(Minecraft.getMinecraft().thePlayer.inventory.currentItem != 0) {
            isOverridingSlot = false;
            return false;
        }
        return isOverridingSlot;
    }

    @SubscribeEvent
    public void onMousePress(MouseEvent event) {
        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() || !NotEnoughUpdates.INSTANCE.config.storageGUI.showInvBackpack) {
            return;
        }
        if(Minecraft.getMinecraft().currentScreen != null) {
            return;
        }

        if(KeybindHelper.isKeyPressed(NotEnoughUpdates.INSTANCE.config.storageGUI.arrowLeftKey)) {
            selectedIndex--;

            int max = StorageManager.getInstance().storageConfig.displayToStorageIdMap.size()-1;
            if(selectedIndex > max) selectedIndex = max;
            if(selectedIndex < 0) selectedIndex = 0;
        } else if(KeybindHelper.isKeyPressed(NotEnoughUpdates.INSTANCE.config.storageGUI.arrowRightKey)) {
            selectedIndex++;

            int max = StorageManager.getInstance().storageConfig.displayToStorageIdMap.size()-1;
            if(selectedIndex > max) selectedIndex = max;
            if(selectedIndex < 0) selectedIndex = 0;
        } else if(KeybindHelper.isKeyPressed(NotEnoughUpdates.INSTANCE.config.storageGUI.arrowDownKey)) {
            sendToPage(selectedIndex);
        }

        if(isSlotSelected()) {
            int useKeycode = Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode() + 100;
            int attackKeycode = Minecraft.getMinecraft().gameSettings.keyBindAttack.getKeyCode() + 100;

            if(Mouse.getEventButton() == useKeycode || Mouse.getEventButton() == attackKeycode) {
                if(Mouse.getEventButtonState() &&
                        Mouse.getEventButton() != NotEnoughUpdates.INSTANCE.config.storageGUI.backpackScrollKey+100) {
                    sendToPage(selectedIndex);
                }

                event.setCanceled(true);
            }
        }
    }

    private void sendToPage(int displayId) {
        if(!StorageManager.getInstance().storageConfig.displayToStorageIdMap.containsKey(displayId)) {
            return;
        }
        if(getPage(selectedIndex) == null) {
            NotEnoughUpdates.INSTANCE.sendChatMessage("/storage");
        } else {
            int index = StorageManager.getInstance().storageConfig.displayToStorageIdMap.get(selectedIndex);
            StorageManager.getInstance().sendToPage(index);
        }
    }

    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event) {
        if(Minecraft.getMinecraft().gameSettings.keyBindsHotbar[0].isKeyDown()) {
            isOverridingSlot = false;
        }
        if(Minecraft.getMinecraft().currentScreen != null) {
            return;
        }

        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() || !NotEnoughUpdates.INSTANCE.config.storageGUI.showInvBackpack) {
            return;
        }

        if(KeybindHelper.isKeyPressed(NotEnoughUpdates.INSTANCE.config.storageGUI.backpackHotkey)) {
            Minecraft.getMinecraft().thePlayer.inventory.currentItem = 0;
            isOverridingSlot = true;
        } else if(KeybindHelper.isKeyPressed(NotEnoughUpdates.INSTANCE.config.storageGUI.arrowLeftKey)) {
            selectedIndex--;

            int max = StorageManager.getInstance().storageConfig.displayToStorageIdMap.size()-1;
            if(selectedIndex > max) selectedIndex = max;
            if(selectedIndex < 0) selectedIndex = 0;
        } else if(KeybindHelper.isKeyPressed(NotEnoughUpdates.INSTANCE.config.storageGUI.arrowRightKey)) {
            selectedIndex++;

            int max = StorageManager.getInstance().storageConfig.displayToStorageIdMap.size()-1;
            if(selectedIndex > max) selectedIndex = max;
            if(selectedIndex < 0) selectedIndex = 0;
        } else if(KeybindHelper.isKeyPressed(NotEnoughUpdates.INSTANCE.config.storageGUI.arrowDownKey)) {
            sendToPage(selectedIndex);
        }

        if(isSlotSelected()) {
            KeyBinding attack = Minecraft.getMinecraft().gameSettings.keyBindAttack;
            KeyBinding use = Minecraft.getMinecraft().gameSettings.keyBindUseItem;

            if(attack.isPressed() || attack.isKeyDown()) {
                if(attack.getKeyCode() != NotEnoughUpdates.INSTANCE.config.storageGUI.backpackScrollKey) {
                    sendToPage(selectedIndex);
                }

                KeyBinding.setKeyBindState(attack.getKeyCode(), false);
                while(attack.isPressed()){}
            }

            if(use.isPressed() || use.isKeyDown()) {
                if(attack.getKeyCode() != NotEnoughUpdates.INSTANCE.config.storageGUI.backpackScrollKey) {
                    sendToPage(selectedIndex);
                }

                KeyBinding.setKeyBindState(use.getKeyCode(), false);
                while(use.isPressed()){}
            }
        }
    }

    public int onScroll(int direction, int resultantSlot) {
        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() || !NotEnoughUpdates.INSTANCE.config.storageGUI.showInvBackpack) {
            return resultantSlot;
        }
        if(Minecraft.getMinecraft().currentScreen != null) {
            return resultantSlot;
        }

        int keyCode = NotEnoughUpdates.INSTANCE.config.storageGUI.backpackScrollKey;
        if(isOverridingSlot && KeybindHelper.isKeyDown(keyCode)) {
            selectedIndex -= direction;
            int max = StorageManager.getInstance().storageConfig.displayToStorageIdMap.size()-1;

            if(selectedIndex > max) selectedIndex = max;
            if(selectedIndex < 0) selectedIndex = 0;
            return 0;
        }

        if(resultantSlot == 0 && direction == -1 && !isOverridingSlot) {
            isOverridingSlot = true;
            Minecraft.getMinecraft().getItemRenderer().resetEquippedProgress();
            return 0;
        } else if(resultantSlot == 1 && direction == -1 && isOverridingSlot) {
            isOverridingSlot = false;
            Minecraft.getMinecraft().getItemRenderer().resetEquippedProgress();
            return 0;
        } else if(resultantSlot == 8 && direction == 1 && !isOverridingSlot) {
            isOverridingSlot = true;
            Minecraft.getMinecraft().getItemRenderer().resetEquippedProgress();
            return 0;
        }
        return resultantSlot;
    }

    private StorageManager.StoragePage getPage(int selectedIndex) {
        if(!StorageManager.getInstance().storageConfig.displayToStorageIdMap.containsKey(selectedIndex)) {
            return null;
        }
        int index = StorageManager.getInstance().storageConfig.displayToStorageIdMap.get(selectedIndex);
        return StorageManager.getInstance().getPage(index, false);
    }

    public ItemStack getNamedHeldItemOverride() {
        StorageManager.StoragePage page = getPage(selectedIndex);
        if(page != null && page.backpackDisplayStack != null) {
            return page.backpackDisplayStack;
        }
        return new ItemStack(Item.getItemFromBlock(Blocks.chest));
    }

    public ItemStack getHeldItemOverride() {
        return getHeldItemOverride(selectedIndex);
    }

    public ItemStack getHeldItemOverride(int selectedIndex) {
        StorageManager.StoragePage page = getPage(selectedIndex);
        if(page != null) {
            ItemStack stack = page.backpackDisplayStack;
            if(stack == null || stack.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane)) {
                return new ItemStack(Item.getItemFromBlock(Blocks.ender_chest));
            }
            return stack;
        }
        return new ItemStack(Item.getItemFromBlock(Blocks.chest));
    }

    public void render(ScaledResolution scaledResolution, float partialTicks) {
        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() || !NotEnoughUpdates.INSTANCE.config.storageGUI.showInvBackpack) {
            return;
        }
        if(Minecraft.getMinecraft().currentScreen != null) {
            return;
        }

        int max = StorageManager.getInstance().storageConfig.displayToStorageIdMap.size()-1;
        if(selectedIndex > max) selectedIndex = max;
        if(selectedIndex < 0) selectedIndex = 0;

        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();
        FontRenderer fontRendererObj = Minecraft.getMinecraft().fontRendererObj;
        int centerX = width / 2;

        int offset = 91 + 10 + 12;

        if(NotEnoughUpdates.INSTANCE.config.storageGUI.backpackHotbarSide == 1) {
            offset *= -1;
        }

        ItemStack held = getHeldItemOverride();
        int left = centerX - offset - 12;
        int top = scaledResolution.getScaledHeight() - 22;

        if(NotEnoughUpdates.INSTANCE.config.storageGUI.showInvBackpackPreview && isSlotSelected()) {
            StorageManager.StoragePage page = getPage(selectedIndex);

            if(page != null && page.rows > 0) {
                int rows = page.rows;

                ResourceLocation storagePreviewTexture = StorageOverlay.STORAGE_PREVIEW_TEXTURES[NotEnoughUpdates.INSTANCE.config.storageGUI.displayStyle];

                int startX = centerX - 172/2;
                int startY = height - 70 - (10+18*rows);

                GlStateManager.translate(0, 0, 100);
                GL11.glDepthMask(false);

                Minecraft.getMinecraft().getTextureManager().bindTexture(storagePreviewTexture);
                GlStateManager.color(1, 1, 1,
                        NotEnoughUpdates.INSTANCE.config.storageGUI.backpackOpacity/100f);
                Utils.drawTexturedRect(startX, startY, 176, 7, 0, 1, 0, 7/32f, GL11.GL_NEAREST);
                for(int i=0; i<rows; i++) {
                    Utils.drawTexturedRect(startX, startY+7+18*i, 176, 18, 0, 1, 7/32f, 25/32f, GL11.GL_NEAREST);
                }
                Utils.drawTexturedRect(startX, startY+7+18*rows, 176, 7, 0, 1, 25/32f, 1, GL11.GL_NEAREST);

                GL11.glDepthMask(true);

                for(int i=0; i<rows*9; i++) {
                    ItemStack stack = page.items[i];
                    if(stack != null) {
                        Utils.drawItemStack(stack, startX+8+18*(i%9), startY+8+18*(i/9));
                    }
                }

                ItemStack named = getNamedHeldItemOverride();

                Utils.drawItemStack(held, centerX-8, startY-8);

                GlStateManager.translate(0, 0, 100);
                Utils.drawStringCentered(named.getDisplayName(), fontRendererObj, centerX, height - 66, true, 0xffff0000);
                int keyCode = NotEnoughUpdates.INSTANCE.config.storageGUI.backpackScrollKey;
                if(KeybindHelper.isKeyValid(keyCode) && !KeybindHelper.isKeyDown(keyCode)) {
                    String keyName = KeybindHelper.getKeyName(keyCode);
                    Utils.drawStringCentered("["+keyName+"] Scroll Backpacks", fontRendererObj, centerX, startY-10, true, 0xff32CD32);
                }
                GlStateManager.translate(0, 0, -200);

            } else if(page == null) {
                Utils.drawStringCentered("Run /storage to enable this feature!", fontRendererObj, centerX, height - 80, true, 0xffff0000);
            } else {
                Utils.drawStringCentered("Right-click to load items", fontRendererObj, centerX, height - 80, true, 0xffff0000);
            }
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(ICONS);
        GlStateManager.color(1, 1, 1, 1);
        Utils.drawTexturedRect(left + 1, top,
                22, 22, 0, 22/64f, 0, 22/64f, GL11.GL_NEAREST);
        if(isSlotSelected()) {
            Utils.drawTexturedRect(left, top - 1,
                    24, 22, 0, 24/64f, 22/64f, 44/64f, GL11.GL_NEAREST);
        }

        int index = 1;
        if(StorageManager.getInstance().storageConfig.displayToStorageIdMap.containsKey(selectedIndex)) {
            int displayIndex = StorageManager.getInstance().storageConfig.displayToStorageIdMap.get(selectedIndex);
            if(displayIndex < 9) {
                index = displayIndex+1;
            } else {
                index = displayIndex-8;
            }
        }

        Utils.drawItemStackWithText(held, left + 4, top + 3, ""+index);

        GlStateManager.enableBlend();
    }

}
