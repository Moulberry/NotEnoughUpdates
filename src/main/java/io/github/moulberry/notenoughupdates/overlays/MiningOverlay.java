package io.github.moulberry.notenoughupdates.overlays;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.core.util.StringUtils;
import io.github.moulberry.notenoughupdates.core.util.lerp.LerpUtils;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zone.nora.moulberry.MoulberryKt;

import java.util.*;
import java.util.function.Supplier;

import static net.minecraft.util.EnumChatFormatting.*;

public class MiningOverlay extends TextOverlay {

    public MiningOverlay(Position position, Supplier<List<String>> dummyStrings, Supplier<TextOverlayStyle> styleSupplier) {
        super(position, dummyStrings, styleSupplier);
    }

    public static Map<String, Float> commissionProgress = new LinkedHashMap<>();

    @Override
    public void update() {
        overlayStrings = null;

        if(!NotEnoughUpdates.INSTANCE.config.mining.dwarvenOverlay) return;
        if(SBInfo.getInstance().getLocation() == null) return;
        if(!SBInfo.getInstance().getLocation().equals("mining_3")) return;

        overlayStrings = new ArrayList<>();
        commissionProgress.clear();
        List<String> forgeStrings = new ArrayList<>();
        List<String> forgeStringsEmpty = new ArrayList<>();
        String mithrilPowder = null;

        boolean commissions = false;
        boolean forges = false;
        List<NetworkPlayerInfo> players = playerOrdering.sortedCopy(Minecraft.getMinecraft().thePlayer.sendQueue.getPlayerInfoMap());
        for(NetworkPlayerInfo info : players) {
            String name = Minecraft.getMinecraft().ingameGUI.getTabList().getPlayerName(info);
            if(name.contains("Mithril Powder:")) {
                mithrilPowder = DARK_AQUA+Utils.trimIgnoreColour(name).replaceAll("\u00a7[f|F|r]", "");
            }
            if(name.equals(RESET.toString()+BLUE+BOLD+"Forges"+RESET)) {
                commissions = false;
                forges = true;
                continue;
            } else if(name.equals(RESET.toString()+BLUE+BOLD+"Commissions"+RESET)) {
                commissions = true;
                forges = false;
                continue;
            }
            String clean = StringUtils.cleanColour(name);
            if(forges && clean.startsWith(" ")) {
                char firstChar = clean.trim().charAt(0);
                if(firstChar < '0' || firstChar > '9') {
                    forges = false;
                } else {
                    if(name.contains("LOCKED")) continue;
                    if(name.contains("EMPTY")) {
                        forgeStringsEmpty.add(DARK_AQUA+"Forge "+ Utils.trimIgnoreColour(name).replaceAll("\u00a7[f|F|r]", ""));
                    } else {
                        forgeStrings.add(DARK_AQUA+"Forge "+ Utils.trimIgnoreColour(name).replaceAll("\u00a7[f|F|r]", ""));
                    }
                }
            } else if(commissions && clean.startsWith(" ")) {
                String[] split = clean.trim().split(": ");
                if(split.length == 2) {
                    if(split[1].endsWith("%")) {
                        try {
                            float progress = Float.parseFloat(split[1].replace("%", ""))/100;
                            progress = LerpUtils.clampZeroOne(progress);
                            commissionProgress.put(split[0], progress);
                        } catch(Exception ignored) {}
                    } else {
                        commissionProgress.put(split[0], 1.0f);
                    }
                }
            } else {
                commissions = false;
                forges = false;
            }
        }

        List<String> commissionsStrings = new ArrayList<>();
        for(Map.Entry<String, Float> entry : commissionProgress.entrySet()) {
            if(entry.getValue() >= 1) {
                commissionsStrings.add(DARK_AQUA+entry.getKey() + ": " + GREEN + "DONE");
            } else {
                EnumChatFormatting col = RED;
                if(entry.getValue() >= 0.75) {
                    col = GREEN;
                } else if(entry.getValue() >= 0.5) {
                    col = YELLOW;
                } else if(entry.getValue() >= 0.25) {
                    col = GOLD;
                }

                String valS = Utils.floatToString(entry.getValue()*100, 1);

                commissionsStrings.add(DARK_AQUA+entry.getKey() + ": " + col+valS+"%");
            }
        }
        /*boolean hasAny = false;
        if(NotEnoughUpdates.INSTANCE.config.mining.dwarvenOverlay) {
            overlayStrings.addAll(commissionsStrings);
            hasAny = true;
        }
        if(NotEnoughUpdates.INSTANCE.config.mining.powderOverlay) {
            if(mithrilPowder != null) {
                if(hasAny) overlayStrings.add(null);
                overlayStrings.add(DARK_AQUA+mithrilPowder);
                hasAny = true;
            }
        }
        if(NotEnoughUpdates.INSTANCE.config.mining.forgeOverlay) {
            if(hasAny) overlayStrings.add(null);
            overlayStrings.addAll(forgeStrings);
        }*/

        for(int index : NotEnoughUpdates.INSTANCE.config.mining.dwarvenText) {
            String finalMithrilPowder = mithrilPowder;
            MoulberryKt.javaSwitch(index, iSwitch -> {
                iSwitch.addCase(0, false, () -> overlayStrings.addAll(commissionsStrings));
                iSwitch.addCase(1, false, () -> overlayStrings.add(finalMithrilPowder));
                iSwitch.addCase(2, false, () -> overlayStrings.addAll(forgeStrings));
                iSwitch.addCase(3, false, () -> overlayStrings.addAll(forgeStringsEmpty));
                return iSwitch;
            });
        }

        if(overlayStrings.isEmpty()) overlayStrings = null;
    }

    private static final Ordering<NetworkPlayerInfo> playerOrdering = Ordering.from(new PlayerComparator());

    @SideOnly(Side.CLIENT)
    static class PlayerComparator implements Comparator<NetworkPlayerInfo> {
        private PlayerComparator() { }

        public int compare(NetworkPlayerInfo o1, NetworkPlayerInfo o2) {
            ScorePlayerTeam team1 = o1.getPlayerTeam();
            ScorePlayerTeam team2 = o2.getPlayerTeam();
            return ComparisonChain.start().compareTrueFirst(
                    o1.getGameType() != WorldSettings.GameType.SPECTATOR,
                    o2.getGameType() != WorldSettings.GameType.SPECTATOR)
                            .compare(team1 != null ? team1.getRegisteredName() : "", team2 != null ? team2.getRegisteredName() : "")
                            .compare(o1.getGameProfile().getName(), o2.getGameProfile().getName()).result();
        }
    }


}
