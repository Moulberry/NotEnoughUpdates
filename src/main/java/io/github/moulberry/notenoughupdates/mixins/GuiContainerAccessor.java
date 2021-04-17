package io.github.moulberry.notenoughupdates.mixins;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiContainer.class)
public interface GuiContainerAccessor {

    @Accessor
    int getXSize();

    @Accessor
    int getYSize();

    @Accessor
    int getGuiLeft();

    @Accessor
    int getGuiTop();

}
