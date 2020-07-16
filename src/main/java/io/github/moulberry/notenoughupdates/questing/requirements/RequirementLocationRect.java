package io.github.moulberry.notenoughupdates.questing.requirements;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class RequirementLocationRect extends Requirement {

    private int x1;
    private int y1;
    private int z1;
    private int x2;
    private int y2;
    private int z2;

    public RequirementLocationRect(int x1, int y1, int z1, int x2, int y2, int z2, Requirement... preconditions) {
        super(preconditions);
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.z1 = Math.min(z1, z2);

        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.z2 = Math.max(z1, z2);
    }

    @Override
    public void updateRequirement() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        completed = true;
        completed &= player.posX > x1 && player.posX < x2;
        completed &= player.posY > y1 && player.posY < y2;
        completed &= player.posZ > z1 && player.posZ < z2;
    }
}
