package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.miscgui.InventoryStorageSelector;
import net.minecraft.entity.player.InventoryPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryPlayer.class)
public class MixinInventoryPlayer {

    @Inject(method="changeCurrentItem", at=@At("RETURN"))
    public void changeCurrentItem(int direction, CallbackInfo ci) {
        InventoryPlayer $this = (InventoryPlayer)(Object)this;

        $this.currentItem = InventoryStorageSelector.getInstance().onScroll(direction, $this.currentItem);
    }

}
