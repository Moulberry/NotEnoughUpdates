package io.github.moulberry.notenoughupdates.overlays;

import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class TextOverlay {

    private Position position;
    private Supplier<TextOverlayStyle> styleSupplier;
    public int overlayWidth = -1;
    public int overlayHeight = -1;
    public List<String> overlayStrings = null;
    private Supplier<List<String>> dummyStrings;

    public boolean shouldUpdateFrequent = false;

    private static final int PADDING_X = 5;
    private static final int PADDING_Y = 5;

    public TextOverlay(Position position, Supplier<List<String>> dummyStrings, Supplier<TextOverlayStyle> styleSupplier) {
        this.position = position;
        this.styleSupplier = styleSupplier;
        if(dummyStrings == null) {
            this.dummyStrings = () -> null;
        } else {
            this.dummyStrings = dummyStrings;
        }
    }

    public Vector2f getDummySize() {
        List<String> dummyStrings = this.dummyStrings.get();

        if(dummyStrings != null) {
            int overlayHeight = 0;
            int overlayWidth = 0;
            for(String s : dummyStrings) {
                if(s == null) {
                    overlayHeight += 3;
                    continue;
                }
                for(String s2 : s.split("\n")) {
                    int sWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(s2);
                    if(sWidth > overlayWidth) {
                        overlayWidth = sWidth;
                    }
                    overlayHeight += 10;
                }
            }
            overlayHeight -= 2;

            return new Vector2f(overlayWidth+PADDING_X*2, overlayHeight+PADDING_Y*2);
        }
        return new Vector2f(100, 50);
    }

    public void tick() {
        update();
    }

    public void updateFrequent() {}
    public abstract void update();

    public void renderDummy() {
        List<String> dummyStrings = this.dummyStrings.get();
        render(dummyStrings);
    }

    public void render() {
        if(shouldUpdateFrequent) {
            updateFrequent();
            shouldUpdateFrequent = false;
        }
        render(overlayStrings);
    }

    private void render(List<String> strings) {
        if(strings == null) return;

        overlayHeight = 0;
        overlayWidth = 0;
        for(String s : strings) {
            if(s == null) {
                overlayHeight += 3;
                continue;
            }
            for(String s2 : s.split("\n")) {
                int sWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(s2);
                if(sWidth > overlayWidth) {
                    overlayWidth = sWidth;
                }
                overlayHeight += 10;
            }
        }
        overlayHeight -= 2;

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());

        int x = position.getAbsX(scaledResolution, overlayWidth+PADDING_X*2);
        int y = position.getAbsY(scaledResolution, overlayHeight+PADDING_Y*2);

        TextOverlayStyle style = styleSupplier.get();

        if(style == TextOverlayStyle.BACKGROUND) Gui.drawRect(x, y, x+overlayWidth+PADDING_X*2, y+overlayHeight+PADDING_Y*2, 0x80000000);

        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int yOff = 0;
        for(String s : strings) {
            if(s == null) {
                yOff += 3;
            } else {
                for(String s2 : s.split("\n")) {
                    if(style == TextOverlayStyle.FULL_SHADOW) {
                        String clean = Utils.cleanColourNotModifiers(s2);
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
                    Minecraft.getMinecraft().fontRendererObj.drawString(s2,
                            x+PADDING_X, y+PADDING_Y+yOff, 0xffffff, style == TextOverlayStyle.MC_SHADOW);

                    yOff += 10;
                }
            }
        }
    }

}
