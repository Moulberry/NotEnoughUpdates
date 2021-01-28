package io.github.moulberry.notenoughupdates.overlays;

import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.core.util.lerp.LerpUtils;
import io.github.moulberry.notenoughupdates.util.XPInformation;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.function.Supplier;

public class FarmingOverlay extends TextOverlay {

    private long lastUpdate = -1;
    private int counterLast = -1;
    private int counter = -1;
    private float cropsPerSecondLast = 0;
    private float cropsPerSecond = 0;
    private LinkedList<Integer> counterQueue = new LinkedList<>();

    private float lastTotalXp = -1;
    private boolean isFarming = false;
    private LinkedList<Float> xpGainQueue = new LinkedList<>();
    private float xpGainHourLast = -1;
    private float xpGainHour = -1;

    public FarmingOverlay(Position position, Supplier<TextOverlayStyle> styleSupplier) {
        super(position, styleSupplier);
    }

    @Override
    public void update() {
        lastUpdate = System.currentTimeMillis();
        counterLast = counter;
        xpGainHourLast = xpGainHour;
        counter = -1;

        XPInformation.SkillInfo skillInfo = XPInformation.getInstance().getSkillInfo("Farming");
        if(skillInfo != null) {
            float totalXp = skillInfo.totalXp;

            if(lastTotalXp > 0) {
                float delta = totalXp - lastTotalXp;

                if(delta > 0 && delta < 1000) {
                    xpGainQueue.add(delta);
                    while (xpGainQueue.size() > 120) {
                        xpGainQueue.removeLast();
                    }

                    float totalGain = 0;
                    for(float f : xpGainQueue) totalGain += f;

                    xpGainHour = totalGain*(60*60)/xpGainQueue.size();

                    isFarming = true;
                } else if(delta <= 0) {
                    isFarming = false;
                }
            }

            lastTotalXp = totalXp;
        }

        if(Minecraft.getMinecraft().thePlayer == null) return;

        ItemStack stack = Minecraft.getMinecraft().thePlayer.getHeldItem();
        if(stack != null && stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();

            if(tag.hasKey("ExtraAttributes", 10)) {
                NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");

                if(ea.hasKey("mined_crops", 99)) {
                    counter = ea.getInteger("mined_crops");
                    counterQueue.add(0, counter);
                }
            }
        }

        while(counterQueue.size() >= 4) {
            counterQueue.removeLast();
        }

        if(counterQueue.isEmpty()) {
            cropsPerSecond = -1;
            cropsPerSecondLast = 0;
        } else {
            cropsPerSecondLast = cropsPerSecond;
            int last = counterQueue.getLast();
            int first = counterQueue.getFirst();

            cropsPerSecond = (first - last)/3f;
        }

        if(counter != -1) {
            overlayStrings = new ArrayList<>();
        } else {
            overlayStrings = null;
        }

    }

    @Override
    public void updateFrequent() {
        if(counter < 0) {
            overlayStrings = null;
        } else {
            overlayStrings = new ArrayList<>();

            int counterInterp = counter;
            if(counterLast > 0 && counterLast != counter) {
                float factor = (System.currentTimeMillis()-lastUpdate)/1000f;
                factor = LerpUtils.clampZeroOne(factor);
                counterInterp = (int)(counterLast + (counter - counterLast) * factor);
            }

            NumberFormat format = NumberFormat.getIntegerInstance();
            overlayStrings.add(EnumChatFormatting.AQUA+"Counter: "+EnumChatFormatting.YELLOW+format.format(counterInterp));

            if(cropsPerSecondLast == cropsPerSecond && cropsPerSecond <= 0) {
                overlayStrings.add(EnumChatFormatting.AQUA+"Crops/m: "+EnumChatFormatting.YELLOW+"N/A");
            } else {
                float cpsInterp = cropsPerSecond;
                if(cropsPerSecondLast >= 0 && cropsPerSecondLast != cropsPerSecond) {
                    float factor = (System.currentTimeMillis()-lastUpdate)/1000f;
                    factor = LerpUtils.clampZeroOne(factor);
                    cpsInterp = cropsPerSecondLast + (cropsPerSecond - cropsPerSecondLast) * factor;
                }

                overlayStrings.add(EnumChatFormatting.AQUA+"Crops/m: "+EnumChatFormatting.YELLOW+
                        String.format("%.2f", cpsInterp*60));
            }

            XPInformation.SkillInfo skillInfo = XPInformation.getInstance().getSkillInfo("Farming");
            if(skillInfo != null) {
                StringBuilder levelStr = new StringBuilder(EnumChatFormatting.AQUA + "Level: ");

                levelStr.append(EnumChatFormatting.YELLOW)
                        .append(skillInfo.level)
                        .append(EnumChatFormatting.GRAY)
                        .append(" [");

                float progress = skillInfo.currentXp / skillInfo.currentXpMax;

                float lines = 25;
                for(int i=0; i<lines; i++) {
                    if(i/lines < progress) {
                        levelStr.append(EnumChatFormatting.YELLOW);
                    } else {
                        levelStr.append(EnumChatFormatting.DARK_GRAY);
                    }
                    levelStr.append('|');
                }

                levelStr.append(EnumChatFormatting.GRAY)
                        .append("] ")
                        .append(EnumChatFormatting.YELLOW)
                        .append((int)(progress*100))
                        .append("%");

                overlayStrings.add(levelStr.toString());
                overlayStrings.add(EnumChatFormatting.AQUA+"Current XP: " + EnumChatFormatting.YELLOW+
                        format.format((int)skillInfo.currentXp));
                overlayStrings.add(EnumChatFormatting.AQUA+"Remaining XP: " + EnumChatFormatting.YELLOW+
                        format.format((int)(skillInfo.currentXpMax - skillInfo.currentXp)));
            }

            if(xpGainHourLast == xpGainHour && xpGainHour <= 0) {
                overlayStrings.add(EnumChatFormatting.AQUA+"XP/h: "+EnumChatFormatting.YELLOW+"N/A");
            } else {
                float xpInterp = xpGainHour;
                if(xpGainHourLast >= 0 && cropsPerSecondLast != xpGainHour) {
                    float factor = (System.currentTimeMillis()-lastUpdate)/1000f;
                    factor = LerpUtils.clampZeroOne(factor);
                    xpInterp = xpGainHourLast + (xpGainHour - xpGainHourLast) * factor;
                }

                overlayStrings.add(EnumChatFormatting.AQUA+"XP/h: "+EnumChatFormatting.YELLOW+
                        format.format(xpInterp)+(isFarming ? "" : EnumChatFormatting.RED + " (PAUSED)"));
            }

            float yaw = Minecraft.getMinecraft().thePlayer.rotationYawHead;
            yaw %= 360;
            if(yaw < 0) yaw += 360;
            if(yaw > 180) yaw -= 360;

            overlayStrings.add(EnumChatFormatting.AQUA+"Yaw: "+EnumChatFormatting.YELLOW+
                    String.format("%.2f", yaw)+EnumChatFormatting.BOLD+"\u1D52");
        }
    }


}
