package io.github.moulberry.notenoughupdates.cosmetics;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Matrix4f;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.awt.*;
import java.util.List;
import java.util.*;

public class GuiCosmetics extends GuiScreen {

    public static final ResourceLocation pv_bg = new ResourceLocation("notenoughupdates:pv_bg.png");
    public static final ResourceLocation pv_dropdown = new ResourceLocation("notenoughupdates:pv_dropdown.png");
    public static final ResourceLocation cosmetics_fg = new ResourceLocation("notenoughupdates:cosmetics_fg.png");
    public static final ResourceLocation pv_elements = new ResourceLocation("notenoughupdates:pv_elements.png");

    private CosmeticsPage currentPage = CosmeticsPage.CAPES;
    private int sizeX;
    private int sizeY;
    private int guiLeft;
    private int guiTop;

    private String wantToEquipCape = null;
    private long lastCapeEquip = 0;

    private List<String> tooltipToDisplay = null;

    public enum CosmeticsPage {
        CAPES(new ItemStack(Items.chainmail_chestplate));

        public final ItemStack stack;

        CosmeticsPage(ItemStack stack) {
            this.stack = stack;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.sizeX = 431;
        this.sizeY = 202;
        this.guiLeft = (this.width-this.sizeX)/2;
        this.guiTop = (this.height-this.sizeY)/2;

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawDefaultBackground();

        blurBackground();
        renderBlurredBackground(width, height, guiLeft+2, guiTop+2, sizeX-4, sizeY-4);

        GlStateManager.enableDepth();
        GlStateManager.translate(0, 0, 5);
        renderTabs(true);
        GlStateManager.translate(0, 0, -3);

        GlStateManager.disableDepth();
        GlStateManager.translate(0, 0, -2);
        renderTabs(false);
        GlStateManager.translate(0, 0, 2);

        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);

        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_bg);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);

        GlStateManager.color(1, 1, 1, 1);
        switch (currentPage) {
            case CAPES:
                drawCapesPage(mouseX, mouseY, partialTicks);
                break;
        }

        if(tooltipToDisplay != null) {
            List<String> grayTooltip = new ArrayList<>(tooltipToDisplay.size());
            for(String line : tooltipToDisplay) {
                grayTooltip.add(EnumChatFormatting.GRAY + line);
            }
            Utils.drawHoveringText(grayTooltip, mouseX, mouseY, width, height, -1, Minecraft.getMinecraft().fontRendererObj);
            tooltipToDisplay = null;
        }

        StringBuilder statusMsg = new StringBuilder("Last Sync: ");
        if(CapeManager.INSTANCE.lastCapeSynced == 0) {
            statusMsg.append("Not Synced");
        } else {
            statusMsg.append((System.currentTimeMillis() - CapeManager.INSTANCE.lastCapeSynced)/1000).append("s ago");
        }
        statusMsg.append(" - Next Sync: ");
        if(CapeManager.INSTANCE.lastCapeUpdate == 0) {
            statusMsg.append("ASAP");
        } else {
            statusMsg.append(60 - (System.currentTimeMillis() - CapeManager.INSTANCE.lastCapeUpdate)/1000).append("s");
        }

        Minecraft.getMinecraft().fontRendererObj.drawString(EnumChatFormatting.AQUA+statusMsg.toString(),
                guiLeft+sizeX-Minecraft.getMinecraft().fontRendererObj.getStringWidth(statusMsg.toString()), guiTop-12, 0, true);

        if(currentPage == CosmeticsPage.CAPES) {
            GlStateManager.color(1, 1, 1, 1);
            Minecraft.getMinecraft().getTextureManager().bindTexture(pv_dropdown);
            Utils.drawTexturedRect(guiLeft+sizeX/2f-50, guiTop+sizeY+5, 100, 20, 0, 100/200f, 0, 20/185f, GL11.GL_NEAREST);

            String equipMsg;
            if(wantToEquipCape != null) {
                equipMsg = EnumChatFormatting.GREEN + "Equip Cape";
                if(System.currentTimeMillis() - lastCapeEquip < 20*1000) {
                    equipMsg += " - " + (20 - (System.currentTimeMillis() - lastCapeEquip)/1000) + "s";
                }
            } else {
                equipMsg = EnumChatFormatting.GREEN + "Unequip";
                if(System.currentTimeMillis() - lastCapeEquip < 20*1000) {
                    equipMsg += " - " + (20 - (System.currentTimeMillis() - lastCapeEquip)/1000) + "s";
                }
            }

            Utils.drawStringCenteredScaledMaxWidth(equipMsg, Minecraft.getMinecraft().fontRendererObj,
                    guiLeft+sizeX/2f, guiTop+sizeY+5+10, false, 90, 0);
        }
    }

    private void renderTabs(boolean renderPressed) {
        int ignoredTabs = 0;
        for(int i = 0; i< CosmeticsPage.values().length; i++) {
            CosmeticsPage page = CosmeticsPage.values()[i];
            if(page.stack == null) {
                ignoredTabs++;
                continue;
            }
            boolean pressed = page == currentPage;
            if(pressed == renderPressed) {
                renderTab(page.stack, i-ignoredTabs, pressed);
            }
        }
    }

    private void renderTab(ItemStack stack, int xIndex, boolean pressed) {
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);

        int x = guiLeft+xIndex*28;
        int y = guiTop-28;

        float uMin = 0;
        float uMax = 28/256f;
        float vMin = 20/256f;
        float vMax = 51/256f;
        if(pressed) {
            vMin = 52/256f;
            vMax = 84/256f;

            if(xIndex != 0) {
                uMin = 28/256f;
                uMax = 56/256f;
            }

            renderBlurredBackground(width, height, x+2, y+2, 28-4, 28-4);
        } else {
            renderBlurredBackground(width, height, x+2, y+4, 28-4, 28-4);
        }

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);

        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
        Utils.drawTexturedRect(x, y, 28, pressed?32:31, uMin, uMax, vMin, vMax, GL11.GL_NEAREST);

        GlStateManager.enableDepth();
        Utils.drawItemStack(stack, x+6, y+9);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (int i = 0; i < CosmeticsPage.values().length; i++) {
            CosmeticsPage page = CosmeticsPage.values()[i];
            int x = guiLeft + i * 28;
            int y = guiTop - 28;

            if (mouseX > x && mouseX < x + 28) {
                if (mouseY > y && mouseY < y + 32) {
                    if (currentPage != page) Utils.playPressSound();
                    currentPage = page;
                    return;
                }
            }
        }
        if(mouseY > guiTop+177 && mouseY < guiTop+177+12) {
            if(mouseX > guiLeft+15+371*scroll && mouseX < guiLeft+15+371*scroll+32) {
                scrollClickedX = mouseX - (int)(guiLeft+15+371*scroll);
                return;
            }
        }

        int index = 0;
        int displayingCapes = 0;
        for(String cape : CapeManager.INSTANCE.getCapes()) {
            boolean equipable = CapeManager.INSTANCE.getAvailableCapes() == null || CapeManager.INSTANCE.getAvailableCapes().contains(cape);
            if (!CapeManager.INSTANCE.specialCapes[index++] || equipable) {
                displayingCapes++;
            }
        }

        float totalNeeded = 91*displayingCapes;
        float totalAvail = sizeX-20;
        float xOffset = scroll*(totalNeeded-totalAvail);

        index = 0;
        int displayIndex = 0;
        for(String cape : CapeManager.INSTANCE.getCapes()) {
            boolean equipable = CapeManager.INSTANCE.getAvailableCapes() == null || CapeManager.INSTANCE.getAvailableCapes().contains(cape);
            if(CapeManager.INSTANCE.specialCapes[index++] && !equipable) continue;

            GlStateManager.color(1, 1, 1, 1);
            Utils.drawTexturedRect(guiLeft + 20 + 91 * displayIndex - xOffset, guiTop + 123, 81, 20,
                    0, 81 / 256f, 216 / 256f, 236 / 256f, GL11.GL_NEAREST);

            if(mouseX > guiLeft + 20 + 91 * displayIndex - xOffset && mouseX < guiLeft + 20 + 91 * displayIndex - xOffset+81) {
                if(mouseY > guiTop + 123 && mouseY < guiTop + 123 + 20) {
                    if(CapeManager.INSTANCE.localCape != null && CapeManager.INSTANCE.localCape.getRight().equals(cape)) {
                        CapeManager.INSTANCE.setCape(Minecraft.getMinecraft().thePlayer.getUniqueID().toString().replace("-", ""),
                                "null", true);
                    } else {
                        CapeManager.INSTANCE.setCape(Minecraft.getMinecraft().thePlayer.getUniqueID().toString().replace("-", ""),
                                cape, true);
                    }

                    return;
                } else if(equipable && mouseY > guiTop + 149 && mouseY < guiTop + 149 + 20) {
                    if(cape.equals(wantToEquipCape)) {
                        wantToEquipCape = null;
                    } else {
                        wantToEquipCape = cape;
                    }
                    return;
                }
            }

            displayIndex++;
        }

        if(currentPage == CosmeticsPage.CAPES) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(pv_dropdown);
            Utils.drawTexturedRect(guiLeft+sizeX/2f-50, guiTop+sizeY+5, 100, 20, 0, 100/200f, 0, 20/185f, GL11.GL_NEAREST);

            if(mouseX > guiLeft+sizeX/2f-50 && mouseX < guiLeft+sizeX/2f+50) {
                if(mouseY > guiTop+sizeY+5 && mouseY < guiTop+sizeY+25) {
                    if(System.currentTimeMillis() - lastCapeEquip > 20*1000) {
                        CapeManager.INSTANCE.setCape(Minecraft.getMinecraft().thePlayer.getUniqueID().toString().replace("-", ""),
                                null, true);

                        lastCapeEquip = System.currentTimeMillis();
                        if(wantToEquipCape == null) {
                            NotEnoughUpdates.INSTANCE.manager.hypixelApi.getMyApiAsync("cgi-bin/changecape.py?capeType=null&accessToken="+
                                    Minecraft.getMinecraft().getSession().getToken(), (jsonObject) -> { System.out.println(jsonObject); }, () -> {
                                System.out.println("change cape error");
                            });
                        } else {
                            NotEnoughUpdates.INSTANCE.manager.hypixelApi.getMyApiAsync("cgi-bin/changecape.py?capeType="+wantToEquipCape+"&accessToken="+
                                    Minecraft.getMinecraft().getSession().getToken(), (jsonObject) -> { System.out.println(jsonObject); }, () -> {
                                System.out.println("change cape error");
                            });
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);

        scrollClickedX = -1;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

        if(scrollClickedX >= 0) {
            float scrollStartX = mouseX - scrollClickedX;
            scroll = (scrollStartX-(guiLeft+15))/371f;
            scroll = Math.max(0, Math.min(1, scroll));
        }
    }

    private HashMap<String, ResourceLocation> capesLocation = new HashMap<>();
    private float scroll = 0f;
    private int scrollClickedX = -1;
    private void drawCapesPage(int mouseX, int mouseY, float partialTicks) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(cosmetics_fg);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);

        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
        Utils.drawTexturedRect(guiLeft+15+371*scroll, guiTop+177, 32, 12,
                0, 32/256f, 192/256f, 204/256f, GL11.GL_NEAREST);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(Minecraft.getMinecraft().displayWidth*(guiLeft+3)/width, 0,
                Minecraft.getMinecraft().displayWidth*(sizeX-6)/width,  Minecraft.getMinecraft().displayHeight);

        int index = 0;
        int displayingCapes = 0;
        for(String cape : CapeManager.INSTANCE.getCapes()) {
            boolean equipable = CapeManager.INSTANCE.getAvailableCapes() == null || CapeManager.INSTANCE.getAvailableCapes().contains(cape);
            if (!CapeManager.INSTANCE.specialCapes[index++] || equipable) {
                displayingCapes++;
            }
        }

        float totalNeeded = 91*displayingCapes;
        float totalAvail = sizeX-20;
        float xOffset = scroll*(totalNeeded-totalAvail);

        index = 0;
        int displayIndex = 0;
        for(String cape : CapeManager.INSTANCE.getCapes()) {
            boolean equipable = CapeManager.INSTANCE.getAvailableCapes() == null || CapeManager.INSTANCE.getAvailableCapes().contains(cape);
            if(CapeManager.INSTANCE.specialCapes[index++] && !equipable) continue;

            if(cape.equals(CapeManager.INSTANCE.getCape(Minecraft.getMinecraft().thePlayer.getUniqueID().toString().replace("-", "")))) {
                GlStateManager.color(250 / 255f, 200 / 255f, 0 / 255f, 1);
                Utils.drawGradientRect(guiLeft + 20 + 91 * displayIndex - (int) xOffset, guiTop + 10,
                        guiLeft + 20 + 91 * displayIndex - (int) xOffset + 81, guiTop + 10 + 108,
                        new Color(150, 100, 0, 40).getRGB(), new Color(250, 200, 0, 40).getRGB());
            } else if(cape.equals(wantToEquipCape)) {
                GlStateManager.color(0, 200 / 255f, 250 / 255f, 1);
                Utils.drawGradientRect(guiLeft + 20 + 91 * displayIndex - (int) xOffset, guiTop + 10,
                        guiLeft + 20 + 91 * displayIndex - (int) xOffset + 81, guiTop + 10 + 108,
                        new Color(0, 100, 150, 40).getRGB(), new Color(0, 200, 250, 40).getRGB());
            } else if(CapeManager.INSTANCE.localCape != null && CapeManager.INSTANCE.localCape.getRight().equals(cape)) {
                GlStateManager.color(100/255f, 250/255f, 150/255f, 1);
                Utils.drawGradientRect(guiLeft+20+91*displayIndex-(int)xOffset, guiTop+10,
                        guiLeft+20+91*displayIndex-(int)xOffset+81, guiTop+10+108,
                        new Color(50, 100, 75, 40).getRGB(), new Color(100, 250, 150, 40).getRGB());
            }
            Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
            Utils.drawTexturedRect(guiLeft+20+91*displayIndex-xOffset, guiTop+10, 81, 108,
                    0, 81/256f, 84/256f, 192/256f, GL11.GL_NEAREST);
            GlStateManager.color(1, 1, 1, 1);

            Utils.drawTexturedRect(guiLeft+20+91*displayIndex-xOffset, guiTop+123, 81, 20,
                    0, 81/256f, 216/256f, 236/256f, GL11.GL_NEAREST);

            boolean equipPressed = cape.equals(wantToEquipCape);
            if(!equipable) GlStateManager.color(1, 1, 1, 0.5f);
            Utils.drawTexturedRect(guiLeft+20+91*displayIndex-xOffset, guiTop+149, 81, 20,
                    equipPressed?81/256f:0, equipPressed?0:81/256f, equipPressed?236/256f:216/256f, equipPressed?216/256f:236/256f, GL11.GL_NEAREST);

            Utils.drawStringCenteredScaledMaxWidth("Try it out", Minecraft.getMinecraft().fontRendererObj,
                    guiLeft+20+91*displayIndex+81/2f-xOffset, guiTop+123+10, false, 75, new Color(100, 250, 150).getRGB());
            if(equipable) {
                Utils.drawStringCenteredScaledMaxWidth("Equip", Minecraft.getMinecraft().fontRendererObj,
                        guiLeft+20+91*displayIndex+81/2f-xOffset, guiTop+149+10, false, 75, new Color(100, 250, 150).getRGB());
            } else {
                Utils.drawStringCenteredScaledMaxWidth("Not Unlocked", Minecraft.getMinecraft().fontRendererObj,
                        guiLeft+20+91*displayIndex+81/2f-xOffset, guiTop+149+10, false, 75, new Color(200, 50, 50, 100).getRGB());
            }
            GlStateManager.color(1, 1, 1, 1);

            ResourceLocation capeTexture = capesLocation.computeIfAbsent(cape, k -> new ResourceLocation("notenoughupdates", "capes/"+cape+".png"));
            Minecraft.getMinecraft().getTextureManager().bindTexture(capeTexture);
            Utils.drawTexturedRect(guiLeft+31+91*displayIndex-xOffset, guiTop+24, 59, 84,
                    0, 293/1024f, 0, 420/1024f, GL11.GL_NEAREST);

            displayIndex++;
        }

        GL11.glScissor(0, 0, Minecraft.getMinecraft().displayWidth,  Minecraft.getMinecraft().displayHeight);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
    }

    Shader blurShaderHorz = null;
    Framebuffer blurOutputHorz = null;
    Shader blurShaderVert = null;
    Framebuffer blurOutputVert = null;

    /**
     * Creates a projection matrix that projects from our coordinate space [0->width; 0->height] to OpenGL coordinate
     * space [-1 -> 1; 1 -> -1] (Note: flipped y-axis).
     *
     * This is so that we can render to and from the framebuffer in a way that is familiar to us, instead of needing to
     * apply scales and translations manually.
     */
    private Matrix4f createProjectionMatrix(int width, int height) {
        Matrix4f projMatrix  = new Matrix4f();
        projMatrix.setIdentity();
        projMatrix.m00 = 2.0F / (float)width;
        projMatrix.m11 = 2.0F / (float)(-height);
        projMatrix.m22 = -0.0020001999F;
        projMatrix.m33 = 1.0F;
        projMatrix.m03 = -1.0F;
        projMatrix.m13 = 1.0F;
        projMatrix.m23 = -1.0001999F;
        return projMatrix;
    }

    /**
     * Renders whatever is currently in the Minecraft framebuffer to our two framebuffers, applying a horizontal
     * and vertical blur separately in order to significantly save computation time.
     * This is only possible if framebuffers are supported by the system, so this method will exit prematurely
     * if framebuffers are not available. (Apple machines, for example, have poor framebuffer support).
     */
    private double lastBgBlurFactor = -1;
    private void blurBackground() {
        int width = Minecraft.getMinecraft().displayWidth;
        int height = Minecraft.getMinecraft().displayHeight;

        if(blurOutputHorz == null) {
            blurOutputHorz = new Framebuffer(width, height, false);
            blurOutputHorz.setFramebufferFilter(GL11.GL_NEAREST);
        }
        if(blurOutputVert == null) {
            blurOutputVert = new Framebuffer(width, height, false);
            blurOutputVert.setFramebufferFilter(GL11.GL_NEAREST);
        }
        if(blurOutputHorz.framebufferWidth != width || blurOutputHorz.framebufferHeight != height) {
            blurOutputHorz.createBindFramebuffer(width, height);
            blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }
        if(blurOutputVert.framebufferWidth != width || blurOutputVert.framebufferHeight != height) {
            blurOutputVert.createBindFramebuffer(width, height);
            blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }

        if(blurShaderHorz == null) {
            try {
                blurShaderHorz = new Shader(Minecraft.getMinecraft().getResourceManager(), "blur",
                        Minecraft.getMinecraft().getFramebuffer(), blurOutputHorz);
                blurShaderHorz.getShaderManager().getShaderUniform("BlurDir").set(1, 0);
                blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
            } catch(Exception e) { }
        }
        if(blurShaderVert == null) {
            try {
                blurShaderVert = new Shader(Minecraft.getMinecraft().getResourceManager(), "blur",
                        blurOutputHorz, blurOutputVert);
                blurShaderVert.getShaderManager().getShaderUniform("BlurDir").set(0, 1);
                blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
            } catch(Exception e) { }
        }
        if(blurShaderHorz != null && blurShaderVert != null) {
            if(15 != lastBgBlurFactor) {
                blurShaderHorz.getShaderManager().getShaderUniform("Radius").set((float)15);
                blurShaderVert.getShaderManager().getShaderUniform("Radius").set((float)15);
                lastBgBlurFactor = 15;
            }
            GL11.glPushMatrix();
            blurShaderHorz.loadShader(0);
            blurShaderVert.loadShader(0);
            GlStateManager.enableDepth();
            GL11.glPopMatrix();

            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }
    }

    /**
     * Renders a subsection of the blurred framebuffer on to the corresponding section of the screen.
     * Essentially, this method will "blur" the background inside the bounds specified by [x->x+blurWidth, y->y+blurHeight]
     */
    public void renderBlurredBackground(int width, int height, int x, int y, int blurWidth, int blurHeight) {
        float uMin = x/(float)width;
        float uMax = (x+blurWidth)/(float)width;
        float vMin = (height-y)/(float)height;
        float vMax = (height-y-blurHeight)/(float)height;

        blurOutputVert.bindFramebufferTexture();
        GlStateManager.color(1f, 1f, 1f, 1f);
        //Utils.setScreen(width*f, height*f, f);
        Utils.drawTexturedRect(x, y, blurWidth, blurHeight, uMin, uMax, vMin, vMax);
        //Utils.setScreen(width, height, f);
        blurOutputVert.unbindFramebufferTexture();
    }
}
