package io.github.moulberry.notenoughupdates;

import io.github.moulberry.notenoughupdates.questing.SBInfo;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class DungeonBlocks implements IResourceManagerReloadListener {

    //public static Framebuffer framebuffer = null;
    private static int textureId = -1;
    private static IntBuffer intbuffer = null;
    private static HashMap<String, Integer> modified = new HashMap<>();

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        reset();
    }

    public static boolean textureExists() {
        return textureId != -1 && isInDungeons();
    }

    public static void bindTextureIfExists() {
        if(textureExists()) {
            GlStateManager.bindTexture(textureId);
        }
    }

    public static boolean isInDungeons() {
        return !NotEnoughUpdates.INSTANCE.manager.config.disableDungeonBlocks.value &&
                (NotEnoughUpdates.INSTANCE.manager.config.dungeonBlocksEverywhere.value ||
                (SBInfo.getInstance().getLocation() != null && SBInfo.getInstance().getLocation().equals("dungeon")));
    }

    public static void reset() {
        textureId = -1;
        for(int tex : modified.values()) {
            GlStateManager.deleteTexture(tex);
        }
        modified.clear();
    }

    public static int getModifiedTexture(ResourceLocation location, int colour) {
        if(!isInDungeons()) {
            return -1;
        }

        if(((colour >> 24) & 0xFF) < 50) {
            return -1;
        }

        String id = location.getResourceDomain()+":"+location.getResourcePath();
        if(modified.containsKey(id)) {
            return modified.get(id);
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(location);
        int mipmapLevels = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL);
        int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

        if(intbuffer == null || intbuffer.capacity() < w*h) intbuffer = BufferUtils.createIntBuffer(w*h);

        int textureId = TextureUtil.glGenTextures();
        GlStateManager.bindTexture(textureId);

        if (mipmapLevels >= 0) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, mipmapLevels);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0.0F);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, (float)mipmapLevels);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0F);
        }

        for (int i = 0; i <= mipmapLevels; ++i) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, i, GL11.GL_RGBA, w >> i, h >> i, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer)((IntBuffer)null));
        }

        GlStateManager.bindTexture(textureId);

        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        for (int level = 0; level <= mipmapLevels; level++) {
            int w2 = w >> level;
            int h2 = h >> level;

            Minecraft.getMinecraft().getTextureManager().bindTexture(location);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, level, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, intbuffer);

            for(int x=0; x<w2; x++) {
                for(int y=0; y<h2; y++) {
                    int index = x+y*w2;

                    int newCol = colour;
                    float newAlpha = ((newCol >> 24) & 0xFF)/255f;
                    float newRed = ((newCol >> 16) & 0xFF)/255f;
                    float newGreen = ((newCol >> 8) & 0xFF)/255f;
                    float newBlue = (newCol & 0xFF)/255f;

                    int oldCol = intbuffer.get(index);
                    int oldAlpha = (oldCol >> 24) & 0xFF;
                    float oldRed = ((oldCol >> 16) & 0xFF)/255f;
                    float oldGreen = ((oldCol >> 8) & 0xFF)/255f;
                    float oldBlue = (oldCol & 0xFF)/255f;

                    int r = (int)((newRed*newAlpha + oldRed*(1-newAlpha))*255);
                    int g = (int)((newGreen*newAlpha + oldGreen*(1-newAlpha))*255);
                    int b = (int)((newBlue*newAlpha + oldBlue*(1-newAlpha))*255);

                    intbuffer.put(index, oldAlpha << 24 | r << 16 | g << 8 | b);
                }
            }

            GlStateManager.bindTexture(textureId);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, level, GL11.GL_RGBA, w2, h2,
                    0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, intbuffer);
        }

        modified.put(id, textureId);
        return textureId;
    }

    public static void tick() {
        if(!isInDungeons()) {
            return;
        }

        if(textureId == -1) {
            int locationBlocksId = Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.locationBlocksTexture).getGlTextureId();

            GlStateManager.bindTexture(locationBlocksId);
            int mipmapLevels = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL);
            int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

            if(intbuffer == null || intbuffer.capacity() < w*h) intbuffer = BufferUtils.createIntBuffer(w*h);

            if(textureId == -1) {
                textureId = TextureUtil.glGenTextures();
                GlStateManager.bindTexture(textureId);

                if (mipmapLevels >= 0) {
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, mipmapLevels);
                    GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0.0F);
                    GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, (float)mipmapLevels);
                    GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0F);
                }

                for (int i = 0; i <= mipmapLevels; ++i) {
                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, i, GL11.GL_RGBA, w >> i, h >> i, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer)((IntBuffer)null));
                }
            }
            GlStateManager.bindTexture(textureId);

            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

            HashMap<TextureAtlasSprite, Integer> spriteMap = new HashMap<>();
            spriteMap.put(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/stonebrick_cracked"),
                    SpecialColour.specialToSimpleRGB(NotEnoughUpdates.INSTANCE.manager.config.dungCrackedColour.value));
            spriteMap.put(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/dispenser_front_horizontal"),
                    SpecialColour.specialToSimpleRGB(NotEnoughUpdates.INSTANCE.manager.config.dungDispenserColour.value));
            spriteMap.put(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/lever"),
                    SpecialColour.specialToSimpleRGB(NotEnoughUpdates.INSTANCE.manager.config.dungLeverColour.value));
            spriteMap.put(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/trip_wire"),
                    SpecialColour.specialToSimpleRGB(NotEnoughUpdates.INSTANCE.manager.config.dungTripWireColour.value));

            for (int level = 0; level <= mipmapLevels; level++) {
                int w2 = w >> level;
                int h2 = h >> level;

                GlStateManager.bindTexture(locationBlocksId);
                GL11.glGetTexImage(GL11.GL_TEXTURE_2D, level, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, intbuffer);

                for(Map.Entry<TextureAtlasSprite, Integer> entry : spriteMap.entrySet()) {
                    if(((entry.getValue() >> 24) & 0xFF) < 50) continue;

                    TextureAtlasSprite tas = entry.getKey();
                    for(int x=(int)(w2*tas.getMinU()); x<w2*tas.getMaxU(); x++) {
                        for(int y=(int)(h2*tas.getMinV()); y<h2*tas.getMaxV(); y++) {
                            int index = x+y*w2;

                            int newCol = entry.getValue();
                            /*float newAlpha = ((newCol >> 24) & 0xFF)/255f;
                            float newRed = ((newCol >> 16) & 0xFF)/255f;
                            float newGreen = ((newCol >> 8) & 0xFF)/255f;
                            float newBlue = (newCol & 0xFF)/255f;*/

                            /*int oldCol = intbuffer.get(index);
                            int oldAlpha = (oldCol >> 24) & 0xFF;
                            float oldRed = ((oldCol >> 16) & 0xFF)/255f;
                            float oldGreen = ((oldCol >> 8) & 0xFF)/255f;
                            float oldBlue = (oldCol & 0xFF)/255f;

                            int r = (int)((newRed*newAlpha + oldRed*(1-newAlpha))*255);
                            int g = (int)((newGreen*newAlpha + oldGreen*(1-newAlpha))*255);
                            int b = (int)((newBlue*newAlpha + oldBlue*(1-newAlpha))*255);*/

                            intbuffer.put(index, newCol);
                        }
                    }
                }

                GlStateManager.bindTexture(textureId);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, level, GL11.GL_RGBA, w2, h2,
                        0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, intbuffer);
            }
        }
        /*if(framebuffer == null || true) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

            framebuffer = checkFramebufferSizes(framebuffer, w, h);

            try {
                int locationBlocksId = Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.locationBlocksTexture).getGlTextureId();

                //framebuffer2.bindFramebufferTexture();
                //GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL11.GL_RGBA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, ((ByteBuffer)null));

                //textureId = GlStateManager.generateTexture();
                //GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, ((ByteBuffer)null));

                GL11.glPushMatrix();

                GlStateManager.matrixMode(5889);
                GlStateManager.loadIdentity();
                GlStateManager.ortho(0.0D, w, h, 0.0D, 1000.0D, 3000.0D);
                GlStateManager.matrixMode(5888);
                GlStateManager.loadIdentity();
                GlStateManager.translate(0.0F, 0.0F, -2000.0F);

                framebuffer.bindFramebufferTexture();
                if (Minecraft.getMinecraft().gameSettings.mipmapLevels >= 0) {
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, Minecraft.getMinecraft().gameSettings.mipmapLevels);
                    GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0.0F);
                    GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, (float)Minecraft.getMinecraft().gameSettings.mipmapLevels);
                    GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0F);
                }

                for (int i = 0; i <= Minecraft.getMinecraft().gameSettings.mipmapLevels; ++i) {
                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, i, GL11.GL_RGBA, w >> i, h >> i,
                            0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, ((IntBuffer)null));
                }

                //framebuffer.framebufferClear();
                framebuffer.bindFramebuffer(true);
                GlStateManager.clearColor(1, 1, 1, 0);
                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
                GL11.glClearColor(1, 1, 1, 0);

                Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
                GlStateManager.color(1, 1, 1, 1);
                Utils.drawTexturedRect(0, 0, w, h, 0, 1, 1, 0, GL11.GL_LINEAR);

                framebuffer.bindFramebufferTexture();
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, Minecraft.getMinecraft().gameSettings.mipmapLevels);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0.0F);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, (float)Minecraft.getMinecraft().gameSettings.mipmapLevels);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0F);
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

                ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
                GlStateManager.matrixMode(5889);
                GlStateManager.loadIdentity();
                GlStateManager.ortho(0.0D, scaledResolution.getScaledWidth_double(), scaledResolution.getScaledHeight_double(),
                        0.0D, 1000.0D, 3000.0D);
                GlStateManager.matrixMode(5888);
                GlStateManager.loadIdentity();
                GlStateManager.translate(0.0F, 0.0F, -2000.0F);

                GL11.glPopMatrix();

                Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);


                /*framebuffer.bindFramebufferTexture();
                if(Keyboard.isKeyDown(Keyboard.KEY_B)) Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
                Utils.drawTexturedRect(0, 0, w, h, GL11.GL_NEAREST);*/

                /*GlStateManager.bindTexture(textureId);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GlStateManager.enableBlend();
                GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

                //GlStateManager.enableTexture2D();
                //GlStateManager.enableBlend();
                //GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);


                //GlStateManager.disableBlend();

                Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }*/
    }

    private static Framebuffer checkFramebufferSizes(Framebuffer framebuffer, int width, int height) {
        if(framebuffer == null || framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
            if(framebuffer == null) {
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
