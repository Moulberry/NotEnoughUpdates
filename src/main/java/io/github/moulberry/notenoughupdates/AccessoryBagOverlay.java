package io.github.moulberry.notenoughupdates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.profileviewer.PlayerStats;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.moulberry.notenoughupdates.GuiTextures.*;

public class AccessoryBagOverlay {

    private static final int TAB_BASIC = 0;
    private static final int TAB_TOTAL = 1;
    private static final int TAB_BONUS = 2;
    private static final int TAB_DUP = 3;
    private static final int TAB_MISSING = 4;
    private static final int TAB_OPTIMIZER = 5;

    private static final ItemStack[] TAB_STACKS = new ItemStack[] {
            Utils.createItemStack(Items.dye, EnumChatFormatting.DARK_AQUA+"Basic Information",
                    10, EnumChatFormatting.GREEN+"- Talis count by rarity"),
            Utils.createItemStack(Items.diamond_sword, EnumChatFormatting.DARK_AQUA+"Total Stat Bonuses",
                    0),
            Utils.createItemStack(Item.getItemFromBlock(Blocks.anvil), EnumChatFormatting.DARK_AQUA+"Total Stat Bonuses (from reforges)",
                    0),
            Utils.createItemStack(Items.dye, EnumChatFormatting.DARK_AQUA+"Duplicates",
                    8),
            Utils.createItemStack(Item.getItemFromBlock(Blocks.barrier), EnumChatFormatting.DARK_AQUA+"Missing",
                    0),
            Utils.createItemStack(Item.getItemFromBlock(Blocks.redstone_block), EnumChatFormatting.DARK_AQUA+"Optimizer",
                    0),
    };

    private static int currentTab = TAB_BASIC;

