package io.github.moulberry.notenoughupdates.core.util.render;

import io.github.moulberry.notenoughupdates.core.BackgroundBlur;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector3f;

public class RenderUtils {

    public static void drawFloatingRectDark(int x, int y, int width, int height) {
        drawFloatingRectDark(x, y, width, height, true);
    }

    public static void drawFloatingRectDark(int x, int y, int width, int height, boolean shadow) {
        int alpha = 0xf0000000;

        if(OpenGlHelper.isFramebufferEnabled()) {
            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            BackgroundBlur.renderBlurredBackground(15, scaledResolution.getScaledWidth(),
                    scaledResolution.getScaledHeight(), x, y, width, height, true);
        } else {
            alpha = 0xff000000;
        }

        int main = alpha | 0x202026;
        int light = 0xff303036;
        int dark = 0xff101016;
        Gui.drawRect(x, y, x+1, y+height, light); //Left
        Gui.drawRect(x+1, y, x+width, y+1, light); //Top
        Gui.drawRect(x+width-1, y+1, x+width, y+height, dark); //Right
        Gui.drawRect(x+1, y+height-1, x+width-1, y+height, dark); //Bottom
        Gui.drawRect(x+1, y+1, x+width-1, y+height-1, main); //Middle
        if(shadow) {
            Gui.drawRect(x+width, y+2, x+width+2, y+height+2, 0x70000000); //Right shadow
            Gui.drawRect(x+2, y+height, x+width, y+height+2, 0x70000000); //Bottom shadow
        }
    }

    public static void drawFloatingRect(int x, int y, int width, int height) {
        drawFloatingRectWithAlpha(x, y, width, height, 0xFF, true);
    }

    public static void drawFloatingRectWithAlpha(int x, int y, int width, int height, int alpha, boolean shadow) {
        int main = (alpha << 24) | 0xc0c0c0;
        int light = (alpha << 24) | 0xf0f0f0;
        int dark = (alpha << 24) | 0x909090;
        Gui.drawRect(x, y, x+1, y+height, light); //Left
        Gui.drawRect(x+1, y, x+width, y+1, light); //Top
        Gui.drawRect(x+width-1, y+1, x+width, y+height, dark); //Right
        Gui.drawRect(x+1, y+height-1, x+width-1, y+height, dark); //Bottom
        Gui.drawRect(x+1, y+1, x+width-1, y+height-1, main); //Middle
        if(shadow) {
            Gui.drawRect(x+width, y+2, x+width+2, y+height+2, (alpha*3/5) << 24); //Right shadow
            Gui.drawRect(x+2, y+height, x+width, y+height+2, (alpha*3/5) << 24); //Bottom shadow
        }
    }

    public static void drawTexturedRect(float x, float y, float width, float height) {
        drawTexturedRect(x, y, width, height, 0, 1, 0 , 1);
    }

    public static void drawTexturedRect(float x, float y, float width, float height, int filter) {
        drawTexturedRect(x, y, width, height, 0, 1, 0 , 1, filter);
    }

    public static void drawTexturedRect(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax) {
        drawTexturedRect(x, y, width, height, uMin, uMax, vMin , vMax, GL11.GL_NEAREST);
    }

    public static void drawTexturedRect(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax, int filter) {
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

        drawTexturedRectNoBlend(x, y, width, height, uMin, uMax, vMin, vMax, filter);

        GlStateManager.disableBlend();
    }

    public static void drawTexturedRectNoBlend(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax, int filter) {
        GlStateManager.enableTexture2D();

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer
                .pos(x, y+height, 0.0D)
                .tex(uMin, vMax).endVertex();
        worldrenderer
                .pos(x+width, y+height, 0.0D)
                .tex(uMax, vMax).endVertex();
        worldrenderer
                .pos(x+width, y, 0.0D)
                .tex(uMax, vMin).endVertex();
        worldrenderer
                .pos(x, y, 0.0D)
                .tex(uMin, vMin).endVertex();
        tessellator.draw();

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    public static void drawGradientRect(int zLevel, int left, int top, int right, int bottom, int startColor, int endColor) {
        float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
        float startRed = (float)(startColor >> 16 & 255) / 255.0F;
        float startGreen = (float)(startColor >> 8 & 255) / 255.0F;
        float startBlue = (float)(startColor & 255) / 255.0F;
        float endAlpha = (float)(endColor >> 24 & 255) / 255.0F;
        float endRed = (float)(endColor >> 16 & 255) / 255.0F;
        float endGreen = (float)(endColor >> 8 & 255) / 255.0F;
        float endBlue = (float)(endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(right, top, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        worldrenderer.pos(left, top, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        worldrenderer.pos(left, bottom, zLevel).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        worldrenderer.pos(right, bottom, zLevel).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void renderWayPoint(String str, BlockPos loc, float partialTicks) {
        renderWayPoint(str, new Vector3f(loc.getX(), loc.getY(), loc.getZ()), partialTicks);
    }
    public static void renderWayPoint(String str, Vector3f loc, float partialTicks) {
        GlStateManager.alphaFunc(516, 0.1F);

        GlStateManager.pushMatrix();

        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;

        double x = loc.x-viewerX+0.5f;
        double y = loc.y-viewerY-viewer.getEyeHeight();
        double z = loc.z-viewerZ+0.5f;

        double distSq = x*x + y*y + z*z;
        double dist = Math.sqrt(distSq);
        if(distSq > 144) {
            x *= 12/dist;
            y *= 12/dist;
            z *= 12/dist;
        }
        GlStateManager.translate(x, y, z);
        GlStateManager.translate(0, viewer.getEyeHeight(), 0);

        renderNametag(str);

        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0, -0.25f, 0);
        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);

        renderNametag(EnumChatFormatting.YELLOW.toString()+Math.round(dist)+"m");

        GlStateManager.popMatrix();

        GlStateManager.disableLighting();
    }

    public static void renderNametag(String str) {
        FontRenderer fontrenderer = Minecraft.getMinecraft().fontRendererObj;
        float f = 1.6F;
        float f1 = 0.016666668F * f;
        GlStateManager.pushMatrix();
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-f1, -f1, f1);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        int i = 0;

        int j = fontrenderer.getStringWidth(str) / 2;
        GlStateManager.disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos((double)(-j - 1), (double)(-1 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double)(-j - 1), (double)(8 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double)(j + 1), (double)(8 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double)(j + 1), (double)(-1 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        fontrenderer.drawString(str, -fontrenderer.getStringWidth(str) / 2, i, 553648127);
        GlStateManager.depthMask(true);

        fontrenderer.drawString(str, -fontrenderer.getStringWidth(str) / 2, i, -1);

        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

}
