package io.github.moulberry.notenoughupdates.infopanes;

import io.github.moulberry.notenoughupdates.BetterContainers;
import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.NEUOverlay;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
import io.github.moulberry.notenoughupdates.util.Utils;
import io.github.moulberry.notenoughupdates.itemeditor.GuiElementTextField;
import io.github.moulberry.notenoughupdates.options.Options;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import scala.tools.cmd.Spec;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static io.github.moulberry.notenoughupdates.GuiTextures.*;

public class SettingsInfoPane extends InfoPane {

    private final Map<Options.Option<?>, GuiElementTextField> textConfigMap = new HashMap<>();
    private int page = 0;
    private int maxPages = 1;

    private Options.Option clickedSlider = null;
    private int clickedSliderX = 0;
    private float clickedSliderMult = 0;

    private static final int colourEditorBG = new Color(80, 80, 80, 220).getRGB();
    private static ResourceLocation colourPickerLocation = new ResourceLocation("notenoughupdates:dynamic/colourpicker");
    private static ResourceLocation colourPickerBarValueLocation = new ResourceLocation("notenoughupdates:dynamic/colourpickervalue");
    private static ResourceLocation colourPickerBarOpacityLocation = new ResourceLocation("notenoughupdates:dynamic/colourpickeropacity");

    private GuiElementTextField hexField = new GuiElementTextField("",
            GuiElementTextField.SCALE_TEXT | GuiElementTextField.FORCE_CAPS | GuiElementTextField.NO_SPACE);
    private ColourEditor activeColourEditor = null;

    private class ColourEditor {
        public int x;
        public int y;
        public Options.Option<String> option;
        public String special;

        public ColourEditor(int x, int y, Options.Option<String> option, String special) {
            this.x = x;
            this.y = y;
            this.option = option;
            this.special = special;
        }
    }

    public SettingsInfoPane(NEUOverlay overlay, NEUManager manager) {
        super(overlay, manager);
    }

    public void reset() {
        textConfigMap.clear();
        activeColourEditor = null;
        hexField.otherComponentClick();
    }

    private void showColourEditor(int mouseX, int mouseY, Options.Option<String> option, String special) {
        activeColourEditor = new ColourEditor(mouseX, mouseY, option, special);
        hexField.otherComponentClick();
    }

    public void render(int width, int height, Color bg, Color fg, ScaledResolution scaledresolution, int mouseX,
                       int mouseY) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

        int paneWidth = (int)(width/3*overlay.getWidthMult());
        int rightSide = (int)(width*overlay.getInfoPaneOffsetFactor());
        int leftSide = rightSide - paneWidth;

        this.renderDefaultBackground(width, height, bg);

        if(page > maxPages-1) page = maxPages-1;
        if(page < 0) page = 0;

        overlay.renderNavElement(leftSide+overlay.getBoxPadding(), rightSide-overlay.getBoxPadding(),
                maxPages,page+1,"Settings: ");

