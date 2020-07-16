package io.github.moulberry.notenoughupdates;

import io.github.moulberry.notenoughupdates.mbgui.MBAnchorPoint;
import io.github.moulberry.notenoughupdates.mbgui.MBGuiElement;
import io.github.moulberry.notenoughupdates.mbgui.MBGuiGroup;
import io.github.moulberry.notenoughupdates.mbgui.MBGuiGroupFloating;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.io.IOException;

public class NEUOverlayPlacements extends GuiScreen {

    private int clickedX;
    private int clickedY;
    private int clickedAnchorX;
    private int clickedAnchorY;
    private MBGuiElement clickedElement;
    private GuiButton guiButton = new GuiButton(0, 5, 5, "Reset to Default");

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawDefaultBackground();

        if(mouseX < 300 && mouseY < 300 && clickedElement != null) {
            guiButton.yPosition = height - 5 - guiButton.height;
        } else {
            guiButton.yPosition = 5;
        }

        guiButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY);

        NotEnoughUpdates.INSTANCE.overlay.updateGuiGroupSize();

        MBGuiGroupFloating mainGroup = NotEnoughUpdates.INSTANCE.overlay.guiGroup;
        for(MBGuiElement element : mainGroup.getChildren()) {
            MBAnchorPoint anchorPoint = mainGroup.getChildrenMap().get(element);
            Vector2f position = mainGroup.getChildrenPosition().get(element);

            drawRect((int)position.x, (int)position.y,
                    (int)position.x+element.getWidth(), (int)position.y+element.getHeight(), new Color(100, 100, 100, 200).getRGB());


            switch(anchorPoint.anchorPoint) {
                case TOPLEFT:
                case TOPRIGHT:
                case BOTLEFT:
                case BOTRIGHT:
                    drawRect((int)(position.x+element.getWidth()*anchorPoint.anchorPoint.x*0.9f),
                            (int)(position.y+element.getHeight()*anchorPoint.anchorPoint.y*0.9f),
                            (int)(position.x+element.getWidth()*anchorPoint.anchorPoint.x*0.9f+element.getWidth()*0.1f),
                            (int)(position.y+element.getHeight()*anchorPoint.anchorPoint.y*0.9f+element.getHeight()*0.1f),
                            new Color(200, 200, 200, 100).getRGB());
                    break;
                case TOPMID:
                    drawRect((int)position.x, (int)position.y,
                            (int)position.x+element.getWidth(), (int)(position.y+element.getHeight()*0.1f),
                            new Color(200, 200, 200, 100).getRGB());
                    break;
                case MIDLEFT:
                    drawRect((int)position.x, (int)position.y,
                            (int)(position.x+element.getWidth()*0.1f), (int)position.y+element.getHeight(),
                            new Color(200, 200, 200, 100).getRGB());
                    break;
                case MIDRIGHT:
                    drawRect((int)(position.x+element.getWidth()*0.9f), (int)position.y,
                            (int)position.x+element.getWidth(), (int)position.y+element.getHeight(),
                            new Color(200, 200, 200, 100).getRGB());
                    break;
                case BOTMID:
                    drawRect((int)position.x, (int)(position.y+element.getHeight()*0.9f),
                            (int)position.x+element.getWidth(), (int)position.y+element.getHeight(),
                            new Color(200, 200, 200, 100).getRGB());
                    break;
                case MIDMID:
                    drawRect((int)(position.x+element.getWidth()*0.45f), (int)(position.y+element.getHeight()*0.45f),
                            (int)(position.x+element.getWidth()*0.55f), (int)(position.y+element.getHeight()*0.55f),
                            new Color(200, 200, 200, 100).getRGB());
                    break;

            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        MBGuiGroupFloating mainGroup = NotEnoughUpdates.INSTANCE.overlay.guiGroup;
        for(MBGuiElement element : mainGroup.getChildren()) {
            MBAnchorPoint anchorPoint = mainGroup.getChildrenMap().get(element);
            Vector2f position = mainGroup.getChildrenPosition().get(element);

            if(mouseX > position.x && mouseX < position.x + element.getWidth()) {
                if(mouseY > position.y && mouseY < position.y + element.getHeight()) {
                    if(mouseButton == 0) {
                        clickedElement = element;
                        clickedX = mouseX;
                        clickedY = mouseY;
                        clickedAnchorX = (int)anchorPoint.offset.x;
                        clickedAnchorY = (int)anchorPoint.offset.y;
                    } else {
                        float anchorX = (width-element.getWidth()) * anchorPoint.anchorPoint.x + anchorPoint.offset.x;
                        float anchorY = (height-element.getHeight()) * anchorPoint.anchorPoint.y + anchorPoint.offset.y;

                        MBAnchorPoint.AnchorPoint[] vals = MBAnchorPoint.AnchorPoint.values();
                        anchorPoint.anchorPoint = vals[(anchorPoint.anchorPoint.ordinal()+1)%vals.length];

                        float screenX = (width-element.getWidth()) * anchorPoint.anchorPoint.x;
                        float screenY = (height-element.getHeight()) * anchorPoint.anchorPoint.y;
                        anchorPoint.offset.x = anchorX - screenX;
                        anchorPoint.offset.y = anchorY - screenY;

                        mainGroup.recalculate();
                    }
                    return;
                }
            }
        }

        if(guiButton.mousePressed(Minecraft.getMinecraft(), mouseX, mouseY)) {
            NotEnoughUpdates.INSTANCE.manager.config.overlayQuickCommand.value = "";
            NotEnoughUpdates.INSTANCE.manager.config.overlaySearchBar.value = "";
            try { NotEnoughUpdates.INSTANCE.manager.saveConfig(); } catch(IOException ignored) {}
            NotEnoughUpdates.INSTANCE.overlay.resetAnchors(false);

            mainGroup.recalculate();
        }
        clickedElement = null;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        clickedElement = null;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if(clickedElement != null) {
            MBGuiGroupFloating mainGroup = NotEnoughUpdates.INSTANCE.overlay.guiGroup;
            MBAnchorPoint anchorPoint = mainGroup.getChildrenMap().get(clickedElement);

            if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                int dX = mouseX - clickedX;
                int dY = mouseY - clickedY;
                if(Math.abs(dX) > Math.abs(dY)) {
                    anchorPoint.offset.x = mouseX - clickedX + clickedAnchorX;
                    anchorPoint.offset.y = clickedAnchorY;
                } else {
                    anchorPoint.offset.x = clickedAnchorX;
                    anchorPoint.offset.y = mouseY - clickedY + clickedAnchorY;
                }
            } else {
                anchorPoint.offset.x = mouseX - clickedX + clickedAnchorX;
                anchorPoint.offset.y = mouseY - clickedY + clickedAnchorY;
            }

            int index = 0;
            for(MBGuiElement element : mainGroup.getChildren()) {
                if(element == clickedElement) {
                    if(index == 0) {
                        NotEnoughUpdates.INSTANCE.manager.config.overlaySearchBar.value = anchorPoint.toString();
                    } else if(index == 1) {
                        NotEnoughUpdates.INSTANCE.manager.config.overlayQuickCommand.value = anchorPoint.toString();
                    }
                    try { NotEnoughUpdates.INSTANCE.manager.saveConfig(); } catch(IOException ignored) {}
                    break;
                }
                index++;
            }

            mainGroup.recalculate();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
