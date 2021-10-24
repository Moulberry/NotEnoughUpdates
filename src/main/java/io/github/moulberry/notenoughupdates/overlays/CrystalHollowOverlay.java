package io.github.moulberry.notenoughupdates.overlays;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.miscfeatures.StorageManager;
import io.github.moulberry.notenoughupdates.options.NEUConfig;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.util.vector.Vector2f;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrystalHollowOverlay extends TextOverlay {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final StorageManager storageManager = StorageManager.getInstance();
    private final Pattern notFoundPattern = Pattern.compile("\\[NPC] Keeper of \\w+: Talk to me when you have found a (?<item>[a-z-A-Z ]+)!");
    private final Pattern foundPattern = Pattern.compile("\\[NPC] Keeper of \\w+: Excellent! You have returned the (?<item>[a-z-A-Z ]+) to its rightful place!");
    private final Pattern resetPattern = Pattern.compile("\\[NPC] Keeper of \\w+: (You found all of the items! Behold\\.\\.\\. the Jade Crystal!)");
    private final Pattern alreadyFoundPattern = Pattern.compile("\\[NPC] Keeper of \\w+: You have already restored this Dwarf's (?<item>[a-z-A-Z ]+)!");
    private final Pattern givePattern = Pattern.compile("\\[NPC] Professor Robot: Thanks for bringing me the (?<part>[a-zA-Z0-9 ]+)! Bring me (\\d+|one) more components? to fix the giant!");
    private final Pattern notFinalPattern = Pattern.compile("\\[NPC] Professor Robot: That's not the final component! Bring me a (?<part>[a-zA-Z0-9 ]+) to gain access to Automaton Prime's storage container!");
    private final Pattern obtainCrystalPattern = Pattern.compile(" +(?<crystal>[a-zA-Z]+) Crystal");
    private final Pattern crystalNotPlacedPattern = Pattern.compile(".*: You haven't placed the (?<crystal>[a-zA-Z]+) Crystal yet!");
    private final Pattern crystalPlacedPattern = Pattern.compile(".*: You have already placed the (?<crystal>[a-zA-Z]+) Crystal!");
    private final Pattern crystalPlacePattern = Pattern.compile("✦ You placed the (?<crystal>[a-zA-Z]+) Crystal!");
    private final Pattern crystalReclaimPattern = Pattern.compile("✦ You reclaimed the (?<crystal>[a-zA-Z]+) Crystal!");

    public CrystalHollowOverlay(Position position, Supplier<List<String>> dummyStrings, Supplier<TextOverlayStyle> styleSupplier) {
        super(position, dummyStrings, styleSupplier);
    }

    @Override
    public void update() {
        overlayStrings = null;
        if (!NotEnoughUpdates.INSTANCE.config.mining.crystalHollowOverlay || SBInfo.getInstance().getLocation() == null ||
                !SBInfo.getInstance().getLocation().equals("crystal_hollows"))
            return;

        NEUConfig.HiddenProfileSpecific hidden = NotEnoughUpdates.INSTANCE.config.getProfileSpecific();
        if (hidden == null) return;
        overlayStrings = new ArrayList<>();
        HashMap<String, Integer> inventoryData = new HashMap<>();
        for (String key : hidden.automatonParts.keySet())
            inventoryData.put(key, 0);
        for (String key : hidden.divanMinesParts.keySet())
            inventoryData.put(key, 0);
        HashMap<String, Integer> storageData = new HashMap<>(inventoryData);
        for (ItemStack item : mc.thePlayer.inventory.mainInventory)
            if (item != null) {
                String name = Utils.cleanColour(item.getDisplayName());
                if (inventoryData.containsKey(name))
                    inventoryData.put(name, inventoryData.get(name) + item.stackSize);
            }
        for (Map.Entry<Integer, Integer> entry : storageManager.storageConfig.displayToStorageIdMap.entrySet()) {
            int storageId = entry.getValue();
            StorageManager.StoragePage page = storageManager.getPage(storageId, false);
            if (page != null && page.rows > 0)
                for (ItemStack item : page.items)
                    if (item != null) {
                        String name = Utils.cleanColour(item.getDisplayName());
                        if (storageData.containsKey(name))
                            storageData.put(name, storageData.get(name) + item.stackSize);
                    }
        }

        for (int i : NotEnoughUpdates.INSTANCE.config.mining.crystalHollowText) {
            switch (i) {
                case 0:
                    if (crystalCheck()) {
                        for (String part : hidden.crystals.keySet()) {
                            switch (hidden.crystals.get(part)) {
                                case 2:
                                    if (!NotEnoughUpdates.INSTANCE.config.mining.crystalHollowHideDone)
                                        overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + part + ": " +
                                                EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPlacedColor] + "Placed");
                                    break;
                                case 1:
                                    overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + part + ": " +
                                            EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowCollectedColor] + "Collected");
                                    break;
                                case 0:
                                    overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + part + ": " +
                                            EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowMissingColor] + "Missing");
                                    break;
                            }
                        }
                    }
                    break;
                case 1:
                    if (crystalCheck()) {
                        int count = getCountCrystal(hidden.crystals);
                        float percent = (float) count / hidden.crystals.size() * 100;
                        overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + "Crystals: " + getColor(percent)
                                + count + "/" + hidden.crystals.size());
                    }
                    break;
                case 2:
                    if (crystalCheck()) {
                        int count = getCountCrystal(hidden.crystals);
                        float percent = (float) count / hidden.crystals.size() * 100;
                        DecimalFormat df = new DecimalFormat("#.#");
                        overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + "Crystals: " + getColor(percent) +
                                df.format(percent) + "%");
                    }
                    break;
                case 3:
                    if (automatonCheck())
                        renderParts(hidden.automatonParts, inventoryData, storageData);
                    break;
                case 4:
                    if (automatonCheck())
                        renderPartsNumbers(hidden.automatonParts, inventoryData, storageData);
                    break;
                case 5:
                    if (automatonCheck())
                        renderCount("Automaton parts", hidden.automatonParts, inventoryData, storageData);
                    break;
                case 6:
                    if (automatonCheck())
                        renderPercent("Automaton parts", hidden.automatonParts, inventoryData, storageData);
                    break;
                case 7:
                    if (divanCheck())
                        renderParts(hidden.divanMinesParts, inventoryData, storageData);
                    break;
                case 8:
                    if (divanCheck())
                        renderPartsNumbers(hidden.divanMinesParts, inventoryData, storageData);
                    break;
                case 9:
                    if (divanCheck())
                        renderCount("Mines of Divan parts", hidden.divanMinesParts, inventoryData, storageData);
                    break;
                case 10:
                    if (divanCheck())
                        renderPercent("Mines of Divan parts", hidden.divanMinesParts, inventoryData, storageData);
                    break;
            }
        }
    }

    private void renderParts(HashMap<String, Boolean> parts, HashMap<String, Integer> inventoryData, HashMap<String, Integer> storageData) {
        for (String part : parts.keySet()) {
            if (parts.get(part) && !NotEnoughUpdates.INSTANCE.config.mining.crystalHollowHideDone)
                overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + part + ": " +
                        EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowDoneColor] + "Done");
            else if (inventoryData.get(part) >= 1)
                overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + part + ": " +
                        EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowInventoryColor] + "In Inventory");
            else if (storageData.get(part) >= 1)
                overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + part + ": " +
                        EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowStorageColor] + "In Storage");
            else
                overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + part + ": " +
                        EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowMissingColor] + "Missing");
        }
    }

    private void renderPartsNumbers(HashMap<String, Boolean> parts, HashMap<String, Integer> inventoryData, HashMap<String, Integer> storageData) {
        for (String part : parts.keySet()) {
            if (parts.get(part))
                overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + part + ": " +
                        EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowDoneColor] + (inventoryData.get(part) + storageData.get(part)));
            else if (inventoryData.get(part) >= 1)
                overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + part + ": " +
                        EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowInventoryColor] + (inventoryData.get(part) + storageData.get(part)));
            else if (storageData.get(part) >= 1)
                overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + part + ": " +
                        EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowStorageColor] + (inventoryData.get(part) + storageData.get(part)));
            else
                overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + part + ": " +
                        EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowMissingColor] + (inventoryData.get(part) + storageData.get(part)));
        }
    }

    private void renderCount(String text, HashMap<String, Boolean> parts, HashMap<String, Integer> inventoryData, HashMap<String, Integer> storageData) {
        int count = getCount(parts, inventoryData, storageData);
        float percent = (float) count / parts.size() * 100;
        overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + text + ": " + getColor(percent) + count
                + "/" + parts.size());
    }

    private void renderPercent(String text, HashMap<String, Boolean> parts, HashMap<String, Integer> inventoryData, HashMap<String, Integer> storageData) {
        int count = getCount(parts, inventoryData, storageData);
        float percent = (float) count / parts.size() * 100;
        DecimalFormat df = new DecimalFormat("#.#");
        overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowPartColor] + text + ": " + getColor(percent) +
                df.format(percent) + "%");
    }

    private EnumChatFormatting getColor(float percent) {
        if (percent >= 66)
            return EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowAllColor];
        else if (percent >= 33)
            return EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowMiddleColor];
        else
            return EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.crystalHollowNoneColor];
    }

    private int getCount(HashMap<String, Boolean> parts, HashMap<String, Integer> inventoryData, HashMap<String, Integer> storageData) {
        int count = 0;
        for (String part : parts.keySet())
            if (parts.get(part) || inventoryData.get(part) > 0 || storageData.get(part) > 0)
                count++;
        return count;
    }

    private int getCountCrystal(HashMap<String, Integer> parts) {
        int count = 0;
        for (String part : parts.keySet())
            if (parts.get(part) > 0)
                count++;
        return count;
    }

    private boolean automatonCheck() {
        return NotEnoughUpdates.INSTANCE.config.mining.crystalHollowAutomatonLocation == 0 ||
                NotEnoughUpdates.INSTANCE.config.mining.crystalHollowAutomatonLocation == 1 &&
                        SBInfo.getInstance().location.equals("Precursor Remnants") ||
                NotEnoughUpdates.INSTANCE.config.mining.crystalHollowAutomatonLocation >= 1 &&
                        SBInfo.getInstance().location.equals("Lost Precursor City");
    }

    private boolean divanCheck() {
        return NotEnoughUpdates.INSTANCE.config.mining.crystalHollowDivanLocation == 0 ||
                NotEnoughUpdates.INSTANCE.config.mining.crystalHollowDivanLocation == 1 &&
                        SBInfo.getInstance().location.equals("Mithril Deposits") ||
                NotEnoughUpdates.INSTANCE.config.mining.crystalHollowDivanLocation >= 1 &&
                        SBInfo.getInstance().location.equals("Mines of Divan");
    }

    private boolean crystalCheck() {
        return NotEnoughUpdates.INSTANCE.config.mining.crystalHollowCrystalLocation == 0 || !divanCheck() && !automatonCheck();
    }

    public void message(String message) {
        NEUConfig.HiddenProfileSpecific hidden = NotEnoughUpdates.INSTANCE.config.getProfileSpecific();
        if (hidden == null) return;

        Matcher crystalNotPlacedMatcher = crystalNotPlacedPattern.matcher(message);
        Matcher crystalPlacedMatcher = crystalPlacedPattern.matcher(message);
        Matcher crystalPlaceMatcher = crystalPlacePattern.matcher(message);
        Matcher crystalReclaimMatcher = crystalReclaimPattern.matcher(message);
        if (message.equals("  You've earned a Crystal Loot Bundle!"))
            hidden.crystals.replaceAll((k, v) -> 0);
        if (crystalNotPlacedMatcher.matches() && hidden.crystals.containsKey(crystalNotPlacedMatcher.group("crystal"))) {
            hidden.crystals.put(crystalNotPlacedMatcher.group("crystal"), 1);
            resetCrystal(hidden, crystalNotPlacedMatcher.group("crystal"));
        } else if (crystalPlacedMatcher.matches() && hidden.crystals.containsKey(crystalPlacedMatcher.group("crystal"))) {
            hidden.crystals.put(crystalPlacedMatcher.group("crystal"), 2);
            resetCrystal(hidden, crystalPlacedMatcher.group("crystal"));
        } else if (crystalPlaceMatcher.matches() && hidden.crystals.containsKey(crystalPlaceMatcher.group("crystal")))
            hidden.crystals.put(crystalPlaceMatcher.group("crystal"), 2);
        else if (crystalReclaimMatcher.matches() && hidden.crystals.containsKey(crystalReclaimMatcher.group("crystal")))
            hidden.crystals.put(crystalReclaimMatcher.group("crystal"), 1);
        else if (message.startsWith("[NPC] Keeper of ")) {
            Matcher foundMatcher = foundPattern.matcher(message);
            Matcher alreadyFoundMatcher = alreadyFoundPattern.matcher(message);
            Matcher notFoundMatcher = notFoundPattern.matcher(message);
            Matcher resetMatcher = resetPattern.matcher(message);
            if (foundMatcher.matches() && hidden.divanMinesParts.containsKey(foundMatcher.group("item")))
                hidden.divanMinesParts.put(foundMatcher.group("item"), true);
            else if (notFoundMatcher.matches() && hidden.divanMinesParts.containsKey(notFoundMatcher.group("item")))
                hidden.divanMinesParts.put(notFoundMatcher.group("item"), false);
            else if (resetMatcher.matches())
                hidden.divanMinesParts.replaceAll((k, v) -> false);
            else if (alreadyFoundMatcher.matches() && hidden.divanMinesParts.containsKey(alreadyFoundMatcher.group("item")))
                hidden.divanMinesParts.put(alreadyFoundMatcher.group("item"), true);
        } else if (message.startsWith("  ")) {
            Matcher crystalMatcher = obtainCrystalPattern.matcher(message);
            if (crystalMatcher.matches() && hidden.crystals.containsKey(crystalMatcher.group("crystal")))
                hidden.crystals.put(crystalMatcher.group("crystal"), 1);
            else {
                String item = message.replace("  ", "");
                if (hidden.automatonParts.containsKey(item))
                    hidden.automatonParts.put(item, false);
            }
        } else if (message.startsWith("[NPC] Professor Robot: ")) {
            switch (message) {
                case "[NPC] Professor Robot: That's not one of the components I need! Bring me one of the missing components:":
                    hidden.automatonParts.replaceAll((k, v) -> true);
                    break;
                case "[NPC] Professor Robot: You've brought me all of the components!":
                    hidden.automatonParts.replaceAll((k, v) -> false);
                    break;
                default:
                    Matcher giveMatcher = givePattern.matcher(message);
                    Matcher notFinalMatcher = notFinalPattern.matcher(message);
                    if (giveMatcher.matches()) {
                        String item = giveMatcher.group("part");
                        if (hidden.automatonParts.containsKey(item)) {
                            hidden.automatonParts.put(item, true);
                        }
                    } else if (notFinalMatcher.matches()) {
                        String item = notFinalMatcher.group("part");
                        if (hidden.automatonParts.containsKey(item)) {
                            hidden.automatonParts.replaceAll((k, v) -> true);
                            hidden.automatonParts.put(item, false);
                        }
                    }
                    break;
            }
        }
    }

    private void resetCrystal(NEUConfig.HiddenProfileSpecific hidden, String crystal) {
        switch (crystal) {
            case "Sapphire":
                hidden.automatonParts.replaceAll((k, v) -> false);
                break;
            case "Jade":
                hidden.divanMinesParts.replaceAll((k, v) -> false);
                break;
        }
    }

    @Override
    protected void renderLine(String line, Vector2f position, boolean dummy) {
        if (!NotEnoughUpdates.INSTANCE.config.mining.crystalHollowIcons) return;
        GlStateManager.enableDepth();

        ItemStack icon = null;
        String cleaned = Utils.cleanColour(line);
        String beforeColon = cleaned.split(":")[0];
        switch (beforeColon) {
            case "Scavenged Lapis Sword":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("DWARVEN_LAPIS_SWORD"));
                break;
            case "Scavenged Golden Hammer":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("DWARVEN_GOLD_HAMMER"));
                break;
            case "Scavenged Diamond Axe":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("DWARVEN_DIAMOND_AXE"));
                break;
            case "Scavenged Emerald Hammer":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("DWARVEN_EMERALD_HAMMER"));
                break;
            case "Electron Transmitter":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("ELECTRON_TRANSMITTER"));
                break;
            case "FTX 3070":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("FTX_3070"));
                break;
            case "Robotron Reflector":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("ROBOTRON_REFLECTOR"));
                break;
            case "Superlite Motor":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("SUPERLITE_MOTOR"));
                break;
            case "Control Switch":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("CONTROL_SWITCH"));
                break;
            case "Synthetic Heart":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("SYNTHETIC_HEART"));
                break;
            case "Amber":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("PERFECT_AMBER_GEM"));
                break;
            case "Sapphire":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("PERFECT_SAPPHIRE_GEM"));
                break;
            case "Jade":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("PERFECT_JADE_GEM"));
                break;
            case "Amethyst":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("PERFECT_AMETHYST_GEM"));
                break;
            case "Topaz":
                icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("PERFECT_TOPAZ_GEM"));
                break;
        }

        if (icon != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(position.x, position.y, 0);
            GlStateManager.scale(0.5f, 0.5f, 1f);
            Utils.drawItemStack(icon, 0, 0);
            GlStateManager.popMatrix();

            position.x += 12;
        }

        super.renderLine(line, position, dummy);
    }

    @Override
    protected Vector2f getSize(List<String> strings) {
        if (NotEnoughUpdates.INSTANCE.config.mining.crystalHollowIcons)
            return super.getSize(strings).translate(12, 0);
        return super.getSize(strings);
    }
}
