package io.github.moulberry.notenoughupdates;

import io.github.moulberry.notenoughupdates.util.SpecialColour;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.*;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import scala.tools.cmd.Spec;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class CustomItemEffects {

    public static final CustomItemEffects INSTANCE = new CustomItemEffects();

    private static final int MAX_BUILDERS_BLOCKS = 164;

    public long aoteUseMillis = 0;
    public int aoteTeleportationMillis = 0;
    public Vector3f aoteTeleportationCurr = null;

    public long lastMillis = 0;

    public Vector3f getCurrentPosition() {
        if(aoteTeleportationMillis <= 0) return null;
        return aoteTeleportationCurr;
    }

    @SubscribeEvent
    public void onTick(TickEvent.RenderTickEvent event) {
        //if(aoteTeleportationTicks > 7) aoteTeleportationTicks = 7;
        long currentTime = System.currentTimeMillis();
        int delta = (int)(currentTime - lastMillis);
        lastMillis = currentTime;

        if(delta <= 0) return;

        if(aoteTeleportationMillis > NotEnoughUpdates.INSTANCE.manager.config.smoothAoteMillis.value.intValue()*2) {
            aoteTeleportationMillis = NotEnoughUpdates.INSTANCE.manager.config.smoothAoteMillis.value.intValue()*2;
        }
        if(aoteTeleportationMillis < 0) aoteTeleportationMillis = 0;

        if(currentTime - aoteUseMillis > 1000 && aoteTeleportationMillis <= 0) {
            aoteTeleportationCurr = null;
        }

        if(aoteTeleportationCurr != null) {
            if(aoteTeleportationMillis > 0) {
                int deltaMin = Math.min(delta, aoteTeleportationMillis);

                float factor = deltaMin/(float)aoteTeleportationMillis;

                float dX = aoteTeleportationCurr.x - (float)Minecraft.getMinecraft().thePlayer.posX;
                float dY = aoteTeleportationCurr.y - (float)Minecraft.getMinecraft().thePlayer.posY;
                float dZ = aoteTeleportationCurr.z - (float)Minecraft.getMinecraft().thePlayer.posZ;

                aoteTeleportationCurr.x -= dX*factor;
                aoteTeleportationCurr.y -= dY*factor;
                aoteTeleportationCurr.z -= dZ*factor;

                if(Minecraft.getMinecraft().theWorld.getBlockState(new BlockPos(aoteTeleportationCurr.x,
                        aoteTeleportationCurr.y, aoteTeleportationCurr.z)).getBlock().getMaterial() != Material.air) {
                    aoteTeleportationCurr.y = (float)Math.ceil(aoteTeleportationCurr.y);
                }

                aoteTeleportationMillis -= deltaMin;
            } else {
                aoteTeleportationCurr.x = (float) Minecraft.getMinecraft().thePlayer.posX;
                aoteTeleportationCurr.y = (float) Minecraft.getMinecraft().thePlayer.posY;
                aoteTeleportationCurr.z = (float) Minecraft.getMinecraft().thePlayer.posZ;
            }
        } else {
            aoteUseMillis = 0;
            aoteTeleportationMillis = 0;
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(NotEnoughUpdates.INSTANCE.manager.config.smoothAoteMillis.value <= 0
            || Minecraft.getMinecraft().gameSettings.thirdPersonView != 0) return;

        if(event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR || event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();
            if(held != null) {
                String internal = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(held);
                if(internal != null && internal.equals("ASPECT_OF_THE_END")) {
                    aoteUseMillis = System.currentTimeMillis();
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

    @SubscribeEvent
    public void onOverlayDrawn(RenderGameOverlayEvent event) {
        if(!NotEnoughUpdates.INSTANCE.manager.config.disableWandOverlay.value &&
                Minecraft.getMinecraft().objectMouseOver != null &&
                Minecraft.getMinecraft().objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK &&
                ((event.type == null && Loader.isModLoaded("labymod")) ||
                event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS)) {

            IBlockState hover = Minecraft.getMinecraft().theWorld.getBlockState(
                    Minecraft.getMinecraft().objectMouseOver.getBlockPos().offset(
                            Minecraft.getMinecraft().objectMouseOver.sideHit, 1));
            if(hover.getBlock() == Blocks.air) {
                ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();
                String heldInternal = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(held);

                if(heldInternal != null && heldInternal.equals("BUILDERS_WAND")) {
                    ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());

                    HashSet<BlockPos> candidatesOld = new HashSet<>();
                    TreeMap<Float, Set<BlockPos>> candidatesOldSorted = new TreeMap<>();

                    IBlockState match = Minecraft.getMinecraft().theWorld.getBlockState(Minecraft.getMinecraft().objectMouseOver.getBlockPos());
                    Item matchItem = Item.getItemFromBlock(match.getBlock());
                    if(matchItem != null) {
                        ItemStack matchStack =  new ItemStack(matchItem, 1,
                                match.getBlock().getDamageValue(Minecraft.getMinecraft().theWorld, Minecraft.getMinecraft().objectMouseOver.getBlockPos()));
                        int itemCount = countItemsInInventoryAndStorage(matchStack);

                        getBuildersWandCandidates(Minecraft.getMinecraft().thePlayer, Minecraft.getMinecraft().objectMouseOver, event.partialTicks,
                                candidatesOld, candidatesOldSorted, 999-MAX_BUILDERS_BLOCKS);

                        if(candidatesOld.size() > MAX_BUILDERS_BLOCKS) {
                            Utils.drawStringCentered(EnumChatFormatting.RED.toString()+candidatesOld.size()+"/"+MAX_BUILDERS_BLOCKS,
                                    Minecraft.getMinecraft().fontRendererObj,
                                    scaledResolution.getScaledWidth()/2f, scaledResolution.getScaledHeight()/2f+10, true, 0);
                        } else {
                            String pre = EnumChatFormatting.GREEN.toString();
                            if(itemCount < candidatesOld.size()) {
                                pre = EnumChatFormatting.RED.toString();
                            }
                            Utils.drawStringCentered(pre+Math.min(candidatesOld.size(), itemCount)+"/"+
                                            Math.min(candidatesOld.size(), MAX_BUILDERS_BLOCKS),
                                    Minecraft.getMinecraft().fontRendererObj,
                                    scaledResolution.getScaledWidth()/2f, scaledResolution.getScaledHeight()/2f+10, true, 0);
                        }

                        String itemCountS = EnumChatFormatting.DARK_GRAY+"x"+EnumChatFormatting.RESET+countItemsInInventoryAndStorage(matchStack);
                        int itemCountLen = Minecraft.getMinecraft().fontRendererObj.getStringWidth(itemCountS);

                        if(NotEnoughUpdates.INSTANCE.manager.config.wandBlockCount.value) {
                            Utils.drawItemStack(matchStack, scaledResolution.getScaledWidth()/2 - (itemCountLen+16)/2, scaledResolution.getScaledHeight()/2+10+4);
                            Minecraft.getMinecraft().fontRendererObj.drawString(itemCountS,
                                    scaledResolution.getScaledWidth()/2f - (itemCountLen+16)/2f+16, scaledResolution.getScaledHeight()/2f+10+8,
                                    -1,
                                    true);
                        }

                        GlStateManager.color(1, 1, 1, 1);
                    }

                }
            }
        }
    }

    public int countItemsInInventoryAndStorage(ItemStack match) {
        int count = 0;

        for(ItemStack stack : Minecraft.getMinecraft().thePlayer.inventory.mainInventory) {
            if(match.isItemEqual(stack)) {
                count += stack.stackSize;
            }
        }

        ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();
        String heldInternal = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(held);

        if(heldInternal != null && heldInternal.equals("BUILDERS_WAND")) {
            //System.out.println("1");
            if(held.hasTagCompound() && held.getTagCompound().hasKey("ExtraAttributes", 10) &&
                    held.getTagCompound().getCompoundTag("ExtraAttributes").hasKey("builder's_wand_data", 7)) {
                //System.out.println("2");
                byte[] bytes = held.getTagCompound().getCompoundTag("ExtraAttributes").getByteArray("builder's_wand_data");
                try {
                    NBTTagCompound contents_nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes));
                    NBTTagList items = contents_nbt.getTagList("i", 10);
                    for(int j=0; j<items.tagCount(); j++) {
                        NBTTagCompound buildersItem = items.getCompoundTagAt(j);
                        if(buildersItem.getKeySet().size() > 0) {
                            if(buildersItem.getInteger("id") == Item.getIdFromItem(match.getItem())) {
                                count += items.getCompoundTagAt(j).getByte("Count");
                            }
                        }
                    }
                } catch(Exception e) {
                    return count;
                }
            }
        }

        return count;
    }

    @SubscribeEvent
    public void renderBlockOverlay(DrawBlockHighlightEvent event) {
        if(aoteTeleportationCurr != null && aoteTeleportationMillis > 0) {
            event.setCanceled(true);
        }
        ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();
        String heldInternal = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(held);
        if(heldInternal != null) {
            if(!NotEnoughUpdates.INSTANCE.manager.config.disableTreecapOverlay.value &&
                    (heldInternal.equals("JUNGLE_AXE") || heldInternal.equals("TREECAPITATOR_AXE"))) {
                int maxWood = 10;
                if(heldInternal.equals("TREECAPITATOR_AXE")) maxWood = 35;

                if (event.target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.color(0.0F, 0.0F, 0.0F, 0.4F);
                    GlStateManager.disableTexture2D();
                    GlStateManager.depthMask(false);

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
                                double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)event.partialTicks;
                                double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)event.partialTicks;
                                double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)event.partialTicks;

                                drawFilledBoundingBox(block.getSelectedBoundingBox(Minecraft.getMinecraft().theWorld, candidate)
                                        .expand(0.001D, 0.001D, 0.001D).offset(-d0, -d1, -d2),
                                        random ? 0.5f : 1f, NotEnoughUpdates.INSTANCE.manager.config.treecapOverlayColour.value);
                            }
                        }
                    }

                    GlStateManager.depthMask(true);
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                }
            } else if(!NotEnoughUpdates.INSTANCE.manager.config.disableWandOverlay.value && heldInternal.equals("BUILDERS_WAND")) {
                int maxBlocks = 164;
                if (event.target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    IBlockState hover = Minecraft.getMinecraft().theWorld.getBlockState(event.target.getBlockPos().offset(event.target.sideHit, 1));
                    if(hover.getBlock() == Blocks.air) {
                        EntityPlayer player = event.player;

                        IBlockState match = Minecraft.getMinecraft().theWorld.getBlockState(event.target.getBlockPos());
                        Item matchItem = Item.getItemFromBlock(match.getBlock());
                        if(matchItem != null) {
                            GlStateManager.enableBlend();
                            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                            GlStateManager.disableTexture2D();
                            GlStateManager.depthMask(false);

                            ItemStack matchStack =  new ItemStack(matchItem, 1, match.getBlock().getMetaFromState(match));
                            int itemCount = countItemsInInventoryAndStorage(matchStack);

                            HashSet<BlockPos> candidatesOld = new HashSet<>();
                            TreeMap<Float, Set<BlockPos>> candidatesOldSorted = new TreeMap<>();

                            getBuildersWandCandidates(player, event.target, event.partialTicks, candidatesOld, candidatesOldSorted, 10);

                            String special = (candidatesOld.size() <= itemCount) ? NotEnoughUpdates.INSTANCE.manager.config.wandOverlayColour.value :
                                    "0:255:255:0:0";

                            if(candidatesOld.size() <= maxBlocks) {
                                double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)event.partialTicks;
                                double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)event.partialTicks;
                                double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)event.partialTicks;

                                for(Set<BlockPos> candidatesSorted : candidatesOldSorted.values()) {
                                    for(BlockPos candidate : candidatesSorted) {
                                        match.getBlock().setBlockBoundsBasedOnState(Minecraft.getMinecraft().theWorld, candidate);
                                        AxisAlignedBB bb = match.getBlock().getSelectedBoundingBox(Minecraft.getMinecraft().theWorld, candidate)
                                                .offset(event.target.sideHit.getFrontOffsetX(), event.target.sideHit.getFrontOffsetY(),
                                                        event.target.sideHit.getFrontOffsetZ());

                                        drawBlock((int)bb.minX, (int)bb.minY, (int)bb.minZ+1, match, event.partialTicks, 0.75f);
                                    }
                                }

                                for(BlockPos candidate : candidatesOld) {
                                    match.getBlock().setBlockBoundsBasedOnState(Minecraft.getMinecraft().theWorld, candidate);
                                    AxisAlignedBB bb = match.getBlock().getSelectedBoundingBox(Minecraft.getMinecraft().theWorld, candidate)
                                            .expand(0.001D, 0.001D, 0.001D).offset(-d0, -d1, -d2)
                                            .offset(event.target.sideHit.getFrontOffsetX(), event.target.sideHit.getFrontOffsetY(),
                                                    event.target.sideHit.getFrontOffsetZ());

                                    drawOutlineBoundingBox(bb, 1f, special);
                                }
                            }

                            GlStateManager.depthMask(true);
                            GlStateManager.enableTexture2D();
                            GlStateManager.disableBlend();
                        }
                    }
                }
            }
        }
    }

    public void getBuildersWandCandidates(EntityPlayer player, MovingObjectPosition target, float partialTicks,
                                          HashSet<BlockPos> candidatesOld, TreeMap<Float, Set<BlockPos>> candidatesOldSorted, int extraMax) {
        IBlockState match = Minecraft.getMinecraft().theWorld.getBlockState(target.getBlockPos());

        candidatesOld.clear();
        candidatesOldSorted.clear();
        LinkedList<BlockPos> candidates = new LinkedList<>();
        LinkedList<BlockPos> candidatesNew = new LinkedList<>();

        candidatesNew.add(target.getBlockPos());

        double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTicks;
        double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTicks;
        double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTicks;

        while(candidatesOld.size() <= MAX_BUILDERS_BLOCKS+extraMax) {
            if(candidatesNew.isEmpty()) {
                break;
            }

            candidates.addAll(candidatesNew);
            candidatesNew.clear();

            while(!candidates.isEmpty()) {
                if(candidatesOld.size() > MAX_BUILDERS_BLOCKS+extraMax) break;

                BlockPos candidate = candidates.pop();

                float distSq = (float)((candidate.getX()+0.5f-d0)*(candidate.getX()+0.5f-d0) +
                        (candidate.getY()+0.5f-d1-player.getEyeHeight())*(candidate.getY()+0.5f-d1-player.getEyeHeight()) +
                        (candidate.getZ()+0.5f-d2)*(candidate.getZ()+0.5f-d2));
                candidatesOldSorted.computeIfAbsent(distSq, k->new HashSet<>()).add(candidate);

                candidatesOld.add(candidate);

                for(int x = -1; x <= 1; x++) {
                    for(int y = -1; y <= 1; y++) {
                        for(int z = -1; z <= 1; z++) {
                            if(x*x+y*y+z*z == 1) {
                                if(((x == 0) && (target.sideHit.getAxis() == EnumFacing.Axis.X)) ||
                                        ((y == 0) && (target.sideHit.getAxis() == EnumFacing.Axis.Y)) ||
                                        ((z == 0) && (target.sideHit.getAxis() == EnumFacing.Axis.Z))) {
                                    if(Minecraft.getMinecraft().theWorld.getBlockState(candidate.add(
                                            x+target.sideHit.getFrontOffsetX(),
                                            y+target.sideHit.getFrontOffsetY(),
                                            z+target.sideHit.getFrontOffsetZ())).getBlock() == Blocks.air) {
                                        BlockPos posNew = candidate.add(x, y, z);
                                        if(!candidatesOld.contains(posNew) && !candidates.contains(posNew) && !candidatesNew.contains(posNew)) {
                                            IBlockState blockNew = Minecraft.getMinecraft().theWorld.getBlockState(posNew);
                                            if(blockNew == match) {
                                                candidatesNew.add(posNew);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void drawBlock(int x, int y, int z, IBlockState state, float partialTicks, float brightness) {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTicks;
        double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTicks;
        double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTicks;

        BlockRendererDispatcher blockrendererdispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();

        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.cullFace(GL11.GL_BACK);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x-d0, y-d1, z-d2);

        int i = state.getBlock().getRenderType();
        if(i == 3) {
            IBakedModel ibakedmodel = blockrendererdispatcher.getModelFromBlockState(state, Minecraft.getMinecraft().theWorld, null);

            Block block = state.getBlock();
            block.setBlockBoundsForItemRender();
            GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
            int colour = block.getRenderColor(block.getStateForEntityRender(state));

            if (EntityRenderer.anaglyphEnable) {
                colour = TextureUtil.anaglyphColor(i);
            }

            colour = (colour & 0x00FFFFFF) | (100 << 24); //Set alpha to 100

            for (EnumFacing enumfacing : EnumFacing.values()) {
                renderModelBrightnessColorQuads(colour, ibakedmodel.getFaceQuads(enumfacing));
            }

            renderModelBrightnessColorQuads(colour, ibakedmodel.getGeneralQuads());
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.translate(-x+d0, -y+d1, -z+d2);
        GlStateManager.popMatrix();
    }

    private static void renderModelBrightnessColorQuads(int c, List<BakedQuad> listQuads) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        for (BakedQuad bakedquad : listQuads) {
            worldrenderer.begin(7, DefaultVertexFormats.ITEM);
            worldrenderer.addVertexData(bakedquad.getVertexData());

            worldrenderer.putColor4(c);

            Vec3i vec3i = bakedquad.getFace().getDirectionVec();
            worldrenderer.putNormal((float)vec3i.getX(), (float)vec3i.getY(), (float)vec3i.getZ());
            tessellator.draw();
        }
    }

    public static void drawFilledBoundingBox(AxisAlignedBB p_181561_0_, float alpha, String special) {
        Color c = new Color(SpecialColour.specialToChromaRGB(special), true);
        GlStateManager.color(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, c.getAlpha()/255f*alpha);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

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

    public static void drawOutlineBoundingBox(AxisAlignedBB p_181561_0_, float alpha, String special) {
        Color c = new Color(SpecialColour.specialToChromaRGB(special), true);
        GlStateManager.color(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, c.getAlpha()/255f*alpha);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        GL11.glLineWidth(3);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(3, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(3, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(1, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        tessellator.draw();

        GL11.glLineWidth(1);
    }

}
