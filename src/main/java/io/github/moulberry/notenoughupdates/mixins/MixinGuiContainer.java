package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.BetterContainers;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer {

    @Inject(method="drawSlot", at=@At("HEAD"), cancellable = true)
    public void drawSlot(Slot slot, CallbackInfo ci) {
        if(slot != null && BetterContainers.isOverriding() && BetterContainers.isBlankStack(slot.getStack())) {
            ci.cancel();
        }
    }

    private static final String TARGET_CANBEHOVERED = "Lnet/minecraft/inventory/Slot;canBeHovered()Z";
    @Redirect(method="drawScreen", at=@At(value="INVOKE", target=TARGET_CANBEHOVERED))
    public boolean drawScreen_canBeHovered(Slot slot) {
        if(BetterContainers.isBlankStack(slot.getStack())) {
            return false;
        }
        return slot.canBeHovered();
    }

    @Inject(method="handleMouseClick", at=@At(value="HEAD"), cancellable = true)
    public void handleMouseClick(Slot slotIn, int slotId, int clickedButton, int clickType, CallbackInfo ci) {
        if(slotIn != null && BetterContainers.isOverriding() && (BetterContainers.isBlankStack(slotIn.getStack()) ||
                BetterContainers.isButtonStack(slotIn.getStack()))) {
            BetterContainers.clickSlot(slotIn.getSlotIndex());
            Utils.playPressSound();

            GuiContainer $this = (GuiContainer)(Object)this;
            $this.mc.playerController.windowClick($this.inventorySlots.windowId, slotId, 2, clickType, $this.mc.thePlayer);
            ci.cancel();
        }
    }

}
