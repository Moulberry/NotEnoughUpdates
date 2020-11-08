package io.github.moulberry.notenoughupdates;

import io.github.moulberry.morus.MorusSubstitutor;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class MorusIntegration {

    private static MorusIntegration INSTANCE = new MorusIntegration();

    public static MorusIntegration getInstance() {
        return INSTANCE;
    }

    private HashMap<String, Integer> itemDrops = null;
    private HashMap<String, Integer> inventoryItems = null;

    public void tick() {
        if(itemDrops == null) {
            itemDrops = new HashMap<>();
            for(String item : NotEnoughUpdates.INSTANCE.manager.getItemInformation().keySet()) {
                itemDrops.put(item, 0);
            }
        }

        HashMap<String, Integer> newInventoryItems = getInventoryItems();
        if(inventoryItems != null) {
            for(String internal : newInventoryItems.keySet()) {
                int newAmount = newInventoryItems.get(internal);
                int oldAmount = inventoryItems.getOrDefault(internal, 0);
                if(newAmount > oldAmount) {
                    itemDrops.put(internal, itemDrops.getOrDefault(internal, 0)+newAmount-oldAmount);
                }
            }
        }
        inventoryItems = newInventoryItems;

        for(Map.Entry<String, Integer> entry : itemDrops.entrySet()) {
            MorusSubstitutor.putSubstiution("notenoughupdates", "itemdrops."+entry.getKey().toLowerCase(), ""+entry.getValue());
        }

    }

    public HashMap<String, Integer> getInventoryItems() {
        HashMap<String, Integer> inventoryItems = new HashMap<>();
        if(Minecraft.getMinecraft().thePlayer != null) {
            for(ItemStack stack : Minecraft.getMinecraft().thePlayer.inventory.mainInventory) {
                String internalname = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(stack);
                if(internalname != null) {
                    inventoryItems.put(internalname, inventoryItems.getOrDefault(internalname, 0)+stack.stackSize);
                }
            }
        }
        return inventoryItems;
    }

}
