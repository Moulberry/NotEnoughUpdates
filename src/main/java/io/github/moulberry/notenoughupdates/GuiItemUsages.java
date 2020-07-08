package io.github.moulberry.notenoughupdates;

import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.util.TexLoc;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class GuiItemUsages extends GuiCrafting {
    private static final ResourceLocation resourcePacksTexture = new ResourceLocation("textures/gui/resource_packs.png");

    private List<ItemStack[]> craftMatrices;
    private List<JsonObject> results;
    private int currentIndex = 0;

    private String text;
    private String craftText = "";
    private String collectionText = "";
    private NEUManager manager;

    private TexLoc left = new TexLoc(0, 0, Keyboard.KEY_N);
    private TexLoc right = new TexLoc(0, 0, Keyboard.KEY_M);

    public GuiItemUsages(List<ItemStack[]> craftMatrices, List<JsonObject> results, String text, NEUManager manager) {
        super(Minecraft.getMinecraft().thePlayer.inventory, Minecraft.getMinecraft().theWorld);

        this.craftMatrices = craftMatrices;
        this.results = results;
        this.text = text;
        this.manager = manager;

        setIndex(0);
    }

    private void setIndex(int index) {
        if(index < 0 || index >= craftMatrices.size()) {
            return;
        } else {
            currentIndex = index;

            ContainerWorkbench cw = (ContainerWorkbench) this.inventorySlots;
            for(int i=0; i<Math.min(craftMatrices.get(currentIndex).length, 9); i++) {
                cw.craftMatrix.setInventorySlotContents(i, craftMatrices.get(currentIndex)[i]);
            }
            if(results.get(currentIndex).has("crafttext")) {
                craftText = results.get(currentIndex).get("crafttext").getAsString();
            } else {
                craftText = "";
            }
            cw.craftResult.setInventorySlotContents(0, manager.jsonToStack(results.get(currentIndex)));
        }
    }

    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String t = "Crafting Usages";

        setIndex(currentIndex);

        int guiX = mouseX - guiLeft;
        int guiY = mouseY - guiTop;

        int buttonWidth = 7;
        int buttonHeight = 11;

        boolean leftSelected = false;
        boolean rightSelected = false;

        if(guiY > + 63 && guiY < + 63 + buttonHeight) {
            if(guiX > + 110 && guiX < 110 + buttonWidth) {
                leftSelected = true;
            } else if(guiX > 147 && guiX < 147 + buttonWidth) {
                rightSelected = true;
            }
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(resourcePacksTexture);
        //Left arrow
        Utils.drawTexturedRect(110, 63, 7, 11, 34/256f, 48/256f,
                5/256f + (leftSelected ? 32/256f : 0), 27/256f + (leftSelected ? 32/256f : 0));
        //Right arrow
        Utils.drawTexturedRect(147, 63, 7, 11, 10/256f, 24/256f,
                5/256f + (rightSelected ? 32/256f : 0), 27/256f + (rightSelected ? 32/256f : 0));
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        String str = (currentIndex+1)+"/"+craftMatrices.size();
        Utils.drawStringCenteredScaledMaxWidth(str, fontRendererObj, 132, 69,
                false, 24, Color.BLACK.getRGB());


        Utils.drawStringCenteredScaledMaxWidth(craftText, fontRendererObj, 132, 25,
                false, 75, 4210752);


        Utils.drawStringScaledMaxWidth(t, fontRendererObj, 28, 6, t.contains("\u00a7"), xSize-38, 4210752);
        this.fontRendererObj.drawString(I18n.format("container.inventory", new Object[0]), 8, this.ySize - 96 + 2, 4210752);
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput(); //TODO: r and u
        left.handleKeyboardInput();
        right.handleKeyboardInput();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        int guiX = mouseX - guiLeft;
        int guiY = mouseY - guiTop;

        int buttonWidth = 7;
        int buttonHeight = 11;

        if(guiY > + 63 && guiY < + 63 + buttonHeight) {
            if(guiX > + 110 && guiX < 110 + buttonWidth) {
                setIndex(currentIndex-1);
            } else if(guiX > 147 && guiX < 147 + buttonWidth) {
                setIndex(currentIndex+1);
            }
        }
    }

    protected void handleMouseClick(Slot slotIn, int slotId, int clickedButton, int clickType) {
        ItemStack click = null;
        if(slotId >= 1 && slotId <= 9) {
            click = craftMatrices.get(currentIndex)[slotId-1];
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
