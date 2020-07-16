package io.github.moulberry.notenoughupdates.questing;

import net.minecraft.client.gui.GuiScreen;

import java.awt.*;

public class GuiQuestLine extends GuiScreen {

    public static QuestLine questLine = new QuestLine();

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        drawDefaultBackground();

        drawRect(0, 0, 20, height, new Color(100, 100, 100, 100).getRGB());
        drawRect(20, 0, 21, height, new Color(50, 50, 50, 200).getRGB());

        questLine.render(width, height, mouseX, mouseY);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
