package io.github.moulberry.notenoughupdates.core;

import io.github.moulberry.notenoughupdates.core.util.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.util.Matrix4f;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class BackgroundBlur {

    private static int fogColour = 0;
    private static boolean registered = false;
    public static void registerListener() {
        if(!registered) {
            MinecraftForge.EVENT_BUS.register(new BackgroundBlur());
        }
    }

    private boolean shouldBlur = true;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if(Minecraft.getMinecraft().theWorld != null) {
            shouldBlur = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScreenRender(RenderGameOverlayEvent.Pre event) {
        if(shouldBlur && event.type == RenderGameOverlayEvent.ElementType.ALL) {
            shouldBlur = false;
            blurBackground();
        }
    }

    @SubscribeEvent
    public void onFogColour(EntityViewRenderEvent.FogColors event) {
        fogColour = 0xff000000;
        fogColour |= ((int)(event.red*255) & 0xFF) << 16;
        fogColour |= ((int)(event.green*255) & 0xFF) << 8;
        fogColour |= (int)(event.blue*255) & 0xFF;
    }

    private static Shader blurShaderHorz = null;
    private static Framebuffer blurOutputHorz = null;
    private static Shader blurShaderVert = null;
    private static Framebuffer blurOutputVert = null;

    /**
     * Creates a projection matrix that projects from our coordinate space [0->width; 0->height] to OpenGL coordinate
     * space [-1 -> 1; 1 -> -1] (Note: flipped y-axis).
     *
     * This is so that we can render to and from the framebuffer in a way that is familiar to us, instead of needing to
     * apply scales and translations manually.
     */
    private static Matrix4f createProjectionMatrix(int width, int height) {
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

    private static double lastBgBlurFactor = -1;
    private static void blurBackground() {
        if(!OpenGlHelper.isFramebufferEnabled()) return;

        int width = Minecraft.getMinecraft().displayWidth;
        int height = Minecraft.getMinecraft().displayHeight;

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, width, height, 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);

        if(blurOutputHorz == null) {
            blurOutputHorz = new Framebuffer(width, height, false);
            blurOutputHorz.setFramebufferFilter(GL11.GL_NEAREST);
        }
        if(blurOutputVert == null) {
            blurOutputVert = new Framebuffer(width, height, false);
            blurOutputVert.setFramebufferFilter(GL11.GL_NEAREST);
        }
        if(blurOutputHorz == null || blurOutputVert == null) {
            return;
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
                        blurOutputVert, blurOutputHorz);
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
            if(blurShaderHorz.getShaderManager().getShaderUniform("Radius") == null) {
                //Corrupted shader?
                return;
            }
            if(15 != lastBgBlurFactor) {
                blurShaderHorz.getShaderManager().getShaderUniform("Radius").set((float)15);
                blurShaderVert.getShaderManager().getShaderUniform("Radius").set((float)15);
                lastBgBlurFactor = 15;
            }
            GL11.glPushMatrix();
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, Minecraft.getMinecraft().getFramebuffer().framebufferObject);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, blurOutputVert.framebufferObject);
            GL30.glBlitFramebuffer(0, 0, width, height,
                    0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

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
    public static void renderBlurredBackground(int screenWidth, int screenHeight,
                                               int x, int y, int blurWidth, int blurHeight) {
        if(!OpenGlHelper.isFramebufferEnabled()) return;
        if(blurOutputVert == null) return;

        float uMin = x/(float)screenWidth;
        float uMax = (x+blurWidth)/(float)screenWidth;
        float vMin = (screenHeight-y)/(float)screenHeight;
        float vMax = (screenHeight-y-blurHeight)/(float)screenHeight;

        GlStateManager.depthMask(false);
        Gui.drawRect(x, y, x+blurWidth, y+blurHeight, fogColour);
        blurOutputVert.bindFramebufferTexture();
        GlStateManager.color(1f, 1f, 1f, 1f);
        RenderUtils.drawTexturedRect(x, y, blurWidth, blurHeight, uMin, uMax, vMin, vMax);
        blurOutputVert.unbindFramebufferTexture();
        GlStateManager.depthMask(true);
    }

}
