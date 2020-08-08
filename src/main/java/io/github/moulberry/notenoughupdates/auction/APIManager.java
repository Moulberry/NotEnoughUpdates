package io.github.moulberry.notenoughupdates.auction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class APIManager {

    private NEUManager manager;
    public final CustomAH customAH;

    private TreeMap<String, Auction> auctionMap = new TreeMap<>();
    public HashMap<String, HashSet<String>> internalnameToAucIdMap = new HashMap<>();
    private HashSet<String> playerBids = new HashSet<>();
    private HashSet<String> playerBidsNotified = new HashSet<>();
    private HashSet<String> playerBidsFinishedNotified = new HashSet<>();

    private HashMap<String, TreeMap<Integer, String>> internalnameToLowestBIN = new HashMap<>();

    private LinkedList<Integer> pagesToDownload = null;

    public TreeMap<String, HashMap<Integer, HashSet<String>>> extrasToAucIdMap = new TreeMap<>();

    private long lastAuctionUpdate = 0;
    private long lastShortAuctionUpdate = 0;
    private long lastCustomAHSearch = 0;
    private long lastCleanup = 0;

    private long lastApiUpdate = 0;
    private long firstHypixelApiUpdate = 0;

    public int activeAuctions = 0;
    public int uniqueItems = 0;
    public int totalTags = 0;
    public int internalnameTaggedAuctions = 0;
    public int taggedAuctions = 0;
    public int processMillis = 0;

    public APIManager(NEUManager manager) {
        this.manager = manager;
        customAH = new CustomAH(manager);
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

    public void markNeedsUpdate() {
        firstHypixelApiUpdate = 0;
        pagesToDownload = null;
    }

    public void tick() {
        if(manager.config.apiKey.value == null || manager.config.apiKey.value.isEmpty()) return;

        customAH.tick();
        long currentTime = System.currentTimeMillis();
        if(currentTime - lastAuctionUpdate > 60*1000) {
            lastAuctionUpdate = currentTime;
            updatePageTick();
        }

        if(currentTime - lastShortAuctionUpdate > 10*1000) {
            lastShortAuctionUpdate = currentTime;
            updatePageTickShort();
            ahNotification();
        }
        /*if(currentTime - lastProfileUpdate > 10*1000) {
            lastProfileUpdate = System.currentTimeMillis();
            updateProfiles(Minecraft.getMinecraft().thePlayer.getUniqueID().toString().replace("-", ""));
        }*/
        if(currentTime - lastCleanup > 120*1000) {
            lastCleanup = currentTime;
            cleanup();
        }
        if(currentTime - lastCustomAHSearch > 60*1000) {
            lastCustomAHSearch = currentTime;
            if(Minecraft.getMinecraft().currentScreen instanceof CustomAHGui || customAH.isRenderOverAuctionView()) {
                customAH.updateSearch();
                calculateStats();
            }
        }
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

    private ExecutorService es = Executors.newSingleThreadExecutor();
    private void cleanup() {
        es.submit(() -> {
            try {
                long currTime = System.currentTimeMillis();
                Set<String> toRemove = new HashSet<>();
                for(Map.Entry<String, Auction> entry : auctionMap.entrySet()) {
                    long timeToEnd = entry.getValue().end - currTime;
                    if(timeToEnd < -120*1000) { //2 minutes
                        toRemove.add(entry.getKey());
                    }
                }
                toRemove.removeAll(playerBids);
                remove(toRemove);
            } catch(ConcurrentModificationException e) {
                lastCleanup = System.currentTimeMillis() - 110*1000;
            }
        });
    }

    private void remove(Set<String> toRemove) {
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
    }

    private void updatePageTickShort() {
        if(pagesToDownload == null || pagesToDownload.isEmpty()) return;

        if(firstHypixelApiUpdate == 0 || (System.currentTimeMillis() - firstHypixelApiUpdate)%(60*1000) > 15*1000) return;

        JsonObject disable = Utils.getConstant("disable");
        if(disable != null && disable.get("auctions").getAsBoolean()) return;

        while(!pagesToDownload.isEmpty()) {
            int page = pagesToDownload.pop();
            getPageFromAPI(page);
        }
    }

    private void updatePageTick() {
        JsonObject disable = Utils.getConstant("disable");
        if(disable != null && disable.get("auctions").getAsBoolean()) return;

        if(pagesToDownload == null) {
            getPageFromAPI(0);
        }

        manager.hypixelApi.getApiGZIPAsync("http://moulberry.codes/auction.json.gz", jsonObject -> {
            if(jsonObject.get("success").getAsBoolean()) {
                long apiUpdate = (long)jsonObject.get("time").getAsFloat();
                if(lastApiUpdate == apiUpdate) {
                    lastAuctionUpdate -= 30*1000;
                }
                lastApiUpdate = apiUpdate;

                JsonArray new_auctions = jsonObject.get("new_auctions").getAsJsonArray();
                for(JsonElement auctionElement : new_auctions) {
                    JsonObject auction = auctionElement.getAsJsonObject();
                    //System.out.println("New auction " + auction);
                    processAuction(auction);
                }
                JsonArray new_bids = jsonObject.get("new_bids").getAsJsonArray();
                for(JsonElement newBidElement : new_bids) {
                    JsonObject newBid = newBidElement.getAsJsonObject();
                    String newBidUUID = newBid.get("uuid").getAsString();
                    //System.out.println("new bid" + newBidUUID);
                    int newBidAmount = newBid.get("highest_bid_amount").getAsInt();
                    int end = newBid.get("end").getAsInt();
                    int bid_count = newBid.get("bid_count").getAsInt();

                    Auction auc = auctionMap.get(newBidUUID);
                    if(auc != null) {
                        //System.out.println("Setting auction " + newBidUUID + " price to " + newBidAmount);
                        auc.highest_bid_amount = newBidAmount;
                        auc.end = end;
                        auc.bid_count = bid_count;
                    }
                }
                Set<String> toRemove = new HashSet<>();
                JsonArray removed_auctions = jsonObject.get("removed_auctions").getAsJsonArray();
                for(JsonElement removedAuctionsElement : removed_auctions) {
                    String removed = removedAuctionsElement.getAsString();
                    toRemove.add(removed);
                }
                remove(toRemove);
            }
        }, () -> {
            System.out.println("Error downloading auction from Moulberry's jank API. :(");
        });
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

    private String[] romans = new String[]{"I","II","III","IV","V","VI","VII","VIII","IX","X","XI",
            "XII","XIII","XIV","XV","XVI","XVII","XIX","XX"};


    String[] categoryItemType = new String[]{"sword","fishingrod","pickaxe","axe",
            "shovel","petitem","travelscroll","reforgestone","bow"};
    String playerUUID = null;
    private void processAuction(JsonObject auction) {
        if(playerUUID == null) playerUUID = Minecraft.getMinecraft().thePlayer.getUniqueID().toString().replaceAll("-","");

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
        String extras = auction.get("extra").getAsString().toLowerCase();
        String item_name = auction.get("item_name").getAsString();
        String item_lore = Utils.fixBrokenAPIColour(auction.get("item_lore").getAsString());
        String item_bytes = auction.get("item_bytes").getAsString();
        String rarity = auction.get("tier").getAsString();
        JsonArray bids = auction.get("bids").getAsJsonArray();

        try {
            NBTTagCompound item_tag;
            try {
                item_tag = CompressedStreamTools.readCompressed(
                        new ByteArrayInputStream(Base64.getDecoder().decode(item_bytes)));
            } catch(IOException e) { return; }

            NBTTagCompound tag = item_tag.getTagList("i", 10).getCompoundTagAt(0).getCompoundTag("tag");
            String internalname = manager.getInternalnameFromNBT(tag);

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

            if(tag.hasKey("ExtraAttributes", 10)) {
                NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");

                if(ea.hasKey("enchantments", 10)) {
                    NBTTagCompound enchantments = ea.getCompoundTag("enchantments");
                    for(String key : enchantments.getKeySet()) {
                        String enchantname = key.toLowerCase().replace("_", " ");
                        int enchantlevel = enchantments.getInteger(key);
                        String enchantLevelStr;
                        if(enchantlevel >= 1 && enchantlevel <= 20) {
                            enchantLevelStr = romans[enchantlevel-1];
                        } else {
                            enchantLevelStr = String.valueOf(enchantlevel);
                        }
                        extras = extras.replace(enchantname, enchantname + " " + enchantLevelStr);
                    }
                }
            }

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
            if(category.equals("consumables") && extras.contains("enchanted book")) category = "ebook";
            if(category.equals("consumables") && extras.endsWith("potion")) category = "potion";
            if(category.equals("misc") && extras.contains("rune")) category = "rune";
            if(category.equals("misc") && item_lore.split("\n")[0].endsWith("Furniture")) category = "furniture";
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

            auctionMap.put(auctionUuid, auction1);
            internalnameToAucIdMap.computeIfAbsent(internalname, k -> new HashSet<>()).add(auctionUuid);
        } catch(Exception e) {e.printStackTrace();}
    }

    private void getPageFromAPI(int page) {
        System.out.println("downloading page:"+page);
        //System.out.println("Trying to update page: " + page);
        HashMap<String, String> args = new HashMap<>();
        args.put("page", ""+page);
        manager.hypixelApi.getHypixelApiAsync(manager.config.apiKey.value, "skyblock/auctions",
            args, jsonObject -> {
                    if(jsonObject == null) return;

                    if (jsonObject.get("success").getAsBoolean()) {
                        if(pagesToDownload == null) {
                            int totalPages = jsonObject.get("totalPages").getAsInt();
                            pagesToDownload = new LinkedList<>();
                            for(int i=0; i<totalPages; i++) {
                                pagesToDownload.add(i);
                            }
                        }
                        if(firstHypixelApiUpdate == 0) {
                            firstHypixelApiUpdate = jsonObject.get("lastUpdated").getAsLong();
                        }
                        activeAuctions = jsonObject.get("totalAuctions").getAsInt();

                        long startProcess = System.currentTimeMillis();
                        JsonArray auctions = jsonObject.get("auctions").getAsJsonArray();
                        for (int i = 0; i < auctions.size(); i++) {
                            JsonObject auction = auctions.get(i).getAsJsonObject();

                            processAuction(auction);
                        }
                        processMillis = (int)(System.currentTimeMillis() - startProcess);
                    } else {
                        pagesToDownload.addLast(page);
                    }
                }, () -> {
                    pagesToDownload.addLast(page);
                }
        );
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
