package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.BetterContainers;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.StreamerMode;
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

}
