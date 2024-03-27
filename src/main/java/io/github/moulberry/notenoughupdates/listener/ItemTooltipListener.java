/*
 * Copyright (C) 2022 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.moulberry.notenoughupdates.listener;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import io.github.moulberry.notenoughupdates.ItemPriceInformation;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.util.MiscUtils;
import io.github.moulberry.notenoughupdates.core.util.StringUtils;
import io.github.moulberry.notenoughupdates.miscfeatures.PetInfoOverlay;
import io.github.moulberry.notenoughupdates.util.Constants;
import io.github.moulberry.notenoughupdates.util.PetLeveling;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemTooltipListener {
	public static final String petToolTipRegex =
		"((Farming)|(Combat)|(Fishing)|(Mining)|(Foraging)|(Enchanting)|(Alchemy)) ((Mount)|(Pet)|(Morph)).*";
	private final NotEnoughUpdates neu;
	private final Pattern xpLevelPattern = Pattern.compile("(.*) (\\xA7e(.*)\\xA76/\\xA7e(.*))");
	private final HashSet<String> percentStats = new HashSet<>();
	DecimalFormat myFormatter = new DecimalFormat("#,###,###.###");
	private String currentRarity = "COMMON";
	private boolean copied = false;
	private boolean showReforgeStoneStats = true;
	private boolean pressedArrowLast = false;
	private boolean pressedShiftLast = false;

	public ItemTooltipListener(NotEnoughUpdates neu) {
		this.neu = neu;
		percentStats.add("bonus_attack_speed");
		percentStats.add("crit_damage");
		percentStats.add("crit_chance");
		percentStats.add("sea_creature_chance");
		percentStats.add("ability_damage");
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public void onItemTooltipLow(ItemTooltipEvent event) {
		if (!NotEnoughUpdates.INSTANCE.isOnSkyblock()) return;

		String internalName = NotEnoughUpdates.INSTANCE.manager
			.createItemResolutionQuery()
			.withCurrentGuiContext()
			.withItemStack(event.itemStack)
			.resolveInternalName();

		if (internalName == null) {
			return;
		}
		petToolTipXPExtendPetMenu(event);

		boolean hasEnchantments = event.itemStack.getTagCompound().getCompoundTag("ExtraAttributes").hasKey(
			"enchantments",
			10
		);
		Set<String> enchantIds = new HashSet<>();
		if (hasEnchantments) enchantIds = event.itemStack.getTagCompound().getCompoundTag("ExtraAttributes").getCompoundTag(
			"enchantments").getKeySet();

		JsonObject enchantsConst = Constants.ENCHANTS;
		JsonArray allItemEnchs = null;
		Set<String> ignoreFromPool = new HashSet<>();
		if (enchantsConst != null && hasEnchantments && NotEnoughUpdates.INSTANCE.config.tooltipTweaks.missingEnchantList) {
			try {
				JsonArray enchantPools = enchantsConst.get("enchant_pools").getAsJsonArray();
				for (JsonElement element : enchantPools) {
					Set<String> currentPool = new HashSet<>();
					for (JsonElement poolElement : element.getAsJsonArray()) {
						String poolS = poolElement.getAsString();
						currentPool.add(poolS);
					}
					for (JsonElement poolElement : element.getAsJsonArray()) {
						String poolS = poolElement.getAsString();
						if (enchantIds.contains(poolS)) {
							ignoreFromPool.addAll(currentPool);
							break;
						}
					}
				}

				JsonObject enchantsObj = enchantsConst.get("enchants").getAsJsonObject();
				NBTTagCompound tag = event.itemStack.getTagCompound();
				if (tag != null) {
					NBTTagCompound display = tag.getCompoundTag("display");
					if (display.hasKey("Lore", 9)) {
						NBTTagList list = display.getTagList("Lore", 8);
						out:
						for (int i = list.tagCount(); i >= 0; i--) {
							String line = list.getStringTagAt(i);
							for (int j = 0; j < Utils.rarityArrC.length; j++) {
								for (Map.Entry<String, JsonElement> entry : enchantsObj.entrySet()) {
									if (line.contains(Utils.rarityArrC[j] + " " + entry.getKey()) || line.contains(
										Utils.rarityArrC[j] + " DUNGEON " + entry.getKey()) || line.contains(
										"SHINY " + Utils.rarityArrC[j].replaceAll("§.§.", "") + " DUNGEON " + entry.getKey())) {
										allItemEnchs = entry.getValue().getAsJsonArray();
										break out;
									}
								}
							}
						}
					}
				}
			} catch (Exception ignored) {
			}
		}

		boolean gotToEnchants = false;
		boolean passedEnchants = false;

		boolean dungeonProfit = false;
		List<String> newTooltip = new ArrayList<>();

		for (int k = 0; k < event.toolTip.size(); k++) {
			String line = event.toolTip.get(k);

			if (line.endsWith(EnumChatFormatting.DARK_GRAY + "Reforge Stone") &&
				NotEnoughUpdates.INSTANCE.config.tooltipTweaks.showReforgeStats) {
				JsonObject reforgeStones = Constants.REFORGESTONES;

				if (reforgeStones != null && reforgeStones.has(internalName)) {
					boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
					if (!pressedShiftLast && shift) {
						showReforgeStoneStats = !showReforgeStoneStats;
					}
					pressedShiftLast = shift;

					newTooltip.add(line);
					newTooltip.add("");
					if (!showReforgeStoneStats) {
						newTooltip.add(EnumChatFormatting.DARK_GRAY + "[Press SHIFT to show extra info]");
					} else {
						newTooltip.add(EnumChatFormatting.DARK_GRAY + "[Press SHIFT to hide extra info]");
					}

					JsonObject reforgeInfo = reforgeStones.get(internalName).getAsJsonObject();
					JsonArray requiredRaritiesArray = reforgeInfo.get("requiredRarities").getAsJsonArray();

					if (showReforgeStoneStats && requiredRaritiesArray.size() > 0) {
						String reforgeName = Utils.getElementAsString(reforgeInfo.get("reforgeName"), "");

						String[] requiredRarities = new String[requiredRaritiesArray.size()];
						for (int i = 0; i < requiredRaritiesArray.size(); i++) {
							requiredRarities[i] = requiredRaritiesArray.get(i).getAsString();
						}

						int rarityIndex = requiredRarities.length - 1;
						String rarity = requiredRarities[rarityIndex];
						for (int i = 0; i < requiredRarities.length; i++) {
							String rar = requiredRarities[i];
							if (rar.equalsIgnoreCase(currentRarity)) {
								rarity = rar;
								rarityIndex = i;
								break;
							}
						}

						boolean left = Keyboard.isKeyDown(Keyboard.KEY_LEFT);
						boolean right = Keyboard.isKeyDown(Keyboard.KEY_RIGHT);
						if (!pressedArrowLast && (left || right)) {
							if (left) {
								rarityIndex--;
							} else {
								rarityIndex++;
							}
							if (rarityIndex < 0) rarityIndex = 0;
							if (rarityIndex >= requiredRarities.length) rarityIndex = requiredRarities.length - 1;
							currentRarity = requiredRarities[rarityIndex];
							rarity = currentRarity;
						}
						pressedArrowLast = left || right;

						JsonElement statsE = reforgeInfo.get("reforgeStats");

						String rarityFormatted = Utils.rarityArrMap.getOrDefault(rarity, rarity);

						JsonElement reforgeAbilityE = reforgeInfo.get("reforgeAbility");
						String reforgeAbility = null;
						if (reforgeAbilityE != null) {
							if (reforgeAbilityE.isJsonPrimitive() && reforgeAbilityE.getAsJsonPrimitive().isString()) {
								reforgeAbility = Utils.getElementAsString(reforgeInfo.get("reforgeAbility"), "");

							} else if (reforgeAbilityE.isJsonObject()) {
								if (reforgeAbilityE.getAsJsonObject().has(rarity)) {
									reforgeAbility = reforgeAbilityE.getAsJsonObject().get(rarity).getAsString();
								}
							}
						}

						if (reforgeAbility != null && !reforgeAbility.isEmpty()) {
							String text = EnumChatFormatting.BLUE + (reforgeName.isEmpty() ? "Bonus: " : reforgeName + " Bonus: ") +
								EnumChatFormatting.GRAY + reforgeAbility;
							boolean first = true;
							for (String s : Minecraft.getMinecraft().fontRendererObj.listFormattedStringToWidth(text, 150)) {
								newTooltip.add((first ? "" : "  ") + s);
								first = false;
							}
							newTooltip.add("");
						}

						newTooltip.add(EnumChatFormatting.BLUE + "Stats for " + rarityFormatted + "§9: [§l§m< §9Switch§l➡§9]");

						if (statsE != null && statsE.isJsonObject()) {
							JsonObject stats = statsE.getAsJsonObject();

							JsonElement statsRarE = stats.get(rarity);
							if (statsRarE != null && statsRarE.isJsonObject()) {

								JsonObject statsRar = statsRarE.getAsJsonObject();

								TreeSet<Map.Entry<String, JsonElement>> sorted = new TreeSet<>(Map.Entry.comparingByKey());
								sorted.addAll(statsRar.entrySet());

								for (Map.Entry<String, JsonElement> entry : sorted) {
									if (entry.getValue().isJsonPrimitive() && ((JsonPrimitive) entry.getValue()).isNumber()) {
										float statNumF = entry.getValue().getAsFloat();
										String statNumS;
										if (statNumF % 1 == 0) {
											statNumS = String.valueOf(Math.round(statNumF));
										} else {
											statNumS = Utils.floatToString(statNumF, 1);
										}
										String reforgeNamePretty = WordUtils.capitalizeFully(entry.getKey().replace("_", " "));
										String text =
											EnumChatFormatting.GRAY + reforgeNamePretty + ": " + EnumChatFormatting.GREEN + "+" + statNumS;
										if (percentStats.contains(entry.getKey())) {
											text += "%";
										}
										newTooltip.add("  " + text);
									}
								}
							}
						}

						JsonElement reforgeCostsE = reforgeInfo.get("reforgeCosts");
						int reforgeCost = -1;
						if (reforgeCostsE != null) {
							if (reforgeCostsE.isJsonPrimitive() && reforgeCostsE.getAsJsonPrimitive().isNumber()) {
								reforgeCost = (int) Utils.getElementAsFloat(reforgeInfo.get("reforgeAbility"), -1);

							} else if (reforgeCostsE.isJsonObject()) {
								if (reforgeCostsE.getAsJsonObject().has(rarity)) {
									reforgeCost = (int) Utils.getElementAsFloat(reforgeCostsE.getAsJsonObject().get(rarity), -1);
								}
							}
						}

						if (reforgeCost >= 0) {
							String text = EnumChatFormatting.BLUE + "Apply Cost: " + EnumChatFormatting.GOLD +
								StringUtils.formatNumber(reforgeCost) + " coins";
							newTooltip.add("");
							newTooltip.add(text);
						}

					}

					continue;
				}

			} else if (line.contains("\u00A7cR\u00A76a\u00A7ei\u00A7an\u00A7bb\u00A79o\u00A7dw\u00A79 Rune")) {
				line = line.replace(
					"\u00A7cR\u00A76a\u00A7ei\u00A7an\u00A7bb\u00A79o\u00A7dw\u00A79 Rune",
					Utils.chromaString("Rainbow Rune", k, false) + EnumChatFormatting.BLUE
				);
			} else if (hasEnchantments) {
				if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) &&
					NotEnoughUpdates.INSTANCE.config.tooltipTweaks.missingEnchantList) {
					boolean lineHasEnch = false;
					for (String s : enchantIds) {
						String enchantName = WordUtils.capitalizeFully(s.replace("_", " "));
						if (line.contains(enchantName)) {
							lineHasEnch = true;
							break;
						}
					}
					if (lineHasEnch) {
						gotToEnchants = true;
					} else {
						if (gotToEnchants && !passedEnchants && Utils.cleanColour(line).trim().length() == 0) {
							if (enchantsConst != null && allItemEnchs != null) {
								List<String> missing = new ArrayList<>();
								for (JsonElement enchIdElement : allItemEnchs) {
									String enchId = enchIdElement.getAsString();
									if (!enchId.startsWith("ultimate_") && !ignoreFromPool.contains(enchId) &&
										!enchantIds.contains(enchId)) {
										missing.add(enchId);
									}
								}
								if (!missing.isEmpty()) {
									newTooltip.add("");
									StringBuilder currentLine = new StringBuilder(
										EnumChatFormatting.RED + "Missing: " + EnumChatFormatting.GRAY);
									for (int i = 0; i < missing.size(); i++) {
										String enchName = WordUtils.capitalizeFully(missing.get(i).replace("_", " "));
										if (currentLine.length() != 0 &&
											(Utils.cleanColour(currentLine.toString()).length() + enchName.length()) > 40) {
											newTooltip.add(currentLine.toString());
											currentLine = new StringBuilder();
										}
										if (currentLine.length() != 0 && i != 0) {
											currentLine.append(", ").append(enchName);
										} else {
											currentLine.append(EnumChatFormatting.GRAY).append(enchName);
										}
									}
									if (currentLine.length() != 0) {
										newTooltip.add(currentLine.toString());
									}
								}
							}
							passedEnchants = true;
						}
					}
				}
			}

			newTooltip.add(line);

			if (NotEnoughUpdates.INSTANCE.config.tooltipTweaks.showPriceInfoAucItem) {
				if (line.contains(EnumChatFormatting.GRAY + "Buy it now: ") || line.contains(
					EnumChatFormatting.GRAY + "Bidder: ") || line.contains(EnumChatFormatting.GRAY + "Starting bid: ")) {

					if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
						newTooltip.add("");
						newTooltip.add(EnumChatFormatting.GRAY + "[SHIFT for Price Info]");
					} else {
						ItemPriceInformation.addToTooltip(newTooltip, internalName, event.itemStack);
					}
				}
			}

			if (NotEnoughUpdates.INSTANCE.config.dungeons.profitDisplayLoc == 2 &&
				Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
				if (line.contains(EnumChatFormatting.GREEN + "Open Reward Chest")) {
					dungeonProfit = true;
				} else if (k == 7 && dungeonProfit) {
					GuiChest eventGui = (GuiChest) Minecraft.getMinecraft().currentScreen;
					ContainerChest cc = (ContainerChest) eventGui.inventorySlots;
					IInventory lower = cc.getLowerChestInventory();

					int chestCost = 0;
					try {
						String line6 = Utils.cleanColour(line);
						StringBuilder cost = new StringBuilder();
						for (int i = 0; i < line6.length(); i++) {
							char c = line6.charAt(i);
							if ("0123456789".indexOf(c) >= 0) {
								cost.append(c);
							}
						}
						if (cost.length() > 0) {
							chestCost = Integer.parseInt(cost.toString());
						}
					} catch (Exception ignored) {
					}

					String missingItem = null;
					int totalValue = 0;
					HashMap<String, Double> itemValues = new HashMap<>();
					for (int i = 0; i < 5; i++) {
						ItemStack item = lower.getStackInSlot(11 + i);
						String internal = neu.manager.getInternalNameForItem(item);
						if (internal != null) {
							internal = internal.replace("\u00CD", "I").replace("\u0130", "I");
							float bazaarPrice = -1;
							JsonObject bazaarInfo = neu.manager.auctionManager.getBazaarInfo(internal);
							if (bazaarInfo != null && bazaarInfo.has("curr_sell")) {
								bazaarPrice = bazaarInfo.get("curr_sell").getAsFloat();
							}
							if (bazaarPrice < 5000000 && internal.equals("RECOMBOBULATOR_3000")) bazaarPrice = 5000000;

							double worth = -1;
							if (bazaarPrice > 0) {
								worth = bazaarPrice;
							} else {
								switch (NotEnoughUpdates.INSTANCE.config.dungeons.profitType) {
									case 1:
										worth = neu.manager.auctionManager.getItemAvgBin(internal);
										break;
									case 2:
										JsonObject auctionInfo = neu.manager.auctionManager.getItemAuctionInfo(internal);
										if (auctionInfo != null) {
											if (auctionInfo.has("clean_price")) {
												worth = (long) auctionInfo.get("clean_price").getAsDouble();
											} else {
												worth =
													(long) (auctionInfo.get("price").getAsDouble() / auctionInfo.get("count").getAsDouble());
											}
										}
										break;
									default:
										worth = neu.manager.auctionManager.getLowestBin(internal);
								}
								if (worth <= 0) {
									worth = neu.manager.auctionManager.getLowestBin(internal);
									if (worth <= 0) {
										worth = neu.manager.auctionManager.getItemAvgBin(internal);
										if (worth <= 0) {
											JsonObject auctionInfo = neu.manager.auctionManager.getItemAuctionInfo(internal);
											if (auctionInfo != null) {
												if (auctionInfo.has("clean_price")) {
													worth = (int) auctionInfo.get("clean_price").getAsFloat();
												} else {
													worth = (int) (auctionInfo.get("price").getAsFloat() / auctionInfo.get("count").getAsFloat());
												}
											}
										}
									}
								}
							}

							if (worth > 0 && totalValue >= 0) {
								totalValue += worth;

								String display = item.getDisplayName();

								if (display.contains("Enchanted Book")) {
									NBTTagCompound tag = item.getTagCompound();
									if (tag != null && tag.hasKey("ExtraAttributes", 10)) {
										NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");
										NBTTagCompound enchants = ea.getCompoundTag("enchantments");

										int highestLevel = -1;
										for (String enchname : enchants.getKeySet()) {
											int level = enchants.getInteger(enchname);
											if (level > highestLevel) {
												display = EnumChatFormatting.BLUE + WordUtils.capitalizeFully(enchname
													.replace("_", " ")
													.replace("Ultimate", "")
													.trim()) + " " + level;
											}
										}
									}
								}

								itemValues.put(display, worth);
							} else {
								if (totalValue != -1) {
									missingItem = internal;
								}
								totalValue = -1;
							}
						}
					}

					String valueStringBIN1;
					String valueStringBIN2;
					if (totalValue >= 0) {
						valueStringBIN1 = EnumChatFormatting.YELLOW + "Value (BIN): ";
						valueStringBIN2 = EnumChatFormatting.GOLD + StringUtils.formatNumber(totalValue) + " coins";
					} else {
						valueStringBIN1 = EnumChatFormatting.YELLOW + "Can't find BIN: ";
						valueStringBIN2 = missingItem;
					}

					int profitLossBIN = totalValue - chestCost;
					String profitPrefix = EnumChatFormatting.DARK_GREEN.toString();
					String lossPrefix = EnumChatFormatting.RED.toString();
					String prefix = profitLossBIN >= 0 ? profitPrefix : lossPrefix;

					String plStringBIN;
					if (profitLossBIN >= 0) {
						plStringBIN = prefix + "+" + StringUtils.formatNumber(profitLossBIN) + " coins";
					} else {
						plStringBIN = prefix + "-" + StringUtils.formatNumber(-profitLossBIN) + " coins";
					}

					String neu = EnumChatFormatting.YELLOW + "[NEU] ";

					newTooltip.add(neu + valueStringBIN1 + " " + valueStringBIN2);
					if (totalValue >= 0) {
						newTooltip.add(neu + EnumChatFormatting.YELLOW + "Profit/Loss: " + plStringBIN);
					}

					for (Map.Entry<String, Double> entry : itemValues.entrySet()) {
						newTooltip.add(neu + entry.getKey() + prefix + "+" + StringUtils.formatNumber(entry.getValue().intValue()));
					}
				}
			}
		}

		pressedShiftLast = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
		pressedArrowLast = Keyboard.isKeyDown(Keyboard.KEY_LEFT) || Keyboard.isKeyDown(Keyboard.KEY_RIGHT);

		event.toolTip.clear();
		event.toolTip.addAll(newTooltip);

		if (NotEnoughUpdates.INSTANCE.config.tooltipTweaks.showPriceInfoInvItem) {
			ItemPriceInformation.addToTooltip(event.toolTip, internalName, event.itemStack);
		}

		if (event.itemStack.getTagCompound() != null && event.itemStack.getTagCompound().getBoolean("NEUHIDEPETTOOLTIP") &&
			NotEnoughUpdates.INSTANCE.config.petOverlay.hidePetTooltip) {
			event.toolTip.clear();
		}
	}

	private void petToolTipXPExtendPetMenu(ItemTooltipEvent event) {
		if (!NotEnoughUpdates.INSTANCE.config.tooltipTweaks.petExtendExp) return;
		//7 is just a random number i chose, prob no pets with less lines than 7
		if (event.toolTip.size() < 7) return;
		if (event.itemStack.getTagCompound().hasKey("NEUHIDEPETTOOLTIP")) return;
		if (Utils.cleanColour(event.toolTip.get(1)).matches(petToolTipRegex)) {
			PetLeveling.PetLevel petLevel;

			int xpLine = -1;
			for (int i = event.toolTip.size() - 1; i >= 0; i--) {
				Matcher matcher = xpLevelPattern.matcher(event.toolTip.get(i));
				if (matcher.matches()) {
					xpLine = i;
					event.toolTip.set(xpLine, matcher.group(1));
					break;
				} else if (event.toolTip.get(i).matches("MAX LEVEL")) {
					return;
				}
			}

			PetInfoOverlay.Pet pet = PetInfoOverlay.getPetFromStack(
				event.itemStack.getTagCompound()
			);
			if (pet == null) {
				return;
			}
			petLevel = pet.petLevel;

			if (petLevel == null || xpLine == -1) {
				return;
			}

			event.toolTip.add(
				xpLine + 1,
				EnumChatFormatting.GRAY + "EXP: " + EnumChatFormatting.YELLOW + myFormatter.format(petLevel.getExpInCurrentLevel()) +
					EnumChatFormatting.GOLD + "/" + EnumChatFormatting.YELLOW +
					myFormatter.format(petLevel.getExpRequiredForNextLevel())
			);

		}
	}

	/**
	 * This method does the following:
	 * Remove reforge stats for Legendary items from Hypixel if enabled
	 * Show NBT data when holding LCONTROL
	 */
	@SubscribeEvent
	public void onItemTooltip(ItemTooltipEvent event) {
		if (!neu.isOnSkyblock()) return;
		if (event.toolTip == null) return;

		if (event.toolTip.size() > 2 && NotEnoughUpdates.INSTANCE.config.tooltipTweaks.hideDefaultReforgeStats) {
			String secondLine = StringUtils.cleanColour(event.toolTip.get(1));
			if (secondLine.equals("Reforge Stone")) {
				Integer startIndex = null;
				Integer cutoffIndex = null;
				//loop from the back of the List to find the wanted index sooner
				for (int i = event.toolTip.size() - 1; i >= 0; i--) {
					//rarity or mining level requirement
					String line = StringUtils.cleanColour(event.toolTip.get(i));
					if (line.contains("REFORGE STONE") || line.contains("Requires Mining Skill Level")) {
						cutoffIndex = i;
					}

					//The line where the Hypixel stats start
					if (line.contains("(Legendary):")) {
						startIndex = i;
						break;
					}
				}
				if (startIndex != null && cutoffIndex != null && startIndex < cutoffIndex) {
					event.toolTip.subList(startIndex, cutoffIndex).clear();
				}
			}
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && NotEnoughUpdates.INSTANCE.config.hidden.dev &&
			event.toolTip.size() > 0 && event.toolTip.get(event.toolTip.size() - 1).startsWith(
			EnumChatFormatting.DARK_GRAY + "NBT: ")) {
			event.toolTip.remove(event.toolTip.size() - 1);

			StringBuilder sb = new StringBuilder();
			String nbt = event.itemStack.getTagCompound().toString();
			int indent = 0;
			for (char c : nbt.toCharArray()) {
				boolean newline = false;
				if (c == '{' || c == '[') {
					indent++;
					newline = true;
				} else if (c == '}' || c == ']') {
					indent--;
					sb.append("\n");
					for (int i = 0; i < indent; i++) sb.append("  ");
				} else if (c == ',') {
					newline = true;
				} else if (c == '\"') {
					sb.append(EnumChatFormatting.RESET).append(EnumChatFormatting.GRAY);
				}

				sb.append(c);
				if (newline) {
					sb.append("\n");
					for (int i = 0; i < indent; i++) sb.append("  ");
				}
			}
			event.toolTip.add(sb.toString());
			if (Keyboard.isKeyDown(Keyboard.KEY_H)) {
				if (!copied) {
					copied = true;
					StringSelection selection = new StringSelection(sb.toString().replace("§r§7", ""));
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
				}
			} else {
				copied = false;
			}
		} else if (NotEnoughUpdates.INSTANCE.packDevEnabled) {
			event.toolTip.add("");
			event.toolTip.add(EnumChatFormatting.AQUA + "NEU Pack Dev Info:");
			event.toolTip.add(
				EnumChatFormatting.GRAY + "Press " + EnumChatFormatting.GOLD + "[KEY]" + EnumChatFormatting.GRAY +
					" to copy line");

			String internal = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(event.itemStack);

			boolean k = Keyboard.isKeyDown(Keyboard.KEY_K);
			boolean m = Keyboard.isKeyDown(Keyboard.KEY_M);
			boolean n = Keyboard.isKeyDown(Keyboard.KEY_N);
			boolean f = Keyboard.isKeyDown(Keyboard.KEY_F);
			boolean b = Keyboard.isKeyDown(Keyboard.KEY_B);

			if (!copied && f && NotEnoughUpdates.INSTANCE.config.hidden.dev) {
				MiscUtils.copyToClipboard(NotEnoughUpdates.INSTANCE.manager.getSkullValueForItem(event.itemStack));
			}

			event.toolTip.add(
				EnumChatFormatting.AQUA + "Internal Name: " + EnumChatFormatting.GRAY + internal + EnumChatFormatting.GOLD +
					" [K]");
			if (!copied && k) {
				MiscUtils.copyToClipboard(internal);
			}

			if (event.itemStack.getTagCompound() != null) {
				NBTTagCompound tag = event.itemStack.getTagCompound();

				event.toolTip.add(EnumChatFormatting.AQUA + "NBT: " + EnumChatFormatting.GRAY + "[...]" +
					EnumChatFormatting.GOLD + " [B]");
				if (!copied && b) {
					MiscUtils.copyToClipboard(tag.toString());
				}

				if (tag.hasKey("SkullOwner", 10)) {
					GameProfile gameprofile = NBTUtil.readGameProfileFromNBT(tag.getCompoundTag("SkullOwner"));

					if (gameprofile != null) {
						event.toolTip.add(EnumChatFormatting.AQUA + "Skull UUID: " + EnumChatFormatting.GRAY + gameprofile.getId() +
							EnumChatFormatting.GOLD + " [M]");
						if (!copied && m) {
							MiscUtils.copyToClipboard(gameprofile.getId().toString());
						}

						Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map =
							Minecraft.getMinecraft().getSkinManager().loadSkinFromCache(gameprofile);

						if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
							MinecraftProfileTexture profTex = map.get(MinecraftProfileTexture.Type.SKIN);
							event.toolTip.add(
								EnumChatFormatting.AQUA + "Skull Texture Link: " + EnumChatFormatting.GRAY + profTex.getUrl() +
									EnumChatFormatting.GOLD + " [N]");

							if (!copied && n) {
								MiscUtils.copyToClipboard(profTex.getUrl());
							}
						}
					}
				}
			}

			copied = k || m || n || f || b;
		}
	}
}
