package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderFish;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Items;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(RenderFish.class)
public abstract class MixinRenderFish extends Render<EntityFishHook> {

    protected MixinRenderFish(RenderManager renderManager) {
        super(renderManager);
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/projectile/EntityFishHook;DDDFF)V", at=@At(value = "HEAD"), cancellable = true)
    public void render(EntityFishHook entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if(!NotEnoughUpdates.INSTANCE.manager.config.rodColours.value || entity == null) return;

        String internalname = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(entity.angler.getHeldItem());
        if (NotEnoughUpdates.INSTANCE.isOnSkyblock() && internalname != null && entity.angler != null &&
                entity.angler.getHeldItem().getItem().equals(Items.fishing_rod)) {
            if (!internalname.equals("GRAPPLING_HOOK") && !internalname.endsWith("_WHIP")) {
                ci.cancel();

                GlStateManager.pushMatrix();
                GlStateManager.translate((float)x, (float)y, (float)z);
                GlStateManager.enableRescaleNormal();
                GlStateManager.scale(0.5F, 0.5F, 0.5F);
                this.bindEntityTexture(entity);
                Tessellator tessellator = Tessellator.getInstance();
                WorldRenderer worldrenderer = tessellator.getWorldRenderer();
                GlStateManager.rotate(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
                worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
                worldrenderer.pos(-0.5D, -0.5D, 0.0D).tex(0.0625D, 0.1875D).normal(0.0F, 1.0F, 0.0F).endVertex();
                worldrenderer.pos(0.5D, -0.5D, 0.0D).tex(0.125D, 0.1875D).normal(0.0F, 1.0F, 0.0F).endVertex();
                worldrenderer.pos(0.5D, 0.5D, 0.0D).tex(0.125D, 0.125D).normal(0.0F, 1.0F, 0.0F).endVertex();
                worldrenderer.pos(-0.5D, 0.5D, 0.0D).tex(0.0625D, 0.125D).normal(0.0F, 1.0F, 0.0F).endVertex();
                tessellator.draw();
                GlStateManager.disableRescaleNormal();
                GlStateManager.popMatrix();

                if (entity.angler != null) {
                    float f7 = entity.angler.getSwingProgress(partialTicks);
                    float f8 = MathHelper.sin(MathHelper.sqrt_float(f7) * (float)Math.PI);

                    double d0;
                    double d1;
                    double d2;
                    double d3;
                    if(this.renderManager.options.thirdPersonView == 0 && entity.angler == Minecraft.getMinecraft().thePlayer) {
                        double fov = this.renderManager.options.fovSetting;
                        fov = fov / 90.0;
                        double xFactor = 0.5 + 0.55*((fov-0.333)/0.889);
                        Vec3 vec3 = new Vec3(-xFactor * fov, -0.045D * fov, 0.4D);
                        vec3 = vec3.rotatePitch(-(entity.angler.prevRotationPitch + (entity.angler.rotationPitch - entity.angler.prevRotationPitch) * partialTicks) * (float)Math.PI / 180.0F);
                        vec3 = vec3.rotateYaw(-(entity.angler.prevRotationYaw + (entity.angler.rotationYaw - entity.angler.prevRotationYaw) * partialTicks) * (float)Math.PI / 180.0F);
                        vec3 = vec3.rotateYaw(f8 * 0.5F);
                        vec3 = vec3.rotatePitch(-f8 * 0.7F);
                        d0 = entity.angler.prevPosX + (entity.angler.posX - entity.angler.prevPosX) * (double)partialTicks + vec3.xCoord;
                        d1 = entity.angler.prevPosY + (entity.angler.posY - entity.angler.prevPosY) * (double)partialTicks + vec3.yCoord;
                        d2 = entity.angler.prevPosZ + (entity.angler.posZ - entity.angler.prevPosZ) * (double)partialTicks + vec3.zCoord;
                        d3 = entity.angler.getEyeHeight();
                    } else {
                        float f9 = (entity.angler.prevRenderYawOffset + (entity.angler.renderYawOffset - entity.angler.prevRenderYawOffset) * partialTicks) * (float)Math.PI / 180.0F;
                        double d4 = MathHelper.sin(f9);
                        double d6 = MathHelper.cos(f9);
                        d0 = entity.angler.prevPosX + (entity.angler.posX - entity.angler.prevPosX) * (double)partialTicks - d6 * 0.35D - d4 * 0.8D;
                        d1 = entity.angler.prevPosY + entity.angler.getEyeHeight() + (entity.angler.posY - entity.angler.prevPosY) * (double)partialTicks - 0.45D;
                        d2 = entity.angler.prevPosZ + (entity.angler.posZ - entity.angler.prevPosZ) * (double)partialTicks - d4 * 0.35D + d6 * 0.8D;
                        d3 = entity.angler.isSneaking() ? -0.1875D : 0.0D;
                    }

                    double d13 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double)partialTicks;
                    double d5 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double)partialTicks + 0.25D;
                    double d7 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double)partialTicks;
                    double d9 = (double)((float)(d0 - d13));
                    double d11 = (double)((float)(d1 - d5)) + d3;
                    double d12 = (double)((float)(d2 - d7));
                    GlStateManager.disableTexture2D();
                    GlStateManager.disableLighting();
                    GlStateManager.enableBlend();
                    GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    worldrenderer.begin(3, DefaultVertexFormats.POSITION_COLOR);

                    String specialColour;
                    if (entity.angler.getUniqueID().equals(Minecraft.getMinecraft().thePlayer.getUniqueID())) {
                        specialColour = NotEnoughUpdates.INSTANCE.manager.config.selfRodLineColour.value;
                    } else {
                        specialColour = NotEnoughUpdates.INSTANCE.manager.config.otherRodLineColour.value;
                    }
                    int colourI = SpecialColour.specialToChromaRGB(specialColour);

                    for (int l = 0; l <= 16; ++l) {
                        if(SpecialColour.getSpeed(specialColour) > 0) { //has chroma
                            colourI = SpecialColour.rotateHue(colourI, 10);
                        }
                        Color colour = new Color(colourI, true);

                        float f10 = (float)l / 16.0F;
                        worldrenderer.pos(x + d9 * (double)f10, y + d11 * (double)(f10 * f10 + f10) * 0.5D + 0.25D, z + d12 * (double)f10)
                                     .color(colour.getRed(), colour.getGreen(), colour.getBlue(), colour.getAlpha()).endVertex();
                    }

                    tessellator.draw();
                    GlStateManager.disableBlend();
                    GlStateManager.enableLighting();
                    GlStateManager.enableTexture2D();
                }
            }
        }
    }

}