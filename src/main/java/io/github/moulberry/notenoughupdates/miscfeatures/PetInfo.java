package io.github.moulberry.notenoughupdates.miscfeatures;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.config.gui.GuiPositionEditor;
import io.github.moulberry.notenoughupdates.options.NEUConfig;
import io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer;
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewer;
import io.github.moulberry.notenoughupdates.util.Constants;
import io.github.moulberry.notenoughupdates.util.ProfileApiSyncer;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.text.WordUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PetInfo {

    private static final Pattern XP_GAIN_AND_SKILL_PATTERN = Pattern.compile("\\+(\\d*\\.?\\d*) (Farming|Mining|Combat|Foraging|Fishing|Enchanting|Alchemy) (\\(([0-9.,]+)/([0-9.,]+)\\))");
    private static final Pattern XP_BOOST_PATTERN = Pattern.compile("PET_ITEM_(COMBAT|FISHING|MINING|FORAGING|ALL|FARMING)_(SKILL|SKILLS)_BOOST_(COMMON|UNCOMMON|RARE|EPIC)");

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

        Rarity(int petOffset, int petId, int beastcreatMultiplyer, EnumChatFormatting chatFormatting){
            this.chatFormatting = chatFormatting;
            this.petOffset = petOffset;
            this.petId = petId;
            this.beastcreatMultiplyer = beastcreatMultiplyer;
        }

        public static Rarity getRarityFromColor(EnumChatFormatting chatFormatting){
            for (int i = 0; i < Rarity.values().length; i++) {
                if (Rarity.values()[i].chatFormatting.equals(chatFormatting))
                    return Rarity.values()[i];
            }
            return COMMON;
        }
    }

    public static class pet {
        public String petType;
        public double petExp;
        public Rarity rarity;
        public GuiProfileViewer.PetLevel petLevel;
        public String petXpType;
        public String petItem;
    }

    public static pet currentPet = null;
    public static HashMap<String, pet> petList = new HashMap<>();

    public static double currentXp = 0.0;
    public static String currentXpType = "";
    public static int tamingLevel = 1;
    public static double beastMultiplier = 0;
    public static boolean ignoreNextXp = false;

    public static void clearPet(){ currentPet = null; }

    public float getLevelPercent(){
        DecimalFormat df = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        try {
            return Float.parseFloat(df.format(currentPet.petLevel.levelPercentage * 100f ));
        }catch (Exception ignored){ return 0; }
    }

    private static void getAndSetPet(ProfileViewer.Profile profile) {
        JsonObject petObject = profile.getPetsInfo(profile.getLatestProfile());
        JsonObject skillInfo = profile.getSkillInfo(profile.getLatestProfile());
        JsonObject invInfo = profile.getInventoryInfo(profile.getLatestProfile());
        JsonObject profileInfo = profile.getProfileInformation(profile.getLatestProfile());
        if (invInfo != null && profileInfo != null){
            JsonObject stats = profileInfo.get("stats").getAsJsonObject();
            boolean hasBeastmasterCrest = false;
            Rarity currentBeastRarity = Rarity.COMMON;
            for (JsonElement talisman : invInfo.get("talisman_bag").getAsJsonArray()) {
                if (talisman.isJsonNull()) continue;
                String internalName = talisman.getAsJsonObject().get("internalname").getAsString();
                if (internalName.startsWith("BEASTMASTER_CREST")) {
                    hasBeastmasterCrest = true;
                    try {
                        Rarity talismanRarity = Rarity.valueOf(internalName.replace("BEASTMASTER_CREST_", ""));
                        if (talismanRarity.beastcreatMultiplyer > currentBeastRarity.beastcreatMultiplyer) currentBeastRarity = talismanRarity;
                    } catch (Exception ignored) {}
                }
            }
            if (hasBeastmasterCrest) {
                if (stats.has("mythos_kills")) {
                    int mk = stats.get("mythos_kills").getAsInt();
                    double petXpBoost = mk > 10000 ? 1 : mk > 7500 ? 0.9 : mk > 5000 ? 0.8 : mk > 2500 ? 0.7 :
                    mk > 1000 ? 0.6 : mk > 500 ? 0.5 : mk > 250 ? 0.4 : mk > 100 ? 0.3 : mk > 25 ? 0.2 : 0.1;
                    beastMultiplier = petXpBoost * currentBeastRarity.beastcreatMultiplyer;
                }else beastMultiplier = 0.1 * currentBeastRarity.beastcreatMultiplyer;
            }
        }
        if (skillInfo != null) tamingLevel = skillInfo.get("level_skill_taming").getAsInt();
        JsonObject petsJson = Constants.PETS;
        if (petsJson != null) {
            if (petObject != null) {
                boolean hasActivePet = false;
                petList.clear();

                for (int i = 0; i < petObject.getAsJsonArray("pets").size(); i++) {
                    JsonElement petElement = petObject.getAsJsonArray("pets").get(i);
                    JsonObject petObj = petElement.getAsJsonObject();
                    pet pet = new pet();
                    pet.petType = petObj.get("type").getAsString();
                    Rarity rarity;
                    try {
                        rarity = Rarity.valueOf(petObj.get("tier").getAsString());
                    } catch (Exception ignored) {
                        rarity = Rarity.COMMON;
                    }
                    pet.rarity = rarity;
                    pet.petExp = petObj.get("exp").getAsDouble();
                    pet.petLevel = GuiProfileViewer.getPetLevel(petsJson.get("pet_levels").getAsJsonArray(), rarity.petOffset, (float) pet.petExp);
                    JsonElement heldItem = petObj.get("heldItem");
                    pet.petItem = heldItem.isJsonNull() ? null : heldItem.getAsString();
                    JsonObject petTypes = petsJson.get("pet_types").getAsJsonObject();
                    pet.petXpType = petTypes.has(pet.petType) ? petTypes.get(pet.petType.toUpperCase()).getAsString().toLowerCase() : "unknown";

                    petList.put(pet.petType + ";" + pet.rarity.petId, pet);
                    if (petObj.get("active").getAsBoolean()) {
                        hasActivePet = true;
                        if (currentPet == null || (pet.petType.equalsIgnoreCase(currentPet.petType) && pet.rarity.equals(currentPet.rarity))) {
                            if (currentPet != null && ((currentPet.petLevel.level == pet.petLevel.level &&  currentPet.petLevel.levelPercentage > pet.petLevel.levelPercentage) || currentPet.petLevel.level > pet.petLevel.level))
                                pet.petLevel = currentPet.petLevel;
                            currentPet = pet;
                        }
                    }
                }
                if (!hasActivePet){
                    clearPet();
                }
            }
        }
    }

    public static void longTick(){
        NEUConfig config = NotEnoughUpdates.INSTANCE.config;
        int updateTime = 90000;
        if ((config.treecap.enableMonkeyCheck || config.notifications.showWrongPetMsg) && !config.overlay.enablePetInfo) updateTime = 300000;

        if (NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()){
            ProfileApiSyncer.getInstance().requestResync("petinfo", updateTime, () -> {}, PetInfo::getAndSetPet);
        }
    }

    public static float getCurrentLevelReqs(float level, pet pet){
        JsonObject petsJson = Constants.PETS;
        if (petsJson != null){
            return petsJson.get("pet_levels").getAsJsonArray().get((int) (level+pet.rarity.petOffset)).getAsFloat();
        }
        return 0;
    }

    public static double getBoostMultiplyer(String boostName){
        if (boostName == null) return 1;
        if (boostName.equalsIgnoreCase("PET_ITEM_ALL_SKILLS_BOOST_COMMON")) {
            return 1.1;
        }else if (boostName.equalsIgnoreCase("ALL_SKILLS_SUPER_BOOST")){
            return 1.2;
        }else if (boostName.endsWith("epic")){
            return 1.5;
        }else if (boostName.endsWith("rare")){
            return 1.4;
        }else if (boostName.endsWith("uncommon")){
            return 1.3;
        }else if (boostName.endsWith("common")){
            return 1.2;
        }
        else return 1;
    }

    public static double getXpGain(pet pet, double xp, String xpType){
        double tamingPercent = 1.0 + (tamingLevel / 100f);
        xp = xp * tamingPercent;
        xp = xp + (xp * beastMultiplier);
        if (pet.petXpType != null && !pet.petXpType.equalsIgnoreCase(xpType)){
            xp = ((xpType.equalsIgnoreCase("alchemy") && !pet.petXpType.equalsIgnoreCase("alchemy")) || xpType.equalsIgnoreCase("enchanting") ) ?
                    xp * 0.08 : xp * 0.33;
        }
        if (xpType.equalsIgnoreCase("mining") || xpType.equalsIgnoreCase("fishing")){
            xp = xp * 1.5;
        }
        if (pet.petItem != null) {
            Matcher petItemMatcher = XP_BOOST_PATTERN.matcher(pet.petItem);
            if ((petItemMatcher.matches() && petItemMatcher.group(1).equalsIgnoreCase(pet.petXpType)) || pet.petItem.equalsIgnoreCase("ALL_SKILLS_SUPER_BOOST"))
                xp = xp * getBoostMultiplyer(pet.petItem);
        }
        return xp;
    }


    @SubscribeEvent
    public void onOverlayDrawn(RenderGameOverlayEvent.Post event) {
        NEUConfig config = NotEnoughUpdates.INSTANCE.config;
        if(NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() && config.overlay.enablePetInfo && ((event.type == null && Loader.isModLoaded("labymod")) ||
                event.type == RenderGameOverlayEvent.ElementType.ALL)
        ){
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.gameSettings.showDebugInfo ||
                    (mc.gameSettings.keyBindPlayerList.isKeyDown() &&
                            (!mc.isIntegratedServerRunning() ||
                                    mc.thePlayer.sendQueue.getPlayerInfoMap().size() > 1))) {
                return;
            }

            if (currentPet != null && currentPet.petLevel != null && !currentPet.petType.isEmpty()) {
                ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());

                FontRenderer font = mc.fontRendererObj;

                int overlayStyle = config.overlay.petInfoOverlayStyle;

                String petName = EnumChatFormatting.GREEN + "[Lvl " + (int) currentPet.petLevel.level + "] " + currentPet.rarity.chatFormatting +
                        WordUtils.capitalizeFully(currentPet.petType.replace("_", " "));
                String lvlString = EnumChatFormatting.AQUA + "" + Utils.shortNumberFormat((currentPet.petLevel.currentLevelRequirement * currentPet.petLevel.levelPercentage), 0) + "/" + Utils.shortNumberFormat(currentPet.petLevel.currentLevelRequirement, 0) + EnumChatFormatting.YELLOW + " (" + getLevelPercent() + "%)";

                int xPos = config.overlay.petInfoPosition.getAbsX(scaledResolution, Math.max(font.getStringWidth(petName), font.getStringWidth(lvlString)) + 20);
                int yPos = config.overlay.petInfoPosition.getAbsY(scaledResolution, (currentPet.petLevel.level < 100 ? 22 : 11)) + 2;

                if (!(mc.currentScreen instanceof GuiPositionEditor) && overlayStyle == 0)
                    Gui.drawRect(xPos, yPos-2, xPos+Math.max(font.getStringWidth(lvlString), font.getStringWidth(petName))+20, yPos+(currentPet.petLevel.level < 100 ? 20 : 16), 0x80000000);

                if (overlayStyle == 3) {
                    for (int xO = -2; xO <= 2; xO++) {
                        for (int yO = -2; yO <= 2; yO++) {
                            if (Math.abs(xO) != Math.abs(yO)) {
                                font.drawString(Utils.cleanColour(petName), xPos + 20 + xO / 2f, yPos + (currentPet.petLevel.level < 100 ? 0 : 4) + yO / 2f, 0x000000, false);
                            }
                        }
                    }
                }

                font.drawString(petName, xPos + 20, yPos + (currentPet.petLevel.level < 100 ? 0 : 4), 0xffffff, overlayStyle == 2);
                if (currentPet.petLevel.level < 100){
                    if (overlayStyle == 3) {
                        for (int xO = -2; xO <= 2; xO++) {
                            for (int yO = -2; yO <= 2; yO++) {
                                if (Math.abs(xO) != Math.abs(yO)) {
                                    font.drawString(Utils.cleanColour(lvlString), xPos + 20 + xO / 2f, yPos + font.FONT_HEIGHT + yO / 2f, 0x000000, false);
                                }
                            }
                        }
                    }
                    font.drawString(lvlString, xPos + 20, yPos + font.FONT_HEIGHT, 0xffffff, overlayStyle == 2);
                }

                JsonObject petItem = NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(currentPet.petType+";"+currentPet.rarity.petId);
                if(petItem != null) {
                    ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(petItem, false, false, false);
                    Utils.drawItemStack(stack, xPos, yPos);
                }
                GlStateManager.color(0,0,0);
            }
        }
    }

    @SubscribeEvent
    public void switchWorld(WorldEvent.Load event) {
        if (NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) {
            ProfileApiSyncer.getInstance().requestResync("petinfo", 10000, () -> {
            }, PetInfo::getAndSetPet);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChatReceived(ClientChatReceivedEvent event) {
        NEUConfig config = NotEnoughUpdates.INSTANCE.config;
        if (NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() && (config.overlay.enablePetInfo || config.treecap.enableMonkeyCheck || config.notifications.showWrongPetMsg)) {
            if (event.type == 0) {
                String chatMessage = Utils.cleanColour(event.message.getUnformattedText());
                String petLevelMessage = "your " + (currentPet != null ? currentPet.petType.toLowerCase().replace("_", " ") : "") + " levelled up to level";
                if (chatMessage.toLowerCase().startsWith("you summoned your")) {
                    String pet = chatMessage.trim().toUpperCase().replace("YOU SUMMONED YOUR ", "").replace("!", "").replace(" ", "_");
                    Rarity rarity = event.message.getSiblings().size() == 3 ? Rarity.getRarityFromColor(event.message.getSiblings().get(1).getChatStyle().getColor()) : Rarity.COMMON;
                    if (petList.containsKey(pet + ";" + rarity.petId)) {
                        if (currentPet != null) petList.put(currentPet.petType + ";" + currentPet.rarity.petId, currentPet);
                        pet summonedPet = new pet();
                        summonedPet.petType = pet;
                        summonedPet.rarity = rarity;
                        summonedPet.petLevel = petList.get(pet + ";" + rarity.petId).petLevel;
                        summonedPet.petXpType = petList.get(pet + ";" + rarity.petId).petXpType;
                        currentPet = summonedPet;
                    }
                } else if (chatMessage.toLowerCase().startsWith("you despawned your")) {
                    if (currentPet != null) {
                        petList.put(currentPet.petType + ";" + currentPet.rarity.petId, currentPet);
                    }
                    clearPet();
                } else if (chatMessage.toLowerCase().startsWith(petLevelMessage)) {
                    if (currentPet != null) {
                        try {
                            ignoreNextXp = true;
                            currentPet.petLevel.level = Integer.parseInt(chatMessage.toLowerCase().replace(petLevelMessage, "").replace("!", "").replace(" ", ""));
                            currentPet.petLevel.levelPercentage = 0;
                            currentPet.petLevel.currentLevelRequirement = getCurrentLevelReqs(currentPet.petLevel.level, currentPet);
                        } catch (Exception ignored) {}
                    }
                } else if (chatMessage.toLowerCase().contains("switching to profile")) {
                    clearPet();
                    petList.clear();
                }
            }
            if (event.type == 2) {
                String[] parts = Utils.cleanColour(event.message.getUnformattedText()).split(" {3,}");
                if (parts.length == 3) {
                    Matcher matcher = XP_GAIN_AND_SKILL_PATTERN.matcher(parts[1].trim());
                    if (currentPet != null && matcher.matches()) {
                        String oldXpType = currentXpType;
                        currentXpType = matcher.group(2);
                        try {
                            double actionXp = Double.parseDouble(matcher.group(4).replace(",", ""));
                            if (actionXp != currentXp && actionXp != 0) {
                                if (NotEnoughUpdates.INSTANCE.config.notifications.showWrongPetMsg &&
                                        currentXpType.equalsIgnoreCase(oldXpType) &&
                                        !currentXpType.equalsIgnoreCase(currentPet.petXpType)
                                ){
                                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED +
                                            "[NEU]" + EnumChatFormatting.GOLD + " \u26A0 You're using a " + WordUtils.capitalizeFully(currentPet.petXpType) + " pet while gathering " + WordUtils.capitalizeFully(currentXpType) + " skill xp."));
                                }
                                double xpGain = !currentXpType.equalsIgnoreCase(oldXpType) ? Double.parseDouble(matcher.group(1)) : actionXp - currentXp;
                                currentXp = actionXp;
                                double xp = currentPet.petLevel.levelPercentage * currentPet.petLevel.currentLevelRequirement;
                                double newXp = xp + getXpGain(currentPet, xpGain, currentXpType);
                                if (ignoreNextXp) {
                                    //TODO : This needs to be changed to a better system as you can lose accuracy of levels with this,
                                    // will fix it self when it syncs to the api
                                    ignoreNextXp = false;
                                } else {
                                    currentPet.petExp = newXp;
                                    currentPet.petLevel.levelPercentage = (float) (currentPet.petExp / currentPet.petLevel.currentLevelRequirement);
                                }
                            }
                        }catch (NumberFormatException ignored){}
                    }
                }
            }
        }
    }
}
