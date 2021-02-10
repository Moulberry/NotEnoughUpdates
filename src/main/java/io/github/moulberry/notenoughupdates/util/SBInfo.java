package io.github.moulberry.notenoughupdates.util;

import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.miscfeatures.EnchantingSolvers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SBInfo {

    private static final SBInfo INSTANCE = new SBInfo();

    private static final Pattern timePattern = Pattern.compile(".+(am|pm)");

    public String location = "";
    public String date = "";
    public String time = "";
    public String objective = "";

    public String mode = "";

    public Date currentTimeDate = null;

    public String lastOpenContainerName = null;

    public static SBInfo getInstance() {
        return INSTANCE;
    }

    private long lastManualLocRaw = -1;
    private long lastLocRaw = -1;
    private long joinedWorld = -1;
    private JsonObject locraw = null;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return;

        if(event.gui instanceof GuiChest) {
            GuiChest chest = (GuiChest) event.gui;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            String containerName = container.getLowerChestInventory().getDisplayName().getUnformattedText();

            lastOpenContainerName = containerName;
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Load event) {
        lastLocRaw = -1;
        locraw = null;
        mode = null;
        joinedWorld = System.currentTimeMillis();
        lastOpenContainerName = null;
    }

    private static final Pattern JSON_BRACKET_PATTERN = Pattern.compile("\\{.+}");

    public void onSendChatMessage(String msg) {
        if(msg.trim().startsWith("/locraw") || msg.trim().startsWith("/locraw ")) {
            lastManualLocRaw = System.currentTimeMillis();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    public void onChatMessage(ClientChatReceivedEvent event) {
        Matcher matcher = JSON_BRACKET_PATTERN.matcher(event.message.getUnformattedText());
        if(matcher.find()) {
            try {
                JsonObject obj = NotEnoughUpdates.INSTANCE.manager.gson.fromJson(matcher.group(), JsonObject.class);
                if(obj.has("server")) {
                    if(System.currentTimeMillis() - lastManualLocRaw > 5000) event.setCanceled(true);
                    if(obj.has("gametype") && obj.has("mode") && obj.has("map")) {
                        locraw = obj;
                        mode = locraw.get("mode").getAsString();
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getLocation() {
        if(mode == null) {
            return null;
        }
        return mode;
    }

    public void tick() {
        long currentTime = System.currentTimeMillis();

        if(Minecraft.getMinecraft().thePlayer != null &&
                Minecraft.getMinecraft().theWorld != null &&
                locraw == null &&
                (currentTime - joinedWorld) > 1000 &&
                (currentTime - lastLocRaw) > 15000) {
            lastLocRaw = System.currentTimeMillis();
            NotEnoughUpdates.INSTANCE.sendChatMessage("/locraw");
        }

        try {
            Scoreboard scoreboard = Minecraft.getMinecraft().thePlayer.getWorldScoreboard();

            ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1); //§707/14/20

            List<Score> scores = new ArrayList<>(scoreboard.getSortedScores(sidebarObjective));

            List<String> lines = new ArrayList<>();
            for(int i=scores.size()-1; i>=0; i--) {
                Score score = scores.get(i);
                ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score.getPlayerName());
                String line = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score.getPlayerName());
                line = Utils.cleanDuplicateColourCodes(line);
                lines.add(line);
            }
            if(lines.size() >= 5) {
                date = Utils.cleanColour(lines.get(1)).trim();
                //§74:40am
                Matcher matcher = timePattern.matcher(lines.get(2));
                if(matcher.find()) {
                    time = Utils.cleanColour(matcher.group()).trim();
                    try {
                        String timeSpace = time.replace("am", " am").replace("pm", " pm");
                        SimpleDateFormat parseFormat = new SimpleDateFormat("hh:mm a");
                        currentTimeDate = parseFormat.parse(timeSpace);
                    } catch (ParseException e) {}
                }
                location = Utils.cleanColour(lines.get(3)).replaceAll("[^A-Za-z0-9() ]", "").trim();
            }
            objective = null;

            boolean objTextLast = false;
            for(String line : lines) {
                if(objTextLast) {
                    objective = line;
                }

                objTextLast = line.equals("Objective");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
