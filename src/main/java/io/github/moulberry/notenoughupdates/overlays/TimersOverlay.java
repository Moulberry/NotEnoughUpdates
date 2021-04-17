package io.github.moulberry.notenoughupdates.overlays;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.options.NEUConfig;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraft.util.EnumChatFormatting.*;

public class TimersOverlay extends TextOverlay {

    private static final Pattern PATTERN_ACTIVE_EFFECTS = Pattern.compile("\u00a77You have \u00a7r\u00a7e(\\d+) \u00a7r\u00a77active effects.");

    public TimersOverlay(Position position, Supplier<List<String>> dummyStrings, Supplier<TextOverlayStyle> styleSupplier) {
        super(position, dummyStrings, styleSupplier);
    }

    private static final Pattern CAKE_PATTERN = Pattern.compile("\u00a7r\u00a7d\u00a7lYum! \u00a7r\u00a7eYou gain .+ \u00a7r\u00a7efor \u00a7r\u00a7a48 \u00a7r\u00a7ehours!\u00a7r");
    private static final Pattern PUZZLER_PATTERN = Pattern.compile("\u00a7r\u00a7dPuzzler\u00a7r\u00a76 gave you .+ \u00a7r\u00a76for solving the puzzle!\u00a7r");
    private static final Pattern FETCHUR_PATTERN = Pattern.compile("\u00a7e\\[NPC] Fetchur\u00a7f: \u00a7rthanks thats probably what i needed\u00a7r");
    private static final Pattern FETCHUR2_PATTERN = Pattern.compile("\u00a7e\\[NPC] Fetchur\u00a7f: \u00a7rcome back another time, maybe tmrw\u00a7r");
    private static final Pattern GODPOT_PATTERN = Pattern.compile("\u00a7r\u00a7a\u00a7lGULP! \u00a7r\u00a7eThe \u00a7r\u00a7cGod Potion \u00a7r\u00a7egrants you" +
            " powers for \u00a7r\u00a7924 hours\u00a7r\u00a7e!\u00a7r");

    private boolean hideGodpot = false;

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChatMessageReceived(ClientChatReceivedEvent event) {
        NEUConfig.HiddenProfileSpecific hidden = NotEnoughUpdates.INSTANCE.config.getProfileSpecific();
        if(hidden == null) return;

        if(event.type == 0) {
            long currentTime = System.currentTimeMillis();

            Matcher cakeMatcher = CAKE_PATTERN.matcher(event.message.getFormattedText());
            if(cakeMatcher.matches()) {
                hidden.firstCakeAte = currentTime;
                return;
            }

            Matcher puzzlerMatcher = PUZZLER_PATTERN.matcher(event.message.getFormattedText());
            if(puzzlerMatcher.matches()) {
                hidden.puzzlerCompleted = currentTime;
                return;
            }

            Matcher fetchurMatcher = FETCHUR_PATTERN.matcher(event.message.getFormattedText());
            if(fetchurMatcher.matches()) {
                hidden.fetchurCompleted = currentTime;
                return;
            }

            Matcher fetchur2Matcher = FETCHUR2_PATTERN.matcher(event.message.getFormattedText());
            if(fetchur2Matcher.matches()) {
                hidden.fetchurCompleted = currentTime;
                return;
            }

            Matcher godpotMatcher = GODPOT_PATTERN.matcher(event.message.getFormattedText());
            if(godpotMatcher.matches()) {
                hidden.godPotionDrunk = currentTime;
                return;
            }
        }
    }

    @Override
    protected Vector2f getSize(List<String> strings) {
        return super.getSize(strings).translate(12, 0);
    }

    private static final ItemStack CAKES_ICON = new ItemStack(Items.cake);
    private static final ItemStack PUZZLER_ICON = new ItemStack(Items.book);
    private static ItemStack[] FETCHUR_ICONS = null;
    private static final ItemStack COMMISSIONS_ICON = new ItemStack(Items.iron_pickaxe);
    private static final ItemStack EXPERIMENTS_ICON = new ItemStack(Items.enchanted_book);
    private static final ItemStack COOKIE_ICON = new ItemStack(Items.cookie);

