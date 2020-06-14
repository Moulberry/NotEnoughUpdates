package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.Utils;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin({InventoryEffectRenderer.class})
public class MixinInventoryEffectRenderer {

    @ModifyVariable(method="updateActivePotionEffects", at=@At(value="STORE"))
    public boolean hasVisibleEffect_updateActivePotionEffects(boolean hasVisibleEffect) {
        if(NotEnoughUpdates.INSTANCE.manager.config.hidePotionEffect.value &&
                NotEnoughUpdates.INSTANCE.isOnSkyblock()) {
            return false;
        } else {
            return hasVisibleEffect;
        }
    }

}
