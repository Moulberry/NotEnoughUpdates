package io.github.moulberry.notenoughupdates;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import java.util.HashSet;
import java.util.LinkedList;

public class CustomItemEffects {

    public static final CustomItemEffects INSTANCE = new CustomItemEffects();

    public int aoteUseTicks = 0;
    public int aoteTeleportationTicks = 0;
    public Vector3f aoteTeleportationLast = null;
    public Vector3f aoteTeleportationCurr = null;
    public boolean teleported = false;
    public float partialTicks;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if(aoteTeleportationTicks > 7) aoteTeleportationTicks = 7;

        if(aoteUseTicks > 20 && aoteTeleportationCurr != null && !teleported) {
            aoteTeleportationCurr = null;
            aoteTeleportationTicks = 0;
        }

        if(aoteTeleportationCurr != null && aoteTeleportationTicks > 0) {
            aoteUseTicks++;
            if(teleported) {
                float factor = 1f/aoteTeleportationTicks;

                aoteTeleportationLast = new Vector3f(aoteTeleportationCurr);

                float dX = aoteTeleportationCurr.x - (float)Minecraft.getMinecraft().thePlayer.posX;
                float dY = aoteTeleportationCurr.y - (float)Minecraft.getMinecraft().thePlayer.posY;
                float dZ = aoteTeleportationCurr.z - (float)Minecraft.getMinecraft().thePlayer.posZ;

                aoteTeleportationCurr.x -= dX*factor;
                aoteTeleportationCurr.y -= dY*factor;
                aoteTeleportationCurr.z -= dZ*factor;

                aoteTeleportationTicks--;
            } else {
                aoteTeleportationCurr.x = (float) Minecraft.getMinecraft().thePlayer.posX;
                aoteTeleportationCurr.y = (float) Minecraft.getMinecraft().thePlayer.posY;
                aoteTeleportationCurr.z = (float) Minecraft.getMinecraft().thePlayer.posZ;
                aoteTeleportationLast = new Vector3f(aoteTeleportationCurr);
            }
        } else {
            aoteTeleportationCurr = null;
            aoteUseTicks = 0;
            teleported = false;
        }
    }

    public Vector3f getCurrentPosition() {
        if(!teleported || aoteTeleportationLast == null || aoteTeleportationCurr == null) return null;
        return new Vector3f(aoteTeleportationLast.x + (aoteTeleportationCurr.x - aoteTeleportationLast.x) * partialTicks,
                            aoteTeleportationLast.y + (aoteTeleportationCurr.y - aoteTeleportationLast.y) * partialTicks,
                            aoteTeleportationLast.z + (aoteTeleportationCurr.z - aoteTeleportationLast.z) * partialTicks);
    }

    @SubscribeEvent
    public void renderBlockOverlay(DrawBlockHighlightEvent event) {
        if(aoteTeleportationCurr != null && aoteTeleportationTicks > 0 && teleported) {
            event.setCanceled(true);
        }
        ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();
        String heldInternal = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(held);
        if(heldInternal != null) {
            if(heldInternal.equals("JUNGLE_AXE") || heldInternal.equals("TREECAPITATOR_AXE")) {
                int maxWood = 10;
                if(heldInternal.equals("TREECAPITATOR_AXE")) maxWood = 35;

                if (event.target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.color(0.0F, 0.0F, 0.0F, 0.4F);
                    GL11.glLineWidth(2.0F);
                    GlStateManager.disableTexture2D();
                    GlStateManager.depthMask(false);
                    float f = 0.002F;
                    //BlockPos blockpos = ;
                    //Block block = Minecraft.getMinecraft().theWorld.getBlockState(blockpos).getBlock();

                    if(Minecraft.getMinecraft().theWorld.getBlockState(event.target.getBlockPos()).getBlock() == Blocks.log ||
                            Minecraft.getMinecraft().theWorld.getBlockState(event.target.getBlockPos()).getBlock() == Blocks.log2) {
                        EntityPlayer player = event.player;

                        int woods = 0;

                        HashSet<BlockPos> candidatesOld = new HashSet<>();
                        LinkedList<BlockPos> candidates = new LinkedList<>();
                        LinkedList<BlockPos> candidatesNew = new LinkedList<>();

                        candidatesNew.add(event.target.getBlockPos());

                        while(woods < maxWood) {
                            if(candidatesNew.isEmpty()) {
                                break;
                            }

                            candidates.addAll(candidatesNew);
                            candidatesNew.clear();

                            woods += candidates.size();
                            boolean random = woods > maxWood;

                            while(!candidates.isEmpty()) {
                                BlockPos candidate = candidates.pop();
                                Block block = Minecraft.getMinecraft().theWorld.getBlockState(candidate).getBlock();

                                candidatesOld.add(candidate);

                                for(int x = -1; x <= 1; x++) {
                                    for(int y = -1; y <= 1; y++) {
                                        for(int z = -1; z <= 1; z++) {
                                            if(x != 0 || y != 0 || z != 0) {
                                                BlockPos posNew = candidate.add(x, y, z);
                                                if(!candidatesOld.contains(posNew) && !candidates.contains(posNew) && !candidatesNew.contains(posNew)) {
                                                    Block blockNew = Minecraft.getMinecraft().theWorld.getBlockState(posNew).getBlock();
                                                    if(blockNew == Blocks.log || blockNew == Blocks.log2) {
                                                        candidatesNew.add(posNew);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                block.setBlockBoundsBasedOnState(Minecraft.getMinecraft().theWorld, candidate);
                                double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTicks;
                                double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTicks;
                                double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTicks;

                                drawSelectionBoundingBox(block.getSelectedBoundingBox(Minecraft.getMinecraft().theWorld, candidate)
                                        .expand(0.001D, 0.001D, 0.001D).offset(-d0, -d1, -d2),
                                        random ? 0.1f : 0.2f);
                            }
                        }
                    }

                    GlStateManager.depthMask(true);
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                }
            }
        }
    }

    public static void drawSelectionBoundingBox(AxisAlignedBB p_181561_0_, float alpha) {
        GlStateManager.color(64/255f, 224/255f, 208/255f, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        //vertical
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        tessellator.draw();
        //x

        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        tessellator.draw();

        //z
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        tessellator.draw();

    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR || event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();
            if(held != null) {
                String internal = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(held);
                if(internal != null && internal.equals("ASPECT_OF_THE_END")) {
                    aoteTeleportationTicks += 5;

                    if(aoteTeleportationCurr == null) {
                        aoteTeleportationCurr = new Vector3f();
                        aoteTeleportationCurr.x = (float) Minecraft.getMinecraft().thePlayer.posX;
                        aoteTeleportationCurr.y = (float) Minecraft.getMinecraft().thePlayer.posY;
                        aoteTeleportationCurr.z = (float) Minecraft.getMinecraft().thePlayer.posZ;
                    }

                }
            }
        }
    }

}
