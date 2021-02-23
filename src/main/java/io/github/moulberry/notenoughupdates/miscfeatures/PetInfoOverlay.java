package io.github.moulberry.notenoughupdates.miscfeatures;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.core.util.lerp.LerpUtils;
import io.github.moulberry.notenoughupdates.options.NEUConfig;
import io.github.moulberry.notenoughupdates.overlays.TextOverlay;
import io.github.moulberry.notenoughupdates.overlays.TextOverlayStyle;
import io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer;
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewer;
import io.github.moulberry.notenoughupdates.util.Constants;
import io.github.moulberry.notenoughupdates.util.ProfileApiSyncer;
import io.github.moulberry.notenoughupdates.util.Utils;
import io.github.moulberry.notenoughupdates.util.XPInformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.util.vector.Vector2f;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PetInfoOverlay extends TextOverlay {

    private static final Pattern XP_BOOST_PATTERN = Pattern.compile("PET_ITEM_(COMBAT|FISHING|MINING|FORAGING|ALL|FARMING)_(SKILL|SKILLS)_BOOST_(COMMON|UNCOMMON|RARE|EPIC)");
    private static final Pattern PET_CONTAINER_PAGE = Pattern.compile("\\((\\d)/(\\d)\\) Pets");
    private static final Pattern PET_NAME_PATTERN = Pattern.compile("\u00a77\\[Lvl \\d+] \u00a7(.+)");
    private static final Pattern XP_LINE_PATTERN = Pattern.compile("-------------------- (\\d+(?:,\\d+)+(?:\\.\\d+))/(\\d+[b|m|k]?)");

    public PetInfoOverlay(Position position, Supplier<List<String>> dummyStrings, Supplier<TextOverlayStyle> styleSupplier) {
        super(position, dummyStrings, styleSupplier);
    }



    public enum Rarity {
        COMMON(0, 0, 1, EnumChatFormatting.WHITE),
        UNCOMMON(6, 1, 2, EnumChatFormatting.GREEN),
        RARE(11, 2, 3, EnumChatFormatting.BLUE),
        EPIC(16, 3, 4, EnumChatFormatting.DARK_PURPLE),
        LEGENDARY(20, 4, 5, EnumChatFormatting.GOLD),
        MYTHIC(20, 4, 5, EnumChatFormatting.LIGHT_PURPLE);

        public int petOffset;
        public EnumChatFormatting chatFormatting;
        public int petId;
        public int beastcreatMultiplyer;

        Rarity(int petOffset, int petId, int beastcreatMultiplyer, EnumChatFormatting chatFormatting) {
            this.chatFormatting = chatFormatting;
            this.petOffset = petOffset;
            this.petId = petId;
            this.beastcreatMultiplyer = beastcreatMultiplyer;
        }

        public static Rarity getRarityFromColor(EnumChatFormatting chatFormatting) {
            for(int i = 0; i < Rarity.values().length; i++) {
                if(Rarity.values()[i].chatFormatting.equals(chatFormatting))
                    return Rarity.values()[i];
            }
            return COMMON;
        }
    }

    public static class Pet {
        public String petType;
        public Rarity rarity;
        public GuiProfileViewer.PetLevel petLevel;
        public String petXpType;
        public String petItem;
    }

    private static long lastXpGain = 0;

    private static HashMap<Integer, Pet> petMap = new HashMap<>();
    private static int selectedPet = -1;
    //public static Pet currentPet = null;
    //public static HashMap<String, Set<Pet>> petList = new HashMap<>();

    public static int tamingLevel = 1;
    public static float beastMultiplier = 0;
    public static boolean setActivePet = false;

    private long lastUpdate = 0;
    private float levelXpLast = 0;

    private LinkedList<Float> xpGainQueue = new LinkedList<>();
    private float xpGainHourLast = -1;
    private float xpGainHour = -1;

    private int xpAddTimer = 0;

    public static void clearPet() {
        selectedPet = -1;
    }

    public static Pet getCurrentPet() {
        return petMap.get(selectedPet);
    }

    public float getLevelPercent() {
        DecimalFormat df = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        Pet pet = getCurrentPet();
        if(pet == null) return 0;
        try {
            return Float.parseFloat(df.format(getCurrentPet().petLevel.levelPercentage * 100f));
        } catch(Exception ignored) {
            return 0;
        }
    }

    private static int getIdForPet(Pet pet) {
        for(Map.Entry<Integer, Pet> entry : petMap.entrySet()) {
            if(entry.getValue() == pet) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private static int getClosestPetIndex(String petType, int petId, String petItem, float petLevel) {
        Pet pet = getClosestPet(petType, petId, petItem, petLevel);
        if(pet == null) {
            return -1;
        } else {
            return getIdForPet(pet);
        }
    }

    private static Pet getClosestPet(String petType, int petId, String petItem, float petLevel) {

        Set<Pet> pets = new HashSet<>();
        for(Pet pet : petMap.values()) {
            if(pet.petType.equals(petType) && pet.rarity.petId == petId) {
                pets.add(pet);
            }
        }

        if(pets == null || pets.isEmpty()) {
            return null;
        }

        if(pets.size() == 1) {
            return pets.iterator().next();
        }

        String searchItem = petItem;

        Set<Pet> itemMatches = new HashSet<>();
        for(Pet pet : pets) {
            if((searchItem == null && pet.petItem == null) ||
                    (searchItem != null && searchItem.equals(pet.petItem))) {
                itemMatches.add(pet);
            }
        }

        if(itemMatches.size() == 1) {
            return itemMatches.iterator().next();
        }
        if(itemMatches.size() > 1) {
            pets = itemMatches;
        }

        float closestXp = -1;
        Pet closestPet = null;

        for(Pet pet : pets) {
            float distXp = Math.abs(pet.petLevel.level - petLevel);

            if(closestPet == null || distXp < closestXp) {
                closestXp = distXp;
                closestPet = pet;
            }
        }

        if(closestPet != null) {
            return closestPet;
        } else {
            return pets.iterator().next();
        }
    }

    private static void getAndSetPet(ProfileViewer.Profile profile) {
        JsonObject skillInfo = profile.getSkillInfo(profile.getLatestProfile());
        JsonObject invInfo = profile.getInventoryInfo(profile.getLatestProfile());
        JsonObject profileInfo = profile.getProfileInformation(profile.getLatestProfile());
        if(invInfo != null && profileInfo != null) {
            JsonObject stats = profileInfo.get("stats").getAsJsonObject();
            boolean hasBeastmasterCrest = false;
            Rarity currentBeastRarity = Rarity.COMMON;
            for(JsonElement talisman : invInfo.get("talisman_bag").getAsJsonArray()) {
                if(talisman.isJsonNull()) continue;
                String internalName = talisman.getAsJsonObject().get("internalname").getAsString();
                if(internalName.startsWith("BEASTMASTER_CREST")) {
                    hasBeastmasterCrest = true;
                    try {
                        Rarity talismanRarity = Rarity.valueOf(internalName.replace("BEASTMASTER_CREST_", ""));
                        if(talismanRarity.beastcreatMultiplyer > currentBeastRarity.beastcreatMultiplyer)
                            currentBeastRarity = talismanRarity;
                    } catch(Exception ignored) {
                    }
                }
            }
            if(hasBeastmasterCrest) {
                if(stats.has("mythos_kills")) {
                    int mk = stats.get("mythos_kills").getAsInt();
                    float petXpBoost = mk > 10000 ? 1f : mk > 7500 ? 0.9f : mk > 5000 ? 0.8f : mk > 2500 ? 0.7f :
                            mk > 1000 ? 0.6f : mk > 500 ? 0.5f : mk > 250 ? 0.4f : mk > 100 ? 0.3f : mk > 25 ? 0.2f : 0.1f;
                    beastMultiplier = petXpBoost * currentBeastRarity.beastcreatMultiplyer;
                } else {
                    beastMultiplier = 0.1f * currentBeastRarity.beastcreatMultiplyer;
                }
            }
        }
        if(skillInfo != null) tamingLevel = skillInfo.get("level_skill_taming").getAsInt();
        //JsonObject petObject = profile.getPetsInfo(profile.getLatestProfile());
        /*JsonObject petsJson = Constants.PETS;
        if(petsJson != null) {
            if(petObject != null) {
                boolean forceUpdateLevels = System.currentTimeMillis() - lastXpGain > 30000;
                Set<String> foundPets = new HashSet<>();
                Set<Pet> addedPets = new HashSet<>();
                for(int i = 0; i < petObject.getAsJsonArray("pets").size(); i++) {
                    JsonElement petElement = petObject.getAsJsonArray("pets").get(i);
                    JsonObject petObj = petElement.getAsJsonObject();
                    Pet pet = new Pet();
                    pet.petType = petObj.get("type").getAsString();
                    Rarity rarity;
                    try {
                        rarity = Rarity.valueOf(petObj.get("tier").getAsString());
                    } catch(Exception ignored) {
                        rarity = Rarity.COMMON;
                    }
                    pet.rarity = rarity;
                    pet.petLevel = GuiProfileViewer.getPetLevel(petsJson.get("pet_levels").getAsJsonArray(), rarity.petOffset, petObj.get("exp").getAsFloat());
                    JsonElement heldItem = petObj.get("heldItem");
                    pet.petItem = heldItem.isJsonNull() ? null : heldItem.getAsString();
                    if(rarity != Rarity.MYTHIC && pet.petItem != null && pet.petItem.equals("PET_ITEM_TIER_BOOST")) {
                        rarity = Rarity.values()[rarity.ordinal()+1];
                    }
                    JsonObject petTypes = petsJson.get("pet_types").getAsJsonObject();
                    pet.petXpType = petTypes.has(pet.petType) ? petTypes.get(pet.petType.toUpperCase()).getAsString().toLowerCase() : "unknown";

                    Pet closest = null;
                    if(petList.containsKey(pet.petType + ";" + pet.rarity.petId)) {
                        closest = getClosestPet(pet);
                        if(addedPets.contains(closest)) {
                            closest = null;
                        }

                        if(closest != null) {
                            if(!forceUpdateLevels || Math.floor(pet.petLevel.level) < Math.floor(closest.petLevel.level)) {
                                pet.petLevel = closest.petLevel;
                            }
                            petList.get(pet.petType + ";" + pet.rarity.petId).remove(closest);
                        }
                    }
                    foundPets.add(pet.petType + ";" + pet.rarity.petId);
                    petList.computeIfAbsent(pet.petType + ";" + pet.rarity.petId, k->new HashSet<>()).add(pet);
                    addedPets.add(pet);

                    if(petObj.get("active").getAsBoolean()) {
                        if(currentPet == null && !setActivePet) {
                            currentPet = pet;
                        } else if(closest == currentPet) {
                            currentPet = pet;
                        }
                    }
                }
                petList.keySet().retainAll(foundPets);
                setActivePet = true;
            }
        }*/
    }

    private float interp(float now, float last) {
        float interp = now;
        if(last >= 0 && last != now) {
            float factor = (System.currentTimeMillis()-lastUpdate)/1000f;
            factor = LerpUtils.clampZeroOne(factor);
            interp = last + (now - last) * factor;
        }
        return interp;
    }

    @Override
    public void updateFrequent() {
        Pet currentPet = getCurrentPet();
        if(!NotEnoughUpdates.INSTANCE.config.petOverlay.enablePetInfo || currentPet == null) {
            overlayStrings = null;
        } else {
            float levelXp = interp(currentPet.petLevel.levelXp, levelXpLast);
            if(levelXp < 0) levelXp = 0;

            String petName = EnumChatFormatting.GREEN + "[Lvl " + (int) currentPet.petLevel.level + "] " + currentPet.rarity.chatFormatting +
                    WordUtils.capitalizeFully(currentPet.petType.replace("_", " "));

            String lvlStringShort = EnumChatFormatting.AQUA + "" + roundFloat(levelXp) + "/" +
                    roundFloat(currentPet.petLevel.currentLevelRequirement)
                    + EnumChatFormatting.YELLOW + " (" + getLevelPercent() + "%)";

            String lvlString = EnumChatFormatting.AQUA + "" + Utils.shortNumberFormat(levelXp, 0) + "/" +
                    Utils.shortNumberFormat(currentPet.petLevel.currentLevelRequirement, 0)
                    + EnumChatFormatting.YELLOW + " (" + getLevelPercent() + "%)";

            float xpGain = interp(xpGainHour, xpGainHourLast);
            if(xpGain < 0) xpGain = 0;
            String xpGainString = EnumChatFormatting.AQUA + "XP/h: " +
                    EnumChatFormatting.YELLOW + roundFloat(xpGain);
            if(xpGain > 0 && xpGainHour == xpGainHourLast) xpGainString += EnumChatFormatting.RED + " (PAUSED)";

            String totalXpString = EnumChatFormatting.AQUA + "Total XP: " + EnumChatFormatting.YELLOW + roundFloat(currentPet.petLevel.totalXp);

            String petItemStr = EnumChatFormatting.AQUA+ "Held Item: " + EnumChatFormatting.RED + "None";
            if(currentPet.petItem != null) {
                JsonObject json = NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(currentPet.petItem);
                if(json != null) {
                    String name = NotEnoughUpdates.INSTANCE.manager.jsonToStack(json).getDisplayName();
                    petItemStr = EnumChatFormatting.AQUA + "Held Item: " + name;
                }
            }

            String etaStr = null;
            String etaMaxStr = null;
            if(currentPet.petLevel.level < 100) {
                float remaining = currentPet.petLevel.currentLevelRequirement - currentPet.petLevel.levelXp;
                if(remaining > 0) {
                    if(xpGain < 1000) {
                        etaStr = EnumChatFormatting.AQUA+"Until L"+(int)(currentPet.petLevel.level+1)+": " +
                                EnumChatFormatting.YELLOW+"N/A";
                    } else {
                        etaStr = EnumChatFormatting.AQUA+"Until L"+(int)(currentPet.petLevel.level+1)+": " +
                                EnumChatFormatting.YELLOW + Utils.prettyTime((long)(remaining)*1000*60*60/(long)xpGain);
                    }
                }

                float remainingMax = currentPet.petLevel.maxXP - currentPet.petLevel.totalXp;
                if(remaining > 0) {
                    if(xpGain < 1000) {
                        etaMaxStr = EnumChatFormatting.AQUA+"Until L100: " +
                                EnumChatFormatting.YELLOW+"N/A";
                    } else {
                        etaMaxStr = EnumChatFormatting.AQUA+"Until L100: " +
                                EnumChatFormatting.YELLOW + Utils.prettyTime((long)(remainingMax)*1000*60*60/(long)xpGain);
                    }
                }
            }

            overlayStrings = new ArrayList<>();

            for(int index : NotEnoughUpdates.INSTANCE.config.petOverlay.petOverlayText) {
                switch(index) {
                    case 0:
                        overlayStrings.add(petName); break;
                    case 1:
                        overlayStrings.add(lvlStringShort); break;
                    case 2:
                        overlayStrings.add(lvlString); break;
                    case 3:
                        overlayStrings.add(xpGainString); break;
                    case 4:
                        overlayStrings.add(totalXpString); break;
                    case 5:
                        overlayStrings.add(petItemStr); break;
                    case 6:
                        if(etaStr != null) overlayStrings.add(etaStr); break;
                    case 7:
                        if(etaMaxStr != null) overlayStrings.add(etaMaxStr); break;
                }
            }

        }
    }

    public void update() {
        if(!NotEnoughUpdates.INSTANCE.config.petOverlay.enablePetInfo && !NotEnoughUpdates.INSTANCE.config.itemOverlays.enableMonkeyCheck) {
            overlayStrings = null;
            return;
        }

        NEUConfig config = NotEnoughUpdates.INSTANCE.config;
        int updateTime = 60000;
        if((config.itemOverlays.enableMonkeyCheck) && !config.petOverlay.enablePetInfo)
            updateTime = 300000;

        if(NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) {
            /*if(petList.isEmpty()) {
                ProfileViewer.Profile profile = NotEnoughUpdates.profileViewer.getProfileRaw(Minecraft.getMinecraft().thePlayer
                        .getUniqueID().toString().replace("-", ""));
                if(profile != null) {
                    getAndSetPet(profile);
                }
            }*/

            ProfileApiSyncer.getInstance().requestResync("petinfo", updateTime, () -> {
            }, PetInfoOverlay::getAndSetPet);
        }

        Pet currentPet = getCurrentPet();
        if(currentPet == null) {
            overlayStrings = null;
        } else {
            lastUpdate = System.currentTimeMillis();
            levelXpLast = currentPet.petLevel.levelXp;
            updatePetLevels();
        }
    }

    private GuiProfileViewer.PetLevel getLevel(JsonArray levels, int offset, int xpThisLevel, int xpMaxThisLevel) {
        float xpTotal = 0;
        float level = 1;
        float currentLevelRequirement = 0;
        float exp = xpThisLevel;

        boolean addLevel = true;

        for(int i=offset; i<offset+99; i++) {
            if(addLevel) {
                currentLevelRequirement = levels.get(i).getAsFloat();
                xpTotal += currentLevelRequirement;

                if(currentLevelRequirement >= xpMaxThisLevel) {
                    addLevel = false;
                } else {
                    exp += currentLevelRequirement;
                    level += 1;
                }
            } else {
                xpTotal += levels.get(i).getAsFloat();
            }
        }

        level += xpThisLevel/currentLevelRequirement;
        if(level <= 0) {
            level = 1;
        } else if(level > 100) {
            level = 100;
        }
        GuiProfileViewer.PetLevel levelObj = new GuiProfileViewer.PetLevel();
        levelObj.level = level;
        levelObj.currentLevelRequirement = currentLevelRequirement;
        levelObj.maxXP = xpTotal;
        levelObj.levelPercentage = xpThisLevel/currentLevelRequirement;
        levelObj.levelXp = xpThisLevel;
        levelObj.totalXp = exp;
        return levelObj;
    }

    public Pet getPetFromStack(String[] lore) {
        String petType = null;
        Rarity rarity = null;
        String heldItem = null;
        GuiProfileViewer.PetLevel level = null;

        for(String line : lore) {
            Matcher petNameMatcher = PET_NAME_PATTERN.matcher(line);
            Matcher xpLineMatcher = XP_LINE_PATTERN.matcher(Utils.cleanColour(line));
            if(petNameMatcher.matches()) {
                String petStringMatch = petNameMatcher.group(1);

                char colChar = petStringMatch.charAt(0);
                EnumChatFormatting col = EnumChatFormatting.RESET;
                for(EnumChatFormatting formatting : EnumChatFormatting.values()) {
                    if(formatting.toString().equals("\u00a7"+colChar)) {
                        col = formatting;
                        break;
                    }
                }

                rarity = Rarity.COMMON;
                if(col != EnumChatFormatting.RESET) {
                    rarity = Rarity.getRarityFromColor(col);
                }

                petType = Utils.cleanColour(petStringMatch.substring(1))
                        .replaceAll("[^\\w ]", "").trim()
                        .replace(" ", "_").toUpperCase();
            } else if(line.startsWith("\u00a76Held Item: ")) {
                String after = line.substring("\u00a76Held Item: ".length());

                if(itemMap == null) {
                    itemMap = new HashMap<>();

                    for(Map.Entry<String, JsonObject> entry : NotEnoughUpdates.INSTANCE.manager.getItemInformation().entrySet()) {
                        boolean petItem = false;

                        if(entry.getKey().startsWith("PET_ITEM_")) {
                            petItem = true;
                        } else {
                            String[] itemLore = NotEnoughUpdates.INSTANCE.manager.getLoreFromNBT(NotEnoughUpdates.INSTANCE.manager.
                                    jsonToStack(entry.getValue()).getTagCompound());

                            for(String itemLoreLine : itemLore) {
                                if(itemLoreLine.contains("PET ITEM")) {
                                    petItem = true;
                                    break;
                                }
                            }
                        }

                        if(petItem) {
                            ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(entry.getValue());
                            itemMap.put(stack.getDisplayName(), entry.getKey());
                        }
                    }
                }

                if(itemMap.containsKey(after)) {
                    heldItem = itemMap.get(after);
                }
            } else if(xpLineMatcher.matches() && rarity != null) {
                String xpThisLevelS = xpLineMatcher.group(1);
                String xpMaxThisLevelS = xpLineMatcher.group(2);

                try {
                    int xpThisLevel = Integer.parseInt(xpThisLevelS);
                    int xpMaxThisLevel = Integer.parseInt(xpMaxThisLevelS);

                    level = getLevel(Constants.PETS.get("pet_levels").getAsJsonArray(), rarity.petOffset, xpThisLevel, xpMaxThisLevel);
                } catch(NumberFormatException ignored) {}
            }
        }

        if(level != null && petType != null && rarity != null) {
            Pet pet = new Pet();
            pet.petItem = heldItem;
            pet.petLevel = level;
            pet.rarity = rarity;
            pet.petType = petType;
            JsonObject petTypes = Constants.PETS.get("pet_types").getAsJsonObject();
            pet.petXpType = petTypes.has(pet.petType) ? petTypes.get(pet.petType.toUpperCase()).getAsString().toLowerCase() : "unknown";

            return pet;
        }

        return null;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if(Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
            if(Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
                GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;
                ContainerChest container = (ContainerChest) chest.inventorySlots;
                IInventory lower = container.getLowerChestInventory();
                String containerName = lower.getDisplayName().getUnformattedText();

                if(lower.getSizeInventory() >= 54) {
                    int page = 0;
                    int maxPage = 1;
                    boolean isPets = false;

                    if(containerName.equals("Pets")) {
                        isPets = true;
                    } else {
                        Matcher matcher = PET_CONTAINER_PAGE.matcher(containerName);
                        if(matcher.matches()) {
                            try {
                                page = Integer.parseInt(matcher.group(1));
                                maxPage = Integer.parseInt(matcher.group(2));
                                isPets = true;
                            } catch(NumberFormatException ignored) {}
                        }
                    }

                    if(isPets) {
                        Set<Integer> clear = new HashSet<>();
                        for(int i : petMap.keySet()) {
                            if(i >= maxPage*28) {
                                clear.add(i);
                            }
                        }
                        petMap.keySet().removeAll(clear);

                        for(int index=0; index<28; index++) {
                            int petIndex = page*28+index;
                            int itemIndex = 10+index;

                            ItemStack stack = lower.getStackInSlot(itemIndex);

                            if(stack == null) {
                                petMap.remove(petIndex);
                            } else {
                                String[] lore = NotEnoughUpdates.INSTANCE.manager.getLoreFromNBT(stack.getTagCompound());
                                petMap.put(petIndex, getPetFromStack(lore));

                                for(String line : lore) {
                                    if(line.equals("\u00a77\u00a7cClick to despawn ")) {
                                        selectedPet = petIndex;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected Vector2f getSize(List<String> strings) {
        if(!NotEnoughUpdates.INSTANCE.config.petOverlay.petOverlayIcon) return super.getSize(strings);
        return super.getSize(strings).translate(25, 0);
    }

    @Override
    protected Vector2f getTextOffset() {
        if(!NotEnoughUpdates.INSTANCE.config.petOverlay.petOverlayIcon) return super.getTextOffset();
        if(this.styleSupplier.get() != TextOverlayStyle.BACKGROUND) return super.getTextOffset().translate(30, 0);
        return super.getTextOffset().translate(25, 0);
    }

    @Override
    public void renderDummy() {
        super.renderDummy();

        if(!NotEnoughUpdates.INSTANCE.config.petOverlay.petOverlayIcon) return;

        JsonObject petItem = NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("ROCK;0");
        if(petItem != null) {
            Vector2f position = getPosition(overlayWidth, overlayHeight);
            int x = (int)position.x;
            int y = (int)position.y;

            ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(petItem);
            GlStateManager.enableDepth();
            GlStateManager.pushMatrix();
            GlStateManager.translate(x-2, y-2, 0);
            GlStateManager.scale(2, 2, 1);
            Utils.drawItemStack(stack, 0, 0);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void render() {
        super.render();

        Pet currentPet = getCurrentPet();
        if(currentPet == null) {
            overlayStrings = null;
            return;
        }

        if(overlayStrings == null) {
            return;
        }

        if(!NotEnoughUpdates.INSTANCE.config.petOverlay.petOverlayIcon) return;

        JsonObject petItem = NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(currentPet.petType + ";" + currentPet.rarity.petId);
        if(petItem != null) {
            Vector2f position = getPosition(overlayWidth, overlayHeight);
            int x = (int)position.x;
            int y = (int)position.y;

            ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(petItem);
            GlStateManager.enableDepth();
            GlStateManager.pushMatrix();
            GlStateManager.translate(x-2, y-2, 0);
            GlStateManager.scale(2, 2, 1);
            Utils.drawItemStack(stack, 0, 0);
            GlStateManager.popMatrix();
        }
    }

    public static float getBoostMultiplier(String boostName) {
        if(boostName == null) return 1;
        boostName = boostName.toLowerCase();
        if(boostName.equalsIgnoreCase("PET_ITEM_ALL_SKILLS_BOOST_COMMON")) {
            return 1.1f;
        } else if(boostName.equalsIgnoreCase("ALL_SKILLS_SUPER_BOOST")) {
            return 1.2f;
        } else if(boostName.endsWith("epic")) {
            return 1.5f;
        } else if(boostName.endsWith("rare")) {
            return 1.4f;
        } else if(boostName.endsWith("uncommon")) {
            return 1.3f;
        } else if(boostName.endsWith("common")) {
            return 1.2f;
        } else {
            return 1;
        }
    }

    private static List<String> validXpTypes = Lists.newArrayList("mining","foraging","enchanting","farming","combat","fishing","alchemy");

    public static float getXpGain(Pet pet, float xp, String xpType) {
        if(validXpTypes == null) validXpTypes = Lists.newArrayList("mining","foraging","enchanting","farming","combat","fishing","alchemy");
        if(!validXpTypes.contains(xpType.toLowerCase())) return 0;

        float tamingPercent = 1.0f + (tamingLevel / 100f);
        xp = xp * tamingPercent;
        xp = xp + (xp * beastMultiplier / 100f);
        if(pet.petXpType != null && !pet.petXpType.equalsIgnoreCase(xpType)) {
            xp = xp / 3f;

            if(xpType.equalsIgnoreCase("alchemy") || xpType.equalsIgnoreCase("enchanting")) {
                xp = xp / 4f;
            }
        }
        if(xpType.equalsIgnoreCase("mining") || xpType.equalsIgnoreCase("fishing")) {
            xp = xp * 1.5f;
        }
        if(pet.petItem != null) {
            Matcher petItemMatcher = XP_BOOST_PATTERN.matcher(pet.petItem);
            if((petItemMatcher.matches() && petItemMatcher.group(1).equalsIgnoreCase(xpType))
                    || pet.petItem.equalsIgnoreCase("ALL_SKILLS_SUPER_BOOST")) {
                xp = xp * getBoostMultiplier(pet.petItem);
            }
        }
        return xp;
    }

    private final HashMap<String, Float> skillInfoMapLast = new HashMap<>();
    public void updatePetLevels() {
        HashMap<String, XPInformation.SkillInfo> skillInfoMap = XPInformation.getInstance().getSkillInfoMap();

        long currentTime = System.currentTimeMillis();

        float totalGain = 0;

        Pet currentPet = getCurrentPet();
        for(Map.Entry<String, XPInformation.SkillInfo> entry : skillInfoMap.entrySet()) {
            if(entry.getValue().level == 50 && entry.getValue().fromApi) continue;

            float skillXp = entry.getValue().totalXp;
            if(skillInfoMapLast.containsKey(entry.getKey())) {
                float skillXpLast = skillInfoMapLast.get(entry.getKey());

                if(skillXpLast <= 0) {
                    skillInfoMapLast.put(entry.getKey(), skillXp);
                } else if(skillXp > skillXpLast) {
                    lastXpGain = currentTime;

                    float deltaXp = skillXp - skillXpLast;

                    float gain = getXpGain(currentPet, deltaXp, entry.getKey().toUpperCase());
                    totalGain += gain;

                    skillInfoMapLast.put(entry.getKey(), skillXp);
                }
            } else {
                skillInfoMapLast.put(entry.getKey(), skillXp);
            }
        }

        xpGainHourLast = xpGainHour;
        if(xpAddTimer > 0 || totalGain > 0) {
            if(totalGain > 0) {
                xpAddTimer = 10;
            } else {
                xpAddTimer--;
            }

            currentPet.petLevel.totalXp += totalGain;

            xpGainQueue.add(0, totalGain);
            while(xpGainQueue.size() > 30) {
                xpGainQueue.removeLast();
            }

            if(xpGainQueue.size() > 1) {
                float tot = 0;
                float greatest = 0;
                for(float f : xpGainQueue) {
                    tot += f;
                    greatest = Math.max(greatest, f);
                }

                xpGainHour = (tot-greatest)*(60*60)/(xpGainQueue.size()-1);
            }
        }

        JsonObject petsJson = Constants.PETS;
        if(currentPet != null && petsJson != null) {
            currentPet.petLevel = GuiProfileViewer.getPetLevel(petsJson.get("pet_levels").getAsJsonArray(), currentPet.rarity.petOffset, currentPet.petLevel.totalXp);
        }
    }

    public String roundFloat(float f) {
        if(f % 1 < 0.05f) {
            return NumberFormat.getNumberInstance().format((int)f);
        } else {
            String s = Utils.floatToString(f, 1);
            if(s.contains(".")) {
                return NumberFormat.getNumberInstance().format((int)f) + '.' + s.split("\\.")[1];
            } else if(s.contains(",")) {
                return NumberFormat.getNumberInstance().format((int)f) + ',' + s.split(",")[1];
            } else {
                return s;
            }
        }
    }

    @SubscribeEvent
    public void switchWorld(WorldEvent.Load event) {
        if(NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) {
            ProfileApiSyncer.getInstance().requestResync("petinfo_quick", 10000, () -> {
            }, PetInfoOverlay::getAndSetPet);
        }
    }

    private int lastLevelHovered = 0;
    private String lastItemHovered = null;

    private HashMap<String, String> itemMap = null;

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onTooltip(ItemTooltipEvent event) {
        for(String line : event.toolTip) {
            if(line.startsWith("\u00a7o\u00a77[Lvl ")) {
                lastItemHovered = null;

                String after = line.substring("\u00a7o\u00a77[Lvl ".length());
                if(after.contains("]")) {
                    String levelStr = after.split("]")[0];

                    try {
                        lastLevelHovered = Integer.parseInt(levelStr.trim());
                    } catch(Exception ignored) {}
                }
            } else if(line.startsWith("\u00a75\u00a7o\u00a76Held Item: ")) {
                String after = line.substring("\u00a75\u00a7o\u00a76Held Item: ".length());

                if(itemMap == null) {
                    itemMap = new HashMap<>();

                    for(Map.Entry<String, JsonObject> entry : NotEnoughUpdates.INSTANCE.manager.getItemInformation().entrySet()) {
                        if(entry.getKey().equals("ALL_SKILLS_SUPER_BOOST") ||
                                XP_BOOST_PATTERN.matcher(entry.getKey()).matches()) {
                            ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(entry.getValue());
                            itemMap.put(stack.getDisplayName(), entry.getKey());
                        }
                    }
                }

                if(itemMap.containsKey(after)) {
                    lastItemHovered = itemMap.get(after);
                }
            }
        }
    }
    
    private static final Pattern AUTOPET_EQUIP = Pattern.compile("\u00a7cAutopet \u00a7eequipped your \u00a77\\[Lvl (\\d+)] \u00a7(.{2,})\u00a7e! \u00a7a\u00a7lVIEW RULE\u00a7r");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChatReceived(ClientChatReceivedEvent event) {
        NEUConfig config = NotEnoughUpdates.INSTANCE.config;
        if(NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() && (config.petOverlay.enablePetInfo || config.itemOverlays.enableMonkeyCheck)) {
            if(event.type == 0) {
                String chatMessage = Utils.cleanColour(event.message.getUnformattedText());

                Matcher autopetMatcher = AUTOPET_EQUIP.matcher(event.message.getFormattedText());
                if(autopetMatcher.matches()) {
                    try {
                        lastLevelHovered = Integer.parseInt(autopetMatcher.group(1));
                    } catch(NumberFormatException ignored) {}

                    String petStringMatch = autopetMatcher.group(2);
                    char colChar = petStringMatch.charAt(0);
                    EnumChatFormatting col = EnumChatFormatting.RESET;
                    for(EnumChatFormatting formatting : EnumChatFormatting.values()) {
                        if(formatting.toString().equals("\u00a7"+colChar)) {
                            col = formatting;
                            break;
                        }

                    }
                    Rarity rarity = Rarity.COMMON;
                    if(col != EnumChatFormatting.RESET) {
                        rarity = Rarity.getRarityFromColor(col);
                    }

                    String pet = Utils.cleanColour(petStringMatch.substring(1))
                            .replaceAll("[^\\w ]", "").trim()
                            .replace(" ", "_").toUpperCase();

                    selectedPet = getClosestPetIndex(pet, rarity.petId, "", lastLevelHovered);
                    if(selectedPet == -1) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED+"[NEU] Can't find pet \u00a7" + petStringMatch +
                                EnumChatFormatting.RED + " try visiting all pages of /pets."));
                    }
                } else if(chatMessage.toLowerCase().startsWith("you summoned your")) {
                    clearPet();

                    String pet = chatMessage.trim().toUpperCase().replace("YOU SUMMONED YOUR ", "")
                            .replaceAll("[^\\w ]", "").trim()
                            .replace(" ", "_");
                    Rarity rarity = event.message.getSiblings().size() == 3 ? Rarity.getRarityFromColor(event.message.getSiblings().get(1).getChatStyle().getColor()) : Rarity.COMMON;

                    selectedPet = getClosestPetIndex(pet, rarity.petId, lastItemHovered, lastLevelHovered);

                    if(selectedPet == -1) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED+"[NEU] Can't find pet " + pet+";"+rarity.petId +
                                EnumChatFormatting.RED + " try visiting all pages of /pets."));
                    }
                } else if(chatMessage.toLowerCase().startsWith("you despawned your")) {
                    clearPet();
                } else if(chatMessage.toLowerCase().contains("switching to profile")) {
                    clearPet();
                }
            }
        }
    }
}
