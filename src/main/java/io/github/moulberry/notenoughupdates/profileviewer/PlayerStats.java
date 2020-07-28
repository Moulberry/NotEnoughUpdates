package io.github.moulberry.notenoughupdates.profileviewer;

import com.google.gson.*;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.nbt.*;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerStats {
    public static final String HEALTH = "health";
    public static final String DEFENCE = "defence";
    public static final String STRENGTH = "strength";
    public static final String SPEED = "speed";
    public static final String CRIT_CHANCE = "crit_chance";
    public static final String CRIT_DAMAGE = "crit_damage";
    public static final String BONUS_ATTACK_SPEED = "bonus_attack_speed";
    public static final String INTELLIGENCE = "intelligence";
    public static final String SEA_CREATURE_CHANCE = "sea_creature_chance";
    public static final String MAGIC_FIND = "magic_find";
    public static final String PET_LUCK = "pet_luck";

    public static final String[] defaultStatNames = new String[]{"health","defence","strength","speed","crit_chance",
            "crit_damage","bonus_attack_speed","intelligence","sea_creature_chance","magic_find","pet_luck"};
    public static final String[] defaultStatNamesPretty = new String[]{EnumChatFormatting.RED+"\u2764 Health",EnumChatFormatting.GREEN+"\u2748 Defence",
            EnumChatFormatting.RED+"\u2741 Strength",EnumChatFormatting.WHITE+"\u2726 Speed",EnumChatFormatting.BLUE+"\u2623 Crit Chance",
            EnumChatFormatting.BLUE+"\u2620 Crit Damage",EnumChatFormatting.YELLOW+"\u2694 Attack Speed",EnumChatFormatting.AQUA+"\u270e Intelligence",
            EnumChatFormatting.DARK_AQUA+"\u03b1 SC Chance",EnumChatFormatting.AQUA+"\u272f Magic Find",EnumChatFormatting.LIGHT_PURPLE+"\u2663 Pet Luck"};

    public static class Stats {
        JsonObject statsJson = new JsonObject();

        /*public float health;
        public float defence;
        public float strength;
        public float speed;
        public float crit_chance;
        public float crit_damage;
        public float bonus_attack_speed;
        public float intelligence;
        public float sea_creature_chance;
        public float magic_find;
        public float pet_luck;*/

        public Stats(Stats... statses) {
            for(Stats stats : statses) {
                add(stats);
            }
        }

        /*@Override
        public String toString() {
            return String.format("{health=%s,defence=%s,strength=%s,speed=%s,crit_chance=%s,crit_damage=%s," +
                    "bonus_attack_speed=%s,intelligence=%s,sea_creature_chance=%s,magic_find=%s,pet_luck=%s}",
                    stats.get("health"), defence, strength, speed, crit_chance, crit_damage, bonus_attack_speed, intelligence,
                    sea_creature_chance, magic_find, pet_luck);
        }*/

        public float get(String statName) {
            if(statsJson.has(statName)) {
                return statsJson.get(statName).getAsFloat();
            } else {
                return 0;
            }
        }

        public Stats add(Stats stats) {
            for(Map.Entry<String, JsonElement> statEntry : stats.statsJson.entrySet()) {
                if(statEntry.getValue().isJsonPrimitive() && ((JsonPrimitive)statEntry.getValue()).isNumber()) {
                    if(!statsJson.has(statEntry.getKey())) {
                        statsJson.add(statEntry.getKey(), statEntry.getValue());
                    } else {
                        JsonPrimitive e = statsJson.get(statEntry.getKey()).getAsJsonPrimitive();
                        float statNum = e.getAsFloat() + statEntry.getValue().getAsFloat();
                        statsJson.add(statEntry.getKey(), new JsonPrimitive(statNum));
                    }
                }
            }
            return this;
        }

        public void scaleAll(float scale) {
            for(Map.Entry<String, JsonElement> statEntry : statsJson.entrySet()) {
                statsJson.add(statEntry.getKey(), new JsonPrimitive(statEntry.getValue().getAsFloat()*scale));
            }
        }

        public void addStat(String statName, float amount) {
            if(!statsJson.has(statName)) {
                statsJson.add(statName, new JsonPrimitive(amount));
            } else {
                JsonPrimitive e = statsJson.get(statName).getAsJsonPrimitive();
                statsJson.add(statName, new JsonPrimitive(e.getAsFloat() + amount));
            }
        }
    }

    public static Stats getBaseStats() {
        JsonObject misc = Utils.getConstant("misc");
        if(misc == null) return null;

        Stats stats = new Stats();
        for(String statName : defaultStatNames) {
            stats.addStat(statName, Utils.getElementAsFloat(Utils.getElement(misc, "base_stats."+statName), 0));
        }
        return stats;
    }

    private static Stats getFairyBonus(int fairyExchanges) {
        Stats bonus = new Stats();

        bonus.addStat(SPEED, fairyExchanges/10);

        for(int i=0; i<fairyExchanges; i++) {
            bonus.addStat(STRENGTH, (i + 1) % 5 == 0 ? 2 : 1);
            bonus.addStat(DEFENCE, (i + 1) % 5 == 0 ? 2 : 1);
            bonus.addStat(HEALTH, 3 + i / 2);
        }

        return bonus;
    }

    private static Stats getSkillBonus(JsonObject skillInfo) {
        JsonObject bonuses = Utils.getConstant("bonuses");
        if(bonuses == null) return null;

        Stats skillBonus = new Stats();

        for(Map.Entry<String, JsonElement> entry : skillInfo.entrySet()) {
            if(entry.getKey().startsWith("level_")) {
                String skill = entry.getKey().substring("level_".length());
                JsonObject skillStatMap = Utils.getElement(bonuses, "bonus_stats."+skill).getAsJsonObject();

                Stats currentBonus = new Stats();
                for(int i=1; i<=entry.getValue().getAsFloat(); i++) {
                    if(skillStatMap.has(""+i)) {
                        currentBonus = new Stats();
                        for(Map.Entry<String, JsonElement> entry2 : skillStatMap.get(""+i).getAsJsonObject().entrySet()) {
                            currentBonus.addStat(entry2.getKey(), entry2.getValue().getAsFloat());
                        }
                    }
                    skillBonus.add(currentBonus);
                }
            }
        }

        return skillBonus;
    }

    private static Stats getPetBonus(JsonObject profile) {
        JsonObject bonuses = Utils.getConstant("bonuses");
        if(bonuses == null) return null;

        JsonElement petsElement = Utils.getElement(profile, "pets");
        if(petsElement == null) return new Stats();

        JsonArray pets = petsElement.getAsJsonArray();

        HashMap<String, String> highestRarityMap = new HashMap<>();

        for(int i=0; i<pets.size(); i++) {
            JsonObject pet = pets.get(i).getAsJsonObject();
            highestRarityMap.put(pet.get("type").getAsString(), pet.get("tier").getAsString());
        }

        int petScore = 0;
        for(String value : highestRarityMap.values()) {
            petScore += Utils.getElementAsFloat(Utils.getElement(bonuses, "pet_value."+value.toUpperCase()), 0);
        }

        JsonElement petRewardsElement = Utils.getElement(bonuses, "pet_rewards");
        if(petRewardsElement == null) return null;
        JsonObject petRewards = petRewardsElement.getAsJsonObject();

        Stats petBonus = new Stats();
        for(int i=0; i<=petScore; i++) {
            if(petRewards.has(""+i)) {
                petBonus = new Stats();
                for(Map.Entry<String, JsonElement> entry : petRewards.get(""+i).getAsJsonObject().entrySet()) {
                    petBonus.addStat(entry.getKey(), entry.getValue().getAsFloat());
                }
            }
        }
        return petBonus;
    }

    private static float harpBonus(JsonObject profile) {
        String talk_to_melody = Utils.getElementAsString(Utils.getElement(profile, "objectives.talk_to_melody.status"), "INCOMPLETE");
        if(talk_to_melody.equalsIgnoreCase("COMPLETE")) {
            return 26;
        } else {
            return 0;
        }
    }


    public static Stats getPassiveBonuses(JsonObject skillInfo, JsonObject profile) {
        Stats passiveBonuses = new Stats();

        Stats fairyBonus = getFairyBonus((int)Utils.getElementAsFloat(Utils.getElement(profile, "fairy_exchanges"), 0));
        Stats skillBonus = getSkillBonus(skillInfo);
        Stats petBonus = getPetBonus(profile);

        if(fairyBonus == null || skillBonus == null || petBonus == null) return null;

        passiveBonuses.add(fairyBonus);
        passiveBonuses.add(skillBonus);
        passiveBonuses.addStat(INTELLIGENCE, harpBonus(profile));
        passiveBonuses.add(petBonus);

        return passiveBonuses;
    }

    private static String getFullset(JsonArray armor, int ignore) {
        String fullset = null;
        for(int i=0; i<armor.size(); i++) {
            if(i == ignore) continue;

            JsonElement itemElement = armor.get(i);
            if(itemElement == null || !itemElement.isJsonObject()) {
                fullset = null;
                break;
            }
            JsonObject item = itemElement.getAsJsonObject();
            String internalname = item.get("internalname").getAsString();

            String[] split = internalname.split("_");
            split[split.length-1] = "";
            String armorname = StringUtils.join(split, "_");

            if(fullset == null) {
                fullset = armorname;
            } else if(!fullset.equalsIgnoreCase(armorname)){
                fullset = null;
                break;
            }
        }
        return fullset;
    }

    private static Stats getSetBonuses(Stats stats, JsonObject inventoryInfo, JsonObject collectionInfo, JsonObject skillInfo, JsonObject profile) {
        JsonArray armor = Utils.getElement(inventoryInfo, "inv_armor").getAsJsonArray();

        Stats bonuses = new Stats();

        String fullset = getFullset(armor, -1);

        if(fullset != null) {
            switch(fullset) {
                case "LAPIS_ARMOR_":
                    bonuses.addStat(HEALTH, 60);
                    break;
                case "EMERALD_ARMOR_":
                    {
                        int bonus = (int)Math.floor(Utils.getElementAsFloat(Utils.getElement(collectionInfo, "EMERALD"), 0)/3000);
                        bonuses.addStat(HEALTH, bonus);
                        bonuses.addStat(DEFENCE, bonus);
                    }
                    break;
                case "FAIRY_":
                    bonuses.addStat(HEALTH, Utils.getElementAsFloat(Utils.getElement(profile, "fairy_souls_collected"), 0));
                    break;
                case "SPEEDSTER_":
                    bonuses.addStat(SPEED, 20);
                    break;
                case "YOUNG_DRAGON_":
                    bonuses.addStat(SPEED, 70);
                    break;
                case "MASTIFF_":
                    bonuses.addStat(HEALTH, 50*Math.round(stats.get(CRIT_DAMAGE)));
                    break;
                case "ANGLER_":
                    bonuses.addStat(HEALTH, 10*(float)Math.floor(Utils.getElementAsFloat(Utils.getElement(skillInfo, "level_skill_fishing"), 0)));
                    bonuses.addStat(SEA_CREATURE_CHANCE, 4);
                    break;
                case "ARMOR_OF_MAGMA_":
                    int bonus = (int)Math.min(200, Math.floor(Utils.getElementAsFloat(Utils.getElement(profile, "stats.kills_magma_cube"), 0)/10));
                    bonuses.addStat(HEALTH, bonus);
                    bonuses.addStat(INTELLIGENCE, bonus);
                case "OLD_DRAGON_":
                    bonuses.addStat(HEALTH, 200);
                    bonuses.addStat(DEFENCE, 40);
                    break;
            }
        }

        JsonElement chestplateElement = armor.get(2);
        if(chestplateElement != null && chestplateElement.isJsonObject()) {
            JsonObject chestplate = chestplateElement.getAsJsonObject();
            if(chestplate.get("internalname").getAsString().equals("OBSIDIAN_CHESTPLATE")) {
                JsonArray inventory = Utils.getElement(inventoryInfo, "inv_contents").getAsJsonArray();
                for(int i=0; i<inventory.size(); i++) {
                    JsonElement itemElement = inventory.get(i);
                    if(itemElement != null && itemElement.isJsonObject()) {
                        JsonObject item = itemElement.getAsJsonObject();
                        if(item.get("internalname").getAsString().equals("OBSIDIAN")) {
                            int count = 1;
                            if(item.has("count")) {
                                count = item.get("count").getAsInt();
                            }
                            bonuses.addStat(SPEED, count/20);
                        }
                    }
                }
            }
        }

        return bonuses;
    }

    private static final Pattern HEALTH_PATTERN = Pattern.compile("^Health: ((?:\\+|-)[0-9]+)");
    private static final Pattern DEFENCE_PATTERN = Pattern.compile("^Defense: ((?:\\+|-)[0-9]+)");
    private static final Pattern STRENGTH_PATTERN = Pattern.compile("^Strength: ((?:\\+|-)[0-9]+)");
    private static final Pattern SPEED_PATTERN = Pattern.compile("^Speed: ((?:\\+|-)[0-9]+)");
    private static final Pattern CC_PATTERN = Pattern.compile("^Crit Chance: ((?:\\+|-)[0-9]+)");
    private static final Pattern CD_PATTERN = Pattern.compile("^Crit Damage: ((?:\\+|-)[0-9]+)");
    private static final Pattern ATKSPEED_PATTERN = Pattern.compile("^Bonus Attack Speed: ((?:\\+|-)[0-9]+)");
    private static final Pattern INTELLIGENCE_PATTERN = Pattern.compile("^Intelligence: ((?:\\+|-)[0-9]+)");
    private static final Pattern SCC_PATTERN = Pattern.compile("^Sea Creature Chance: ((?:\\+|-)[0-9]+)");
    private static final HashMap<String, Pattern> STAT_PATTERN_MAP = new HashMap<>();
    static {
        STAT_PATTERN_MAP.put("health", HEALTH_PATTERN);
        STAT_PATTERN_MAP.put("defence", DEFENCE_PATTERN);
        STAT_PATTERN_MAP.put("strength", STRENGTH_PATTERN);
        STAT_PATTERN_MAP.put("speed", SPEED_PATTERN);
        STAT_PATTERN_MAP.put("crit_chance", CC_PATTERN);
        STAT_PATTERN_MAP.put("crit_damage", CD_PATTERN);
        STAT_PATTERN_MAP.put("bonus_attack_speed", ATKSPEED_PATTERN);
        STAT_PATTERN_MAP.put("intelligence", INTELLIGENCE_PATTERN);
        STAT_PATTERN_MAP.put("sea_creature_chance", SCC_PATTERN);
    }
    private static Stats getStatForItem(String internalname, JsonObject item, JsonArray lore) {
        Stats stats = new Stats();
        for(int i=0; i<lore.size(); i++) {
            String line = lore.get(i).getAsString();
            for(Map.Entry<String, Pattern> entry : STAT_PATTERN_MAP.entrySet()) {
                Matcher matcher = entry.getValue().matcher(Utils.cleanColour(line));
                if(matcher.find()) {
                    int bonus = Integer.parseInt(matcher.group(1));
                    stats.addStat(entry.getKey(), bonus);
                }
            }
        }
        if(internalname.equals("DAY_CRYSTAL") || internalname.equals("NIGHT_CRYSTAL")) {
            stats.addStat(STRENGTH, 2.5f);
            stats.addStat(DEFENCE, 2.5f);
        }
        if(internalname.equals("NEW_YEAR_CAKE_BAG") && item.has("item_contents")) {
            JsonArray bytesArr = item.get("item_contents").getAsJsonArray();
            byte[] bytes = new byte[bytesArr.size()];
            for(int i=0; i<bytesArr.size(); i++) {
                bytes[i] = bytesArr.get(i).getAsByte();
            }
            try {
                NBTTagCompound contents_nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes));
                NBTTagList items = contents_nbt.getTagList("i", 10);
                HashSet<Integer> cakes = new HashSet<>();
                for(int j=0; j<items.tagCount(); j++) {
                    if(items.getCompoundTagAt(j).getKeySet().size() > 0) {
                        NBTTagCompound nbt = items.getCompoundTagAt(j).getCompoundTag("tag");
                        if(nbt != null && nbt.hasKey("ExtraAttributes", 10)) {
                            NBTTagCompound ea = nbt.getCompoundTag("ExtraAttributes");
                            if (ea.hasKey("new_years_cake")) {
                                cakes.add(ea.getInteger("new_years_cake"));
                            }
                        }
                    }
                }
                stats.addStat(HEALTH, cakes.size());
            } catch(IOException e) {
                e.printStackTrace();
                return stats;
            }
        }
        return stats;
    }
    private static Stats getItemBonuses(boolean talismanOnly, JsonArray... inventories) {
        JsonObject misc = Utils.getConstant("misc");
        if(misc == null) return null;
        JsonElement talisman_upgrades_element = misc.get("talisman_upgrades");
        if(talisman_upgrades_element == null) return null;
        JsonObject talisman_upgrades = talisman_upgrades_element.getAsJsonObject();

        HashMap<String, Stats> itemBonuses = new HashMap<>();
        for(JsonArray inventory : inventories) {
            for(int i=0; i<inventory.size(); i++) {
                JsonElement itemElement = inventory.get(i);
                if(itemElement != null && itemElement.isJsonObject()) {
                    JsonObject item = itemElement.getAsJsonObject();
                    String internalname = item.get("internalname").getAsString();
                    if(itemBonuses.containsKey(internalname)) {
                        continue;
                    }
                    if(!talismanOnly || Utils.checkItemType(item.get("lore").getAsJsonArray(), true, "ACCESSORY", "HATCCESSORY") >= 0) {
                        Stats itemBonus = getStatForItem(internalname, item, item.get("lore").getAsJsonArray());

                        itemBonuses.put(internalname, itemBonus);

                        for(Map.Entry<String, JsonElement> talisman_upgrades_item : talisman_upgrades.entrySet()) {
                            JsonArray upgrades = talisman_upgrades_item.getValue().getAsJsonArray();
                            for(int j=0; j<upgrades.size(); j++) {
                                String upgrade = upgrades.get(j).getAsString();
                                if(upgrade.equals(internalname)) {
                                    itemBonuses.put(talisman_upgrades_item.getKey(), new Stats());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        Stats itemBonusesStats = new Stats();
        for(Stats stats : itemBonuses.values()) {
            itemBonusesStats.add(stats);
        }
        return itemBonusesStats;
    }

    private static float getStatMult(JsonObject inventoryInfo) {
        float mult = 1f;

        JsonArray armor = Utils.getElement(inventoryInfo, "inv_armor").getAsJsonArray();

        String fullset = getFullset(armor, -1);

        if(fullset != null && fullset.equals("SUPERIOR_DRAGON_")) {
            mult *= 1.05f;
        }

        for(int i=0; i<armor.size(); i++) {
            JsonElement itemElement = armor.get(i);
            if(itemElement == null || !itemElement.isJsonObject()) continue;

            JsonObject item = itemElement.getAsJsonObject();
            String internalname = item.get("internalname").getAsString();

            String reforge = Utils.getElementAsString(Utils.getElement(item, "ExtraAttributes.modifier"), "");

            if(reforge.equals("renowned")) {
                mult *= 1.01f;
            }
        }

        return mult;
    }

    private static void applyLimits(Stats stats, JsonObject inventoryInfo) {
        //>0
        JsonArray armor = Utils.getElement(inventoryInfo, "inv_armor").getAsJsonArray();

        String fullset = getFullset(armor, 3);

        if(fullset != null) {
            switch(fullset) {
                case "CHEAP_TUXEDO_":
                    stats.statsJson.add(HEALTH, new JsonPrimitive(Math.min(75, stats.get(HEALTH))));
                case "FANCY_TUXEDO_":
                    stats.statsJson.add(HEALTH, new JsonPrimitive(Math.min(150, stats.get(HEALTH))));
                case "ELEGANT_TUXEDO_":
                    stats.statsJson.add(HEALTH, new JsonPrimitive(Math.min(250, stats.get(HEALTH))));
            }
        }

        for(Map.Entry<String, JsonElement> statEntry : stats.statsJson.entrySet()) {
            if(statEntry.getKey().equals(CRIT_DAMAGE) ||
                    statEntry.getKey().equals(INTELLIGENCE) ||
                    statEntry.getKey().equals(BONUS_ATTACK_SPEED)) continue;
            stats.statsJson.add(statEntry.getKey(), new JsonPrimitive(Math.max(0, statEntry.getValue().getAsFloat())));
        }
    }

    public static Stats getStats(JsonObject skillInfo, JsonObject inventoryInfo, JsonObject collectionInfo, JsonObject profile) {
        if(skillInfo == null || inventoryInfo == null || collectionInfo == null || profile == null) return null;

        JsonArray armor = Utils.getElement(inventoryInfo, "inv_armor").getAsJsonArray();
        JsonArray inventory = Utils.getElement(inventoryInfo, "inv_contents").getAsJsonArray();
        JsonArray talisman_bag = Utils.getElement(inventoryInfo, "talisman_bag").getAsJsonArray();

        Stats passiveBonuses = getPassiveBonuses(skillInfo, profile);
        Stats armorBonuses = getItemBonuses(false, armor);
        Stats talismanBonuses = getItemBonuses(true, inventory, talisman_bag);

        if(passiveBonuses == null || armorBonuses == null || talismanBonuses == null) return null;

        Stats stats = getBaseStats().add(passiveBonuses).add(armorBonuses).add(talismanBonuses);

        stats.add(getSetBonuses(stats, inventoryInfo, collectionInfo, skillInfo, profile));

        stats.scaleAll(getStatMult(inventoryInfo));

        applyLimits(stats, inventoryInfo);

        return stats;
    }

}