        AtomicReference<List<String>> textToDisplay = new AtomicReference<>(null);
        AtomicReference<GuiElementTextField> tfTop = new AtomicReference<>();
        AtomicInteger tfTopX = new AtomicInteger();
        AtomicInteger tfTopY = new AtomicInteger();
        iterateSettingTile(new SettingsTileConsumer() {
            public void consume(int x, int y, int tileWidth, int tileHeight, Options.Option<?> option, Options.Button button) {
                float mult = tileWidth/90f;

                drawRect(x, y, x+tileWidth, y+tileHeight, fg.getRGB());

                if(manager.config.hideApiKey.value && option==manager.config.apiKey) return;

                if(option == null) {
                    Utils.renderStringTrimWidth(button.displayName, fr, true, x+(int)(8*mult), y+(int)(8*mult),
                            tileWidth-(int)(16*mult), new Color(100,255,150).getRGB(), 3,
                            2f/scaledresolution.getScaleFactor());

                    GlStateManager.color(1f, 1f, 1f, 1f);
                    Minecraft.getMinecraft().getTextureManager().bindTexture(button_tex);
                    Utils.drawTexturedRect(x + tileWidth/2f - (int) (32 * mult), y + tileHeight - (int) (20 * mult), (int) (48 * mult), (int) (16 * mult));

                    Minecraft.getMinecraft().getTextureManager().bindTexture(help);
                    Utils.drawTexturedRect(x + tileWidth/2f + (int) (19 * mult), y + tileHeight - (int) (19 * mult), (int) (14 * mult), (int) (14 * mult));
                    GlStateManager.bindTexture(0);

                    if (mouseX > x + tileWidth / 2 + (int) (19 * mult) && mouseX < x + tileWidth / 2 + (int) (19 * mult) + (int) (14 * mult)) {
                        if (mouseY > y + tileHeight - (int) (19 * mult) && mouseY < y + tileHeight - (int) (19 * mult) + (int) (14 * mult)) {
                            List<String> textLines = new ArrayList<>();
                            textLines.add(button.displayName);
                            textLines.add(EnumChatFormatting.GRAY + button.desc);
                            textToDisplay.set(textLines);
                        }
                    }
                    return;
                }

                Utils.renderStringTrimWidth(option.displayName, fr, true, x+(int)(8*mult), y+(int)(8*mult),
                        tileWidth-(int)(16*mult), new Color(100,255,150).getRGB(), 3,
                        2f/scaledresolution.getScaleFactor());

                if(option.value instanceof Boolean) {
                    GlStateManager.color(1f, 1f, 1f, 1f);
                    Minecraft.getMinecraft().getTextureManager().bindTexture(((Boolean) option.value) ? on : off);
                    Utils.drawTexturedRect(x + tileWidth/2f - (int) (32 * mult), y + tileHeight - (int) (20 * mult), (int) (48 * mult), (int) (16 * mult));

                    Minecraft.getMinecraft().getTextureManager().bindTexture(help);
                    Utils.drawTexturedRect(x + tileWidth/2f + (int) (19 * mult), y + tileHeight - (int) (19 * mult), (int) (14 * mult), (int) (14 * mult));
                    GlStateManager.bindTexture(0);

                    if (mouseX > x + tileWidth / 2 + (int) (19 * mult) && mouseX < x + tileWidth / 2 + (int) (19 * mult) + (int) (14 * mult)) {
                        if (mouseY > y + tileHeight - (int) (19 * mult) && mouseY < y + tileHeight - (int) (19 * mult) + (int) (14 * mult)) {
                            List<String> textLines = new ArrayList<>();
                            textLines.add(option.displayName);
                            textLines.add(EnumChatFormatting.GRAY + option.desc);
                            textToDisplay.set(textLines);
                        }
                    }
                } else if(option.value instanceof Double) {
                    if(!textConfigMap.containsKey(option)) {
                        textConfigMap.put(option, new GuiElementTextField(String.valueOf(option.value),
                                GuiElementTextField.NUM_ONLY | GuiElementTextField.NO_SPACE | GuiElementTextField.SCALE_TEXT));
                    }

                    GuiElementTextField tf = textConfigMap.get(option);
                    if(tf.getText().trim().endsWith(".0")) {
                        tf.setText(tf.getText().trim().substring(0, tf.getText().trim().length()-2));
                    }
                    if(tf.getFocus()) {
                        tf.setSize(Math.max((int)(20*mult), fr.getStringWidth(tf.getText())+10), (int)(16*mult));
                        tfTop.set(tf);
                        tfTopX.set(x+(int)(65*mult));
                        tfTopY.set(y+tileHeight-(int)(20*mult));
                    } else {
                        tf.setSize((int)(20*mult), (int)(16*mult));
                        tf.render(x+(int)(65*mult), y+tileHeight-(int)(20*mult));
                    }

                    double sliderAmount = (((Options.Option<Double>)option).value-option.minValue)/(option.maxValue-option.minValue);
                    sliderAmount = Math.max(0, Math.min(1, sliderAmount));

                    GlStateManager.color(1f, 1f, 1f, 1f);
                    Minecraft.getMinecraft().getTextureManager().bindTexture(slider_on);
                    Utils.drawTexturedRect(x+5*mult, y + tileHeight-20*mult, (float)(54*mult*sliderAmount), 16*mult,
                            0, (float)sliderAmount, 0, 1);

                    Minecraft.getMinecraft().getTextureManager().bindTexture(slider_off);
                    Utils.drawTexturedRect((float)(x+5*mult+54*mult*sliderAmount), y + tileHeight - 20*mult,
                            (float)(54*mult*(1-sliderAmount)), 16*mult,
                            (float)(sliderAmount), 1, 0, 1);

                    Minecraft.getMinecraft().getTextureManager().bindTexture(slider_button);
                    Utils.drawTexturedRect(x+1*mult+(float)(54*sliderAmount*mult), y + tileHeight - 20*mult,
                            8*mult, 16*mult);
                } else {
                    if((option.flags & Options.FLAG_COLOUR) != 0) {
                        Utils.renderStringTrimWidth(option.displayName, fr, true, x+(int)(8*mult), y+(int)(8*mult),
                                tileWidth-(int)(16*mult), new Color(100,255,150).getRGB(), 3,
                                2f/scaledresolution.getScaleFactor());

                        Color c = new Color(SpecialColour.specialToChromaRGB((String)option.value));
                        GlStateManager.color(
                                Math.min(1f, c.getRed()/255f),
                                Math.min(1f, c.getGreen()/255f),
                                Math.min(1f, c.getBlue()/255f), 1f);
                        Minecraft.getMinecraft().getTextureManager().bindTexture(button_white);
                        Utils.drawTexturedRect(x + tileWidth/2f - (int) (32 * mult), y + tileHeight - (int) (20 * mult), (int) (48 * mult), (int) (16 * mult));

                        GlStateManager.color(1, 1, 1, 1);
                        Minecraft.getMinecraft().getTextureManager().bindTexture(help);
                        Utils.drawTexturedRect(x + tileWidth/2f + (int) (19 * mult), y + tileHeight - (int) (19 * mult), (int) (14 * mult), (int) (14 * mult));
                        GlStateManager.bindTexture(0);

                        if (mouseX > x + tileWidth / 2 + (int) (19 * mult) && mouseX < x + tileWidth / 2 + (int) (19 * mult) + (int) (14 * mult)) {
                            if (mouseY > y + tileHeight - (int) (19 * mult) && mouseY < y + tileHeight - (int) (19 * mult) + (int) (14 * mult)) {
                                List<String> textLines = new ArrayList<>();
                                textLines.add(option.displayName);
                                textLines.add(EnumChatFormatting.GRAY + option.desc);
                                textToDisplay.set(textLines);
                            }
                        }
                    } else {
                        if(!textConfigMap.containsKey(option)) {
                            textConfigMap.put(option, new GuiElementTextField(String.valueOf(option.value), 0));
                        }
                        GuiElementTextField tf = textConfigMap.get(option);
                        if(tf.getFocus()) {
                            tf.setSize(Math.max(tileWidth-(int)(20*mult), fr.getStringWidth(tf.getText())+10), (int)(16*mult));
                            tfTop.set(tf);
                            tfTopX.set(x+(int)(10*mult));
                            tfTopY.set(y+tileHeight-(int)(20*mult));
                        } else {
                            tf.setSize(tileWidth-(int)(20*mult), (int)(16*mult));
                            tf.render(x+(int)(10*mult), y+tileHeight-(int)(20*mult));
                        }
                    }
                }
            }
        });
        if(tfTop.get() != null) {
            tfTop.get().render(tfTopX.get(), tfTopY.get());
        }

