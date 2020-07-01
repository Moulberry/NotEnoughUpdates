package io.github.moulberry.notenoughupdates;

import com.mojang.authlib.Agent;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import io.github.moulberry.notenoughupdates.util.TexLoc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static boolean hasEffectOverride = false;

    public static <T> ArrayList<T> createList(T... values) {
        ArrayList<T> list = new ArrayList<>();
        for(T value : values)list.add(value);
        return list;
    }

    public static boolean getHasEffectOverride() {
        return hasEffectOverride;
    }

    public static void drawItemStackWithoutGlint(ItemStack stack, int x, int y) {
        RenderItem itemRender = Minecraft.getMinecraft().getRenderItem();

        RenderHelper.enableGUIStandardItemLighting();
        itemRender.zLevel = -145; //Negates the z-offset of the below method.
        hasEffectOverride = true;
        try {
            itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        } catch(Exception e) {e.printStackTrace();} //Catch exceptions to ensure that hasEffectOverride is set back to false.
        hasEffectOverride = false;
        itemRender.zLevel = 0;
        RenderHelper.disableStandardItemLighting();
    }

    public static void drawItemStack(ItemStack stack, int x, int y) {
        RenderItem itemRender = Minecraft.getMinecraft().getRenderItem();

        RenderHelper.enableGUIStandardItemLighting();
        itemRender.zLevel = -145; //Negates the z-offset of the below method.
        itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        itemRender.zLevel = 0;
        RenderHelper.disableStandardItemLighting();
    }

    public static Method getMethod(Class<?> clazz, Class<?>[] params, String... methodNames) {
        for(String methodName : methodNames) {
            try {
                return clazz.getDeclaredMethod(methodName, params);
            } catch(Exception e) {}
        }
        return null;
    }

    public static Object getField(Class<?> clazz, Object o, String... fieldNames) {
        Field field = null;
        for(String fieldName : fieldNames) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch(Exception e) {}
        }
        if(field != null) {
            field.setAccessible(true);
            try {
                return field.get(o);
            } catch(IllegalAccessException e) {
            }
        }
        return null;
    }

    public static Slot getSlotUnderMouse(GuiContainer container) {
        return (Slot) getField(GuiContainer.class, container, "theSlot", "field_147006_u");
    }

    public static void drawTexturedRect(float x, float y, float width, float height) {
        drawTexturedRect(x, y, width, height, 0, 1, 0 , 1);
    }

    public static void drawTexturedRect(float x, float y, float width, float height, int filter) {
        drawTexturedRect(x, y, width, height, 0, 1, 0 , 1, filter);
    }

    public static void drawTexturedRect(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax) {
        drawTexturedRect(x, y, width, height, uMin, uMax, vMin , vMax, GL11.GL_LINEAR);
    }

    public static String cleanColour(String in) {
        return in.replaceAll("(?i)\\u00A7.", "");
    }

    public static void drawTexturedRect(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax, int filter) {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

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

        GlStateManager.disableBlend();
    }

    public static void drawStringScaledMaxWidth(String str, FontRenderer fr, float x, float y, boolean shadow, int len, int colour) {
        int strLen = fr.getStringWidth(str);
        float factor = len/(float)strLen;
        factor = Math.min(1, factor);

        drawStringScaled(str, fr, x, y, shadow, colour, factor);
    }

    public static void drawStringScaled(String str, FontRenderer fr, float x, float y, boolean shadow, int colour, float factor) {
        GlStateManager.scale(factor, factor, 1);
        fr.drawString(str, x/factor, y/factor, colour, shadow);
        GlStateManager.scale(1/factor, 1/factor, 1);
    }

    public static void drawStringCenteredScaledMaxWidth(String str, FontRenderer fr, float x, float y, boolean shadow, int len, int colour) {
        int strLen = fr.getStringWidth(str);
        float factor = len/(float)strLen;
        factor = Math.min(1, factor);
        int newLen = Math.min(strLen, len);

        float fontHeight = 8*factor;

        drawStringScaled(str, fr, x-newLen/2, y-fontHeight/2, shadow, colour, factor);
    }

    public static void drawStringCenteredScaled(String str, FontRenderer fr, float x, float y, boolean shadow, int len, int colour) {
        int strLen = fr.getStringWidth(str);
        float factor = len/(float)strLen;
        float fontHeight = 8*factor;

        drawStringScaled(str, fr, x-len/2, y-fontHeight/2, shadow, colour, factor);
    }

    public static void drawStringCenteredYScaled(String str, FontRenderer fr, float x, float y, boolean shadow, int len, int colour) {
        int strLen = fr.getStringWidth(str);
        float factor = len/(float)strLen;
        float fontHeight = 8*factor;

        drawStringScaled(str, fr, x, y-fontHeight/2, shadow, colour, factor);
    }

    public static int renderStringTrimWidth(String str, FontRenderer fr, boolean shadow, int x, int y, int len, int colour, int maxLines) {
        return renderStringTrimWidth(str, fr, shadow, x, y, len, colour, maxLines, 1);
    }

    public static int renderStringTrimWidth(String str, FontRenderer fr, boolean shadow, int x, int y, int len, int colour, int maxLines, float scale) {
        len = (int)(len/scale);

        int yOff = 0;
        String excess;
        String trimmed = trimToWidth(str, len);

        String colourCodes = "";
        Pattern pattern = Pattern.compile("\\u00A7.");
        Matcher matcher = pattern.matcher(trimmed);
        while(matcher.find()) {
            colourCodes += matcher.group();
        }

        boolean firstLine = true;
        int trimmedCharacters = trimmed.length();
        int lines = 0;
        while((lines++<maxLines) || maxLines<0) {
            if(trimmed.length() == str.length()) {
                drawStringScaled(trimmed, fr, x, y+yOff, shadow, colour, scale);
                //fr.drawString(trimmed, x, y + yOff, colour, shadow);
                break;
            } else if(trimmed.isEmpty()) {
                yOff -= 12*scale;
                break;
            } else {
                if(firstLine) {
                    drawStringScaled(trimmed, fr, x, y+yOff, shadow, colour, scale);
                    firstLine = false;
                } else {
                    if(trimmed.startsWith(" ")) {
                        trimmed = trimmed.substring(1);
                    }
                    drawStringScaled(colourCodes + trimmed, fr, x, y+yOff, shadow, colour, scale);
                }

                excess = str.substring(trimmedCharacters);
                trimmed = trimToWidth(excess, len);
                trimmedCharacters += trimmed.length();
                yOff += 12*scale;
            }
        }
        return yOff;
    }

    public static String trimToWidth(String str, int len) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        String trim = fr.trimStringToWidth(str, len);

        if(str.length() != trim.length() && !trim.endsWith(" ")) {
            char next = str.charAt(trim.length());
            if(next != ' ') {
                String[] split = trim.split(" ");
                String last = split[split.length-1];
                if(last.length() < 8) {
                    trim = trim.substring(0, trim.length()-last.length());
                }
            }
        }

        return trim;
    }

    public static void drawHoveringText(List<String> textLines, final int mouseX, final int mouseY, final int screenWidth, final int screenHeight, final int maxTextWidth, FontRenderer font) {
        if (!textLines.isEmpty())
        {
            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            int tooltipTextWidth = 0;

            for (String textLine : textLines)
            {
                int textLineWidth = font.getStringWidth(textLine);

                if (textLineWidth > tooltipTextWidth)
                {
                    tooltipTextWidth = textLineWidth;
                }
            }

            boolean needsWrap = false;

            int titleLinesCount = 1;
            int tooltipX = mouseX + 12;
            if (tooltipX + tooltipTextWidth + 4 > screenWidth)
            {
                tooltipX = mouseX - 16 - tooltipTextWidth;
                if (tooltipX < 4) // if the tooltip doesn't fit on the screen
                {
                    if (mouseX > screenWidth / 2)
                    {
                        tooltipTextWidth = mouseX - 12 - 8;
                    }
                    else
                    {
                        tooltipTextWidth = screenWidth - 16 - mouseX;
                    }
                    needsWrap = true;
                }
            }

            if (maxTextWidth > 0 && tooltipTextWidth > maxTextWidth)
            {
                tooltipTextWidth = maxTextWidth;
                needsWrap = true;
            }

            if (needsWrap)
            {
                int wrappedTooltipWidth = 0;
                List<String> wrappedTextLines = new ArrayList<String>();
                for (int i = 0; i < textLines.size(); i++)
                {
                    String textLine = textLines.get(i);
                    List<String> wrappedLine = font.listFormattedStringToWidth(textLine, tooltipTextWidth);
                    if (i == 0)
                    {
                        titleLinesCount = wrappedLine.size();
                    }

                    for (String line : wrappedLine)
                    {
                        int lineWidth = font.getStringWidth(line);
                        if (lineWidth > wrappedTooltipWidth)
                        {
                            wrappedTooltipWidth = lineWidth;
                        }
                        wrappedTextLines.add(line);
                    }
                }
                tooltipTextWidth = wrappedTooltipWidth;
                textLines = wrappedTextLines;

                if (mouseX > screenWidth / 2)
                {
                    tooltipX = mouseX - 16 - tooltipTextWidth;
                }
                else
                {
                    tooltipX = mouseX + 12;
                }
            }

            int tooltipY = mouseY - 12;
            int tooltipHeight = 8;

            if (textLines.size() > 1)
            {
                tooltipHeight += (textLines.size() - 1) * 10;
                if (textLines.size() > titleLinesCount) {
                    tooltipHeight += 2; // gap between title lines and next lines
                }
            }

            if (tooltipY + tooltipHeight + 6 > screenHeight)
            {
                tooltipY = screenHeight - tooltipHeight - 6;
            }

            final int zLevel = 300;
            final int backgroundColor = 0xF0100010;
            drawGradientRect(zLevel, tooltipX - 3, tooltipY - 4, tooltipX + tooltipTextWidth + 3, tooltipY - 3, backgroundColor, backgroundColor);
            drawGradientRect(zLevel, tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 4, backgroundColor, backgroundColor);
            drawGradientRect(zLevel, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
            drawGradientRect(zLevel, tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
            drawGradientRect(zLevel, tooltipX + tooltipTextWidth + 3, tooltipY - 3, tooltipX + tooltipTextWidth + 4, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
            final int borderColorStart = 0x505000FF;
            final int borderColorEnd = (borderColorStart & 0xFEFEFE) >> 1 | borderColorStart & 0xFF000000;
            drawGradientRect(zLevel, tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
            drawGradientRect(zLevel, tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
            drawGradientRect(zLevel, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY - 3 + 1, borderColorStart, borderColorStart);
            drawGradientRect(zLevel, tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, borderColorEnd, borderColorEnd);

            for (int lineNumber = 0; lineNumber < textLines.size(); ++lineNumber)
            {
                String line = textLines.get(lineNumber);
                font.drawStringWithShadow(line, (float)tooltipX, (float)tooltipY, -1);

                if (lineNumber + 1 == titleLinesCount)
                {
                    tooltipY += 2;
                }

                tooltipY += 10;
            }

            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
            RenderHelper.enableStandardItemLighting();
            GlStateManager.enableRescaleNormal();
        }
        GlStateManager.disableLighting();
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

}
