package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.miscgui.InventoryStorageSelector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemRenderer;
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

    @Shadow private ItemStack itemToRender;

    @Redirect(method="renderItemInFirstPerson", at=@At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/ItemRenderer;itemToRender:Lnet/minecraft/item/ItemStack;",
            opcode = Opcodes.GETFIELD
    ))
    public ItemStack modifyStackToRender(ItemRenderer renderer) {
        if(InventoryStorageSelector.getInstance().isSlotSelected()) {
            return InventoryStorageSelector.getInstance().getHeldItemOverride();
        }
        return itemToRender;
    }

}