    public static boolean mouseClick() {
        if(!Mouse.getEventButtonState()) return false;
        try {
            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());

            int width = scaledResolution.getScaledWidth();
            int height = scaledResolution.getScaledHeight();

            int mouseX = Mouse.getX() / scaledResolution.getScaleFactor();
            int mouseY = height - Mouse.getY() / scaledResolution.getScaleFactor();

            int xSize = (int) Utils.getField(GuiContainer.class, Minecraft.getMinecraft().currentScreen, "xSize", "field_146999_f");
            int ySize = (int) Utils.getField(GuiContainer.class, Minecraft.getMinecraft().currentScreen, "ySize", "field_147000_g");
            int guiLeft = (int) Utils.getField(GuiContainer.class, Minecraft.getMinecraft().currentScreen, "guiLeft", "field_147003_i");
            int guiTop = (int) Utils.getField(GuiContainer.class, Minecraft.getMinecraft().currentScreen, "guiTop", "field_147009_r");

            if(mouseX < guiLeft+xSize+3 || mouseX > guiLeft+xSize+80+28) return false;
            if(mouseY < guiTop || mouseY > guiTop+166) return false;

            if(mouseX > guiLeft+xSize+83 && mouseY < guiTop+20*TAB_MISSING+22) {
                currentTab = (mouseY - guiTop)/20;
                if(currentTab < 0) currentTab = 0;
                if(currentTab > TAB_MISSING) currentTab = TAB_MISSING;
            }

            if(currentTab == TAB_OPTIMIZER) {
                int x = guiLeft+xSize+3;
                int y = guiTop;

                if(mouseY > y+92 && mouseY < y+103) {
                    if(mouseX > x+5 && mouseX < x+75) {
                        mainWeapon = (int)Math.floor((mouseX-x-5)/70f*9);
                        if(mainWeapon < 1) {
                            mainWeapon = 1;
                        } else if(mainWeapon > 9) {
                            mainWeapon = 9;
                        }
                    }
                }

                if(mouseX > x+5 && mouseX < x+35 || mouseX > x+45 && mouseX < x+75) {
                    boolean set = mouseX > x+5 && mouseX < x+35;

                    if(mouseY > y+32 && mouseY < y+43) {
                        forceCC = set;
                    } else if(mouseY > y+52 && mouseY < y+63) {
                        forceAS = set;
                    } else if(mouseY > y+72 && mouseY < y+83) {
                        useGodPot = set;
                    } else if(mouseY > y+92 && mouseY < y+103) {
                        allowShaded = set;
                    }
                }
            }

            return true;
        } catch(Exception e) {
            return false;
        }
    }

    public static void resetCache() {
        accessoryStacks = new HashSet<>();
        pagesVisited = new HashSet<>();
        talismanCountRarity = null;
        totalStats = null;
        reforgeStats = null;
        duplicates = null;
        missing = null;
    }

    private static Set<ItemStack> accessoryStacks = new HashSet<>();
    private static Set<Integer> pagesVisited = new HashSet<>();

    public static void renderVisitOverlay(int x, int y) {
        Utils.drawStringCenteredScaledMaxWidth("Please visit all", Minecraft.getMinecraft().fontRendererObj, x+40, y+78, true, 70, -1);
        Utils.drawStringCenteredScaledMaxWidth("pages of the bag", Minecraft.getMinecraft().fontRendererObj, x+40, y+86, true, 70, -1);
    }

    private static TreeMap<Integer, Integer> talismanCountRarity = null;
    public static void renderBasicOverlay(int x, int y) {
        if(talismanCountRarity == null) {
            talismanCountRarity = new TreeMap<>();
            for(ItemStack stack : accessoryStacks) {
                int rarity = getRarity(stack);
                if(rarity >= 0) {
                    talismanCountRarity.put(rarity, talismanCountRarity.getOrDefault(rarity, 0)+1);
                }
            }
        }

        Utils.drawStringCenteredScaledMaxWidth("# By Rarity", Minecraft.getMinecraft().fontRendererObj, x+40, y+12, false, 70,
                new Color(80, 80, 80).getRGB());

        int yIndex = 0;
        for(Map.Entry<Integer, Integer> entry : talismanCountRarity.descendingMap().entrySet()) {
            String rarityName = rarityArrC[entry.getKey()];
            renderAlignedString(rarityName, EnumChatFormatting.WHITE.toString()+entry.getValue(), x+5, y+20+11*yIndex, 70);
            yIndex++;
        }
    }


    private static PlayerStats.Stats totalStats = null;
    public static void renderTotalStatsOverlay(int x, int y) {
        if(totalStats == null) {
            totalStats = new PlayerStats.Stats();
            for(ItemStack stack : accessoryStacks) {
                if(stack != null) totalStats.add(getStatForItem(stack, STAT_PATTERN_MAP, true));
            }
        }

        Utils.drawStringCenteredScaledMaxWidth("Total Stats", Minecraft.getMinecraft().fontRendererObj, x+40, y+12, false, 70,
                new Color(80, 80, 80).getRGB());
        int yIndex = 0;
        for(int i=0; i<PlayerStats.defaultStatNames.length; i++) {
            String statName = PlayerStats.defaultStatNames[i];
            String statNamePretty = PlayerStats.defaultStatNamesPretty[i];

            int val = Math.round(totalStats.get(statName));

            if(Math.abs(val) < 1E-5) continue;

            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableBlend();
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            renderAlignedString(statNamePretty, EnumChatFormatting.WHITE.toString()+val, x+5, y+20+11*yIndex, 70);

            yIndex++;
        }
    }

    private static PlayerStats.Stats reforgeStats = null;
    public static void renderReforgeStatsOverlay(int x, int y) {
        if(reforgeStats == null) {
            reforgeStats = new PlayerStats.Stats();
            for(ItemStack stack : accessoryStacks) {
                if(stack != null) reforgeStats.add(getStatForItem(stack, STAT_PATTERN_MAP_BONUS, false));
            }
        }

        Utils.drawStringCenteredScaledMaxWidth("Reforge Stats", Minecraft.getMinecraft().fontRendererObj, x+40, y+12, false, 70,
                new Color(80, 80, 80).getRGB());
        int yIndex = 0;
        for(int i=0; i<PlayerStats.defaultStatNames.length; i++) {
            String statName = PlayerStats.defaultStatNames[i];
            String statNamePretty = PlayerStats.defaultStatNamesPretty[i];

            int val = Math.round(reforgeStats.get(statName));

            if(Math.abs(val) < 1E-5) continue;

            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableBlend();
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            renderAlignedString(statNamePretty, EnumChatFormatting.WHITE.toString()+val, x+5, y+20+11*yIndex, 70);

            yIndex++;
        }
    }

    private static Set<ItemStack> duplicates = null;
    public static void renderDuplicatesOverlay(int x, int y) {
        if(duplicates == null) {
            JsonObject misc = Utils.getConstant("misc");
            if(misc == null) {
                Utils.drawStringCenteredScaledMaxWidth("Duplicates: ERROR", Minecraft.getMinecraft().fontRendererObj, x+40, y+12, false, 70,
                        new Color(80, 80, 80).getRGB());
                return;
            }
            JsonElement talisman_upgrades_element = misc.get("talisman_upgrades");
            if(talisman_upgrades_element == null) {
                Utils.drawStringCenteredScaledMaxWidth("Duplicates: ERROR", Minecraft.getMinecraft().fontRendererObj, x+40, y+12, false, 70,
                        new Color(80, 80, 80).getRGB());
                return;
            }
            JsonObject talisman_upgrades = talisman_upgrades_element.getAsJsonObject();

            duplicates = new HashSet<>();

            Set<String> prevInternalnames = new HashSet<>();
            for(ItemStack stack : accessoryStacks) {
                String internalname = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(stack);

                if(prevInternalnames.contains(internalname)) {
                    duplicates.add(stack);
                    continue;
                }
                prevInternalnames.add(internalname);

                if(talisman_upgrades.has(internalname)) {
                    JsonArray upgrades = talisman_upgrades.get(internalname).getAsJsonArray();
                    for(ItemStack stack2 : accessoryStacks) {
                        if(stack != stack2) {
                            String internalname2 = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(stack2);
                            for(int j=0; j<upgrades.size(); j++) {
                                String upgrade = upgrades.get(j).getAsString();
                                if(internalname2.equals(upgrade)) {
                                    duplicates.add(stack);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        if(duplicates.isEmpty()) {
            Utils.drawStringCenteredScaledMaxWidth("No Duplicates", Minecraft.getMinecraft().fontRendererObj, x+40, y+12, false, 70,
                    new Color(80, 80, 80).getRGB());
        } else {
            Utils.drawStringCenteredScaledMaxWidth("Duplicates: " + duplicates.size(), Minecraft.getMinecraft().fontRendererObj, x+40, y+12, false, 70,
                    new Color(80, 80, 80).getRGB());

            int yIndex = 0;
            for(ItemStack duplicate : duplicates) {
                renderAlignedString(duplicate.getDisplayName(), "", x+5, y+20+11*yIndex, 70);
                if(duplicates.size() > 11) {
                    if(++yIndex >= 10) break;
                } else {
                    if(++yIndex >= 11) break;
                }
            }
            
            if(duplicates.size() > 11) {
                Utils.drawStringCenteredScaledMaxWidth("+" + (duplicates.size()-10) + " More",
                        Minecraft.getMinecraft().fontRendererObj, x+40, y+16+121, false, 70,
                        new Color(80, 80, 80).getRGB());
            }
        }
    }

    private static List<ItemStack> missing = null;
    public static void renderMissingOverlay(int x, int y) {
        if(missing == null) {
            JsonObject misc = Utils.getConstant("misc");
            if(misc == null) {
                Utils.drawStringCenteredScaledMaxWidth("Duplicates: ERROR", Minecraft.getMinecraft().fontRendererObj, x+40, y+12, false, 70,
                        new Color(80, 80, 80).getRGB());
                return;
            }
            JsonElement talisman_upgrades_element = misc.get("talisman_upgrades");
            if(talisman_upgrades_element == null) {
                Utils.drawStringCenteredScaledMaxWidth("Duplicates: ERROR", Minecraft.getMinecraft().fontRendererObj, x+40, y+12, false, 70,
                        new Color(80, 80, 80).getRGB());
                return;
            }
            JsonObject talisman_upgrades = talisman_upgrades_element.getAsJsonObject();

            missing = new ArrayList<>();

            List<String> missingInternal = new ArrayList<>();
            for(Map.Entry<String, JsonObject> entry : NotEnoughUpdates.INSTANCE.manager.getItemInformation().entrySet()) {
                if(entry.getValue().has("lore")) {
                    if(checkItemType(entry.getValue().get("lore").getAsJsonArray(), "ACCESSORY", "HATCCESSORY") >= 0) {
                        missingInternal.add(entry.getKey());
                    }
                }
            }

            for(ItemStack stack : accessoryStacks) {
                String internalname = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(stack);
                missingInternal.remove(internalname);

                for(Map.Entry<String, JsonElement> talisman_upgrade_element : talisman_upgrades.entrySet()) {
                    JsonArray upgrades = talisman_upgrade_element.getValue().getAsJsonArray();
                    for(int j=0; j<upgrades.size(); j++) {
                        String upgrade = upgrades.get(j).getAsString();
                        if(internalname.equals(upgrade)) {
                            missingInternal.remove(talisman_upgrade_element.getKey());
                            break;
                        }
                    }
                }
            }

            missingInternal.sort(getItemComparator());

            for(String internal : missingInternal) {
                missing.add(NotEnoughUpdates.INSTANCE.manager.jsonToStack(NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(internal)));
            }
        }
        if(missing.isEmpty()) {
            Utils.drawStringCenteredScaledMaxWidth("No Missing", Minecraft.getMinecraft().fontRendererObj, x+40, y+12, false, 70,
                    new Color(80, 80, 80).getRGB());
        } else {
            Utils.drawStringCenteredScaledMaxWidth("Missing: " + missing.size(), Minecraft.getMinecraft().fontRendererObj, x+40, y+12, false, 70,
                    new Color(80, 80, 80).getRGB());

            int yIndex = 0;
            for(ItemStack missingStack : missing) {
                renderAlignedString(missingStack.getDisplayName(), "", x+5, y+20+11*yIndex, 70);
                if(missing.size() > 11) {
                    if(++yIndex >= 10) break;
                } else {
                    if(++yIndex >= 11) break;
                }
            }

            if(missing.size() > 11) {
                Utils.drawStringCenteredScaledMaxWidth("+" + (missing.size()-10) + " More",
                        Minecraft.getMinecraft().fontRendererObj, x+40, y+16+121, false, 70,
                        new Color(80, 80, 80).getRGB());
            }
        }
    }

    private static boolean forceCC = false;
    private static boolean forceAS = false;
    private static boolean useGodPot = true;
    private static boolean allowShaded = true;
    private static int mainWeapon = 1;

    public static void renderOptimizerOverlay(int x, int y) {
        Utils.drawStringCenteredScaledMaxWidth("Optimizer", Minecraft.getMinecraft().fontRendererObj, x+40, y+12, false, 70,
                new Color(80, 80, 80).getRGB());

        int light = new Color(220, 220, 220).getRGB();
        int dark = new Color(170, 170, 170).getRGB();

        Gui.drawRect(x+5, y+32, x+35, y+43, forceCC?dark:light);
        Gui.drawRect(x+45, y+32, x+75, y+43, forceCC?light:dark);

        Gui.drawRect(x+5, y+52, x+35, y+63, forceAS?dark:light);
        Gui.drawRect(x+45, y+52, x+75, y+63, forceAS?light:dark);

        Gui.drawRect(x+5, y+72, x+35, y+83, useGodPot?dark:light);
        Gui.drawRect(x+45, y+72, x+75, y+83, useGodPot?light:dark);

        Gui.drawRect(x+5, y+92, x+35, y+103, allowShaded?dark:light);
        Gui.drawRect(x+45, y+92, x+75, y+103, allowShaded?light:dark);

        Gui.drawRect(x+5, y+102, x+75, y+113, light);
        Gui.drawRect(x+5+(int)((mainWeapon-1)/9f*70), y+102, x+5+(int)(mainWeapon/9f*70), y+113, dark);

        Utils.drawStringCenteredScaledMaxWidth("Force 100% CC", Minecraft.getMinecraft().fontRendererObj, x+40, y+27, false, 70,
                new Color(80, 80, 80).getRGB());
        Utils.drawStringCenteredScaledMaxWidth((forceCC?EnumChatFormatting.GREEN:EnumChatFormatting.GRAY)+"YES",
                Minecraft.getMinecraft().fontRendererObj, x+20, y+37,
                true, 30, new Color(80, 80, 80).getRGB());
        Utils.drawStringCenteredScaledMaxWidth((forceCC?EnumChatFormatting.GRAY:EnumChatFormatting.RED)+"NO", Minecraft.getMinecraft().fontRendererObj, x+60, y+37,
                true, 30, new Color(80, 80, 80).getRGB());

        Utils.drawStringCenteredScaledMaxWidth("Force 100% ATKSPEED", Minecraft.getMinecraft().fontRendererObj, x+40, y+47, false, 70,
                new Color(80, 80, 80).getRGB());
        Utils.drawStringCenteredScaledMaxWidth((forceAS?EnumChatFormatting.GREEN:EnumChatFormatting.GRAY)+"YES", Minecraft.getMinecraft().fontRendererObj, x+20, y+57,
                true, 30, new Color(80, 80, 80).getRGB());
        Utils.drawStringCenteredScaledMaxWidth((forceAS?EnumChatFormatting.GRAY:EnumChatFormatting.RED)+"NO", Minecraft.getMinecraft().fontRendererObj, x+60, y+57,
                true, 30, new Color(80, 80, 80).getRGB());

        Utils.drawStringCenteredScaledMaxWidth("Use God Potion", Minecraft.getMinecraft().fontRendererObj, x+40, y+67, false, 70,
                new Color(80, 80, 80).getRGB());
        Utils.drawStringCenteredScaledMaxWidth((useGodPot?EnumChatFormatting.GREEN:EnumChatFormatting.GRAY)+"YES", Minecraft.getMinecraft().fontRendererObj, x+20, y+77,
                true, 30, new Color(80, 80, 80).getRGB());
        Utils.drawStringCenteredScaledMaxWidth((useGodPot?EnumChatFormatting.GRAY:EnumChatFormatting.RED)+"NO", Minecraft.getMinecraft().fontRendererObj, x+60, y+77,
                true, 30, new Color(80, 80, 80).getRGB());

        Utils.drawStringCenteredScaledMaxWidth("Use God Potion", Minecraft.getMinecraft().fontRendererObj, x+40, y+87, false, 70,
                new Color(80, 80, 80).getRGB());
        Utils.drawStringCenteredScaledMaxWidth((allowShaded?EnumChatFormatting.GREEN:EnumChatFormatting.GRAY)+"YES",
                Minecraft.getMinecraft().fontRendererObj, x+20, y+97,
                true, 30, new Color(80, 80, 80).getRGB());
        Utils.drawStringCenteredScaledMaxWidth((allowShaded?EnumChatFormatting.GRAY:EnumChatFormatting.RED)+"NO",
                Minecraft.getMinecraft().fontRendererObj, x+60, y+97,
                true, 30, new Color(80, 80, 80).getRGB());
        
        Utils.drawStringCenteredScaledMaxWidth("Main Weapon", Minecraft.getMinecraft().fontRendererObj, x+40, y+107, false, 70,
                new Color(80, 80, 80).getRGB());
        Utils.drawStringCenteredScaled("1 2 3 4 5 6 7 8 9",
                Minecraft.getMinecraft().fontRendererObj, x+40, y+117,
                true, 70, new Color(80, 80, 80).getRGB());
    }

    private static Comparator<String> getItemComparator() {
        return (o1, o2) -> {
            float cost1;
            JsonObject o1Auc = NotEnoughUpdates.INSTANCE.manager.auctionManager.getItemAuctionInfo(o1);
            if(o1Auc != null && o1Auc.has("price")) {
                cost1 = o1Auc.get("price").getAsFloat();
            } else {
                cost1 = NotEnoughUpdates.INSTANCE.manager.auctionManager.getCraftCost(o1).craftCost;
            }
            float cost2;
            JsonObject o2Auc = NotEnoughUpdates.INSTANCE.manager.auctionManager.getItemAuctionInfo(o2);
            if(o2Auc != null && o2Auc.has("price")) {
                cost2 = o2Auc.get("price").getAsFloat();
            } else {
                cost2 = NotEnoughUpdates.INSTANCE.manager.auctionManager.getCraftCost(o2).craftCost;
            }


            if(cost1 < cost2) return -1;
            if(cost1 > cost2) return 1;

            return o1.compareTo(o2);
        };
    }

    public static void renderOverlay() {
        if(Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
            GuiChest eventGui = (GuiChest) Minecraft.getMinecraft().currentScreen;
            ContainerChest cc = (ContainerChest) eventGui.inventorySlots;
            String containerName = cc.getLowerChestInventory().getDisplayName().getUnformattedText();
            if(containerName.trim().startsWith("Accessory Bag")) {
                try {
                    int xSize = (int) Utils.getField(GuiContainer.class, eventGui, "xSize", "field_146999_f");
                    int ySize = (int) Utils.getField(GuiContainer.class, eventGui, "ySize", "field_147000_g");
                    int guiLeft = (int) Utils.getField(GuiContainer.class, eventGui, "guiLeft", "field_147003_i");
                    int guiTop = (int) Utils.getField(GuiContainer.class, eventGui, "guiTop", "field_147009_r");

                    if(accessoryStacks.isEmpty()) {
                        for(ItemStack stack : Minecraft.getMinecraft().thePlayer.inventory.mainInventory) {
                            if(stack != null && isAccessory(stack)) {
                                accessoryStacks.add(stack);
                            }
                        }
                    }

                    if(containerName.trim().contains("(")) {
                        String first = containerName.trim().split("\\(")[1].split("/")[0];
                        Integer currentPageNumber = Integer.parseInt(first);
                        //System.out.println("current:"+currentPageNumber);
                        if(!pagesVisited.contains(currentPageNumber)) {
                            boolean hasStack = false;
                            if(Minecraft.getMinecraft().thePlayer.openContainer instanceof ContainerChest) {
                                IInventory inv = ((ContainerChest)Minecraft.getMinecraft().thePlayer.openContainer).getLowerChestInventory();
                                for(int i=0; i<inv.getSizeInventory(); i++) {
                                    ItemStack stack = inv.getStackInSlot(i);
                                    if(stack != null) {
                                        hasStack = true;
                                        if(isAccessory(stack)) {
                                            accessoryStacks.add(stack);
                                        }
                                    }
                                }
                            }

                            if(hasStack) pagesVisited.add(currentPageNumber);
                        }

                        String second = containerName.trim().split("/")[1].split("\\)")[0];
                        //System.out.println(second + ":" + pagesVisited.size());
                        if(Integer.parseInt(second) > pagesVisited.size()) {
                            Minecraft.getMinecraft().getTextureManager().bindTexture(accessory_bag_overlay);
                            Utils.drawTexturedRect(guiLeft+xSize+3, guiTop, 80, 149, 0, 80/256f, 0, 149/256f, GL11.GL_NEAREST);

                            renderVisitOverlay(guiLeft+xSize+3, guiTop);
                            return;
                        }
                    } else if(pagesVisited.isEmpty()) {
                        boolean hasStack = false;
                        if(Minecraft.getMinecraft().thePlayer.openContainer instanceof ContainerChest) {
                            IInventory inv = ((ContainerChest)Minecraft.getMinecraft().thePlayer.openContainer).getLowerChestInventory();
                            for(int i=0; i<inv.getSizeInventory(); i++) {
                                ItemStack stack = inv.getStackInSlot(i);
                                if(stack != null) {
                                    hasStack = true;
                                    if(isAccessory(stack)) {
                                        accessoryStacks.add(stack);
                                    }
                                }
                            }
                        }

                        if(hasStack) pagesVisited.add(1);
                    }

                    for(int i=0; i<=TAB_MISSING; i++) {
                        if(i != currentTab) {
                            Minecraft.getMinecraft().getTextureManager().bindTexture(accessory_bag_overlay);
                            Utils.drawTexturedRect(guiLeft+xSize+80, guiTop+20*i, 25, 22,
                                    80/256f, 105/256f, 0, 22/256f, GL11.GL_NEAREST);
                            Utils.drawItemStack(TAB_STACKS[i], guiLeft+xSize+80+5, guiTop+20*i+3);
                        }
                    }

                    Minecraft.getMinecraft().getTextureManager().bindTexture(accessory_bag_overlay);
                    Utils.drawTexturedRect(guiLeft+xSize+3, guiTop, 80, 149, 0, 80/256f, 0, 149/256f, GL11.GL_NEAREST);

                    if(pagesVisited.size() < 1) {
                        renderVisitOverlay(guiLeft+xSize+3, guiTop);
                        return;
                    }

                    Minecraft.getMinecraft().getTextureManager().bindTexture(accessory_bag_overlay);
                    Utils.drawTexturedRect(guiLeft+xSize+80, guiTop+20*currentTab, 28, 22,
                            80/256f, 108/256f, 22/256f, 44/256f, GL11.GL_NEAREST);
                    Utils.drawItemStack(TAB_STACKS[currentTab], guiLeft+xSize+80+8, guiTop+20*currentTab+3);

                    switch (currentTab) {
                        case TAB_BASIC:
                            renderBasicOverlay(guiLeft+xSize+3, guiTop); return;
                        case TAB_TOTAL:
                            renderTotalStatsOverlay(guiLeft+xSize+3, guiTop); return;
                        case TAB_BONUS:
                            renderReforgeStatsOverlay(guiLeft+xSize+3, guiTop); return;
                        case TAB_DUP:
                            renderDuplicatesOverlay(guiLeft+xSize+3, guiTop); return;
                        case TAB_MISSING:
                            renderMissingOverlay(guiLeft+xSize+3, guiTop); return;
                        case TAB_OPTIMIZER:
                            renderOptimizerOverlay(guiLeft+xSize+3, guiTop); return;
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void renderAlignedString(String first, String second, float x, float y, int length) {
        FontRenderer fontRendererObj = Minecraft.getMinecraft().fontRendererObj;

        if(fontRendererObj.getStringWidth(first + " " + second) >= length) {
            for(int xOff=-2; xOff<=2; xOff++) {
                for(int yOff=-2; yOff<=2; yOff++) {
                    if(Math.abs(xOff) != Math.abs(yOff)) {
                        Utils.drawStringCenteredScaledMaxWidth(Utils.cleanColourNotModifiers(first + " " + second), Minecraft.getMinecraft().fontRendererObj,
                                x+length/2f+xOff/2f, y+4+yOff/2f, false, length,
                                new Color(0, 0, 0, 200/Math.max(Math.abs(xOff), Math.abs(yOff))).getRGB());
                    }
                }
            }

            GlStateManager.color(1, 1, 1, 1);
            Utils.drawStringCenteredScaledMaxWidth(first + " " + second, Minecraft.getMinecraft().fontRendererObj,
                    x+length/2f, y+4, false, length, 4210752);
        } else {
            for(int xOff=-2; xOff<=2; xOff++) {
                for(int yOff=-2; yOff<=2; yOff++) {
                    if(Math.abs(xOff) != Math.abs(yOff)) {
                        fontRendererObj.drawString(Utils.cleanColourNotModifiers(first),
                                x+xOff/2f, y+yOff/2f,
                                new Color(0, 0, 0, 200/Math.max(Math.abs(xOff), Math.abs(yOff))).getRGB(), false);
                    }
                }
            }

            int secondLen = fontRendererObj.getStringWidth(second);
            GlStateManager.color(1, 1, 1, 1);
            fontRendererObj.drawString(first, x, y, 4210752, false);
            for(int xOff=-2; xOff<=2; xOff++) {
                for(int yOff=-2; yOff<=2; yOff++) {
                    if(Math.abs(xOff) != Math.abs(yOff)) {
                        fontRendererObj.drawString(Utils.cleanColourNotModifiers(second),
                                x+length-secondLen+xOff/2f, y+yOff/2f,
                                new Color(0, 0, 0, 200/Math.max(Math.abs(xOff), Math.abs(yOff))).getRGB(), false);
                    }
                }
            }

            GlStateManager.color(1, 1, 1, 1);
            fontRendererObj.drawString(second, x+length-secondLen, y, 4210752, false);
        }
    }

    private static final Pattern HEALTH_PATTERN_BONUS = Pattern.compile("^Health: (?:\\+|-)[0-9]+ HP \\([a-zA-Z]+ ((?:\\+|-)[0-9]+)");
    private static final Pattern DEFENCE_PATTERN_BONUS = Pattern.compile("^Defense: (?:\\+|-)[0-9]+ \\([a-zA-Z]+ ((?:\\+|-)[0-9]+)");
    private static final Pattern STRENGTH_PATTERN_BONUS = Pattern.compile("^Strength: (?:\\+|-)[0-9]+ \\([a-zA-Z]+ ((?:\\+|-)[0-9]+)");
    private static final Pattern SPEED_PATTERN_BONUS = Pattern.compile("^Speed: (?:\\+|-)[0-9]+ \\([a-zA-Z]+ ((?:\\+|-)[0-9]+)");
    private static final Pattern CC_PATTERN_BONUS = Pattern.compile("^Crit Chance: (?:\\+|-)[0-9]+% \\([a-zA-Z]+ ((?:\\+|-)[0-9]+)");
    private static final Pattern CD_PATTERN_BONUS = Pattern.compile("^Crit Damage: (?:\\+|-)[0-9]+% \\([a-zA-Z]+ ((?:\\+|-)[0-9]+)");
    private static final Pattern ATKSPEED_PATTERN_BONUS = Pattern.compile("^Bonus Attack Speed: (?:\\+|-)[0-9]+% \\([a-zA-Z]+ ((?:\\+|-)[0-9]+)");
    private static final Pattern INTELLIGENCE_PATTERN_BONUS = Pattern.compile("^Intelligence: (?:\\+|-)[0-9]+ \\([a-zA-Z]+ ((?:\\+|-)[0-9]+)");
    private static final Pattern SCC_PATTERN_BONUS = Pattern.compile("^Sea Creature Chance: (?:\\+|-)[0-9]+ \\([a-zA-Z]+ ((?:\\+|-)[0-9]+)");
    private static final HashMap<String, Pattern> STAT_PATTERN_MAP_BONUS = new HashMap<>();
    static {
        STAT_PATTERN_MAP_BONUS.put("health", HEALTH_PATTERN_BONUS);
        STAT_PATTERN_MAP_BONUS.put("defence", DEFENCE_PATTERN_BONUS);
        STAT_PATTERN_MAP_BONUS.put("strength", STRENGTH_PATTERN_BONUS);
        STAT_PATTERN_MAP_BONUS.put("speed", SPEED_PATTERN_BONUS);
        STAT_PATTERN_MAP_BONUS.put("crit_chance", CC_PATTERN_BONUS);
        STAT_PATTERN_MAP_BONUS.put("crit_damage", CD_PATTERN_BONUS);
        STAT_PATTERN_MAP_BONUS.put("bonus_attack_speed", ATKSPEED_PATTERN_BONUS);
        STAT_PATTERN_MAP_BONUS.put("intelligence", INTELLIGENCE_PATTERN_BONUS);
        STAT_PATTERN_MAP_BONUS.put("sea_creature_chance", SCC_PATTERN_BONUS);
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
    private static PlayerStats.Stats getStatForItem(ItemStack stack, HashMap<String, Pattern> patternMap, boolean addExtras) {
        String internalname = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(stack);
        NBTTagCompound tag = stack.getTagCompound();
        PlayerStats.Stats stats = new PlayerStats.Stats();

        if(internalname == null) {
            return stats;
        }

        if(tag != null) {
            NBTTagCompound display = tag.getCompoundTag("display");
            if (display.hasKey("Lore", 9)) {
                NBTTagList list = display.getTagList("Lore", 8);
                for (int i = 0; i < list.tagCount(); i++) {
                    String line = list.getStringTagAt(i);
                    for(Map.Entry<String, Pattern> entry : patternMap.entrySet()) {
                        Matcher matcher = entry.getValue().matcher(Utils.cleanColour(line));
                        if(matcher.find()) {
                            int bonus = Integer.parseInt(matcher.group(1));
                            stats.addStat(entry.getKey(), bonus);
                        }
                    }
                }
            }
        }

        if(!addExtras) return stats;

        if(internalname.equals("DAY_CRYSTAL") || internalname.equals("NIGHT_CRYSTAL")) {
            stats.addStat(PlayerStats.STRENGTH, 2.5f);
            stats.addStat(PlayerStats.DEFENCE, 2.5f);
        }

        if(internalname.equals("NEW_YEAR_CAKE_BAG") && tag != null && tag.hasKey("ExtraAttributes", 10)) {
            NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");

            byte[] bytes = null;
            for (String key : ea.getKeySet()) {
                if (key.endsWith("backpack_data") || key.equals("new_year_cake_bag_data")) {
                    bytes = ea.getByteArray(key);
                    try {
                        NBTTagCompound contents_nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes));
                        NBTTagList items = contents_nbt.getTagList("i", 10);
                        HashSet<Integer> cakes = new HashSet<>();
                        for(int j=0; j<items.tagCount(); j++) {
                            if(items.getCompoundTagAt(j).getKeySet().size() > 0) {
                                NBTTagCompound nbt = items.getCompoundTagAt(j).getCompoundTag("tag");
                                if(nbt != null && nbt.hasKey("ExtraAttributes", 10)) {
                                    NBTTagCompound ea2 = nbt.getCompoundTag("ExtraAttributes");
                                    if (ea2.hasKey("new_years_cake")) {
                                        cakes.add(ea2.getInteger("new_years_cake"));
                                    }
                                }
                            }
                        }
                        stats.addStat(PlayerStats.HEALTH, cakes.size());
                    } catch(IOException e) {
                        e.printStackTrace();
                        return stats;
                    }
                    break;
                }
            }
        }
        return stats;
    }

    private static String[] rarityArr = new String[] {
            "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "SPECIAL", "VERY SPECIAL",
    };
    private static String[] rarityArrC = new String[] {
            EnumChatFormatting.WHITE+EnumChatFormatting.BOLD.toString()+"COMMON",
            EnumChatFormatting.GREEN+EnumChatFormatting.BOLD.toString()+"UNCOMMON",
            EnumChatFormatting.BLUE+EnumChatFormatting.BOLD.toString()+"RARE",
            EnumChatFormatting.DARK_PURPLE+EnumChatFormatting.BOLD.toString()+"EPIC",
            EnumChatFormatting.GOLD+EnumChatFormatting.BOLD.toString()+"LEGENDARY",
            EnumChatFormatting.LIGHT_PURPLE+EnumChatFormatting.BOLD.toString()+"MYTHIC",
            EnumChatFormatting.RED+EnumChatFormatting.BOLD.toString()+"SPECIAL",
            EnumChatFormatting.RED+EnumChatFormatting.BOLD.toString()+"VERY SPECIAL",
            EnumChatFormatting.DARK_RED+EnumChatFormatting.BOLD.toString()+"SUPREME",
    };
    public static int checkItemType(ItemStack stack, boolean contains, String... typeMatches) {
        NBTTagCompound tag = stack.getTagCompound();
        if(tag != null) {
            NBTTagCompound display = tag.getCompoundTag("display");
            if (display.hasKey("Lore", 9)) {
                NBTTagList list = display.getTagList("Lore", 8);
                for (int i = list.tagCount()-1; i >= 0; i--) {
                    String line = list.getStringTagAt(i);
                    for(String rarity : rarityArr) {
                        for(int j=0; j<typeMatches.length; j++) {
                            if(contains) {
                                if(line.trim().contains(rarity + " " + typeMatches[j])) {
                                    return j;
                                } else if(line.trim().contains(rarity + " DUNGEON " + typeMatches[j])) {
                                    return j;
                                }
                            } else {
                                if(line.trim().endsWith(rarity + " " + typeMatches[j])) {
                                    return j;
                                } else if(line.trim().endsWith(rarity + " DUNGEON " + typeMatches[j])) {
                                    return j;
                                }
                            }
                        }
                    }
                }
            }
        }
        return -1;
    }
    private static int checkItemType(JsonArray lore, String... typeMatches) {
        for(int i=lore.size()-1; i>=0; i--) {
            String line = lore.get(i).getAsString();

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
    
    public static boolean isAccessory(ItemStack stack) {
        return checkItemType(stack, false, "ACCESSORY", "HATCCESSORY") >= 0;
    }

    public static int getRarity(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if(tag != null) {
            NBTTagCompound display = tag.getCompoundTag("display");
            if (display.hasKey("Lore", 9)) {
                NBTTagList list = display.getTagList("Lore", 8);
                for (int i = list.tagCount(); i >= 0; i--) {
                    String line = list.getStringTagAt(i);
                    for(int j=0; j<rarityArrC.length; j++) {
                        if(line.startsWith(rarityArrC[j])) {
                            return j;
                        }
                    }
                }
            }
        }
        return -1;
    }
}
