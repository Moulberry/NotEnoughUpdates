package io.github.moulberry.notenoughupdates.dungeons;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class DungeonBlocks {

    private static Framebuffer framebufferBlocksTo = null;
    private static Framebuffer framebufferBlocksFrom = null;

    private static HashMap<String, Framebuffer> framebuffersDynamicTo = new HashMap<>();
    public static HashMap<String, Framebuffer> framebuffersDynamicFrom = new HashMap<>();
    private static HashSet<String> dynamicUpdated = new HashSet<>();

    private static FloatBuffer projectionMatrixOld = BufferUtils.createFloatBuffer(16);
    private static FloatBuffer modelviewMatrixOld = BufferUtils.createFloatBuffer(16);

    public static boolean textureExists() {
        return framebufferBlocksFrom != null && isOverriding();
    }

    public static void bindTextureIfExists() {
        if (textureExists()) {
            framebufferBlocksFrom.bindFramebufferTexture();
        }
    }

    public static boolean isOverriding() {
        return OpenGlHelper.isFramebufferEnabled() && NotEnoughUpdates.INSTANCE.config.dungeons.enableDungBlockOverlay &&
                (NotEnoughUpdates.INSTANCE.config.dungeons.dungeonBlocksEverywhere ||
                        (SBInfo.getInstance().getLocation() != null && SBInfo.getInstance().getLocation().equals("dungeon")));
    }

    public static boolean bindModifiedTexture(ResourceLocation location, int colour) {
        if (!isOverriding()) {
            return false;
        }

        if (Utils.disableCustomDungColours) {
            return false;
        }

        if (((colour >> 24) & 0xFF) < 10) {
            return false;
        }

        String id = location.getResourceDomain() + ":" + location.getResourcePath();
        if (dynamicUpdated.contains(id) && framebuffersDynamicFrom.containsKey(id)) {
            framebuffersDynamicFrom.get(id).bindFramebufferTexture();
            return true;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(location);
        int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

        Framebuffer to = checkFramebufferSizes(framebuffersDynamicTo.get(id), w, h);
        dynamicUpdated.add(id);

        try {
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionMatrixOld);
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelviewMatrixOld);

            GL11.glPushMatrix();

            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0D, w, h, 0.0D, 1000.0D, 3000.0D);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();
            GlStateManager.translate(0.0F, 0.0F, -2000.0F);

            to.bindFramebuffer(true);
            GlStateManager.clearColor(0, 1, 0, 1);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);

            GlStateManager.disableBlend();
            GlStateManager.disableLighting();
            GlStateManager.disableFog();

            Minecraft.getMinecraft().getTextureManager().bindTexture(location);
            GlStateManager.color(1, 1, 1, 1);
            Utils.drawTexturedRectNoBlend(0, 0, w, h, 0, 1, 1, 0, GL11.GL_LINEAR);

            GlStateManager.enableBlend();
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            Utils.drawRectNoBlend(0, 0, w, h, colour);

            GL11.glPopMatrix();

            to.bindFramebufferTexture();
            if (Minecraft.getMinecraft().gameSettings.mipmapLevels >= 0) {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, Minecraft.getMinecraft().gameSettings.mipmapLevels);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0.0F);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, (float) Minecraft.getMinecraft().gameSettings.mipmapLevels);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0F);
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            }

            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GL11.glLoadMatrix(projectionMatrixOld);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadMatrix(modelviewMatrixOld);

            Framebuffer from = checkFramebufferSizes(framebuffersDynamicFrom.get(id), w, h);
            framebuffersDynamicFrom.put(id, to);
            framebuffersDynamicTo.put(id, from);

            to.bindFramebufferTexture();

            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        return false;
    }

    private static HashMap<ResourceLocation, String> dynamicPreloadMap = new HashMap<>();

    static {
        dynamicPreloadMap.put(new ResourceLocation("textures/entity/bat.png"),
                NotEnoughUpdates.INSTANCE.config.dungeons.dungBatColour);
        dynamicPreloadMap.put(new ResourceLocation("textures/entity/chest/normal.png"),
                NotEnoughUpdates.INSTANCE.config.dungeons.dungChestColour);
        dynamicPreloadMap.put(new ResourceLocation("textures/entity/chest/normal_double.png"),
                NotEnoughUpdates.INSTANCE.config.dungeons.dungChestColour);
        dynamicPreloadMap.put(new ResourceLocation("textures/entity/chest/trapped.png"),
                NotEnoughUpdates.INSTANCE.config.dungeons.dungTrappedChestColour);
        dynamicPreloadMap.put(new ResourceLocation("textures/entity/chest/trapped_double.png"),
                NotEnoughUpdates.INSTANCE.config.dungeons.dungTrappedChestColour);
    }

    public static void tick() {
        if (!isOverriding() || Minecraft.getMinecraft().theWorld == null) {
            return;
        }

        dynamicUpdated.clear();

        for (Map.Entry<ResourceLocation, String> entry : dynamicPreloadMap.entrySet()) {
            bindModifiedTexture(entry.getKey(), SpecialColour.specialToChromaRGB(entry.getValue()));
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

        Framebuffer to = checkFramebufferSizes(framebufferBlocksTo, w, h);

        try {
            GL11.glPushMatrix();

            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0D, w, h, 0.0D, 1000.0D, 3000.0D);
            GlStateManager.matrixMode(5888);
            GlStateManager.loadIdentity();
            GlStateManager.translate(0.0F, 0.0F, -2000.0F);

            to.bindFramebuffer(true);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);

            GlStateManager.disableBlend();
            GlStateManager.disableLighting();
            GlStateManager.disableFog();

            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            GlStateManager.color(1, 1, 1, 1);
            Utils.drawTexturedRectNoBlend(0, 0, w, h, 0, 1, 1, 0, GL11.GL_LINEAR);

            HashMap<TextureAtlasSprite, Integer> spriteMap = new HashMap<>();
            spriteMap.put(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/stonebrick_cracked"),
                    SpecialColour.specialToChromaRGB(NotEnoughUpdates.INSTANCE.config.dungeons.dungCrackedColour));
            spriteMap.put(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/dispenser_front_horizontal"),
                    SpecialColour.specialToChromaRGB(NotEnoughUpdates.INSTANCE.config.dungeons.dungDispenserColour));
            spriteMap.put(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/lever"),
                    SpecialColour.specialToChromaRGB(NotEnoughUpdates.INSTANCE.config.dungeons.dungLeverColour));
            spriteMap.put(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/trip_wire"),
                    SpecialColour.specialToChromaRGB(NotEnoughUpdates.INSTANCE.config.dungeons.dungTripWireColour));

            for (Map.Entry<TextureAtlasSprite, Integer> entry : spriteMap.entrySet()) {
                if (((entry.getValue() >> 24) & 0xFF) < 10) continue;

                TextureAtlasSprite tas = entry.getKey();
                Gui.drawRect((int) (w * tas.getMinU()), h - (int) (h * tas.getMaxV()) - 1,
                        (int) (w * tas.getMaxU()) + 1, h - (int) (h * tas.getMinV()), entry.getValue());
            }

            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0D, scaledResolution.getScaledWidth_double(), scaledResolution.getScaledHeight_double(),
                    0.0D, 1000.0D, 3000.0D);
            GlStateManager.matrixMode(5888);
            GlStateManager.loadIdentity();
            GlStateManager.translate(0.0F, 0.0F, -2000.0F);

            GL11.glPopMatrix();

            to.bindFramebufferTexture();
            if (Minecraft.getMinecraft().gameSettings.mipmapLevels >= 0) {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, Minecraft.getMinecraft().gameSettings.mipmapLevels);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0.0F);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, (float) Minecraft.getMinecraft().gameSettings.mipmapLevels);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0F);
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            }

            Framebuffer from = checkFramebufferSizes(framebufferBlocksFrom, w, h);
            framebufferBlocksFrom = to;
            framebufferBlocksTo = from;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
        GlStateManager.enableBlend();
    }

    private static Framebuffer checkFramebufferSizes(Framebuffer framebuffer, int width, int height) {
        if (framebuffer == null || framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
            if (framebuffer == null) {
                framebuffer = new Framebuffer(width, height, false);
                framebuffer.framebufferColor[0] = 1f;
                framebuffer.framebufferColor[1] = 0f;
                framebuffer.framebufferColor[2] = 0f;
                framebuffer.framebufferColor[3] = 0;
            } else {
                framebuffer.createBindFramebuffer(width, height);
            }
            framebuffer.setFramebufferFilter(GL11.GL_NEAREST);
        }
        return framebuffer;
    }
}
