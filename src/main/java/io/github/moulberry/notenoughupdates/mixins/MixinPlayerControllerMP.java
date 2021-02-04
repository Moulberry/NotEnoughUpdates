package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.miscfeatures.DwarvenMinesTextures;
import io.github.moulberry.notenoughupdates.miscfeatures.FairySouls;
import io.github.moulberry.notenoughupdates.miscfeatures.ItemCooldowns;
import io.github.moulberry.notenoughupdates.miscfeatures.MiningStuff;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    @Inject(method="clickBlock", at=@At("HEAD"), cancellable = true)
    public void clickBlock(BlockPos loc, EnumFacing face, CallbackInfoReturnable<Boolean> cir) {
        ItemCooldowns.blockClicked(loc);
        if(MiningStuff.blockClicked(loc)) {
            cir.setReturnValue(false);
            ((PlayerControllerMP)(Object)this).resetBlockRemoving();
        }
    }

}
