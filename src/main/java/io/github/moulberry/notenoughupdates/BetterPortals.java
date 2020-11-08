package io.github.moulberry.notenoughupdates;

import io.github.moulberry.notenoughupdates.util.TexLoc;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class BetterPortals extends Gui {

    private Set<Vector3f> loadedPortals = new HashSet<>();
    private HashMap<BlockPos, String> portalNameMap = new HashMap<>();

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Load event) {
        portalNameMap.clear();
        loadedPortals.clear();
    }

    /** The current GL viewport */
    private static final IntBuffer VIEWPORT = GLAllocation.createDirectIntBuffer(16);
    /** The current GL modelview matrix */
    private static final FloatBuffer MODELVIEW = GLAllocation.createDirectFloatBuffer(16);
    /** The current GL projection matrix */
    private static final FloatBuffer PROJECTION = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer WINCOORDS = GLAllocation.createDirectFloatBuffer(3);

    private float getFOVModifier(float partialTicks) {
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();

        float f = Minecraft.getMinecraft().gameSettings.fovSetting;
        //f = f * (this.fovModifierHandPrev + (this.fovModifierHand - this.fovModifierHandPrev) * partialTicks);

        if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).getHealth() <= 0.0F) {
            float f1 = (float)((EntityLivingBase)entity).deathTime + partialTicks;
            f /= (1.0F - 500.0F / (f1 + 500.0F)) * 2.0F + 1.0F;
        }

        Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(Minecraft.getMinecraft().theWorld, entity, partialTicks);

        if (block.getMaterial() == Material.water) {
            f = f * 60.0F / 70.0F;
        }

        return net.minecraftforge.client.ForgeHooksClient.getFOVModifier(Minecraft.getMinecraft().entityRenderer, entity, block, partialTicks, f);
    }

    TexLoc tl = new TexLoc(0, 1, Keyboard.KEY_M);

    @SubscribeEvent
    public void renderWorld(RenderGameOverlayEvent event) {

        GlStateManager.getFloat(2982, MODELVIEW);
        GlStateManager.getFloat(2983, PROJECTION);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);

        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        GlStateManager.disableCull();

        tl.handleKeyboardInput();

        WINCOORDS.flip().limit(3);

        /*float objx = -(float)(0-player.posX);
        float objy = (float)(100-player.posY-player.eyeHeight);
        float objz = (float)(0-player.posZ);*/

        float dX = -(float)(0-player.posX);
        float dY = (float)(100-player.posY-player.eyeHeight);
        float dZ = (float)(0-player.posZ);

        //GLU.gluProject(objx, objy, objz, MODELVIEW, PROJECTION, VIEWPORT, WINCOORDS);

        double x = dX*Math.cos(Math.toRadians(player.rotationYawHead))-dZ*Math.sin(Math.toRadians(player.rotationYawHead));
        double z = dX*Math.sin(Math.toRadians(player.rotationYawHead))+dZ*Math.cos(Math.toRadians(player.rotationYawHead));

        float fov = getFOVModifier(event.partialTicks);
        x = x / z * Math.toRadians(fov);
        dY = (float)(dY / z * Math.toRadians(fov));

        //System.out.println(z);

        //GLU.gluProject((float)x, dY, (float)z, MODELVIEW, PROJECTION, VIEWPORT, WINCOORDS);
        //x = x / z * 2;
        //dY = (float)(dY / z * 2);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        GL11.glScissor((int)(x*Minecraft.getMinecraft().displayWidth*tl.x/tl.y)+Minecraft.getMinecraft().displayWidth/2,
                (int)(dY+Minecraft.getMinecraft().displayHeight/2), 2, 2);

        drawRect(0, 0, 2000, 2000, -1);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        /*for(BlockPos pos : portalNameMap.keySet()) {
            WINCOORDS.flip().limit(3);

            /*float objx = -(float)((pos.getX()-player.posX)*Math.cos(Math.toRadians(player.rotationYawHead))*Math.cos(Math.toRadians(player.rotationPitch)));
            float objy = (float)((pos.getY()-player.posY)*Math.sin(Math.toRadians(player.rotationPitch)));
            float objz = (float)((pos.getZ()-player.posZ)*Math.sin(Math.toRadians(player.rotationYawHead)));

            float objx = -(float)(pos.getX()-player.posX);
            float objy = (float)(pos.getY()-player.posY-player.eyeHeight);
            float objz = (float)(pos.getZ()-player.posZ);

            GLU.gluProject(objx, objy, objz, MODELVIEW, PROJECTION, VIEWPORT, WINCOORDS);
            //GLU.glu

            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            //System.out.println(WINCOORDS.get(1));

            GL11.glScissor((int)WINCOORDS.get(0)*2,
                    Minecraft.getMinecraft().displayHeight-(int)WINCOORDS.get(1), (int)50, (int)50);

            //0-1
            //-1 - 1
            //0-1920

            drawRect(0, 0, 2000, 2000, -1);

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }*/

        GlStateManager.enableCull();
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if(Minecraft.getMinecraft().theWorld != null) {
            List<Vector3f> travelToPositions = new ArrayList<>();
            for(Entity entity : Minecraft.getMinecraft().theWorld.loadedEntityList) {
                if(entity instanceof EntityArmorStand) {
                    EntityArmorStand armorStand = (EntityArmorStand) entity;
                    if(armorStand.isInvisible() && armorStand.hasCustomName()) {
                        String customName = armorStand.getCustomNameTag();
                        if(customName.equals(EnumChatFormatting.AQUA+"Travel to:")) {
                            travelToPositions.add(new Vector3f((float)armorStand.posX, (float)armorStand.posY, (float)armorStand.posZ));
                        }
                    }
                }
            }
            travelToPositions.removeAll(loadedPortals);
            for(Entity entity : Minecraft.getMinecraft().theWorld.loadedEntityList) {
                if(entity instanceof EntityArmorStand) {
                    EntityArmorStand armorStand = (EntityArmorStand) entity;
                    if(armorStand.isInvisible() && armorStand.hasCustomName()) {
                        String customName = armorStand.getCustomNameTag();
                        for(Vector3f position : travelToPositions) {
                            if(position.x == (float)armorStand.posX && position.y-0.375 == (float)armorStand.posY && position.z == (float)armorStand.posZ) {
                                float smallestDist = 999;
                                BlockPos closestPortal = null;
                                for(int xOff=-3; xOff<=3; xOff++) {
                                    for(int zOff=-3; zOff<=3; zOff++) {
                                        if(xOff != 0 && zOff != 0) continue;
                                        BlockPos pos = new BlockPos(armorStand.posX+xOff, armorStand.posY+2, armorStand.posZ+zOff);
                                        if(Minecraft.getMinecraft().theWorld.getBlockState(pos).getBlock() == Blocks.portal) {
                                            float dist = (float)(armorStand.posX-(pos.getX()+0.5) + armorStand.posZ-(pos.getZ()+0.5));
                                            if(closestPortal == null || dist < smallestDist) {
                                                smallestDist = dist;
                                                closestPortal = pos;
                                            }
                                        }
                                    }
                                }
                                if(closestPortal != null) {
                                    portalNameMap.put(closestPortal, customName);
                                }
                            }
                        }
                    }
                }
            }
            loadedPortals.addAll(travelToPositions);
        }
    }

    public void tryRegisterPortal() {

    }

}
