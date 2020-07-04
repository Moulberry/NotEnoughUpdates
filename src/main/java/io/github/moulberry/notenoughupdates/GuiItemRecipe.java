package io.github.moulberry.notenoughupdates;

import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class GuiItemRecipe extends GuiCrafting {

    private ItemStack[] craftMatrix;
    private String text;
    private String craftText = "";
    private NEUManager manager;

    public GuiItemRecipe(ItemStack[] craftMatrix, JsonObject result, String text, NEUManager manager) {
        super(Minecraft.getMinecraft().thePlayer.inventory, Minecraft.getMinecraft().theWorld);
        this.craftMatrix = craftMatrix;
        this.text = text;
        this.manager = manager;

        ContainerWorkbench cw = (ContainerWorkbench) this.inventorySlots;
        for(int i=0; i<Math.min(craftMatrix.length, 9); i++) {
            if(craftMatrix[i] == null) continue;
            cw.craftMatrix.setInventorySlotContents(i, craftMatrix[i]);
        }
        if(result.has("crafttext")) {
            craftText = result.get("crafttext").getAsString();
        }
        cw.craftResult.setInventorySlotContents(0, manager.jsonToStack(result));
    }

    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String t = text.equals("") ? I18n.format("container.crafting", new Object[0]) : text;

        Utils.drawStringScaledMaxWidth(t, fontRendererObj, 28, 6, t.contains("\u00a7"), xSize-38, 4210752);
        this.fontRendererObj.drawString(I18n.format("container.inventory", new Object[0]), 8, this.ySize - 96 + 2, 4210752);

        Utils.drawStringCenteredScaledMaxWidth(craftText, fontRendererObj, 132, 25,
                false, 75, 4210752);
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    }

    protected void handleMouseClick(Slot slotIn, int slotId, int clickedButton, int clickType) {
        ItemStack click = null;
        if(slotId >= 1 && slotId <= 9) {
            click = craftMatrix[slotId-1];
        } else if(slotId == 0) {
            ContainerWorkbench cw = (ContainerWorkbench) this.inventorySlots;
            click = cw.craftResult.getStackInSlot(0);
        }
        if(click != null) {
            if(clickedButton == 0) {
                manager.displayGuiItemRecipe(manager.getInternalNameForItem(click), "");
            } else if(clickedButton == 1) {
                manager.displayGuiItemUsages(manager.getInternalNameForItem(click), "");
            }
        }
    }

    /*public void handleMouseInput() throws IOException {
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int height = scaledresolution.getScaledHeight();

        int mouseX = Mouse.getX() / scaledresolution.getScaleFactor();
        int mouseY = height - Mouse.getY() / scaledresolution.getScaleFactor();
        if(mouseY > this.guiTop + this.ySize - 94 || mouseY < this.guiTop ||
                mouseX < this.guiLeft || mouseX > this.guiLeft+this.xSize) {
            //Potentially allow mouse input in the future. For now this is still broken.
            //super.handleMouseInput();
        }
    }*/

    public void onCraftMatrixChanged(IInventory inventoryIn){}
}
