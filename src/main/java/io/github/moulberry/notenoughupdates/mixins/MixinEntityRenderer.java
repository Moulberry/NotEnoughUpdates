package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.CustomItemEffects;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import net.minecraft.client.renderer.EntityRenderer;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    //orientCamera
    @ModifyVariable(method="orientCamera", at=@At(value="STORE"), ordinal = 0)
    public double orientCamera_d0(double d0) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.x;
        }
        return d0;
    }

    @ModifyVariable(method="orientCamera", at=@At(value="STORE"), ordinal = 1)
    public double orientCamera_d1(double d1) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.y;
        }
        return d1;
    }

    @ModifyVariable(method="orientCamera", at=@At(value="STORE"), ordinal = 2)
    public double orientCamera_d2(double d2) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.z;
        }
        return d2;
    }

    //renderWorldPass
    @Inject(method="renderWorldPass", at=@At("HEAD"))
    public void renderWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        CustomItemEffects.INSTANCE.partialTicks = partialTicks;
    }

    @ModifyVariable(method="renderWorldPass", at=@At(value="STORE"), ordinal = 0)
    public double renderWorldPass_d0(double d0) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.x;
        }
        return d0;
    }

    @ModifyVariable(method="renderWorldPass", at=@At(value="STORE"), ordinal = 1)
    public double renderWorldPass_d1(double d1) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.y;
        }
        return d1;
    }

    @ModifyVariable(method="renderWorldPass", at=@At(value="STORE"), ordinal = 2, print = true)
    public double renderWorldPass_d2(double d2) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.z;
        }
        return d2;
    }

}
