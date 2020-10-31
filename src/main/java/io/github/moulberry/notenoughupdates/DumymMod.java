package io.github.moulberry.notenoughupdates;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DumymMod {

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        int width = event.gui.width / 2;
        int height = event.gui.height / 2 - 106;

        if (event.gui instanceof GuiChest)
        {
            event.buttonList.add(new GuiButtonItem(1001, width + 88, height + 47, new ItemStack(Blocks.crafting_table)));
            event.buttonList.add(new GuiButtonItem(1000, width + 88, height + 66, new ItemStack(Blocks.ender_chest)));
        }
    }

    private long lastButtonClick = -1;

    @SubscribeEvent
    public void onPostActionPerformedGui(GuiScreenEvent.ActionPerformedEvent.Post event) {
        long now = System.currentTimeMillis();

        if (event.gui instanceof GuiChest)
        {
            if (now - this.lastButtonClick > 100L)
            {
                if (event.button.id == 1000)
                {
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/enderchest");
                }
                else if (event.button.id == 1001)
                {
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/craft");
                }
                this.lastButtonClick = now;
            }
        }
    }
}