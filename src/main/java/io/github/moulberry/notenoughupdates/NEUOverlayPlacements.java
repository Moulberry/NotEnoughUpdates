package io.github.moulberry.notenoughupdates;

import io.github.moulberry.notenoughupdates.mbgui.*;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
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

    private boolean dropdownMenuShown = false;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawDefaultBackground();

        /*GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(icons);
        GlStateManager.enableBlend();

        GlStateManager.tryBlendFuncSeparate(775, 769, 1, 0);
        GlStateManager.enableAlpha();
        this.drawTexturedModalRect(width / 2 - 7, height / 2 - 7, 0, 0, 16, 16);*/

        if(mouseX < 300 && mouseY < 300 && clickedElement != null) {
            guiButton.yPosition = height - 5 - guiButton.height;
        } else {
            guiButton.yPosition = 5;
        }

        EnumChatFormatting GOLD = EnumChatFormatting.GOLD;

        guiButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY);

        NotEnoughUpdates.INSTANCE.overlay.updateGuiGroupSize();

        drawRect((width-176)/2, (height-166)/2,
                (width+176)/2, (height+166)/2, new Color(100, 100, 100, 200).getRGB());
        Utils.drawStringCentered(GOLD+"Inventory", Minecraft.getMinecraft().fontRendererObj, width/2f, height/2f, false, 0);

        MBGuiGroupFloating mainGroup = NotEnoughUpdates.INSTANCE.overlay.guiGroup;
        mainGroup.render(0, 0);
        GlStateManager.translate(0, 0, 500);
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

            if(anchorPoint.inventoryRelative) {
                Utils.drawStringCentered(GOLD+"Inv-Relative", Minecraft.getMinecraft().fontRendererObj,
                        position.x+element.getWidth()*0.5f, position.y+element.getHeight()*0.5f, false, 0);
            }
        }
        GlStateManager.translate(0, 0, -500);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if(mouseButton != 0 && mouseButton != 1) return;

        MBGuiGroupFloating mainGroup = NotEnoughUpdates.INSTANCE.overlay.guiGroup;
        int index=0;
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
                        if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                            anchorPoint.inventoryRelative = !anchorPoint.inventoryRelative;
                        } else {
                            MBAnchorPoint.AnchorPoint[] vals = MBAnchorPoint.AnchorPoint.values();
                            anchorPoint.anchorPoint = vals[(anchorPoint.anchorPoint.ordinal()+1)%vals.length];

                            mainGroup.recalculate();

                            anchorPoint.offset.x += position.x - mainGroup.getChildrenPosition().get(element).x;
                            anchorPoint.offset.y += position.y - mainGroup.getChildrenPosition().get(element).y;

                            mainGroup.recalculate();

                            if(index == 0) {
                                NotEnoughUpdates.INSTANCE.manager.config.overlaySearchBar.value = anchorPoint.toString();
                            } else if(index == 1) {
                                NotEnoughUpdates.INSTANCE.manager.config.overlayQuickCommand.value = anchorPoint.toString();
                            }
                            try { NotEnoughUpdates.INSTANCE.manager.saveConfig(); } catch(IOException ignored) {}
                        }
                    }
                    return;
                }
            }
            index++;
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
            try { MBDeserializer.serializeAndSave(mainGroup, "overlay"); } catch(Exception e) {}
            mainGroup.recalculate();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
