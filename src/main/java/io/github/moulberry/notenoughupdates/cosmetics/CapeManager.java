package io.github.moulberry.notenoughupdates.cosmetics;

import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CapeManager {

    public static final CapeManager INSTANCE = new CapeManager();

    private HashMap<String, Pair<NEUCape, String>> capeMap = new HashMap<>();
    private String[] capes = new String[]{"patreon1", "patreon2", "gravy", "fade", "contrib"};

    public static CapeManager getInstance() {
        return INSTANCE;
    }

    public void setCape(String player, String capename) {
        if(capename == null) {
            NotEnoughUpdates.INSTANCE.manager.config.selectedCape.value = "";
            capeMap.remove(player);
            return;
        }
        if(player.equalsIgnoreCase(Minecraft.getMinecraft().thePlayer.getName())) {
            NotEnoughUpdates.INSTANCE.manager.config.selectedCape.value = capename;
        }
        if(capeMap.containsKey(player)) {
            Pair<NEUCape, String> capePair = capeMap.get(player);
            capePair.setValue(capename);
        } else {
            capeMap.put(player, new MutablePair<>(new NEUCape(capename), capename));
        }
    }

    public String getCape(String player) {
        if(capeMap.containsKey(player)) {
            return capeMap.get(player).getRight();
        }
        return null;
    }

    public EntityPlayer getPlayerForName(String name) {
        if(Minecraft.getMinecraft().theWorld != null) {
            for(EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
                if(player.getName().equals(name)) {
                    return player;
                }
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Post e) {
        //if(e.partialRenderTick == 1.0F) return; //rendering in inventory
        if(Minecraft.getMinecraft().thePlayer != null &&
                e.entityPlayer.getName().equals(Minecraft.getMinecraft().thePlayer.getName())) {
            if(NotEnoughUpdates.INSTANCE.manager.config.selectedCape.value != null &&
                    !NotEnoughUpdates.INSTANCE.manager.config.selectedCape.value.isEmpty()) {
                setCape(Minecraft.getMinecraft().thePlayer.getName(),
                        NotEnoughUpdates.INSTANCE.manager.config.selectedCape.value);
            }
        }
        if(e.entityPlayer.getName().equals("Moulberry")) setCape(e.entityPlayer.getName(), "fade");
        if(capeMap.containsKey(e.entityPlayer.getName())) {
            capeMap.get(e.entityPlayer.getName()).getLeft().onRenderPlayer(e);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Set<String> toRemove = new HashSet<>();
        for(String playerName : capeMap.keySet()) {
            EntityPlayer player = getPlayerForName(playerName);
            if(player == null) {
                toRemove.add(playerName);
            } else {
                String capeName = capeMap.get(playerName).getRight();
                if(capeName != null) {
                    capeMap.get(playerName).getLeft().setCapeTexture(capeName);
                    capeMap.get(playerName).getLeft().onTick(event, player);
                } else {
                    toRemove.add(playerName);
                }
            }
        }
        for(String playerName : toRemove) {
            capeMap.remove(playerName);
        }
    }

    public String[] getCapes() {
        return capes;
    }

    private String[] contributors = new String[]{"thatgravyboat", "twasnt", "traxyrr", "some1sm", "meguminqt", "marethyu_77"};

    public boolean getPermissionForCape(String player, String capename) {
        if(capename == null) {
            return false;
        } else if(player.equalsIgnoreCase("Moulberry")) {
            return true; //Oh yeah gimme gimme
        } else {
            switch(capename) {
                case "nullzee": return player.equalsIgnoreCase("Nullzee");
                case "gravy": return player.equalsIgnoreCase("ThatGravyBoat");
                case "contrib": return ArrayUtils.contains(contributors, player.toLowerCase());
                case "fade": return true;
            }
        }
        return false;
    }

}
