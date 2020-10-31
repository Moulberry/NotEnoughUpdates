package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderFish;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(RenderFish.class)
public class MixinRenderFish {

    private EntityFishHook fishHook = null;

    @Inject(method = "doRender(Lnet/minecraft/entity/projectile/EntityFishHook;DDDFF)V", at=@At(value = "HEAD"), cancellable = true)
    public void render(EntityFishHook entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo cbi){
        fishHook = entity;
    }

    private static final String TARGET = "Lnet/minecraft/client/renderer/WorldRenderer;" +
            "color(IIII)Lnet/minecraft/client/renderer/WorldRenderer;";
    @Redirect(method="doRender", at=@At(value="INVOKE", target=TARGET))
    public WorldRenderer worldrenderColor_DoRender(WorldRenderer renderer, int r, int b, int g, int a){
        Minecraft mc = Minecraft.getMinecraft();
        if (NotEnoughUpdates.INSTANCE.isOnSkyblock() && fishHook != null && fishHook.angler != null && fishHook.angler.getHeldItem().getItem().equals(Items.fishing_rod)){
            String internalName = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(fishHook.angler.getHeldItem());
            if (internalName != null && !internalName.equals("GRAPPLING_HOOK") && !internalName.endsWith("_WHIP")) {
                if (fishHook.angler.getUniqueID().equals(mc.thePlayer.getUniqueID())) {
                    Color colour = new Color(SpecialColour.specialToChromaRGB(NotEnoughUpdates.INSTANCE.manager.config.selfRodLineColour.value), true);
                    renderer.color(colour.getRed(), colour.getGreen(), colour.getBlue(), colour.getAlpha());
                }else {
                    Color colour = new Color(SpecialColour.specialToChromaRGB(NotEnoughUpdates.INSTANCE.manager.config.otherRodLineColour.value), true);
                    renderer.color(colour.getRed(), colour.getGreen(), colour.getBlue(), colour.getAlpha());
                }
            }else {
                renderer.color(r, g, b, a);
            }
        }else {
            renderer.color(r, g, b, a);
        }
        return renderer;
    }

}
