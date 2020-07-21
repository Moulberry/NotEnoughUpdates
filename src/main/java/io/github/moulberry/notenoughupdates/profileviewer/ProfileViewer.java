package io.github.moulberry.notenoughupdates.profileviewer;

import com.google.common.base.Splitter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
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
        skillToSkillDisplayMap.put("slayer_zombie", Utils.createItemStack(Items.rotten_flesh, EnumChatFormatting.GOLD+"Rev Slayer"));
        skillToSkillDisplayMap.put("slayer_spider", Utils.createItemStack(Items.spider_eye, EnumChatFormatting.GOLD+"Tara Slayer"));
        skillToSkillDisplayMap.put("slayer_wolf", Utils.createItemStack(Items.bone, EnumChatFormatting.GOLD+"Sven Slayer"));
    }

    public static Map<String, ItemStack> getSkillToSkillDisplayMap() {
        return Collections.unmodifiableMap(skillToSkillDisplayMap);
    }

    public class Profile {
        private final String uuid;
        private String latestProfile = null;

        private JsonArray playerInformation = null;
        private JsonObject basicInfo = null;

        private final HashMap<String, JsonObject> profileMap = new HashMap<>();
        private final HashMap<String, JsonObject> skillInfoMap = new HashMap<>();
        private final HashMap<String, JsonObject> inventoryInfoMap = new HashMap<>();
        private final HashMap<String, JsonObject> collectionInfoMap = new HashMap<>();
        private JsonObject playerStatus = null;
        private PlayerStats.Stats stats = null;
        private PlayerStats.Stats passiveStats = null;

        public Profile(String uuid) {
            this.uuid = uuid;
        }

        private AtomicBoolean updatingPlayerInfoState = new AtomicBoolean(false);
        private AtomicBoolean updatingPlayerStatusState = new AtomicBoolean(false);

        public JsonObject getPlayerStatus() {
            if(playerStatus != null) return playerStatus;
            if(updatingPlayerStatusState.get()) return null;

            updatingPlayerStatusState.set(true);

            HashMap<String, String> args = new HashMap<>();
            args.put("uuid", ""+uuid);
            manager.hypixelApi.getHypixelApiAsync(manager.config.apiKey.value, "status",
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

        public JsonArray getPlayerInformation(Runnable runnable) {
            if(playerInformation != null) return playerInformation;
            if(updatingPlayerInfoState.get()) return null;

            updatingPlayerInfoState.set(true);

            HashMap<String, String> args = new HashMap<>();
            args.put("uuid", ""+uuid);
            manager.hypixelApi.getHypixelApiAsync(manager.config.apiKey.value, "skyblock/profiles",
                args, jsonObject -> {
                    if(jsonObject == null) return;

                    updatingPlayerInfoState.set(false);
                    if(jsonObject.has("success") && jsonObject.get("success").getAsBoolean()) {
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
                                if(member.has("last_save")) {
                                    long last_save = member.get("last_save").getAsLong();
                                    if(last_save > backupLastSave) {
                                        backupLastSave = last_save;
                                        backup = cute_name;
                                    }
                                }
                            }
                        }
                        System.out.println("accepting runnable");
                        if(runnable != null) runnable.run();
                        latestProfile = backup;
                    }
                }, () -> {
                    updatingPlayerInfoState.set(false);
                }
            );

            return null;
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
                    if(!members.has(uuid)) return null;
                    JsonObject profileInfo = members.get(uuid).getAsJsonObject();
                    if(profile.has("banking")) {
                        profileInfo.add("banking", profile.get("banking").getAsJsonObject());
                    }
                    System.out.println("got profile");
                    profileMap.put(profileId, profileInfo);
                    return profileInfo;
                }
            }
            System.out.println("couldnt get profile");

            return null;
        }

        public void resetCache() {
            playerInformation = null;
            basicInfo = null;
            playerStatus = null;
            profileMap.clear();
            skillInfoMap.clear();
            inventoryInfoMap.clear();
            collectionInfoMap.clear();
        }

        private class Level {
            private float level = 0;
            private float maxXpForLevel = 0;
            private boolean maxed = false;
        }

        public Level getLevel(JsonArray levelingArray, float xp, boolean cumulative) {
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
                    return levelObj;
                } else {
                    if(!cumulative) xp -= levelXp;
                }
            }
            levelObj.level = levelingArray.size();
            levelObj.maxed = true;
            return levelObj;
        }

        public JsonObject getSkillInfo(String profileId) {
            JsonObject profileInfo = getProfileInformation(profileId);
            if(profileInfo == null) return null;
            if(profileId == null) profileId = latestProfile;
            if(skillInfoMap.containsKey(profileId)) return skillInfoMap.get(profileId);
            JsonObject leveling = Utils.getConstant("leveling");
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

            float experience_slayer_zombie = Utils.getElementAsFloat(Utils.getElement(profileInfo, "slayer_bosses.zombie.xp"), 0);
            float experience_slayer_spider = Utils.getElementAsFloat(Utils.getElement(profileInfo, "slayer_bosses.spider.xp"), 0);
            float experience_slayer_wolf = Utils.getElementAsFloat(Utils.getElement(profileInfo, "slayer_bosses.wolf.xp"), 0);

            float totalSkillXP = experience_skill_taming + experience_skill_mining + experience_skill_foraging
                    + experience_skill_enchanting + experience_skill_carpentry + experience_skill_farming
                    + experience_skill_combat + experience_skill_fishing + experience_skill_alchemy
                    + experience_skill_runecrafting;

            if(totalSkillXP <= 0) {
                System.out.println("couldnt get skill xp");
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

            skillInfo.addProperty("experience_slayer_zombie", experience_slayer_zombie);
            skillInfo.addProperty("experience_slayer_spider", experience_slayer_spider);
            skillInfo.addProperty("experience_slayer_wolf", experience_slayer_wolf);

            Level level_skill_taming = getLevel(Utils.getElement(leveling, "leveling_xp").getAsJsonArray(), experience_skill_taming, false);
            Level level_skill_mining = getLevel(Utils.getElement(leveling, "leveling_xp").getAsJsonArray(), experience_skill_mining, false);
            Level level_skill_foraging = getLevel(Utils.getElement(leveling, "leveling_xp").getAsJsonArray(), experience_skill_foraging, false);
            Level level_skill_enchanting = getLevel(Utils.getElement(leveling, "leveling_xp").getAsJsonArray(), experience_skill_enchanting, false);
            Level level_skill_carpentry = getLevel(Utils.getElement(leveling, "leveling_xp").getAsJsonArray(), experience_skill_carpentry, false);
            Level level_skill_farming = getLevel(Utils.getElement(leveling, "leveling_xp").getAsJsonArray(), experience_skill_farming, false);
            Level level_skill_combat = getLevel(Utils.getElement(leveling, "leveling_xp").getAsJsonArray(), experience_skill_combat, false);
            Level level_skill_fishing = getLevel(Utils.getElement(leveling, "leveling_xp").getAsJsonArray(), experience_skill_fishing, false);
            Level level_skill_alchemy = getLevel(Utils.getElement(leveling, "leveling_xp").getAsJsonArray(), experience_skill_alchemy, false);
            Level level_skill_runecrafting = getLevel(Utils.getElement(leveling, "runecrafting_xp").getAsJsonArray(), experience_skill_runecrafting, false);

            Level level_slayer_zombie = getLevel(Utils.getElement(leveling, "slayer_xp.zombie").getAsJsonArray(), experience_slayer_zombie, true);
            Level level_slayer_spider = getLevel(Utils.getElement(leveling, "slayer_xp.spider").getAsJsonArray(), experience_slayer_spider, true);
            Level level_slayer_wolf = getLevel(Utils.getElement(leveling, "slayer_xp.wolf").getAsJsonArray(), experience_slayer_wolf, true);

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

            //inv_armor, fishing_bag, quiver, ender_chest_contents, wardrobe_contents, potion_bag, inv_contents, talisman_bag, candy_inventory_contents

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

            return inventoryInfo;
        }

        private final Pattern COLL_TIER_PATTERN = Pattern.compile("_(-?[0-9]+)");
        public JsonObject getCollectionInfo(String profileId) {
            JsonObject profileInfo = getProfileInformation(profileId);
            if(profileInfo == null) return null;
            if(profileId == null) profileId = latestProfile;
            if(collectionInfoMap.containsKey(profileId)) return collectionInfoMap.get(profileId);


            JsonElement unlocked_coll_tiers_element = Utils.getElement(profileInfo, "unlocked_coll_tiers");
            if(unlocked_coll_tiers_element == null) {
                JsonObject collectionInfo = new JsonObject();
                collectionInfo.add("collection_tiers", new JsonObject());
                return collectionInfo;
            }
            JsonArray unlocked_coll_tiers = unlocked_coll_tiers_element.getAsJsonArray();

            JsonElement collectionInfoElement = Utils.getElement(profileInfo, "collections");
            if(collectionInfoElement == null) {
                JsonObject collectionInfo = new JsonObject();
                collectionInfo.add("collection_tiers", new JsonObject());
                return collectionInfo;
            }
            JsonObject collectionInfo = collectionInfoElement.getAsJsonObject();
            JsonObject collectionTiers = new JsonObject();

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

            collectionInfo.add("collection_tiers", collectionInfo);

            return collectionInfo;
        }

        public PlayerStats.Stats getPassiveStats(String profileId) {
            if(passiveStats != null) return passiveStats;
            JsonObject profileInfo = getProfileInformation(profileId);
            if(profileInfo == null) return null;

            passiveStats = PlayerStats.getPassiveBonuses(getSkillInfo(profileId), profileInfo);

            if(passiveStats != null) {
                passiveStats.add(PlayerStats.getBaseStats());
            }

            return passiveStats;
        }

        public PlayerStats.Stats getStats(String profileId) {
            if(stats != null) return stats;
            JsonObject profileInfo = getProfileInformation(profileId);
            if(profileInfo == null) return null;

            stats = PlayerStats.getStats(getSkillInfo(profileId), getInventoryInfo(profileId), getCollectionInfo(profileId), profileInfo);
            return stats;
        }

        public String getUuid() {
            return uuid;
        }

        public JsonObject getHypixelProfile() {
            if(uuidToHypixelProfile.containsKey(uuid)) return uuidToHypixelProfile.get(uuid);
            return null;
        }
    }

    private HashMap<String, JsonObject> nameToHypixelProfile = new HashMap<>();
    private HashMap<String, JsonObject> uuidToHypixelProfile = new HashMap<>();
    private HashMap<String, Profile> uuidToProfileMap = new HashMap<>();

    public void getHypixelProfile(String name, Consumer<JsonObject> callback) {
        HashMap<String, String> args = new HashMap<>();
        args.put("name", ""+name);
        manager.hypixelApi.getHypixelApiAsync(manager.config.apiKey.value, "player",
            args, jsonObject -> {
                    if(jsonObject == null) return;

                    if(jsonObject.has("success") && jsonObject.get("success").getAsBoolean()) {
                    nameToHypixelProfile.put(name, jsonObject.get("player").getAsJsonObject());
                    uuidToHypixelProfile.put(jsonObject.get("player").getAsJsonObject().get("uuid").getAsString(), jsonObject.get("player").getAsJsonObject());
                    if(callback != null) callback.accept(jsonObject);
                }
            }
        );
    }

    public Profile getProfileByName(String name, Consumer<Profile> callback) {
        if(nameToHypixelProfile.containsKey(name)) {
            Profile profile = getProfileReset(nameToHypixelProfile.get(name).get("uuid").getAsString(), ignored -> {});
            callback.accept(profile);
            return profile;
        } else {
            getHypixelProfile(name, jsonObject -> {
                callback.accept(getProfileReset(jsonObject.get("player").getAsJsonObject().get("uuid").getAsString(), ignored -> {}));
            });
        }
        return null;
    }

    public Profile getProfile(String uuid, Consumer<Profile> callback) {
        Profile profile = uuidToProfileMap.computeIfAbsent(uuid, k -> new Profile(uuid));
        if(profile.playerInformation != null) {
            System.out.println("getting profile with callback1");
            callback.accept(profile);
        } else {
            System.out.println("getting profile with callback3");
            profile.getPlayerInformation(() -> callback.accept(profile));
        }
        return profile;
    }

    public Profile getProfileReset(String uuid, Consumer<Profile> callback) {
        Profile profile = getProfile(uuid, callback);
        profile.resetCache();
        return profile;
    }

}
