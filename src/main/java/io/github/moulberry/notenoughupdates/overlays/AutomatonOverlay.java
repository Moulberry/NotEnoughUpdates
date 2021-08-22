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

public class AutomatonOverlay extends TextOverlay {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final StorageManager storageManager = StorageManager.getInstance();
    private final Pattern givePattern = Pattern.compile("\\[NPC] Professor Robot: Thanks for bringing me the (?<part>[a-zA-Z0-9 ]+)! Bring me (\\d+|one) more components? to fix the giant!");
    private final Pattern notFinalPattern = Pattern.compile("\\[NPC] Professor Robot: That's not the final component! Bring me a (?<part>[a-zA-Z0-9 ]+) to gain access to Automaton Prime's storage container!");

    public AutomatonOverlay(Position position, Supplier<List<String>> dummyStrings, Supplier<TextOverlayStyle> styleSupplier) {
        super(position, dummyStrings, styleSupplier);
    }

    @Override
    public void update() {
        NEUConfig.HiddenProfileSpecific hidden = NotEnoughUpdates.INSTANCE.config.getProfileSpecific();
        overlayStrings = null;
        if (!NotEnoughUpdates.INSTANCE.config.mining.automatonOverlay || SBInfo.getInstance().getLocation() == null ||
                !SBInfo.getInstance().getLocation().equals("crystal_hollows") || !SBInfo.getInstance().location.equals("Lost Precursor City"))
            return;

        overlayStrings = new ArrayList<>();
        HashMap<String, String> states = new HashMap<>();
        for (String key : hidden.automatonParts.keySet()) {
            Boolean has = hidden.automatonParts.get(key);
            if (has)
                states.put(key, EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.automatonDoneColor] + "Done");
        }
        for (ItemStack item : mc.thePlayer.inventory.mainInventory) {
            if (item != null) {
                String name = Utils.cleanColour(item.getDisplayName());
                if (!states.containsKey(name)) {
                    states.put(name, EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.automatonInventoryColor] + "In Inventory");
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
                            states.put(name, EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.automatonStorageColor] + "In Storage");
                        }
                    }
                }
            }
        }
        for (String key : hidden.automatonParts.keySet()) {
            if (!NotEnoughUpdates.INSTANCE.config.mining.automatonHideDone || !hidden.automatonParts.get(key)) {
                if (!states.containsKey(key))
                    states.put(key, EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.automatonMissingColor] + "Missing");
                overlayStrings.add(EnumChatFormatting.values()[NotEnoughUpdates.INSTANCE.config.mining.automatonPartColor] + key + ": " + states.get(key));
            }
        }
    }

    public void message(String message) {
        NEUConfig.HiddenProfileSpecific hidden = NotEnoughUpdates.INSTANCE.config.getProfileSpecific();
        if (message.startsWith("  ")) {
            String item = message.replace("  ", "");
            if (hidden.automatonParts.containsKey(item)) {
                hidden.automatonParts.put(item, false);
            }
        } else if (message.startsWith("[NPC] Professor Robot: ")) {
            switch (message) {
                case "[NPC] Professor Robot: That's not one of the components I need! Bring me one of the missing components:":
                    hidden.automatonParts.replaceAll((k, v) -> true);
                    break;
                case "[NPC] Professor Robot: You've brought me all of the components!":
                case "[NPC] Professor Robot: You haven't placed the Sapphire Crystal yet!":
                case "[NPC] Professor Robot: You have already placed the Sapphire Crystal!":
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

    @Override
    protected void renderLine(String line, Vector2f position, boolean dummy) {
        if (!NotEnoughUpdates.INSTANCE.config.mining.automatonIcons) return;
        GlStateManager.enableDepth();

        ItemStack icon = null;
        String cleaned = Utils.cleanColour(line);
        String beforeColon = cleaned.split(":")[0];
        switch (beforeColon) {
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
        }

        if (icon != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(position.x - 2, position.y - 2, 0);
            GlStateManager.scale(0.7f, 0.7f, 1f);
            Utils.drawItemStack(icon, 0, 0);
            GlStateManager.popMatrix();

            position.x += 12;
        }

        super.renderLine(line, position, dummy);
    }

    @Override
    protected Vector2f getSize(List<String> strings) {
        if (NotEnoughUpdates.INSTANCE.config.mining.automatonIcons)
            return super.getSize(strings).translate(12, 0);
        return super.getSize(strings);
    }
}
