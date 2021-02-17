package io.github.moulberry.notenoughupdates.overlays;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.core.util.StringUtils;
import io.github.moulberry.notenoughupdates.core.util.lerp.LerpUtils;
import io.github.moulberry.notenoughupdates.cosmetics.CapeManager;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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

        /*if(Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
            GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            String containerName = container.getLowerChestInventory().getDisplayName().getUnformattedText();


            long currentTime = System.currentTimeMillis();
            if(currentTime - lastSkymallSync > 60*1000) {
                if(CapeManager.getInstance().lastJsonSync != null) {
                    JsonObject obj = CapeManager.getInstance().lastJsonSync;
                    if(obj.has("skymall") && obj.get("skymall").isJsonPrimitive()) {
                        activeSkymall = obj.get("skymall").getAsString();
                    }
                }
            }

            if(containerName.equals("Heart of the Mountain") && container.getLowerChestInventory().getSizeInventory() > 10) {
                System.out.println("HOTM Container");
                ItemStack stack = container.getLowerChestInventory().getStackInSlot(10);
                if(stack != null && stack.getDisplayName().equals(GREEN+"Sky Mall")) {
                    NotEnoughUpdates.INSTANCE.config.hidden.skymallActive = false;

                    String[] lines = NotEnoughUpdates.INSTANCE.manager.getLoreFromNBT(stack.getTagCompound());

                    for(String line : lines) {
                        if(line.equals("\u00a7aYour Current Effect")) {
                            System.out.println("Current effect");
                            NotEnoughUpdates.INSTANCE.config.hidden.skymallActive = true;
                        } else if(NotEnoughUpdates.INSTANCE.config.hidden.skymallActive) {
                            String prevActiveSkymall = activeSkymall;
                            System.out.println("Setting");
                            if(line.contains("Gain \u00a7a+100 \u00a76\u2E15 Mining Speed")) {
                                activeSkymall = "mining_speed";
                            } else if(line.contains("Gain \u00a7a+50 \u00a76\u2618 Mining Fortune")) {
                                activeSkymall = "mining_fortune";
                            } else if(line.contains("Gain \u00a7a+15% \u00a77Powder from mining")) {
                                activeSkymall = "powder";
                            } else if(line.contains("Reduce Pickaxe Ability cooldown")) {
                                activeSkymall = "pickaxe_ability";
                            } else if(line.contains("10x \u00a77chance to find Goblins")) {
                                activeSkymall = "goblin";
                            } else if(line.contains("Gain \u00a7a5x \u00a79Titanium \u00a77drops")) {
                                activeSkymall = "titanium";
                            } else {
                                System.out.println("Unknown");
                                activeSkymall = "unknown";
                            }
                            if(!activeSkymall.equals(prevActiveSkymall)) {
                                System.out.println("Maybe sending to server");
                                if(currentTime - lastSkymallSync > 60*1000) {
                                    lastSkymallSync = currentTime;
                                    System.out.println("Sending to server");
                                    NotEnoughUpdates.INSTANCE.manager.hypixelApi.getMyApiAsync("skymall?"+activeSkymall, (jsonObject) -> {
                                        System.out.println("Success!");
                                    }, () -> {
                                        System.out.println("Error!");
                                    });
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }*/

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
            switch(index) {
                case 0:
                    overlayStrings.addAll(commissionsStrings); break;
                case 1:
                    overlayStrings.add(mithrilPowder); break;
                case 2:
                    overlayStrings.addAll(forgeStrings); break;
                case 3:
                    overlayStrings.addAll(forgeStringsEmpty); break;
            }
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
