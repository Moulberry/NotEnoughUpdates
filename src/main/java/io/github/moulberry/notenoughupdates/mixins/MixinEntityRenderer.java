package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.miscfeatures.CustomItemEffects;
import net.minecraft.client.renderer.EntityRenderer;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

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

    @ModifyVariable(method="renderWorldPass", at=@At(value="STORE"), ordinal = 2)
    public double renderWorldPass_d2(double d2) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.z;
        }
        return d2;
    }

}
