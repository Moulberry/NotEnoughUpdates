package io.github.moulberry.notenoughupdates.questing.requirements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;

public class RequirementGuiOpen extends Requirement {

    private String guiName;

    public RequirementGuiOpen(String guiName, Requirement... preconditions) {
        super(preconditions);
        this.guiName = guiName;
    }

    @Override
    public void updateRequirement() {
        if(Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
            GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            String containerName = container.getLowerChestInventory().getDisplayName().getUnformattedText();

            if(containerName.equals(guiName)) {
                completed = true;
            }
        }
    }
}
