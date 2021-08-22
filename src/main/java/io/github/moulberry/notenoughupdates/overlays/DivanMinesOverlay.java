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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DivanMinesOverlay extends TextOverlay {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final StorageManager storageManager = StorageManager.getInstance();
    private final Pattern notFoundPattern = Pattern.compile("\\[NPC] Keeper of \\w+: Talk to me when you have found a (?<item>[a-z-A-Z ]+)!");
    private final Pattern foundPattern = Pattern.compile("\\[NPC] Keeper of \\w+: Excellent! You have returned the (?<item>[a-z-A-Z ]+) to its rightful place!");
    private final Pattern resetPattern = Pattern.compile("\\[NPC] Keeper of \\w+: (You haven't placed the Jade Crystal yet!|You found all of the items! Behold\\.\\.\\. the Jade Crystal!|You have already placed the Jade Crystal!)");
    private final Pattern alreadyFoundPattern = Pattern.compile("\\[NPC] Keeper of \\w+: You have already restored this Dwarf's (?<item>[a-z-A-Z ]+)!");

    public DivanMinesOverlay(Position position, Supplier<List<String>> dummyStrings, Supplier<TextOverlayStyle> styleSupplier) {
        super(position, dummyStrings, styleSupplier);
    }

    @Override
    public void update() {
        overlayStrings = null;
        if (!NotEnoughUpdates.INSTANCE.config.mining.divanMinesOverlay || SBInfo.getInstance().getLocation() == null ||
                !SBInfo.getInstance().getLocation().equals("crystal_hollows") || !SBInfo.getInstance().location.equals("Mines of Divan"))
            return;

        NEUConfig.HiddenProfileSpecific hidden = NotEnoughUpdates.INSTANCE.config.getProfileSpecific();
        if (hidden == null) return;
        overlayStrings = new ArrayList<>();
        HashMap<String, String> states = new HashMap<>();
        for (String key : hidden.divanMinesParts.keySet()) {
            Boolean has = hidden.divanMinesParts.get(key);
            if (has)
                states.put(key, EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.divanMinesDoneColor] + "Done");
        }
        for (ItemStack item : mc.thePlayer.inventory.mainInventory) {
            if (item != null) {
                String name = Utils.cleanColour(item.getDisplayName());
                if (!states.containsKey(name)) {
                    states.put(name, EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.divanMinesInventoryColor] + "In Inventory");
                }
            }
        }
        for (Map.Entry<Integer, Integer> entry : storageManager.storageConfig.displayToStorageIdMap.entrySet()) {
            int storageId = entry.getValue();
            StorageManager.StoragePage page = storageManager.getPage(storageId, false);
            if (page != null && page.rows > 0) {
                for (ItemStack item : page.items) {
                    if (item != null) {
                        String name = Utils.cleanColour(item.getDisplayName());
                        if (!states.containsKey(name)) {
                            states.put(name, EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.divanMinesStorageColor] + "In Storage");
                        }
                    }
                }
            }
        }
        for (String key : hidden.divanMinesParts.keySet()) {
            if (!NotEnoughUpdates.INSTANCE.config.mining.divanMinesHideDone || !hidden.divanMinesParts.get(key)) {
                if (!states.containsKey(key))
                    states.put(key, EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.divanMinesMissingColor] + "Missing");
                overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.divanMinesPartColor] + key + ": " + states.get(key));
            }
        }
    }

    public void message(String message) {
        NEUConfig.HiddenProfileSpecific hidden = NotEnoughUpdates.INSTANCE.config.getProfileSpecific();
        if (hidden == null) return;
        Matcher foundMatcher = foundPattern.matcher(message);
        Matcher alreadyFoundMatcher = alreadyFoundPattern.matcher(message);
        Matcher notFoundMatcher = notFoundPattern.matcher(message);
        Matcher resetMatcher = resetPattern.matcher(message);
        System.out.println(message);
        if (foundMatcher.matches() && hidden.divanMinesParts.containsKey(foundMatcher.group("item")))
            hidden.divanMinesParts.put(foundMatcher.group("item"), true);
        else if (notFoundMatcher.matches() && hidden.divanMinesParts.containsKey(notFoundMatcher.group("item")))
            hidden.divanMinesParts.put(notFoundMatcher.group("item"), false);
        else if (resetMatcher.matches())
            hidden.divanMinesParts.replaceAll((k, v) -> false);
        else if (alreadyFoundMatcher.matches() && hidden.divanMinesParts.containsKey(alreadyFoundMatcher.group("item")))
            hidden.divanMinesParts.put(alreadyFoundMatcher.group("item"), true);
    }

    @Override
    protected void renderLine(String line, Vector2f position, boolean dummy) {
        if (!NotEnoughUpdates.INSTANCE.config.mining.divanMinesIcons) return;
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
        if (NotEnoughUpdates.INSTANCE.config.mining.divanMinesIcons)
            return super.getSize(strings).translate(12, 0);
        return super.getSize(strings);
    }
}
