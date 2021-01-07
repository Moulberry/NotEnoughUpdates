package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.miscfeatures.ItemRarityHalo;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RenderItem.class})
public abstract class MixinRenderItem {

    //Lnet/minecraft/client/renderer/entity/RenderItem;renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/resources/model/IBakedModel;)V
    @Inject(method="Lnet/minecraft/client/renderer/entity/RenderItem;renderItemIntoGUI(Lnet/minecraft/item/ItemStack;II)V",
            at=@At("HEAD"), cancellable = true)
    public void renderItemIntoGUI(ItemStack stack, int x, int y, CallbackInfo ci) {
        if(x == 0 && y == 0 || true) return;
        ItemRarityHalo.onItemRender(stack, x, y);

        if(Keyboard.isKeyDown(Keyboard.KEY_H)) ci.cancel();
    }
}