    @Override
    protected void renderLine(String line, Vector2f position, boolean dummy) {
        if(!NotEnoughUpdates.INSTANCE.config.miscOverlays.todoIcons) {
            return;
        }
        GlStateManager.enableDepth();

        ItemStack icon = null;

        String clean = Utils.cleanColour(line);
        String beforeColon = clean.split(":")[0];
        switch(beforeColon) {
            case "Cakes": icon = CAKES_ICON; break;
            case "Puzzler": icon = PUZZLER_ICON; break;
            case "Godpot": icon = NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("GOD_POTION")); break;
            case "Fetchur": {
                if(FETCHUR_ICONS == null) {
                    FETCHUR_ICONS = new ItemStack[] {
                            new ItemStack(Blocks.wool, 50, 14),
                            new ItemStack(Blocks.stained_glass, 20, 4),
                            new ItemStack(Items.compass, 1, 0),
                            new ItemStack(Items.prismarine_crystals, 20, 0),
                            new ItemStack(Items.fireworks, 1, 0),
                            NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("CHEAP_COFFEE")),
                            new ItemStack(Items.oak_door, 1, 0),
                            new ItemStack(Items.rabbit_foot, 3, 0),
                            NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("SUPERBOOM_TNT")),
                            new ItemStack(Blocks.pumpkin, 1, 0),
                            new ItemStack(Items.flint_and_steel, 1, 0),
                            new ItemStack(Blocks.quartz_ore, 50, 0),
                            new ItemStack(Items.ender_pearl, 16, 0)
                    };
                }
                long currentTime = System.currentTimeMillis();

                long fetchurIndex = (currentTime-18000000)/86400000 % 13 - 4;
                if(fetchurIndex < 0) fetchurIndex += 13;

