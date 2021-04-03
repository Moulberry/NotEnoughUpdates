package io.github.moulberry.notenoughupdates.miscfeatures;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.util.Constants;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

public class FairySouls extends WaypointBeacons {

    private static String foundWaypointsFileName = "collected_fairy_souls.json";
    private static WaypointBeaconData waypointData =
            new WaypointBeaconData("fairy soul",
                    "fairy souls",
                    "NEU Fairy Soul Waypoint Guide");

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        waypointData.currentWaypointList = null;
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event){
        if(waypointData.currentWaypointList == null) return;

        if(event.message.getFormattedText().equals("\u00A7r\u00A7dYou have already found that Fairy Soul!\u00A7r") ||
                event.message.getFormattedText().equals("\u00A7d\u00A7lSOUL! \u00A7fYou found a \u00A7r\u00A7dFairy Soul\u00A7r\u00A7f!\u00A7r")) {
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

        WaypointBeacons.tick(Constants.FAIRYSOULS, waypointData);
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        super.onRenderLast(event, waypointData);
    }

    public static class FairySoulsCommand extends WaypointBeaconCommand {

        public FairySoulsCommand() {
            super("neusouls", waypointData);
        }
    }
}
