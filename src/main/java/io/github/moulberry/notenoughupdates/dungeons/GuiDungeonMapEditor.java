package io.github.moulberry.notenoughupdates.dungeons;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.itemeditor.GuiElementTextField;
import io.github.moulberry.notenoughupdates.options.Options;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Matrix4f;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec4b;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import static io.github.moulberry.notenoughupdates.GuiTextures.*;

import static io.github.moulberry.notenoughupdates.GuiTextures.*;
import static io.github.moulberry.notenoughupdates.GuiTextures.colour_selector_dot;

public class GuiDungeonMapEditor extends GuiScreen {

    public static final ResourceLocation BACKGROUND = new ResourceLocation("notenoughupdates:dungeon_map/editor/background.png");
    public static final ResourceLocation BUTTON = new ResourceLocation("notenoughupdates:button.png");
    private static final DungeonMap demoMap = new DungeonMap();

    private int sizeX;
    private int sizeY;
    private int guiLeft;
    private int guiTop;

    private List<Button> buttons = new ArrayList<>();

    private static final int colourEditorBG = new Color(80, 80, 80, 220).getRGB();
    private static ResourceLocation colourPickerLocation = new ResourceLocation("notenoughupdates:dynamic/colourpicker");
    private static ResourceLocation colourPickerBarValueLocation = new ResourceLocation("notenoughupdates:dynamic/colourpickervalue");
    private static ResourceLocation colourPickerBarOpacityLocation = new ResourceLocation("notenoughupdates:dynamic/colourpickeropacity");

    private GuiElementTextField hexField = new GuiElementTextField("",
            GuiElementTextField.SCALE_TEXT | GuiElementTextField.FORCE_CAPS | GuiElementTextField.NO_SPACE);

    private GuiElementTextField xField = new GuiElementTextField("", GuiElementTextField.NUM_ONLY | GuiElementTextField.NO_SPACE);
    private GuiElementTextField yField = new GuiElementTextField("", GuiElementTextField.NUM_ONLY | GuiElementTextField.NO_SPACE);
    private GuiElementTextField blurField = new GuiElementTextField("", GuiElementTextField.NUM_ONLY | GuiElementTextField.NO_SPACE);
    private ColourEditor activeColourEditor = null;

    private Options.Option<Double> clickedSlider = null;

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

    class Button {
        private int id;
        private int x;
        private int y;
        private String text;
        private Color colour = new Color(-1, true);
        private Options.Option<?> option;

        public Button(int id, int x, int y, String text) {
            this(id, x, y, text, null);
        }

        public Button(int id, int x, int y, String text, Options.Option<?> option) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.text = text;
            this.option = option;
        }

        public List<String> getTooltip() {
            if(option == null) {
                return null;
            }

            List<String> tooltip = new ArrayList<>();
            tooltip.add(EnumChatFormatting.YELLOW+option.displayName);
            for(String line : option.desc.split("\n")) {
                tooltip.add(EnumChatFormatting.AQUA+line);
            }
            return tooltip;
        }

