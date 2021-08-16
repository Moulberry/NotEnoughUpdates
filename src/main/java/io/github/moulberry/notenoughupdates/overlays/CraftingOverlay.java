package io.github.moulberry.notenoughupdates.overlays;

import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.luaj.vm2.ast.Str;
import org.lwjgl.input.Mouse;

import java.util.List;

public class CraftingOverlay {
    private static ItemStack[] items = new ItemStack[9];
    public static boolean shouldRender = false;
    private static String text = null;

    public static void render() {
        if (shouldRender) {
            ContainerChest container = (ContainerChest) Minecraft.getMinecraft().thePlayer.openContainer;
            GuiChest gc = (GuiChest) Minecraft.getMinecraft().currentScreen;
            FontRenderer ft = Minecraft.getMinecraft().fontRendererObj;
            int width = Utils.peekGuiScale().getScaledWidth();
            int height = Utils.peekGuiScale().getScaledHeight();
            int mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
            int mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;
            List<String> tooltip = null;
            for (int i = 0; i < 9; i++) {
                if (items[i] != null) {
                    int slotIndex = (int) (10 + 9 * Math.floor(i / 3f) + (i % 3));
                    Slot slot = container.inventorySlots.get(slotIndex);
                    int x = slot.xDisplayPosition + gc.guiLeft;
                    int y = slot.yDisplayPosition + gc.guiTop;
                    if (!slot.getHasStack() || !slot.getStack().getDisplayName().equals(items[i].getDisplayName()) ||
                            slot.getStack().stackSize < items[i].stackSize)
                        Gui.drawRect(x, y,
                                x + 16, y + 16, 0x64ff0000);
                    if (!slot.getHasStack())
                        Utils.drawItemStack(items[i], x, y);
                    if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16)
                        tooltip = items[i].getTooltip(Minecraft.getMinecraft().thePlayer, false);
                }
            }
            if (text != null)
                ft.drawStringWithShadow(text,
                        Utils.peekGuiScale().getScaledWidth() / 2f - ft.getStringWidth(text) / 2f,
                        gc.guiTop - 15f, 0x808080);
            if (tooltip != null)
                Utils.drawHoveringText(tooltip, mouseX, mouseY, width, height, -1, ft);
        }
    }

    public static void updateItem(JsonObject item) {
        items = new ItemStack[9];
        String[] x = {"1", "2", "3"};
        String[] y = {"A", "B", "C"};
        for (int i = 0; i < 9; i++) {
            String name = y[i / 3] + x[i % 3];
            String itemS = item.getAsJsonObject("recipe").get(name).getAsString();
            if (itemS != null && !itemS.equals("")) {
                int count = 1;
                if (itemS.split(":").length == 2) {
                    count = Integer.parseInt(itemS.split(":")[1]);
                    itemS = itemS.split(":")[0];
                }
                JsonObject craft = NEUManager.getItemInformation().get(itemS);
                if (craft != null) {
                    ItemStack stack = NEUManager.jsonToStack(craft);
                    stack.stackSize = count;
                    items[i] = stack;
                }
            }
        }
        if (item.has("crafttext")) {
            text = item.get("crafttext").getAsString();
        }
        shouldRender = true;
    }
}
