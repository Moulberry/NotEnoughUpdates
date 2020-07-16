package io.github.moulberry.notenoughupdates.questing;

import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NEUResourceManager;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.cosmetics.CapeNode;
import io.github.moulberry.notenoughupdates.questing.requirements.*;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.moulberry.notenoughupdates.GuiTextures.item_mask;

public class QuestLine {

    private List<Quest> quests = new ArrayList<>();
    private Framebuffer framebuffer = null;
    private Framebuffer framebufferGrayscale = null;
    private Shader grayscaleShader = null;

    public class Quest {
        //requirements
        //rewards
        //display icon
        //x,y offset
        //prerequisite quests id:0,6

        private Requirement requirement;

        private int x;
        private int y;
        private ItemStack display;
        private String[] tooltip = new String[0];
        private int[] prerequisites;
        private boolean completed = false;

        public Quest(ItemStack display, Requirement requirement, int x, int y, int... prerequisites) {
            this.requirement = requirement;
            this.display = display;
            this.x = x;
            this.y = y;
            this.prerequisites = prerequisites;
        }

        public String[] render(int mouseX, int mouseY, boolean hasCompleted) {
            if(hasCompleted != completed) return null;

            Minecraft.getMinecraft().getTextureManager().bindTexture(item_mask);
            if(completed) {
                GlStateManager.color(200/255f, 150/255f, 50/255f, 255/255f);
            } else {
                GlStateManager.color(100/255f, 100/255f, 100/255f, 255/255f);
            }
            Utils.drawTexturedRect(x-9, y-9, 18, 18, GL11.GL_NEAREST);

            Utils.drawItemStack(display, x-8, y-8);

            GlStateManager.color(100/255f, 100/255f, 100/255f, 255/255f);
            GlStateManager.disableTexture2D();
            for(int prereq : prerequisites) {
                Quest other = quests.get(prereq);

                Tessellator tessellator = Tessellator.getInstance();
                WorldRenderer worldrenderer = tessellator.getWorldRenderer();
                worldrenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
                worldrenderer.pos(x, y, 0).endVertex();
                worldrenderer.pos(other.x, other.y, 0).endVertex();
                tessellator.draw();
            }

            if(mouseX > x-9 && mouseX < x+9) {
                if(mouseY > y-9 && mouseY < y+9) {
                    return tooltip;
                }
            }
            return null;
        }

        public void tick() {
            if(completed) return;
            for(int prereq : prerequisites) {
                Quest other = quests.get(prereq);
                if(!other.completed) return;
            }
            requirement.updateRequirement();
            if(requirement.getCompleted()) {
                completed = true;
            }
        }
    }

    private Framebuffer checkFramebufferSizes(Framebuffer framebuffer, int width, int height, int scaleFactor) {
        int sw = width*scaleFactor;
        int sh = height*scaleFactor;

        if(framebuffer == null || framebuffer.framebufferWidth != sw || framebuffer.framebufferHeight != sh) {
            if(framebuffer == null) {
                framebuffer = new Framebuffer(sw, sh, true);
            } else {
                framebuffer.createBindFramebuffer(sw, sh);
            }
            framebuffer.setFramebufferFilter(GL11.GL_NEAREST);
        }
        return framebuffer;
    }

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

    public void render(int width, int height, int mouseX, int mouseY) {
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());

        GL11.glTranslatef(width/2f, height/2f, 0);
        String[] tooltipDisplay = null;
        for(Quest quest : quests) {
            String[] tooltip = quest.render(mouseX-width/2, mouseY-height/2, true);
            if(tooltip != null && tooltip.length > 0) tooltipDisplay = tooltip;
        }
        GL11.glTranslatef(-width/2f, -height/2f, 0);

        //FBO/Shader setup
        if(framebuffer != null && grayscaleShader != null && (framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height)) {
            grayscaleShader.setProjectionMatrix(createProjectionMatrix(
                    width*scaledresolution.getScaleFactor(), height*scaledresolution.getScaleFactor()));
        }
        framebuffer = checkFramebufferSizes(framebuffer, width, height,
                scaledresolution.getScaleFactor());
        framebufferGrayscale = checkFramebufferSizes(framebufferGrayscale, width, height,
                scaledresolution.getScaleFactor());
        if(grayscaleShader == null) {
            try {
                grayscaleShader = new Shader(new NEUResourceManager(Minecraft.getMinecraft().getResourceManager()),
                        "grayscale",
                        framebuffer, framebufferGrayscale);
                grayscaleShader.setProjectionMatrix(createProjectionMatrix(
                        width*scaledresolution.getScaleFactor(), height*scaledresolution.getScaleFactor()));
            } catch(Exception e) {
                return;
            }
        }

        //Render contents of framebuffer to screen
        framebufferGrayscale.bindFramebufferTexture();
        GlStateManager.color(1f, 1f, 1f, 1f);
        Utils.drawTexturedRect(0, 0, width, height, 0, 1, 1, 0);
        framebufferGrayscale.unbindFramebufferTexture();

        //Render uncompleted quests to framebuffer
        GL11.glPushMatrix();
        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(false);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GL11.glTranslatef(width/2f, height/2f, 0);
        for(Quest quest : quests) {
            String[] tooltip = quest.render(mouseX-width/2, mouseY-height/2, false);
            if(tooltip != null && tooltip.length > 0) tooltipDisplay = tooltip;
        }
        GL11.glTranslatef(-width/2f, -height/2f, 0);
        framebuffer.unbindFramebuffer();
        GL11.glPopMatrix();

        //Execute shader
        GL11.glPushMatrix();
        grayscaleShader.loadShader(0);
        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
        GL11.glPopMatrix();

        if(tooltipDisplay != null) {
            List<String> tooltip = new ArrayList<>();
            tooltip.addAll(Arrays.asList(tooltipDisplay));

            Utils.drawHoveringText(tooltip, mouseX, mouseY, width, height, -1, Minecraft.getMinecraft().fontRendererObj, false);
        }
    }

    public void tick() {
        for(Quest quest : quests) {
            quest.tick();
        }
    }

    {
        //Requirement hubRequirement = new RequirementIslandType("Village");
        //Requirement locationRequirement = new RequirementLocation(-29, 79, -108, 2);
        //Requirement locationRequirement2 = new RequirementLocation(-29, 79, -108, 2, hubRequirement);
        Requirement req1 = new RequirementApi("coin_purse>20000");
        Requirement req2 = new RequirementIslandType("The End");
        Requirement req3 = new RequirementGuiOpen("Auctions Browser");

        Quest quest1 = new Quest(new ItemStack(Items.rotten_flesh), req1, 0, 0);
        Quest quest2 = new Quest(new ItemStack(Items.cooked_porkchop), req2, 0, 30);
        Quest quest3 = new Quest(new ItemStack(Items.diamond_axe), req3, 30, 30, 0, 1);
        quest1.tooltip = new String[]{"Line1", "Line2", "Line3"};
        quests.add(quest1);
        quests.add(quest2);
        quests.add(quest3);
    }

}
