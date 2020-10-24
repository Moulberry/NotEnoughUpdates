package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.BetterContainers;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
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
        if(slot == null) return;

        GuiContainer $this = (GuiContainer)(Object)this;
        ItemStack stack = slot.getStack();

        if(stack == null && System.currentTimeMillis() - BetterContainers.lastRenderMillis < 300 && $this instanceof GuiChest) {
            Container container = ((GuiChest)$this).inventorySlots;
            if(container instanceof ContainerChest) {
                IInventory lower = ((ContainerChest)container).getLowerChestInventory();
                int size = lower.getSizeInventory();
                if(slot.slotNumber < size) {
                    boolean found = false;
                    for(int index=0; index<size; index++) {
                        if(lower.getStackInSlot(index) != null) {
                            found = true;
                            break;
                        }
                    }
                    if(!found) {
                        stack = BetterContainers.itemCache.get(slot.slotNumber);
                    }
                }

            }
        }

        if(BetterContainers.isOverriding() && !BetterContainers.shouldRenderStack(stack)) {
            ci.cancel();
        }
    }

    private static final String TARGET_GETSTACK = "Lnet/minecraft/inventory/Slot;getStack()Lnet/minecraft/item/ItemStack;";
    @Redirect(method="drawSlot", at=@At(value="INVOKE", target=TARGET_GETSTACK))
    public ItemStack drawSlot_getStack(Slot slot) {
        GuiContainer $this = (GuiContainer)(Object)this;
        ItemStack stack = slot.getStack();

        if($this instanceof GuiChest) {
            Container container = ((GuiChest)$this).inventorySlots;
            if(container instanceof ContainerChest) {
                IInventory lower = ((ContainerChest)container).getLowerChestInventory();
                int size = lower.getSizeInventory();
                if(slot.slotNumber >= size) {
                    return stack;
                }
                if(System.currentTimeMillis() - BetterContainers.lastRenderMillis < 300 && stack == null) {
                    for(int index=0; index<size; index++) {
                        if(lower.getStackInSlot(index) != null) {
                            BetterContainers.itemCache.put(slot.slotNumber, null);
                            return null;
                        }
                    }
                    return BetterContainers.itemCache.get(slot.slotNumber);
                } else {
                    BetterContainers.itemCache.put(slot.slotNumber, stack);
                }
            }
        }
        return stack;
    }

    private static final String TARGET_CANBEHOVERED = "Lnet/minecraft/inventory/Slot;canBeHovered()Z";
    @Redirect(method="drawScreen", at=@At(value="INVOKE", target=TARGET_CANBEHOVERED))
    public boolean drawScreen_canBeHovered(Slot slot) {
        if(NotEnoughUpdates.INSTANCE.manager.config.hideEmptyPanes.value &&
                BetterContainers.isOverriding() && BetterContainers.isBlankStack(slot.getStack())) {
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

            if(BetterContainers.isBlankStack(slotIn.getStack())) {
                GuiContainer $this = (GuiContainer)(Object)this;
                $this.mc.playerController.windowClick($this.inventorySlots.windowId, slotId, 2, clickType, $this.mc.thePlayer);
                ci.cancel();
            }
        }
    }

}
