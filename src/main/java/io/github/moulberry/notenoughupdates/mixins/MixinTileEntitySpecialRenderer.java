package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.DungeonBlocks;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderList;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TileEntitySpecialRenderer.class})
public abstract class MixinTileEntitySpecialRenderer {

    @Inject(method="bindTexture", at=@At("HEAD"), cancellable = true)
    public void bindTexture(ResourceLocation location, CallbackInfo info) {
        if(DungeonBlocks.isInDungeons() && location.getResourcePath().equals("textures/entity/chest/normal.png")) {
            info.cancel();

            GlStateManager.bindTexture(DungeonBlocks.getModifiedTexture(location,
                    SpecialColour.specialToSimpleRGB(NotEnoughUpdates.INSTANCE.manager.config.dungChestColour.value)));
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }
    }
}
