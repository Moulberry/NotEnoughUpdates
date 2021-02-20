package io.github.moulberry.notenoughupdates.overlays;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.options.NEUConfig;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraft.util.EnumChatFormatting.*;

public class TimersOverlay extends TextOverlay {

    public TimersOverlay(Position position, Supplier<List<String>> dummyStrings, Supplier<TextOverlayStyle> styleSupplier) {
        super(position, dummyStrings, styleSupplier);
    }

    private static final Pattern CAKE_PATTERN = Pattern.compile("\u00a7r\u00a7d\u00a7lYum! \u00a7r\u00a7eYou gain .+ \u00a7r\u00a7efor \u00a7r\u00a7a48 \u00a7r\u00a7ehours!\u00a7r");
    private static final Pattern PUZZLER_PATTERN = Pattern.compile("\u00a7r\u00a7dPuzzler\u00a7r\u00a76 gave you .+ \u00a7r\u00a76for solving the puzzle!\u00a7r");
    private static final Pattern FETCHUR_PATTERN = Pattern.compile("\u00a7e\\[NPC] Fetchur\u00a7f: \u00a7rthanks thats probably what i needed\u00a7r");
    private static final Pattern FETCHUR2_PATTERN = Pattern.compile("\u00a7e\\[NPC] Fetchur\u00a7f: \u00a7rcome back another time, maybe tmrw\u00a7r");
    private static final Pattern GODPOT_PATTERN = Pattern.compile("\u00a7r\u00a7a\u00a7lGULP! \u00a7r\u00a7eThe \u00a7r\u00a7cGod Potion \u00a7r\u00a7egrants you" +
            " powers for \u00a7r\u00a7924 hours\u00a7r\u00a7e!\u00a7r");

    private boolean hideGodpot = false;

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChatMessageReceived(ClientChatReceivedEvent event) {
        NEUConfig.HiddenProfileSpecific hidden = NotEnoughUpdates.INSTANCE.config.getProfileSpecific();
        if(hidden == null) return;

        if(event.type == 0) {
            long currentTime = System.currentTimeMillis();

            Matcher cakeMatcher = CAKE_PATTERN.matcher(event.message.getFormattedText());
            if(cakeMatcher.matches()) {
                hidden.firstCakeAte = currentTime;
                return;
            }

            Matcher puzzlerMatcher = PUZZLER_PATTERN.matcher(event.message.getFormattedText());
            if(puzzlerMatcher.matches()) {
                hidden.puzzlerCompleted = currentTime;
                return;
            }

            Matcher fetchurMatcher = FETCHUR_PATTERN.matcher(event.message.getFormattedText());
            if(fetchurMatcher.matches()) {
                hidden.fetchurCompleted = currentTime;
                return;
            }

            Matcher fetchur2Matcher = FETCHUR2_PATTERN.matcher(event.message.getFormattedText());
            if(fetchur2Matcher.matches()) {
                hidden.fetchurCompleted = currentTime;
                return;
            }

            Matcher godpotMatcher = GODPOT_PATTERN.matcher(event.message.getFormattedText());
            if(godpotMatcher.matches()) {
                hidden.godPotionDrunk = currentTime;
                return;
            }
        }
    }

    private static final Pattern PATTERN_ACTIVE_EFFECTS = Pattern.compile("\u00a77You have \u00a7r\u00a7e(\\d+) \u00a7r\u00a77active effects.");

