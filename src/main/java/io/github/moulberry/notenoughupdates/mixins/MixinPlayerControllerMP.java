package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.miscfeatures.FairySouls;
import io.github.moulberry.notenoughupdates.miscfeatures.MiningStuff;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    @Inject(method="clickBlock", at=@At("HEAD"))
    public void clickBlock(BlockPos loc, EnumFacing face, CallbackInfoReturnable<Boolean> cir) {
        MiningStuff.blockClicked(loc);
    }

}
