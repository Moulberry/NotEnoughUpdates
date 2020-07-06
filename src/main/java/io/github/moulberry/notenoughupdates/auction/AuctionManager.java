package io.github.moulberry.notenoughupdates.auction;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

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

    public TreeMap<String, HashMap<String, List<Integer>>> extrasToAucIdMap = new TreeMap<>();

    private long lastPageUpdate = 0;
    private long lastCustomAHSearch = 0;
    private long lastCleanup = 0;

    public AuctionManager(NEUManager manager) {
        this.manager = manager;
        customAH = new CustomAH(manager);
    }

    public TreeMap<String, Auction> getAuctionItems() {
        return auctionMap;
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
        public String item_bytes;
        private ItemStack stack;

        public Auction(String auctioneerUuid, long end, int starting_bid, int highest_bid_amount, int bid_count,
                       boolean bin, String category, String rarity, String item_bytes) {
            this.auctioneerUuid = auctioneerUuid;
            this.end = end;
            this.starting_bid = starting_bid;
            this.highest_bid_amount = highest_bid_amount;
            this.bid_count = bid_count;
            this.bin = bin;
            this.category = category;
            this.rarity = rarity;
            this.item_bytes = item_bytes;
        }

        public ItemStack getStack() {
            if(stack != null) {
                return stack;
            } else {
                JsonObject item = manager.getJsonFromItemBytes(item_bytes);
                ItemStack stack = manager.jsonToStack(item, false);
                this.stack = stack;
                return stack;
            }
        }
    }

    public void tick() {
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
            customAH.updateSearch();
        }
    }

    private void cleanup() {
        try {
            Set<String> toRemove = new HashSet<>();
            for(Map.Entry<String, Auction> entry : auctionMap.entrySet()) {
                long timeToEnd = entry.getValue().end - System.currentTimeMillis();
                if(timeToEnd < -60) {
                    toRemove.add(entry.getKey());
                }
            }
            for(String aucid : toRemove) {
                auctionMap.remove(aucid);
                extrasToAucIdMap.remove(aucid);
            }
        } catch(ConcurrentModificationException e) {
            cleanup();
        }
    }

    private void updatePageTick() {
        if(totalPages == 0) {
            getPageFromAPI(0);
        } else {
            if(needUpdate.isEmpty()) resetNeedUpdate();

            int pageToUpdate;
            for(pageToUpdate = needUpdate.pop(); pageToUpdate >= totalPages && !needUpdate.isEmpty();
                pageToUpdate = needUpdate.pop()) {}

            getPageFromAPI(pageToUpdate);
        }
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
                    //pages.put(page, jsonObject);
                    totalPages = jsonObject.get("totalPages").getAsInt();

                    int lastUpdated = jsonObject.get("lastUpdated").getAsInt();

                    if(lastApiUpdate != lastUpdated) {
                        resetNeedUpdate();
                    }

                    lastApiUpdate = lastUpdated;

                    String[] categoryItemType = new String[]{"sword","fishingrod","pickaxe","axe",
                            "shovel","petitem","travelscroll","reforgestone","bow"};

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
                        String item_lore = auction.get("item_lore").getAsString();
                        String item_bytes = auction.get("item_bytes").getAsString();
                        String rarity = auction.get("tier").getAsString();

                        String tag = extras + " " + Utils.cleanColour(item_lore).replaceAll("\n", " ");

                        int wordIndex=0;
                        for(String str : tag.split(" ")) {
                            str = Utils.cleanColour(str).toLowerCase();
                            if(!extrasToAucIdMap.containsKey(str)) {
                                extrasToAucIdMap.put(str, new HashMap<>());
                            }
                            if(!extrasToAucIdMap.get(str).containsKey(auctionUuid)) {
                                extrasToAucIdMap.get(str).put(auctionUuid, new ArrayList<>());
                            }
                            extrasToAucIdMap.get(str).get(auctionUuid).add(wordIndex);
                            wordIndex++;
                        }

                        //Categories
                        String category = sbCategory; //§6§lLEGENDARY FISHING ROD
                        int itemType = checkItemType(item_lore, "SWORD", "FISHING ROD", "PICKAXE",
                                "AXE", "SHOVEL", "PET ITEM", "TRAVEL SCROLL", "REFORGE STONE", "BOW");
                        if(itemType >= 0 && itemType < categoryItemType.length) {
                            category = categoryItemType[itemType];
                        }
                        if(extras.startsWith("Enchanted Book")) category = "ebook";
                        if(extras.endsWith("Potion")) category = "potion";
                        if(extras.contains("Rune")) category = "rune";
                        if(item_lore.substring(2).startsWith("Furniture")) category = "furniture";
                        if(item_lore.split("\n")[0].endsWith("Pet") ||
                                item_lore.split("\n")[0].endsWith("Mount")) category = "pet";

                        auctionMap.put(auctionUuid, new Auction(auctioneerUuid, end, starting_bid, highest_bid_amount,
                                bid_count, bin, category, rarity, item_bytes));
                    }
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
