package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.CustomItemEffects;
import net.minecraft.client.renderer.RenderGlobal;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    //setupTerrain
    @ModifyVariable(method="setupTerrain", at=@At(value="STORE"), ordinal = 4)
    public double setupTerrain_d0(double d3) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.x;
        }
        return d3;
    }

    @ModifyVariable(method="setupTerrain", at=@At(value="STORE"), ordinal = 5)
    public double setupTerrain_d1(double d4) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.y;
        }
        return d4;
    }

    @ModifyVariable(method="setupTerrain", at=@At(value="STORE"), ordinal = 6)
    public double setupTerrain_d2(double d5) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.z;
        }
        return d5;
    }

    //renderEntities
    @ModifyVariable(method="renderEntities", at=@At(value="STORE"), ordinal = 3)
    public double renderEntities_d0(double d3) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.x;
        }
        return d3;
    }

    @ModifyVariable(method="renderEntities", at=@At(value="STORE"), ordinal = 4)
    public double renderEntities_d1(double d4) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.y;
        }
        return d4;
    }

    @ModifyVariable(method="renderEntities", at=@At(value="STORE"), ordinal = 5)
    public double renderEntities_d2(double d5) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.z;
        }
        return d5;
    }

}
