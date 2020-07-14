package io.github.moulberry.notenoughupdates.infopanes;

import io.github.moulberry.notenoughupdates.cosmetics.CapeManager;
import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.NEUOverlay;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.HashMap;

public class CosmeticsInfoPane extends InfoPane {

    public CosmeticsInfoPane(NEUOverlay overlay, NEUManager manager) {
        super(overlay, manager);
    }

    private HashMap<String, ResourceLocation> capeTextures = new HashMap<>();

    private String selectedCape = null;

    public void render(int width, int height, Color bg, Color fg, ScaledResolution scaledresolution, int mouseX,
                       int mouseY) {
        super.renderDefaultBackground(width, height, bg);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        int currentY = overlay.getBoxPadding()+10;
        fr.drawString("NEU Capes", overlay.getBoxPadding()+10, currentY, Color.WHITE.getRGB(), true); currentY += 10;

        selectedCape = null;
        for(String cape : CapeManager.getInstance().getCapes()) {
            if(CapeManager.getInstance().getPermissionForCape(Minecraft.getMinecraft().thePlayer.getName(), cape)) {
                currentY += renderCapeSelector(cape, currentY, mouseX, mouseY);
                currentY += 5;
            }
        }
    }

    public int renderCapeSelector(String capename, int y, int mouseX, int mouseY) {
        if(mouseX > overlay.getBoxPadding()+5 && mouseX < overlay.getBoxPadding()+75) {
            if(mouseY > y && mouseY < y+100) {
                selectedCape = capename;
            }
        }
        boolean selected = capename.equals(CapeManager.getInstance().getCape(Minecraft.getMinecraft().thePlayer.getName()));

        if(selected) {
            drawRect(overlay.getBoxPadding()+5, y, overlay.getBoxPadding()+75, y+100, Color.YELLOW.getRGB());
            drawGradientRect(overlay.getBoxPadding()+10, y+5, overlay.getBoxPadding()+70, y+95, Color.GRAY.darker().getRGB(), Color.GRAY.getRGB());
        } else {
            drawGradientRect(overlay.getBoxPadding()+5, y, overlay.getBoxPadding()+75, y+100, Color.GRAY.darker().getRGB(), Color.GRAY.getRGB());
        }

        GlStateManager.color(1, 1, 1, 1);

        ResourceLocation capeTex = capeTextures.computeIfAbsent(capename, k -> new ResourceLocation("notenoughupdates:"+capename+".png"));

        Minecraft.getMinecraft().getTextureManager().bindTexture(capeTex);
        Utils.drawTexturedRect(overlay.getBoxPadding()+10, y+10, 60, 80, 0, 300/1024f, 0, 425/1024f);
        return 100;
    }

    public void mouseInput(int width, int height, int mouseX, int mouseY, boolean mouseDown) {
        if(mouseDown && selectedCape != null) {
            if(selectedCape.equals(CapeManager.getInstance().getCape(Minecraft.getMinecraft().thePlayer.getName()))) {
                for(EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
                    CapeManager.getInstance().setCape(player.getName(), null);
                }
            } else {
                for(EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
                    CapeManager.getInstance().setCape(player.getName(), selectedCape);
                }
            }
        }
    }

    @Override
    public boolean keyboardInput() {
        return false;
    }

}
