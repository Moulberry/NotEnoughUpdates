package io.github.moulberry.notenoughupdates.overlays;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.core.util.lerp.LerpUtils;
import io.github.moulberry.notenoughupdates.util.Utils;
import io.github.moulberry.notenoughupdates.util.XPInformation;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class CombatSkillOverlay extends TextOverlay { //Im sure there is a much better way to do this besides making another class ¯\_(ツ)_/¯

    private long lastUpdate = -1;
    private LinkedList<Integer> compactQueue = new LinkedList<>();

    private XPInformation.SkillInfo skillInfo = null;
    private XPInformation.SkillInfo skillInfoLast = null;

    private float lastTotalXp = -1;
    private boolean isFite = false;
    private LinkedList<Float> xpGainQueue = new LinkedList<>();
    private float xpGainHourLast = -1;
    private float xpGainHour = -1;

    private int xpGainTimer = 0;

    private String skillType = "Combat";

    public CombatSkillOverlay(Position position, Supplier<List<String>> dummyStrings, Supplier<TextOverlayStyle> styleSupplier) {
        super(position, dummyStrings, styleSupplier);
    }

    private float interp(float now, float last) {
        float interp = now;
        if(last >= 0 && last != now) {
            float factor = (System.currentTimeMillis()-lastUpdate)/1000f;
            factor = LerpUtils.clampZeroOne(factor);
            interp = last + (now - last) * factor;
        }
        return interp;
    }

    @Override
    public void update() {
        if(!NotEnoughUpdates.INSTANCE.config.skillOverlays.combatSkillOverlay) {
            overlayStrings = null;
            return;
        }

        lastUpdate = System.currentTimeMillis();
        xpGainHourLast = xpGainHour;

        if(Minecraft.getMinecraft().thePlayer == null) return;

        ItemStack stack = Minecraft.getMinecraft().thePlayer.getHeldItem();

        String internalname = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(stack);

        skillInfoLast = skillInfo;
        skillInfo = XPInformation.getInstance().getSkillInfo(skillType);
        if(skillInfo != null) {
            float totalXp = skillInfo.totalXp;

            if(lastTotalXp > 0) {
                float delta = totalXp - lastTotalXp;

                if(delta > 0 && delta < 1000) {
                    xpGainTimer = 3;

                    xpGainQueue.add(0, delta);
                    while(xpGainQueue.size() > 30) {
                        xpGainQueue.removeLast();
                    }

                    float totalGain = 0;
                    for(float f : xpGainQueue) totalGain += f;

                    xpGainHour = totalGain * (60 * 60) / xpGainQueue.size();

                    isFite = true;
                } else if(xpGainTimer > 0) {
                    xpGainTimer--;

                    xpGainQueue.add(0, 0f);
                    while(xpGainQueue.size() > 30) {
                        xpGainQueue.removeLast();
                    }

                    float totalGain = 0;
                    for(float f : xpGainQueue) totalGain += f;

                    xpGainHour = totalGain * (60 * 60) / xpGainQueue.size();

                    isFite = true;
                } else if(delta <= 0) {
                    isFite = false;
                }
            }

            lastTotalXp = totalXp;
        }

        if(NotEnoughUpdates.INSTANCE.config.skillOverlays.combatSkillOverlay) {
            overlayStrings = new ArrayList<>();
        } else {
            overlayStrings = null;
        }

    }

    @Override
    public void updateFrequent() {
        super.updateFrequent();

        if(!NotEnoughUpdates.INSTANCE.config.skillOverlays.combatSkillOverlay) {
            overlayStrings = null;
        } else {
            HashMap<Integer, String> lineMap = new HashMap<>();

            overlayStrings = new ArrayList<>();

            NumberFormat format = NumberFormat.getIntegerInstance();

            float xpInterp = xpGainHour;
            if (xpGainHourLast == xpGainHour && xpGainHour <= 0) {
                lineMap.put(3, EnumChatFormatting.AQUA + "XP/h: " + EnumChatFormatting.YELLOW + "N/A");
            } else {
                xpInterp = interp(xpGainHour, xpGainHourLast);

                lineMap.put(3, EnumChatFormatting.AQUA + "XP/h: " + EnumChatFormatting.YELLOW +
                        format.format(xpInterp) + (isFite ? "" : EnumChatFormatting.RED + " (PAUSED)"));
            }

            if (skillInfo != null && skillInfo.level < 60) {
                StringBuilder levelStr = new StringBuilder(EnumChatFormatting.AQUA + "Combat" + ": ");
                levelStr.append(EnumChatFormatting.YELLOW)
                        .append(skillInfo.level)
                        .append(EnumChatFormatting.GRAY)
                        .append(" [");

                float progress = skillInfo.currentXp / skillInfo.currentXpMax;
                if (skillInfoLast != null && skillInfo.currentXpMax == skillInfoLast.currentXpMax) {
                    progress = interp(progress, skillInfoLast.currentXp / skillInfoLast.currentXpMax);
                }

                float lines = 25;
                for (int i = 0; i < lines; i++) {
                    if (i / lines < progress) {
                        levelStr.append(EnumChatFormatting.YELLOW);
                    } else {
                        levelStr.append(EnumChatFormatting.DARK_GRAY);
                    }
                    levelStr.append('|');
                }

                levelStr.append(EnumChatFormatting.GRAY)
                        .append("] ")
                        .append(EnumChatFormatting.YELLOW)
                        .append((int) (progress * 100))
                        .append("%");

                int current = (int) skillInfo.currentXp;
                if (skillInfoLast != null && skillInfo.currentXpMax == skillInfoLast.currentXpMax) {
                    current = (int) interp(current, skillInfoLast.currentXp);
                }

                int remaining = (int) (skillInfo.currentXpMax - skillInfo.currentXp);
                if (skillInfoLast != null && skillInfo.currentXpMax == skillInfoLast.currentXpMax) {
                    remaining = (int) interp(remaining, (int) (skillInfoLast.currentXpMax - skillInfoLast.currentXp));
                }

                lineMap.put(0, levelStr.toString());
                lineMap.put(1, EnumChatFormatting.AQUA + "Current XP: " + EnumChatFormatting.YELLOW + format.format(current));
                if (remaining < 0) {
                    lineMap.put(2, EnumChatFormatting.AQUA + "Remaining XP: " + EnumChatFormatting.YELLOW + "MAXED!");
                    lineMap.put(4, EnumChatFormatting.AQUA + "ETA: " + EnumChatFormatting.YELLOW + "MAXED!");
                } else {
                    lineMap.put(2, EnumChatFormatting.AQUA + "Remaining XP: " + EnumChatFormatting.YELLOW + format.format(remaining));
                    if (xpGainHour < 1000) {
                        lineMap.put(4, EnumChatFormatting.AQUA + "ETA: " + EnumChatFormatting.YELLOW + "N/A");
                    } else {
                        lineMap.put(4, EnumChatFormatting.AQUA + "ETA: " + EnumChatFormatting.YELLOW + Utils.prettyTime((long) (remaining) * 1000 * 60 * 60 / (long) xpInterp));
                    }
                }

            }

            if (skillInfo != null && skillInfo.level == 60) {
                int current = (int) skillInfo.currentXp;
                if (skillInfoLast != null && skillInfo.currentXpMax == skillInfoLast.currentXpMax) {
                    current = (int) interp(current, skillInfoLast.currentXp);
                }

                lineMap.put(0, EnumChatFormatting.AQUA + "Combat: " + EnumChatFormatting.YELLOW + "60 " + EnumChatFormatting.RED + "(Maxed)");
                lineMap.put(2, EnumChatFormatting.AQUA + "Current XP: " + EnumChatFormatting.YELLOW + format.format(current));

            }

            if(overlayStrings != null && overlayStrings.isEmpty()) overlayStrings = null;
        }
    }


}