                icon = FETCHUR_ICONS[(int)fetchurIndex];
                break;
            }
            case "Commissions": icon = COMMISSIONS_ICON; break;
            case "Experiments": icon = EXPERIMENTS_ICON; break;
            case "Cookie Buff": icon = COOKIE_ICON; break;
        }

        if(icon != null) {
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
    public void update() {
        long currentTime = System.currentTimeMillis();

        NEUConfig.HiddenProfileSpecific hidden = NotEnoughUpdates.INSTANCE.config.getProfileSpecific();
        if(hidden == null) return;

        if(Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
            GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            IInventory lower = container.getLowerChestInventory();
            String containerName = lower.getDisplayName().getUnformattedText();

            if(containerName.equals("Commissions") && lower.getSizeInventory() >= 18) {
                if(hidden.commissionsCompleted == 0) {
                    hidden.commissionsCompleted = currentTime;
                }
                for(int i=9; i<18; i++) {
                    ItemStack stack = lower.getStackInSlot(i);
                    if(stack != null && stack.hasTagCompound()) {
                        String[] lore = NotEnoughUpdates.INSTANCE.manager.getLoreFromNBT(stack.getTagCompound());
                        for(String line : lore) {
                            if(line.contains("(Daily")) {
                                hidden.commissionsCompleted = 0;
                                break;
                            }
                        }
                    }
                }
            } else if(containerName.equals("Experimentation Table") && lower.getSizeInventory() >= 36) {
                ItemStack stack = lower.getStackInSlot(31);
                if(stack != null) {
                    if(stack.getItem() == Items.blaze_powder) {
                        if(hidden.experimentsCompleted == 0) {
                            hidden.experimentsCompleted = currentTime;
                        }
                    } else {
                        hidden.experimentsCompleted = 0;
                    }
                }
            }
        }

        boolean foundCookieBuffText = false;
        if(SBInfo.getInstance().getLocation() != null && !SBInfo.getInstance().getLocation().equals("dungeon") && SBInfo.getInstance().footer != null) {
            String formatted = SBInfo.getInstance().footer.getFormattedText();
            for(String line : formatted.split("\n")) {
                Matcher activeEffectsMatcher = PATTERN_ACTIVE_EFFECTS.matcher(line);
                if(activeEffectsMatcher.find()) {
                    String numEffectsS = activeEffectsMatcher.group(1);
                    try {
                        int numEffects = Integer.parseInt(numEffectsS);
                        hideGodpot = numEffects > 25;
                    } catch(NumberFormatException ignored) {}
                } else if(line.contains("\u00a7d\u00a7lCookie Buff")) {
                    foundCookieBuffText = true;
                } else if(foundCookieBuffText) {
                    String cleanNoSpace = line.replaceAll("(\u00a7.| )", "");

                    hidden.cookieBuffRemaining = 0;
                    StringBuilder number = new StringBuilder();
                    for(int i=0; i<cleanNoSpace.length(); i++) {
                        char c = cleanNoSpace.charAt(i);

                        if(c >= '0' && c <= '9') {
                            number.append(c);
                        } else {
                            if(number.length() == 0) {
                                hidden.cookieBuffRemaining = 0;
                                break;
                            }
                            if("ydhms".contains(""+c)) {
                                try {
                                    long val = Integer.parseInt(number.toString());
                                    switch(c) {
                                        case 'y': hidden.cookieBuffRemaining += val*365*24*60*60*1000; break;
                                        case 'd': hidden.cookieBuffRemaining += val*24*60*60*1000; break;
                                        case 'h': hidden.cookieBuffRemaining += val*60*60*1000; break;
                                        case 'm': hidden.cookieBuffRemaining += val*60*1000; break;
                                        case 's': hidden.cookieBuffRemaining += val*1000; break;
                                    }
                                } catch(NumberFormatException e) {
                                    hidden.cookieBuffRemaining = 0;
                                    break;
                                }

                                number = new StringBuilder();
                            } else {
                                hidden.cookieBuffRemaining = 0;
                                break;
                            }
                        }
                    }

                    break;
                }
            }
        }

        if(!NotEnoughUpdates.INSTANCE.config.miscOverlays.todoOverlay) {
            overlayStrings = null;
            return;
        }

        HashMap<Integer, String> map = new HashMap<>();

        long cakeEnd = hidden.firstCakeAte + 1000*60*60*48 - currentTime;
        if(cakeEnd < 0) {
            map.put(0, DARK_AQUA+"Cakes: "+YELLOW+"Inactive!");
            map.put(0+7, DARK_AQUA+"Cakes: "+YELLOW+"Inactive!");
        } else {
            map.put(0+7, DARK_AQUA+"Cakes: "+YELLOW+Utils.prettyTime(cakeEnd));
        }

        if(hidden.cookieBuffRemaining <= 0) {
            map.put(1, DARK_AQUA+"Cookie Buff: "+YELLOW+"Inactive!");
            map.put(1+7, DARK_AQUA+"Cookie Buff: "+YELLOW+"Inactive!");
        } else {
            map.put(1+7, DARK_AQUA+"Cookie Buff: "+YELLOW+Utils.prettyTime(hidden.cookieBuffRemaining));
        }

        long godpotEnd = hidden.godPotionDrunk + 1000*60*60*24 - currentTime;
        if(godpotEnd < 0) {
            if(!hideGodpot) {
                map.put(2, DARK_AQUA+"Godpot: "+YELLOW+"Inactive!");
                map.put(2+7, DARK_AQUA+"Godpot: "+YELLOW+"Inactive!");
            }
        } else {
            map.put(2+7, DARK_AQUA+"Godpot: "+YELLOW+Utils.prettyTime(godpotEnd));
        }

        long puzzlerEnd = hidden.puzzlerCompleted + 1000*60*60*24 - currentTime;
        if(puzzlerEnd < 0) {
            map.put(3, DARK_AQUA+"Puzzler: "+YELLOW+"Ready!");
            map.put(3+7, DARK_AQUA+"Puzzler: "+YELLOW+"Ready!");
        } else {
            map.put(3+7, DARK_AQUA+"Puzzler: "+YELLOW+Utils.prettyTime(puzzlerEnd));
        }

        long midnightReset = (currentTime-18000000)/86400000*86400000+18000000;
        long fetchurComplete = hidden.fetchurCompleted;
        if(fetchurComplete < midnightReset) {
            map.put(4, DARK_AQUA+"Fetchur: "+YELLOW+"Ready!");
            map.put(4+7, DARK_AQUA+"Fetchur: "+YELLOW+"Ready!");
        } else {
            map.put(4+7, DARK_AQUA+"Fetchur: "+YELLOW+Utils.prettyTime(midnightReset + 86400000 - currentTime));
        }

        if(hidden.commissionsCompleted < midnightReset) {
            map.put(5, DARK_AQUA+"Commissions: "+YELLOW+"Ready!");
            map.put(5+7, DARK_AQUA+"Commissions: "+YELLOW+"Ready!");
        } else {
            map.put(5+7, DARK_AQUA+"Commissions: "+YELLOW+Utils.prettyTime(midnightReset + 86400000 - currentTime));
        }

        if(hidden.experimentsCompleted < midnightReset) {
            map.put(6, DARK_AQUA+"Experiments: "+YELLOW+"Ready!");
            map.put(6+7, DARK_AQUA+"Experiments: "+YELLOW+"Ready!");
        } else {
            map.put(6+7, DARK_AQUA+"Experiments: "+YELLOW+Utils.prettyTime(midnightReset + 86400000 - currentTime));
        }

        overlayStrings = new ArrayList<>();
        for(int index : NotEnoughUpdates.INSTANCE.config.miscOverlays.todoText) {
            if(map.containsKey(index)) {
                overlayStrings.add(map.get(index));
            }
        }
        if(overlayStrings.isEmpty()) overlayStrings = null;
    }

}
