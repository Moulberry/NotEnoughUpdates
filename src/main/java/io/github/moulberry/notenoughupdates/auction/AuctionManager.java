package io.github.moulberry.notenoughupdates.auction;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumChatFormatting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionManager {

    private NEUManager manager;
    public final CustomAH customAH;

    private int totalPages = 0;
    private int lastApiUpdate;
    private LinkedList<Integer> needUpdate = new LinkedList<>();

    private TreeMap<String, Auction> auctionMap = new TreeMap<>();
    public HashMap<String, HashSet<String>> internalnameToAucIdMap = new HashMap<>();
    private HashSet<String> playerBids = new HashSet<>();

    public TreeMap<String, HashSet<String>> extrasToAucIdMap = new TreeMap<>();

    private long lastPageUpdate = 0;
    private long lastCustomAHSearch = 0;
    private long lastCleanup = 0;

    public int activeAuctions = 0;
    public int uniqueItems = 0;
    public int totalTags = 0;
    public int internalnameTaggedAuctions = 0;
    public int taggedAuctions = 0;
    public int processMillis = 0;

    public AuctionManager(NEUManager manager) {
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

    public void tick() {
        customAH.tick();
        if(System.currentTimeMillis() - lastPageUpdate > 5*1000) {
            lastPageUpdate = System.currentTimeMillis();
            updatePageTick();
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
            for(HashSet<String> aucids : extrasToAucIdMap.values()) {
                for(String aucid : toRemove) {
                    aucids.remove(aucid);
                }
            }
            for(HashSet<String> aucids : internalnameToAucIdMap.values()) {
                aucids.removeAll(toRemove);
            }
            playerBids.removeAll(toRemove);
        } catch(ConcurrentModificationException e) {
            cleanup();
        }
    }

    private void updatePageTick() {
        if(totalPages == 0) {
            getPageFromAPI(0);
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
            for(HashSet<String> aucids : extrasToAucIdMap.values()) {
                aucs.addAll(aucids);
            }
            taggedAuctions = aucs.size();
        } catch(Exception e) {}
    }

    String[] rarityArr = new String[] {
       "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "SPECIAL", "VERY SPECIAL",
    };

    public int checkItemType(String lore, String... typeMatches) {
        String[] split = lore.split("\n");
        for(int i=split.length-1; i>=0; i--) {
            String line = split[i];
            for(String rarity : rarityArr) {
                for(int j=0; j<typeMatches.length; j++) {
                    if(line.trim().endsWith(rarity + " " + typeMatches[j])) {
                        return j;
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

                            for(String str : extras.replaceAll(displayNormal, "").split(" ")) {
                                str = Utils.cleanColour(str).toLowerCase();
                                if(str.length() > 0) {
                                    HashSet<String> aucids = extrasToAucIdMap.computeIfAbsent(str, k -> new HashSet<>());
                                    aucids.add(auctionUuid);
                                }
                            }

                            for(int j=0; j<bids.size(); j++) {
                                JsonObject bid = bids.get(j).getAsJsonObject();
                                if(bid.get("bidder").getAsString().equalsIgnoreCase(playerUUID)) {
                                    playerBids.add(auctionUuid);
                                }
                            }

                            //Categories
                            String category = sbCategory;
                            int itemType = checkItemType(item_lore, "SWORD", "FISHING ROD", "PICKAXE",
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
