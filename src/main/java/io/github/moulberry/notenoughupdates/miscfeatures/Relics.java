package io.github.moulberry.notenoughupdates.miscfeatures;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.util.Constants;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Relics extends WaypointBeacons {

    private static String foundWaypointsFileName = "collected_relics.json";

    private static WaypointBeaconData waypointData = new WaypointBeaconData("relic", "relics");

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        waypointData.currentWaypointList = null;
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event){
        if(waypointData.currentWaypointList == null) return;

        // Sample relic message when found for the first time:
        //   +10,000 Coins! (7/28 Relics)
        // Nothing is printed if the relic has already been found
        if(event.message.getFormattedText().contains(" Relics)")) {
            markClosestRelicFound(waypointData);
        }
    }

    public static void load(File neuDir, Gson gson) {
        WaypointBeacons.loadFoundWaypoints(new File(neuDir, foundWaypointsFileName), gson, waypointData);
    }

    public static void save(File neuDir, Gson gson) {
        WaypointBeacons.saveFoundWaypoints(new File(neuDir, foundWaypointsFileName), gson, waypointData);
    }

    public static void tick() {
        if(Minecraft.getMinecraft().theWorld == null) {
            waypointData.currentWaypointList = null;
            return;
        }

        WaypointBeacons.tick(Constants.RELICS, waypointData);
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        super.onRenderLast(event, waypointData);
    }

    public static class RelicsCommand extends WaypointBeaconCommand {

        public RelicsCommand() {
            super("neurelics", waypointData);
        }
    }
}
