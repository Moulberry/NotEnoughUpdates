package io.github.moulberry.notenoughupdates.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gson.*;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.*;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static boolean hasEffectOverride = false;
    public static boolean disableCustomDungColours = false;
    private static LinkedList<Integer> guiScales = new LinkedList<>();
    private static ScaledResolution lastScale = new ScaledResolution(Minecraft.getMinecraft());
    //Labymod compatibility
    private static FloatBuffer projectionMatrixOld = BufferUtils.createFloatBuffer(16);
    private static FloatBuffer modelviewMatrixOld = BufferUtils.createFloatBuffer(16);

    public static <T> ArrayList<T> createList(T... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    public static void resetGuiScale() {
        guiScales.clear();
    }

    public static ScaledResolution peekGuiScale() {
        return lastScale;
    }

    public static ScaledResolution pushGuiScale(int scale) {
        if(guiScales.size() == 0) {
            if(Loader.isModLoaded("labymod")) {
                GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionMatrixOld);
                GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelviewMatrixOld);
            }
        }

        if(scale < 0) {
            if(guiScales.size() > 0) {
                guiScales.pop();
            }
        } else {
            if(scale == 0) {
                guiScales.push(Minecraft.getMinecraft().gameSettings.guiScale);
            } else {
                guiScales.push(scale);
            }
        }

        int newScale = guiScales.size() > 0 ? Math.max(0, Math.min(4, guiScales.peek())) : Minecraft.getMinecraft().gameSettings.guiScale;
        if(newScale == 0) newScale = Minecraft.getMinecraft().gameSettings.guiScale;

        int oldScale = Minecraft.getMinecraft().gameSettings.guiScale;
        Minecraft.getMinecraft().gameSettings.guiScale = newScale;
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        Minecraft.getMinecraft().gameSettings.guiScale = oldScale;

        if(guiScales.size() > 0) {
            GlStateManager.viewport(0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0D,
                    scaledresolution.getScaledWidth_double(),
                    scaledresolution.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();
            GlStateManager.translate(0.0F, 0.0F, -2000.0F);
        } else {
            if(Loader.isModLoaded("labymod") && projectionMatrixOld.limit() > 0 && modelviewMatrixOld.limit() > 0) {
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GL11.glLoadMatrix(projectionMatrixOld);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GL11.glLoadMatrix(modelviewMatrixOld);
            } else {
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.loadIdentity();
                GlStateManager.ortho(0.0D,
                        scaledresolution.getScaledWidth_double(),
                        scaledresolution.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.loadIdentity();
                GlStateManager.translate(0.0F, 0.0F, -2000.0F);
            }
        }

        lastScale = scaledresolution;
        return scaledresolution;
    }

    public static boolean getHasEffectOverride() {
        return hasEffectOverride;
    }

    public static void drawItemStackWithoutGlint(ItemStack stack, int x, int y) {
        RenderItem itemRender = Minecraft.getMinecraft().getRenderItem();

        disableCustomDungColours = true;
        RenderHelper.enableGUIStandardItemLighting();
        itemRender.zLevel = -145; //Negates the z-offset of the below method.
        hasEffectOverride = true;
        try {
            itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        } catch(Exception e) {e.printStackTrace();} //Catch exceptions to ensure that hasEffectOverride is set back to false.
        itemRender.renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRendererObj, stack, x, y, null);
        hasEffectOverride = false;
        itemRender.zLevel = 0;
        RenderHelper.disableStandardItemLighting();
        disableCustomDungColours = false;
    }

    public static void drawItemStackWithText(ItemStack stack, int x, int y, String text) {
        if(stack == null)return;

        RenderItem itemRender = Minecraft.getMinecraft().getRenderItem();

        disableCustomDungColours = true;
        RenderHelper.enableGUIStandardItemLighting();
        itemRender.zLevel = -145; //Negates the z-offset of the below method.
        itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        itemRender.renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRendererObj, stack, x, y, text);
        itemRender.zLevel = 0;
        RenderHelper.disableStandardItemLighting();
        disableCustomDungColours = false;
    }

    public static void drawItemStack(ItemStack stack, int x, int y) {
        if(stack == null) return;

        drawItemStackWithText(stack, x, y, null);
    }

    private static final EnumChatFormatting[] rainbow = new EnumChatFormatting[]{
            EnumChatFormatting.RED,
            EnumChatFormatting.GOLD,
            EnumChatFormatting.YELLOW,
            EnumChatFormatting.GREEN,
            EnumChatFormatting.AQUA,
            EnumChatFormatting.LIGHT_PURPLE,
            EnumChatFormatting.DARK_PURPLE
    };

    public static String chromaString(String str) {
        return chromaString(str, 0, false);
    }

    private static final Pattern CHROMA_REPLACE_PATTERN = Pattern.compile("\u00a7z(.+?)(?=\u00a7|$)");

    public static String chromaStringByColourCode(String str) {
        if(str.contains("\u00a7z")) {
            Matcher matcher = CHROMA_REPLACE_PATTERN.matcher(str);

            StringBuffer sb = new StringBuffer();

            while(matcher.find()) {
                matcher.appendReplacement(sb,
                        Utils.chromaString(matcher.group(1))
                                .replace("\\", "\\\\")
                                .replace("$", "\\$")
                );
            }
            matcher.appendTail(sb);

            str = sb.toString();
        }
        return str;
    }

    private static long startTime = 0;
    public static String chromaString(String str, float offset, boolean bold) {
        str = cleanColour(str);

        long currentTimeMillis = System.currentTimeMillis();
        if(startTime == 0) startTime = currentTimeMillis;

        int chromaSpeed = NotEnoughUpdates.INSTANCE.config.misc.chromaSpeed;
        if(chromaSpeed < 10) chromaSpeed = 10;
        if(chromaSpeed > 5000) chromaSpeed = 5000;

        StringBuilder rainbowText = new StringBuilder();
        int len = 0;
        for(int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            int index = ((int)(offset+len/12f-(currentTimeMillis-startTime)/chromaSpeed))%rainbow.length;
            len += Minecraft.getMinecraft().fontRendererObj.getCharWidth(c);
            if(bold) len++;

            if(index < 0) index += rainbow.length;
            rainbowText.append(rainbow[index]);
            if(bold) rainbowText.append(EnumChatFormatting.BOLD);
            rainbowText.append(c);
        }
        return rainbowText.toString();
    }

    private static char[] c = new char[]{'k', 'm', 'b', 't'};
    public static String shortNumberFormat(double n, int iteration) {
        double d = ((long) n / 100) / 10.0;
        boolean isRound = (d * 10) %10 == 0;
        return (d < 1000?
                ((d > 99.9 || isRound || (!isRound && d > 9.99)?
                        (int) d * 10 / 10 : d + ""
                ) + "" + c[iteration])
                : shortNumberFormat(d, iteration+1));
    }

    public static String trimIgnoreColour(String str) {
        return trimIgnoreColourStart(trimIgnoreColourEnd(str));
    }

    public static String trimIgnoreColourStart(String str) {
        str = str.trim();
        boolean colourCodeLast = false;
        StringBuilder colours = new StringBuilder();
        for(int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            if(colourCodeLast) {
                colours.append('\u00a7').append(c);
                colourCodeLast = false;
                continue;
            }
            if(c == '\u00A7') {
                colourCodeLast = true;
            } else if(c != ' ') {
                return colours.append(str.substring(i)).toString();
            }
        }

        return "";
    }

    public static String trimIgnoreColourEnd(String str) {
        str = str.trim();
        for(int i=str.length()-1; i>=0; i--) {
            char c = str.charAt(i);
            if(c == ' ') {
                continue;
            } else if(i > 0 && str.charAt(i-1) == '\u00a7') {
                i--;
                continue;
            }

            return str.substring(0, i+1);
        }

        return "";
    }

    public static List<String> getRawTooltip(ItemStack stack) {
        List<String> list = Lists.newArrayList();
        String s = stack.getDisplayName();

        if (stack.hasDisplayName()) {
            s = EnumChatFormatting.ITALIC + s;
        }

        s = s + EnumChatFormatting.RESET;

        if (!stack.hasDisplayName() && stack.getItem() == Items.filled_map) {
            s = s + " #" + stack.getItemDamage();
        }

        list.add(s);

        if (stack.hasTagCompound()) {
            if (stack.getTagCompound().hasKey("display", 10)) {
                NBTTagCompound nbttagcompound = stack.getTagCompound().getCompoundTag("display");

                if (nbttagcompound.hasKey("color", 3)) {
                    list.add(EnumChatFormatting.ITALIC + StatCollector.translateToLocal("item.dyed"));
                }

                if (nbttagcompound.getTagId("Lore") == 9) {
                    NBTTagList nbttaglist1 = nbttagcompound.getTagList("Lore", 8);

                    if (nbttaglist1.tagCount() > 0) {
                        for (int j1 = 0; j1 < nbttaglist1.tagCount(); ++j1) {
                            list.add(EnumChatFormatting.DARK_PURPLE + "" + EnumChatFormatting.ITALIC + nbttaglist1.getStringTagAt(j1));
                        }
                    }
                }
            }
        }

        return list;
    }

    public static String floatToString(float f, int decimals) {
        if(decimals <= 0) {
            return String.valueOf(Math.round(f));
        } else {
            return String.format("%."+decimals+"f", f + 0.00001f);
        }
    }

    public static void drawItemStackLinear(ItemStack stack, int x, int y) {
        if(stack == null)return;

        RenderItem itemRender = Minecraft.getMinecraft().getRenderItem();

        RenderHelper.enableGUIStandardItemLighting();
        itemRender.zLevel = -145; //Negates the z-offset of the below method.

        IBakedModel ibakedmodel = itemRender.getItemModelMesher().getItemModel(stack);
        GlStateManager.pushMatrix();
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(true, true);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        setupGuiTransform(x, y, ibakedmodel.isGui3d());
        ibakedmodel = net.minecraftforge.client.ForgeHooksClient.handleCameraTransforms(ibakedmodel, ItemCameraTransforms.TransformType.GUI);
        itemRender.renderItem(stack, ibakedmodel);
        GlStateManager.disableAlpha();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();

        itemRender.renderItemOverlays(Minecraft.getMinecraft().fontRendererObj, stack, x, y);
        itemRender.zLevel = 0;
        RenderHelper.disableStandardItemLighting();
    }

    private static void setupGuiTransform(int xPosition, int yPosition, boolean isGui3d) {
        GlStateManager.translate((float)xPosition, (float)yPosition, 5);
        GlStateManager.translate(8.0F, 8.0F, 0.0F);
        GlStateManager.scale(1.0F, 1.0F, -1.0F);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);

        if (isGui3d) {
            GlStateManager.scale(40.0F, 40.0F, 40.0F);
            GlStateManager.rotate(210.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.enableLighting();
        } else {
            GlStateManager.scale(64.0F, 64.0F, 64.0F);
            GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.disableLighting();
        }
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

    public static String cleanColourNotModifiers(String in) {
        return in.replaceAll("(?i)\\u00A7[0-9a-f]", "");
    }

    public static String fixBrokenAPIColour(String in) {
        return in.replaceAll("(?i)\\u00C2(\\u00A7.)", "$1");
    }

    public static String prettyCase(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private static String[] rarityArr = new String[] {
            "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "SPECIAL", "VERY SPECIAL",
    };
    public static int checkItemType(JsonArray lore, boolean contains, String... typeMatches) {
        for(int i=lore.size()-1; i>=0; i--) {
            String line = lore.get(i).getAsString();
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
        return -1;
    }

    public static void playPressSound() {
        if(NotEnoughUpdates.INSTANCE.config.misc.guiButtonClicks) {
            Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(
                    new ResourceLocation("gui.button.press"), 1.0F));
        }
    }

    public static String cleanDuplicateColourCodes(String line) {
        StringBuilder sb = new StringBuilder();
        char currentColourCode = 'r';
        boolean sectionSymbolLast = false;
        for(char c : line.toCharArray()) {
            if((int)c > 50000) continue;

            if(c == '\u00a7') {
                sectionSymbolLast = true;
            } else {
                if(sectionSymbolLast) {
                    if(currentColourCode != c) {
                        sb.append('\u00a7');
                        sb.append(c);
                        currentColourCode = c;
                    }
                    sectionSymbolLast = false;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    public static void drawTexturedRect(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax, int filter) {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
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

    public static ItemStack createItemStack(Item item, String displayname, String... lore) {
        return createItemStack(item, displayname, 0, lore);
    }

    public static ItemStack createItemStack(Item item, String displayname, int damage, String... lore) {
        ItemStack stack = new ItemStack(item, 1, damage);
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound display = new NBTTagCompound();
        NBTTagList Lore = new NBTTagList();

        for(String line : lore) {
            Lore.appendTag(new NBTTagString(line));
        }

        display.setString("Name", displayname);
        display.setTag("Lore", Lore);

        tag.setTag("display", display);
        tag.setInteger("HideFlags", 254);

        stack.setTagCompound(tag);

        return stack;
    }

    public static void drawStringF(String str, FontRenderer fr, float x, float y, boolean shadow, int colour) {
        fr.drawString(str, x, y, colour, shadow);
    }

    public static int getCharVertLen(char c) {
        if("acegmnopqrsuvwxyz".indexOf(c) >= 0) {
            return 5;
        } else {
            return 7;
        }
    }

    public static float getVerticalHeight(String str) {
        str = cleanColour(str);
        float height = 0;
        for(int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            int charHeight =  getCharVertLen(c);
            height += charHeight + 1.5f;
        }
        return height;
    }

    public static void drawStringVertical(String str, FontRenderer fr, float x, float y, boolean shadow, int colour) {
        String format = FontRenderer.getFormatFromString(str);
        str = cleanColour(str);
        for(int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            
            int charHeight =  getCharVertLen(c);
            int charWidth = fr.getCharWidth(c);
            fr.drawString(format+c, x+(5-charWidth)/2f, y-7+charHeight, colour, shadow);

            y += charHeight + 1.5f;
        }
    }

    public static void renderShadowedString(String str, float x, float y, int maxLength) {
        int strLen = Minecraft.getMinecraft().fontRendererObj.getStringWidth(str);
        float factor;
        if(maxLength < 0) {
            factor = 1;
        } else {
            factor = maxLength/(float)strLen;
            factor = Math.min(1, factor);
        }

        for(int xOff=-2; xOff<=2; xOff++) {
            for(int yOff=-2; yOff<=2; yOff++) {
                if(Math.abs(xOff) != Math.abs(yOff)) {
                    Utils.drawStringCenteredScaledMaxWidth(Utils.cleanColourNotModifiers(str), Minecraft.getMinecraft().fontRendererObj,
                            x+xOff/2f*factor, y+4+yOff/2f*factor, false, maxLength,
                            new Color(0, 0, 0, 200/Math.max(Math.abs(xOff), Math.abs(yOff))).getRGB());
                }
            }
        }

        GlStateManager.color(1, 1, 1, 1);
        Utils.drawStringCenteredScaledMaxWidth(str, Minecraft.getMinecraft().fontRendererObj,
                x, y+4, false, maxLength, 4210752);
    }

    public static void renderAlignedString(String first, String second, float x, float y, int length) {
        FontRenderer fontRendererObj = Minecraft.getMinecraft().fontRendererObj;
        if(fontRendererObj.getStringWidth(first + " " + second) >= length) {
            renderShadowedString(first + " " + second, x+length/2f, y, length);
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

    public static void drawStringScaledMaxWidth(String str, FontRenderer fr, float x, float y, boolean shadow, int len, int colour) {
        int strLen = fr.getStringWidth(str);
        float factor = len/(float)strLen;
        factor = Math.min(1, factor);

        drawStringScaled(str, fr, x, y, shadow, colour, factor);
    }

    public static void drawStringCentered(String str, FontRenderer fr, float x, float y, boolean shadow, int colour) {
        int strLen = fr.getStringWidth(str);

        float x2 = x - strLen/2f;
        float y2 = y - fr.FONT_HEIGHT/2f;

        GL11.glTranslatef(x2, y2, 0);
        fr.drawString(str, 0, 0, colour, shadow);
        GL11.glTranslatef(-x2, -y2, 0);
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

    public static Matrix4f createProjectionMatrix(int width, int height) {
        Matrix4f projMatrix  = new Matrix4f();
        projMatrix.setIdentity();
        projMatrix.m00 = 2.0F / (float)width;
        projMatrix.m11 = 2.0F / (float)(-height);
        projMatrix.m22 = -0.0020001999F;
        projMatrix.m33 = 1.0F;
        projMatrix.m03 = -1.0F;
        projMatrix.m13 = 1.0F;
        projMatrix.m23 = -1.0001999F;
        return projMatrix;
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

    public static void drawStringCenteredYScaledMaxWidth(String str, FontRenderer fr, float x, float y, boolean shadow, int len, int colour) {
        int strLen = fr.getStringWidth(str);
        float factor = len/(float)strLen;
        factor = Math.min(1, factor);
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

        StringBuilder colourCodes = new StringBuilder();
        Pattern pattern = Pattern.compile("\\u00A7.");
        Matcher matcher = pattern.matcher(trimmed);
        while(matcher.find()) {
            colourCodes.append(matcher.group());
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

    public static void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(right, top, 0).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, top, 0).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, bottom, 0).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos(right, bottom, 0).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void drawGradientRectHorz(int left, int top, int right, int bottom, int startColor, int endColor) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(right, top, 0).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos(left, top, 0).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, bottom, 0).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(right, bottom, 0).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void drawHoveringText(List<String> textLines, final int mouseX, final int mouseY, final int screenWidth, final int screenHeight, final int maxTextWidth, FontRenderer font) {
        drawHoveringText(textLines, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth, font, true);
    }

    public static JsonObject getConstant(String constant, Gson gson) {
        return getConstant(constant, gson, JsonObject.class);
    }

    public static <T> T getConstant(String constant, Gson gson, Class<T> clazz) {
        File repo = NotEnoughUpdates.INSTANCE.manager.repoLocation;
        if(repo.exists()) {
            File jsonFile = new File(repo, "constants/"+constant+".json");
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8))) {
                return gson.fromJson(reader, clazz);
            } catch(Exception e) { return null; }
        }
        return null;
    }

    public static float getElementAsFloat(JsonElement element, float def) {
        if(element == null) return def;
        if(!element.isJsonPrimitive()) return def;
        JsonPrimitive prim = element.getAsJsonPrimitive();
        if(!prim.isNumber()) return def;
        return prim.getAsFloat();
    }

    public static String getElementAsString(JsonElement element, String def) {
        if(element == null) return def;
        if(!element.isJsonPrimitive()) return def;
        JsonPrimitive prim = element.getAsJsonPrimitive();
        if(!prim.isString()) return def;
        return prim.getAsString();
    }

    public static Splitter PATH_SPLITTER = Splitter.on(".").omitEmptyStrings().limit(2);
    public static JsonElement getElement(JsonElement element, String path) {
        List<String> path_split = PATH_SPLITTER.splitToList(path);
        if(element instanceof JsonObject) {
            JsonElement e = element.getAsJsonObject().get(path_split.get(0));
            if(path_split.size() > 1) {
                return getElement(e, path_split.get(1));
            } else {
                return e;
            }
        } else {
            return element;
        }
    }

    public static ChatStyle createClickStyle(ClickEvent.Action action, String value) {
        ChatStyle style = new ChatStyle();
        style.setChatClickEvent(new ClickEvent(action, value));
        style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW+value)));
        return style;
    }

    public static void recursiveDelete(File file) {
        if(file.isDirectory() && !Files.isSymbolicLink(file.toPath())) {
            for(File child : file.listFiles()) {
                recursiveDelete(child);
            }
        }
        file.delete();
    }

    public static char getPrimaryColourCode(String displayname) {
        int lastColourCode = -99;
        int currentColour = 0;
        int[] mostCommon = new int[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        for(int i=0; i<displayname.length(); i++) {
            char c = displayname.charAt(i);
            if(c == '\u00A7') {
                lastColourCode = i;
            } else if(lastColourCode == i-1) {
                int colIndex = "0123456789abcdef".indexOf(c);
                currentColour = Math.max(colIndex, 0);
            } else if("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(c) >= 0){
                if(currentColour > 0) {
                    mostCommon[currentColour]++;
                }
            }
        }
        int mostCommonCount = 0;
        for(int index=0; index<mostCommon.length; index++) {
            if(mostCommon[index] > mostCommonCount) {
                mostCommonCount = mostCommon[index];
                currentColour = index;
            }
        }

        return "0123456789abcdef".charAt(currentColour);
    }

    public static Color getPrimaryColour(String displayname) {
        int colourInt = Minecraft.getMinecraft().fontRendererObj.getColorCode(getPrimaryColourCode(displayname));
        return new Color(colourInt).darker();
    }

    public static void scrollTooltip(int dY) {
        scrollY.setTarget(scrollY.getTarget()+dY/10f);
        scrollY.resetTimer();
    }

    private static LerpingFloat scrollY = new LerpingFloat(0, 100);
    public static void drawHoveringText(List<String> textLines, final int mouseX, final int mouseY, final int screenWidth, final int screenHeight, final int maxTextWidth, FontRenderer font, boolean coloured) {
        if (!textLines.isEmpty())
        {
            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            GlStateManager.enableDepth();
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
                List<String> wrappedTextLines = new ArrayList<>();
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

            //Scrollable tooltips
            if(tooltipHeight + 6 > screenHeight) {
                if(scrollY.getTarget() < 0) {
                    scrollY.setTarget(0);
                    scrollY.resetTimer();
                } else if(screenHeight - tooltipHeight - 12 + (int)scrollY.getTarget() > 0) {
                    scrollY.setTarget(-screenHeight + tooltipHeight + 12);
                    scrollY.resetTimer();
                }
            } else {
                scrollY.setValue(0);
                scrollY.resetTimer();
            }
            scrollY.tick();

            if (tooltipY + tooltipHeight + 6 > screenHeight)
            {
                tooltipY = screenHeight - tooltipHeight - 6 + (int)scrollY.getValue();
            }

            final int zLevel = 300;
            final int backgroundColor = 0xF0100010;
            drawGradientRect(zLevel, tooltipX - 3, tooltipY - 4, tooltipX + tooltipTextWidth + 3, tooltipY - 3, backgroundColor, backgroundColor);
            drawGradientRect(zLevel, tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 4, backgroundColor, backgroundColor);
            drawGradientRect(zLevel, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
            drawGradientRect(zLevel, tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
            drawGradientRect(zLevel, tooltipX + tooltipTextWidth + 3, tooltipY - 3, tooltipX + tooltipTextWidth + 4, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
            //TODO: Coloured Borders
            int borderColorStart = 0x505000FF;
            if(NotEnoughUpdates.INSTANCE.config.tooltipTweaks.tooltipBorderColours && coloured) {
                if(textLines.size() > 0) {
                    String first = textLines.get(0);
                    borderColorStart = getPrimaryColour(first).getRGB() & 0x00FFFFFF |
                            ((NotEnoughUpdates.INSTANCE.config.tooltipTweaks.tooltipBorderOpacity) << 24);
                }
            }
            final int borderColorEnd = (borderColorStart & 0xFEFEFE) >> 1 | borderColorStart & 0xFF000000;
            drawGradientRect(zLevel, tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
            drawGradientRect(zLevel, tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
            drawGradientRect(zLevel, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY - 3 + 1, borderColorStart, borderColorStart);
            drawGradientRect(zLevel, tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, borderColorEnd, borderColorEnd);

            GlStateManager.disableDepth();
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

    public static void drawRectNoBlend(int left, int top, int right, int bottom, int color) {
        if (left < right) {
            int i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            int j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.disableTexture2D();
        GlStateManager.color(f, f1, f2, f3);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(left, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, top, 0.0D).endVertex();
        worldrenderer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    public static String prettyTime(long millis) {
        long seconds = millis / 1000 % 60;
        long minutes = (millis / 1000 / 60) % 60;
        long hours = (millis / 1000 / 60 / 60) % 24;
        long days = (millis / 1000 / 60 / 60 / 24);

        String endsIn = "";
        if(millis < 0) {
            endsIn += "Ended!";
        } else if(minutes == 0 && hours == 0 && days == 0) {
            endsIn += seconds + "s";
        } else if(hours==0 && days==0) {
            endsIn += minutes + "m" + seconds + "s";
        } else if(days==0) {
            if(hours <= 6) {
                endsIn += hours + "h" + minutes + "m" + seconds + "s";
            } else {
                endsIn += hours + "h";
            }
        } else {
            endsIn += days + "d" + hours + "h";
        }

        return endsIn;
    }

}
