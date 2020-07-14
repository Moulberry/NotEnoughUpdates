package io.github.moulberry.notenoughupdates.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CapeManager {

    public static final CapeManager INSTANCE = new CapeManager();

    private HashMap<String, Pair<NEUCape, String>> capeMap = new HashMap<>();
    private String[] capes = new String[]{"testcape", "nullzee", "gravy", "fade", "contrib"};

    public static CapeManager getInstance() {
        return INSTANCE;
    }

    public void setCape(String player, String capename) {
        if(capename == null) {
            capeMap.remove(player);
            return;
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

    public boolean getPermissionForCape(String player, String capename) {
        if(capename == null) {
            return false;
        } else if(player.equalsIgnoreCase("Moulberry")) {
            return true; //Oh yeah gimme gimme
        } else if(capename.equals("nullzee")) {
            return player.equalsIgnoreCase("Nullzee");
        } else if(capename.equals("gravy")) {
            return player.equalsIgnoreCase("ThatGravyBoat");
        }
        return false;
    }

}
