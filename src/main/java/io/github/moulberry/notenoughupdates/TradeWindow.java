package io.github.moulberry.notenoughupdates;

import io.github.moulberry.notenoughupdates.util.TexLoc;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;

public class TradeWindow {

    private static ResourceLocation location = new ResourceLocation("notenoughupdates", "custom_trade.png");

    private static final int xSize = 176;
    private static final int ySize = 204;
    private static int guiLeft;
    private static int guiTop;

    public static boolean tradeWindowActive() {
        if(Keyboard.isKeyDown(Keyboard.KEY_J)) return false;

        GuiScreen guiScreen = Minecraft.getMinecraft().currentScreen;
        if(guiScreen instanceof GuiChest) {
            GuiChest eventGui = (GuiChest) guiScreen;
            ContainerChest cc = (ContainerChest) eventGui.inventorySlots;
            String containerName = cc.getLowerChestInventory().getDisplayName().getUnformattedText();
            if(containerName.trim().startsWith("You     ")) {
                return true;
            }
        }
        return false;
    }

    private static TexLoc tl = new TexLoc(0, 0, Keyboard.KEY_M);

    public static void render(int mouseX, int mouseY) {
        if(!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) return;

        GuiContainer chest = ((GuiContainer)Minecraft.getMinecraft().currentScreen);
        ContainerChest cc = (ContainerChest) chest.inventorySlots;
        String containerName = cc.getLowerChestInventory().getDisplayName().getUnformattedText();

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());

        guiLeft = (scaledResolution.getScaledWidth()-xSize)/2;
        guiTop = (scaledResolution.getScaledHeight()-ySize)/2;

        List<String> tooltipToDisplay = null;
        tl.handleKeyboardInput();

        Minecraft.getMinecraft().getTextureManager().bindTexture(location);
        Utils.drawTexturedRect(guiLeft, guiTop, xSize, ySize, 0, 176/256f, 0, 204/256f, GL11.GL_NEAREST);
        
        Utils.drawTexturedRect(guiLeft+42, guiTop+92, 40, 14,
                0, 40/256f, ySize/256f, (ySize+14)/256f, GL11.GL_NEAREST);
        Utils.drawStringCentered("Confirm", Minecraft.getMinecraft().fontRendererObj, guiLeft+64, guiTop+96,
                false, 4210752);

        Utils.drawStringF(new ChatComponentTranslation("container.inventory").getUnformattedText(),
                Minecraft.getMinecraft().fontRendererObj, guiLeft+8, guiTop+111, false, 4210752);
        Utils.drawStringF("You", Minecraft.getMinecraft().fontRendererObj, guiLeft+8,
                guiTop+5, false, 4210752);
        String[] split = containerName.split(" ");
        if(split.length >= 1) {
            Utils.drawStringF(split[split.length-1], Minecraft.getMinecraft().fontRendererObj, guiLeft+109,
                    guiTop+5, false, 4210752);
        }

        int index=0;
        for(ItemStack stack : Minecraft.getMinecraft().thePlayer.inventory.mainInventory) {
            int x = 8+18*(index % 9);
            int y = 104+18*(index / 9);
            if(index < 9) y = 180;

            Utils.drawItemStack(stack, guiLeft+x, guiTop+y);

            if(mouseX > guiLeft+x-1 && mouseX < guiLeft+x+18) {
                if(mouseY > guiTop+y-1 && mouseY < guiTop+y+18) {
                    if(stack != null) tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, true);

                    GlStateManager.disableLighting();
                    GlStateManager.disableDepth();
                    GlStateManager.colorMask(true, true, true, false);
                    Utils.drawGradientRect(guiLeft+x, guiTop+y,
                            guiLeft+x + 16, guiTop+y + 16, -2130706433, -2130706433);
                    GlStateManager.colorMask(true, true, true, true);
                    GlStateManager.enableLighting();
                    GlStateManager.enableDepth();
                }
            }

