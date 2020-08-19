package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.CustomItemEffects;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.StreamerMode;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    private static final String TARGET = "Lnet/minecraft/entity/player/EntityPlayer;" +
            "setPositionAndRotation(DDDFF)V";
    @Redirect(method="handlePlayerPosLook", at=@At(value="INVOKE", target=TARGET))
    public void handlePlayerPosLook_setPositionAndRotation(EntityPlayer player, double x, double y, double z, float yaw, float pitch) {
        if(CustomItemEffects.INSTANCE.aoteTeleportationCurr != null) {
            CustomItemEffects.INSTANCE.aoteTeleportationMillis += 175;
        }
        player.setPositionAndRotation(x, y, z, yaw, pitch);
    }

}
