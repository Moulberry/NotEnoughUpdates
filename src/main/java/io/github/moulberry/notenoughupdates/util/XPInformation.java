package io.github.moulberry.notenoughupdates.util;

import com.google.common.base.Splitter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.core.util.StringUtils;
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewer;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XPInformation {

    private static XPInformation INSTANCE = new XPInformation();

    public static XPInformation getInstance() {
        return INSTANCE;
    }

    public static class SkillInfo {
        public int level;
        public float totalXp;
        public float currentXp;
        public float currentXpMax;
    }

    private HashMap<String, SkillInfo> skillInfoMap = new HashMap<>();

    private static Splitter SPACE_SPLITTER = Splitter.on("  ").omitEmptyStrings().trimResults();
    private static Pattern SKILL_PATTERN = Pattern.compile("\\+(\\d+(?:,\\d+)*(?:\\.\\d+)?) (.+) \\((\\d+(?:,\\d+)*(?:\\.\\d+)?)/(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\)");

    public HashMap<String, SkillInfo> getSkillInfoMap() {
        return skillInfoMap;
    }

    public SkillInfo getSkillInfo(String skillName) {
        return skillInfoMap.get(skillName.toLowerCase());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChatReceived(ClientChatReceivedEvent event) {
        if(event.type == 2) {
            JsonObject leveling = Constants.LEVELING;
            if(leveling == null)  return;

            List<String> components = SPACE_SPLITTER.splitToList(StringUtils.cleanColour(event.message.getUnformattedText()));

            for(String component : components) {
                Matcher matcher = SKILL_PATTERN.matcher(component);
                if(matcher.matches()) {
                    String skillS = matcher.group(2);
                    String currentXpS = matcher.group(3).replace(",","");
                    String maxXpS = matcher.group(4).replace(",","");;

                    float currentXp = Float.parseFloat(currentXpS);
                    float maxXp = Float.parseFloat(maxXpS);

                    SkillInfo skillInfo = new SkillInfo();
                    skillInfo.currentXp = currentXp;
                    skillInfo.currentXpMax = maxXp;
                    skillInfo.totalXp = currentXp;

                    JsonArray levelingArray = leveling.getAsJsonArray("leveling_xp");
                    for(int i=0; i<levelingArray.size(); i++) {
                        float cap = levelingArray.get(i).getAsFloat();
                        if(maxXp <= cap) {
                            break;
                        }

                        skillInfo.totalXp += cap;
                        skillInfo.level++;
                    }

                    skillInfoMap.put(skillS.toLowerCase(), skillInfo);
                }
            }
        }
    }

    public void tick() {
        ProfileApiSyncer.getInstance().requestResync("xpinformation", 5*60*1000,
                () -> {}, this::onApiUpdated);
    }

    private static final String[] skills = {"taming","mining","foraging","enchanting","carpentry","farming","combat","fishing","alchemy","runecrafting"};

    private void onApiUpdated(ProfileViewer.Profile profile) {
        JsonObject skillInfo = profile.getSkillInfo(null);

        for(String skill : skills) {
            SkillInfo info = new SkillInfo();

            float level = skillInfo.get("level_skill_"+skill).getAsFloat();

            info.totalXp = skillInfo.get("experience_skill_"+skill).getAsFloat();
            info.currentXpMax = skillInfo.get("maxxp_skill_"+skill).getAsFloat();
            info.level = (int)level;
            info.currentXp = (level%1)*info.currentXpMax;

            skillInfoMap.put(skill.toLowerCase(), info);
        }
    }

}
