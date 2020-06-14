package io.github.moulberry.notenoughupdates.infopanes;

import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.NEUOverlay;
import io.github.moulberry.notenoughupdates.Utils;
import io.github.moulberry.notenoughupdates.itemeditor.GuiElementTextField;
import io.github.moulberry.notenoughupdates.options.Options;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

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
            public void consume(int x, int y, int tileWidth, int tileHeight, Options.Option<?> option) {
                float mult = tileWidth/90f;

                drawRect(x, y, x+tileWidth, y+tileHeight, fg.getRGB());
                if(scaledresolution.getScaleFactor()==4) {
                    GL11.glScalef(0.5f,0.5f,1);
                    Utils.renderStringTrimWidth(option.displayName, fr, true, (x+(int)(8*mult))*2, (y+(int)(8*mult))*2,
                            (tileWidth-(int)(16*mult))*2, new Color(100,255,150).getRGB(), 3);
                    GL11.glScalef(2,2,1);
                } else {
                    Utils.renderStringTrimWidth(option.displayName, fr, true, x+(int)(8*mult), y+(int)(8*mult),
                            tileWidth-(int)(16*mult), new Color(100,255,150).getRGB(), 3);
                }

                if(option.value instanceof Boolean) {
                    GlStateManager.color(1f, 1f, 1f, 1f);
                    Minecraft.getMinecraft().getTextureManager().bindTexture(((Boolean)option.value) ? on : off);
                    Utils.drawTexturedRect(x+tileWidth/2-(int)(32*mult), y+tileHeight-(int)(20*mult), (int)(48*mult), (int)(16*mult));

                    Minecraft.getMinecraft().getTextureManager().bindTexture(help);
                    Utils.drawTexturedRect(x+tileWidth/2+(int)(19*mult), y+tileHeight-(int)(19*mult), (int)(14*mult), (int)(14*mult));
                    GlStateManager.bindTexture(0);

                    if(mouseX > x+tileWidth/2+(int)(19*mult) && mouseX < x+tileWidth/2+(int)(19*mult)+(int)(14*mult)) {
                        if(mouseY > y+tileHeight-(int)(19*mult) && mouseY < y+tileHeight-(int)(19*mult)+(int)(14*mult)) {
                            List<String> textLines = new ArrayList<>();
                            textLines.add(option.displayName);
                            textLines.add(EnumChatFormatting.GRAY+option.desc);
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
        } catch(Exception e) {
            tf.setCustomBorderColour(Color.RED.getRGB());
        }
    }

    public void mouseInput(int width, int height, int mouseX, int mouseY, boolean mouseDown) {
        iterateSettingTile(new SettingsTileConsumer() {
            @Override
            public void consume(int x, int y, int tileWidth, int tileHeight, Options.Option<?> option) {
                float mult = tileWidth/90f;
                if(option.value instanceof Boolean) {
                    if(Mouse.getEventButtonState()) {
                        if(mouseX > x+tileWidth/2-(int)(32*mult) && mouseX < x+tileWidth/2-(int)(32*mult)+(int)(48*mult)) {
                            if(mouseY > y+tileHeight-(int)(20*mult) && mouseY < y+tileHeight-(int)(20*mult)+(int)(16*mult)) {
                                ((Options.Option<Boolean>)option).value = !((Boolean)option.value);
                                overlay.redrawItems();
                                return;
                            }
                        }
                    }
                } else {
                    if(!textConfigMap.containsKey(option)) {
                        textConfigMap.put(option, new GuiElementTextField(String.valueOf(option.value), 0));
                    }
                    GuiElementTextField tf = textConfigMap.get(option);
                    if(mouseX > x+(int)(10*mult) && mouseX < x+(int)(10*mult)+tileWidth-(int)(20*mult)) {
                        if(mouseY > y+tileHeight-(int)(20*mult) && mouseY < y+tileHeight-(int)(20*mult)+(int)(16*mult)) {
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
            public void consume(int x, int y, int tileWidth, int tileHeight, Options.Option<?> option) {
                if(!textConfigMap.containsKey(option)) {
                    textConfigMap.put(option, new GuiElementTextField(String.valueOf(option.value), 0));
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
        public abstract void consume(int x, int y, int tileWidth, int tileHeight, Options.Option<?> option);
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
        for(int i=0; i<manager.config.getOptions().size(); i++) {
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
                settingsTileConsumer.consume(boxLeft+x, y, tileWidth, tileHeight, manager.config.getOptions().get(i));
            }

            x+=tileWidth;
        }
    }
}
