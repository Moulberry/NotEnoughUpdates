package io.github.moulberry.notenoughupdates;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.EnumChatFormatting;

public class CustomItems {

    /**
     * So it has come to this, huh? Snooping through the source to find all my carefully crafted easter eggs. You
     * cheated not only the game, but yourself. You didn't grow. You didn't improve. You took a
     * shortcut and gained nothing. You experienced a hollow victory. Nothing was risked and nothing was gained.
     * It's sad that you don't know the difference.
     */

    public static JsonObject NULLZEE = create(
            "NULLZEE",
            "dirt",
            "Nullzee242 Youtube Channel",
            "Dirt, AOTD. Dirt, AOTD.",
            "Dirt, AOTD. Dirt, AOTD.",
            "Ooh, Dirt to Midas! Let's shake it up a little.",
            "",
            "Also, did you know that only 8.7% of the people watching are subscribed?",
            "It's OK, everyone makes mistakes",
            "Also follow -> twitch.tv/nullzeelive",
            "Also -> discord.gg/nullzee");
    public static JsonObject DUCTTAPE = create(
            "DUCTTAPE",
            "iron_shovel",
            "You ever accidentally bury your duct tape?",
        "No problem! Our team of experts specialise in",
            "subterranean duct tape excavation. That's right:",
            "your buried duct tape problems are a thing of the past,",
            "all for the low price of $7.99 or a subscription",
            "to the Ducttapedigger youtube channel!");
    public static JsonObject SPINAXX = create(
            "SPINAXX",
            "emerald",
            "Spinaxx",
            "Famous streamer btw :)");
    public static JsonObject RUNE = create("RUNE", "paper", "No.", "I hate runes.");
    public static JsonObject TWOBEETWOTEE = create("2B2T", "bedrock", "Minecraft's oldest anarchy Minecraft server in Minecraft.",
            "This Minecraft anarchy server is the oldest server,",
                "being a server since 2010 when Minecraft was a game with a server.",
                "It is complete anarchy in Minecraft which means that there is total anarchy.",
                "Hacking is allowed in Minecraft on this anarchy server which",
                "is the oldest anarchy server in Minecraft, 2b2t. Hack. Steal. Cheat. Lie.",
                "On the oldest anarchy server in Minecraft. 2b2t. The worst server in Minecraft,",
                "where there are no rules. On the oldest anarchy server in Minecraft.",
                "In this Minecraft anarchy server, there have been numerous Minecraft",
                "incursions on the server, some of which I, a player on this Minecraft",
                "anarchy server in Minecraft, have participated in. One of this server's",
                "most infamous Minecraft players on the oldest Minecraft");
    public static JsonObject LEOCTHL = create("LEOCTHL", "dragon_egg", "--- Stats below may not be entirely accurate ---",
            "17 legendary dragon pets",
                  "24 epic dragon pets",
                  "18 epic golem pets",
                  "12 legendary golem pets",
                  "39 legendary phoenix pets",
                  "",
                  "get flexed");

    /**
     * SHAAAAAAAAAAAAAAAAAAME
     */

    private static JsonObject create(String internalname, String itemid, String displayname, String... lore) {
        JsonObject json = new JsonObject();
        json.addProperty("itemid", itemid);
        json.addProperty("internalname", internalname);
        json.addProperty("displayname", EnumChatFormatting.RED+displayname);
        JsonArray jsonlore = new JsonArray();
        for(String line : lore) {
            jsonlore.add(new JsonPrimitive(EnumChatFormatting.GRAY + line));
        }
        json.add("lore", jsonlore);
        return json;
    }
}
