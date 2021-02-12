package io.github.moulberry.notenoughupdates;

import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.auction.APIManager;
import io.github.moulberry.notenoughupdates.util.Constants;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import zone.nora.moulberry.MoulberryKt;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class ItemPriceInformation {

    public static boolean addToTooltip(List<String> tooltip, String internalname, ItemStack stack) {
        JsonObject auctionInfo = NotEnoughUpdates.INSTANCE.manager.auctionManager.getItemAuctionInfo(internalname);
        JsonObject bazaarInfo = NotEnoughUpdates.INSTANCE.manager.auctionManager.getBazaarInfo(internalname);
        float lowestBinAvg = NotEnoughUpdates.INSTANCE.manager.auctionManager.getItemAvgBin(internalname);

        int lowestBin = NotEnoughUpdates.INSTANCE.manager.auctionManager.getLowestBin(internalname);
        APIManager.CraftInfo craftCost = NotEnoughUpdates.INSTANCE.manager.auctionManager.getCraftCost(internalname);

        boolean auctionItem = lowestBin > 0 || lowestBinAvg > 0 || auctionInfo != null;
        boolean bazaarItem = bazaarInfo != null;

        NumberFormat format = NumberFormat.getInstance(Locale.US);

        if(bazaarItem) {
            int[] lines = {
                    NotEnoughUpdates.INSTANCE.config.priceInfoBaz.line1,
                    NotEnoughUpdates.INSTANCE.config.priceInfoBaz.line2,
                    NotEnoughUpdates.INSTANCE.config.priceInfoBaz.line3,
                    NotEnoughUpdates.INSTANCE.config.priceInfoBaz.line4,
                    NotEnoughUpdates.INSTANCE.config.priceInfoBaz.line5,
                    NotEnoughUpdates.INSTANCE.config.priceInfoBaz.line6
            };

            final AtomicBoolean added = new AtomicBoolean(false);

            boolean shiftPressed = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);

            int stackMultiplier = 1;
            int shiftStackMultiplier = 64;
            if(shiftPressed) {
                stackMultiplier = shiftStackMultiplier;
            }

            //values = {"", "Buy", "Sell", "Buy (Insta)", "Sell (Insta)", "Raw Craft Cost"}
            for(int lineId : lines) {
                int stackMultiplier2 = stackMultiplier;
                MoulberryKt.javaSwitch(lineId, lineSwitch -> {
                    lineSwitch.addCase(1, false, () -> {
                        if(bazaarInfo.has("avg_buy")) {
                            if(!added.get()) {
                                tooltip.add("");
                                if(!shiftPressed) tooltip.add(EnumChatFormatting.DARK_GRAY.toString()+"[SHIFT show x"+shiftStackMultiplier+"]");
                                added.set(true);
                            }
                            int bazaarBuyPrice = (int)bazaarInfo.get("avg_buy").getAsFloat()* stackMultiplier2;
                            tooltip.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Bazaar Buy: "+
                                    EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+format.format(bazaarBuyPrice)+" coins");
                        }
                    });
                    lineSwitch.addCase(2, false, () -> {
                        if(bazaarInfo.has("avg_sell")) {
                            if(!added.get()) {
                                tooltip.add("");
                                if(!shiftPressed) tooltip.add(EnumChatFormatting.DARK_GRAY.toString()+"[SHIFT show x"+shiftStackMultiplier+"]");
                                added.set(true);
                            }
                            int bazaarSellPrice = (int)bazaarInfo.get("avg_sell").getAsFloat()*stackMultiplier2;
                            tooltip.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Bazaar Sell: "+
                                    EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+format.format(bazaarSellPrice)+" coins");
                        }
                    });
                    lineSwitch.addCase(3, false, () -> {
                        if(bazaarInfo.has("curr_buy")) {
                            if(!added.get()) {
                                tooltip.add("");
                                if(!shiftPressed) tooltip.add(EnumChatFormatting.DARK_GRAY.toString()+"[SHIFT show x"+shiftStackMultiplier+"]");
                                added.set(true);
                            }
                            int bazaarInstantBuyPrice = (int)bazaarInfo.get("curr_buy").getAsFloat()*stackMultiplier2;
                            tooltip.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Bazaar Insta-Buy: "+
                                    EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+format.format(bazaarInstantBuyPrice)+" coins");
                        }
                    });
                    lineSwitch.addCase(4, false, () -> {
                        if(bazaarInfo.has("curr_sell")) {
                            if(!added.get()) {
                                tooltip.add("");
                                if(!shiftPressed) tooltip.add(EnumChatFormatting.DARK_GRAY.toString()+"[SHIFT show x"+shiftStackMultiplier+"]");
                                added.set(true);
                            }
                            int bazaarInstantSellPrice = (int)bazaarInfo.get("curr_sell").getAsFloat()*stackMultiplier2;
                            tooltip.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Bazaar Insta-Sell: "+
                                    EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+format.format(bazaarInstantSellPrice)+" coins");
                        }
                    });
                    lineSwitch.addCase(5, false, () -> {
                        if(craftCost.fromRecipe) {
                            if(!added.get()) {
                                tooltip.add("");
                                added.set(true);
                            }
                            tooltip.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Raw Craft Cost: "+
                                    EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+format.format((int)craftCost.craftCost)+" coins");
                        }
                    });
                    return lineSwitch;
                });
            }

            return added.get();
        } else if(auctionItem) {
            int[] lines = {
                NotEnoughUpdates.INSTANCE.config.priceInfoAuc.line1,
                NotEnoughUpdates.INSTANCE.config.priceInfoAuc.line2,
                NotEnoughUpdates.INSTANCE.config.priceInfoAuc.line3,
                NotEnoughUpdates.INSTANCE.config.priceInfoAuc.line4,
                NotEnoughUpdates.INSTANCE.config.priceInfoAuc.line5,
                NotEnoughUpdates.INSTANCE.config.priceInfoAuc.line6
            };
            final AtomicBoolean added = new AtomicBoolean(false);

            for(int lineId : lines) {
                MoulberryKt.javaSwitch(lineId, lineSwitch -> {
                    lineSwitch.addCase(1, false, () -> {
                        if(lowestBin > 0) {
                            if(!added.get()) {
                                tooltip.add("");
                                added.set(true);
                            }
                            tooltip.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Lowest BIN: " +
                                    EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+format.format(lowestBin)+" coins");
                        }
                    });
                    lineSwitch.addCase(2, false, () -> {
                        if(auctionInfo != null) {
                            if(!added.get()) {
                                tooltip.add("");
                                added.set(true);
                            }
                            if(auctionInfo.has("clean_price")) {
                                tooltip.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"AH Price (Clean): "+ EnumChatFormatting.GOLD+
                                        EnumChatFormatting.BOLD+
                                        format.format((int)auctionInfo.get("clean_price").getAsFloat())+" coins");
                            } else {
                                int auctionPrice = (int)(auctionInfo.get("price").getAsFloat() / auctionInfo.get("count").getAsFloat());
                                tooltip.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"AH Price: "+ EnumChatFormatting.GOLD+
                                        EnumChatFormatting.BOLD+format.format(auctionPrice)+" coins");
                            }
                        }
                    });
                    lineSwitch.addCase(3, false, () -> {
                        if(auctionInfo != null) {
                            if(!added.get()) {
                                tooltip.add("");
                                added.set(true);
                            }
                            if(auctionInfo.has("clean_price")) {
                                tooltip.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"AH Sales (Clean): "+
                                        EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+
                                        format.format(auctionInfo.get("clean_sales").getAsFloat())+" sales/day");
                            } else {
                                tooltip.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"AH Sales: "+
                                        EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+
                                        format.format(auctionInfo.get("sales").getAsFloat())+" sales/day");
                            }
                        }
                    });
                    lineSwitch.addCase(4, false, () -> {
                        if(craftCost.fromRecipe) {
                            if(!added.get()) {
                                tooltip.add("");
                                added.set(true);
                            }
                            tooltip.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Raw Craft Cost: "+
                                    EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+format.format((int)craftCost.craftCost)+" coins");
                        }
                    });
                    lineSwitch.addCase(5, false, () -> {
                        if(lowestBinAvg > 0) {
                            if(!added.get()) {
                                tooltip.add("");
                                added.set(true);
                            }
                            tooltip.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"AVG Lowest BIN: "+
                                    EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+format.format(lowestBinAvg)+" coins");
                        }
                    });
                    lineSwitch.addCase(6, false, () -> {
                        if(Constants.ESSENCECOSTS == null) return;
                        JsonObject essenceCosts = Constants.ESSENCECOSTS;
                        if(!essenceCosts.has(internalname)) {
                            return;
                        }
                        JsonObject itemCosts = essenceCosts.get(internalname).getAsJsonObject();
                        String essenceType = itemCosts.get("type").getAsString();

                        int dungeonItemLevel = -1;
                        if(stack != null && stack.hasTagCompound() &&
                                stack.getTagCompound().hasKey("ExtraAttributes", 10)) {
                            NBTTagCompound ea = stack.getTagCompound().getCompoundTag("ExtraAttributes");

                            if (ea.hasKey("dungeon_item_level", 99)) {
                                dungeonItemLevel = ea.getInteger("dungeon_item_level");
                            }
                        }
                        if(dungeonItemLevel == -1) {
                            int dungeonizeCost = 0;
                            if(itemCosts.has("dungeonize")) {
                                dungeonizeCost = itemCosts.get("dungeonize").getAsInt();
                            }
                            tooltip.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Dungeonize Cost: " +
                                    EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+dungeonizeCost+" "+essenceType);
                        } else if(dungeonItemLevel >= 0 && dungeonItemLevel <= 4) {
                            String costType = (dungeonItemLevel+1)+"";

                            int upgradeCost = itemCosts.get(costType).getAsInt();
                            StringBuilder star = new StringBuilder();
                            for(int i=0; i<=dungeonItemLevel; i++) {
                                star.append('\u272A');
                            }
                            tooltip.add(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"Upgrade to "+
                                    EnumChatFormatting.GOLD+star+EnumChatFormatting.YELLOW+EnumChatFormatting.BOLD+": " +
                                    EnumChatFormatting.GOLD+EnumChatFormatting.BOLD+upgradeCost+" "+essenceType);
                        }
                    });
                    return lineSwitch;
                });
            }

            return added.get();
        }

        return false;
    }

}
