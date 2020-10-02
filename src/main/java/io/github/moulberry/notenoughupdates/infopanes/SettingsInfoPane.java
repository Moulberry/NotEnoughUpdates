package io.github.moulberry.notenoughupdates.infopanes;

import io.github.moulberry.notenoughupdates.BetterContainers;
import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.NEUOverlay;
import io.github.moulberry.notenoughupdates.util.Utils;
import io.github.moulberry.notenoughupdates.itemeditor.GuiElementTextField;
import io.github.moulberry.notenoughupdates.options.Options;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
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

    public SettingsInfoPane(NEUOverlay overlay, NEUManager manager) {
        super(overlay, manager);
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
        });
        if(tfTop.get() != null) {
            tfTop.get().render(tfTopX.get(), tfTopY.get());
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
    }

    public void mouseInput(int width, int height, int mouseX, int mouseY, boolean mouseDown) {
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
