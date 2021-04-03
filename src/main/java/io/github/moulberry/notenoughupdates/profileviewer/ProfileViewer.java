package io.github.moulberry.notenoughupdates.profileviewer;

import com.google.common.base.Splitter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.Constants;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileViewer {

    private final NEUManager manager;

    public ProfileViewer(NEUManager manager) {
        this.manager = manager;
    }


    private static HashMap<String, String> petRarityToNumMap = new HashMap<>();
    static {
        petRarityToNumMap.put("COMMON", "0");
        petRarityToNumMap.put("UNCOMMON", "1");
        petRarityToNumMap.put("RARE", "2");
        petRarityToNumMap.put("EPIC", "3");
        petRarityToNumMap.put("LEGENDARY", "4");
    }

    private static final LinkedHashMap<String, ItemStack> skillToSkillDisplayMap = new LinkedHashMap<>();
    static {
        skillToSkillDisplayMap.put("skill_taming", Utils.createItemStack(Items.spawn_egg, EnumChatFormatting.LIGHT_PURPLE+"Taming"));
        skillToSkillDisplayMap.put("skill_mining", Utils.createItemStack(Items.stone_pickaxe, EnumChatFormatting.GRAY+"Mining"));
        skillToSkillDisplayMap.put("skill_foraging", Utils.createItemStack(Item.getItemFromBlock(Blocks.sapling), EnumChatFormatting.DARK_GREEN+"Foraging"));
        skillToSkillDisplayMap.put("skill_enchanting", Utils.createItemStack(Item.getItemFromBlock(Blocks.enchanting_table), EnumChatFormatting.GREEN+"Enchanting"));
        skillToSkillDisplayMap.put("skill_carpentry", Utils.createItemStack(Item.getItemFromBlock(Blocks.crafting_table), EnumChatFormatting.DARK_RED+"Carpentry"));
        skillToSkillDisplayMap.put("skill_farming", Utils.createItemStack(Items.golden_hoe, EnumChatFormatting.YELLOW+"Farming"));
        skillToSkillDisplayMap.put("skill_combat", Utils.createItemStack(Items.stone_sword, EnumChatFormatting.RED+"Combat"));
        skillToSkillDisplayMap.put("skill_fishing", Utils.createItemStack(Items.fishing_rod, EnumChatFormatting.AQUA+"Fishing"));
        skillToSkillDisplayMap.put("skill_alchemy", Utils.createItemStack(Items.brewing_stand, EnumChatFormatting.BLUE+"Alchemy"));
        skillToSkillDisplayMap.put("skill_runecrafting", Utils.createItemStack(Items.magma_cream, EnumChatFormatting.DARK_PURPLE+"Runecrafting"));
        //skillToSkillDisplayMap.put("skill_catacombs", Utils.createItemStack(Item.getItemFromBlock(Blocks.deadbush), EnumChatFormatting.GOLD+"Catacombs"));
        skillToSkillDisplayMap.put("slayer_zombie", Utils.createItemStack(Items.rotten_flesh, EnumChatFormatting.GOLD+"Rev Slayer"));
        skillToSkillDisplayMap.put("slayer_spider", Utils.createItemStack(Items.spider_eye, EnumChatFormatting.GOLD+"Tara Slayer"));
        skillToSkillDisplayMap.put("slayer_wolf", Utils.createItemStack(Items.bone, EnumChatFormatting.GOLD+"Sven Slayer"));
    }

    private static final ItemStack CAT_FARMING = Utils.createItemStack(Items.golden_hoe, EnumChatFormatting.YELLOW+"Farming");
    private static final ItemStack CAT_MINING = Utils.createItemStack(Items.stone_pickaxe, EnumChatFormatting.GRAY+"Mining");
    private static final ItemStack CAT_COMBAT = Utils.createItemStack(Items.stone_sword, EnumChatFormatting.RED+"Combat");
    private static final ItemStack CAT_FORAGING = Utils.createItemStack(Item.getItemFromBlock(Blocks.sapling), EnumChatFormatting.DARK_GREEN+"Foraging");
    private static final ItemStack CAT_FISHING = Utils.createItemStack(Items.fishing_rod, EnumChatFormatting.AQUA+"Fishing");

    private static final LinkedHashMap<ItemStack, List<String>> collectionCatToCollectionMap = new LinkedHashMap<>();
    static {
        collectionCatToCollectionMap.put(CAT_FARMING,
                Utils.createList("WHEAT", "CARROT_ITEM", "POTATO_ITEM", "PUMPKIN", "MELON", "SEEDS", "MUSHROOM_COLLECTION",
                        "INK_SACK:3", "CACTUS", "SUGAR_CANE", "FEATHER", "LEATHER", "PORK", "RAW_CHICKEN", "MUTTON",
                        "RABBIT", "NETHER_STALK"));
        collectionCatToCollectionMap.put(CAT_MINING,
                Utils.createList("COBBLESTONE", "COAL", "IRON_INGOT", "GOLD_INGOT", "DIAMOND", "INK_SACK:4",
                        "EMERALD", "REDSTONE", "QUARTZ", "OBSIDIAN", "GLOWSTONE_DUST", "GRAVEL", "ICE", "NETHERRACK",
                        "SAND", "ENDER_STONE"));
        collectionCatToCollectionMap.put(CAT_COMBAT,
                Utils.createList("ROTTEN_FLESH", "BONE", "STRING", "SPIDER_EYE", "SULPHUR", "ENDER_PEARL",
                        "GHAST_TEAR", "SLIME_BALL", "BLAZE_ROD", "MAGMA_CREAM"));
        collectionCatToCollectionMap.put(CAT_FORAGING,
                Utils.createList("LOG", "LOG:1", "LOG:2", "LOG_2:1", "LOG_2", "LOG:3"));
        collectionCatToCollectionMap.put(CAT_FISHING,
                Utils.createList("RAW_FISH", "RAW_FISH:1", "RAW_FISH:2", "RAW_FISH:3", "PRISMARINE_SHARD",
                        "PRISMARINE_CRYSTALS", "CLAY_BALL", "WATER_LILY", "INK_SACK", "SPONGE"));
    }

    private static final LinkedHashMap<ItemStack, List<String>> collectionCatToMinionMap = new LinkedHashMap<>();
    static {
        collectionCatToMinionMap.put(CAT_FARMING,
                Utils.createList("WHEAT", "CARROT", "POTATO", "PUMPKIN", "MELON", null, "MUSHROOM",
                        "COCOA", "CACTUS", "SUGAR_CANE", "CHICKEN", "COW", "PIG", null, "SHEEP",
                        "RABBIT", "NETHER_WARTS"));
        collectionCatToMinionMap.put(CAT_MINING,
                Utils.createList("COBBLESTONE", "COAL", "IRON", "GOLD", "DIAMOND", "LAPIS",
                        "EMERALD", "REDSTONE", "QUARTZ", "OBSIDIAN", "GLOWSTONE", "GRAVEL", "ICE", null,
                        "SAND", "ENDER_STONE"));
        collectionCatToMinionMap.put(CAT_COMBAT,
                Utils.createList("ZOMBIE", "SKELETON", "SPIDER", "CAVESPIDER", "CREEPER", "ENDERMAN",
                        "GHAST", "SLIME", "BLAZE", "MAGMA_CUBE"));
        collectionCatToMinionMap.put(CAT_FORAGING,
                Utils.createList("OAK", "SPRUCE", "BIRCH", "DARK_OAK", "ACACIA", "JUNGLE"));
        collectionCatToMinionMap.put(CAT_FISHING,
                Utils.createList("FISHING", null, null, null, null,
                        null, "CLAY", null, null, null));
    }

    private static final LinkedHashMap<String, ItemStack> collectionToCollectionDisplayMap = new LinkedHashMap<>();
    static {
        /** FARMING COLLECTIONS **/
        collectionToCollectionDisplayMap.put("WHEAT", Utils.createItemStack(Items.wheat,
                EnumChatFormatting.YELLOW+"Wheat"));
        collectionToCollectionDisplayMap.put("CARROT_ITEM", Utils.createItemStack(Items.carrot,
                EnumChatFormatting.YELLOW+"Carrot"));
        collectionToCollectionDisplayMap.put("POTATO_ITEM", Utils.createItemStack(Items.potato,
                EnumChatFormatting.YELLOW+"Potato"));
        collectionToCollectionDisplayMap.put("PUMPKIN", Utils.createItemStack(Item.getItemFromBlock(Blocks.pumpkin),
                EnumChatFormatting.YELLOW+"Pumpkin"));
        collectionToCollectionDisplayMap.put("MELON", Utils.createItemStack(Items.melon,
                EnumChatFormatting.YELLOW+"Melon"));
        collectionToCollectionDisplayMap.put("SEEDS", Utils.createItemStack(Items.wheat_seeds,
                EnumChatFormatting.YELLOW+"Seeds"));
        collectionToCollectionDisplayMap.put("MUSHROOM_COLLECTION",
                Utils.createItemStack(Item.getItemFromBlock(Blocks.red_mushroom)
                        , EnumChatFormatting.YELLOW+"Mushroom"));
        collectionToCollectionDisplayMap.put("INK_SACK:3", Utils.createItemStack(Items.dye,
                EnumChatFormatting.YELLOW+"Cocoa Beans", 3));
        collectionToCollectionDisplayMap.put("CACTUS", Utils.createItemStack(Item.getItemFromBlock(Blocks.cactus),
                EnumChatFormatting.YELLOW+"Cactus"));
        collectionToCollectionDisplayMap.put("SUGAR_CANE", Utils.createItemStack(Items.reeds,
                EnumChatFormatting.YELLOW+"Sugar Cane"));
        collectionToCollectionDisplayMap.put("FEATHER", Utils.createItemStack(Items.feather,
                EnumChatFormatting.YELLOW+"Feather"));
        collectionToCollectionDisplayMap.put("LEATHER", Utils.createItemStack(Items.leather,
                EnumChatFormatting.YELLOW+"Leather"));
        collectionToCollectionDisplayMap.put("PORK", Utils.createItemStack(Items.porkchop,
                EnumChatFormatting.YELLOW+"Porkchop"));
        collectionToCollectionDisplayMap.put("RAW_CHICKEN", Utils.createItemStack(Items.chicken,
                EnumChatFormatting.YELLOW+"Chicken"));
        collectionToCollectionDisplayMap.put("MUTTON", Utils.createItemStack(Items.mutton,
                EnumChatFormatting.YELLOW+"Mutton"));
        collectionToCollectionDisplayMap.put("RABBIT", Utils.createItemStack(Items.rabbit,
                EnumChatFormatting.YELLOW+"Rabbit"));
        collectionToCollectionDisplayMap.put("NETHER_STALK", Utils.createItemStack(Items.nether_wart,
                EnumChatFormatting.YELLOW+"Nether Wart"));

        /** MINING COLLECTIONS **/
        collectionToCollectionDisplayMap.put("COBBLESTONE", Utils.createItemStack(Item.getItemFromBlock(Blocks.cobblestone),
                EnumChatFormatting.GRAY+"Cobblestone"));
        collectionToCollectionDisplayMap.put("COAL", Utils.createItemStack(Items.coal,
                EnumChatFormatting.GRAY+"Coal"));
        collectionToCollectionDisplayMap.put("IRON_INGOT", Utils.createItemStack(Items.iron_ingot,
                EnumChatFormatting.GRAY+"Iron Ingot"));
        collectionToCollectionDisplayMap.put("GOLD_INGOT", Utils.createItemStack(Items.gold_ingot,
                EnumChatFormatting.GRAY+"Gold Ingot"));
        collectionToCollectionDisplayMap.put("DIAMOND", Utils.createItemStack(Items.diamond,
                EnumChatFormatting.GRAY+"Diamond"));
        collectionToCollectionDisplayMap.put("INK_SACK:4", Utils.createItemStack(Items.dye,
                EnumChatFormatting.GRAY+"Lapis Lazuli", 4));
        collectionToCollectionDisplayMap.put("EMERALD", Utils.createItemStack(Items.emerald,
                EnumChatFormatting.GRAY+"Emerald"));
        collectionToCollectionDisplayMap.put("REDSTONE", Utils.createItemStack(Items.redstone,
                EnumChatFormatting.GRAY+"Redstone"));
        collectionToCollectionDisplayMap.put("QUARTZ", Utils.createItemStack(Items.quartz,
                EnumChatFormatting.GRAY+"Nether Quartz"));
        collectionToCollectionDisplayMap.put("OBSIDIAN", Utils.createItemStack(Item.getItemFromBlock(Blocks.obsidian),
                EnumChatFormatting.GRAY+"Obsidian"));
        collectionToCollectionDisplayMap.put("GLOWSTONE_DUST", Utils.createItemStack(Items.glowstone_dust,
                EnumChatFormatting.GRAY+"Glowstone"));
        collectionToCollectionDisplayMap.put("GRAVEL", Utils.createItemStack(Item.getItemFromBlock(Blocks.gravel),
                EnumChatFormatting.GRAY+"Gravel"));
        collectionToCollectionDisplayMap.put("ICE", Utils.createItemStack(Item.getItemFromBlock(Blocks.ice),
                EnumChatFormatting.GRAY+"Ice"));
        collectionToCollectionDisplayMap.put("NETHERRACK", Utils.createItemStack(Item.getItemFromBlock(Blocks.netherrack),
                EnumChatFormatting.GRAY+"Netherrack"));
        collectionToCollectionDisplayMap.put("SAND", Utils.createItemStack(Item.getItemFromBlock(Blocks.sand),
                EnumChatFormatting.GRAY+"Sand"));
        collectionToCollectionDisplayMap.put("ENDER_STONE", Utils.createItemStack(Item.getItemFromBlock(Blocks.end_stone),
                EnumChatFormatting.GRAY+"End Stone"));

        /** COMBAT COLLECTIONS **/
        collectionToCollectionDisplayMap.put("ROTTEN_FLESH", Utils.createItemStack(Items.rotten_flesh,
                EnumChatFormatting.RED+"Rotten Flesh"));
        collectionToCollectionDisplayMap.put("BONE", Utils.createItemStack(Items.bone,
                EnumChatFormatting.RED+"Bone"));
        collectionToCollectionDisplayMap.put("STRING", Utils.createItemStack(Items.string,
                EnumChatFormatting.RED+"String"));
        collectionToCollectionDisplayMap.put("SPIDER_EYE", Utils.createItemStack(Items.spider_eye,
                EnumChatFormatting.RED+"Spider Eye"));
        collectionToCollectionDisplayMap.put("SULPHUR", Utils.createItemStack(Items.gunpowder,
                EnumChatFormatting.RED+"Gunpowder"));
        collectionToCollectionDisplayMap.put("ENDER_PEARL", Utils.createItemStack(Items.ender_pearl,
                EnumChatFormatting.RED+"Ender Pearl"));
        collectionToCollectionDisplayMap.put("GHAST_TEAR", Utils.createItemStack(Items.ghast_tear,
                EnumChatFormatting.RED+"Ghast Tear"));
        collectionToCollectionDisplayMap.put("SLIME_BALL", Utils.createItemStack(Items.slime_ball,
                EnumChatFormatting.RED+"Slimeball"));
        collectionToCollectionDisplayMap.put("BLAZE_ROD", Utils.createItemStack(Items.blaze_rod,
                EnumChatFormatting.RED+"Blaze Rod"));
        collectionToCollectionDisplayMap.put("MAGMA_CREAM", Utils.createItemStack(Items.magma_cream,
                EnumChatFormatting.RED+"Magma Cream"));

        /** FORAGING COLLECTIONS **/
        collectionToCollectionDisplayMap.put("LOG", Utils.createItemStack(Item.getItemFromBlock(Blocks.log),
                EnumChatFormatting.DARK_GREEN+"Oak"));
        collectionToCollectionDisplayMap.put("LOG:1", Utils.createItemStack(Item.getItemFromBlock(Blocks.log),
                EnumChatFormatting.DARK_GREEN+"Birch", 1));
        collectionToCollectionDisplayMap.put("LOG:2", Utils.createItemStack(Item.getItemFromBlock(Blocks.log),
                EnumChatFormatting.DARK_GREEN+"Spruce", 2));
        collectionToCollectionDisplayMap.put("LOG_2:1", Utils.createItemStack(Item.getItemFromBlock(Blocks.log2),
                EnumChatFormatting.DARK_GREEN+"Dark Oak", 1));
        collectionToCollectionDisplayMap.put("LOG_2", Utils.createItemStack(Item.getItemFromBlock(Blocks.log2),
                EnumChatFormatting.DARK_GREEN+"Acacia"));
        collectionToCollectionDisplayMap.put("LOG:3", Utils.createItemStack(Item.getItemFromBlock(Blocks.log),
                EnumChatFormatting.DARK_GREEN+"Jungle", 3));

        /** FISHING COLLECTIONS **/
        collectionToCollectionDisplayMap.put("RAW_FISH", Utils.createItemStack(Items.fish,
                EnumChatFormatting.AQUA+"Fish"));
        collectionToCollectionDisplayMap.put("RAW_FISH:1", Utils.createItemStack(Items.fish,
                EnumChatFormatting.AQUA+"Salmon", 1));
        collectionToCollectionDisplayMap.put("RAW_FISH:2", Utils.createItemStack(Items.fish,
                EnumChatFormatting.AQUA+"Clownfish", 2));
        collectionToCollectionDisplayMap.put("RAW_FISH:3", Utils.createItemStack(Items.fish,
                EnumChatFormatting.AQUA+"Pufferfish", 3));
        collectionToCollectionDisplayMap.put("PRISMARINE_SHARD", Utils.createItemStack(Items.prismarine_shard,
                EnumChatFormatting.AQUA+"Prismarine Shard"));
        collectionToCollectionDisplayMap.put("PRISMARINE_CRYSTALS", Utils.createItemStack(Items.prismarine_crystals,
                EnumChatFormatting.AQUA+"Prismarine Crystals"));
        collectionToCollectionDisplayMap.put("CLAY_BALL", Utils.createItemStack(Items.clay_ball,
                EnumChatFormatting.AQUA+"Clay"));
        collectionToCollectionDisplayMap.put("WATER_LILY", Utils.createItemStack(Item.getItemFromBlock(Blocks.waterlily),
                EnumChatFormatting.AQUA+"Lilypad"));
        collectionToCollectionDisplayMap.put("INK_SACK", Utils.createItemStack(Items.dye,
                EnumChatFormatting.AQUA+"Ink Sack"));
        collectionToCollectionDisplayMap.put("SPONGE", Utils.createItemStack(Item.getItemFromBlock(Blocks.sponge),
                EnumChatFormatting.AQUA+"Sponge"));
    }

    public static LinkedHashMap<ItemStack, List<String>> getCollectionCatToMinionMap() {
        return collectionCatToMinionMap;
    }

    public static LinkedHashMap<String, ItemStack> getCollectionToCollectionDisplayMap() {
        return collectionToCollectionDisplayMap;
    }

    public static LinkedHashMap<ItemStack, List<String>> getCollectionCatToCollectionMap() {
        return collectionCatToCollectionMap;
    }

    public static Map<String, ItemStack> getSkillToSkillDisplayMap() {
        return Collections.unmodifiableMap(skillToSkillDisplayMap);
    }

    public static class Level {
        public float level = 0;
        public float maxXpForLevel = 0;
        public boolean maxed = false;
    }

    public static Level getLevel(JsonArray levelingArray, float xp, int levelCap, boolean cumulative) {
        Level levelObj = new Level();
        for(int level=0; level<levelingArray.size(); level++) {
            float levelXp = levelingArray.get(level).getAsFloat();
            if(levelXp > xp) {
                if(cumulative) {
                    float previous = 0;
                    if(level > 0) previous = levelingArray.get(level-1).getAsFloat();
                    levelObj.maxXpForLevel = (levelXp-previous);
                    levelObj.level = 1 + level + (xp-levelXp)/levelObj.maxXpForLevel;
                } else {
                    levelObj.maxXpForLevel = levelXp;
                    levelObj.level = level + xp/levelXp;
                }
                if(levelObj.level > levelCap) {
                    levelObj.level = levelCap;
                    levelObj.maxed = true;
                }
                return levelObj;
            } else {
                if(!cumulative) xp -= levelXp;
            }
        }
        levelObj.level = levelingArray.size();
        if(levelObj.level > levelCap) {
            levelObj.level = levelCap;
        }
        levelObj.maxed = true;
        return levelObj;
    }

    public class Profile {
        private final String uuid;
        private String latestProfile = null;

        private JsonArray playerInformation = null;
        private JsonObject basicInfo = null;

        private final HashMap<String, JsonObject> profileMap = new HashMap<>();
        private final HashMap<String, JsonObject> petsInfoMap = new HashMap<>();
        private final HashMap<String, List<JsonObject>> coopProfileMap = new HashMap<>();
        private final HashMap<String, JsonObject> skillInfoMap = new HashMap<>();
        private final HashMap<String, JsonObject> inventoryInfoMap = new HashMap<>();
        private final HashMap<String, JsonObject> collectionInfoMap = new HashMap<>();
        private List<String> profileIds = new ArrayList<>();
        private JsonObject playerStatus = null;
        private HashMap<String, PlayerStats.Stats> stats = new HashMap<>();
        private HashMap<String, PlayerStats.Stats> passiveStats = new HashMap<>();
        private HashMap<String, Long> networth = new HashMap<>();

        public Profile(String uuid) {
            this.uuid = uuid;
        }

        private AtomicBoolean updatingPlayerInfoState = new AtomicBoolean(false);
        private long lastPlayerInfoState = 0;
        private AtomicBoolean updatingPlayerStatusState = new AtomicBoolean(false);

        public JsonObject getPlayerStatus() {
            if(playerStatus != null) return playerStatus;
            if(updatingPlayerStatusState.get()) return null;

            updatingPlayerStatusState.set(true);

            HashMap<String, String> args = new HashMap<>();
            args.put("uuid", ""+uuid);
            manager.hypixelApi.getHypixelApiAsync(NotEnoughUpdates.INSTANCE.config.apiKey.apiKey, "status",
                    args, jsonObject -> {
                        if(jsonObject == null) return;

                        updatingPlayerStatusState.set(false);
                        if(jsonObject.has("success") && jsonObject.get("success").getAsBoolean()) {
                            playerStatus = jsonObject.get("session").getAsJsonObject();
                        }
                    }, () -> updatingPlayerStatusState.set(false)
            );

            return null;
        }

        public long getNetWorth(String profileId) {
            if(profileId == null) profileId = latestProfile;
            if(networth.get(profileId) != null) return networth.get(profileId);
            if(getProfileInformation(profileId) == null) return -1;
            if(getInventoryInfo(profileId) == null) return -1;

            JsonObject inventoryInfo = getInventoryInfo(profileId);
            JsonObject profileInfo = getProfileInformation(profileId);

            HashMap<String, Long> mostExpensiveInternal = new HashMap<>();

            long networth = 0;
            for(Map.Entry<String, JsonElement> entry : inventoryInfo.entrySet()) {
                if(entry.getValue().isJsonArray()) {
                    for(JsonElement element : entry.getValue().getAsJsonArray()) {
                        if(element != null && element.isJsonObject()) {
                            JsonObject item = element.getAsJsonObject();
                            String internalname = item.get("internalname").getAsString();

                            if(manager.auctionManager.isVanillaItem(internalname)) continue;

                            JsonObject bzInfo = manager.auctionManager.getBazaarInfo(internalname);

                            int auctionPrice;
                            if(bzInfo != null && bzInfo.has("curr_sell")) {
                                auctionPrice = (int)bzInfo.get("curr_sell").getAsFloat();
                            } else {
                                auctionPrice = (int)manager.auctionManager.getItemAvgBin(internalname);
                                if(auctionPrice <= 0) {
                                    auctionPrice = manager.auctionManager.getLowestBin(internalname);
                                }
                            }

                            try {
                                if(item.has("item_contents")) {
                                    JsonArray bytesArr = item.get("item_contents").getAsJsonArray();
                                    byte[] bytes = new byte[bytesArr.size()];
                                    for (int bytesArrI = 0; bytesArrI < bytesArr.size(); bytesArrI++) {
                                        bytes[bytesArrI] = bytesArr.get(bytesArrI).getAsByte();
                                    }
                                    NBTTagCompound contents_nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes));
                                    NBTTagList items = contents_nbt.getTagList("i", 10);
                                    for(int j=0; j<items.tagCount(); j++) {
                                        if(items.getCompoundTagAt(j).getKeySet().size() > 0) {
                                            NBTTagCompound nbt = items.getCompoundTagAt(j).getCompoundTag("tag");
                                            String internalname2 = manager.getInternalnameFromNBT(nbt);
                                            if(internalname2 != null) {
                                                if(manager.auctionManager.isVanillaItem(internalname2)) continue;

                                                JsonObject bzInfo2 = manager.auctionManager.getBazaarInfo(internalname2);

                                                int auctionPrice2;
                                                if(bzInfo2 != null && bzInfo2.has("curr_sell")) {
                                                    auctionPrice2 = (int)bzInfo2.get("curr_sell").getAsFloat();
                                                } else {
                                                    auctionPrice2 = (int)manager.auctionManager.getItemAvgBin(internalname2);
                                                    if(auctionPrice2 <= 0) {
                                                        auctionPrice2 = manager.auctionManager.getLowestBin(internalname2);
                                                    }
                                                }

                                                int count2 = items.getCompoundTagAt(j).getByte("Count");

                                                mostExpensiveInternal.put(internalname2, auctionPrice2 * count2 + mostExpensiveInternal.getOrDefault(internalname2, 0L));
                                                networth += auctionPrice2 * count2;
                                            }
                                        }
                                    }
                                }
                            } catch(IOException ignored) {}

                            int count = 1;
                            if(element.getAsJsonObject().has("count")) {
                                count = element.getAsJsonObject().get("count").getAsInt();
                            }
                            mostExpensiveInternal.put(internalname, auctionPrice * count + mostExpensiveInternal.getOrDefault(internalname, 0L));
                            networth += auctionPrice * count;
                        }
                    }
                }
            }
            if(networth == 0) return -1;

            //System.out.println(profileId);
            for(Map.Entry<String, Long> entry : mostExpensiveInternal.entrySet()) {
                //System.out.println(entry.getKey() + ":" + entry.getValue());
            }

            networth = (int)(networth*1.3f);

            JsonObject petsInfo = getPetsInfo(profileId);
            if(petsInfo != null && petsInfo.has("pets")) {
                if(petsInfo.get("pets").isJsonArray()) {
                    JsonArray pets = petsInfo.get("pets").getAsJsonArray();
                    for(JsonElement element : pets) {
                        if(element.isJsonObject()) {
                            JsonObject pet = element.getAsJsonObject();

                            String petname = pet.get("type").getAsString();
                            String tier = pet.get("tier").getAsString();
                            String tierNum = petRarityToNumMap.get(tier);
                            if(tierNum != null) {
                                String internalname2 = petname+";"+tierNum;
                                JsonObject info2 = manager.auctionManager.getItemAuctionInfo(internalname2);
                                if(info2 == null || !info2.has("price") || !info2.has("count")) continue;
                                int auctionPrice2 = (int)(info2.get("price").getAsFloat() / info2.get("count").getAsFloat());

                                networth += auctionPrice2;
                            }
                        }
                    }
                }
            }

            float bankBalance = Utils.getElementAsFloat(Utils.getElement(profileInfo, "banking.balance"), 0);
            float purseBalance = Utils.getElementAsFloat(Utils.getElement(profileInfo, "coin_purse"), 0);

            networth += bankBalance+purseBalance;

            this.networth.put(profileId, networth);
            return networth;
        }

        public String getLatestProfile() {
            return latestProfile;
        }

        public JsonArray getPlayerInformation(Runnable runnable) {
            if (playerInformation != null) return playerInformation;

            long currentTime = System.currentTimeMillis();

            if (currentTime - lastPlayerInfoState < 15*1000 && updatingPlayerInfoState.get()) return null;

            lastPlayerInfoState = currentTime;
            updatingPlayerInfoState.set(true);

            HashMap<String, String> args = new HashMap<>();
            args.put("uuid", "" + uuid);
            manager.hypixelApi.getHypixelApiAsync(NotEnoughUpdates.INSTANCE.config.apiKey.apiKey, "skyblock/profiles",
                    args, jsonObject -> {
                        updatingPlayerInfoState.set(false);

                        if (jsonObject == null) return;
                        if (jsonObject.has("success") && jsonObject.get("success").getAsBoolean()) {
                            playerInformation = jsonObject.get("profiles").getAsJsonArray();
                            if (playerInformation == null) return;
                            String backup = null;
                            long backupLastSave = 0;

                            profileIds.clear();

                            for (int i = 0; i < playerInformation.size(); i++) {
                                JsonObject profile = playerInformation.get(i).getAsJsonObject();

                                if (!profile.has("members")) continue;
                                JsonObject members = profile.get("members").getAsJsonObject();

                                if (members.has(uuid)) {
                                    JsonObject member = members.get(uuid).getAsJsonObject();

                                    if(member.has("coop_invitation")) {
                                        JsonObject coop_invitation = member.get("coop_invitation").getAsJsonObject();
                                        if(!coop_invitation.get("confirmed").getAsBoolean()) {
                                            continue;
                                        }
                                    }

                                    String cute_name = profile.get("cute_name").getAsString();
                                    if (backup == null) backup = cute_name;
                                    profileIds.add(cute_name);
                                    if (member.has("last_save")) {
                                        long last_save = member.get("last_save").getAsLong();
                                        if (last_save > backupLastSave) {
                                            backupLastSave = last_save;
                                            backup = cute_name;
                                        }
                                    }

                                }
                            }
                            latestProfile = backup;
                            if (runnable != null) runnable.run();
                        }
                    }, () -> {
                        updatingPlayerInfoState.set(false);
                    }
            );

            return null;
        }

        public List<String> getProfileIds() {
            return profileIds;
        }

        public JsonObject getProfileInformation(String profileId) {
            JsonArray playerInfo = getPlayerInformation(() -> {});
            if(playerInfo == null) return null;
            if(profileId == null) profileId = latestProfile;
            if(profileMap.containsKey(profileId)) return profileMap.get(profileId);

            for(int i=0; i<playerInformation.size(); i++) {
                if(!playerInformation.get(i).isJsonObject()) {
                    playerInformation = null;
                    return null;
                }
                JsonObject profile = playerInformation.get(i).getAsJsonObject();
                if(profile.get("cute_name").getAsString().equalsIgnoreCase(profileId)) {
                    if(!profile.has("members")) return null;
                    JsonObject members = profile.get("members").getAsJsonObject();
                    if(!members.has(uuid)) continue;
                    JsonObject profileInfo = members.get(uuid).getAsJsonObject();
                    if(profile.has("banking")) {
                        profileInfo.add("banking", profile.get("banking").getAsJsonObject());
                    }
                    profileMap.put(profileId, profileInfo);
                    return profileInfo;
                }
            }

            return null;
        }

        public List<JsonObject> getCoopProfileInformation(String profileId) {
            JsonArray playerInfo = getPlayerInformation(() -> {});
            if(playerInfo == null) return null;
            if(profileId == null) profileId = latestProfile;
            if(coopProfileMap.containsKey(profileId)) return coopProfileMap.get(profileId);

            for(int i=0; i<playerInformation.size(); i++) {
                if(!playerInformation.get(i).isJsonObject()) {
                    playerInformation = null;
                    return null;
                }
                JsonObject profile = playerInformation.get(i).getAsJsonObject();
                if(profile.get("cute_name").getAsString().equalsIgnoreCase(profileId)) {
                    if(!profile.has("members")) return null;
                    JsonObject members = profile.get("members").getAsJsonObject();
                    if(!members.has(uuid)) return null;
                    List<JsonObject> coopList = new ArrayList<>();
                    for(Map.Entry<String, JsonElement> islandMember : members.entrySet()) {
                        if(!islandMember.getKey().equals(uuid)) {
                            JsonObject coopProfileInfo = islandMember.getValue().getAsJsonObject();
                            coopList.add(coopProfileInfo);
                        }
                    }
                    coopProfileMap.put(profileId, coopList);
                    return coopList;
                }
            }

            return null;
        }

        public void resetCache() {
            playerInformation = null;
            basicInfo = null;
            playerStatus = null;
            stats.clear();
            passiveStats.clear();
            profileIds.clear();
            profileMap.clear();
            coopProfileMap.clear();
            petsInfoMap.clear();
            skillInfoMap.clear();
            inventoryInfoMap.clear();
            collectionInfoMap.clear();
            networth.clear();
        }

        public int getCap(JsonObject leveling, String skillName) {
            JsonElement capsElement = Utils.getElement(leveling, "leveling_caps");
            if(capsElement == null || !capsElement.isJsonObject()) {
                return 50;
            }
            JsonObject caps = capsElement.getAsJsonObject();
            if(caps.has(skillName)) {
                return caps.get(skillName).getAsInt();
            }
            return 50;
        }

        public JsonObject getSkillInfo(String profileId) {
            JsonObject profileInfo = getProfileInformation(profileId);
            if(profileInfo == null) return null;
            if(profileId == null) profileId = latestProfile;
            if(skillInfoMap.containsKey(profileId)) return skillInfoMap.get(profileId);
            JsonObject leveling = Constants.LEVELING;
            if(leveling == null)  return null;

            float experience_skill_taming = Utils.getElementAsFloat(Utils.getElement(profileInfo, "experience_skill_taming"), 0);
            float experience_skill_mining = Utils.getElementAsFloat(Utils.getElement(profileInfo, "experience_skill_mining"), 0);
            float experience_skill_foraging = Utils.getElementAsFloat(Utils.getElement(profileInfo, "experience_skill_foraging"), 0);
            float experience_skill_enchanting = Utils.getElementAsFloat(Utils.getElement(profileInfo, "experience_skill_enchanting"), 0);
            float experience_skill_carpentry = Utils.getElementAsFloat(Utils.getElement(profileInfo, "experience_skill_carpentry"), 0);
            float experience_skill_farming = Utils.getElementAsFloat(Utils.getElement(profileInfo, "experience_skill_farming"), 0);
            float experience_skill_combat = Utils.getElementAsFloat(Utils.getElement(profileInfo, "experience_skill_combat"), 0);
            float experience_skill_fishing = Utils.getElementAsFloat(Utils.getElement(profileInfo, "experience_skill_fishing"), 0);
            float experience_skill_alchemy = Utils.getElementAsFloat(Utils.getElement(profileInfo, "experience_skill_alchemy"), 0);
            float experience_skill_runecrafting = Utils.getElementAsFloat(Utils.getElement(profileInfo, "experience_skill_runecrafting"), 0);

            float experience_skill_catacombs = Utils.getElementAsFloat(Utils.getElement(profileInfo, "dungeons.dungeon_types.catacombs.experience"), 0);

            float experience_slayer_zombie = Utils.getElementAsFloat(Utils.getElement(profileInfo, "slayer_bosses.zombie.xp"), 0);
            float experience_slayer_spider = Utils.getElementAsFloat(Utils.getElement(profileInfo, "slayer_bosses.spider.xp"), 0);
            float experience_slayer_wolf = Utils.getElementAsFloat(Utils.getElement(profileInfo, "slayer_bosses.wolf.xp"), 0);

            float totalSkillXP = experience_skill_taming + experience_skill_mining + experience_skill_foraging
                    + experience_skill_enchanting + experience_skill_carpentry + experience_skill_farming
                    + experience_skill_combat + experience_skill_fishing + experience_skill_alchemy
                    + experience_skill_runecrafting;

            if(totalSkillXP <= 0) {
                return null;
            }

            JsonObject skillInfo = new JsonObject();

            skillInfo.addProperty("experience_skill_taming", experience_skill_taming);
            skillInfo.addProperty("experience_skill_mining", experience_skill_mining);
            skillInfo.addProperty("experience_skill_foraging", experience_skill_foraging);
            skillInfo.addProperty("experience_skill_enchanting", experience_skill_enchanting);
            skillInfo.addProperty("experience_skill_carpentry", experience_skill_carpentry);
            skillInfo.addProperty("experience_skill_farming", experience_skill_farming);
            skillInfo.addProperty("experience_skill_combat", experience_skill_combat);
            skillInfo.addProperty("experience_skill_fishing", experience_skill_fishing);
            skillInfo.addProperty("experience_skill_alchemy", experience_skill_alchemy);
            skillInfo.addProperty("experience_skill_runecrafting", experience_skill_runecrafting);

            skillInfo.addProperty("experience_skill_catacombs", experience_skill_catacombs);

            skillInfo.addProperty("experience_slayer_zombie", experience_slayer_zombie);
            skillInfo.addProperty("experience_slayer_spider", experience_slayer_spider);
            skillInfo.addProperty("experience_slayer_wolf", experience_slayer_wolf);

            JsonArray levelingArray = Utils.getElement(leveling, "leveling_xp").getAsJsonArray();
            int farmingCap = getCap(leveling, "farming") + (int)Utils.getElementAsFloat(
                    Utils.getElement(profileInfo, "jacob2.perks.farming_level_cap"), 0);
            Level level_skill_taming = getLevel(levelingArray, experience_skill_taming, getCap(leveling, "taming"), false);
            Level level_skill_mining = getLevel(levelingArray, experience_skill_mining, getCap(leveling, "mining"), false);
            Level level_skill_foraging = getLevel(levelingArray, experience_skill_foraging, getCap(leveling, "foraging"), false);
            Level level_skill_enchanting = getLevel(levelingArray, experience_skill_enchanting, getCap(leveling, "enchanting"),  false);
            Level level_skill_carpentry = getLevel(levelingArray, experience_skill_carpentry,getCap(leveling, "carpetry"),  false);
            Level level_skill_farming = getLevel(levelingArray, experience_skill_farming, farmingCap, false);
            Level level_skill_combat = getLevel(levelingArray, experience_skill_combat, getCap(leveling, "combat"), false);
            Level level_skill_fishing = getLevel(levelingArray, experience_skill_fishing, getCap(leveling, "fishing"), false);
            Level level_skill_alchemy = getLevel(levelingArray, experience_skill_alchemy, getCap(leveling, "alchemy"), false);
            Level level_skill_runecrafting = getLevel(Utils.getElement(leveling, "runecrafting_xp").getAsJsonArray(),
                    experience_skill_runecrafting, getCap(leveling, "runecrafting"), false);

            Level level_skill_catacombs = getLevel(Utils.getElement(leveling, "catacombs").getAsJsonArray(),
                    experience_skill_catacombs, getCap(leveling, "catacombs"), false);

            Level level_slayer_zombie = getLevel(Utils.getElement(leveling, "slayer_xp.zombie").getAsJsonArray(),
                    experience_slayer_zombie, 9,true);
            Level level_slayer_spider = getLevel(Utils.getElement(leveling, "slayer_xp.spider").getAsJsonArray(),
                    experience_slayer_spider, 9,true);
            Level level_slayer_wolf = getLevel(Utils.getElement(leveling, "slayer_xp.wolf").getAsJsonArray(),
                    experience_slayer_wolf, 9,true);

            skillInfo.addProperty("level_skill_taming", level_skill_taming.level);
            skillInfo.addProperty("level_skill_mining", level_skill_mining.level);
            skillInfo.addProperty("level_skill_foraging", level_skill_foraging.level);
            skillInfo.addProperty("level_skill_enchanting", level_skill_enchanting.level);
            skillInfo.addProperty("level_skill_carpentry", level_skill_carpentry.level);
            skillInfo.addProperty("level_skill_farming", level_skill_farming.level);
            skillInfo.addProperty("level_skill_combat", level_skill_combat.level);
            skillInfo.addProperty("level_skill_fishing", level_skill_fishing.level);
            skillInfo.addProperty("level_skill_alchemy", level_skill_alchemy.level);
            skillInfo.addProperty("level_skill_runecrafting", level_skill_runecrafting.level);

            skillInfo.addProperty("level_skill_catacombs", level_skill_catacombs.level);

            skillInfo.addProperty("level_slayer_zombie", level_slayer_zombie.level);
            skillInfo.addProperty("level_slayer_spider", level_slayer_spider.level);
            skillInfo.addProperty("level_slayer_wolf", level_slayer_wolf.level);

            skillInfo.addProperty("maxed_skill_taming", level_skill_taming.maxed);
            skillInfo.addProperty("maxed_skill_mining", level_skill_mining.maxed);
            skillInfo.addProperty("maxed_skill_foraging", level_skill_foraging.maxed);
            skillInfo.addProperty("maxed_skill_enchanting", level_skill_enchanting.maxed);
            skillInfo.addProperty("maxed_skill_carpentry", level_skill_carpentry.maxed);
            skillInfo.addProperty("maxed_skill_farming", level_skill_farming.maxed);
            skillInfo.addProperty("maxed_skill_combat", level_skill_combat.maxed);
            skillInfo.addProperty("maxed_skill_fishing", level_skill_fishing.maxed);
            skillInfo.addProperty("maxed_skill_alchemy", level_skill_alchemy.maxed);
            skillInfo.addProperty("maxed_skill_runecrafting", level_skill_runecrafting.maxed);

            skillInfo.addProperty("maxed_skill_catacombs", level_skill_catacombs.maxed);

            skillInfo.addProperty("maxed_slayer_zombie", level_slayer_zombie.maxed);
            skillInfo.addProperty("maxed_slayer_spider", level_slayer_spider.maxed);
            skillInfo.addProperty("maxed_slayer_wolf", level_slayer_wolf.maxed);

            skillInfo.addProperty("maxxp_skill_taming", level_skill_taming.maxXpForLevel);
            skillInfo.addProperty("maxxp_skill_mining", level_skill_mining.maxXpForLevel);
            skillInfo.addProperty("maxxp_skill_foraging", level_skill_foraging.maxXpForLevel);
            skillInfo.addProperty("maxxp_skill_enchanting", level_skill_enchanting.maxXpForLevel);
            skillInfo.addProperty("maxxp_skill_carpentry", level_skill_carpentry.maxXpForLevel);
            skillInfo.addProperty("maxxp_skill_farming", level_skill_farming.maxXpForLevel);
            skillInfo.addProperty("maxxp_skill_combat", level_skill_combat.maxXpForLevel);
            skillInfo.addProperty("maxxp_skill_fishing", level_skill_fishing.maxXpForLevel);
            skillInfo.addProperty("maxxp_skill_alchemy", level_skill_alchemy.maxXpForLevel);
            skillInfo.addProperty("maxxp_skill_runecrafting", level_skill_runecrafting.maxXpForLevel);

            skillInfo.addProperty("maxxp_skill_catacombs", level_skill_catacombs.maxXpForLevel);

            skillInfo.addProperty("maxxp_slayer_zombie", level_slayer_zombie.maxXpForLevel);
            skillInfo.addProperty("maxxp_slayer_spider", level_slayer_spider.maxXpForLevel);
            skillInfo.addProperty("maxxp_slayer_wolf", level_slayer_wolf.maxXpForLevel);

            return skillInfo;
        }

        public JsonObject getInventoryInfo(String profileId) {
            JsonObject profileInfo = getProfileInformation(profileId);
            if(profileInfo == null) return null;
            if(profileId == null) profileId = latestProfile;
            if(inventoryInfoMap.containsKey(profileId)) return inventoryInfoMap.get(profileId);

            String inv_armor_bytes = Utils.getElementAsString(Utils.getElement(profileInfo, "inv_armor.data"), "Hz8IAAAAAAAAAD9iYD9kYD9kAAMAPwI/Gw0AAAA=");
            String fishing_bag_bytes = Utils.getElementAsString(Utils.getElement(profileInfo, "fishing_bag.data"), "Hz8IAAAAAAAAAD9iYD9kYD9kAAMAPwI/Gw0AAAA=");
            String quiver_bytes = Utils.getElementAsString(Utils.getElement(profileInfo, "quiver.data"), "Hz8IAAAAAAAAAD9iYD9kYD9kAAMAPwI/Gw0AAAA=");
            String ender_chest_contents_bytes = Utils.getElementAsString(Utils.getElement(profileInfo, "ender_chest_contents.data"), "Hz8IAAAAAAAAAD9iYD9kYD9kAAMAPwI/Gw0AAAA=");
            String wardrobe_contents_bytes = Utils.getElementAsString(Utils.getElement(profileInfo, "wardrobe_contents.data"), "Hz8IAAAAAAAAAD9iYD9kYD9kAAMAPwI/Gw0AAAA=");
            String potion_bag_bytes = Utils.getElementAsString(Utils.getElement(profileInfo, "potion_bag.data"), "Hz8IAAAAAAAAAD9iYD9kYD9kAAMAPwI/Gw0AAAA=");
            String inv_contents_bytes = Utils.getElementAsString(Utils.getElement(profileInfo, "inv_contents.data"), "Hz8IAAAAAAAAAD9iYD9kYD9kAAMAPwI/Gw0AAAA=");
            String talisman_bag_bytes = Utils.getElementAsString(Utils.getElement(profileInfo, "talisman_bag.data"), "Hz8IAAAAAAAAAD9iYD9kYD9kAAMAPwI/Gw0AAAA=");
            String candy_inventory_contents_bytes = Utils.getElementAsString(Utils.getElement(profileInfo, "candy_inventory_contents.data"), "Hz8IAAAAAAAAAD9iYD9kYD9kAAMAPwI/Gw0AAAA=");

            JsonObject inventoryInfo = new JsonObject();

            String[] inv_names = new String[]{"inv_armor", "fishing_bag", "quiver", "ender_chest_contents", "wardrobe_contents",
                    "potion_bag", "inv_contents", "talisman_bag", "candy_inventory_contents"};
            String[] inv_bytes = new String[]{inv_armor_bytes, fishing_bag_bytes, quiver_bytes, ender_chest_contents_bytes, wardrobe_contents_bytes,
                    potion_bag_bytes, inv_contents_bytes, talisman_bag_bytes, candy_inventory_contents_bytes};
            for(int i=0; i<inv_bytes.length; i++) {
                try {
                    String bytes = inv_bytes[i];

                    JsonArray contents = new JsonArray();
                    NBTTagCompound inv_contents_nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(Base64.getDecoder().decode(bytes)));
                    NBTTagList items = inv_contents_nbt.getTagList("i", 10);
                    for(int j=0; j<items.tagCount(); j++) {
                        JsonObject item = manager.getJsonFromNBTEntry(items.getCompoundTagAt(j));
                        contents.add(item);
                    }
                    inventoryInfo.add(inv_names[i], contents);
                } catch(IOException e) {
                    inventoryInfo.add(inv_names[i], new JsonArray());
                }
            }

            inventoryInfoMap.put(profileId, inventoryInfo);

            return inventoryInfo;
        }

        public JsonObject getPetsInfo(String profileId) {
            JsonObject profileInfo = getProfileInformation(profileId);
            if(profileInfo == null) return null;
            if(petsInfoMap.containsKey(profileId)) return petsInfoMap.get(profileId);

            JsonObject petsInfo = new JsonObject();
            JsonElement petsElement = profileInfo.get("pets");
            if(petsElement != null && petsElement.isJsonArray()) {
                JsonObject activePet = null;
                JsonArray pets = petsElement.getAsJsonArray();
                for(int i=0; i<pets.size(); i++) {
                    JsonObject pet = pets.get(i).getAsJsonObject();
                    if(pet.has("active") && pet.get("active").getAsJsonPrimitive().getAsBoolean()) {
                        activePet = pet;
                        break;
                    }
                }
                petsInfo.add("active_pet", activePet);
                petsInfo.add("pets", pets);
                petsInfoMap.put(profileId, petsInfo);
                return petsInfo;
            }
            return null;
        }

        private final Pattern COLL_TIER_PATTERN = Pattern.compile("_(-?[0-9]+)");
        public JsonObject getCollectionInfo(String profileId) {
            JsonObject profileInfo = getProfileInformation(profileId);
            if(profileInfo == null) return null;
            JsonObject resourceCollectionInfo = getResourceCollectionInformation();
            if(resourceCollectionInfo == null) return null;
            if(profileId == null) profileId = latestProfile;
            if(collectionInfoMap.containsKey(profileId)) return collectionInfoMap.get(profileId);

            JsonElement unlocked_coll_tiers_element = Utils.getElement(profileInfo, "unlocked_coll_tiers");
            JsonElement crafted_generators_element = Utils.getElement(profileInfo, "crafted_generators");
            JsonElement collectionInfoElement = Utils.getElement(profileInfo, "collection");

            if(unlocked_coll_tiers_element == null || collectionInfoElement == null) {
                return null;
            }

            JsonObject collectionInfo = new JsonObject();
            JsonObject collectionTiers = new JsonObject();
            JsonObject minionTiers = new JsonObject();
            JsonObject personalAmounts = new JsonObject();
            JsonObject totalAmounts = new JsonObject();

            if(collectionInfoElement.isJsonObject()) {
                personalAmounts = collectionInfoElement.getAsJsonObject();
            }

            for(Map.Entry<String, JsonElement> entry : personalAmounts.entrySet()) {
                totalAmounts.addProperty(entry.getKey(), entry.getValue().getAsInt());
            }

            List<JsonObject> coopProfiles = getCoopProfileInformation(profileId);
            if(coopProfiles != null) {
                for(JsonObject coopProfile : coopProfiles) {
                    JsonElement coopCollectionInfoElement = Utils.getElement(coopProfile, "collection");
                    if(coopCollectionInfoElement != null && coopCollectionInfoElement.isJsonObject()) {
                        for(Map.Entry<String, JsonElement> entry : coopCollectionInfoElement.getAsJsonObject().entrySet()) {
                            float existing = Utils.getElementAsFloat(totalAmounts.get(entry.getKey()), 0);
                            totalAmounts.addProperty(entry.getKey(), existing+entry.getValue().getAsInt());
                        }
                    }
                }
            }

            if(unlocked_coll_tiers_element.isJsonArray()) {
                JsonArray unlocked_coll_tiers = unlocked_coll_tiers_element.getAsJsonArray();
                for(int i=0; i<unlocked_coll_tiers.size(); i++) {
                    String unlocked = unlocked_coll_tiers.get(i).getAsString();

                    Matcher matcher = COLL_TIER_PATTERN.matcher(unlocked);

                    if(matcher.find()) {
                        String tier_str = matcher.group(1);
                        int tier = Integer.parseInt(tier_str);
                        String coll = unlocked.substring(0, unlocked.length()-(matcher.group().length()));
                        if(!collectionTiers.has(coll) || collectionTiers.get(coll).getAsInt() < tier) {
                            collectionTiers.addProperty(coll, tier);
                        }
                    }
                }
            }

            if(crafted_generators_element != null && crafted_generators_element.isJsonArray()) {
                JsonArray crafted_generators = crafted_generators_element.getAsJsonArray();
                for(int i=0; i<crafted_generators.size(); i++) {
                    String unlocked = crafted_generators.get(i).getAsString();

                    Matcher matcher = COLL_TIER_PATTERN.matcher(unlocked);

                    if(matcher.find()) {
                        String tier_str = matcher.group(1);
                        int tier = Integer.parseInt(tier_str);
                        String coll = unlocked.substring(0, unlocked.length()-(matcher.group().length()));
                        if(!minionTiers.has(coll) || minionTiers.get(coll).getAsInt() < tier) {
                            minionTiers.addProperty(coll, tier);
                        }
                    }
                }
            }

            JsonObject maxAmount = new JsonObject();
            JsonObject updatedCollectionTiers = new JsonObject();
            for(Map.Entry<String, JsonElement> totalAmountsEntry : totalAmounts.entrySet()) {
                String collName = totalAmountsEntry.getKey();
                int collTier = (int)Utils.getElementAsFloat(collectionTiers.get(collName), 0);

                int currentAmount = (int)Utils.getElementAsFloat(totalAmounts.get(collName), 0);
                if(currentAmount > 0) {
                    for(Map.Entry<String, JsonElement> resourceEntry : resourceCollectionInfo.entrySet()) {
                        JsonElement tiersElement = Utils.getElement(resourceEntry.getValue(), "items."+collName+".tiers");
                        if(tiersElement != null && tiersElement.isJsonArray()) {
                            JsonArray tiers = tiersElement.getAsJsonArray();
                            int maxTierAcquired = -1;
                            int maxAmountRequired = -1;
                            for(int i=0; i<tiers.size(); i++) {
                                JsonObject tierInfo = tiers.get(i).getAsJsonObject();
                                int tier = tierInfo.get("tier").getAsInt();
                                int amountRequired = tierInfo.get("amountRequired").getAsInt();
                                if(currentAmount >= amountRequired) {
                                    maxTierAcquired = tier;
                                }
                                maxAmountRequired = amountRequired;
                            }
                            if(maxTierAcquired >= 0 && maxTierAcquired > collTier) {
                                updatedCollectionTiers.addProperty(collName, maxTierAcquired);
                            }
                            maxAmount.addProperty(collName, maxAmountRequired);
                        }
                    }
                }
            }

            for(Map.Entry<String, JsonElement> collectionTiersEntry : updatedCollectionTiers.entrySet()) {
                collectionTiers.add(collectionTiersEntry.getKey(), collectionTiersEntry.getValue());
            }

            collectionInfo.add("minion_tiers", minionTiers);
            collectionInfo.add("max_amounts", maxAmount);
            collectionInfo.add("personal_amounts", personalAmounts);
            collectionInfo.add("total_amounts", totalAmounts);
            collectionInfo.add("collection_tiers", collectionTiers);

            return collectionInfo;
        }

        public PlayerStats.Stats getPassiveStats(String profileId) {
            if(passiveStats.get(profileId) != null) return passiveStats.get(profileId);
            JsonObject profileInfo = getProfileInformation(profileId);
            if(profileInfo == null) return null;

            PlayerStats.Stats passiveStats = PlayerStats.getPassiveBonuses(getSkillInfo(profileId), profileInfo);

            if(passiveStats != null) {
                passiveStats.add(PlayerStats.getBaseStats());
            }

            this.passiveStats.put(profileId, passiveStats);

            return passiveStats;
        }

        public PlayerStats.Stats getStats(String profileId) {
            //if(stats.get(profileId) != null) return stats.get(profileId);
            JsonObject profileInfo = getProfileInformation(profileId);
            if(profileInfo == null) {
                return null;
            }

            PlayerStats.Stats stats = PlayerStats.getStats(getSkillInfo(profileId), getInventoryInfo(profileId), getCollectionInfo(profileId),
                    getPetsInfo(profileId), profileInfo);
            this.stats.put(profileId, stats);
            return stats;
        }

        public String getUuid() {
            return uuid;
        }

        public @Nullable JsonObject getHypixelProfile() {
            if(uuidToHypixelProfile.containsKey(uuid)) return uuidToHypixelProfile.get(uuid);
            return null;
        }
    }

    private HashMap<String, JsonObject> nameToHypixelProfile = new HashMap<>();
    private HashMap<String, JsonObject> uuidToHypixelProfile = new HashMap<>();
    private HashMap<String, Profile> uuidToProfileMap = new HashMap<>();

    public void getHypixelProfile(String name, Consumer<JsonObject> callback) {
        String nameF = name.toLowerCase();
        HashMap<String, String> args = new HashMap<>();
        args.put("name", ""+nameF);
        manager.hypixelApi.getHypixelApiAsync(NotEnoughUpdates.INSTANCE.config.apiKey.apiKey, "player",
                args, jsonObject -> {
                    if(jsonObject != null && jsonObject.has("success") && jsonObject.get("success").getAsBoolean()
                            && jsonObject.get("player").isJsonObject()) {
                        nameToUuid.put(nameF, jsonObject.get("player").getAsJsonObject().get("uuid").getAsString());
                        uuidToHypixelProfile.put(jsonObject.get("player").getAsJsonObject().get("uuid").getAsString(), jsonObject.get("player").getAsJsonObject());
                        if(callback != null) callback.accept(jsonObject);
                    } else {
                        if(callback != null) callback.accept(null);
                    }
                }
        );
    }

    private final HashMap<String, String> nameToUuid = new HashMap<>();

    public void putNameUuid(String name, String uuid) {
        nameToUuid.put(name, uuid);
    }

    public void getPlayerUUID(String name, Consumer<String> uuidCallback) {
        String nameF = name.toLowerCase();
        if(nameToUuid.containsKey(nameF)) {
            uuidCallback.accept(nameToUuid.get(nameF));
            return;
        }

        manager.hypixelApi.getApiAsync("https://api.mojang.com/users/profiles/minecraft/"+nameF,
                (jsonObject) -> {
                    if(jsonObject.has("id") && jsonObject.get("id").isJsonPrimitive() &&
                            ((JsonPrimitive)jsonObject.get("id")).isString()) {
                        String uuid = jsonObject.get("id").getAsString();
                        nameToUuid.put(nameF, uuid);
                        uuidCallback.accept(uuid);
                        return;
                    }
                    uuidCallback.accept(null);
                }, () -> uuidCallback.accept(null)
        );
    }

    public void getProfileByName(String name, Consumer<Profile> callback) {
        String nameF = name.toLowerCase();

        if(nameToUuid.containsKey(nameF) && nameToUuid.get(nameF) == null) {
            callback.accept(null);
            return;
        }

        getPlayerUUID(nameF, (uuid) -> {
            if(uuid == null) {
                getHypixelProfile(nameF, jsonObject -> {
                    if(jsonObject != null) {
                        callback.accept(getProfileReset(nameToUuid.get(nameF), ignored -> {}));
                    } else {
                        callback.accept(null);
                        nameToUuid.put(nameF, null);
                    }
                });
            } else {
                if(!uuidToHypixelProfile.containsKey(uuid)) {
                    getHypixelProfile(nameF, jsonObject -> {});
                }
                callback.accept(getProfileReset(uuid, ignored -> {}));
            }
        });

        return;
    }

    public Profile getProfileRaw(String uuid) {
        return uuidToProfileMap.get(uuid);
    }

    public Profile getProfile(String uuid, Consumer<Profile> callback) {
        Profile profile = uuidToProfileMap.computeIfAbsent(uuid, k -> new Profile(uuid));
        if(profile.playerInformation != null) {
            callback.accept(profile);
        } else {
            profile.getPlayerInformation(() -> callback.accept(profile));
        }
        return profile;
    }

    public Profile getProfileReset(String uuid, Consumer<Profile> callback) {
        if(uuidToProfileMap.containsKey(uuid)) uuidToProfileMap.get(uuid).resetCache();
        return getProfile(uuid, callback);
    }

    private static JsonObject resourceCollection = null;
    private static AtomicBoolean updatingResourceCollection = new AtomicBoolean(false);
    public static JsonObject getResourceCollectionInformation() {
        if(resourceCollection != null) return resourceCollection;
        if(updatingResourceCollection.get()) return null;

        updatingResourceCollection.set(true);

        HashMap<String, String> args = new HashMap<>();
        NotEnoughUpdates.INSTANCE.manager.hypixelApi.getHypixelApiAsync(NotEnoughUpdates.INSTANCE.config.apiKey.apiKey, "resources/skyblock/collections",
                args, jsonObject -> {
                    updatingResourceCollection.set(false);
                    if(jsonObject != null && jsonObject.has("success") && jsonObject.get("success").getAsBoolean()) {
                        resourceCollection = jsonObject.get("collections").getAsJsonObject();
                    }
                }, () -> {
                    updatingResourceCollection.set(false);
                }
        );

        return null;
    }

}
