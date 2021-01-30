package io.github.moulberry.notenoughupdates.miscfeatures;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.text.NumberFormat;
import java.util.HashMap;

public class DamageCommas {

    private static final HashMap<Integer, ChatComponentText> replacementMap = new HashMap<>();

    public static void tick() {
        replacementMap.clear();
    }

    public static IChatComponent replaceName(IChatComponent name) {
        if(!NotEnoughUpdates.INSTANCE.config.misc.damageCommas) return name;

        String formatted = name.getFormattedText();
        int hashCode = formatted.hashCode();

        if(replacementMap.containsKey(hashCode)) {
            ChatComponentText component = replacementMap.get(hashCode);
            if(component == null) return name;
            return component;
        }

        if(formatted.length() >= 7 && formatted.startsWith("\u00A7f\u2727") &&
                formatted.endsWith("\u2727\u00a7r")) {

            StringBuilder builder = new StringBuilder();
            boolean numLast = false;
            boolean colLast = false;
            boolean colLastLast;
            int numCount = 0;
            for(int i=formatted.length()-4; i>=3; i--) {
                char c = formatted.charAt(i);
                colLastLast = colLast;

                if(c == '\u00a7') {
                    if(numLast) numCount--;
                    numLast = false;
                    colLast = true;
                } else if(c >= '0' && c <= '9') {
                    numLast = true;
                    colLast = false;
                    numCount++;
                } else {
                    if(colLast) {
                        replacementMap.put(hashCode, null);
                        return name;
                    }
                    numLast = false;
                }

                if(colLastLast && numLast && numCount > 1 && (numCount-1) % 3 == 0) builder.append(',');
                builder.append(c);
            }

            ChatComponentText ret = new ChatComponentText("\u00A7f\u2727"+builder.reverse().toString()+"\u2727\u00a7r");
            replacementMap.put(hashCode, ret);
            return ret;
        }

        if(formatted.length() >= 5 && formatted.startsWith(EnumChatFormatting.GRAY.toString()) &&
                formatted.endsWith(EnumChatFormatting.RESET.toString())) {
            String damageS = formatted.substring(2, formatted.length()-2);

            for(int i=0; i<damageS.length(); i++) {
                char c = damageS.charAt(i);
                if(c < '0' || c > '9') {
                    replacementMap.put(hashCode, null);
                    return name;
                }
            }

            try {
                int damage = Integer.parseInt(damageS);

                String damageFormatted = NumberFormat.getIntegerInstance().format(damage);

                ChatComponentText ret = new ChatComponentText(EnumChatFormatting.GRAY+damageFormatted+EnumChatFormatting.RESET);
                replacementMap.put(hashCode, ret);
                return ret;
            } catch(Exception e) {
                replacementMap.put(hashCode, null);
                return name;
            }
        }
        replacementMap.put(hashCode, null);
        return name;
    }

}
