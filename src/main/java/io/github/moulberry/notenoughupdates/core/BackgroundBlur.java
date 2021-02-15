package io.github.moulberry.notenoughupdates.core;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
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
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BackgroundBlur {

    private static HashMap<Float, Framebuffer> blurOutput = new HashMap<>();
    private static HashMap<Float, Long> lastBlurUse = new HashMap<>();
    private static HashSet<Float> requestedBlurs = new HashSet<>();

    private static int fogColour = 0;
    private static boolean registered = false;
    public static void registerListener() {
        if(!registered) {
            registered = true;
            MinecraftForge.EVENT_BUS.register(new BackgroundBlur());
        }
    }

    private static boolean shouldBlur = true;

    public static void markDirty() {
        if(Minecraft.getMinecraft().theWorld != null) {
            shouldBlur = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScreenRender(RenderGameOverlayEvent.Pre event) {
        if(shouldBlur && event.type == RenderGameOverlayEvent.ElementType.ALL) {
            shouldBlur = false;

            long currentTime = System.currentTimeMillis();

            for(float blur : requestedBlurs) {
                lastBlurUse.put(blur, currentTime);

                int width = Minecraft.getMinecraft().displayWidth;
                int height = Minecraft.getMinecraft().displayHeight;

                Framebuffer output = blurOutput.computeIfAbsent(blur, k -> {
                    Framebuffer fb = new Framebuffer(width, height, false);
                    fb.setFramebufferFilter(GL11.GL_NEAREST);
                    return fb;
                });

                output.framebufferWidth = output.framebufferTextureWidth = width;
                output.framebufferHeight = output.framebufferTextureHeight = height;

                blurBackground(output, blur);
            }

            Set<Float> remove = new HashSet<>();
            for(Map.Entry<Float, Long> entry : lastBlurUse.entrySet()) {
                if(currentTime - entry.getValue() > 30*1000) {
                    remove.add(entry.getKey());
                }
            }
            remove.remove((float)NotEnoughUpdates.INSTANCE.config.itemlist.bgBlurFactor);

            lastBlurUse.keySet().removeAll(remove);
            blurOutput.keySet().removeAll(remove);

            requestedBlurs.clear();
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
    private static Shader blurShaderVert = null;
    private static Framebuffer blurOutputHorz = null;

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
    private static void blurBackground(Framebuffer output, float blurFactor) {
        if(!OpenGlHelper.isFramebufferEnabled() || !OpenGlHelper.areShadersSupported()) return;

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
        if(blurOutputHorz == null || output == null) {
            return;
        }
        if(blurOutputHorz.framebufferWidth != width || blurOutputHorz.framebufferHeight != height) {
            blurOutputHorz.createBindFramebuffer(width, height);
            blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }

        /*if(blurShaderHorz == null) {

        }*/
        try {
            blurShaderHorz = new Shader(Minecraft.getMinecraft().getResourceManager(), "blur",
                    Minecraft.getMinecraft().getFramebuffer(), blurOutputHorz);
            blurShaderHorz.getShaderManager().getShaderUniform("BlurDir").set(1, 0);
            blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
        } catch(Exception e) { }
        try {
            blurShaderVert = new Shader(Minecraft.getMinecraft().getResourceManager(), "blur",
                    blurOutputHorz, output);
            blurShaderVert.getShaderManager().getShaderUniform("BlurDir").set(0, 1);
            blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
        } catch(Exception e) { }
        if(blurShaderHorz != null && blurShaderVert != null) {
            if(blurShaderHorz.getShaderManager().getShaderUniform("Radius") == null) {
                //Corrupted shader?
                return;
            }

            blurShaderHorz.getShaderManager().getShaderUniform("Radius").set(blurFactor);
            blurShaderVert.getShaderManager().getShaderUniform("Radius").set(blurFactor);

            GL11.glPushMatrix();
            /*GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, Minecraft.getMinecraft().getFramebuffer().framebufferObject);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, blurOutputVert.framebufferObject);
            GL30.glBlitFramebuffer(0, 0, width, height,
                    0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);*/

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
    public static void renderBlurredBackground(float blurStrength, int screenWidth, int screenHeight,
                                               int x, int y, int blurWidth, int blurHeight) {
        if(blurStrength < 0.5) return;
        requestedBlurs.add(blurStrength);

        if(!OpenGlHelper.isFramebufferEnabled() || !OpenGlHelper.areShadersSupported()) return;

        if(blurOutput.isEmpty()) return;

        Framebuffer fb = blurOutput.get(blurStrength);
        if(fb == null) {
            System.out.println("Blur not found:"+blurStrength);
            fb = blurOutput.values().iterator().next();
        }

        float uMin = x/(float)screenWidth;
        float uMax = (x+blurWidth)/(float)screenWidth;
        float vMin = (screenHeight-y)/(float)screenHeight;
        float vMax = (screenHeight-y-blurHeight)/(float)screenHeight;

        GlStateManager.depthMask(false);
        Gui.drawRect(x, y, x+blurWidth, y+blurHeight, fogColour);
        fb.bindFramebufferTexture();
        GlStateManager.color(1f, 1f, 1f, 1f);
        RenderUtils.drawTexturedRect(x, y, blurWidth, blurHeight, uMin, uMax, vMin, vMax);
        fb.unbindFramebufferTexture();
        GlStateManager.depthMask(true);
    }

}
