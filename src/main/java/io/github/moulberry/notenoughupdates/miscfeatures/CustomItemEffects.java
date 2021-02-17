package io.github.moulberry.notenoughupdates.miscfeatures;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.*;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.List;

public class CustomItemEffects {

    public static final CustomItemEffects INSTANCE = new CustomItemEffects();

    private static final int MAX_BUILDERS_BLOCKS = 241;

    public long aoteUseMillis = 0;

    public long lastUsedHyperion = 0;

    private boolean heldBonemerang = false;

    public final Set<EntityLivingBase> bonemeragedEntities = new HashSet<>();
    public boolean bonemerangBreak = false;

    public int aoteTeleportationMillis = 0;
    public Vector3f aoteTeleportationCurr = null;

    public long lastMillis = 0;

    public Vector3f getCurrentPosition() {
        if(aoteTeleportationMillis <= 0) return null;
        return aoteTeleportationCurr;
    }

    @SubscribeEvent
    public void onTick(TickEvent.RenderTickEvent event) {
        if(Minecraft.getMinecraft().thePlayer == null) return;

        long currentTime = System.currentTimeMillis();
        int delta = (int)(currentTime - lastMillis);
        lastMillis = currentTime;

        if(delta <= 0) return;

        if(aoteTeleportationMillis > NotEnoughUpdates.INSTANCE.config.smoothAOTE.smoothTpMillis*2) {
            aoteTeleportationMillis = NotEnoughUpdates.INSTANCE.config.smoothAOTE.smoothTpMillis*2;
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
        if(event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR || event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();
            String internal = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(held);
            if(internal != null) {
                boolean shadowWarp = false;
                if(internal.equals("HYPERION") || internal.equals("VALKYRIE") || internal.equals("SCYLLA") || internal.equals("ASTRAEA")) {
                    NBTTagCompound tag = held.getTagCompound();
                    if(tag != null && tag.hasKey("ExtraAttributes", 10)) {
                        NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");
                        if(ea != null && ea.hasKey("ability_scroll", 9)) {
                            NBTTagList list = ea.getTagList("ability_scroll", 8);
                            for(int i=0; i<list.tagCount(); i++) {
                                if(list.getStringTagAt(i).equals("IMPLOSION_SCROLL")) {
                                    lastUsedHyperion = System.currentTimeMillis();
                                } else if(list.getStringTagAt(i).equals("SHADOW_WARP_SCROLL")) {
                                    shadowWarp = true;
                                }
                            }
                        }
                    }
                }

                if(NotEnoughUpdates.INSTANCE.config.smoothAOTE.smoothTpMillis <= 0
                        || Minecraft.getMinecraft().gameSettings.thirdPersonView != 0) return;

                boolean aote = NotEnoughUpdates.INSTANCE.config.smoothAOTE.enableSmoothAOTE && internal.equals("ASPECT_OF_THE_END");
                boolean hyp = NotEnoughUpdates.INSTANCE.config.smoothAOTE.enableSmoothHyperion && shadowWarp;
                if(aote || hyp) {
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
    public void onGameTick(TickEvent.ClientTickEvent event) {
        if(event.phase != TickEvent.Phase.END) return;

        heldBonemerang = false;
        bonemerangBreak = false;
        bonemeragedEntities.clear();
        if(Minecraft.getMinecraft().thePlayer == null) return;
        if(Minecraft.getMinecraft().theWorld == null) return;

        ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();

        String internal = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(held);
        if(internal != null && internal.equals("BONE_BOOMERANG")) {
            heldBonemerang = true;

            EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
            float stepSize = 0.15f;
            float bonemerangDistance = 15;

            Vector3f position = new Vector3f((float)p.posX, (float)p.posY + p.getEyeHeight(), (float)p.posZ);
            Vec3 look = p.getLook(0);

            Vector3f step = new Vector3f((float)look.xCoord, (float)look.yCoord, (float)look.zCoord);
            step.scale(stepSize / step.length());

            for(int i=0; i<Math.floor(bonemerangDistance/stepSize)-2; i++) {
                AxisAlignedBB bb = new AxisAlignedBB(position.x - 0.75f, position.y - 0.1, position.z - 0.75f,
                        position.x + 0.75f, position.y + 0.25, position.z + 0.75);

                BlockPos blockPos = new BlockPos(position.x, position.y, position.z);

                if(!Minecraft.getMinecraft().theWorld.isAirBlock(blockPos) &&
                        Minecraft.getMinecraft().theWorld.getBlockState(blockPos).getBlock().isFullCube()) {
                    if(NotEnoughUpdates.INSTANCE.config.bonemerangOverlay.showBreak) {
                        bonemerangBreak = true;
                    }
                    break;
                }

                if(NotEnoughUpdates.INSTANCE.config.bonemerangOverlay.highlightTargeted) {
                    List<Entity> entities = Minecraft.getMinecraft().theWorld.getEntitiesWithinAABBExcludingEntity(Minecraft.getMinecraft().thePlayer, bb);
                    for(Entity entity : entities) {
                        if(entity instanceof EntityLivingBase && !(entity instanceof EntityArmorStand)) {
                            if(!bonemeragedEntities.contains(entity)) {
                                bonemeragedEntities.add((EntityLivingBase)entity);
                            }
                        }
                    }
                }

                position.translate(step.x, step.y, step.z);
            }
        }


    }

    @SubscribeEvent
    public void onOverlayDrawn(RenderGameOverlayEvent.Post event) {
        if(((event.type == null && Loader.isModLoaded("labymod")) ||
                event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS)) {
            if(heldBonemerang) {
                if(bonemerangBreak) {
                    ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
                    Utils.drawStringCentered(EnumChatFormatting.RED+"Bonemerang will break!",
                            Minecraft.getMinecraft().fontRendererObj,
                            scaledResolution.getScaledWidth()/2f, scaledResolution.getScaledHeight()/2f+10, true, 0);
                }
            } else if(NotEnoughUpdates.INSTANCE.config.builderWand.enableWandOverlay &&
                    Minecraft.getMinecraft().objectMouseOver != null &&
                    Minecraft.getMinecraft().objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {

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

                            getBuildersWandCandidates(Minecraft.getMinecraft().thePlayer, Minecraft.getMinecraft().objectMouseOver, event.partialTicks,
                                    candidatesOld, candidatesOldSorted, 999-MAX_BUILDERS_BLOCKS);

                            boolean usingDirtWand = false;
                            int itemCount;
                            if(match.getBlock() == Blocks.dirt && matchStack.getItemDamage() == 0 && hasDirtWand()) {
                                itemCount = candidatesOld.size();
                                usingDirtWand = true;
                            } else {
                                itemCount = countItemsInInventoryAndStorage(matchStack);
                            }

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

                            String itemCountS = EnumChatFormatting.DARK_GRAY+"x"+EnumChatFormatting.RESET+itemCount;
                            int itemCountLen = Minecraft.getMinecraft().fontRendererObj.getStringWidth(itemCountS);

                            if(NotEnoughUpdates.INSTANCE.config.builderWand.wandBlockCount) {
                                if(usingDirtWand) {
                                    Utils.drawItemStack(new ItemStack(Items.gold_nugget), scaledResolution.getScaledWidth()/2 - (itemCountLen+16)/2,
                                            scaledResolution.getScaledHeight()/2+10+4);
                                    Minecraft.getMinecraft().fontRendererObj.drawString(itemCountS,
                                            scaledResolution.getScaledWidth()/2f - (itemCountLen+16)/2f+11, scaledResolution.getScaledHeight()/2f+10+8,
                                            -1,
                                            true);
                                } else {
                                    Utils.drawItemStack(matchStack, scaledResolution.getScaledWidth()/2 - (itemCountLen+16)/2,
                                            scaledResolution.getScaledHeight()/2+10+4);
                                    Minecraft.getMinecraft().fontRendererObj.drawString(itemCountS,
                                            scaledResolution.getScaledWidth()/2f - (itemCountLen+16)/2f+16, scaledResolution.getScaledHeight()/2f+10+8,
                                            -1,
                                            true);
                                }

                            }

                            GlStateManager.color(1, 1, 1, 1);
                        }

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
            if(held.hasTagCompound() && held.getTagCompound().hasKey("ExtraAttributes", 10) &&
                    held.getTagCompound().getCompoundTag("ExtraAttributes").hasKey("builder's_wand_data", 7)) {
                byte[] bytes = held.getTagCompound().getCompoundTag("ExtraAttributes").getByteArray("builder's_wand_data");
                try {
                    NBTTagCompound contents_nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes));
                    NBTTagList items = contents_nbt.getTagList("i", 10);
                    for(int j=0; j<items.tagCount(); j++) {
                        NBTTagCompound buildersItem = items.getCompoundTagAt(j);
                        if(buildersItem.getKeySet().size() > 0) {
                            if(buildersItem.getInteger("id") == Item.getIdFromItem(match.getItem()) &&
                                    buildersItem.getInteger("Damage") == match.getItemDamage()) {
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

    public boolean hasDirtWand() {
        for(ItemStack stack : Minecraft.getMinecraft().thePlayer.inventory.mainInventory) {
            String internalname = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(stack);
            if(internalname != null && internalname.equals("INFINIDIRT_WAND")) {
                return true;
            }
        }

        ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();
        String heldInternal = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(held);

        if(heldInternal != null && heldInternal.equals("BUILDERS_WAND")) {
            if(held.hasTagCompound() && held.getTagCompound().hasKey("ExtraAttributes", 10) &&
                    held.getTagCompound().getCompoundTag("ExtraAttributes").hasKey("builder's_wand_data", 7)) {
                byte[] bytes = held.getTagCompound().getCompoundTag("ExtraAttributes").getByteArray("builder's_wand_data");
                try {
                    NBTTagCompound contents_nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes));
                    NBTTagList items = contents_nbt.getTagList("i", 10);
                    for(int j=0; j<items.tagCount(); j++) {
                        NBTTagCompound buildersItem = items.getCompoundTagAt(j);
                        if(buildersItem.getKeySet().size() > 0) {
                            String internalname = NotEnoughUpdates.INSTANCE.manager.getInternalnameFromNBT(buildersItem.getCompoundTag("tag"));
                            if(internalname != null && internalname.equals("INFINIDIRT_WAND")) {
                                return true;
                            }
                        }
                    }
                } catch(Exception e) {
                }
            }
        }

        return false;
    }

    @SubscribeEvent
    public void renderBlockOverlay(DrawBlockHighlightEvent event) {
        if(aoteTeleportationCurr != null && aoteTeleportationMillis > 0) {
            event.setCanceled(true);
        }
        ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();
        String heldInternal = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(held);
        if(heldInternal != null) {
            EntityPlayer player = event.player;
            double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)event.partialTicks;
            double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)event.partialTicks;
            double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)event.partialTicks;

            if(NotEnoughUpdates.INSTANCE.config.treecap.enableTreecapOverlay &&
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

                                drawFilledBoundingBox(block.getSelectedBoundingBox(Minecraft.getMinecraft().theWorld, candidate)
                                        .expand(0.001D, 0.001D, 0.001D).offset(-d0, -d1, -d2),
                                        random ? 0.5f : 1f, NotEnoughUpdates.INSTANCE.config.treecap.treecapOverlayColour);
                            }
                        }
                    }

                    GlStateManager.depthMask(true);
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                }
            } else if(NotEnoughUpdates.INSTANCE.config.builderWand.enableWandOverlay) {
                if(heldInternal.equals("BUILDERS_WAND")) {
                    int maxBlocks = MAX_BUILDERS_BLOCKS;
                    if (event.target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                        IBlockState hover = Minecraft.getMinecraft().theWorld.getBlockState(event.target.getBlockPos().offset(event.target.sideHit, 1));
                        if(hover.getBlock() == Blocks.air) {
                            IBlockState match = Minecraft.getMinecraft().theWorld.getBlockState(event.target.getBlockPos());
                            Item matchItem = Item.getItemFromBlock(match.getBlock());
                            if(matchItem != null) {
                                GlStateManager.enableBlend();
                                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                                GlStateManager.disableTexture2D();
                                GlStateManager.depthMask(false);

                                HashSet<BlockPos> candidatesOld = new HashSet<>();
                                TreeMap<Float, Set<BlockPos>> candidatesOldSorted = new TreeMap<>();

                                getBuildersWandCandidates(player, event.target, event.partialTicks, candidatesOld, candidatesOldSorted, 10);

                                ItemStack matchStack =  new ItemStack(matchItem, 1,
                                        match.getBlock().getDamageValue(Minecraft.getMinecraft().theWorld, event.target.getBlockPos()));
                                int itemCount;
                                if(match.getBlock() == Blocks.dirt && matchStack.getItemDamage() == 0 && hasDirtWand()) {
                                    itemCount = candidatesOld.size();
                                } else {
                                    itemCount = countItemsInInventoryAndStorage(matchStack);
                                }

                                String special = (candidatesOld.size() <= itemCount) ? NotEnoughUpdates.INSTANCE.config.builderWand.wandOverlayColour :
                                        "0:255:255:0:0";

                                if(candidatesOld.size() <= maxBlocks) {
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
                } else if(heldInternal.equals("INFINIDIRT_WAND") && event.target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    BlockPos hover = event.target.getBlockPos().offset(event.target.sideHit, 1);
                    IBlockState hoverState = Minecraft.getMinecraft().theWorld.getBlockState(event.target.getBlockPos().offset(event.target.sideHit, 1));
                    if(hoverState.getBlock() == Blocks.air) {
                        GlStateManager.enableBlend();
                        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                        GlStateManager.disableTexture2D();
                        GlStateManager.depthMask(false);

                        String special = NotEnoughUpdates.INSTANCE.config.builderWand.wandOverlayColour;

                        AxisAlignedBB bb = Blocks.dirt.getSelectedBoundingBox(Minecraft.getMinecraft().theWorld, hover);
                        drawBlock((int)bb.minX, (int)bb.minY, (int)bb.minZ+1, Blocks.dirt.getDefaultState(),
                                event.partialTicks, 0.75f);

                        AxisAlignedBB bbExpanded = Blocks.dirt.getSelectedBoundingBox(Minecraft.getMinecraft().theWorld, hover)
                                .expand(0.001D, 0.001D, 0.001D).offset(-d0, -d1, -d2);
                        drawOutlineBoundingBox(bbExpanded, 1f, special);

                        GlStateManager.depthMask(true);
                        GlStateManager.enableTexture2D();
                        GlStateManager.disableBlend();
                    }
                } else if((heldInternal.equals("WATER_BUCKET") || heldInternal.equals("MAGICAL_WATER_BUCKET")) &&
                        event.target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    BlockPos hover = event.target.getBlockPos().offset(event.target.sideHit, 1);

                    HashSet<BlockPos> verticalSources = new HashSet<>();
                    TreeMap<Float, HashMap<BlockPos, EnumFacing>> watersSorted = new TreeMap<>();

                    for(int xOff=-1; xOff<=1; xOff++) {
                        for(int yOff=0; yOff<=1; yOff++) {
                            for(int zOff=-1; zOff<=1; zOff++) {
                                if((xOff == 0 && yOff == 0) ||
                                    (xOff == 0 && zOff == 0) ||
                                    (zOff == 0 && yOff == 0)) {

                                    BlockPos checkPos = hover.add(-xOff, -yOff, -zOff);
                                    IBlockState check = Minecraft.getMinecraft().theWorld.getBlockState(checkPos);
                                    if(check.getBlock() == Blocks.prismarine && check.getBlock().getMetaFromState(check) == 2) {
                                        for(int i=0; i<300; i++) {
                                            BlockPos renderPos = hover.add(xOff*i, yOff*i, zOff*i);

                                            if(Math.abs(renderPos.getX()) > 128) {
                                                break;
                                            }
                                            if(Math.abs(renderPos.getY()) > 255) {
                                                break;
                                            }
                                            if(Math.abs(renderPos.getZ()) > 128) {
                                                break;
                                            }

                                            IBlockState renderState = Minecraft.getMinecraft().theWorld.getBlockState(renderPos);

                                            if(renderState.getBlock() != Blocks.air && renderState.getBlock() != Blocks.water &&
                                                renderState.getBlock() != Blocks.flowing_water) {
                                                break;
                                            }

                                            if(yOff != 0) {
                                                verticalSources.add(renderPos);
                                            } else {
                                                IBlockState belowState = Minecraft.getMinecraft().theWorld.getBlockState(renderPos.add(0, -1, 0));
                                                if(belowState.getBlock() == Blocks.air) {
                                                    break;
                                                }
                                            }

                                            for(EnumFacing facing : EnumFacing.values()) {
                                                float xDist = (float)(renderPos.getX()+0.5f+0.5f*facing.getFrontOffsetX()-d0);
                                                float yDist = (float)(renderPos.getY()+0.5f+0.5f*facing.getFrontOffsetY()-d1-player.getEyeHeight());
                                                float zDist = (float)(renderPos.getZ()+0.5f+0.5f*facing.getFrontOffsetZ()-d2);

                                                float distSq = xDist*xDist + yDist*yDist + zDist*zDist;

                                                watersSorted.computeIfAbsent(distSq, k->new HashMap<>()).put(renderPos, facing);
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }

                    GlStateManager.enableDepth();
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableTexture2D();
                    GlStateManager.depthMask(true);

                    for(HashMap<BlockPos, EnumFacing> blockPoses : watersSorted.values()) {
                        for(Map.Entry<BlockPos, EnumFacing> entry : blockPoses.entrySet()) {
                            boolean vertical = verticalSources.contains(entry.getKey());
                            AxisAlignedBB bbExpanded = Blocks.water.getSelectedBoundingBox(Minecraft.getMinecraft().theWorld, entry.getKey())
                                    .expand(-0.001D, -0.001D-(vertical?0:0.0625D), -0.001D)
                                    .offset(-d0, -d1-(vertical?0:0.0625), -d2);
                            drawFilledBoundingBoxSide(bbExpanded, entry.getValue(), 1f, "0:100:20:50:160");
                        }
                    }

                    GlStateManager.depthMask(true);
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                }  else if((heldInternal.equals("HOE_OF_GREAT_TILLING") || heldInternal.equals("HOE_OF_GREATER_TILLING")) &&
                        event.target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    BlockPos target = event.target.getBlockPos();
                    IBlockState targetState = Minecraft.getMinecraft().theWorld.getBlockState(target);

                    int radius = heldInternal.equals("HOE_OF_GREAT_TILLING") ? 1 : 2;

                    if(targetState.getBlock() == Blocks.dirt || targetState.getBlock() == Blocks.grass) {
                        GlStateManager.enableDepth();
                        GlStateManager.enableBlend();
                        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                        GlStateManager.disableTexture2D();
                        GlStateManager.depthMask(true);

                        for(int xOff=-radius; xOff<=radius; xOff++) {
                            for(int zOff=-radius; zOff<=radius; zOff++) {
                                BlockPos renderPos = target.add(xOff, 0, zOff);
                                IBlockState renderState = Minecraft.getMinecraft().theWorld.getBlockState(renderPos);
                                if(renderState.getBlock() == Blocks.dirt || renderState.getBlock() == Blocks.grass) {
                                    AxisAlignedBB bbExpanded = Blocks.dirt.getSelectedBoundingBox(Minecraft.getMinecraft().theWorld, renderPos)
                                            .expand(0.001D, 0.001D, 0.001D)
                                            .offset(-d0, -d1, -d2);
                                    drawFilledBoundingBox(bbExpanded, 1f, "0:100:178:34:34");
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

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        GlStateManager.color(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, c.getAlpha()/255f*alpha);

        //vertical
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        tessellator.draw();


        GlStateManager.color(c.getRed()/255f*0.8f, c.getGreen()/255f*0.8f, c.getBlue()/255f*0.8f, c.getAlpha()/255f*alpha);

        //x
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        tessellator.draw();


        GlStateManager.color(c.getRed()/255f*0.9f, c.getGreen()/255f*0.9f, c.getBlue()/255f*0.9f, c.getAlpha()/255f*alpha);
        //z
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
        tessellator.draw();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
        tessellator.draw();
    }

    public static void drawFilledBoundingBoxSide(AxisAlignedBB p_181561_0_, EnumFacing facing, float alpha, String special) {
        Color c = new Color(SpecialColour.specialToChromaRGB(special), true);
        GlStateManager.color(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, c.getAlpha()/255f*alpha);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        switch(facing) {
            case UP:
                worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
                worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
                worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
                worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
                break;
            case DOWN:
                worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
                worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
                worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
                worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
                break;
            case EAST:
                worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
                worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
                worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
                worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
                break;
            case WEST:
                worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
                worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
                worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
                worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
                break;
            case SOUTH:
                worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
                worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.maxZ).endVertex();
                worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
                worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.maxZ).endVertex();
                break;
            case NORTH:
                worldrenderer.pos(p_181561_0_.minX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
                worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.minY, p_181561_0_.minZ).endVertex();
                worldrenderer.pos(p_181561_0_.maxX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
                worldrenderer.pos(p_181561_0_.minX, p_181561_0_.maxY, p_181561_0_.minZ).endVertex();
                break;
        }
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
