package io.github.moulberry.notenoughupdates.questing;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NEUQuesting {

    private static final NEUQuesting INSTANCE = new NEUQuesting();

    private static final Pattern locationPattern = Pattern.compile("(\\u00a7)(?!.*\\u00a7).+");
    private static final Pattern timePattern = Pattern.compile(".+(am|pm)");

    public String location = "";
    public String date = "";
    public String time = "";

    public static NEUQuesting getInstance() {
        return INSTANCE;
    }

    public void tick() {
        Scoreboard scoreboard = Minecraft.getMinecraft().thePlayer.getWorldScoreboard();

        ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1); //§707/14/20

        List<Score> scores = new ArrayList<>();
        for(Score score : scoreboard.getSortedScores(sidebarObjective)) {
            scores.add(score);
        }
        List<String> lines = new ArrayList<>();
        for(int i=scores.size()-1; i>=0; i--) {
            Score score = scores.get(i);
            ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score.getPlayerName());
            line = Utils.cleanDuplicateColourCodes(line);
            lines.add(line);
        }
        if(lines.size() >= 5) {
            date = Utils.cleanColour(lines.get(2)).trim();
            //§74:40am
            Matcher matcher = timePattern.matcher(lines.get(3));
            if(matcher.find()) {
                time = Utils.cleanColour(matcher.group()).trim();
            }
            matcher = locationPattern.matcher(lines.get(4));
            if(matcher.find()) {
                location = Utils.cleanColour(matcher.group()).trim();
            }
        }
        //System.out.println(date + ":" + time + ":" + location);
    }

}
