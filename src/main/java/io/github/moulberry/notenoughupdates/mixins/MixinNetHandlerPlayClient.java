package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.miscfeatures.CustomItemEffects;
import io.github.moulberry.notenoughupdates.miscfeatures.EnchantingSolvers;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.miscfeatures.MiningStuff;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    private static final String TARGET = "Lnet/minecraft/entity/player/EntityPlayer;" +
            "setPositionAndRotation(DDDFF)V";
    @Redirect(method="handlePlayerPosLook", at=@At(value="INVOKE", target=TARGET))
    public void handlePlayerPosLook_setPositionAndRotation(EntityPlayer player, double x, double y, double z, float yaw, float pitch) {
        if(CustomItemEffects.INSTANCE.aoteTeleportationCurr != null) {
            CustomItemEffects.INSTANCE.aoteTeleportationMillis +=
                    Math.max(0, Math.min(300, NotEnoughUpdates.INSTANCE.config.smoothAOTE.smoothTpMillis));
        }
        player.setPositionAndRotation(x, y, z, yaw, pitch);
    }

    @Inject(method="handleSetSlot", at=@At("HEAD"))
    public void handleSetSlot(S2FPacketSetSlot packetIn, CallbackInfo ci) {
        EnchantingSolvers.processInventoryContents();
    }

    @Inject(method="handleBlockChange", at=@At("HEAD"))
    public void handleBlockChange(S23PacketBlockChange packetIn, CallbackInfo ci) {
        MiningStuff.processBlockChangePacket(packetIn);
    }



}
