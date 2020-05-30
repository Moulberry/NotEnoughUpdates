package io.github.moulberry.notenoughupdates.infopanes;

import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.NEUOverlay;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;

public abstract class InfoPane extends Gui  {

    final NEUOverlay overlay;
    final NEUManager manager;

    public InfoPane(NEUOverlay overlay, NEUManager manager) {
        this.overlay = overlay;
        this.manager = manager;
    }

    public void tick() {}

    public abstract void render(int width, int height, Color bg, Color fg, ScaledResolution scaledresolution, int mouseX,
                           int mouseY);

    public abstract void mouseInput(int width, int height, int mouseX, int mouseY, boolean mouseDown);

    public abstract void keyboardInput();

    public void renderDefaultBackground(int width, int height, Color bg) {
        int paneWidth = (int)(width/3*overlay.getWidthMult());
        int rightSide = (int)(width*overlay.getInfoPaneOffsetFactor());
        int leftSide = rightSide - paneWidth;

        int boxLeft = leftSide + overlay.BOX_PADDING - 5;
        int boxRight = rightSide - overlay.BOX_PADDING + 5;

        overlay.renderBlurredBackground(width, height,
                boxLeft, overlay.BOX_PADDING-5,
                boxRight-boxLeft, height-overlay.BOX_PADDING*2+10);
        drawRect(boxLeft, overlay.BOX_PADDING - 5, boxRight,
                height - overlay.BOX_PADDING + 5, bg.getRGB());
    }

}
