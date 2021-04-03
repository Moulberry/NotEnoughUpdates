package io.github.moulberry.notenoughupdates.miscfeatures;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.commands.SimpleCommand;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class WaypointBeacons {

    public static void loadFoundWaypoints(File file, Gson gson, WaypointBeaconData waypointData) {
        waypointData.foundWaypoints = new HashMap<>();
        if(file.exists()) {
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                HashMap<String, List<Number>> foundLocationsList = gson.fromJson(reader, HashMap.class);

                for(Map.Entry<String, List<Number>> entry : foundLocationsList.entrySet()) {
                    HashSet<Integer> set = new HashSet<>();
                    for(Number n : entry.getValue()) {
                        set.add(n.intValue());
                    }
                    waypointData.foundWaypoints.put(entry.getKey(), set);
                }
            } catch(Exception e) {}
        }
    }

    public static void saveFoundWaypoints(File file, Gson gson, WaypointBeaconData waypointData) {
        try {
            file.createNewFile();

            try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write(gson.toJson(waypointData.foundWaypoints));
            }
        } catch(IOException ignored) {}
    }

    public static Boolean markClosestRelicFound(WaypointBeaconData waypointData) {
        String location = SBInfo.getInstance().getLocation();
        if(location == null) return false;

        int closestIndex = -1;
        double closestDistSq = 10*10;
        for(int i = 0; i< waypointData.currentWaypointList.size(); i++) {
            BlockPos pos = waypointData.currentWaypointList.get(i);

            double distSq = pos.distanceSq(Minecraft.getMinecraft().thePlayer.getPosition());

            if(distSq < closestDistSq) {
                closestDistSq = distSq;
                closestIndex = i;
            }
        }
        if(closestIndex != -1) {
            Set<Integer> found = waypointData.foundWaypoints.computeIfAbsent(location, k -> new HashSet<>());
            found.add(closestIndex);
            return true;
        }

        return false;
    }

    public static void tick(JsonObject waypointTypeConstant,
                            WaypointBeaconData waypointData) {
        if(!waypointData.enabled) return;

        JsonObject beacons = waypointTypeConstant;
        if(beacons == null) return;

        String location = SBInfo.getInstance().getLocation();
        if(location == null) {
            waypointData.currentWaypointList = null;
            return;
        }

        if(waypointData.currentWaypointList == null) {
            if(beacons.has(location) && beacons.get(location).isJsonArray()) {
                JsonArray locations = beacons.get(location).getAsJsonArray();
                waypointData.currentWaypointList = new ArrayList<>();
                for(int i=0; i<locations.size(); i++) {
                    try {
                        String coord = locations.get(i).getAsString();

                        String[] split = coord.split(",");
                        if(split.length == 3) {
                            String xS = split[0];
                            String yS = split[1];
                            String zS = split[2];

                            int x = Integer.parseInt(xS);
                            int y = Integer.parseInt(yS);
                            int z = Integer.parseInt(zS);

                            waypointData.currentWaypointList.add(new BlockPos(x, y , z));
                        }
                    } catch(Exception ignored) {}
                }
            }
        }

        if(waypointData.currentWaypointList != null && !waypointData.currentWaypointList.isEmpty()) {
            TreeMap<Double, BlockPos> distanceSqMap = new TreeMap<>();

            Set<Integer> found = waypointData.foundWaypoints.computeIfAbsent(location, k -> new HashSet<>());

            for(int i = 0; i< waypointData.currentWaypointList.size(); i++) {
                if(found.contains(i)) continue;

                BlockPos pos = waypointData.currentWaypointList.get(i);
                double distSq = pos.distanceSq(Minecraft.getMinecraft().thePlayer.getPosition());
                distanceSqMap.put(distSq, pos);
            }

            int maxSouls = 15;
            int souls = 0;
            waypointData.currentWaypointListClose = new ArrayList<>();
            for(BlockPos pos : distanceSqMap.values()) {
                waypointData.currentWaypointListClose.add(pos);
                if(++souls >= maxSouls) break;
            }
        }
    }

    private static final ResourceLocation beaconBeam = new ResourceLocation("textures/entity/beacon_beam.png");

    private static void renderBeaconBeam(double x, double y, double z, int rgb, float alphaMult, float partialTicks) {
        int height = 300;
        int bottomOffset = 0;
        int topOffset = bottomOffset + height;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        Minecraft.getMinecraft().getTextureManager().bindTexture(beaconBeam);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 10497.0F);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 10497.0F);
        GlStateManager.disableLighting();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        double time = Minecraft.getMinecraft().theWorld.getTotalWorldTime() + (double)partialTicks;
        double d1 = MathHelper.func_181162_h(-time * 0.2D - (double)MathHelper.floor_double(-time * 0.1D));

        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        double d2 = time * 0.025D * -1.5D;
        double d4 = 0.5D + Math.cos(d2 + 2.356194490192345D) * 0.2D;
        double d5 = 0.5D + Math.sin(d2 + 2.356194490192345D) * 0.2D;
        double d6 = 0.5D + Math.cos(d2 + (Math.PI / 4D)) * 0.2D;
        double d7 = 0.5D + Math.sin(d2 + (Math.PI / 4D)) * 0.2D;
        double d8 = 0.5D + Math.cos(d2 + 3.9269908169872414D) * 0.2D;
        double d9 = 0.5D + Math.sin(d2 + 3.9269908169872414D) * 0.2D;
        double d10 = 0.5D + Math.cos(d2 + 5.497787143782138D) * 0.2D;
        double d11 = 0.5D + Math.sin(d2 + 5.497787143782138D) * 0.2D;
        double d14 = -1.0D + d1;
        double d15 = (double)(height) * 2.5D + d14;
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldrenderer.pos(x + d4, y + topOffset, z + d5).tex(1.0D, d15).color(r, g, b, 1.0F*alphaMult).endVertex();
        worldrenderer.pos(x + d4, y + bottomOffset, z + d5).tex(1.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d6, y + bottomOffset, z + d7).tex(0.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d6, y + topOffset, z + d7).tex(0.0D, d15).color(r, g, b, 1.0F*alphaMult).endVertex();
        worldrenderer.pos(x + d10, y + topOffset, z + d11).tex(1.0D, d15).color(r, g, b, 1.0F*alphaMult).endVertex();
        worldrenderer.pos(x + d10, y + bottomOffset, z + d11).tex(1.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d8, y + bottomOffset, z + d9).tex(0.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d8, y + topOffset, z + d9).tex(0.0D, d15).color(r, g, b, 1.0F*alphaMult).endVertex();
        worldrenderer.pos(x + d6, y + topOffset, z + d7).tex(1.0D, d15).color(r, g, b, 1.0F*alphaMult).endVertex();
        worldrenderer.pos(x + d6, y + bottomOffset, z + d7).tex(1.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d10, y + bottomOffset, z + d11).tex(0.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d10, y + topOffset, z + d11).tex(0.0D, d15).color(r, g, b, 1.0F*alphaMult).endVertex();
        worldrenderer.pos(x + d8, y + topOffset, z + d9).tex(1.0D, d15).color(r, g, b, 1.0F*alphaMult).endVertex();
        worldrenderer.pos(x + d8, y + bottomOffset, z + d9).tex(1.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d4, y + bottomOffset, z + d5).tex(0.0D, d14).color(r, g, b, 1.0F).endVertex();
        worldrenderer.pos(x + d4, y + topOffset, z + d5).tex(0.0D, d15).color(r, g, b, 1.0F*alphaMult).endVertex();
        tessellator.draw();

        GlStateManager.disableCull();
        double d12 = -1.0D + d1;
        double d13 = height + d12;

        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldrenderer.pos(x + 0.2D, y + topOffset, z + 0.2D).tex(1.0D, d13).color(r, g, b, 0.25F*alphaMult).endVertex();
        worldrenderer.pos(x + 0.2D, y + bottomOffset, z + 0.2D).tex(1.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.8D, y + bottomOffset, z + 0.2D).tex(0.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.8D, y + topOffset, z + 0.2D).tex(0.0D, d13).color(r, g, b, 0.25F*alphaMult).endVertex();
        worldrenderer.pos(x + 0.8D, y + topOffset, z + 0.8D).tex(1.0D, d13).color(r, g, b, 0.25F*alphaMult).endVertex();
        worldrenderer.pos(x + 0.8D, y + bottomOffset, z + 0.8D).tex(1.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.2D, y + bottomOffset, z + 0.8D).tex(0.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.2D, y + topOffset, z + 0.8D).tex(0.0D, d13).color(r, g, b, 0.25F*alphaMult).endVertex();
        worldrenderer.pos(x + 0.8D, y + topOffset, z + 0.2D).tex(1.0D, d13).color(r, g, b, 0.25F*alphaMult).endVertex();
        worldrenderer.pos(x + 0.8D, y + bottomOffset, z + 0.2D).tex(1.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.8D, y + bottomOffset, z + 0.8D).tex(0.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.8D, y + topOffset, z + 0.8D).tex(0.0D, d13).color(r, g, b, 0.25F*alphaMult).endVertex();
        worldrenderer.pos(x + 0.2D, y + topOffset, z + 0.8D).tex(1.0D, d13).color(r, g, b, 0.25F*alphaMult).endVertex();
        worldrenderer.pos(x + 0.2D, y + bottomOffset, z + 0.8D).tex(1.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.2D, y + bottomOffset, z + 0.2D).tex(0.0D, d12).color(r, g, b, 0.25F).endVertex();
        worldrenderer.pos(x + 0.2D, y + topOffset, z + 0.2D).tex(0.0D, d13).color(r, g, b, 0.25F*alphaMult).endVertex();
        tessellator.draw();
    }

    public void onRenderLast(RenderWorldLastEvent event,
                             WaypointBeaconData waypointData) {
        if(!waypointData.enabled) return;

        String location = SBInfo.getInstance().getLocation();
        if(location == null) return;
        if(waypointData.currentWaypointList == null || waypointData.currentWaypointList.isEmpty()) return;

        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * event.partialTicks;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * event.partialTicks;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * event.partialTicks;

        Vector3f aoteInterpPos = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(aoteInterpPos != null) {
            viewerX = aoteInterpPos.x;
            viewerY = aoteInterpPos.y;
            viewerZ = aoteInterpPos.z;
        }

        Set<Integer> found = waypointData.foundWaypoints.computeIfAbsent(location, k -> new HashSet<>());

        int rgb = 0xa839ce;
        for(int i = 0; i< waypointData.currentWaypointListClose.size(); i++) {
            BlockPos currentSoul = waypointData.currentWaypointListClose.get(i);
            double x = currentSoul.getX() - viewerX;
            double y = currentSoul.getY() - viewerY;
            double z = currentSoul.getZ() - viewerZ;

            double distSq = x*x + y*y + z*z;

            AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x+1, y+1, z+1);

            GlStateManager.disableDepth();
            GlStateManager.disableCull();
            GlStateManager.disableTexture2D();
            CustomItemEffects.drawFilledBoundingBox(bb, 1f, SpecialColour.special(0, 100, rgb));

            if(distSq > 10*10) {
                renderBeaconBeam(x, y, z, rgb, 1.0f, event.partialTicks);
            }
        }

        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
    }

    public static class WaypointBeaconCommand extends SimpleCommand {

        public WaypointBeaconCommand(String commandName, WaypointBeaconData waypointData) {
            super(commandName, new ProcessCommandRunnable() {
                @Override
                public void processCommand(ICommandSender sender, String[] args) {
                    if(args.length != 1) {
                        printHelp(commandName, waypointData);
                        return;
                    }
                    String subcommand = args[0].toLowerCase();

                    switch (subcommand) {
                        case "help":
                            printHelp(commandName, waypointData);
                            return;
                        case "on":
                        case "enable":
                            print(EnumChatFormatting.DARK_PURPLE+
                                    "Enabled " + waypointData.singularName + " waypoints");
                            waypointData.enabled = true;
                            return;
                        case "off":
                        case "disable":
                            print(EnumChatFormatting.DARK_PURPLE+
                                    "Disabled " + waypointData.singularName + " waypoints");
                            waypointData.enabled = false;
                            return;
                        case "clear": {
                            String location = SBInfo.getInstance().getLocation();
                            if(waypointData.currentWaypointList == null || location == null) {
                                print(EnumChatFormatting.RED+
                                        "No " + waypointData.pluralName + " found in your current world");
                            } else {
                                Set<Integer> found =
                                        waypointData.foundWaypoints.computeIfAbsent(location, k -> new HashSet<>());
                                for(int i = 0; i< waypointData.currentWaypointList.size(); i++) {
                                    found.add(i);
                                }
                                print(EnumChatFormatting.DARK_PURPLE+
                                        "Marked all " + waypointData.pluralName + " as found");
                            }
                        }
                        return;
                        case "unclear":
                            String location = SBInfo.getInstance().getLocation();
                            if(location == null) {
                                print(EnumChatFormatting.RED+
                                        "No " + waypointData.pluralName + " found in your current world");
                            } else {
                                print(EnumChatFormatting.DARK_PURPLE+
                                        "Marked all " + waypointData.pluralName + " as not found");
                                waypointData.foundWaypoints.remove(location);
                            }
                            return;
                        case "markfound":
                            if (!WaypointBeacons.markClosestRelicFound(waypointData)) {
                                print(EnumChatFormatting.RED+
                                        "A " + waypointData.singularName + " was not found nearby");
                            } else {
                                print(EnumChatFormatting.DARK_PURPLE+
                                        "Marked " + waypointData.singularName + " as found");
                            }
                            return;
                    }

                    print(EnumChatFormatting.RED+"Unknown subcommand: " + subcommand);
                }
            });
        }

        private static void print(String s) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(s));
        }

        private static void printHelp(String commandName, WaypointBeaconData waypointData) {
            print("");
            print(EnumChatFormatting.DARK_PURPLE.toString()+EnumChatFormatting.BOLD+
                    "     NEU " + waypointData.singularName + " Waypoint Guide");
            print(EnumChatFormatting.LIGHT_PURPLE+
                    "Shows waypoints for every " + waypointData.singularName + " in your world");
            print(EnumChatFormatting.LIGHT_PURPLE+
                    "Clicking a " + waypointData.singularName + " automatically removes it from the list");
            print(EnumChatFormatting.GOLD.toString()+EnumChatFormatting.BOLD+"     Commands:");
            print(EnumChatFormatting.YELLOW +"/" + commandName +
                    " help          - Display this message");
            print(EnumChatFormatting.YELLOW +"/" + commandName +
                    " on/off        - Enable/disable the waypoint markers");
            print(EnumChatFormatting.YELLOW +"/" + commandName +
                    " clear/unclear - Marks every waypoint in your current world as completed/uncompleted");
            print(EnumChatFormatting.YELLOW +"/" + commandName +
                    " markfound - Mark the closest " + waypointData.singularName + " within 10 blocks as found");
            print("");
        }

    }

}
