package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.miscfeatures.MiningStuff;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerCreeperCharge;
import net.minecraft.entity.monster.EntityCreeper;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LayerCreeperCharge.class)
public class MixinLayerCreeperCharge {

    /*@Redirect(method="doRenderLayer", at=@At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;color(FFFF)V"))
    public void doRenderLayer_color(float red, float green, float blue, float alpha) {
        Vector3f col = MiningStuff.getCreeperColour();
        GlStateManager.color(col.getX(), col.getY(), col.getZ(), alpha);
    }*/



}