    @Override
    public void update() {
        long currentTime = System.currentTimeMillis();

        NEUConfig.HiddenProfileSpecific hidden = NotEnoughUpdates.INSTANCE.config.getProfileSpecific();
        if(hidden == null) return;

        if(Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
            GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            IInventory lower = container.getLowerChestInventory();
            String containerName = lower.getDisplayName().getUnformattedText();

            if(containerName.equals("Commissions") && lower.getSizeInventory() >= 18) {
                if(hidden.commissionsCompleted == 0) {
                    hidden.commissionsCompleted = currentTime;
                }
                for(int i=9; i<18; i++) {
                    ItemStack stack = lower.getStackInSlot(i);
                    if(stack != null && stack.hasTagCompound()) {
                        String[] lore = NotEnoughUpdates.INSTANCE.manager.getLoreFromNBT(stack.getTagCompound());
                        for(String line : lore) {
                            if(line.contains("(Daily")) {
                                hidden.commissionsCompleted = 0;
                                break;
                            }
                        }
                    }
                }
            } else if(containerName.equals("Experimentation Table") && lower.getSizeInventory() >= 36) {
                ItemStack stack = lower.getStackInSlot(31);
                if(stack != null) {
                    if(stack.getItem() == Items.blaze_powder) {
                        if(hidden.experimentsCompleted == 0) {
                            hidden.experimentsCompleted = currentTime;
                        }
                    } else {
                        hidden.experimentsCompleted = 0;
                    }
                }
            }
        }

        boolean foundCookieBuffText = false;
        if(SBInfo.getInstance().footer != null) {
            String formatted = SBInfo.getInstance().footer.getFormattedText();
            for(String line : formatted.split("\n")) {
                Matcher activeEffectsMatcher = PATTERN_ACTIVE_EFFECTS.matcher(line);
                if(activeEffectsMatcher.find()) {
                    String numEffectsS = activeEffectsMatcher.group(1);
                    try {
                        int numEffects = Integer.parseInt(numEffectsS);
                        hideGodpot = numEffects > 25;
                    } catch(NumberFormatException ignored) {}
                } else if(line.contains("\u00a7d\u00a7lCookie Buff")) {
                    foundCookieBuffText = true;
                } else if(foundCookieBuffText) {
                    String cleanNoSpace = line.replaceAll("(\u00a7.| )", "");

                    hidden.cookieBuffRemaining = 0;
                    StringBuilder number = new StringBuilder();
                    for(int i=0; i<cleanNoSpace.length(); i++) {
                        char c = cleanNoSpace.charAt(i);

                        if(c >= '0' && c <= '9') {
                            number.append(c);
                        } else {
                            if(number.length() == 0) {
                                hidden.cookieBuffRemaining = 0;
                                break;
                            }
                            if("ydhms".contains(""+c)) {
                                try {
                                    int val = Integer.parseInt(number.toString());
                                    switch(c) {
                                        case 'y': hidden.cookieBuffRemaining += val*365*24*60*60*1000; break;
                                        case 'd': hidden.cookieBuffRemaining += val*24*60*60*1000; break;
                                        case 'h': hidden.cookieBuffRemaining += val*60*60*1000; break;
                                        case 'm': hidden.cookieBuffRemaining += val*60*1000; break;
                                        case 's': hidden.cookieBuffRemaining += val*1000; break;
                                    }
                                } catch(NumberFormatException e) {
                                    hidden.cookieBuffRemaining = 0;
                                    break;
                                }

                                number = new StringBuilder();
                            } else {
                                hidden.cookieBuffRemaining = 0;
                                break;
                            }
                        }
                    }

                    break;
                }
            }
        }

        overlayStrings = new ArrayList<>();

        long cakeEnd = hidden.firstCakeAte + 1000*60*60*48 - currentTime;
        if(cakeEnd < 0) {
            overlayStrings.add(DARK_AQUA+"Cakes: "+YELLOW+"Ready!");
        } else {
            overlayStrings.add(DARK_AQUA+"Cakes: "+YELLOW+Utils.prettyTime(cakeEnd));
        }

        long puzzlerEnd = hidden.puzzlerCompleted + 1000*60*60*24 - currentTime;
        if(puzzlerEnd < 0) {
            overlayStrings.add(DARK_AQUA+"Puzzler: "+YELLOW+"Ready!");
        } else {
            overlayStrings.add(DARK_AQUA+"Puzzler: "+YELLOW+Utils.prettyTime(puzzlerEnd));
        }

        if(!hideGodpot) {
            long godpotEnd = hidden.godPotionDrunk + 1000*60*60*24 - currentTime;
            if(godpotEnd < 0) {
                overlayStrings.add(DARK_AQUA+"Godpot: "+YELLOW+"Inactive!");
            } else {
                overlayStrings.add(DARK_AQUA+"Godpot: "+YELLOW+Utils.prettyTime(puzzlerEnd));
            }
        }

        long midnightReset = (currentTime-18000000)/86400000*86400000+18000000;
        long fetchurComplete = hidden.fetchurCompleted;
        if(fetchurComplete < midnightReset) {
            overlayStrings.add(DARK_AQUA+"Fetchur: "+YELLOW+"Ready!");
        } else {
            overlayStrings.add(DARK_AQUA+"Fetchur: "+YELLOW+Utils.prettyTime(midnightReset + 86400000 - currentTime));
        }

        if(hidden.commissionsCompleted < midnightReset) {
            overlayStrings.add(DARK_AQUA+"Commissions: "+YELLOW+"Ready!");
        } else {
            overlayStrings.add(DARK_AQUA+"Commissions: "+YELLOW+Utils.prettyTime(midnightReset + 86400000 - currentTime));
        }

        if(hidden.experimentsCompleted < midnightReset) {
            overlayStrings.add(DARK_AQUA+"Experiments: "+YELLOW+"Ready!");
        } else {
            overlayStrings.add(DARK_AQUA+"Experiments: "+YELLOW+Utils.prettyTime(midnightReset + 86400000 - currentTime));
        }

        if(hidden.cookieBuffRemaining <= 0) {
            overlayStrings.add(DARK_AQUA+"Cookie Buff: "+YELLOW+"Inactive!");
        } else {
            overlayStrings.add(DARK_AQUA+"Cookie Buff: "+YELLOW+Utils.prettyTime(hidden.cookieBuffRemaining));
        }

        /*List<NetworkPlayerInfo> players = playerOrdering.sortedCopy(Minecraft.getMinecraft().thePlayer.sendQueue.getPlayerInfoMap());
        Minecraft.getMinecraft().thePlayer.sendQueue.
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
        }*/

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

        /*for(int index : NotEnoughUpdates.INSTANCE.config.mining.dwarvenText) {
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

        if(overlayStrings.isEmpty()) overlayStrings = null;*/
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
