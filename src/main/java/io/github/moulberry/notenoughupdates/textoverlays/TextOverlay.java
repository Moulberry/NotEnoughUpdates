package io.github.moulberry.notenoughupdates.textoverlays;

import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.core.util.StringUtils;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

public abstract class TextOverlay {

    private Position position;
    private Supplier<TextOverlayStyle> styleSupplier;
    public int overlayWidth = -1;
    public int overlayHeight = -1;
    public List<String> overlayStrings = null;

    private static final int PADDING_X = 5;
    private static final int PADDING_Y = 5;

    public TextOverlay(Position position, Supplier<TextOverlayStyle> styleSupplier) {
        this.position = position;
        this.styleSupplier = styleSupplier;
    }

    public void tick() {
        update();

        if(overlayStrings != null) {
            overlayHeight = 0;
            overlayWidth = 0;
            for(String s : overlayStrings) {
                if(s == null) {
                    overlayHeight += 3;
                    continue;
                }
                int sWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(s);
                if(sWidth > overlayWidth) {
                    overlayWidth = sWidth;
                }
                overlayHeight += 10;
            }
            overlayHeight -= 2;
        }
    }

    public abstract void update();

    public void render() {
        if(overlayStrings != null) {
            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());

            int x = position.getAbsX(scaledResolution);
            int y = position.getAbsY(scaledResolution);

            TextOverlayStyle style = styleSupplier.get();

            if(style == TextOverlayStyle.BACKGROUND) Gui.drawRect(x, y, x+overlayWidth+PADDING_X*2, y+overlayHeight+PADDING_Y*2, 0x80000000);

            int yOff = 0;
            for(String s : overlayStrings) {
                if(s == null) {
                    yOff += 3;
                } else {
                    if(style == TextOverlayStyle.FULL_SHADOW) {
                        String clean = Utils.cleanColourNotModifiers(s);
                        for(int xO=-2; xO<=2; xO++) {
                            for(int yO=-2; yO<=2; yO++) {
                                if(Math.abs(xO) != Math.abs(yO)) {
                                    Minecraft.getMinecraft().fontRendererObj.drawString(clean,
                                            x+PADDING_X+xO/2f, y+PADDING_Y+yOff+yO/2f,
                                            new Color(0, 0, 0, 200/Math.max(Math.abs(xO), Math.abs(yO))).getRGB(), false);
                                }
                            }
                        }
                    }
                    Minecraft.getMinecraft().fontRendererObj.drawString(s,
                                x+PADDING_X, y+PADDING_Y+yOff, 0xffffff, style == TextOverlayStyle.MC_SHADOW);

                    yOff += 10;
                }
            }
        }
    }

}
