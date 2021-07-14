package io.github.moulberry.notenoughupdates.miscfeatures;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.ChromaColour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.function.Consumer;

public class ItemCustomizeManager {

    public static class ReloadListener implements IResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(IResourceManager resourceManager) {
            ItemCustomizeManager.loadedCustomGlintTexture = false;
        }
    }

    public static boolean disableTextureBinding = false;

    private static ResourceLocation CUSTOM_GLINT_TEXTURE = new ResourceLocation("notenoughupdates:dynamic/custom_glint_texture");
    private static boolean loadedCustomGlintTexture = false;

    public static final String DEFAULT_GLINT_COLOR = ChromaColour.special(0, 0xcc, 0x6419FF); //A050FF 0x8040cc 100,25,255 64,19

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ItemDataMap itemDataMap = new ItemDataMap();
    private static HashMap<Integer, String> itemUuidCache = new HashMap<>();

    public static class ItemDataMap {
        public HashMap<String, ItemData> itemData = new HashMap<>();
    }

    public static class ItemData {
        public String customName = null;
        public String customNamePrefix = "";
        public boolean overrideEnchantGlint = false;
        public boolean enchantGlintValue;

        public String customGlintColour = DEFAULT_GLINT_COLOR;

        public String customLeatherColour = null;
    }

    public static void putItemData(String uuid, ItemData data) {
        itemDataMap.itemData.put(uuid, data);
    }

    public static void setCustomBlendFunc(String colour) {

        /*int argb = ChromaColour.specialToChromaRGB(colour);
        float[] hsv = Color.RGBtoHSB((argb >> 16) & 0xff, (argb >> 8) & 0xff, argb & 0xff, null);
        GL14.glBlendColor(1, 1, 1, hsv[2]);*/

        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        //GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
    }

    private static void renderEffect(Consumer<Integer> renderModelCallback, int color) {
        GL11.glPushMatrix();

        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(GL11.GL_EQUAL);
        GlStateManager.disableLighting();
        Minecraft.getMinecraft().getTextureManager().bindTexture(getCustomGlintTexture());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GlStateManager.matrixMode(5890);
        GlStateManager.pushMatrix();
        GlStateManager.scale(8.0F, 8.0F, 8.0F);
        float f = (float) (Minecraft.getSystemTime() % 3000L) / 3000.0F / 8.0F;
        GlStateManager.translate(f, 0.0F, 0.0F);
        GlStateManager.rotate(-50.0F, 0.0F, 0.0F, 1.0F);
        renderModelCallback.accept(color);
        GlStateManager.matrixMode(5890);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.scale(8.0F, 8.0F, 8.0F);
        float f1 = (float) (Minecraft.getSystemTime() % 4873L) / 4873.0F / 8.0F;
        GlStateManager.translate(-f1, 0.0F, 0.0F);
        GlStateManager.rotate(10.0F, 0.0F, 0.0F, 1.0F);
        renderModelCallback.accept(color);
        GlStateManager.matrixMode(5890);
        GlStateManager.popMatrix();

        GlStateManager.matrixMode(5888);
        GlStateManager.blendFunc(770, 771);
        GlStateManager.enableLighting();
        GlStateManager.depthFunc(515);
        GlStateManager.depthMask(true);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

        GL11.glPopMatrix();
    }

    private static void renderArmorGlint(Runnable renderModelCallback, float existed, int color) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(getCustomGlintTexture());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager.enableBlend();
        GlStateManager.depthFunc(514);
        GlStateManager.depthMask(false);
        float f1 = 0.5F;
        GlStateManager.color(f1, f1, f1, 1.0F);

        for (int i = 0; i < 2; ++i) {
            GlStateManager.disableLighting();

            float red = ((color >> 16) & 0xFF) / 255f;
            float green = ((color >> 8) & 0xFF) / 255f;
            float blue = (color & 0xFF) / 255f;
            float alpha = ((color >> 24) & 0xFF) / 255f;

            GlStateManager.color(red, green, blue, alpha);
            GlStateManager.matrixMode(5890);
            GlStateManager.loadIdentity();
            float f3 = 0.33333334F;
            GlStateManager.scale(f3, f3, f3);
            GlStateManager.rotate(30.0F - (float) i * 60.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(0.0F, existed * (0.001F + (float) i * 0.003F) * 20.0F, 0.0F);
            GlStateManager.matrixMode(5888);
            renderModelCallback.run();
        }

        GlStateManager.matrixMode(5890);
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(5888);
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(515);
        GlStateManager.disableBlend();
    }

    public static void pre() {
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    }

    public static boolean render3DGlint(String customEnchantGlint, float existed, Runnable renderModelCallback) {
        if (customEnchantGlint != null) {
            int colour = ChromaColour.specialToChromaRGB(customEnchantGlint);

            float[] hsv = Color.RGBtoHSB((colour >> 16) & 0xff, (colour >> 8) & 0xff, colour & 0xff, null);
            GL14.glBlendColor(1, 1, 1, hsv[2]);

            GlStateManager.tryBlendFuncSeparate(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_ALPHA);
            int alphaValue = (int) ((1 - hsv[2] * hsv[2]) * 0xff) * ((colour >> 24) & 0xff) / 0xff;
            renderArmorGlint(renderModelCallback, existed, alphaValue << 24);
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
            renderArmorGlint(renderModelCallback, existed, colour);

            return true;
        }
        return false;
    }

    public static boolean renderEffectHook(String customEnchantGlint, Consumer<Integer> renderModelCallback) {
        if (customEnchantGlint != null) {
            int colour = ChromaColour.specialToChromaRGB(customEnchantGlint);

            float[] hsv = Color.RGBtoHSB((colour >> 16) & 0xff, (colour >> 8) & 0xff, colour & 0xff, null);
            GL14.glBlendColor(1, 1, 1, hsv[2]);

            GL11.glPushMatrix();

            GlStateManager.tryBlendFuncSeparate(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_ALPHA);
            int alphaValue = (int) ((1 - hsv[2] * hsv[2]) * 0xff) * ((colour >> 24) & 0xff) / 0xff;
            renderEffect(renderModelCallback, alphaValue << 24);
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
            renderEffect(renderModelCallback, colour);

            GL11.glPopMatrix();

            return true;
        }
        return false;
    }

    public static ResourceLocation getCustomGlintTexture() {
        if (!loadedCustomGlintTexture) {
            loadedCustomGlintTexture = true;

            final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

            try {
                BufferedImage originalGlint = ImageIO.read(Minecraft.getMinecraft().getResourceManager().getResource(RES_ITEM_GLINT).getInputStream());
                BufferedImage newGlint = new BufferedImage(originalGlint.getWidth(), originalGlint.getHeight(), BufferedImage.TYPE_INT_ARGB);

                for (int x = 0; x < originalGlint.getWidth(); x++) {
                    for (int y = 0; y < originalGlint.getHeight(); y++) {
                        int argb = originalGlint.getRGB(x, y);

                        int avgRGB = ((((argb >> 16) & 0xff) + ((argb >> 8) & 0xff) + (argb & 0xff)) / 3) & 0xff;

                        int newArgb = (avgRGB << 24) | (avgRGB << 16) | (avgRGB << 8) | avgRGB;

                        newGlint.setRGB(x, y, newArgb);
                    }
                }

                Minecraft.getMinecraft().getTextureManager().loadTexture(CUSTOM_GLINT_TEXTURE, new DynamicTexture(newGlint));
            } catch (Exception e) {
                e.printStackTrace();
                CUSTOM_GLINT_TEXTURE = RES_ITEM_GLINT;
            }
        }
        return CUSTOM_GLINT_TEXTURE;
    }

    private static String getUuidForItem(ItemStack stack) {
        if (!stack.hasTagCompound()) return null;

        int nbtHash = stack.getTagCompound().hashCode();

        if (itemUuidCache.containsKey(nbtHash)) {
            return itemUuidCache.get(nbtHash);
        }

        String uuid = NotEnoughUpdates.INSTANCE.manager.getUUIDForItem(stack);

        itemUuidCache.put(nbtHash, uuid);
        return uuid;
    }

    public static ItemData getDataForItem(ItemStack stack) {
        if (stack == null) return null;

        String uuid = getUuidForItem(stack);

        if (uuid == null) {
            return null;
        } else {
            return itemDataMap.itemData.get(uuid);
        }
    }

    public static void tick() {
        itemUuidCache.clear();
        disableTextureBinding = false;
    }

    public static void loadCustomization(File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            itemDataMap = GSON.fromJson(reader, ItemDataMap.class);
        } catch (Exception ignored) {
        }
        if (itemDataMap == null) {
            itemDataMap = new ItemDataMap();
        }
    }

    public static void saveCustomization(File file) {
        try {
            file.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write(GSON.toJson(itemDataMap));
            }
        } catch (Exception ignored) {
        }
    }

}
