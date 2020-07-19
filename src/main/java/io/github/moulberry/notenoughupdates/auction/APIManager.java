package io.github.moulberry.notenoughupdates.auction;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class APIManager {

    private NEUManager manager;
    public final CustomAH customAH;

    private int totalPages = 0;
    private int lastApiUpdate;
    private LinkedList<Integer> needUpdate = new LinkedList<>();

    private TreeMap<String, Auction> auctionMap = new TreeMap<>();
    public HashMap<String, HashSet<String>> internalnameToAucIdMap = new HashMap<>();
    private HashSet<String> playerBids = new HashSet<>();
    private HashSet<String> playerBidsNotified = new HashSet<>();
    private HashSet<String> playerBidsFinishedNotified = new HashSet<>();

    private HashMap<String, TreeMap<Integer, String>> internalnameToLowestBIN = new HashMap<>();

    private JsonArray playerInformation = null;

    public TreeMap<String, HashMap<Integer, HashSet<String>>> extrasToAucIdMap = new TreeMap<>();

    private long lastPageUpdate = 0;
    private long lastProfileUpdate = 0;
    private long lastCustomAHSearch = 0;
    private long lastCleanup = 0;

    public int activeAuctions = 0;
    public int uniqueItems = 0;
    public int totalTags = 0;
    public int internalnameTaggedAuctions = 0;
    public int taggedAuctions = 0;
    public int processMillis = 0;

    private boolean doFullUpdate = false;

    public APIManager(NEUManager manager) {
        this.manager = manager;
        customAH = new CustomAH(manager);
    }

    private AtomicInteger playerInfoVersion = new AtomicInteger(0);

    public JsonObject getPlayerInformation() {
        if(playerInformation == null) return null;
        for(int i=0; i<playerInformation.size(); i++) {
            JsonObject profile = playerInformation.get(i).getAsJsonObject();
            if(profile.get("cute_name").getAsString().equalsIgnoreCase(manager.getCurrentProfile())) {
                if(!profile.has("members")) return null;
                JsonObject members = profile.get("members").getAsJsonObject();
                String uuid = Minecraft.getMinecraft().thePlayer.getUniqueID().toString().replace("-", "");
                if(!members.has(uuid)) return null;
                return members.get(uuid).getAsJsonObject();
            }
        }
        return null;
    }

    public int getPlayerInfoVersion() {
        return playerInfoVersion.get();
    }

    public void incPlayerInfoVersion() {
        playerInfoVersion.incrementAndGet();
    }

    public TreeMap<String, Auction> getAuctionItems() {
        return auctionMap;
    }

    public HashSet<String> getPlayerBids() {
        return playerBids;
    }

    public HashSet<String> getAuctionsForInternalname(String internalname) {
        return internalnameToAucIdMap.computeIfAbsent(internalname, k -> new HashSet<>());
    }

    public class Auction {
        public String auctioneerUuid;
        public long end;
        public int starting_bid;
        public int highest_bid_amount;
        public int bid_count;
        public boolean bin;
        public String category;
        public String rarity;
        public NBTTagCompound item_tag;
        private ItemStack stack;

        public long lastUpdate = 0;

        public int enchLevel = 0; //0 = clean, 1 = ench, 2 = ench/hpb

        public Auction(String auctioneerUuid, long end, int starting_bid, int highest_bid_amount, int bid_count,
                       boolean bin, String category, String rarity, NBTTagCompound item_tag) {
            this.auctioneerUuid = auctioneerUuid;
            this.end = end;
            this.starting_bid = starting_bid;
            this.highest_bid_amount = highest_bid_amount;
            this.bid_count = bid_count;
            this.bin = bin;
            this.category = category;
            this.rarity = rarity;
            this.item_tag = item_tag;
        }

        public ItemStack getStack() {
            if(stack != null) {
                return stack;
            } else {
                JsonObject item = manager.getJsonFromNBT(item_tag);
                ItemStack stack = manager.jsonToStack(item, false);
                this.stack = stack;
                return stack;
            }
        }
    }

    public void tick() {
        customAH.tick();
        if(System.currentTimeMillis() - lastPageUpdate > 5*1000) {
            lastPageUpdate = System.currentTimeMillis();
            updatePageTick();
            ahNotification();
        }
        if(System.currentTimeMillis() - lastProfileUpdate > 10*1000) {
            lastProfileUpdate = System.currentTimeMillis();
            updateProfiles(Minecraft.getMinecraft().thePlayer.getUniqueID().toString().replace("-", ""));
        }
        if(System.currentTimeMillis() - lastCleanup > 120*1000) {
            lastCleanup = System.currentTimeMillis();
            cleanup();
        }
        if(System.currentTimeMillis() - lastCustomAHSearch > 60*1000) {
            lastCustomAHSearch = System.currentTimeMillis();
            if(Minecraft.getMinecraft().currentScreen instanceof CustomAHGui || customAH.isRenderOverAuctionView()) {
                customAH.updateSearch();
                calculateStats();
            }
        }
    }

    public void updateProfiles(String uuid) {
        HashMap<String, String> args = new HashMap<>();
        args.put("uuid", ""+uuid);
        manager.hypixelApi.getHypixelApiAsync(manager.config.apiKey.value, "skyblock/profiles",
            args, jsonObject -> {
                if(jsonObject.has("success") && jsonObject.get("success").getAsBoolean()) {
                    incPlayerInfoVersion();
                    playerInformation = jsonObject.get("profiles").getAsJsonArray();
                    if(playerInformation == null) return;
                    String backup = null;
                    long backupLastSave = 0;
                    for(int i=0; i<playerInformation.size(); i++) {
                        JsonObject profile = playerInformation.get(i).getAsJsonObject();
                        String cute_name = profile.get("cute_name").getAsString();

                        if(backup == null) backup = cute_name;

                        if(!profile.has("members")) continue;
                        JsonObject members = profile.get("members").getAsJsonObject();

                        if(members.has(uuid)) {
                            JsonObject member = members.get(uuid).getAsJsonObject();
                            long last_save = member.get("last_save").getAsLong();
                            if(last_save > backupLastSave) {
                                backupLastSave = last_save;
                                backup = cute_name;
                            }
                        }
                    }

                    manager.setCurrentProfileBackup(backup);
                }
            }
        );
    }

    private String niceAucId(String aucId) {
        if(aucId.length()!=32) return aucId;

        StringBuilder niceAucId = new StringBuilder();
        niceAucId.append(aucId, 0, 8);
        niceAucId.append("-");
        niceAucId.append(aucId, 8, 12);
        niceAucId.append("-");
        niceAucId.append(aucId, 12, 16);
        niceAucId.append("-");
        niceAucId.append(aucId, 16, 20);
        niceAucId.append("-");
        niceAucId.append(aucId, 20, 32);
        return niceAucId.toString();
    }

    public int getLowestBin(String internalname) {
        TreeMap<Integer, String> lowestBIN = internalnameToLowestBIN.get(internalname);
        if(lowestBIN == null || lowestBIN.isEmpty()) return -1;
        return lowestBIN.firstKey();
    }

    private void ahNotification() {
        playerBidsNotified.retainAll(playerBids);
        playerBidsFinishedNotified.retainAll(playerBids);
        if(manager.config.ahNotification.value <= 0) {
            return;
        }
        for(String aucid : playerBids) {
            Auction auc = auctionMap.get(aucid);
            if(!playerBidsNotified.contains(aucid)) {
                if(auc != null && auc.end - System.currentTimeMillis() < 1000*60*manager.config.ahNotification.value) {
                    ChatComponentText message = new ChatComponentText(
                            EnumChatFormatting.YELLOW+"The " + auc.getStack().getDisplayName() + EnumChatFormatting.YELLOW + " you have bid on is ending soon! Click here to view.");
                    ChatStyle clickEvent = new ChatStyle().setChatClickEvent(
                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewauction " + niceAucId(aucid)));
                    clickEvent.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW+"View auction")));
                    message.setChatStyle(clickEvent);
                    Minecraft.getMinecraft().thePlayer.addChatMessage(message);

                    playerBidsNotified.add(aucid);
                }
            }
            if(!playerBidsFinishedNotified.contains(aucid)) {
                if(auc != null && auc.end < System.currentTimeMillis()) {
                    ChatComponentText message = new ChatComponentText(
                            EnumChatFormatting.YELLOW+"The " + auc.getStack().getDisplayName() + EnumChatFormatting.YELLOW + " you have bid on (might) have ended! Click here to view.");
                    ChatStyle clickEvent = new ChatStyle().setChatClickEvent(
                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewauction " + niceAucId(aucid)));
                    clickEvent.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW+"View auction")));
                    message.setChatStyle(clickEvent);
                    Minecraft.getMinecraft().thePlayer.addChatMessage(message);

                    playerBidsFinishedNotified.add(aucid);
                }
            }
        }
    }

    private void cleanup() {
        try {
            long currTime = System.currentTimeMillis();
            Set<String> toRemove = new HashSet<>();
            for(Map.Entry<String, Auction> entry : auctionMap.entrySet()) {
                long timeToEnd = entry.getValue().end - currTime;
                if(timeToEnd < -60) {
                    toRemove.add(entry.getKey());
                } else if(currTime - entry.getValue().lastUpdate > 5*60*1000) {
                    toRemove.add(entry.getKey());
                }
            }
            toRemove.removeAll(playerBids);
            for(String aucid : toRemove) {
                auctionMap.remove(aucid);
            }
            for(HashMap<Integer, HashSet<String>> extrasMap : extrasToAucIdMap.values()) {
                for(HashSet<String> aucids : extrasMap.values()) {
                    for(String aucid : toRemove) {
                        aucids.remove(aucid);
                    }
                }
            }
            for(HashSet<String> aucids : internalnameToAucIdMap.values()) {
                aucids.removeAll(toRemove);
            }
            for(TreeMap<Integer, String> lowestBINs : internalnameToLowestBIN.values()) {
                lowestBINs.values().removeAll(toRemove);
            }
        } catch(ConcurrentModificationException e) {
            cleanup();
        }
    }

    private void updatePageTick() {
        if(totalPages == 0) {
            getPageFromAPI(0);
        } else if(doFullUpdate) {
            doFullUpdate = false;
            for(int i=0; i<totalPages; i++) {
                getPageFromAPI(i);
            }
        } else {
            if(needUpdate.isEmpty()) resetNeedUpdate();

            int pageToUpdate = needUpdate.pop();
            while (pageToUpdate >= totalPages && !needUpdate.isEmpty()) {
                pageToUpdate = needUpdate.pop();
            }

            getPageFromAPI(pageToUpdate);
        }
    }

    public void calculateStats() {
        try {
            uniqueItems = internalnameToAucIdMap.size();
            Set<String> aucs = new HashSet<>();
            for(HashSet<String> aucids : internalnameToAucIdMap.values()) {
                aucs.addAll(aucids);
            }
            internalnameTaggedAuctions = aucs.size();
            totalTags = extrasToAucIdMap.size();
            aucs = new HashSet<>();
            for(HashMap<Integer, HashSet<String>> extrasMap : extrasToAucIdMap.values()) {
                for(HashSet<String> aucids : extrasMap.values()) {
                    aucs.addAll(aucids);
                }
            }
            taggedAuctions = aucs.size();
        } catch(Exception e) {}
    }

    String[] rarityArr = new String[] {
       "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "SPECIAL", "VERY SPECIAL",
    };

    public int checkItemType(String lore, boolean contains, String... typeMatches) {
        String[] split = lore.split("\n");
        for(int i=split.length-1; i>=0; i--) {
            String line = split[i];
            for(String rarity : rarityArr) {
                for(int j=0; j<typeMatches.length; j++) {
                    if(contains) {
                        if(line.trim().contains(rarity + " " + typeMatches[j])) {
                            return j;
                        } else if(line.trim().contains(rarity + " DUNGEON " + typeMatches[j])) {
                            return j;
                        }
                    } else {
                        if(line.trim().endsWith(rarity + " " + typeMatches[j])) {
                            return j;
                        } else if(line.trim().endsWith(rarity + " DUNGEON " + typeMatches[j])) {
                            return j;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private void getPageFromAPI(int page) {
        //System.out.println("Trying to update page: " + page);
        HashMap<String, String> args = new HashMap<>();
        args.put("page", ""+page);
        manager.hypixelApi.getHypixelApiAsync(manager.config.apiKey.value, "skyblock/auctions",
            args, jsonObject -> {
                if (jsonObject.get("success").getAsBoolean()) {
                    totalPages = jsonObject.get("totalPages").getAsInt();
                    activeAuctions = jsonObject.get("totalAuctions").getAsInt();

                    int lastUpdated = jsonObject.get("lastUpdated").getAsInt();

                    if(lastApiUpdate != lastUpdated) {
                        if(manager.config.quickAHUpdate.value &&
                                (Minecraft.getMinecraft().currentScreen instanceof CustomAHGui || customAH.isRenderOverAuctionView())) {
                            doFullUpdate = true;
                        }
                        resetNeedUpdate();
                    }

                    lastApiUpdate = lastUpdated;

                    String[] lvl4Maxes = {"Experience", "Life Steal", "Scavenger", "Looting"};

                    String[] categoryItemType = new String[]{"sword","fishingrod","pickaxe","axe",
                            "shovel","petitem","travelscroll","reforgestone","bow"};
                    String playerUUID = Minecraft.getMinecraft().thePlayer.getUniqueID().toString().replaceAll("-","");

                    long startProcess = System.currentTimeMillis();
                    JsonArray auctions = jsonObject.get("auctions").getAsJsonArray();
                    for (int i = 0; i < auctions.size(); i++) {
                        JsonObject auction = auctions.get(i).getAsJsonObject();

                        String auctionUuid = auction.get("uuid").getAsString();
                        String auctioneerUuid = auction.get("auctioneer").getAsString();
                        long end = auction.get("end").getAsLong();
                        int starting_bid = auction.get("starting_bid").getAsInt();
                        int highest_bid_amount = auction.get("highest_bid_amount").getAsInt();
                        int bid_count = auction.get("bids").getAsJsonArray().size();
                        boolean bin = false;
                        if(auction.has("bin")) {
                            bin = auction.get("bin").getAsBoolean();
                        }
                        String sbCategory = auction.get("category").getAsString();
                        String extras = auction.get("extra").getAsString();
                        String item_name = auction.get("item_name").getAsString();
                        String item_lore = Utils.fixBrokenAPIColour(auction.get("item_lore").getAsString());
                        String item_bytes = auction.get("item_bytes").getAsString();
                        String rarity = auction.get("tier").getAsString();
                        JsonArray bids = auction.get("bids").getAsJsonArray();

                        for(String lvl4Max : lvl4Maxes) {
                            item_lore = item_lore.replaceAll("\\u00A79("+lvl4Max+" IV)", EnumChatFormatting.DARK_PURPLE+"$1");
                        }
                        item_lore = item_lore.replaceAll("\\u00A79([A-Za-z ]+ VI)", EnumChatFormatting.DARK_PURPLE+"$1");
                        item_lore = item_lore.replaceAll("\\u00A79([A-Za-z ]+ VII)", EnumChatFormatting.RED+"$1");

                        try {
                            NBTTagCompound item_tag;
                            try {
                                item_tag = CompressedStreamTools.readCompressed(
                                        new ByteArrayInputStream(Base64.getDecoder().decode(item_bytes)));
                            } catch(IOException e) { continue; }

                            NBTTagCompound tag = item_tag.getTagList("i", 10).getCompoundTagAt(0).getCompoundTag("tag");
                            String internalname = manager.getInternalnameFromNBT(tag);
                            String displayNormal = "";
                            if(manager.getItemInformation().containsKey(internalname)) {
                                displayNormal = Utils.cleanColour(manager.getItemInformation().get(internalname).get("displayname").getAsString());
                            }

                            String[] lore = new String[0];
                            NBTTagCompound display = tag.getCompoundTag("display");
                            if(display.hasKey("Lore", 9)) {
                                NBTTagList loreList = new NBTTagList();
                                for(String line : item_lore.split("\n")) {
                                    loreList.appendTag(new NBTTagString(line));
                                }
                                display.setTag("Lore", loreList);
                            }
                            tag.setTag("display", display);
                            item_tag.getTagList("i", 10).getCompoundTagAt(0).setTag("tag", tag);

                            int index=0;
                            for(String str : extras.split(" ")) {
                                str = Utils.cleanColour(str).toLowerCase();
                                if(str.length() > 0) {
                                    HashMap<Integer, HashSet<String>> extrasMap = extrasToAucIdMap.computeIfAbsent(str, k -> new HashMap<>());
                                    HashSet<String> aucids = extrasMap.computeIfAbsent(index, k -> new HashSet<>());
                                    aucids.add(auctionUuid);
                                }
                                index++;
                            }

                            if(bin) {
                                TreeMap<Integer, String> lowestBINs = internalnameToLowestBIN.computeIfAbsent(internalname, k -> new TreeMap<>());
                                int count = item_tag.getInteger("Count");
                                lowestBINs.put(starting_bid/(count>0?count:1), auctionUuid);
                                if(lowestBINs.size() > 5) {
                                    lowestBINs.keySet().remove(lowestBINs.lastKey());
                                }
                            }

                            for(int j=0; j<bids.size(); j++) {
                                JsonObject bid = bids.get(j).getAsJsonObject();
                                if(bid.get("bidder").getAsString().equalsIgnoreCase(playerUUID)) {
                                    playerBids.add(auctionUuid);
                                }
                            }

                            if(checkItemType(item_lore, true, "DUNGEON") >= 0) {
                                HashMap<Integer, HashSet<String>> extrasMap = extrasToAucIdMap.computeIfAbsent("dungeon", k -> new HashMap<>());
                                HashSet<String> aucids = extrasMap.computeIfAbsent(0, k -> new HashSet<>());
                                aucids.add(auctionUuid);
                            }

                            //Categories
                            String category = sbCategory;
                            int itemType = checkItemType(item_lore, true,"SWORD", "FISHING ROD", "PICKAXE",
                                    "AXE", "SHOVEL", "PET ITEM", "TRAVEL SCROLL", "REFORGE STONE", "BOW");
                            if(itemType >= 0 && itemType < categoryItemType.length) {
                                category = categoryItemType[itemType];
                            }
                            if(extras.startsWith("Enchanted Book")) category = "ebook";
                            if(extras.endsWith("Potion")) category = "potion";
                            if(extras.contains("Rune")) category = "rune";
                            if(item_lore.split("\n")[0].endsWith("Furniture")) category = "furniture";
                            if(item_lore.split("\n")[0].endsWith("Pet") ||
                                    item_lore.split("\n")[0].endsWith("Mount")) category = "pet";

                            Auction auction1 = new Auction(auctioneerUuid, end, starting_bid, highest_bid_amount,
                                    bid_count, bin, category, rarity, item_tag);

                            if(tag.hasKey("ench")) {
                                auction1.enchLevel = 1;
                                if(tag.hasKey("ExtraAttributes", 10)) {
                                    NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");

                                    int hotpotatocount = ea.getInteger("hot_potato_count");
                                    if(hotpotatocount == 10) {
                                        auction1.enchLevel = 2;
                                    }
                                }
                            }

                            auction1.lastUpdate = System.currentTimeMillis();

                            auctionMap.put(auctionUuid, auction1);
                            internalnameToAucIdMap.computeIfAbsent(internalname, k -> new HashSet<>()).add(auctionUuid);
                        } catch(Exception e) {e.printStackTrace();}
                    }
                    processMillis = (int)(System.currentTimeMillis() - startProcess);
                }
            }
        );
    }

    private void resetNeedUpdate() {
        for(Integer page=0; page<totalPages; page++) {
            if(!needUpdate.contains(page)) {
                needUpdate.addLast(page);
            }
        }
    }

    /*ScheduledExecutorService auctionUpdateSES = Executors.newSingleThreadScheduledExecutor();
    private AtomicInteger auctionUpdateId = new AtomicInteger(0);
    public void updateAuctions() {
        HashMap<Integer, JsonObject> pages = new HashMap<>();

        HashMap<String, String> args = new HashMap<>();
        args.put("page", "0");
        AtomicInteger totalPages = new AtomicInteger(1);
        AtomicInteger currentPages = new AtomicInteger(0);
        manager.hypixelApi.getHypixelApiAsync(manager.config.apiKey.value, "skyblock/auctions",
                args, jsonObject -> {
                    if (jsonObject.get("success").getAsBoolean()) {
                        pages.put(0, jsonObject);
                        totalPages.set(jsonObject.get("totalPages").getAsInt());
                        currentPages.incrementAndGet();

                        for (int i = 1; i < totalPages.get(); i++) {
                            int j = i;
                            HashMap<String, String> args2 = new HashMap<>();
                            args2.put("page", "" + i);
                            manager.hypixelApi.getHypixelApiAsync(manager.config.apiKey.value, "skyblock/auctions",
                                    args2, jsonObject2 -> {
                                        if (jsonObject2.get("success").getAsBoolean()) {
                                            pages.put(j, jsonObject2);
                                            currentPages.incrementAndGet();
                                        } else {
                                            currentPages.incrementAndGet();
                                        }
                                    }
                            );
                        }
                    }
                }
        );

        long startTime = System.currentTimeMillis();

        int currentAuctionUpdateId = auctionUpdateId.incrementAndGet();

        auctionUpdateSES.schedule(new Runnable() {
            public void run() {
                if(auctionUpdateId.get() != currentAuctionUpdateId) return;
                System.out.println(currentPages.get() + "/" + totalPages.get());
                if (System.currentTimeMillis() - startTime > 20000) return;

                if (currentPages.get() == totalPages.get()) {
                    TreeMap<String, Auction> auctionItemsTemp = new TreeMap<>();

                    for (int pageNum : pages.keySet()) {
                        System.out.println(pageNum + "/" + pages.size());
                        JsonObject page = pages.get(pageNum);
                        JsonArray auctions = page.get("auctions").getAsJsonArray();
                        for (int i = 0; i < auctions.size(); i++) {
                            JsonObject auction = auctions.get(i).getAsJsonObject();

                            String auctionUuid = auction.get("uuid").getAsString();
                            String auctioneerUuid = auction.get("auctioneer").getAsString();
                            long end = auction.get("end").getAsLong();
                            int starting_bid = auction.get("starting_bid").getAsInt();
                            int highest_bid_amount = auction.get("highest_bid_amount").getAsInt();
                            int bid_count = auction.get("bids").getAsJsonArray().size();
                            boolean bin = false;
                            if(auction.has("bin")) {
                                bin = auction.get("bin").getAsBoolean();
                            }
                            String category = auction.get("category").getAsString();
                            String extras = auction.get("item_lore").getAsString().replaceAll("\n"," ") + " " +
                                    auction.get("extra").getAsString();
                            String item_bytes = auction.get("item_bytes").getAsString();

                            auctionItemsTemp.put(auctionUuid, new Auction(auctioneerUuid, end, starting_bid, highest_bid_amount,
                                    bid_count, bin, category, extras, item_bytes));
                        }
                    }
                    auctionItems = auctionItemsTemp;
                    customAH.updateSearch();
                    return;
                }

                auctionUpdateSES.schedule(this, 1000L, TimeUnit.MILLISECONDS);
            }
        }, 3000L, TimeUnit.MILLISECONDS);
    }*/

}
