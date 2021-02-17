package io.github.moulberry.notenoughupdates.miscfeatures;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.text.NumberFormat;
import java.util.HashMap;

public class DamageCommas {

    private static final HashMap<Integer, ChatComponentText> replacementMap = new HashMap<>();

    private static final EnumChatFormatting[] colours = {EnumChatFormatting.RED, EnumChatFormatting.GOLD, EnumChatFormatting.YELLOW, EnumChatFormatting.WHITE};

    public static void tick() {
        replacementMap.clear();
    }

    public static IChatComponent replaceName(IChatComponent name) {
        if(NotEnoughUpdates.INSTANCE.config.misc.damageIndicatorStyle == 0) return name;

        String formatted = name.getFormattedText();
        int hashCode = formatted.hashCode();

        if(replacementMap.containsKey(hashCode)) {
            ChatComponentText component = replacementMap.get(hashCode);
            if(component == null) return name;
            return component;
        }

        if(formatted.length() >= 7 && formatted.startsWith("\u00A7f\u2727") &&
                formatted.endsWith("\u2727\u00a7r")) {

            if(NotEnoughUpdates.INSTANCE.config.misc.damageIndicatorStyle == 2) {
                String numbers = Utils.cleanColour(formatted.substring(3, formatted.length()-3)).trim().replaceAll("[^0-9]", "");
                try {
                    int damage = Integer.parseInt(numbers);

                    String damageString;
                    if(damage > 999) {
                        damageString = Utils.shortNumberFormat(damage, 0);
                    } else {
                        damageString = NumberFormat.getIntegerInstance().format(damage);
                    }

                    StringBuilder colouredString = new StringBuilder();
                    int colourIndex = 0;
                    for(int i=0; i<damageString.length(); i++) {
                        int index = damageString.length() - 1 - i;
                        char c = damageString.charAt(index);
                        if(c >= '0' && c <= '9') {
                            colouredString.insert(0, c);
                            colouredString.insert(0, colours[colourIndex++ % colours.length]);
                        } else {
                            colouredString.insert(0, c);
                        }
                    }

                    ChatComponentText ret = new ChatComponentText("\u00A7f\u2727"+colouredString+"\u00a7r\u2727\u00a7r");
                    replacementMap.put(hashCode, ret);
                    return ret;
                } catch(NumberFormatException ignored) {}
            }

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

                String damageFormatted;
                if(NotEnoughUpdates.INSTANCE.config.misc.damageIndicatorStyle == 2 && damage > 999) {
                    damageFormatted = Utils.shortNumberFormat(damage, 0);
                } else {
                    damageFormatted = NumberFormat.getIntegerInstance().format(damage);
                }

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
