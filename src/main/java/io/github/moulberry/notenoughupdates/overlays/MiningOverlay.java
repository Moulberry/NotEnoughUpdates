package io.github.moulberry.notenoughupdates.overlays;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.core.util.StringUtils;
import io.github.moulberry.notenoughupdates.core.util.lerp.LerpUtils;
import io.github.moulberry.notenoughupdates.cosmetics.CapeManager;
import io.github.moulberry.notenoughupdates.miscfeatures.ItemCooldowns;
import io.github.moulberry.notenoughupdates.options.NEUConfig;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.Sys;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraft.util.EnumChatFormatting.*;

public class MiningOverlay extends TextOverlay {

    public MiningOverlay(Position position, Supplier<List<String>> dummyStrings, Supplier<TextOverlayStyle> styleSupplier) {
        super(position, dummyStrings, styleSupplier);
    }

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?: |$)");
    private static Map<String, Integer> commissionMaxes = new HashMap<>();
    public static Map<String, Float> commissionProgress = new LinkedHashMap<>();

    @Override
    public void updateFrequent() {
        if(Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
            GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            IInventory lower = container.getLowerChestInventory();
            String containerName = lower.getDisplayName().getUnformattedText();

            if(containerName.equals("Commissions") && lower.getSizeInventory() >= 18) {
                for(int i=9; i<18; i++) {
                    ItemStack stack = lower.getStackInSlot(i);
                    if(stack != null && stack.hasTagCompound()) {
                        String[] lore = NotEnoughUpdates.INSTANCE.manager.getLoreFromNBT(stack.getTagCompound());
                        String name = null;
                        int numberValue = -1;
                        for(String line : lore) {
                            if(name != null) {
                                String clean = Utils.cleanColour(line).trim();
                                if(clean.isEmpty()) {
                                    break;
                                } else {
                                    Matcher matcher = NUMBER_PATTERN.matcher(clean);
                                    if(matcher.find()) {
                                        try {
                                            numberValue = Integer.parseInt(matcher.group());
                                        } catch(NumberFormatException ignored) {}
                                    }
                                }
                            }
                            if(line.startsWith("\u00a77\u00a79")) {
                                String textAfter = line.substring(4);
                                if(!textAfter.contains("\u00a7") && !textAfter.equals("Rewards") && !textAfter.equals("Progress")) {
                                    name = textAfter;
                                }
                            }
                        }
                        if(name != null && numberValue > 0) {
                            commissionMaxes.put(name, numberValue);
                        }
                    }
                }
            }
        }
    }

    private static final Pattern timeRemainingForge = Pattern.compile("\\xA77Time Remaining: \\xA7a((?<Completed>Completed!)|(((?<days>[0-9]+)d)? ?((?<hours>[0-9]+)h)? ?((?<minutes>[0-9]+)m)? ?((?<seconds>[0-9]+)s)?))");
    private static final Pattern timeRemainingTab = Pattern.compile(".*[1-5]\\) (?<ItemName>.*): ((?<Ready>Ready!)|(((?<days>[0-9]+)d)? ?((?<hours>[0-9]+)h)? ?((?<minutes>[0-9]+)m)? ?((?<seconds>[0-9]+)s)?))");
    @Override
    public void update() {
        overlayStrings = null;
        NEUConfig.HiddenProfileSpecific hidden = NotEnoughUpdates.INSTANCE.config.getProfileSpecific();

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

        if (Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
            GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            IInventory lower = container.getLowerChestInventory();
            String containerName = lower.getDisplayName().getUnformattedText();
            if(containerName.equals("Forge") && lower.getSizeInventory() >= 36 && hidden != null){

                itemLoop:
                for (int i = 0; i < 5; i++) {
                    ItemStack stack = lower.getStackInSlot(i+11);
                    if(stack != null) {

                        String[] lore = NotEnoughUpdates.INSTANCE.manager.getLoreFromNBT(stack.getTagCompound());


                        for (int i1 = 0; i1 < lore.length; i1++) {
                            String line = lore[i1];
                            Matcher matcher = timeRemainingForge.matcher(line);
                            if (stack.getDisplayName().matches("\\xA7cSlot #([1-5])")){
                                ForgeItem newForgeItem = new ForgeItem("Locked", 0,1, i, false);
                                replaceForgeOrAdd(newForgeItem, hidden.forgeItems, true);
                                //empty Slot
                            } else if(stack.getDisplayName().matches("\\xA7aSlot #([1-5])")){
                                ForgeItem newForgeItem = new ForgeItem("Empty", 0,0, i, false);
                                replaceForgeOrAdd(newForgeItem, hidden.forgeItems, true);
                            } else if(matcher.matches()){
                                String timeremainingString = matcher.group(1);

                                long duration = 0;

                                if(matcher.group("Completed")!= null && !matcher.group("Completed").equals("")){
                                    ForgeItem newForgeItem = new ForgeItem(Utils.cleanColour(stack.getDisplayName()), 0, 2, i, false);
                                    replaceForgeOrAdd(newForgeItem, hidden.forgeItems, true);
                                } else {

                                    try {
                                        if (matcher.group("days") != null && !matcher.group("days").equals("")) {
                                            duration = duration + (long) Integer.parseInt(matcher.group("days")) * 24 * 60 * 60 * 1000;
                                        }
                                        if (matcher.group("hours") != null && !matcher.group("hours").equals("")) {
                                            duration = duration + (long) Integer.parseInt(matcher.group("hours")) * 60 * 60 * 1000;
                                        }
                                        if (matcher.group("minutes") != null && !matcher.group("minutes").equals("")) {
                                            duration = duration + (long) Integer.parseInt(matcher.group("minutes")) * 60 * 1000;
                                        }
                                        if (matcher.group("seconds") != null && !matcher.group("seconds").equals("")) {
                                            duration = duration + (long) Integer.parseInt(matcher.group("seconds")) * 1000;
                                        }
                                    } catch (Exception ignored) {
                                    }
                                    if (duration > 0) {
                                        ForgeItem newForgeItem = new ForgeItem(Utils.cleanColour(stack.getDisplayName()), System.currentTimeMillis() + duration, 2, i, false);
                                        replaceForgeOrAdd(newForgeItem, hidden.forgeItems, true);
                                    }
                                }

                                continue itemLoop;
                            }
                        }
                        //Locked Slot

                    }
                }
            }
        }

        if(!NotEnoughUpdates.INSTANCE.config.mining.dwarvenOverlay && NotEnoughUpdates.INSTANCE.config.mining.emissaryWaypoints == 0) return;
        if(SBInfo.getInstance().getLocation() == null) return;
        if(!SBInfo.getInstance().getLocation().equals("mining_3") && !SBInfo.getInstance().getLocation().equals("crystal_hollows")) return;

        overlayStrings = new ArrayList<>();
        commissionProgress.clear();

        String mithrilPowder = null;
        String gemstonePowder = null;
        int forgeInt = 0;
        boolean commissions = false;
        boolean forges = false;
        List<NetworkPlayerInfo> players = playerOrdering.sortedCopy(Minecraft.getMinecraft().thePlayer.sendQueue.getPlayerInfoMap());

        for(NetworkPlayerInfo info : players) {
            String name = Minecraft.getMinecraft().ingameGUI.getTabList().getPlayerName(info);
            if(name.contains("Mithril Powder:")) {
                mithrilPowder = DARK_AQUA+Utils.trimIgnoreColour(name).replaceAll("\u00a7[f|F|r]", "");
            }
            if(name.contains("Gemstone Powder:")) {
                gemstonePowder = DARK_AQUA+Utils.trimIgnoreColour(name).replaceAll("\u00a7[f|F|r]", "");
            }
            if(name.equals(RESET.toString()+BLUE+BOLD+"Forges "+RESET)) {
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

                    if(name.contains("LOCKED")) {
                        ForgeItem item = new ForgeItem("Locked", 0,1, forgeInt, true);
                        replaceForgeOrAdd(item, hidden.forgeItems, true);
                    } else if(name.contains("EMPTY")) {
                        ForgeItem item = new ForgeItem("Empty", 0,0, forgeInt, true);
                        replaceForgeOrAdd(item, hidden.forgeItems, true);
                        //forgeStringsEmpty.add(DARK_AQUA+"Forge "+ Utils.trimIgnoreColour(name).replaceAll("\u00a7[f|F|r]", ""));
                    } else {
                        String cleanName = Utils.cleanColour(name);

                        Matcher matcher = timeRemainingTab.matcher(cleanName);

                        if(matcher.matches()){

                            String itemName = matcher.group(1);

                            if(matcher.group("Ready") != null && !matcher.group("Ready").equals("")){
                                ForgeItem item = new ForgeItem(Utils.cleanColour(itemName), 0, 2, forgeInt, true);
                                replaceForgeOrAdd(item, hidden.forgeItems, true);
                            } else {
                                long duration = 0;
                                try {
                                    if (matcher.group("days") != null && !matcher.group("days").equals("")) {
                                        duration = duration + (long) Integer.parseInt(matcher.group("days")) * 24 * 60 * 60 * 1000;
                                    }
                                    if (matcher.group("hours") != null && !matcher.group("hours").equals("")) {
                                        duration = duration + (long) Integer.parseInt(matcher.group("hours")) * 60 * 60 * 1000;
                                    }
                                    if (matcher.group("minutes") != null && !matcher.group("minutes").equals("")) {
                                        duration = duration + (long) Integer.parseInt(matcher.group("minutes")) * 60 * 1000;
                                    }
                                    if (matcher.group("seconds") != null && !matcher.group("seconds").equals("")) {
                                        duration = duration + (long) Integer.parseInt(matcher.group("seconds")) * 1000;
                                    }
                                } catch (Exception ignored) {
                                }
                                if (duration > 0) {
                                    duration = duration + 4000;
                                    ForgeItem item = new ForgeItem(Utils.cleanColour(itemName), System.currentTimeMillis() + duration, 2, forgeInt, true);
                                    replaceForgeOrAdd(item, hidden.forgeItems, false);
                                }
                            }
                        }
                    }
                    forgeInt++;
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
        if(!NotEnoughUpdates.INSTANCE.config.mining.dwarvenOverlay){
            overlayStrings = null;
            return;
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
                if(true && commissionMaxes.containsKey(entry.getKey())) {
                    int max = commissionMaxes.get(entry.getKey());
                    commissionsStrings.add(DARK_AQUA+entry.getKey() + ": " + col+Math.round(entry.getValue()*max)+"/"+max);
                } else {
                    String valS = Utils.floatToString(entry.getValue()*100, 1);

                    commissionsStrings.add(DARK_AQUA+entry.getKey() + ": " + col+valS+"%");
                }
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

        String pickaxeCooldown;
        if(ItemCooldowns.pickaxeUseCooldownMillisRemaining <= 0) {
            pickaxeCooldown = DARK_AQUA+"Pickaxe CD: \u00a7aReady";
        } else {
            pickaxeCooldown = DARK_AQUA+"Pickaxe CD: \u00a7a" + (ItemCooldowns.pickaxeUseCooldownMillisRemaining/1000) + "s";
        }



        for(int index : NotEnoughUpdates.INSTANCE.config.mining.dwarvenText) {
            switch(index) {
                case 0:
                    overlayStrings.addAll(commissionsStrings); break;
                case 1:
                    overlayStrings.add(mithrilPowder); break;
                case 2:
                    overlayStrings.add(gemstonePowder); break;
                case 3:
                    overlayStrings.addAll(getForgeStrings(hidden.forgeItems)); break;
                case 4:
                    //overlayStrings.addAll(forgeStringsEmpty); break;
                case 5:
                    overlayStrings.add(pickaxeCooldown); break;
            }
        }

        if(overlayStrings.isEmpty()) overlayStrings = null;
    }

    private static List<String> getForgeStrings(List<ForgeItem> forgeItems){
        List<String> forgeString = new ArrayList<>();
        long currentTimeMillis = System.currentTimeMillis();
        forgeIDLabel:
        for (int i = 0; i < 5; i++) {
            for (int y = 0; y < forgeItems.size(); y++) {
                if (forgeItems.get(y).forgeID == i) {
                    ForgeItem item = forgeItems.get(y);
                    if (NotEnoughUpdates.INSTANCE.config.mining.forgeDisplay == 0) {
                        if (item.status == 2 && item.finishTime < currentTimeMillis) {

                            forgeString.add(item.getFormattedString(currentTimeMillis));
                            continue forgeIDLabel;
                        }
                    } else if (NotEnoughUpdates.INSTANCE.config.mining.forgeDisplay == 1) {
                        if (item.status == 2) {

                            forgeString.add(item.getFormattedString(currentTimeMillis));
                            continue forgeIDLabel;
                        }
                    } else if (NotEnoughUpdates.INSTANCE.config.mining.forgeDisplay == 2) {
                        if (item.status == 2 || item.status ==0) {

                            forgeString.add(item.getFormattedString(currentTimeMillis));
                            continue forgeIDLabel;
                        }
                    } else if (NotEnoughUpdates.INSTANCE.config.mining.forgeDisplay == 3) {

                        forgeString.add(item.getFormattedString(currentTimeMillis));
                        continue forgeIDLabel;
                    }
                }
            }
        }
        return forgeString;
    }

    private static void replaceForgeOrAdd(ForgeItem item, List<ForgeItem> forgeItems, boolean overwrite){
        for (int i = 0; i < forgeItems.size(); i++) {
            if (forgeItems.get(i).forgeID == item.forgeID) {
                if (overwrite) {
                    forgeItems.set(i, item);
                    return;
                } else {
                    ForgeItem currentItem = forgeItems.get(i);
                    if (!(currentItem.status == 2 && item.status ==2)) {
                        forgeItems.set(i, item);
                        return;
                    } else if(currentItem.fromScoreBoard){
                        forgeItems.set(i, item);
                        return;
                    }
                }
                return;
            }
        }
        forgeItems.add(item);
        return;
    }

    public static class ForgeItem{
        public ForgeItem(String itemName, long finishTime, int status, int forgeID, boolean fromScoreBoard){
            this.itemName = itemName;
            this.finishTime = finishTime;
            this.status = status;
            this.forgeID = forgeID;
            this.fromScoreBoard = fromScoreBoard;
        }


        @Expose public final String itemName;
        @Expose public final long finishTime;
        @Expose public final int status;
        @Expose public final int forgeID;
        @Expose public final boolean fromScoreBoard;



        public String getFormattedString(long currentTimeMillis){
            String returnText = EnumChatFormatting.AQUA+"Forge "+(this.forgeID+1)+": ";
            if(status == 0){
                return returnText +EnumChatFormatting.GRAY +"Empty";
            } else if(status == 1){
                return returnText+ EnumChatFormatting.DARK_RED+"Locked";
            }

            long timeDuration = finishTime - currentTimeMillis;
            returnText =  returnText+ EnumChatFormatting.DARK_PURPLE +this.itemName+" : ";

            int days = (int) (timeDuration / (1000*60*60*24));
            timeDuration = timeDuration-(days*(1000*60*60*24));
            int hours = (int) ((timeDuration / (1000*60*60)) % 24);

            if(days > 0){
                return returnText+EnumChatFormatting.AQUA+days+"d "+hours+"h";
            }
            timeDuration = timeDuration-(hours*(1000*60*60));
            int minutes = (int) ((timeDuration / (1000*60)) % 60);
            if(hours > 0){
                return returnText+EnumChatFormatting.AQUA+hours+"h "+minutes+"m";
            }
            timeDuration = timeDuration-(minutes*(1000*60));
            int seconds = (int) (timeDuration / 1000) % 60 ;
            if(minutes > 0){
                return returnText+EnumChatFormatting.AQUA+minutes+"m "+seconds+"s";
            } else if(seconds > 0){
                return returnText+EnumChatFormatting.AQUA+seconds+"s";
            } else {
                return returnText+ EnumChatFormatting.DARK_GREEN+"Done";
            }
        }
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
