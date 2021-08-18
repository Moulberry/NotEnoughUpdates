package io.github.moulberry.notenoughupdates.miscfeatures;

import io.github.moulberry.notenoughupdates.core.util.render.RenderUtils;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class CrystalMetalDetectorSolver {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static float prev;
    private static List<BlockPos> possibleBlocks = new ArrayList<>();
    private static final List<Float> distances = new ArrayList<>();

    public static void process(IChatComponent message) {
        if (SBInfo.getInstance().getLocation() != null && SBInfo.getInstance().getLocation().equals("crystal_hollows")
                && message.getUnformattedText().contains("TREASURE: ")) {
            float dist = Float.parseFloat(message.getUnformattedText().split("TREASURE: ")[1].split("m")[0].replaceAll("(?!\\.)\\D", ""));
            if (prev == dist && dist > 5 && !distances.contains(dist) && possibleBlocks.size() != 1) { //Distance 5 minimum because distance calculation is inaccurate under 5
                distances.add(dist);
                List<BlockPos> temp = new ArrayList<>();
                for (int zOffset = Math.round(-dist); zOffset <= dist; zOffset++) {
                    float calculatedDist = 0;
                    int xOffset = 0;
                    int yOffset = 0;
                    int loops = 0;
                    while (calculatedDist < dist) {
                        loops++;
                        BlockPos pos = new BlockPos(Math.floor(mc.thePlayer.posX) + xOffset,
                                Math.floor(mc.thePlayer.posY - 1) + yOffset, Math.floor(mc.thePlayer.posZ) + zOffset);
                        BlockPos above = new BlockPos(Math.floor(mc.thePlayer.posX) + xOffset,
                                Math.floor(mc.thePlayer.posY) + yOffset, Math.floor(mc.thePlayer.posZ) + zOffset);
                        if (mc.theWorld.getBlockState(pos).getBlock().getRegistryName().equals("minecraft:air")) {
                            yOffset--;
                            System.out.println("-" + pos + "        " + yOffset + "        " + mc.theWorld.getBlockState(pos).getBlock().getRegistryName() +  "     " +
                                    mc.theWorld.getBlockState(above).getBlock().getRegistryName());
                        } else if (mc.theWorld.getBlockState(above).getBlock().getRegistryName().equals("minecraft:gold_block")) {
                            yOffset++;
                            System.out.println("+" + pos + "        " + yOffset + "        " + mc.theWorld.getBlockState(pos).getBlock().getRegistryName() +  "     " +
                                    mc.theWorld.getBlockState(above).getBlock().getRegistryName());
                        } else {
                            System.out.println("Calculating block distance");
                            xOffset++;
                            calculatedDist = (float) round(mc.thePlayer.getDistance(pos.getX(), pos.getY() + 1, pos.getZ()), 1);
                            if (calculatedDist == dist && (mc.theWorld.getBlockState(pos).getBlock().getRegistryName().equals("minecraft:gold_block")
                                    || mc.theWorld.getBlockState(pos).getBlock().getRegistryName().equals("minecraft:chest")) &&
                                    (possibleBlocks.size() == 0 || possibleBlocks.contains(pos))) {
                                temp.add(pos);
                            }
                        }
                        if (loops > 500) break;
                    }
                    xOffset = 0;
                    calculatedDist = 0;
                    yOffset = 0;
                    loops = 0;
                    while (calculatedDist < dist) {
                        loops++;
                        BlockPos pos = new BlockPos(Math.floor(mc.thePlayer.posX) - xOffset,
                                Math.floor(mc.thePlayer.posY - 1) + yOffset, Math.floor(mc.thePlayer.posZ) + zOffset);
                        BlockPos above = new BlockPos(Math.floor(mc.thePlayer.posX) + xOffset,
                                Math.floor(mc.thePlayer.posY) + yOffset, Math.floor(mc.thePlayer.posZ) + zOffset);
                        if (mc.theWorld.getBlockState(pos).getBlock().getRegistryName().equals("minecraft:air")) {
                            yOffset--;
                            System.out.println("-" + pos + "        " + yOffset + "        " + mc.theWorld.getBlockState(pos).getBlock().getRegistryName() +  "     " +
                                    mc.theWorld.getBlockState(above).getBlock().getRegistryName());
                        } else if (mc.theWorld.getBlockState(above).getBlock().getRegistryName().equals("minecraft:gold_block")) {
                            yOffset++;
                            System.out.println("+" + pos + "        " + yOffset + "        " + mc.theWorld.getBlockState(pos).getBlock().getRegistryName() +  "     " +
                                    mc.theWorld.getBlockState(above).getBlock().getRegistryName());
                        } else {
                            System.out.println("Calculating block distance");
                            xOffset++;
                            calculatedDist = (float) round(mc.thePlayer.getDistance(pos.getX(), pos.getY() + 1, pos.getZ()), 1);
                            if (calculatedDist == dist && (mc.theWorld.getBlockState(pos).getBlock().getRegistryName().equals("minecraft:gold_block")
                                    || mc.theWorld.getBlockState(pos).getBlock().getRegistryName().equals("minecraft:chest")) &&
                                    (possibleBlocks.size() == 0 || possibleBlocks.contains(pos))) {
                                temp.add(pos);
                            }
                        }
                        if (loops > 500) break;
                    }
                }
                possibleBlocks = temp;
                System.out.println(possibleBlocks);
                if (possibleBlocks.size() > 1) {
                    mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[NEU] Need another position to find solution. Possible blocks: "
                            + possibleBlocks.size()));
                } else if (possibleBlocks.size() == 0) {
                    mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[NEU] Failed to find solution."));
                } else {
                    mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[NEU] Found solution."));
                }
            }
            prev = dist;
        }
    }

    public static void reset() {
        possibleBlocks.clear();
        distances.clear();
    }

    public static void render(float partialTicks){
        if (SBInfo.getInstance().getLocation() != null && SBInfo.getInstance().getLocation().equals("crystal_hollows") && possibleBlocks.size() == 1){
            RenderUtils.renderWayPoint("Treasure", possibleBlocks.get(0), partialTicks);
        }
    }

    private static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }
}
