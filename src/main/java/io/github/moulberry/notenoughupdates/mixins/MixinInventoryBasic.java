/*
 * Copyright (C) 2022 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryBasic.class)
public abstract class MixinInventoryBasic {
	@Shadow
	private ItemStack[] inventoryContents;

	@Shadow
	public abstract IChatComponent getDisplayName();

	@Inject(method = "getStackInSlot", at = @At("HEAD"), cancellable = true)
	public void on(int index, CallbackInfoReturnable<ItemStack> cir) {
		ReplaceItemEvent replaceItemEvent = new ReplaceItemEvent(
			index >= 0 && index < this.inventoryContents.length ? this.inventoryContents[index] : null,
			((InventoryBasic) (Object) this),
			index
		);
		replaceItemEvent.post();
		if (replaceItemEvent.getReplacement() != replaceItemEvent.getOriginal()) {
			cir.setReturnValue(replaceItemEvent.getReplacement());
		}
	}
}