        if(activeColourEditor != null) {
            Gui.drawRect(activeColourEditor.x, activeColourEditor.y, activeColourEditor.x+119, activeColourEditor.y+89, colourEditorBG);

            int currentColour = SpecialColour.specialToSimpleRGB(activeColourEditor.special);
            Color c = new Color(currentColour, true);
            float[] hsv = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);

            BufferedImage bufferedImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            for(int x=0; x<256; x++) {
                for(int y=0; y<256; y++) {
                    float radius = (float) Math.sqrt(((x-128)*(x-128)+(y-128)*(y-128))/16384f);
                    float angle = (float) Math.toDegrees(Math.atan((128-x)/(y-128+1E-5))+Math.PI/2);
                    if(y < 128) angle += 180;
                    if(radius <= 1) {
                        int rgb = Color.getHSBColor(angle/360f, (float)Math.pow(radius, 1.5f), hsv[2]).getRGB();
                        bufferedImage.setRGB(x, y, rgb);
                    }
                }
            }

            BufferedImage bufferedImageValue = new BufferedImage(10, 64, BufferedImage.TYPE_INT_ARGB);
            for(int x=0; x<10; x++) {
                for(int y=0; y<64; y++) {
                    if((x == 0 || x == 9) && (y == 0 || y == 63)) continue;

                    int rgb = Color.getHSBColor(hsv[0], hsv[1], (64-y)/64f).getRGB();
                    bufferedImageValue.setRGB(x, y, rgb);
                }
            }

            BufferedImage bufferedImageOpacity = new BufferedImage(10, 64, BufferedImage.TYPE_INT_ARGB);
            for(int x=0; x<10; x++) {
                for(int y=0; y<64; y++) {
                    if((x == 0 || x == 9) && (y == 0 || y == 63)) continue;

                    int rgb = (currentColour & 0x00FFFFFF) | (Math.min(255, (64-y)*4) << 24);
                    bufferedImageOpacity.setRGB(x, y, rgb);
                }
            }

