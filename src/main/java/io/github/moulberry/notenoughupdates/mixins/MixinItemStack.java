package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemStack.class})
public class MixinItemStack {

    @Inject(method="hasEffect", at=@At("HEAD"), cancellable = true)
    public void hasEffect(CallbackInfoReturnable cir) {
        if(Utils.getHasEffectOverride()) {
            cir.setReturnValue(false);
        }
    }

}
