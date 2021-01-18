package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.miscfeatures.CustomItemEffects;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import org.lwjgl.util.vector.Vector3f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EffectRenderer.class)
public class MixinEffectRenderer {

    /*@Redirect(method="renderParticles", at=@At(
            value = "FIELD",
            opcode = Opcodes.PUTSTATIC,
            target = "Lnet/minecraft/client/particle/EntityFX;interpPosX:D")
    )
    public void renderParticles_interpPosX(double interpPosX) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            EntityFX.interpPosX = currentPosition.x;
        }
        EntityFX.interpPosX = interpPosX;
    }

    @Redirect(method="renderParticles", at=@At(
            value = "FIELD",
            opcode = Opcodes.PUTSTATIC,
            target = "Lnet/minecraft/client/particle/EntityFX;interpPosY:D")
    )
    public void renderParticles_interpPosY(double interpPosY) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            EntityFX.interpPosY = currentPosition.y;
        }
        EntityFX.interpPosY = interpPosY;
    }

    @Redirect(method="renderParticles", at=@At(
            value = "FIELD",
            opcode = Opcodes.PUTSTATIC,
            target = "Lnet/minecraft/client/particle/EntityFX;interpPosZ:D")
    )
    public void renderParticles_interpPosZ(double interpPosZ) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            EntityFX.interpPosZ = currentPosition.z;
        }
        EntityFX.interpPosZ = interpPosZ;
    }*/

}
