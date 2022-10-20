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

package io.github.moulberry.notenoughupdates.profileviewer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.util.StringUtils;
import io.github.moulberry.notenoughupdates.cosmetics.ShaderManager;
import io.github.moulberry.notenoughupdates.itemeditor.GuiElementTextField;
import io.github.moulberry.notenoughupdates.miscfeatures.PetInfoOverlay;
import io.github.moulberry.notenoughupdates.profileviewer.bestiary.BestiaryPage;
import io.github.moulberry.notenoughupdates.profileviewer.trophy.TrophyFishPage;
import io.github.moulberry.notenoughupdates.util.AsyncDependencyLoader;
import io.github.moulberry.notenoughupdates.util.Constants;
import io.github.moulberry.notenoughupdates.util.PronounDB;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Matrix4f;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GuiProfileViewer extends GuiScreen {

	public static final ResourceLocation pv_dropdown = new ResourceLocation("notenoughupdates:pv_dropdown.png");
	public static final ResourceLocation pv_bg = new ResourceLocation("notenoughupdates:pv_bg.png");
	public static final ResourceLocation pv_elements = new ResourceLocation("notenoughupdates:pv_elements.png");
	public static final ResourceLocation pv_ironman = new ResourceLocation("notenoughupdates:pv_ironman.png");
	public static final ResourceLocation pv_bingo = new ResourceLocation("notenoughupdates:pv_bingo.png");
	public static final ResourceLocation pv_stranded = new ResourceLocation("notenoughupdates:pv_stranded.png");
	public static final ResourceLocation pv_unknown = new ResourceLocation("notenoughupdates:pv_unknown.png");
	public static final ResourceLocation resource_packs =
		new ResourceLocation("minecraft:textures/gui/resource_packs.png");
	public static final ResourceLocation icons = new ResourceLocation("textures/gui/icons.png");
	public static final HashMap<String, HashMap<String, Float>> PET_STAT_BOOSTS =
		new HashMap<String, HashMap<String, Float>>() {
			{
				put(
					"PET_ITEM_BIG_TEETH_COMMON",
					new HashMap<String, Float>() {
						{
							put("CRIT_CHANCE", 5f);
						}
					}
				);
				put(
					"PET_ITEM_HARDENED_SCALES_UNCOMMON",
					new HashMap<String, Float>() {
						{
							put("DEFENCE", 25f);
						}
					}
				);
				put(
					"PET_ITEM_LUCKY_CLOVER",
					new HashMap<String, Float>() {
						{
							put("MAGIC_FIND", 7f);
						}
					}
				);
				put(
					"PET_ITEM_SHARPENED_CLAWS_UNCOMMON",
					new HashMap<String, Float>() {
						{
							put("CRIT_DAMAGE", 15f);
						}
					}
				);
			}
		};
	public static final HashMap<String, HashMap<String, Float>> PET_STAT_BOOSTS_MULT =
		new HashMap<String, HashMap<String, Float>>() {
			{
				put(
					"PET_ITEM_IRON_CLAWS_COMMON",
					new HashMap<String, Float>() {
						{
							put("CRIT_DAMAGE", 1.4f);
							put("CRIT_CHANCE", 1.4f);
						}
					}
				);
				put(
					"PET_ITEM_TEXTBOOK",
					new HashMap<String, Float>() {
						{
							put("INTELLIGENCE", 2f);
						}
					}
				);
			}
		};

	public static final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

	private static final char[] c = new char[]{'k', 'm', 'b', 't'};
	public static ProfileViewerPage currentPage = ProfileViewerPage.BASIC;
	public static HashMap<String, String> MINION_RARITY_TO_NUM = new HashMap<String, String>() {
		{
			put("COMMON", "0");
			put("UNCOMMON", "1");
			put("RARE", "2");
			put("EPIC", "3");
			put("LEGENDARY", "4");
			put("MYTHIC", "5");
		}
	};
	private static int guiLeft;
	private static int guiTop;
	private static ProfileViewer.Profile profile;
	private static String profileId = null;
	public static AsyncDependencyLoader<Optional<PronounDB.PronounChoice>> pronouns =
		AsyncDependencyLoader.withEqualsInvocation(
			() ->
				NotEnoughUpdates.INSTANCE.config.profileViewer.showPronounsInPv
					? Optional.ofNullable(profile).map(it -> Utils.parseDashlessUUID(it.getUuid()))
					: Optional.<UUID>empty(),
			uuid -> uuid.isPresent()
				? PronounDB.getPronounsFor(uuid.get())
				: CompletableFuture.completedFuture(Optional.empty())
		);
	public final GuiElementTextField playerNameTextField;
	public final GuiElementTextField inventoryTextField = new GuiElementTextField("", GuiElementTextField.SCALE_TEXT);
	private final Map<ProfileViewerPage, GuiProfileViewerPage> pages = new HashMap<>();
	public int sizeX;
	public int sizeY;
	public float backgroundRotation = 0;
	public long currentTime = 0;
	public long lastTime = 0;
	public long startTime = 0;
	public List<String> tooltipToDisplay = null;
	Shader blurShaderHorz = null;
	Framebuffer blurOutputHorz = null;
	Shader blurShaderVert = null;
	Framebuffer blurOutputVert = null;
	private boolean profileDropdownSelected = false;

	private double lastBgBlurFactor = -1;
	private boolean showBingoPage;

	public GuiProfileViewer(ProfileViewer.Profile profile) {
		GuiProfileViewer.profile = profile;
		GuiProfileViewer.profileId = profile.getLatestProfile();
		String name = "";
		if (profile.getHypixelProfile() != null) {
			name = profile.getHypixelProfile().get("displayname").getAsString();
		}
		playerNameTextField = new GuiElementTextField(name, GuiElementTextField.SCALE_TEXT);
		playerNameTextField.setSize(100, 20);

		if (currentPage == ProfileViewerPage.LOADING) {
			currentPage = ProfileViewerPage.BASIC;
		}

		pages.put(ProfileViewerPage.BASIC, new BasicPage(this));
		pages.put(ProfileViewerPage.DUNGEON, new DungeonPage(this));
		pages.put(ProfileViewerPage.EXTRA, new ExtraPage(this));
		pages.put(ProfileViewerPage.INVENTORIES, new InventoriesPage(this));
		pages.put(ProfileViewerPage.COLLECTIONS, new CollectionsPage(this));
		pages.put(ProfileViewerPage.PETS, new PetsPage(this));
		pages.put(ProfileViewerPage.MINING, new MiningPage(this));
		pages.put(ProfileViewerPage.BINGO, new BingoPage(this));
		pages.put(ProfileViewerPage.TROPHY_FISH, new TrophyFishPage(this));
		pages.put(ProfileViewerPage.BESTIARY, new BestiaryPage(this));
	}

	private static float getMaxLevelXp(JsonArray levels, int offset, int maxLevel) {
		float xpTotal = 0;

		for (int i = offset; i < offset + maxLevel - 1; i++) {
			xpTotal += levels.get(i).getAsFloat();
		}

		return xpTotal;
	}

	public static PetLevel getPetLevel(
		String petType,
		String rarity,
		float exp
	) {
		int offset = PetInfoOverlay.Rarity.valueOf(rarity).petOffset;
		int maxLevel = 100;

		JsonArray levels = new JsonArray();
		levels.addAll(Constants.PETS.get("pet_levels").getAsJsonArray());
		JsonElement customLevelingJson = Constants.PETS.get("custom_pet_leveling").getAsJsonObject().get(petType);
		if (customLevelingJson != null) {
			switch (Utils.getElementAsInt(Utils.getElement(customLevelingJson, "type"), 0)) {
				case 1:
					levels.addAll(customLevelingJson.getAsJsonObject().get("pet_levels").getAsJsonArray());
					break;
				case 2:
					levels = customLevelingJson.getAsJsonObject().get("pet_levels").getAsJsonArray();
					break;
			}
			maxLevel = Utils.getElementAsInt(Utils.getElement(customLevelingJson, "max_level"), 100);
		}

		float maxXP = getMaxLevelXp(levels, offset, maxLevel);
		boolean isMaxed = exp >= maxXP;

		int level = 1;
		float currentLevelRequirement = 0;
		float xpThisLevel = 0;
		float pct = 0;

		if (isMaxed) {
			level = maxLevel;
			currentLevelRequirement = levels.get(offset + level - 2).getAsFloat();
			xpThisLevel = currentLevelRequirement;
			pct = 1;
		} else {
			long totalExp = 0;
			for (int i = offset; i < levels.size(); i++) {
				currentLevelRequirement = levels.get(i).getAsLong();
				totalExp += currentLevelRequirement;
				if (totalExp >= exp) {
					xpThisLevel = currentLevelRequirement - (totalExp - exp);
					level = Math.min(i - offset + 1, maxLevel);
					break;
				}
			}
			pct = currentLevelRequirement != 0 ? xpThisLevel / currentLevelRequirement : 0;
			level += pct;
		}

		GuiProfileViewer.PetLevel levelObj = new GuiProfileViewer.PetLevel();
		levelObj.level = level;
		levelObj.maxLevel = maxLevel;
		levelObj.currentLevelRequirement = currentLevelRequirement;
		levelObj.maxXP = maxXP;
		levelObj.levelPercentage = pct;
		levelObj.levelXp = xpThisLevel;
		levelObj.totalXp = exp;
		return levelObj;
	}

	@Deprecated
	public static String shortNumberFormat(double n, int iteration) {
		return StringUtils.shortNumberFormat(n, iteration
		);
	}

	public static int getGuiLeft() {
		return guiLeft;
	}

	public static int getGuiTop() {
		return guiTop;
	}

	public static ProfileViewer.Profile getProfile() {
		return profile;
	}

	public static String getProfileId() {
		return profileId;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		currentTime = System.currentTimeMillis();
		if (startTime == 0) startTime = currentTime;

		ProfileViewerPage page = currentPage;
		if (profile == null) {
			page = ProfileViewerPage.INVALID_NAME;
		} else if (profile.getSkyblockProfiles(null) == null) {
			page = ProfileViewerPage.LOADING;
		} else if (profile.getLatestProfile() == null) {
			page = ProfileViewerPage.NO_SKYBLOCK;
		}

		if (profileId == null && profile != null && profile.getLatestProfile() != null) {
			profileId = profile.getLatestProfile();
		}
		{
			//this is just to cache the guild info
			if (profile != null) {
				profile.getGuildInformation(null);
			}
		}

		this.sizeX = 431;
		this.sizeY = 202;
		guiLeft = (this.width - this.sizeX) / 2;
		guiTop = (this.height - this.sizeY) / 2;

		JsonObject currProfileInfo = profile != null ? profile.getProfileInformation(profileId) : null;
		if (NotEnoughUpdates.INSTANCE.config.profileViewer.alwaysShowBingoTab) {
			showBingoPage = true;
		} else {
			showBingoPage =
				currProfileInfo != null &&
					currProfileInfo.has("game_mode") &&
					currProfileInfo.get("game_mode").getAsString().equals("bingo");
		}

		if (!showBingoPage && currentPage == ProfileViewerPage.BINGO) currentPage = ProfileViewerPage.BASIC;

		super.drawScreen(mouseX, mouseY, partialTicks);
		drawDefaultBackground();

		blurBackground();
		renderBlurredBackground(width, height, guiLeft + 2, guiTop + 2, sizeX - 4, sizeY - 4);

		GlStateManager.enableDepth();
		GlStateManager.translate(0, 0, 5);
		renderTabs(true);
		GlStateManager.translate(0, 0, -3);

		GlStateManager.disableDepth();
		GlStateManager.translate(0, 0, -2);
		renderTabs(false);
		GlStateManager.translate(0, 0, 2);

		GlStateManager.disableLighting();
		GlStateManager.enableDepth();
		GlStateManager.enableBlend();
		GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.enableAlpha();
		GlStateManager.alphaFunc(516, 0.1F);

		Minecraft.getMinecraft().getTextureManager().bindTexture(pv_bg);
		Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);

		if (!(page == ProfileViewerPage.LOADING)) {
			playerNameTextField.render(guiLeft + sizeX - 100, guiTop + sizeY + 5);
			ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());

			if (profile != null) {
				//Render Profile chooser button
				renderBlurredBackground(width, height, guiLeft + 2, guiTop + sizeY + 3 + 2, 100 - 4, 20 - 4);
				Minecraft.getMinecraft().getTextureManager().bindTexture(pv_dropdown);
				Utils.drawTexturedRect(guiLeft, guiTop + sizeY + 3, 100, 20, 0, 100 / 200f, 0, 20 / 185f, GL11.GL_NEAREST);
				Utils.drawStringCenteredScaledMaxWidth(
					profileId,
					Minecraft.getMinecraft().fontRendererObj,
					guiLeft + 50,
					guiTop + sizeY + 3 + 10,
					true,
					90,
					new Color(63, 224, 208, 255).getRGB()
				);
				//ironman icon
				if (
					currProfileInfo != null &&
						currProfileInfo.has("game_mode") &&
						currProfileInfo.get("game_mode").getAsString().equals("ironman")
				) {
					GlStateManager.color(1, 1, 1, 1);
					Minecraft.getMinecraft().getTextureManager().bindTexture(pv_ironman);
					Utils.drawTexturedRect(guiLeft - 16 - 5, guiTop + sizeY + 5, 16, 16, GL11.GL_NEAREST);
				}
				//bingo! icon
				if (
					currProfileInfo != null &&
						currProfileInfo.has("game_mode") &&
						currProfileInfo.get("game_mode").getAsString().equals("bingo")
				) {
					GlStateManager.color(1, 1, 1, 1);
					Minecraft.getMinecraft().getTextureManager().bindTexture(pv_bingo);
					Utils.drawTexturedRect(guiLeft - 16 - 5, guiTop + sizeY + 5, 16, 16, GL11.GL_NEAREST);
				}
				//stranded icon
				if (
					currProfileInfo != null &&
						currProfileInfo.has("game_mode") &&
						currProfileInfo.get("game_mode").getAsString().equals("island")
				) {
					GlStateManager.color(1, 1, 1, 1);
					Minecraft.getMinecraft().getTextureManager().bindTexture(pv_stranded);
					Utils.drawTexturedRect(guiLeft - 16 - 5, guiTop + sizeY + 5, 16, 16, GL11.GL_NEAREST);
				}
				//icon if game mode is unknown
				if (
					currProfileInfo != null &&
						currProfileInfo.has("game_mode") &&
						!currProfileInfo.get("game_mode").getAsString().equals("island") &&
						!currProfileInfo.get("game_mode").getAsString().equals("bingo") &&
						!currProfileInfo.get("game_mode").getAsString().equals("ironman")
				) {
					GlStateManager.color(1, 1, 1, 1);
					Minecraft.getMinecraft().getTextureManager().bindTexture(pv_unknown);
					Utils.drawTexturedRect(guiLeft - 16 - 5, guiTop + sizeY + 5, 16, 16, GL11.GL_NEAREST);
				}
				//Render Open In Skycrypt button
				renderBlurredBackground(width, height, guiLeft + 100 + 6 + 2, guiTop + sizeY + 3 + 2, 100 - 4, 20 - 4);
				Minecraft.getMinecraft().getTextureManager().bindTexture(pv_dropdown);
				Utils.drawTexturedRect(
					guiLeft + 100 + 6,
					guiTop + sizeY + 3,
					100,
					20,
					0,
					100 / 200f,
					0,
					20 / 185f,
					GL11.GL_NEAREST
				);
				Utils.drawStringCenteredScaledMaxWidth(
					"Open in Skycrypt",
					Minecraft.getMinecraft().fontRendererObj,
					guiLeft + 50 + 100 + 6,
					guiTop + sizeY + 3 + 10,
					true,
					90,
					new Color(63, 224, 208, 255).getRGB()
				);

				if (profileDropdownSelected && !profile.getProfileNames().isEmpty() && scaledResolution.getScaleFactor() < 4) {
					int dropdownOptionSize = scaledResolution.getScaleFactor() == 3 ? 10 : 20;

					int numProfiles = profile.getProfileNames().size();
					int sizeYDropdown = numProfiles * dropdownOptionSize;
					renderBlurredBackground(width, height, guiLeft + 2, guiTop + sizeY + 23, 100 - 4, sizeYDropdown - 2);
					Minecraft.getMinecraft().getTextureManager().bindTexture(pv_dropdown);
					Utils.drawTexturedRect(guiLeft, guiTop + sizeY + 23 - 3, 100, 3, 100 / 200f, 1, 0, 3 / 185f, GL11.GL_NEAREST);
					Utils.drawTexturedRect(
						guiLeft,
						guiTop + sizeY + 23 + sizeYDropdown - 4,
						100,
						4,
						100 / 200f,
						1,
						181 / 185f,
						1,
						GL11.GL_NEAREST
					);
					Utils.drawTexturedRect(
						guiLeft,
						guiTop + sizeY + 23,
						100,
						sizeYDropdown - 4,
						100 / 200f,
						1,
						(181 - sizeYDropdown) / 185f,
						181 / 185f,
						GL11.GL_NEAREST
					);

					for (int yIndex = 0; yIndex < profile.getProfileNames().size(); yIndex++) {
						String otherProfileId = profile.getProfileNames().get(yIndex);
						Utils.drawStringCenteredScaledMaxWidth(
							otherProfileId,
							Minecraft.getMinecraft().fontRendererObj,
							guiLeft + 50,
							guiTop + sizeY + 23 + dropdownOptionSize / 2f + dropdownOptionSize * yIndex,
							true,
							90,
							new Color(33, 112, 104, 255).getRGB()
						);
						currProfileInfo = profile.getProfileInformation(otherProfileId);
						if (
							currProfileInfo != null &&
								currProfileInfo.has("game_mode") &&
								currProfileInfo.get("game_mode").getAsString().equals("ironman")
						) {
							GlStateManager.color(1, 1, 1, 1);
							Minecraft.getMinecraft().getTextureManager().bindTexture(pv_ironman);
							Utils.drawTexturedRect(
								guiLeft - 16 - 5,
								guiTop + sizeY + 2 + 23 + dropdownOptionSize * yIndex,
								16,
								16,
								GL11.GL_NEAREST
							);
						}
						if (
							currProfileInfo != null &&
								currProfileInfo.has("game_mode") &&
								currProfileInfo.get("game_mode").getAsString().equals("bingo")
						) {
							GlStateManager.color(1, 1, 1, 1);
							Minecraft.getMinecraft().getTextureManager().bindTexture(pv_bingo);
							Utils.drawTexturedRect(
								guiLeft - 16 - 5,
								guiTop + sizeY + 2 + 23 + dropdownOptionSize * yIndex,
								16,
								16,
								GL11.GL_NEAREST
							);
						}
						if (
							currProfileInfo != null &&
								currProfileInfo.has("game_mode") &&
								currProfileInfo.get("game_mode").getAsString().equals("island")
						) {
							GlStateManager.color(1, 1, 1, 1);
							Minecraft.getMinecraft().getTextureManager().bindTexture(pv_stranded);
							Utils.drawTexturedRect(
								guiLeft - 16 - 5,
								guiTop + sizeY + 2 + 23 + dropdownOptionSize * yIndex,
								16,
								16,
								GL11.GL_NEAREST
							);
						}
						if (
							currProfileInfo != null &&
								currProfileInfo.has("game_mode") &&
								!currProfileInfo.get("game_mode").getAsString().equals("island") &&
								!currProfileInfo.get("game_mode").getAsString().equals("bingo") &&
								!currProfileInfo.get("game_mode").getAsString().equals("ironman")
						) {
							GlStateManager.color(1, 1, 1, 1);
							Minecraft.getMinecraft().getTextureManager().bindTexture(pv_unknown);
							Utils.drawTexturedRect(
								guiLeft - 16 - 5,
								guiTop + sizeY + 2 + 23 + dropdownOptionSize * yIndex,
								16,
								16,
								GL11.GL_NEAREST
							);
						}
					}
				}
			}
		}

		GlStateManager.color(1, 1, 1, 1);

		if (pages.containsKey(page)) {
			pages.get(page).drawPage(mouseX, mouseY, partialTicks);
		} else {
			switch (page) {
				case LOADING:
					String str = EnumChatFormatting.YELLOW + "Loading player profiles.";
					long currentTimeMod = System.currentTimeMillis() % 1000;
					if (currentTimeMod > 333) {
						if (currentTimeMod < 666) {
							str += ".";
						} else {
							str += "..";
						}
					}

					Utils.drawStringCentered(
						str,
						Minecraft.getMinecraft().fontRendererObj,
						guiLeft + sizeX / 2f,
						guiTop + 101,
						true,
						0
					);

					//This is just here to inform the player what to do
					//like typing /api new or telling them to go find a psychotherapist
					long timeDiff = System.currentTimeMillis() - startTime;

					if (timeDiff > 20000) {
						Utils.drawStringCentered(
							EnumChatFormatting.YELLOW + "Its taking a while...",
							Minecraft.getMinecraft().fontRendererObj,
							guiLeft + sizeX / 2f,
							guiTop + 111,
							true,
							0
						);
						Utils.drawStringCentered(
							EnumChatFormatting.YELLOW + "Try \"/api new\".",
							Minecraft.getMinecraft().fontRendererObj,
							guiLeft + sizeX / 2f,
							guiTop + 121,
							true,
							0
						);
						if (timeDiff > 60000) {
							Utils.drawStringCentered(
								EnumChatFormatting.YELLOW + "Might be hypixel's fault.",
								Minecraft.getMinecraft().fontRendererObj,
								guiLeft + sizeX / 2f,
								guiTop + 131,
								true,
								0
							);
							if (timeDiff > 180000) {
								Utils.drawStringCentered(
									EnumChatFormatting.YELLOW + "Wow you're still here?",
									Minecraft.getMinecraft().fontRendererObj,
									guiLeft + sizeX / 2f,
									guiTop + 141,
									true,
									0
								);
								if (timeDiff > 360000) {
									long second = (timeDiff / 1000) % 60;
									long minute = (timeDiff / (1000 * 60)) % 60;
									long hour = (timeDiff / (1000 * 60 * 60)) % 24;

									String time = String.format("%02d:%02d:%02d", hour, minute, second);
									Utils.drawStringCentered(
										EnumChatFormatting.YELLOW + "You've wasted your time here for: " + time,
										Minecraft.getMinecraft().fontRendererObj,
										guiLeft + sizeX / 2f,
										guiTop + 151,
										true,
										0
									);
									Utils.drawStringCentered(
										EnumChatFormatting.YELLOW + "" + EnumChatFormatting.BOLD + "What are you doing with your life?",
										Minecraft.getMinecraft().fontRendererObj,
										guiLeft + sizeX / 2f,
										guiTop + 161,
										true,
										0
									);
									if (timeDiff > 600000) {
										Utils.drawStringCentered(
											EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "Maniac",
											Minecraft.getMinecraft().fontRendererObj,
											guiLeft + sizeX / 2f,
											guiTop + 171,
											true,
											0
										);
										if (timeDiff > 1200000) {
											Utils.drawStringCentered(
												EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "You're a menace to society",
												Minecraft.getMinecraft().fontRendererObj,
												guiLeft + sizeX / 2f,
												guiTop + 181,
												true,
												0
											);
											if (timeDiff > 1800000) {
												Utils.drawStringCentered(
													EnumChatFormatting.RED +
														"" +
														EnumChatFormatting.BOLD +
														"You don't know what's gonna happen to you",
													Minecraft.getMinecraft().fontRendererObj,
													guiLeft + sizeX / 2f,
													guiTop + 191,
													true,
													0
												);
												if (timeDiff > 3000000) {
													Utils.drawStringCentered(
														EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "You really want this?",
														Minecraft.getMinecraft().fontRendererObj,
														guiLeft + sizeX / 2f,
														guiTop + 91,
														true,
														0
													);
													if (timeDiff > 3300000) {
														Utils.drawStringCentered(
															EnumChatFormatting.DARK_RED +
																"" +
																EnumChatFormatting.BOLD +
																"OW LORD FORGIVE ME FOR THIS",
															Minecraft.getMinecraft().fontRendererObj,
															guiLeft + sizeX / 2f,
															guiTop + 71,
															true,
															0
														);
														if (timeDiff > 3600000) {
															throw new Error("Go do something productive") {
																@Override
																public void printStackTrace() {
																	throw new Error("Go do something productive");
																}
															};
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}

					break;
				case INVALID_NAME:
					Utils.drawStringCentered(
						EnumChatFormatting.RED + "Invalid name or API is down!",
						Minecraft.getMinecraft().fontRendererObj,
						guiLeft + sizeX / 2f,
						guiTop + 101,
						true,
						0
					);
					break;
				case NO_SKYBLOCK:
					Utils.drawStringCentered(
						EnumChatFormatting.RED + "No skyblock data found!",
						Minecraft.getMinecraft().fontRendererObj,
						guiLeft + sizeX / 2f,
						guiTop + 101,
						true,
						0
					);
					break;
			}
		}

		lastTime = currentTime;

		if (currentPage != ProfileViewerPage.LOADING && currentPage != ProfileViewerPage.INVALID_NAME) {
			int ignoredTabs = 0;
			List<Integer> configList = NotEnoughUpdates.INSTANCE.config.profileViewer.pageLayout;
			for (int i = 0; i < configList.size(); i++) {
				ProfileViewerPage iPage = ProfileViewerPage.getById(configList.get(i));
				if (iPage == null) continue;
				if (iPage.stack == null || (iPage == ProfileViewerPage.BINGO && !showBingoPage)) {
					ignoredTabs++;
					continue;
				}
				int i2 = i - ignoredTabs;
				int x = guiLeft + i2 * 28;
				int y = guiTop - 28;

				if (mouseX > x && mouseX < x + 28) {
					if (mouseY > y && mouseY < y + 32) {
						if (!iPage.stack
							.getTooltip(Minecraft.getMinecraft().thePlayer, false)
							.isEmpty()) {
							tooltipToDisplay = Collections.singletonList(iPage.stack
								.getTooltip(Minecraft.getMinecraft().thePlayer, false)
								.get(0));
						}
					}
				}
			}
		}

		if (tooltipToDisplay != null) {
			List<String> grayTooltip = new ArrayList<>(tooltipToDisplay.size());
			for (String line : tooltipToDisplay) {
				grayTooltip.add(EnumChatFormatting.GRAY + line);
			}
			Utils.drawHoveringText(grayTooltip, mouseX, mouseY, width, height, -1, Minecraft.getMinecraft().fontRendererObj);
			tooltipToDisplay = null;
		}
	}

	private void renderTabs(boolean renderPressed) {
		int ignoredTabs = 0;
		List<Integer> configList = NotEnoughUpdates.INSTANCE.config.profileViewer.pageLayout;
		for (int i = 0; i < configList.size(); i++) {
			ProfileViewerPage page = ProfileViewerPage.getById(configList.get(i));
			if (page == null) continue;
			if (page.stack == null || (page == ProfileViewerPage.BINGO && !showBingoPage)) {
				ignoredTabs++;
				continue;
			}
			boolean pressed = page == currentPage;
			if (pressed == renderPressed) {
				renderTab(page.stack, i - ignoredTabs, pressed);
			}
		}
	}

	private void renderTab(ItemStack stack, int xIndex, boolean pressed) {
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.enableAlpha();
		GlStateManager.alphaFunc(516, 0.1F);

		int x = guiLeft + xIndex * 28;
		int y = guiTop - 28;

		float uMin = 0;
		float uMax = 28 / 256f;
		float vMin = 20 / 256f;
		float vMax = 51 / 256f;
		if (pressed) {
			vMin = 52 / 256f;
			vMax = 84 / 256f;

			if (xIndex != 0) {
				uMin = 28 / 256f;
				uMax = 56 / 256f;
			}

			renderBlurredBackground(width, height, x + 2, y + 2, 28 - 4, 28 - 4);
		} else {
			renderBlurredBackground(width, height, x + 2, y + 4, 28 - 4, 28 - 4);
		}

		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.enableAlpha();
		GlStateManager.alphaFunc(516, 0.1F);

		Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
		Utils.drawTexturedRect(x, y, 28, pressed ? 32 : 31, uMin, uMax, vMin, vMax, GL11.GL_NEAREST);

		GlStateManager.enableDepth();
		Utils.drawItemStack(stack, x + 6, y + 9);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (currentPage != ProfileViewerPage.LOADING && currentPage != ProfileViewerPage.INVALID_NAME) {
			int ignoredTabs = 0;
			List<Integer> configList = NotEnoughUpdates.INSTANCE.config.profileViewer.pageLayout;
			for (int i = 0; i < configList.size(); i++) {
				ProfileViewerPage page = ProfileViewerPage.getById(configList.get(i));
				if (page == null) continue;
				if (page.stack == null || (page == ProfileViewerPage.BINGO && !showBingoPage)) {
					ignoredTabs++;
					continue;
				}
				int i2 = i - ignoredTabs;
				int x = guiLeft + i2 * 28;
				int y = guiTop - 28;

				if (mouseX > x && mouseX < x + 28) {
					if (mouseY > y && mouseY < y + 32) {
						if (currentPage != page) Utils.playPressSound();
						currentPage = page;
						inventoryTextField.otherComponentClick();
						playerNameTextField.otherComponentClick();
						return;
					}
				}
			}
		}

		if (pages.containsKey(currentPage)) {
			if (pages.get(currentPage).mouseClicked(mouseX, mouseY, mouseButton)) {
				return;
			}
		}

		if (mouseX > guiLeft + sizeX - 100 && mouseX < guiLeft + sizeX) {
			if (mouseY > guiTop + sizeY + 5 && mouseY < guiTop + sizeY + 25) {
				playerNameTextField.mouseClicked(mouseX, mouseY, mouseButton);
				inventoryTextField.otherComponentClick();
				return;
			}
		}
		if (
			mouseX > guiLeft + 106 &&
				mouseX < guiLeft + 106 + 100 &&
				profile != null &&
				!profile.getProfileNames().isEmpty() &&
				profileId != null
		) {
			if (mouseY > guiTop + sizeY + 3 && mouseY < guiTop + sizeY + 23) {
				try {
					Desktop desk = Desktop.getDesktop();
					desk.browse(
						new URI(
							"https://sky.shiiyu.moe/stats/" + profile.getHypixelProfile().get("displayname").getAsString() + "/" +
								profileId
						)
					);
					Utils.playPressSound();
					return;
				} catch (UnsupportedOperationException | IOException | URISyntaxException ignored) {
					//no idea how this sounds, but ya know just in case
					Utils.playSound(new ResourceLocation("game.player.hurt"), true);
					return;
				}
			}
		}

		if (mouseX > guiLeft && mouseX < guiLeft + 100 && profile != null && !profile.getProfileNames().isEmpty()) {
			ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
			if (mouseY > guiTop + sizeY + 3 && mouseY < guiTop + sizeY + 23) {
				if (scaledResolution.getScaleFactor() >= 4) {
					profileDropdownSelected = false;
					int profileNum = 0;
					for (int index = 0; index < profile.getProfileNames().size(); index++) {
						if (profile.getProfileNames().get(index).equals(profileId)) {
							profileNum = index;
							break;
						}
					}
					if (mouseButton == 0) {
						profileNum++;
					} else {
						profileNum--;
					}
					if (profileNum >= profile.getProfileNames().size()) profileNum = 0;
					if (profileNum < 0) profileNum = profile.getProfileNames().size() - 1;

					String newProfileId = profile.getProfileNames().get(profileNum);
					if (profileId != null && !profileId.equals(newProfileId)) {
						resetCache();
					}
					profileId = newProfileId;
				} else {
					profileDropdownSelected = !profileDropdownSelected;
				}
			} else if (scaledResolution.getScaleFactor() < 4 && profileDropdownSelected) {
				int dropdownOptionSize = scaledResolution.getScaleFactor() == 3 ? 10 : 20;
				int extraY = mouseY - (guiTop + sizeY + 23);
				int index = extraY / dropdownOptionSize;
				if (index >= 0 && index < profile.getProfileNames().size()) {
					String newProfileId = profile.getProfileNames().get(index);
					if (profileId != null && !profileId.equals(newProfileId)) {
						resetCache();
					}
					profileId = newProfileId;
				}
			}
			playerNameTextField.otherComponentClick();
			inventoryTextField.otherComponentClick();
			return;
		}
		profileDropdownSelected = false;
		playerNameTextField.otherComponentClick();
		inventoryTextField.otherComponentClick();
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		super.keyTyped(typedChar, keyCode);

		if (pages.containsKey(currentPage)) {
			pages.get(currentPage).keyTyped(typedChar, keyCode);
		}

		if (playerNameTextField.getFocus()) {
			if (keyCode == Keyboard.KEY_RETURN) {
				currentPage = ProfileViewerPage.LOADING;
				NotEnoughUpdates.profileViewer.getProfileByName(
					playerNameTextField.getText(),
					profile -> { //todo: invalid name
						if (profile != null) profile.resetCache();
						Minecraft.getMinecraft().displayGuiScreen(new GuiProfileViewer(profile));
					}
				);
			}
			playerNameTextField.keyTyped(typedChar, keyCode);
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
		super.mouseReleased(mouseX, mouseY, mouseButton);

		if (pages.containsKey(currentPage)) {
			pages.get(currentPage).mouseReleased(mouseX, mouseY, mouseButton);
		}
	}

	public void renderXpBar(
		String skillName,
		ItemStack stack,
		int x,
		int y,
		int xSize,
		ProfileViewer.Level levelObj,
		int mouseX,
		int mouseY
	) {
		float level = levelObj.level;
		int levelFloored = (int) Math.floor(level);

		Utils.renderAlignedString(skillName, EnumChatFormatting.WHITE.toString() + levelFloored, x + 14, y - 4, xSize - 20);

		if (levelObj.maxed) {
			renderGoldBar(x, y + 6, xSize);
		} else {
			if (skillName.contains("Catacombs") && levelObj.level >= 50) {
				renderGoldBar(x, y + 6, xSize);
			} else {
				renderBar(x, y + 6, xSize, level % 1);
			}
		}

		if (mouseX > x && mouseX < x + 120) {
			if (mouseY > y - 4 && mouseY < y + 13) {
				String levelStr;
				String totalXpStr = null;
				if (skillName.contains("Catacombs")) totalXpStr =
					EnumChatFormatting.GRAY + "Total XP: " + EnumChatFormatting.DARK_PURPLE +
						numberFormat.format(levelObj.totalXp);
				if (levelObj.maxed) {
					levelStr = EnumChatFormatting.GOLD + "MAXED!";
				} else {
					int maxXp = (int) levelObj.maxXpForLevel;
					levelStr =
						EnumChatFormatting.DARK_PURPLE +
							StringUtils.shortNumberFormat(Math.round((level % 1) * maxXp)) +
							"/" +
							StringUtils.shortNumberFormat(maxXp);
				}
				if (totalXpStr != null) {
					tooltipToDisplay = Utils.createList(levelStr, totalXpStr);
				} else {
					tooltipToDisplay = Utils.createList(levelStr);
				}
			}
		}

		NBTTagCompound nbt = new NBTTagCompound(); //Adding NBT Data for Custom Resource Packs
		NBTTagCompound display = new NBTTagCompound();
		display.setString("Name", skillName);
		nbt.setTag("display", display);
		stack.setTagCompound(nbt);

		GL11.glTranslatef((x), (y - 6f), 0);
		GL11.glScalef(0.7f, 0.7f, 1);
		Utils.drawItemStackLinear(stack, 0, 0);
		GL11.glScalef(1 / 0.7f, 1 / 0.7f, 1);
		GL11.glTranslatef(-(x), -(y - 6f), 0);
	}

	public EntityOtherPlayerMP getEntityPlayer() {
		return ((BasicPage) pages.get(ProfileViewerPage.BASIC)).entityPlayer;
	}

	public void renderGoldBar(float x, float y, float xSize) {
		if (!OpenGlHelper.areShadersSupported()) {
			renderBar(x, y, xSize, 1);
			return;
		}

		Minecraft.getMinecraft().getTextureManager().bindTexture(icons);
		ShaderManager shaderManager = ShaderManager.getInstance();
		shaderManager.loadShader("make_gold");
		shaderManager.loadData("make_gold", "amount", (startTime - System.currentTimeMillis()) / 10000f);

		Utils.drawTexturedRect(x, y, xSize / 2f, 5, 0 / 256f, (xSize / 2f) / 256f, 79 / 256f, 84 / 256f, GL11.GL_NEAREST);
		Utils.drawTexturedRect(
			x + xSize / 2f,
			y,
			xSize / 2f,
			5,
			(182 - xSize / 2f) / 256f,
			182 / 256f,
			79 / 256f,
			84 / 256f,
			GL11.GL_NEAREST
		);

		GL20.glUseProgram(0);
	}

	public void renderBar(float x, float y, float xSize, float completed) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(icons);

		completed = Math.round(completed / 0.05f) * 0.05f;
		float notCompleted = 1 - completed;
		GlStateManager.color(1, 1, 1, 1);
		float width;

		if (completed < 0.5f) {
			width = (0.5f - completed) * xSize;
			Utils.drawTexturedRect(
				x + xSize * completed,
				y,
				width,
				5,
				xSize * completed / 256f,
				(xSize / 2f) / 256f,
				74 / 256f,
				79 / 256f,
				GL11.GL_NEAREST
			);
		}
		if (completed < 1f) {
			width = Math.min(xSize * notCompleted, xSize / 2f);
			Utils.drawTexturedRect(
				x + (xSize / 2f) + Math.max(xSize * (completed - 0.5f), 0),
				y,
				width,
				5,
				(182 - (xSize / 2f) + Math.max(xSize * (completed - 0.5f), 0)) / 256f,
				182 / 256f,
				74 / 256f,
				79 / 256f,
				GL11.GL_NEAREST
			);
		}

		if (completed > 0f) {
			width = Math.min(xSize * completed, xSize / 2f);
			Utils.drawTexturedRect(x, y, width, 5, 0 / 256f, width / 256f, 79 / 256f, 84 / 256f, GL11.GL_NEAREST);
		}
		if (completed > 0.5f) {
			width = Math.min(xSize * (completed - 0.5f), xSize / 2f);
			Utils.drawTexturedRect(
				x + (xSize / 2f),
				y,
				width,
				5,
				(182 - (xSize / 2f)) / 256f,
				(182 - (xSize / 2f) + width) / 256f,
				79 / 256f,
				84 / 256f,
				GL11.GL_NEAREST
			);
		}
	}

	public void resetCache() {
		pages.values().forEach(GuiProfileViewerPage::resetCache);
	}

	/**
	 * Creates a projection matrix that projects from our coordinate space [0->width; 0->height] to OpenGL coordinate
	 * space [-1 -> 1; 1 -> -1] (Note: flipped y-axis).
	 * <p>
	 * This is so that we can render to and from the framebuffer in a way that is familiar to us, instead of needing to
	 * apply scales and translations manually.
	 */
	private Matrix4f createProjectionMatrix(int width, int height) {
		Matrix4f projMatrix = new Matrix4f();
		projMatrix.setIdentity();
		projMatrix.m00 = 2.0F / (float) width;
		projMatrix.m11 = 2.0F / (float) (-height);
		projMatrix.m22 = -0.0020001999F;
		projMatrix.m33 = 1.0F;
		projMatrix.m03 = -1.0F;
		projMatrix.m13 = 1.0F;
		projMatrix.m23 = -1.0001999F;
		return projMatrix;
	}

	private void blurBackground() {
		if (!OpenGlHelper.isFramebufferEnabled()) return;

		int width = Minecraft.getMinecraft().displayWidth;
		int height = Minecraft.getMinecraft().displayHeight;

		if (blurOutputHorz == null) {
			blurOutputHorz = new Framebuffer(width, height, false);
			blurOutputHorz.setFramebufferFilter(GL11.GL_NEAREST);
		}
		if (blurOutputVert == null) {
			blurOutputVert = new Framebuffer(width, height, false);
			blurOutputVert.setFramebufferFilter(GL11.GL_NEAREST);
		}
		if (blurOutputHorz.framebufferWidth != width || blurOutputHorz.framebufferHeight != height) {
			blurOutputHorz.createBindFramebuffer(width, height);
			blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
			Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
		}
		if (blurOutputVert.framebufferWidth != width || blurOutputVert.framebufferHeight != height) {
			blurOutputVert.createBindFramebuffer(width, height);
			blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
			Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
		}

		if (blurShaderHorz == null) {
			try {
				blurShaderHorz =
					new Shader(
						Minecraft.getMinecraft().getResourceManager(),
						"blur",
						Minecraft.getMinecraft().getFramebuffer(),
						blurOutputHorz
					);
				blurShaderHorz.getShaderManager().getShaderUniform("BlurDir").set(1, 0);
				blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
			} catch (Exception ignored) {
			}
		}
		if (blurShaderVert == null) {
			try {
				blurShaderVert = new Shader(
					Minecraft.getMinecraft().getResourceManager(),
					"blur",
					blurOutputHorz,
					blurOutputVert
				);
				blurShaderVert.getShaderManager().getShaderUniform("BlurDir").set(0, 1);
				blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
			} catch (Exception ignored) {
			}
		}
		if (blurShaderHorz != null && blurShaderVert != null) {
			if (15 != lastBgBlurFactor) {
				blurShaderHorz.getShaderManager().getShaderUniform("Radius").set((float) 15);
				blurShaderVert.getShaderManager().getShaderUniform("Radius").set((float) 15);
				lastBgBlurFactor = 15;
			}
			GL11.glPushMatrix();
			blurShaderHorz.loadShader(0);
			blurShaderVert.loadShader(0);
			GlStateManager.enableDepth();
			GL11.glPopMatrix();

			Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
		}
	}

	/**
	 * Renders a subsection of the blurred framebuffer on to the corresponding section of the screen.
	 * Essentially, this method will "blur" the background inside the bounds specified by [x->x+blurWidth, y->y+blurHeight]
	 */
	public void renderBlurredBackground(int width, int height, int x, int y, int blurWidth, int blurHeight) {
		if (!OpenGlHelper.isFramebufferEnabled()) return;

		float uMin = x / (float) width;
		float uMax = (x + blurWidth) / (float) width;
		float vMin = (height - y) / (float) height;
		float vMax = (height - y - blurHeight) / (float) height;

		blurOutputVert.bindFramebufferTexture();
		GlStateManager.color(1f, 1f, 1f, 1f);
		//Utils.setScreen(width*f, height*f, f);
		Utils.drawTexturedRect(x, y, blurWidth, blurHeight, uMin, uMax, vMin, vMax);
		//Utils.setScreen(width, height, f);
		blurOutputVert.unbindFramebufferTexture();
	}

	public enum ProfileViewerPage {
		LOADING(),
		INVALID_NAME(),
		NO_SKYBLOCK(),
		BASIC(0, Items.paper, "§9Skills"),
		DUNGEON(1, Item.getItemFromBlock(Blocks.deadbush), "§eDungeoneering"),
		EXTRA(2, Items.book, "§7Profile Stats"),
		INVENTORIES(3, Item.getItemFromBlock(Blocks.ender_chest), "§bStorage"),
		COLLECTIONS(4, Items.painting, "§6Collections"),
		PETS(5, Items.bone, "§aPets"),
		MINING(6, Items.iron_pickaxe, "§5Heart of the Mountain"),
		BINGO(7, Items.filled_map, "§zBingo"),
		TROPHY_FISH(8, Items.fishing_rod, "§3Trophy Fish"),
		BESTIARY(9, Items.iron_sword, "§cBestiary");

		public final ItemStack stack;
		public final int id;

		ProfileViewerPage() {
			this(-1, null, null);
		}

		ProfileViewerPage(int id, Item item, String name) {
			this.id = id;
			if (item == null) {
				stack = null;
			} else {
				stack = new ItemStack(item);
				NBTTagCompound nbt = new NBTTagCompound(); //Adding NBT Data for Custom Resource Packs
				NBTTagCompound display = new NBTTagCompound();
				display.setString("Name", name);
				nbt.setTag("display", display);
				stack.setTagCompound(nbt);
			}
		}

		public static ProfileViewerPage getById(int id) {
			for (ProfileViewerPage page : values()) {
				if (page.id == id) {
					return page;
				}
			}
			return null;
		}

		public Optional<ItemStack> getItem() {
			return Optional.ofNullable(stack);
		}
	}

	public static class PetLevel {

		public float level;
		public float maxLevel;
		public float currentLevelRequirement;
		public float maxXP;
		public float levelPercentage;
		public float levelXp;
		public float totalXp;
	}
}