            float selradius = (float) Math.pow(hsv[1], 1/1.5f)*32;
            int selx = (int)(Math.cos(Math.toRadians(hsv[0]*360))*selradius);
            int sely = (int)(Math.sin(Math.toRadians(hsv[0]*360))*selradius);

            Minecraft.getMinecraft().getTextureManager().bindTexture(colour_selector_bar_alpha);
            GlStateManager.color(1, 1, 1, 1);
            Utils.drawTexturedRect(activeColourEditor.x+5+64+5+10+5, activeColourEditor.y+5, 10, 64, GL11.GL_NEAREST);

            Minecraft.getMinecraft().getTextureManager().loadTexture(colourPickerBarValueLocation, new DynamicTexture(bufferedImageValue));
            Minecraft.getMinecraft().getTextureManager().bindTexture(colourPickerBarValueLocation);
            GlStateManager.color(1, 1, 1, 1);
            Utils.drawTexturedRect(activeColourEditor.x+5+64+5, activeColourEditor.y+5, 10, 64, GL11.GL_NEAREST);

            Minecraft.getMinecraft().getTextureManager().loadTexture(colourPickerBarOpacityLocation, new DynamicTexture(bufferedImageOpacity));
            Minecraft.getMinecraft().getTextureManager().bindTexture(colourPickerBarOpacityLocation);
            GlStateManager.color(1, 1, 1, 1);
            Utils.drawTexturedRect(activeColourEditor.x+5+64+5+10+5, activeColourEditor.y+5, 10, 64, GL11.GL_NEAREST);

            int chromaSpeed = SpecialColour.getSpeed(activeColourEditor.special);
            int currentColourChroma = SpecialColour.specialToChromaRGB(activeColourEditor.special);
            Color cChroma = new Color(currentColourChroma, true);
            float hsvChroma[] = Color.RGBtoHSB(cChroma.getRed(), cChroma.getGreen(), cChroma.getBlue(), null);

            if(chromaSpeed > 0) {
                Gui.drawRect(activeColourEditor.x+5+64+5+10+5+10+5+1, activeColourEditor.y+5+1,
                        activeColourEditor.x+5+64+5+10+5+10+5+10-1, activeColourEditor.y+5+64-1,
                        Color.HSBtoRGB(hsvChroma[0], 0.8f, 0.8f));
            } else {
                Gui.drawRect(activeColourEditor.x+5+64+5+10+5+10+5+1, activeColourEditor.y+5+27+1,
                        activeColourEditor.x+5+64+5+10+5+10+5+10-1, activeColourEditor.y+5+37-1,
                        Color.HSBtoRGB((hsvChroma[0]+(System.currentTimeMillis()-SpecialColour.startTime)/1000f)%1, 0.8f, 0.8f));
            }

            Minecraft.getMinecraft().getTextureManager().bindTexture(colour_selector_bar);
            GlStateManager.color(1, 1, 1, 1);
            Utils.drawTexturedRect(activeColourEditor.x+5+64+5, activeColourEditor.y+5, 10, 64, GL11.GL_NEAREST);
            Utils.drawTexturedRect(activeColourEditor.x+5+64+5+10+5, activeColourEditor.y+5, 10, 64, GL11.GL_NEAREST);

            if(chromaSpeed > 0) {
                Utils.drawTexturedRect(activeColourEditor.x+5+64+5+10+5+10+5, activeColourEditor.y+5, 10, 64, GL11.GL_NEAREST);
            } else {
                Minecraft.getMinecraft().getTextureManager().bindTexture(colour_selector_chroma);
                Utils.drawTexturedRect(activeColourEditor.x+5+64+5+10+5+10+5, activeColourEditor.y+5+27, 10, 10, GL11.GL_NEAREST);
            }

            Gui.drawRect(activeColourEditor.x+5+64+5, activeColourEditor.y+5+64-(int)(64*hsv[2]),
                            activeColourEditor.x+5+64+5+10, activeColourEditor.y+5+64-(int)(64*hsv[2])+1, 0xFF000000);
            Gui.drawRect(activeColourEditor.x+5+64+5+10+5, activeColourEditor.y+5+64-c.getAlpha()/4,
                    activeColourEditor.x+5+64+5+10+5+10, activeColourEditor.y+5+64-c.getAlpha()/4-1, 0xFF000000);
            if(chromaSpeed > 0) {
                Gui.drawRect(activeColourEditor.x+5+64+5+10+5+10+5,
                        activeColourEditor.y+5+64-(int)(chromaSpeed/255f*64),
                        activeColourEditor.x+5+64+5+10+5+10+5+10,
                        activeColourEditor.y+5+64-(int)(chromaSpeed/255f*64)+1, 0xFF000000);
            }

