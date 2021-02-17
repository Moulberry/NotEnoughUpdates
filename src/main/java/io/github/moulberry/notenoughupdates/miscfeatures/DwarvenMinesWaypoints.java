package io.github.moulberry.notenoughupdates.miscfeatures;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.overlays.MiningOverlay;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DwarvenMinesWaypoints {

    private HashMap<String, Vector3f> waypointsMap = new HashMap<>();
    {
        waypointsMap.put("Dwarven Village", new Vector3f(-37, 199, -122));
        waypointsMap.put("Miner's Guild", new Vector3f(-74, 220, -122));
        waypointsMap.put("Fetchur", new Vector3f(85, 223, -120));
        waypointsMap.put("Palace Bridge", new Vector3f(129, 186, 8));
        waypointsMap.put("Royal Palace", new Vector3f(129, 194, 194));
        waypointsMap.put("Puzzler", new Vector3f(181, 195, 135));
        waypointsMap.put("Grand Library", new Vector3f(183, 195, 181));
        waypointsMap.put("Barracks of Heroes", new Vector3f(93, 195, 181));
        waypointsMap.put("Royal Mines", new Vector3f(178, 149, 71));
        waypointsMap.put("Cliffside Veins", new Vector3f(40, 136, 17));
        waypointsMap.put("Forge Basin", new Vector3f(0, 169, -2));
        waypointsMap.put("The Forge", new Vector3f(0, 148, -69));
        waypointsMap.put("Rampart's Quarry", new Vector3f(-106, 147, 2));
        waypointsMap.put("Far Reserve", new Vector3f(-160, 148, 17));
        waypointsMap.put("Upper Mines", new Vector3f(-123, 170, -71));
        waypointsMap.put("Goblin Burrows", new Vector3f(-138, 143, 141));
        waypointsMap.put("Great Ice Wall", new Vector3f(0, 127, 160));
        waypointsMap.put("Aristocrat Passage", new Vector3f(129, 150, 137));
        waypointsMap.put("Hanging Court", new Vector3f(91, 186, 129));
        waypointsMap.put("Divan's Gateway", new Vector3f(0, 127, 87));
        waypointsMap.put("Lava Springs", new Vector3f(57, 196, -15));
        waypointsMap.put("The Mist", new Vector3f(0, 75, 82));
    }

    private static final HashSet<String> emissaryNames = new HashSet<>();
    static {
        emissaryNames.add(EnumChatFormatting.GOLD+"Emissary Ceanna"+EnumChatFormatting.RESET);
        emissaryNames.add(EnumChatFormatting.GOLD+"Emissary Carlton"+EnumChatFormatting.RESET);
        emissaryNames.add(EnumChatFormatting.GOLD+"Emissary Wilson"+EnumChatFormatting.RESET);
        emissaryNames.add(EnumChatFormatting.GOLD+"Emissary Lilith"+EnumChatFormatting.RESET);
        emissaryNames.add(EnumChatFormatting.GOLD+"Emissary Frasier"+EnumChatFormatting.RESET);
        emissaryNames.add(EnumChatFormatting.GOLD+"Emissary Eliza"+EnumChatFormatting.RESET);
        emissaryNames.add(EnumChatFormatting.GOLD.toString()+EnumChatFormatting.BOLD+"King Thormyr"+EnumChatFormatting.RESET);
    }

    private enum Emissary {
        THORMYR("King Thormyr", 0, new Vector3f(129, 196, 196)),
        CEANNA("Emissary Ceanna", 1, new Vector3f(42, 134, 22)),
        CARLTON("Emissary Carlton", 1, new Vector3f(-73, 153, -11)),
        WILSON("Emissary Wilson", 2, new Vector3f(171, 150, 31)),
        LILITH("Emissary Lilith", 2, new Vector3f(58, 198, -8)),
        FRAISER("Emissary Frasier", 3, new Vector3f(-132, 174, -50)),
        ELIZA("Emissary Eliza", 3, new Vector3f(-37, 200, -131));

        String name;
        int minMilestone;
        Vector3f loc;
        Emissary(String name, int minMilestone, Vector3f loc) {
            this.name = name;
            this.minMilestone = minMilestone;
            this.loc = loc;
        }
    }

    private long dynamicMillis = 0;
    private String dynamicLocation = null;
    private String dynamicName = null;
    private final Pattern ghastRegex = Pattern.compile("\u00A7r\u00A7eFind the \u00A7r\u00A76Powder Ghast\u00A7r\u00A7e near the \u00A7r\u00A7b(.+)!");
    private final Pattern fallenStarRegex = Pattern.compile("\u00A7r\u00A75Fallen Star \u00A7r\u00A7ehas crashed at \u00A7r\u00A7b(.+)\u00A7r\u00A7e!");

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        Matcher matcherGhast = ghastRegex.matcher(event.message.getFormattedText());
        if(matcherGhast.find()) {
            dynamicLocation = Utils.cleanColour(matcherGhast.group(1).trim());
            dynamicName = EnumChatFormatting.GOLD+"Powder Ghast";
            dynamicMillis = System.currentTimeMillis();
        } else {
            Matcher matcherStar = fallenStarRegex.matcher(event.message.getFormattedText());
            if(matcherStar.find()) {
                dynamicLocation = Utils.cleanColour(matcherStar.group(1).trim());
                dynamicName = EnumChatFormatting.DARK_PURPLE+"Fallen Star";
                dynamicMillis = System.currentTimeMillis();
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        emissaryRemovedDistSq = -1;

        if(SBInfo.getInstance().getLocation() == null) return;
        if(!SBInfo.getInstance().getLocation().equals("mining_3")) return;

        if(Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
            GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            IInventory lower = container.getLowerChestInventory();

            if(lower.getDisplayName().getFormattedText().contains("Commissions")) {
                for(int i=0; i<lower.getSizeInventory(); i++) {
                    ItemStack stack = lower.getStackInSlot(i);
                    if(stack == null) continue;
                    if(stack.getDisplayName().equals(EnumChatFormatting.YELLOW+"Commission Milestones")) {
                        NotEnoughUpdates.INSTANCE.config.hidden.commissionMilestone = 5;
                        String[] lore = NotEnoughUpdates.INSTANCE.manager.getLoreFromNBT(stack.getTagCompound());
                        for(String line : lore) {
                            String clean = Utils.cleanColour(line);
                            if(clean.equals("Tier I Rewards:")) {
                                NotEnoughUpdates.INSTANCE.config.hidden.commissionMilestone = 0;
                            } else if(clean.equals("Tier II Rewards:")) {
                                NotEnoughUpdates.INSTANCE.config.hidden.commissionMilestone = 1;
                            } else if(clean.equals("Tier III Rewards:")) {
                                NotEnoughUpdates.INSTANCE.config.hidden.commissionMilestone = 2;
                            } else if(clean.equals("Tier IV Rewards:")) {
                                NotEnoughUpdates.INSTANCE.config.hidden.commissionMilestone = 3;
                            } else if(clean.equals("Tier V Rewards:")) {
                                NotEnoughUpdates.INSTANCE.config.hidden.commissionMilestone = 4;
                            }
                        }
                        return;
                    }
                }
            }
        }
    }

    private boolean commissionFinished = false;
    private double emissaryRemovedDistSq = 0;

    @SubscribeEvent
    public void onRenderSpecial(RenderLivingEvent.Specials.Pre<EntityArmorStand> event) {
        if(SBInfo.getInstance().getLocation() == null) return;
        if(!SBInfo.getInstance().getLocation().equals("mining_3")) return;

        if(commissionFinished && event.entity instanceof EntityArmorStand) {
            String name = event.entity.getDisplayName().getFormattedText();
            if(emissaryRemovedDistSq > 0 && name.equals(EnumChatFormatting.YELLOW.toString()+EnumChatFormatting.BOLD+"CLICK"+EnumChatFormatting.RESET)) {
                EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
                double distSq = event.entity.getDistanceSq(p.posX, p.posY, p.posZ);
                if(Math.abs(distSq - emissaryRemovedDistSq) < 1) {
                    event.setCanceled(true);
                }
            } else if(emissaryNames.contains(name)) {
                EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
                double distSq = event.entity.getDistanceSq(p.posX, p.posY, p.posZ);
                if(distSq >= 12*12) {
                    emissaryRemovedDistSq = distSq;
                    event.setCanceled(true);
                }
            }
        }
    }


    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        if(SBInfo.getInstance().getLocation() == null) return;
        if(!SBInfo.getInstance().getLocation().equals("mining_3")) return;

        int locWaypoint = NotEnoughUpdates.INSTANCE.config.mining.locWaypoints;

        if(dynamicLocation != null && dynamicName != null &&
                System.currentTimeMillis() - dynamicMillis < 30*1000) {
            for(Map.Entry<String, Vector3f> entry : waypointsMap.entrySet()) {
                if(entry.getKey().equals(dynamicLocation)) {
                    renderWayPoint(dynamicName, new Vector3f(entry.getValue()).translate(0, 15, 0), event.partialTicks);
                    break;
                }
            }
        }

        if(locWaypoint >= 1) {
            for(Map.Entry<String, Vector3f> entry : waypointsMap.entrySet()) {
                if(locWaypoint >= 2) {
                    renderWayPoint(EnumChatFormatting.AQUA+entry.getKey(), entry.getValue(), event.partialTicks);
                } else {
                    for(String commissionName : MiningOverlay.commissionProgress.keySet()) {
                        if(commissionName.toLowerCase().contains(entry.getKey().toLowerCase())) {
                            if(commissionName.contains("Titanium")) {
                                renderWayPoint(EnumChatFormatting.WHITE+entry.getKey(), entry.getValue(), event.partialTicks);
                            } else {
                                renderWayPoint(EnumChatFormatting.AQUA+entry.getKey(), entry.getValue(), event.partialTicks);
                            }
                        }
                    }
                }
            }
        }

        commissionFinished = NotEnoughUpdates.INSTANCE.config.mining.emissaryWaypoints >= 2;

        if(NotEnoughUpdates.INSTANCE.config.mining.emissaryWaypoints == 0) return;

        if(!commissionFinished) {
            for(float f : MiningOverlay.commissionProgress.values()) {
                if (f >= 1) {
                    commissionFinished = true;
                    break;
                }
            }
        }
        if(commissionFinished) {
            for(Emissary emissary : Emissary.values()) {
                if(NotEnoughUpdates.INSTANCE.config.hidden.commissionMilestone >= emissary.minMilestone) {

                    EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
                    double dX = emissary.loc.x + 0.5f - p.posX;
                    double dY = emissary.loc.y + 0.188f - p.posY;
                    double dZ = emissary.loc.z + 0.5f - p.posZ;

                    double distSq = dX*dX + dY*dY + dZ*dZ;
                    if(distSq >= 12*12) {
                        renderWayPoint(EnumChatFormatting.GOLD+emissary.name,
                                new Vector3f(emissary.loc).translate(0.5f, 2.488f, 0.5f),
                                event.partialTicks);
                    }
                }
            }
        }
    }

    private void renderWayPoint(String str, Vector3f loc, float partialTicks) {
        GlStateManager.alphaFunc(516, 0.1F);

        GlStateManager.pushMatrix();

        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;

        double x = loc.x-viewerX+0.5f;
        double y = loc.y-viewerY-viewer.getEyeHeight();
        double z = loc.z-viewerZ+0.5f;

        double distSq = x*x + y*y + z*z;
        double dist = Math.sqrt(distSq);
        if(distSq > 144) {
            x *= 12/dist;
            y *= 12/dist;
            z *= 12/dist;
        }
        GlStateManager.translate(x, y, z);
        GlStateManager.translate(0, viewer.getEyeHeight(), 0);

        renderNametag(str);

        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0, -0.25f, 0);
        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);

        renderNametag(EnumChatFormatting.YELLOW.toString()+Math.round(dist)+"m");

        GlStateManager.popMatrix();

        GlStateManager.disableLighting();
    }

    private void renderNametag(String str) {
        FontRenderer fontrenderer = Minecraft.getMinecraft().fontRendererObj;
        float f = 1.6F;
        float f1 = 0.016666668F * f;
        GlStateManager.pushMatrix();
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-f1, -f1, f1);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        int i = 0;

        int j = fontrenderer.getStringWidth(str) / 2;
        GlStateManager.disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos((double)(-j - 1), (double)(-1 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double)(-j - 1), (double)(8 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double)(j + 1), (double)(8 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double)(j + 1), (double)(-1 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        fontrenderer.drawString(str, -fontrenderer.getStringWidth(str) / 2, i, 553648127);
        GlStateManager.depthMask(true);

        fontrenderer.drawString(str, -fontrenderer.getStringWidth(str) / 2, i, -1);

        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

}
