package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.DungeonBlocks;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Render.class)
public class MixinRender {

    @Inject(method="bindEntityTexture", at=@At("HEAD"), cancellable = true)
    public void bindEntityTexture(Entity entity, CallbackInfoReturnable cir) {
        if(entity instanceof EntityBat && DungeonBlocks.isInDungeons()) {
            int tex = DungeonBlocks.getModifiedTexture(new ResourceLocation("textures/entity/bat.png"),
                    SpecialColour.specialToSimpleRGB(NotEnoughUpdates.INSTANCE.manager.config.dungBatColour.value));

            if(tex >= 0) {
                GlStateManager.bindTexture(tex);
                cir.setReturnValue(true);
            }
        }
    }

}
