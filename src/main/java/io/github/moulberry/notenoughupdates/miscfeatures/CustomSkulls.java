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

package io.github.moulberry.notenoughupdates.miscfeatures;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelHumanoidHead;
import net.minecraft.client.model.ModelSkeletonHead;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.IIconCreator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class CustomSkulls implements IResourceManagerReloadListener {
	private static final CustomSkulls INSTANCE = new CustomSkulls();

	public static CustomSkulls getInstance() {
		return INSTANCE;
	}

	private final ResourceLocation atlas = new ResourceLocation("notenoughupdates", "custom_skull_textures_atlas");
	private final ResourceLocation configuration = new ResourceLocation(
		"notenoughupdates", "custom_skull_textures/customskull.json");
	private final ResourceLocation configuration2 = new ResourceLocation(
		"notenoughupdates", "custom_skull_textures/customskull2.json");
	protected final TextureMap textureMap = new TextureMap("custom_skull_textures");

	public static ItemCameraTransforms.TransformType mostRecentTransformType = ItemCameraTransforms.TransformType.NONE;

	protected final Map<ResourceLocation, TextureAtlasSprite> sprites = Maps.newHashMap();

	private final FaceBakery faceBakery = new FaceBakery();
	private final ModelSkeletonHead humanoidHead = new ModelHumanoidHead();

	private final HashMap<String, CustomSkull> customSkulls = new HashMap<>();
	private final HashMap<String, CustomSkull> customSkulls2 = new HashMap<>();

	private final Gson gson = new GsonBuilder().create();

	private static class CustomSkull {
		private ModelBlock model;
		private IBakedModel modelBaked;

		private ResourceLocation texture;
	}

	private void processConfig(ResourceLocation config, Map<String, CustomSkull> map, Map<String, CustomSkull> cache) {
		map.clear();

		try (
			BufferedReader reader = new BufferedReader(new InputStreamReader(
				Minecraft.getMinecraft().getResourceManager().getResource(config).getInputStream(),
				StandardCharsets.UTF_8
			))
		) {
			JsonObject json = gson.fromJson(reader, JsonObject.class);

			if (json == null) return;

			for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
				if (entry.getValue().isJsonObject()) {
					JsonObject obj = entry.getValue().getAsJsonObject();
					CustomSkull skull;
					if (obj.has("model")) {
						ResourceLocation loc = new ResourceLocation(
							"notenoughupdates",
							"custom_skull_textures/" + obj.get("model").getAsString() + ".json"
						);


						if ((skull = cache.get(loc.toString())) == null) {
							skull = new CustomSkull();
							skull.model = ModelBlock.deserialize(new InputStreamReader(Minecraft
								.getMinecraft()
								.getResourceManager()
								.getResource(loc)
								.getInputStream()));

							cache.put(loc.toString(), skull);
						}

						map.put(entry.getKey(), skull);
					} else if (obj.has("texture")) {
						String location = obj.get("texture").getAsString();
						ResourceLocation loc = new ResourceLocation(
							"notenoughupdates",
							"custom_skull_textures/" + location + ".png"
						);

						if ((skull = cache.get(loc.toString())) == null) {
							skull = new CustomSkull();
							skull.texture = loc;
							Minecraft.getMinecraft().getTextureManager().deleteTexture(skull.texture);

							cache.put(loc.toString(), skull);
						}

						map.put(entry.getKey(), skull);
					}
				}
			}
		} catch (Exception ignored) {}
	}

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
		Map<String, CustomSkull> skullCache = new HashMap<>();
		processConfig(configuration, customSkulls, skullCache);
		processConfig(configuration2, customSkulls2, skullCache);

		if (customSkulls.isEmpty() && customSkulls2.isEmpty()) return;

		try {
			loadSprites(skullCache.values());

			skullCache.values().forEach(
				(CustomSkull skull) -> {
					if (skull.model != null) {
						skull.modelBaked = bakeModel(skull.model, ModelRotation.X0_Y0, false);
					}
				}
			);

			Minecraft.getMinecraft().getTextureManager().loadTexture(atlas, textureMap);
		} catch (Exception ignored) {}
	}

	private void loadSprites(Collection<CustomSkull> models) {
		final Set<ResourceLocation> set = this.getAllTextureLocations(models);
		set.remove(TextureMap.LOCATION_MISSING_TEXTURE);
		IIconCreator iiconcreator = iconRegistry -> {
			for (ResourceLocation resourcelocation : set) {
				TextureAtlasSprite textureatlassprite = iconRegistry.registerSprite(resourcelocation);
				CustomSkulls.this.sprites.put(resourcelocation, textureatlassprite);
			}
		};
		this.textureMap.loadSprites(Minecraft.getMinecraft().getResourceManager(), iiconcreator);
		this.sprites.put(new ResourceLocation("missingno"), this.textureMap.getMissingSprite());
	}

	protected Set<ResourceLocation> getAllTextureLocations(Collection<CustomSkull> models) {
		Set<ResourceLocation> set = new HashSet<>();

		for (CustomSkull skull : models) {
			if (skull.model != null) {
				set.addAll(getTextureLocations(skull.model));
			}
		}

		return set;
	}

	protected Set<ResourceLocation> getTextureLocations(ModelBlock modelBlock) {
		Set<ResourceLocation> set = Sets.newHashSet();

		for (BlockPart blockpart : modelBlock.getElements()) {
			for (BlockPartFace blockpartface : blockpart.mapFaces.values()) {
				ResourceLocation resourcelocation = new ResourceLocation(
					"notenoughupdates",
					modelBlock.resolveTextureName(blockpartface.texture)
				);
				set.add(resourcelocation);
			}
		}

		set.add(new ResourceLocation("notenoughupdates", modelBlock.resolveTextureName("particle")));
		return set;
	}

	protected IBakedModel bakeModel(
		ModelBlock modelBlockIn,
		net.minecraftforge.client.model.ITransformation modelRotationIn,
		boolean uvLocked
	) {
		TextureAtlasSprite textureatlassprite = this.sprites.get(new ResourceLocation(
			"notenoughupdates",
			modelBlockIn.resolveTextureName("particle")
		));
		SimpleBakedModel.Builder simplebakedmodel$builder = (new SimpleBakedModel.Builder(modelBlockIn)).setTexture(
			textureatlassprite);

		for (BlockPart blockpart : modelBlockIn.getElements()) {
			for (EnumFacing enumfacing : blockpart.mapFaces.keySet()) {
				BlockPartFace blockpartface = blockpart.mapFaces.get(enumfacing);
				TextureAtlasSprite textureatlassprite1 = this.sprites.get(new ResourceLocation(
					"notenoughupdates",
					modelBlockIn.resolveTextureName(blockpartface.texture)
				));

				if (blockpartface.cullFace == null || !net.minecraftforge.client.model.TRSRTransformation.isInteger(
					modelRotationIn.getMatrix())) {
					simplebakedmodel$builder.addGeneralQuad(this.makeBakedQuad(
						blockpart,
						blockpartface,
						textureatlassprite1,
						enumfacing,
						modelRotationIn,
						uvLocked
					));
				} else {
					simplebakedmodel$builder.addFaceQuad(
						modelRotationIn.rotate(blockpartface.cullFace),
						this.makeBakedQuad(blockpart, blockpartface, textureatlassprite1, enumfacing, modelRotationIn, uvLocked)
					);
				}
			}
		}

		return simplebakedmodel$builder.makeBakedModel();
	}

	private BakedQuad makeBakedQuad(
		BlockPart p_177589_1_,
		BlockPartFace p_177589_2_,
		TextureAtlasSprite p_177589_3_,
		EnumFacing p_177589_4_,
		ModelRotation p_177589_5_,
		boolean p_177589_6_
	) {
		return makeBakedQuad(
			p_177589_1_,
			p_177589_2_,
			p_177589_3_,
			p_177589_4_,
			(net.minecraftforge.client.model.ITransformation) p_177589_5_,
			p_177589_6_
		);
	}

	protected BakedQuad makeBakedQuad(
		BlockPart p_177589_1_,
		BlockPartFace p_177589_2_,
		TextureAtlasSprite p_177589_3_,
		EnumFacing p_177589_4_,
		net.minecraftforge.client.model.ITransformation p_177589_5_,
		boolean p_177589_6_
	) {
		return this.faceBakery.makeBakedQuad(
			p_177589_1_.positionFrom,
			p_177589_1_.positionTo,
			p_177589_2_,
			p_177589_3_,
			p_177589_4_,
			p_177589_5_,
			p_177589_1_.partRotation,
			p_177589_6_,
			p_177589_1_.shade
		);
	}

	private void renderModel(IBakedModel model, int color) {
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();
		worldrenderer.begin(7, DefaultVertexFormats.ITEM);

		for (EnumFacing enumfacing : EnumFacing.values()) {
			this.renderQuads(worldrenderer, model.getFaceQuads(enumfacing), color);
		}

		this.renderQuads(worldrenderer, model.getGeneralQuads(), color);
		tessellator.draw();
	}

	private void renderQuads(WorldRenderer renderer, List<BakedQuad> quads, int color) {
		for (BakedQuad quad : quads) {
			net.minecraftforge.client.model.pipeline.LightUtil.renderQuadColor(renderer, quad, color);
		}
	}

	public boolean renderSkull(
		float xOffset, float yOffset, float zOffset, EnumFacing placedDirection,
		float rotationDeg, int skullType, GameProfile skullOwner, int damage
	) {
		if (NotEnoughUpdates.INSTANCE.config.misc.disableSkullRetexturing) {
			return false;
		}
		if (skullType != 3) {
			return false;
		}
		if (skullOwner == null || skullOwner.getId() == null) {
			return false;
		}

		CustomSkull skull = customSkulls.get(skullOwner.getId().toString());
		if (skull == null) {
			try {
				skull = customSkulls2.get(skullOwner.getProperties().get("textures").iterator().next().getValue());
				if (skull == null) return false;
			} catch (NoSuchElementException e) {
				return false;
			}
		}

		if (skull.modelBaked != null && skull.model != null) {
			Minecraft.getMinecraft().getTextureManager().bindTexture(atlas);
			GlStateManager.pushMatrix();
			GlStateManager.disableCull();
			GlStateManager.enableLighting();

			final float rot;
			switch (placedDirection) {
				case NORTH: {
					GlStateManager.translate(xOffset + 0.5f, yOffset + 0.25f, zOffset + 0.74f);
					rot = 0f;
					break;
				}
				case SOUTH: {
					GlStateManager.translate(xOffset + 0.5f, yOffset + 0.25f, zOffset + 0.26f);
					rot = 180.0f;
					break;
				}
				case WEST: {
					GlStateManager.translate(xOffset + 0.74f, yOffset + 0.25f, zOffset + 0.5f);
					rot = 270.0f;
					break;
				}
				case EAST: {
					GlStateManager.translate(xOffset + 0.26f, yOffset + 0.25f, zOffset + 0.5f);
					rot = 90.0f;
					break;
				}
				default: {
					GlStateManager.translate(xOffset + 0.5f, yOffset, zOffset + 0.5f);
					rot = rotationDeg;
				}
			}

			GlStateManager.enableRescaleNormal();
			GlStateManager.enableAlpha();

			GlStateManager.rotate(rot, 0, 1, 0);

			GlStateManager.translate(0, 0.25f, 0);

			if (placedDirection == EnumFacing.UP && xOffset == -0.5 && yOffset == 0 && zOffset == -0.5 && rotationDeg == 180) {
				skull.model.getAllTransforms().applyTransform(ItemCameraTransforms.TransformType.HEAD);
			} else {
				skull.model.getAllTransforms().applyTransform(mostRecentTransformType);
			}

			GlStateManager.translate(-0.5f, 0, -0.5f);

			renderModel(skull.modelBaked, 0xffffffff);
			GlStateManager.popMatrix();
		} else if (skull.texture != null) {
			if (Minecraft.getMinecraft().getTextureManager().getTexture(skull.texture) == null) {
				try {
					BufferedImage image = ImageIO.read(Minecraft
						.getMinecraft()
						.getResourceManager()
						.getResource(skull.texture)
						.getInputStream());
					int size = Math.max(image.getHeight(), image.getWidth());

					Minecraft.getMinecraft().getTextureManager().loadTexture(skull.texture, new AbstractTexture() {
						@Override
						public void loadTexture(IResourceManager resourceManager) {
							TextureUtil.allocateTexture(this.getGlTextureId(), size, size);

							int[] rgb = new int[size * size];

							image.getRGB(0, 0, image.getWidth(), image.getHeight(), rgb, 0, image.getWidth());

							TextureUtil.uploadTexture(this.getGlTextureId(), rgb, size, size);
						}
					});
				} catch (IOException ignored) {
				}
			}

			Minecraft.getMinecraft().getTextureManager().bindTexture(skull.texture);

			GlStateManager.pushMatrix();
			GlStateManager.disableCull();

			GlStateManager.translate(xOffset + 0.5F, yOffset, zOffset + 0.5F);

			GlStateManager.enableRescaleNormal();
			GlStateManager.scale(-1.0F, -1.0F, 1.0F);
			GlStateManager.enableAlpha();
			humanoidHead.render(null, 0.0F, 0.0F, 0.0F, rotationDeg, 0.0F, 0.0625F);
			GlStateManager.popMatrix();
		} else {
			return false;
		}

		return true;
	}
}