            Minecraft.getMinecraft().getTextureManager().loadTexture(colourPickerLocation, new DynamicTexture(bufferedImage));
            Minecraft.getMinecraft().getTextureManager().bindTexture(colourPickerLocation);
            GlStateManager.color(1, 1, 1, 1);
            Utils.drawTexturedRect(activeColourEditor.x+5, activeColourEditor.y+5, 64, 64, GL11.GL_NEAREST);

            Minecraft.getMinecraft().getTextureManager().bindTexture(colour_selector_dot);
            GlStateManager.color(1, 1, 1, 1);
            Utils.drawTexturedRect(activeColourEditor.x+5+32+selx-4, activeColourEditor.y+5+32+sely-4, 8, 8, GL11.GL_NEAREST);

            Utils.drawStringCenteredScaledMaxWidth(EnumChatFormatting.GRAY.toString()+Math.round(hsv[2]*100)+"",
                    Minecraft.getMinecraft().fontRendererObj,
                    activeColourEditor.x+5+64+5+5-(Math.round(hsv[2]*100)==100?1:0), activeColourEditor.y+5+64+5+5, true, 13, -1);
            Utils.drawStringCenteredScaledMaxWidth(EnumChatFormatting.GRAY.toString()+Math.round(c.getAlpha()/255f*100)+"",
                    Minecraft.getMinecraft().fontRendererObj,
                    activeColourEditor.x+5+64+5+15+5, activeColourEditor.y+5+64+5+5, true, 13, -1);
            if(chromaSpeed > 0) {
                Utils.drawStringCenteredScaledMaxWidth(EnumChatFormatting.GRAY.toString()+(int)SpecialColour.getSecondsForSpeed(chromaSpeed)+"s",
                        Minecraft.getMinecraft().fontRendererObj,
                        activeColourEditor.x+5+64+5+30+6, activeColourEditor.y+5+64+5+5, true, 13, -1);
            }

            hexField.setSize(48, 10);
            if(!hexField.getFocus()) hexField.setText(Integer.toHexString(c.getRGB() & 0xFFFFFF).toUpperCase());

            StringBuilder sb = new StringBuilder(EnumChatFormatting.GRAY+"#");
            for(int i=0; i<6-hexField.getText().length(); i++) {
                sb.append("0");
            }
            sb.append(EnumChatFormatting.WHITE);

