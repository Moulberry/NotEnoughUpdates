package io.github.moulberry.notenoughupdates.miscfeatures;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.util.render.RenderUtils;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.util.*;

import java.util.ArrayList;
import java.util.List;

public class CrystalMetalDetectorSolver {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static BlockPos prevPlayerPos;
    private static double prevDistToTreasure = 0;
    private static List<BlockPos> possibleBlocks = new ArrayList<>();
    private static final List<BlockPos> locations = new ArrayList<>();

    private static Boolean chestRecentlyFound = false;
    private static long chestLastFoundMillis = 0;

    public static void process(IChatComponent message) {
        // Delay to keep old chest location from being treated as the new chest location
        if (chestRecentlyFound) {
            long currentTimeMillis = System.currentTimeMillis();
            if (chestLastFoundMillis == 0) {
                chestLastFoundMillis = currentTimeMillis;
                return;
            } else if(currentTimeMillis - chestLastFoundMillis < 1000) {
                return;
            }

            chestLastFoundMillis = 0;
            chestRecentlyFound = false;
        }

        if (SBInfo.getInstance().getLocation() != null && SBInfo.getInstance().getLocation().equals("crystal_hollows")
                && message.getUnformattedText().contains("TREASURE: ")) {
            double distToTreasure = Double.parseDouble(message.getUnformattedText().split("TREASURE: ")[1].split("m")[0].replaceAll("(?!\\.)\\D", ""));
            if (NotEnoughUpdates.INSTANCE.config.mining.metalDetectorEnabled && prevDistToTreasure == distToTreasure &&
                    prevPlayerPos.getX() == mc.thePlayer.getPosition().getX() &&
                    prevPlayerPos.getY() == mc.thePlayer.getPosition().getY() &&
                    prevPlayerPos.getZ() == mc.thePlayer.getPosition().getZ() && !locations.contains(mc.thePlayer.getPosition())) {
                if (possibleBlocks.size() == 0) {
                    locations.add(mc.thePlayer.getPosition());
                    for (int zOffset = (int) Math.floor(-distToTreasure); zOffset <= Math.ceil(distToTreasure); zOffset++) {
                        for (int y = 65; y <= 75; y++) {
                            double calculatedDist = 0;
                            int xOffset = 0;
                            while (calculatedDist < distToTreasure) {
                                BlockPos pos = new BlockPos(Math.floor(mc.thePlayer.posX) + xOffset,
                                        y, Math.floor(mc.thePlayer.posZ) + zOffset);
                                calculatedDist = getPlayerPos().distanceTo(new Vec3(pos).addVector(0D, 1D, 0D));
                                if (round(calculatedDist, 1) == distToTreasure && !possibleBlocks.contains(pos)) {
                                    possibleBlocks.add(pos);
                                }
                                xOffset++;
                            }
                            xOffset = 0;
                            calculatedDist = 0;
                            while (calculatedDist < distToTreasure) {
                                BlockPos pos = new BlockPos(Math.floor(mc.thePlayer.posX) - xOffset,
                                        y, Math.floor(mc.thePlayer.posZ) + zOffset);
                                calculatedDist = getPlayerPos().distanceTo(new Vec3(pos).addVector(0D, 1D, 0D));
                                if (round(calculatedDist, 1) == distToTreasure && !possibleBlocks.contains(pos)) {
                                    possibleBlocks.add(pos);
                                }
                                xOffset++;
                            }
                        }
                    }
                    removeDuplicates();
                    sendMessage();
                } else if (possibleBlocks.size() != 1) {
                    locations.add(mc.thePlayer.getPosition());
                    List<BlockPos> temp = new ArrayList<>();
                    for (BlockPos pos : possibleBlocks) {
                        if (round(getPlayerPos().distanceTo(new Vec3(pos).addVector(0D, 1D, 0D)), 1) == distToTreasure) {
                            temp.add(pos);
                        }
                    }
                    possibleBlocks = temp;
                    removeDuplicates();
                    sendMessage();
                } else if (possibleBlocks.size() == 1) {
                    BlockPos pos =  possibleBlocks.get(0);
                    if (distToTreasure > (5 + getPlayerPos().distanceTo(new Vec3(pos)))) {
                        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[NEU] Previous solution is invalid."));
                        reset(false);
                    }
                }

            }

            prevPlayerPos = mc.thePlayer.getPosition();
            prevDistToTreasure = distToTreasure;
        }
    }

    public static void reset(Boolean chestFound) {
        chestRecentlyFound = chestFound;
        possibleBlocks.clear();
        locations.clear();
    }

    public static void render(float partialTicks) {
        int beaconRGB = 0xa839ce;

        if (SBInfo.getInstance().getLocation() != null && SBInfo.getInstance().getLocation().equals("crystal_hollows") &&
                SBInfo.getInstance().location.equals("Mines of Divan")) {

            if (possibleBlocks.size() == 1) {
                BlockPos block = possibleBlocks.get(0);

                RenderUtils.renderBeaconBeam(block, beaconRGB, 1.0f, partialTicks);
                RenderUtils.renderWayPoint("Treasure", possibleBlocks.get(0).add(0, 2.5, 0), partialTicks);
            } else if (possibleBlocks.size() > 1 && (locations.size() > 1 || possibleBlocks.size() < 10) &&
                    NotEnoughUpdates.INSTANCE.config.mining.metalDetectorShowPossible) {
                for (BlockPos block : possibleBlocks) {
                    RenderUtils.renderBeaconBeam(block, beaconRGB, 1.0f, partialTicks);
                    RenderUtils.renderWayPoint("Possible Treasure Location", block.add(0, 2.5, 0), partialTicks);
                }
            }
        }
    }

    private static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    private static void removeDuplicates() {
        if (possibleBlocks.size() > 1 && possibleBlocks.size() < 10) {
            Vec3 firstBlockVec = new Vec3( possibleBlocks.get(0).getX(), 0,  possibleBlocks.get(0).getZ());
            Boolean allBlocksAreClose = true;

            double lowestY = possibleBlocks.get(0).getY();
            int lowestYIndex = 0;

            for (int i = 1; i < possibleBlocks.size(); i++) {
                BlockPos blockPos = possibleBlocks.get(i);
                Vec3 blockVec = new Vec3(blockPos.getX(), 0, blockPos.getZ());

                if (firstBlockVec.distanceTo(blockVec) > 3) {
                    allBlocksAreClose = false;
                    break;
                }

                if (blockPos.getY() < lowestY) {
                    lowestY = blockPos.getY();
                    lowestYIndex = i;
                }
            }

            if (allBlocksAreClose) {
                List<BlockPos> temp = new ArrayList<>();
                temp.add(possibleBlocks.get(lowestYIndex));
                possibleBlocks = temp;
            }
        }
    }

    private static void sendMessage() {
        if (possibleBlocks.size() > 1) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[NEU] Need another position to find solution. Possible blocks: "
                    + possibleBlocks.size()));
        } else if (possibleBlocks.size() == 0) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[NEU] Failed to find solution."));
            reset(false);
        } else {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[NEU] Found solution."));
        }
    }

    private static Vec3 getPlayerPos() {
        return new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + (mc.thePlayer.getEyeHeight() - mc.thePlayer.getDefaultEyeHeight()), mc.thePlayer.posZ);
    }
}