            index++;
        }

        for(int i=0; i<16; i++) {
            int x = i % 4;
            int y = i / 4;
            int containerIndex = y*9+x;

            ItemStack stack = chest.inventorySlots.getInventory().get(containerIndex);

            Utils.drawItemStack(stack, guiLeft+10+x*18, guiTop+15+y*18);

            if(mouseX > guiLeft+10+x*18-1 && mouseX < guiLeft+10+x*18+18) {
                if(mouseY > guiTop+15+y*18-1 && mouseY < guiTop+15+y*18+18) {
                    if(stack != null) tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, true);

                    GlStateManager.disableLighting();
                    GlStateManager.disableDepth();
                    GlStateManager.colorMask(true, true, true, false);
                    Utils.drawGradientRect(guiLeft+10+x*18,  guiTop+15+y*18,
                            guiLeft+10+x*18 + 16,  guiTop+15+y*18 + 16, -2130706433, -2130706433);
                    GlStateManager.colorMask(true, true, true, true);
                    GlStateManager.enableLighting();
                    GlStateManager.enableDepth();
                }
            }
        }

        ItemStack bidStack = chest.inventorySlots.getInventory().get(36);
        if(bidStack != null) {
            Utils.drawItemStack(bidStack, guiLeft+10, guiTop+90);
            if(mouseX > guiLeft+10-1 && mouseX < guiLeft+10+18) {
                if(mouseY > guiTop+90-1 && mouseY < guiTop+90+18) {
                    tooltipToDisplay = bidStack.getTooltip(Minecraft.getMinecraft().thePlayer, true);
                }
            }
        }

        ItemStack confirmStack = chest.inventorySlots.getInventory().get(39);
        if(confirmStack != null) {
            if(mouseX > guiLeft+42 && mouseX < guiLeft+42+40) {
                if (mouseY > guiTop+92 && mouseY < guiTop+92+14) {
                    tooltipToDisplay = confirmStack.getTooltip(Minecraft.getMinecraft().thePlayer, true);
                }
            }
        }

        for(int i=0; i<16; i++) {
            int x = i % 4;
            int y = i / 4;
            int containerIndex = y*9+x+5;

            ItemStack stack = chest.inventorySlots.getInventory().get(containerIndex);

            Utils.drawItemStack(stack, guiLeft+96+x*18, guiTop+15+y*18);

            if(mouseX > guiLeft+96+x*18-1 && mouseX < guiLeft+96+x*18+18) {
                if(mouseY > guiTop+15+y*18-1 && mouseY < guiTop+15+y*18+18) {
                    if(stack != null) tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, true);

                    GlStateManager.disableLighting();
                    GlStateManager.disableDepth();
                    GlStateManager.colorMask(true, true, true, false);
                    Utils.drawGradientRect(guiLeft+96+x*18,  guiTop+15+y*18,
                            guiLeft+96+x*18 + 16,  guiTop+15+y*18 + 16, -2130706433, -2130706433);
                    GlStateManager.colorMask(true, true, true, true);
                    GlStateManager.enableLighting();
                    GlStateManager.enableDepth();
                }
            }
        }

        if(tooltipToDisplay != null) {
            Utils.drawHoveringText(tooltipToDisplay, mouseX, mouseY, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(),
                    -1, Minecraft.getMinecraft().fontRendererObj);
        }
    }

    public static void handleMouseInput() {
        if(!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) return;

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();

        int mouseX = Mouse.getEventX() * width / Minecraft.getMinecraft().displayWidth;
        int mouseY = height - Mouse.getEventY() * height / Minecraft.getMinecraft().displayHeight - 1;

        GuiContainer chest = ((GuiContainer)Minecraft.getMinecraft().currentScreen);

        if(Mouse.getEventButtonState() && Mouse.isButtonDown(0)) {
            int index=0;
            for(ItemStack stack : Minecraft.getMinecraft().thePlayer.inventory.mainInventory) {
                if(stack == null) {
                    index++;
                    continue;
                }

                int x = 8+18*(index % 9);
                int y = 104+18*(index / 9);
                if(index < 9) y = 180;

                if(mouseX > guiLeft+x && mouseX < guiLeft+x+16) {
                    if(mouseY > guiTop+y && mouseY < guiTop+y+16) {
                        Slot slot = chest.inventorySlots.getSlotFromInventory(Minecraft.getMinecraft().thePlayer.inventory, index);
                        Minecraft.getMinecraft().playerController.windowClick(
                                chest.inventorySlots.windowId,
                                slot.slotNumber, 2, 3, Minecraft.getMinecraft().thePlayer);
                        return;
                    }
                }

                index++;
            }

            for(int i=0; i<16; i++) {
                int x = i % 4;
                int y = i / 4;
                int containerIndex = y*9+x;

                if(mouseX > guiLeft+10+x*18-1 && mouseX < guiLeft+10+x*18+18) {
                    if(mouseY > guiTop+15+y*18-1 && mouseY < guiTop+15+y*18+18) {
                        Minecraft.getMinecraft().playerController.windowClick(
                                chest.inventorySlots.windowId,
                                containerIndex, 2, 3, Minecraft.getMinecraft().thePlayer);
                        return;
                    }
                }
            }

            if(mouseX > guiLeft+10-1 && mouseX < guiLeft+10+18) {
                if(mouseY > guiTop+90-1 && mouseY < guiTop+90+18) {
                    Minecraft.getMinecraft().playerController.windowClick(
                            chest.inventorySlots.windowId,
                            36, 2, 3, Minecraft.getMinecraft().thePlayer);
                    return;
                }
            }

            if(mouseX > guiLeft+42 && mouseX < guiLeft+42+40) {
                if (mouseY > guiTop+92 && mouseY < guiTop+92+14) {
                    Minecraft.getMinecraft().playerController.windowClick(
                            chest.inventorySlots.windowId,
                            39, 2, 3, Minecraft.getMinecraft().thePlayer);
                    return;
                }
            }
        }
    }

    public static boolean keyboardInput() {
        return Keyboard.getEventKey() != Keyboard.KEY_ESCAPE;
    }



}