            hexField.setPrependText(sb.toString());
            hexField.render(activeColourEditor.x+5+8, activeColourEditor.y+5+64+5);
        }

        if(textToDisplay.get() != null) {
            Utils.drawHoveringText(textToDisplay.get(), mouseX, mouseY, width, height, 200, fr);
        }
    }

    private void onTextfieldChange(GuiElementTextField tf, Options.Option<?> option) {
        try {
            tf.setCustomBorderColour(-1);
            option.setValue(tf.getText());
            overlay.redrawItems();
            BetterContainers.reset();
        } catch(Exception e) {
            tf.setCustomBorderColour(Color.RED.getRGB());
        }
    }

    @Override
    public void mouseInputOutside() {
        for(GuiElementTextField tf : textConfigMap.values()) {
            tf.otherComponentClick();
        }
        activeColourEditor = null;
        hexField.otherComponentClick();
    }

    public void mouseInput(int width, int height, int mouseX, int mouseY, boolean mouseDown) {
        if(activeColourEditor != null && (Mouse.isButtonDown(0) || Mouse.isButtonDown(1))) {
            if(mouseX >= activeColourEditor.x && mouseX <= activeColourEditor.x+119) {
                if(mouseY >= activeColourEditor.y && mouseY <= activeColourEditor.y+89) {
                    if(Mouse.getEventButtonState()) {
                        if(mouseX > activeColourEditor.x+5+8 && mouseX < activeColourEditor.x+5+8+48) {
                            if(mouseY > activeColourEditor.y+5+64+5 && mouseY < activeColourEditor.y+5+64+5+10) {
                                hexField.mouseClicked(mouseX, mouseY, Mouse.getEventButton());
                                return;
                            }
                        }
                    }
                    hexField.otherComponentClick();

                    int currentColour = SpecialColour.specialToSimpleRGB(activeColourEditor.special);
                    Color c = new Color(currentColour, true);
                    float[] hsv = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);

                    int xWheel = mouseX - activeColourEditor.x - 5;
                    int yWheel = mouseY - activeColourEditor.y - 5;

                    if(xWheel > 0 && xWheel < 64) {
                        if(yWheel > 0 && yWheel < 64) {
                            float radius = (float) Math.sqrt(((xWheel-32)*(xWheel-32)+(yWheel-32)*(yWheel-32))/1024f);
                            float angle = (float) Math.toDegrees(Math.atan((32-xWheel)/(yWheel-32+1E-5))+Math.PI/2);
                            if(yWheel < 32) angle += 180;

                            int rgb = Color.getHSBColor(angle/360f, (float)Math.pow(Math.min(1, radius), 1.5f), hsv[2]).getRGB();
                            activeColourEditor.special = SpecialColour.special(SpecialColour.getSpeed(activeColourEditor.special), c.getAlpha(), rgb);
                            activeColourEditor.option.value = (String) activeColourEditor.special;
                        }
                    }

                    int xValue = mouseX - (activeColourEditor.x+5+64+5);
                    int y = mouseY - activeColourEditor.y - 5;

                    if(y > -5 && y <= 69) {
                        y = Math.max(0, Math.min(64, y));
                        if(xValue > 0 && xValue < 10) {
                            int rgb = Color.getHSBColor(hsv[0], hsv[1], 1-y/64f).getRGB();
                            activeColourEditor.special = SpecialColour.special(SpecialColour.getSpeed(activeColourEditor.special), c.getAlpha(), rgb);
                            activeColourEditor.option.value = activeColourEditor.special;
                        }

                        int xOpacity = mouseX - (activeColourEditor.x+5+64+5+10+5);

                        if(xOpacity > 0 && xOpacity < 10) {
                            activeColourEditor.special = SpecialColour.special(SpecialColour.getSpeed(activeColourEditor.special),
                                    255-(int)(y/64f*255), currentColour);
                            activeColourEditor.option.value = activeColourEditor.special;
                        }
                    }

                    int chromaSpeed = SpecialColour.getSpeed(activeColourEditor.special);

                    int xChroma = mouseX - (activeColourEditor.x+5+64+5+10+5+10+5);
                    if(xChroma > 0 && xChroma < 10) {
                        if(chromaSpeed > 0) {
                            if(y > -5 && y <= 69) {
                                y = Math.max(0, Math.min(64, y));
                                activeColourEditor.special = SpecialColour.special(255-Math.round(y/64f*255), c.getAlpha(), currentColour);
                                activeColourEditor.option.value =  activeColourEditor.special;
                            }
                        } else if(mouseY > activeColourEditor.y+5+27 && mouseY < activeColourEditor.y+5+37) {
                            activeColourEditor.special = SpecialColour.special(200, c.getAlpha(), currentColour);
                            activeColourEditor.option.value =  activeColourEditor.special;
                        }
                    }

                    return;
                }
            }
            if(Mouse.getEventButtonState()) activeColourEditor = null;
        }
        iterateSettingTile(new SettingsTileConsumer() {
            @Override
            public void consume(int x, int y, int tileWidth, int tileHeight, Options.Option<?> option, Options.Button button) {
                float mult = tileWidth/90f;
                if(option == null) {
                    if(Mouse.getEventButtonState()) {
                        if(mouseX > x+tileWidth/2-(int)(32*mult) && mouseX < x+tileWidth/2-(int)(32*mult)+(int)(48*mult)) {
                            if(mouseY > y+tileHeight-(int)(20*mult) && mouseY < y+tileHeight-(int)(20*mult)+(int)(16*mult)) {
                                button.click.run();
                                return;
                            }
                        }
                    }
                } else if(option.value instanceof Boolean) {
                    if(Mouse.getEventButtonState()) {
                        if(mouseX > x+tileWidth/2-(int)(32*mult) && mouseX < x+tileWidth/2-(int)(32*mult)+(int)(48*mult)) {
                            if(mouseY > y+tileHeight-(int)(20*mult) && mouseY < y+tileHeight-(int)(20*mult)+(int)(16*mult)) {
                                ((Options.Option<Boolean>)option).value = !((Boolean)option.value);
                                overlay.redrawItems();
                                return;
                            }
                        }
                    }
                } else if(option.value instanceof Double) {
                    if(!textConfigMap.containsKey(option)) {
                        textConfigMap.put(option, new GuiElementTextField(String.valueOf(option.value),
                                GuiElementTextField.NUM_ONLY | GuiElementTextField.NO_SPACE | GuiElementTextField.SCALE_TEXT));
                    }

                    GuiElementTextField tf = textConfigMap.get(option);
                    int tfX = x+(int)(65*mult);
                    int tfY = y+tileHeight-(int)(20*mult);
                    int tfWidth = tf.getWidth();
                    int tfHeight = tf.getHeight();
                    if(mouseY > tfY && mouseY < tfY+tfHeight) {
                        if(mouseX > tfX && mouseX < tfX+tfWidth) {
                            if(Mouse.getEventButtonState()) {
                                tf.mouseClicked(mouseX, mouseY, Mouse.getEventButton());
                                onTextfieldChange(tf, option);
                                return;
                            } else if(Mouse.getEventButton() == -1 && mouseDown) {
                                tf.mouseClickMove(mouseX, mouseY, 0, 0); //last 2 values are unused
                                return;
                            }
                        } else if(clickedSlider != option && Mouse.getEventButtonState() && mouseX > x+1*mult && mouseX < x+63*mult) {
                            clickedSlider = option;
                            clickedSliderX = x;
                            clickedSliderMult = mult;
                        }
                    }

                    if(clickedSlider == option) {
                        float xMin = clickedSliderX+5*clickedSliderMult;
                        float xMax = clickedSliderX+59*clickedSliderMult;

                        float sliderAmount = (mouseX - xMin)/(xMax - xMin);
                        sliderAmount = Math.max(0, Math.min(1, sliderAmount));

                        double range = option.maxValue - option.minValue;
                        double value = option.minValue + sliderAmount*range;

                        if(range >= 10) {
                            value = Math.round(value);
                        } else if(range >= 1) {
                            value = Math.round(value*10)/10.0;
                        } else {
                            value = Math.round(value*100)/100.0;
                        }

                        value = Math.max(option.minValue, Math.min(option.maxValue, value));

                        tf.setText(String.valueOf(value));
                        onTextfieldChange(tf, option);

                        if(Mouse.getEventButton() == 0 && !Mouse.getEventButtonState()) {
                            clickedSlider = null;
                        }
                    }

                    if(Mouse.getEventButtonState()) tf.otherComponentClick();
                } else {
                    if((option.flags & Options.FLAG_COLOUR) != 0) {
                        if(Mouse.getEventButtonState()) {
                            if(mouseX > x+tileWidth/2-(int)(32*mult) && mouseX < x+tileWidth/2-(int)(32*mult)+(int)(48*mult)) {
                                if(mouseY > y+tileHeight-(int)(20*mult) && mouseY < y+tileHeight-(int)(20*mult)+(int)(16*mult)) {
                                    int editorX = (int)Math.min(mouseX, width*overlay.getInfoPaneOffsetFactor()-129);
                                    int editorY = Math.min(mouseY, height-99);
                                    showColourEditor(editorX, editorY, (Options.Option<String>) option, (String)option.value);
                                    return;
                                }
                            }
                        }
                    } else {
                        if(!textConfigMap.containsKey(option)) {
                            textConfigMap.put(option, new GuiElementTextField(String.valueOf(option.value), 0));
                        }
                        GuiElementTextField tf = textConfigMap.get(option);
                        int tfX = x+(int)(10*mult);
                        int tfY = y+tileHeight-(int)(20*mult);
                        int tfWidth = tf.getWidth();
                        int tfHeight = tf.getHeight();
                        if(mouseX > tfX && mouseX < tfX+tfWidth) {
                            if(mouseY > tfY && mouseY < tfY+tfHeight) {
                                if(Mouse.getEventButtonState()) {
                                    tf.mouseClicked(mouseX, mouseY, Mouse.getEventButton());
                                    onTextfieldChange(tf, option);
                                    return;
                                } else if(Mouse.getEventButton() == -1 && mouseDown) {
                                    tf.mouseClickMove(mouseX, mouseY, 0, 0); //last 2 values are unused
                                    return;
                                }
                            }
                        }
                        if(Mouse.getEventButtonState()) tf.otherComponentClick();
                    }
                }
            }
        });

        if(Mouse.getEventButtonState()) {
            int paneWidth = (int)(width/3*overlay.getWidthMult());
            int rightSide = (int)(width*overlay.getInfoPaneOffsetFactor());
            int leftSide = rightSide - paneWidth;
            rightSide -= overlay.getBoxPadding();
            leftSide += overlay.getBoxPadding();

            FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
            float maxStrLen = fr.getStringWidth(EnumChatFormatting.BOLD+"Settings: " + maxPages + "/" + maxPages);
            float maxButtonXSize = (rightSide-leftSide+2 - maxStrLen*0.5f - 10)/2f;
            int buttonXSize = (int)Math.min(maxButtonXSize, overlay.getSearchBarYSize()*480/160f);
            int ySize = (int)(buttonXSize/480f*160);
            int yOffset = (int)((overlay.getSearchBarYSize()-ySize)/2f);
            int top = overlay.getBoxPadding()+yOffset;

            if(mouseY >= top && mouseY <= top+ySize) {
                int leftPrev = leftSide-1;
                if(mouseX > leftPrev && mouseX < leftPrev+buttonXSize) { //"Previous" button
                    page--;
                }
                int leftNext = rightSide+1-buttonXSize;
                if(mouseX > leftNext && mouseX < leftNext+buttonXSize) { //"Next" button
                    page++;
                }
            }
        }
    }

    public boolean keyboardInput() {
        AtomicBoolean ret = new AtomicBoolean(false);
        iterateSettingTile(new SettingsTileConsumer() {
            @Override
            public void consume(int x, int y, int tileWidth, int tileHeight, Options.Option<?> option, Options.Button button) {
                if(option == null) return;

                if(!textConfigMap.containsKey(option)) {
                    return;
                }
                GuiElementTextField tf = textConfigMap.get(option);

                if(!(option.value instanceof Boolean)) {
                    if(tf.getFocus()) {
                        tf.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
                        onTextfieldChange(tf, option);
                        ret.set(true);
                    }
                }
            }
        });
        if(activeColourEditor != null && hexField.getFocus()) {
            String old = hexField.getText();

            hexField.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());

            if(hexField.getText().length() > 6) {
                hexField.setText(old);
            } else {
                try {
                    String text = hexField.getText().toLowerCase();

                    int rgb = Integer.parseInt(text, 16);
                    int alpha = (SpecialColour.specialToSimpleRGB(activeColourEditor.special) >> 24) & 0xFF;
                    activeColourEditor.special = SpecialColour.special(SpecialColour.getSpeed(activeColourEditor.special), alpha, rgb);
                    activeColourEditor.option.value = activeColourEditor.special;
                } catch(Exception e) {};
            }
        }
        return ret.get();
    }

    private abstract static class SettingsTileConsumer {
        public abstract void consume(int x, int y, int tileWidth, int tileHeight, Options.Option<?> option, Options.Button button);
    }

    public void iterateSettingTile(SettingsTileConsumer settingsTileConsumer) {
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledresolution.getScaledWidth();
        int height = scaledresolution.getScaledHeight();

        int numHorz = scaledresolution.getScaleFactor() >= 3 ? 2 : 3;

        int paneWidth = (int)(width/3*overlay.getWidthMult());
        int rightSide = (int)(width*overlay.getInfoPaneOffsetFactor());
        int leftSide = rightSide - paneWidth;

        int boxLeft = leftSide+overlay.getBoxPadding()-5;
        int boxRight = rightSide-overlay.getBoxPadding()+5;

        int boxBottom = height - overlay.getBoxPadding() + 5;

        int boxWidth = boxRight-boxLeft;
        int tilePadding = 7;
        int tileWidth = (boxWidth-tilePadding*4)/numHorz;
        int tileHeight = tileWidth*3/4;

        maxPages=1;
        int currPage=0;
        int x=0;
        int y=tilePadding+overlay.getBoxPadding()+overlay.getSearchBarYSize();
        for(int i=0; i<manager.config.getOptions().size()+manager.config.getButtons().size(); i++) {
            if(i!=0 && i%numHorz==0) {
                x = 0;
                y += tileHeight+tilePadding;
            }
            if(y + tileHeight > boxBottom) {
                x=0;
                y=tilePadding+overlay.getBoxPadding()+overlay.getSearchBarYSize();
                currPage++;
                maxPages = currPage+1;
            }
            x+=tilePadding;

            if(currPage == page) {
                if(i < manager.config.getButtons().size()) {
                    settingsTileConsumer.consume(boxLeft+x, y, tileWidth, tileHeight,
                            null, manager.config.getButtons().get(i));
                } else {
                    settingsTileConsumer.consume(boxLeft+x, y, tileWidth, tileHeight,
                            manager.config.getOptions().get(i-manager.config.getButtons().size()), null);
                }
            }

            x+=tileWidth;
        }
    }
}
