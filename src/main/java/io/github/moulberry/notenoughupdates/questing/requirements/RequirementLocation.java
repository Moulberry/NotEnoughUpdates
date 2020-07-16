package io.github.moulberry.notenoughupdates.questing.requirements;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class RequirementLocation extends Requirement {

    private int x;
    private int y;
    private int z;
    private float radius;

    public RequirementLocation(int x, int y, int z, float radius, Requirement... preconditions) {
        super(preconditions);
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
    }

    @Override
    public void updateRequirement() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        completed = player.getDistance(x, y, z) < radius;
    }
}
