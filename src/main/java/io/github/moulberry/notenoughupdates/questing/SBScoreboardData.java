package io.github.moulberry.notenoughupdates.questing;

import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SBScoreboardData {

    private static final SBScoreboardData INSTANCE = new SBScoreboardData();

    private static final Pattern locationPattern = Pattern.compile("(\\u00a7)(?!.*\\u00a7).+");
    private static final Pattern timePattern = Pattern.compile(".+(am|pm)");

    public String location = "";
    public String date = "";
    public String time = "";
    public String objective = "";

    public Date currentTimeDate = null;

    public static SBScoreboardData getInstance() {
        return INSTANCE;
    }

    public void tick() {
        try {
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
                    try {
                        String timeSpace = time.replace("am", " am").replace("pm", " pm");
                        SimpleDateFormat parseFormat = new SimpleDateFormat("hh:mm a");
                        currentTimeDate = parseFormat.parse(timeSpace);
                    } catch (ParseException e) {}
                }
                matcher = locationPattern.matcher(lines.get(4));
                if(matcher.find()) {
                    location = Utils.cleanColour(matcher.group()).trim();
                }
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
        //System.out.println(date + ":" + time + ":" + location);
    }

}
