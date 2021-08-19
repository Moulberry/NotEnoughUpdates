package io.github.moulberry.notenoughupdates.miscfeatures;

import io.github.moulberry.notenoughupdates.core.util.render.RenderUtils;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.util.*;

import java.util.ArrayList;
import java.util.List;

public class CrystalMetalDetectorSolver {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static BlockPos prevPos;
    private static float prevDist = 0;
    private static List<BlockPos> possibleBlocks = new ArrayList<>();
    private static final List<BlockPos> locations = new ArrayList<>();

    public static void process(IChatComponent message) {
        if (SBInfo.getInstance().getLocation() != null && SBInfo.getInstance().getLocation().equals("crystal_hollows")
                && message.getUnformattedText().contains("TREASURE: ")) {
            float dist = Float.parseFloat(message.getUnformattedText().split("TREASURE: ")[1].split("m")[0].replaceAll("(?!\\.)\\D", ""));
            if (prevDist == dist && prevPos.getX() == mc.thePlayer.getPosition().getX() && prevPos.getY() == mc.thePlayer.getPosition().getY() &&
                    prevPos.getZ() == mc.thePlayer.getPosition().getZ() && !locations.contains(mc.thePlayer.getPosition())) {
                if (possibleBlocks.size() == 0) {
                    locations.add(mc.thePlayer.getPosition());
                    for (int zOffset = (int) Math.floor(-dist); zOffset <= Math.ceil(dist); zOffset++) {
                        for (int yOffset = 65; yOffset <= 69; yOffset++) {
                            float calculatedDist = 0;
                            int xOffset = 0;
                            while (calculatedDist < dist) {
                                BlockPos pos = new BlockPos(Math.floor(mc.thePlayer.posX) + xOffset,
                                        yOffset, Math.floor(mc.thePlayer.posZ) + zOffset);
                                BlockPos above = new BlockPos(Math.floor(mc.thePlayer.posX) + xOffset,
                                        yOffset + 1, Math.floor(mc.thePlayer.posZ) + zOffset);
                                xOffset++;
                                calculatedDist = round(calculateDistance(new Vec3(pos).addVector(0, 1d, 0), new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)), 1);
                                if (calculatedDist == dist && treasureAllowed(pos) && !possibleBlocks.contains(pos) &&
                                        mc.theWorld.getBlockState(above).getBlock().getRegistryName().equals("minecraft:air")) {
                                    possibleBlocks.add(pos);
                                }
                                xOffset++;
                            }
                            xOffset = 0;
                            calculatedDist = 0;
                            while (calculatedDist < dist) {
                                BlockPos pos = new BlockPos(Math.floor(mc.thePlayer.posX) - xOffset,
                                        yOffset, Math.floor(mc.thePlayer.posZ) + zOffset);
                                BlockPos above = new BlockPos(Math.floor(mc.thePlayer.posX) - xOffset,
                                        yOffset + 1, Math.floor(mc.thePlayer.posZ) + zOffset);
                                calculatedDist = round(calculateDistance(new Vec3(pos).addVector(0, 1d, 0), new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)), 1);
                                if (calculatedDist == dist && treasureAllowed(pos) && !possibleBlocks.contains(pos) &&
                                        mc.theWorld.getBlockState(above).getBlock().getRegistryName().equals("minecraft:air")) {
                                    possibleBlocks.add(pos);
                                }
                                xOffset++;
                            }
                        }
                    }
                    if (possibleBlocks.size() == 1) possibleBlocks.clear(); //protection from completely wrong things
                    sendMessage();
                } else if (possibleBlocks.size() != 1) {
                    locations.add(mc.thePlayer.getPosition());
                    List<BlockPos> temp = new ArrayList<>();
                    for (BlockPos pos : possibleBlocks) {
                        if (round(calculateDistance(new Vec3(pos).addVector(0, 1d, 0), new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)), 1) == dist) {
                            temp.add(pos);
                        }
                    }
                    possibleBlocks = temp;
                    sendMessage();
                }
            }
            prevPos = mc.thePlayer.getPosition();
            prevDist = dist;
        }
    }

    public static void reset() {
        possibleBlocks.clear();
        locations.clear();
    }

    public static void render(float partialTicks) {
        if (SBInfo.getInstance().getLocation() != null && SBInfo.getInstance().getLocation().equals("crystal_hollows")) {
            if (possibleBlocks.size() == 1) {
                RenderUtils.renderWayPoint("Treasure", possibleBlocks.get(0).add(0, 2.5, 0), partialTicks);
            } else {
                for (BlockPos block : possibleBlocks) {
                    RenderUtils.renderWayPoint("Possible Treasure Location", block.add(0, 2.5, 0), partialTicks);
                }
            }
        }
    }

    private static float round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (float) Math.round(value * scale) / scale;
    }

    private static boolean treasureAllowed(BlockPos pos) {
        return mc.theWorld.getBlockState(pos).getBlock().getRegistryName().equals("minecraft:gold_block") ||
                mc.theWorld.getBlockState(pos).getBlock().getRegistryName().equals("minecraft:prismarine") ||
                mc.theWorld.getBlockState(pos).getBlock().getRegistryName().equals("minecraft:chest") ||
                mc.theWorld.getBlockState(pos).getBlock().getRegistryName().equals("minecraft:stained_glass") ||
                mc.theWorld.getBlockState(pos).getBlock().getRegistryName().equals("minecraft:stained_glass_pane") ||
                mc.theWorld.getBlockState(pos).getBlock().getRegistryName().equals("minecraft:wool") ||
                mc.theWorld.getBlockState(pos).getBlock().getRegistryName().equals("minecraft:stained_hardened_clay");
    }

    private static void sendMessage() {
        if (possibleBlocks.size() > 1) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[NEU] Need another position to find solution. Possible blocks: "
                    + possibleBlocks.size()));
        } else if (possibleBlocks.size() == 0) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[NEU] Failed to find solution."));
            reset();
        } else {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[NEU] Found solution."));
        }
    }

    private static float calculateDistance(Vec3 pos1, Vec3 pos2) {
        return (float) Math.sqrt(Math.pow(pos2.xCoord - pos1.xCoord, 2) + Math.pow(pos2.yCoord - pos1.yCoord, 2) + Math.pow(pos2.zCoord - pos1.zCoord, 2));
    }
}
