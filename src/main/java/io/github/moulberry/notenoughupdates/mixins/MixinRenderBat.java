package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.DungeonBlocks;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderBat;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Mixin({RenderBat.class})
public abstract class MixinRenderBat {

    @Inject(method="getEntityTexture", at=@At("HEAD"), cancellable = true)
    public void getEntityTexture(EntityBat entity, CallbackInfoReturnable<ResourceLocation> cir) {
        if(DungeonBlocks.isInDungeons()) {
            ResourceLocation rl = new ResourceLocation("notenoughupdates:dynamic/dungeon_bat");
            Minecraft.getMinecraft().getTextureManager().loadTexture(rl, new AbstractTexture() {
                public void loadTexture(IResourceManager resourceManager) {
                    glTextureId = DungeonBlocks.getModifiedTexture(new ResourceLocation("textures/entity/bat.png"),
                            SpecialColour.specialToSimpleRGB(NotEnoughUpdates.INSTANCE.manager.config.dungBatColour.value));
                }
            });
            cir.setReturnValue(rl);
        }
    }
}