        public void render() {
            if(text == null) return;

            Minecraft.getMinecraft().getTextureManager().bindTexture(BUTTON);
            if(isButtonPressed(id)) {
                GlStateManager.color(colour.getRed()*0.85f/255f, colour.getGreen()*0.85f/255f,
                        colour.getBlue()*0.85f/255f, 1);
                Utils.drawTexturedRect(guiLeft+x, guiTop+y, 48, 16, 1, 0, 1, 0, GL11.GL_NEAREST);
            } else {
                GlStateManager.color(colour.getRed()/255f, colour.getGreen()/255f, colour.getBlue()/255f, 1);
                Utils.drawTexturedRect(guiLeft+x, guiTop+y, 48, 16, GL11.GL_NEAREST);
            }

            if(text.length() > 0) {
                Utils.drawStringCenteredScaledMaxWidth(text, Minecraft.getMinecraft().fontRendererObj , guiLeft+x+24, guiTop+y+8, false, 39, 0xFF000000);
            }
        }

    }

    public GuiDungeonMapEditor() {
        Options options = NotEnoughUpdates.INSTANCE.manager.config;
        //Map Border Size
        //buttons.add(new Button(0, 6, 37, "Small", options.dmBorderSize));
        //buttons.add(new Button(1, 52, 37, "Medium", options.dmBorderSize));
        //buttons.add(new Button(2, 98, 37, "Large", options.dmBorderSize));

        //Map Rooms Size
        //buttons.add(new Button(3, 6, 67+19, "Small", options.dmRoomSize));
        //buttons.add(new Button(4, 52, 67+19, "Medium", options.dmRoomSize));
        //buttons.add(new Button(5, 98, 67+19, "Large", options.dmRoomSize));

        //Map Border Styles
        buttons.add(new Button(6, 6, 97, "None"));
        buttons.add(new Button(7, 52, 97, "Custom"));
        buttons.add(new Button(8, 98, 97, "Stone"));
        buttons.add(new Button(9, 6, 116, "Wood"));
        buttons.add(new Button(10, 52, 116, "Rustic(S)"));
        buttons.add(new Button(11, 98, 116, "Rustic(C)"));
        buttons.add(new Button(12, 6, 135, "Fade"));
        buttons.add(new Button(13, 52, 135, "Ribbons"));
        buttons.add(new Button(14, 98, 135, "Paper"));
        buttons.add(new Button(15, 6, 154, "Crimson"));
        buttons.add(new Button(16, 52, 154, "Ornate"));
        buttons.add(new Button(17, 98, 154, "Dragon"));

        //Dungeon Map
        buttons.add(new Button(18, 20+139, 36, "Yes/No", options.dmEnable));
        //Center
        buttons.add(new Button(19, 84+139, 36, "Player/Map", options.dmCenterPlayer));
        //Rotate
        buttons.add(new Button(20, 20+139, 65, "Player/No Rotate", options.dmRotatePlayer));
        //Icon Style
        buttons.add(new Button(21, 84+139, 65, "Default/Heads", options.dmPlayerHeads));
        //Check Orient
        buttons.add(new Button(22, 20+139, 94, "Normal/Reorient", options.dmOrientCheck));
        //Check Center
        buttons.add(new Button(23, 84+139, 94, "Yes/No", options.dmCenterCheck));
        //Interpolation
        buttons.add(new Button(24, 20+139, 123, "Yes/No", options.dmPlayerInterp));
        //Compatibility
        buttons.add(new Button(25, 84+139, 123, "Normal/No SHD/No FB/SHD", options.dmCompat));

        //Background
        buttons.add(new Button(26, 20+139, 152, "", options.dmBackgroundColour));
        //Border
        buttons.add(new Button(27, 84+139, 152, "", options.dmBorderColour));

        //Chroma Mode
        buttons.add(new Button(28, 84+139, 181, "Normal/Scroll", options.dmChromaBorder));

        //buttons.add(new Button(29, 52, 86+19, "XLarge", options.dmRoomSize));
        //buttons.add(new Button(30, 52, 56, "XLarge", options.dmBorderSize));

        xField.setText(String.valueOf(NotEnoughUpdates.INSTANCE.manager.config.dmCenterX.value));
        yField.setText(String.valueOf(NotEnoughUpdates.INSTANCE.manager.config.dmCenterY.value));
        blurField.setText(String.valueOf(NotEnoughUpdates.INSTANCE.manager.config.dmBackgroundBlur.value));
    }

    private void showColourEditor(int mouseX, int mouseY, Options.Option<String> option, String special) {
        activeColourEditor = new ColourEditor(mouseX, mouseY, option, special);
        hexField.otherComponentClick();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution scaledResolution = Utils.pushGuiScale(2);
        this.width = scaledResolution.getScaledWidth();
        this.height = scaledResolution.getScaledHeight();

        mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        List<String> tooltipToDisplay = null;
        for(Button button : buttons) {
            if(mouseX >= guiLeft+button.x && mouseX <= guiLeft+button.x+48 &&
                    mouseY >= guiTop+button.y-13 && mouseY <= guiTop+button.y+16) {
                if(button.id >= 6 && button.id <= 17) {
                    String mapDesc = null;
                    String mapCredit = null;
                    int id = button.id;
                    switch(id) {
                        case 6:
                            mapDesc = "No Border"; break;
                        case 7:
                            mapDesc = "Used by custom Resource Packs"; break;
                        case 8:
                            mapDesc = "Simple gray border"; mapCredit = "TomEngMaster"; break;
                        case 9:
                            mapDesc = "Viney wood border"; mapCredit = "iDevil4Hell"; break;
                        case 10:
                            mapDesc = "Steampunk-inspired square border"; mapCredit = "ThatGravyBoat"; break;
                        case 11:
                            mapDesc = "Steampunk-inspired circular border"; mapCredit = "ThatGravyBoat"; break;
                        case 12:
                            mapDesc = "Light fade border"; mapCredit = "Qwiken"; break;
                        case 13:
                            mapDesc = "Simple gray border with red ribbons"; mapCredit = "Sai"; break;
                        case 14:
                            mapDesc = "Paper border"; mapCredit = "KingJames02st"; break;
                        case 15:
                            mapDesc = "Nether-inspired border"; mapCredit = "DTRW191"; break;
                        case 16:
                            mapDesc = "Golden ornate border"; mapCredit = "iDevil4Hell"; break;
                        case 17:
                            mapDesc = "Stone dragon border"; mapCredit = "ImperiaL"; break;
                    }

                    ArrayList<String> tooltip = new ArrayList<>();
                    tooltip.add(EnumChatFormatting.YELLOW+"Border Style");
                    tooltip.add(EnumChatFormatting.AQUA+"Customize the look of the dungeon border");
                    tooltip.add("");
                    if(mapDesc != null) tooltip.add(EnumChatFormatting.YELLOW+"Set to: "+EnumChatFormatting.AQUA+mapDesc);
                    if(mapCredit != null) tooltip.add(EnumChatFormatting.YELLOW+"Artist: "+EnumChatFormatting.GOLD+mapCredit);
                    tooltipToDisplay = tooltip;
                } else {
                    tooltipToDisplay = button.getTooltip();
                }
                break;
            }
        }

        this.sizeX = 431;
        this.sizeY = 237;
        this.guiLeft = (this.width - this.sizeX) / 2;
        this.guiTop = (this.height-this.sizeY)/2;

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawDefaultBackground();

        blurBackground();
        renderBlurredBackground(width, height, guiLeft+2, guiTop+2, sizeX-4, sizeY-4);

        Minecraft.getMinecraft().getTextureManager().bindTexture(BACKGROUND);
        GlStateManager.color(1, 1, 1, 1);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);

        Minecraft.getMinecraft().fontRendererObj.drawString("NEU Dungeon Map Editor", guiLeft+8, guiTop+6, 0xFFB4B4B4);

        Utils.drawStringCenteredScaledMaxWidth("Map Border Size", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+76, guiTop+30, false, 137, 0xFFB4B4B4);
        Utils.drawStringCenteredScaledMaxWidth("Map Rooms Size", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+76, guiTop+60, false, 137, 0xFFB4B4B4);
        Utils.drawStringCenteredScaledMaxWidth("Map Border Style", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+76, guiTop+90, false, 137, 0xFFB4B4B4);

        Utils.drawStringCenteredScaledMaxWidth("Dungeon Map", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+44+139, guiTop+30, false, 60, 0xFFB4B4B4);
        Utils.drawStringCenteredScaledMaxWidth("Center", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+108+139, guiTop+30, false, 60, 0xFFB4B4B4);

        Utils.drawStringCenteredScaledMaxWidth("Rotate", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+44+139, guiTop+59, false, 60, 0xFFB4B4B4);
        Utils.drawStringCenteredScaledMaxWidth("Icon Style", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+108+139, guiTop+59, false, 60, 0xFFB4B4B4);

        Utils.drawStringCenteredScaledMaxWidth("Check Orient", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+44+139, guiTop+88, false, 60, 0xFFB4B4B4);
        Utils.drawStringCenteredScaledMaxWidth("Check Center", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+108+139, guiTop+88, false, 60, 0xFFB4B4B4);

        Utils.drawStringCenteredScaledMaxWidth("Interpolation", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+44+139, guiTop+117, false, 60, 0xFFB4B4B4);
        Utils.drawStringCenteredScaledMaxWidth("Compatibility", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+108+139, guiTop+117, false, 60, 0xFFB4B4B4);

        Utils.drawStringCenteredScaledMaxWidth("Background", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+44+139, guiTop+146, false, 60, 0xFFB4B4B4);
        Utils.drawStringCenteredScaledMaxWidth("Border", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+108+139, guiTop+146, false, 60, 0xFFB4B4B4);

        Utils.drawStringCenteredScaledMaxWidth("BG Blur", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+44+139, guiTop+175, false, 60, 0xFFB4B4B4);
        Utils.drawStringCenteredScaledMaxWidth("Chroma Type", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+108+139, guiTop+175, false, 60, 0xFFB4B4B4);

        Utils.drawStringCenteredScaledMaxWidth("X (%)", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+44, guiTop+189, false, 60, 0xFFB4B4B4);
        Utils.drawStringCenteredScaledMaxWidth("Y (%)", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+108, guiTop+189, false, 60, 0xFFB4B4B4);

        drawSlider(NotEnoughUpdates.INSTANCE.manager.config.dmBorderSize, guiLeft+76, guiTop+45);
        drawSlider(NotEnoughUpdates.INSTANCE.manager.config.dmRoomSize, guiLeft+76, guiTop+75);

        Options options = NotEnoughUpdates.INSTANCE.manager.config;
        buttons.get(18-6).text = options.dmEnable.value ? "Enabled" : "Disabled";
        buttons.get(19-6).text = options.dmCenterPlayer.value ? "Player" : "Map";
        buttons.get(20-6).text = options.dmRotatePlayer.value ? "Player" : "Vertical";
        buttons.get(21-6).text = options.dmPlayerHeads.value <= 0 ? "Default" : options.dmPlayerHeads.value >= 3 ? "SmallHeads" :
                options.dmPlayerHeads.value == 1 ? "Heads" : "ScaledHeads";
        buttons.get(22-6).text = options.dmOrientCheck.value ? "Orient" : "Off";
        buttons.get(23-6).text = options.dmCenterCheck.value ? "Center" : "Off";
        buttons.get(24-6).text = options.dmPlayerInterp.value ? "Interp" : "No Interp";
        buttons.get(25-6).text = options.dmCompat.value <= 0 ? "Normal" : options.dmCompat.value >= 2 ? "No FB/SHD" : "No SHD";

        buttons.get(26-6).colour = new Color(SpecialColour.specialToChromaRGB(options.dmBackgroundColour.value));
        buttons.get(27-6).colour = new Color(SpecialColour.specialToChromaRGB(options.dmBorderColour.value));

        buttons.get(28-6).text = options.dmChromaBorder.value ? "Scroll" : "Normal";

        blurField.setSize(48, 16);
        xField.setSize(48, 16);
        yField.setSize(48, 16);
        blurField.render(guiLeft+20+139, guiTop+181);
        xField.render(guiLeft+20, guiTop+195);
        yField.render(guiLeft+84, guiTop+195);

        Map<String, Vec4b> decorations = new HashMap<>();
        Vec4b vec4b = new Vec4b((byte)3, (byte)(((50)-64)*2), (byte)(((40)-64)*2), (byte)((60)*16/360));
        decorations.put(Minecraft.getMinecraft().thePlayer.getName(), vec4b);

        HashSet<String> players = new HashSet<>();
        players.add(Minecraft.getMinecraft().thePlayer.getName());
        GlStateManager.color(1, 1, 1, 1);

        demoMap.renderMap(guiLeft+357, guiTop+125, NotEnoughUpdates.INSTANCE.colourMap, decorations, 0,
                players, false, partialTicks);

        for(Button button : buttons) {
            button.render();
        }

        //List<String> textLines, final int mouseX, final int mouseY, final int screenWidth, final int screenHeight, final int maxTextWidth, FontRenderer font
        if(tooltipToDisplay != null) {
            Utils.drawHoveringText(tooltipToDisplay, mouseX, mouseY, width, height, 200, Minecraft.getMinecraft().fontRendererObj);
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

        Utils.pushGuiScale(-1);
    }

    public void drawSlider(Options.Option<Double> option, int centerX, int centerY) {
        float sliderAmount = (float)Math.max(0, Math.min(1, (option.value-option.minValue)/(option.maxValue-option.minValue)));
        int sliderAmountI = (int)(96*sliderAmount);

        GlStateManager.color(1f, 1f, 1f, 1f);
        Minecraft.getMinecraft().getTextureManager().bindTexture(slider_on_large);
        Utils.drawTexturedRect(centerX-48, centerY-8, sliderAmountI, 16,
                0, sliderAmount, 0, 1, GL11.GL_NEAREST);

        Minecraft.getMinecraft().getTextureManager().bindTexture(slider_off_large);
        Utils.drawTexturedRect(centerX-48+sliderAmountI, centerY-8, 96-sliderAmountI, 16,
                sliderAmount, 1, 0, 1, GL11.GL_NEAREST);

        Minecraft.getMinecraft().getTextureManager().bindTexture(slider_button);
        Utils.drawTexturedRect(centerX-48+sliderAmountI-4, centerY-8, 8, 16,
                0, 1, 0, 1, GL11.GL_NEAREST);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

        if(clickedSlider != null) {
            float sliderAmount = (mouseX - (guiLeft+76-48))/96f;
            double val = clickedSlider.minValue+(clickedSlider.maxValue-clickedSlider.minValue)*sliderAmount;
            if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                val = Math.round(val);
            }
            clickedSlider.value = Math.max(clickedSlider.minValue, Math.min(clickedSlider.maxValue, val));
        }

    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for(Button button : buttons) {
            if(mouseX >= guiLeft+button.x && mouseX <= guiLeft+button.x+48 &&
                    mouseY >= guiTop+button.y && mouseY <= guiTop+button.y+16) {
                buttonClicked(mouseX, mouseY, button.id);

                xField.otherComponentClick();
                yField.otherComponentClick();
                blurField.otherComponentClick();
                return;
            }
        }

        clickedSlider = null;
        if(mouseX >= guiLeft+76-48 && mouseX <= guiLeft+76+48) {
            if(mouseY > guiTop+45-8 && mouseY < guiTop+45+8) {
                clickedSlider = NotEnoughUpdates.INSTANCE.manager.config.dmBorderSize;
                return;
            } else if(mouseY > guiTop+75-8 && mouseY < guiTop+75+8) {
                clickedSlider = NotEnoughUpdates.INSTANCE.manager.config.dmRoomSize;
                return;
            }
        }

        if(mouseY > guiTop+181 && mouseY < guiTop+181+16) {
            if(mouseX > guiLeft+20+139 && mouseX < guiLeft+20+139+48) {
                blurField.mouseClicked(mouseX, mouseY, mouseButton);
                xField.otherComponentClick();
                yField.otherComponentClick();
                return;
            }
        } else if(mouseY > guiTop+195 && mouseY < guiTop+195+16) {
            if(mouseX > guiLeft+20 && mouseX < guiLeft+20+48) {
                xField.mouseClicked(mouseX, mouseY, mouseButton);
                yField.otherComponentClick();
                blurField.otherComponentClick();
                return;
            } else if(mouseX > guiLeft+84 && mouseX < guiLeft+84+48) {
                yField.mouseClicked(mouseX, mouseY, mouseButton);
                xField.otherComponentClick();
                blurField.otherComponentClick();
                return;
            }
        }

        blurField.otherComponentClick();
        xField.otherComponentClick();
        yField.otherComponentClick();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if(activeColourEditor != null && (Mouse.isButtonDown(0) || Mouse.isButtonDown(1))) {
            if(mouseX >= activeColourEditor.x && mouseX <= activeColourEditor.x+119) {
                if(mouseY >= activeColourEditor.y && mouseY <= activeColourEditor.y+89) {
                    if(Mouse.getEventButtonState()) {
                        if(mouseX > activeColourEditor.x+5+8 && mouseX < activeColourEditor.x+5+8+48) {
                            if(mouseY > activeColourEditor.y+5+64+5 && mouseY < activeColourEditor.y+5+64+5+10) {
                                hexField.mouseClicked(mouseX, mouseY, Mouse.getEventButton());
                                Utils.pushGuiScale(-1);
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

                    try { NotEnoughUpdates.INSTANCE.manager.saveConfig(); } catch(IOException ignored) {}
                    return;
                }
            }
            if(Mouse.getEventButtonState()) {
                activeColourEditor = null;
                hexField.otherComponentClick();
            }
        }
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();

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
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);

        if(xField.getFocus()) {
            xField.keyTyped(typedChar, keyCode);

            try {
                xField.setCustomBorderColour(-1);
                NotEnoughUpdates.INSTANCE.manager.config.dmCenterX.setValue(xField.getText());
                try { NotEnoughUpdates.INSTANCE.manager.saveConfig(); } catch(IOException ignored) {}
            } catch(Exception e) {
                xField.setCustomBorderColour(Color.RED.getRGB());
            }
        } else if(yField.getFocus()) {
            yField.keyTyped(typedChar, keyCode);

            try {
                yField.setCustomBorderColour(-1);
                NotEnoughUpdates.INSTANCE.manager.config.dmCenterY.setValue(yField.getText());
                try { NotEnoughUpdates.INSTANCE.manager.saveConfig(); } catch(IOException ignored) {}
            } catch(Exception e) {
                yField.setCustomBorderColour(Color.RED.getRGB());
            }
        }  else if(blurField.getFocus()) {
            blurField.keyTyped(typedChar, keyCode);

            try {
                blurField.setCustomBorderColour(-1);
                NotEnoughUpdates.INSTANCE.manager.config.dmBackgroundBlur.setValue(blurField.getText());
                try { NotEnoughUpdates.INSTANCE.manager.saveConfig(); } catch(IOException ignored) {}
            } catch(Exception e) {
                blurField.setCustomBorderColour(Color.RED.getRGB());
            }
        }
    }

    private void buttonClicked(int mouseX, int mouseY, int id) {
        Options options = NotEnoughUpdates.INSTANCE.manager.config;
        switch (id) {
            case 0:
                options.dmBorderSize.value = 0.0; break;
            case 1:
                options.dmBorderSize.value = 1.0; break;
            case 2:
                options.dmBorderSize.value = 2.0; break;
            case 30:
                options.dmBorderSize.value = 3.0; break;
            case 3:
                options.dmRoomSize.value = 0.0; break;
            case 4:
                options.dmRoomSize.value = 1.0; break;
            case 5:
                options.dmRoomSize.value = 2.0; break;
            case 29:
                options.dmRoomSize.value = 3.0; break;
            case 18:
                options.dmEnable.value = !options.dmEnable.value; break;
            case 19:
                options.dmCenterPlayer.value = !options.dmCenterPlayer.value; break;
            case 20:
                options.dmRotatePlayer.value = !options.dmRotatePlayer.value; break;
            case 21:
                options.dmPlayerHeads.value++;
                if(options.dmPlayerHeads.value > 3) options.dmPlayerHeads.value = 0.0;break;
            case 22:
                options.dmOrientCheck.value = !options.dmOrientCheck.value; break;
            case 23:
                options.dmCenterCheck.value = !options.dmCenterCheck.value; break;
            case 24:
                options.dmPlayerInterp.value = !options.dmPlayerInterp.value; break;
            case 25:
                options.dmCompat.value++;
                if(options.dmCompat.value > 2) options.dmCompat.value = 0.0;
                break;
            case 26:
                showColourEditor(mouseX, mouseY, options.dmBackgroundColour, options.dmBackgroundColour.value); break;
            case 27:
                showColourEditor(mouseX, mouseY, options.dmBorderColour, options.dmBorderColour.value); break;
            case 28:
                options.dmChromaBorder.value = !options.dmChromaBorder.value; break;
            default:
                if(id >= 6 && id <= 17) {
                    options.dmBorderStyle.value = (double)id-6; break;
                }
        }
        try { NotEnoughUpdates.INSTANCE.manager.saveConfig(); } catch(IOException ignored) {};

    }

    private boolean isButtonPressed(int id) {
        Options options = NotEnoughUpdates.INSTANCE.manager.config;

        if(id >= 0 && id <= 2) {
            return options.dmBorderSize.value == id;
        } else if(id >= 3 && id <= 5) {
            return options.dmRoomSize.value == id-3;
        } else if(id >= 6 && id <= 17) {
            return options.dmBorderStyle.value == id-6;
        } else if(id == 29) {
            return options.dmRoomSize.value == 3;
        } else if(id == 30) {
            return options.dmBorderSize.value == 3;
        }
        return false;
    }

    Shader blurShaderHorz = null;
    Framebuffer blurOutputHorz = null;
    Shader blurShaderVert = null;
    Framebuffer blurOutputVert = null;

    /**
     * Creates a projection matrix that projects from our coordinate space [0->width; 0->height] to OpenGL coordinate
     * space [-1 -> 1; 1 -> -1] (Note: flipped y-axis).
     *
     * This is so that we can render to and from the framebuffer in a way that is familiar to us, instead of needing to
     * apply scales and translations manually.
     */
    private Matrix4f createProjectionMatrix(int width, int height) {
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

    private double lastBgBlurFactor = -1;
    private void blurBackground() {
        if(!OpenGlHelper.isFramebufferEnabled()) return;

        int width = Minecraft.getMinecraft().displayWidth;
        int height = Minecraft.getMinecraft().displayHeight;

        if(blurOutputHorz == null) {
            blurOutputHorz = new Framebuffer(width, height, false);
            blurOutputHorz.setFramebufferFilter(GL11.GL_NEAREST);
        }
        if(blurOutputVert == null) {
            blurOutputVert = new Framebuffer(width, height, false);
            blurOutputVert.setFramebufferFilter(GL11.GL_NEAREST);
        }
        if(blurOutputHorz.framebufferWidth != width || blurOutputHorz.framebufferHeight != height) {
            blurOutputHorz.createBindFramebuffer(width, height);
            blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }
        if(blurOutputVert.framebufferWidth != width || blurOutputVert.framebufferHeight != height) {
            blurOutputVert.createBindFramebuffer(width, height);
            blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }

        if(blurShaderHorz == null) {
            try {
                blurShaderHorz = new Shader(Minecraft.getMinecraft().getResourceManager(), "blur",
                        Minecraft.getMinecraft().getFramebuffer(), blurOutputHorz);
                blurShaderHorz.getShaderManager().getShaderUniform("BlurDir").set(1, 0);
                blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
            } catch(Exception e) { }
        }
        if(blurShaderVert == null) {
            try {
                blurShaderVert = new Shader(Minecraft.getMinecraft().getResourceManager(), "blur",
                        blurOutputHorz, blurOutputVert);
                blurShaderVert.getShaderManager().getShaderUniform("BlurDir").set(0, 1);
                blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
            } catch(Exception e) { }
        }
        if(blurShaderHorz != null && blurShaderVert != null) {
            if(15 != lastBgBlurFactor) {
                blurShaderHorz.getShaderManager().getShaderUniform("Radius").set((float)15);
                blurShaderVert.getShaderManager().getShaderUniform("Radius").set((float)15);
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
        if(!OpenGlHelper.isFramebufferEnabled()) return;

        float uMin = x/(float)width;
        float uMax = (x+blurWidth)/(float)width;
        float vMin = (height-y)/(float)height;
        float vMax = (height-y-blurHeight)/(float)height;

        blurOutputVert.bindFramebufferTexture();
        GlStateManager.color(1f, 1f, 1f, 1f);
        Utils.drawTexturedRect(x, y, blurWidth, blurHeight, uMin, uMax, vMin, vMax);
        blurOutputVert.unbindFramebufferTexture();
    }

}
