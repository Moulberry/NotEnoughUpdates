package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.BetterContainers;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.StreamerMode;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GuiChest.class})
public class MixinGuiChest {

    private static final String TARGET = "Lnet/minecraft/client/renderer/texture/TextureManager;" +
            "bindTexture(Lnet/minecraft/util/ResourceLocation;)V";
    @Redirect(method="drawGuiContainerBackgroundLayer", at=@At(value="INVOKE", target=TARGET))
    public void drawGuiContainerBackgroundLayer_bindTexture(TextureManager textureManager, ResourceLocation location) {
        BetterContainers.bindHook(textureManager, location);
    }

    private static final String TARGET_DRAWSTRING = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I";
    @Redirect(method="drawGuiContainerForegroundLayer", at=@At(value="INVOKE", target = TARGET_DRAWSTRING))
    public int drawGuiContainerForegroundLayer_drawString(FontRenderer fontRenderer, String text, int x, int y, int color) {
        return fontRenderer.drawString(text, x, y, BetterContainers.isOverriding() ? BetterContainers.getTextColour() : color);
    }

    private static final String TARGET_SBADRAWSTRING = "Lcodes/biscuit/skyblockaddons/asm/hooks/GuiChestHook;" +
            "drawString(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)I";
    @Redirect(method="drawGuiContainerForegroundLayer", at=@At(value="INVOKE", target = TARGET_SBADRAWSTRING, remap = false))
    public int drawGuiContainerForegroundLayer_SBA_drawString(FontRenderer fontRenderer, String text, int x, int y, int color) {
        try {
            return (int)Class.forName("codes.biscuit.skyblockaddons.asm.hooks.GuiChestHook")
                    .getDeclaredMethod("drawString", FontRenderer.class, String.class, int.class, int.class, int.class)
                    .invoke(null, fontRenderer, text, x, y, BetterContainers.isOverriding() ? BetterContainers.getTextColour() : color);
        } catch(Exception e) {}
        return fontRenderer.drawString(text, x, y, BetterContainers.isOverriding() ? BetterContainers.getTextColour() : color);
    }


}
