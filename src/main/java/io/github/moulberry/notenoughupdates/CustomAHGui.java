package io.github.moulberry.notenoughupdates;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

public class CustomAHGui extends GuiScreen {

    public CustomAHGui() {
        this.allowUserInput = true;
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

}
