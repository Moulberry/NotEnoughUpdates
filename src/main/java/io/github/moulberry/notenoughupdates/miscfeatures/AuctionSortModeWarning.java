package io.github.moulberry.notenoughupdates.miscfeatures;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.util.render.RenderUtils;
import io.github.moulberry.notenoughupdates.core.util.render.TextRenderUtils;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.List;

public class AuctionSortModeWarning {

    private static final AuctionSortModeWarning INSTANCE = new AuctionSortModeWarning();
    public static AuctionSortModeWarning getInstance() {
        return INSTANCE;
    }

    private boolean isAuctionBrowser() {
        return NotEnoughUpdates.INSTANCE.config.ahTweaks.enableSortWarning &&
                Minecraft.getMinecraft().currentScreen instanceof GuiChest &&
                (SBInfo.getInstance().lastOpenContainerName.startsWith("Auctions Browser") ||
                        SBInfo.getInstance().lastOpenContainerName.startsWith("Auctions: \""));
    }

    public void onPostGuiRender() {
        if(isAuctionBrowser()) {
            GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;

            ItemStack stack = chest.inventorySlots.getSlot(50).getStack();

            if(stack != null) {
                List<String> tooltip = stack.getTooltip(Minecraft.getMinecraft().thePlayer, false);

                String selectedSort = null;
                for(String line : tooltip) {
                    if(line.startsWith("\u00a75\u00a7o\u00a7b\u25B6 ")) {
                        selectedSort = Utils.cleanColour(line.substring("\u00a75\u00a7o\u00a7b\u25B6 ".length()));
                    }
                }

                if(selectedSort != null) {
                    if(!selectedSort.trim().equals("Lowest Price")) {
                        GlStateManager.disableLighting();
                        GlStateManager.pushMatrix();
                        GlStateManager.translate(0, 0, 500);

                        String selectedColour = "\u00a7e";

                        if(selectedSort.trim().equals("Highest Price")) {
                            selectedColour = "\u00a7c";
                        }

                        String warningText = "\u00a7aSort: " + selectedColour + selectedSort;
                        int warningLength = Minecraft.getMinecraft().fontRendererObj.getStringWidth(warningText);

                        int centerX = chest.guiLeft+chest.xSize/2+9;
                        int centerY = chest.guiTop+26;

                        RenderUtils.drawFloatingRectDark(centerX - warningLength/2 - 4, centerY - 6,
                                warningLength+8, 12, false);
                        TextRenderUtils.drawStringCenteredScaledMaxWidth(warningText, Minecraft.getMinecraft().fontRendererObj,
                                centerX, centerY, true, chest.width/2, 0xffffffff);
                        GlStateManager.popMatrix();
                    }
                }
            }
        }
    }

}
