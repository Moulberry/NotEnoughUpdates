package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.miscgui.InventoryStorageSelector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @Redirect(method="updateEquippedItem", at=@At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"
    ))
    public ItemStack modifyStackToRender(InventoryPlayer player) {
        if(InventoryStorageSelector.getInstance().isSlotSelected()) {
            return InventoryStorageSelector.getInstance().getHeldItemOverride();
        }
        return player.getCurrentItem();
    }

}
