package io.github.moulberry.notenoughupdates.cosmetics;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.HypixelApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CapeManager {

    public static final CapeManager INSTANCE = new CapeManager();
    public long lastCapeUpdate = 0;
    public long lastCapeSynced = 0;

    public Pair<NEUCape, String> localCape = null;
    private HashMap<String, Pair<NEUCape, String>> capeMap = new HashMap<>();

    private int permSyncTries = 5;
    private boolean allAvailable = false;
    private HashSet<String> availableCapes = new HashSet<>();

    public JsonObject lastJsonSync = null;

    private String[] capes = new String[]{"patreon1", "patreon2", "fade", "contrib", "nullzee",
            "gravy", "space", "mcworld", "lava", "packshq", "mbstaff", "thebakery", "negative", "void", "ironmoon", "krusty", "furf" };
    public Boolean[] specialCapes = new Boolean[]{ true, true, false, true, true, true, false, false, false, true, true, true, false, false, true, false, true };

    public static CapeManager getInstance() {
        return INSTANCE;
    }

    public void tick() {
        long currentTime = System.currentTimeMillis();
        if(currentTime - lastCapeUpdate > 60*1000) {
            lastCapeUpdate = currentTime;
            updateCapes();
        }
    }

    private void updateCapes() {
        NotEnoughUpdates.INSTANCE.manager.hypixelApi.getMyApiAsync("activecapes.json", (jsonObject) -> {
            if(jsonObject.get("success").getAsBoolean()) {
                lastJsonSync = jsonObject;

                lastCapeSynced = System.currentTimeMillis();
                capeMap.clear();
                for(JsonElement active : jsonObject.get("active").getAsJsonArray()) {
                    if(active.isJsonObject()) {
                        JsonObject activeObj = (JsonObject) active;
                        setCape(activeObj.get("_id").getAsString(), activeObj.get("capeType").getAsString(), false);
                    }
                }
            }
        }, () -> {
            System.out.println("[MBAPI] Update capes errored");
        });

        if(Minecraft.getMinecraft().thePlayer != null && permSyncTries > 0) {
            String uuid = Minecraft.getMinecraft().thePlayer.getUniqueID().toString().replace("-", "");
            permSyncTries--;
            NotEnoughUpdates.INSTANCE.manager.hypixelApi.getMyApiAsync("permscapes.json", (jsonObject) -> {
                if(jsonObject.get("success").getAsBoolean()) {
                    permSyncTries = 0;

                    availableCapes.clear();
                    for(JsonElement permPlayer : jsonObject.get("perms").getAsJsonArray()) {
                        if(permPlayer.isJsonObject()) {
                            String playerUuid = permPlayer.getAsJsonObject().get("_id").getAsString();
                            if(playerUuid != null && playerUuid.equals(uuid)) {
                                for(JsonElement perm : permPlayer.getAsJsonObject().get("perms").getAsJsonArray()) {
                                    if(perm.isJsonPrimitive()) {
                                        String cape = perm.getAsString();
                                        if(cape.equals("*")) {
                                            allAvailable = true;
                                        } else {
                                            availableCapes.add(cape);
                                        }
                                    }
                                }
                                return;
                            }
                        }
                    }
                }
            }, () -> {
                System.out.println("[MBAPI] Update capes errored - perms");
            });
        }
    }

    public HashSet<String> getAvailableCapes() {
        return allAvailable ? null : availableCapes;
    }

    public void setCape(String playerUUID, String capename, boolean updateConfig) {
        boolean none = capename == null || capename.equals("null");

        updateConfig = updateConfig && playerUUID.equals(Minecraft.getMinecraft().thePlayer.getUniqueID().toString().replace("-", ""));
        if(updateConfig) {
            NotEnoughUpdates.INSTANCE.config.hidden.selectedCape = String.valueOf(capename);
        }

        if(updateConfig) {
            if(none) {
                localCape = null;
            } else {
                localCape = new MutablePair<>(new NEUCape(capename), capename);
            }
        } else if(capeMap.containsKey(playerUUID)) {
            if(none) {
                capeMap.remove(playerUUID);
            } else {
                Pair<NEUCape, String> capePair = capeMap.get(playerUUID);
                capePair.setValue(capename);
            }
        } else if(!none) {
            capeMap.put(playerUUID, new MutablePair<>(new NEUCape(capename), capename));
        }
    }

    public String getCape(String player) {
        if(capeMap.containsKey(player)) {
            return capeMap.get(player).getRight();
        }
        return null;
    }

    private static BiMap<String, EntityPlayer> playerMap = null;

    public EntityPlayer getPlayerForUUID(String uuid) {
        if(playerMap == null) {
            return null;
        }
        if(playerMap.containsKey(uuid)) {
            return playerMap.get(uuid);
        }
        return null;
    }

    private static Framebuffer checkFramebufferSizes(Framebuffer framebuffer, int width, int height) {
        if(framebuffer == null || framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
            if(framebuffer == null) {
                framebuffer = new Framebuffer(width, height, true);
            } else {
                framebuffer.createBindFramebuffer(width, height);
            }
            framebuffer.setFramebufferFilter(GL11.GL_NEAREST);
        }
        return framebuffer;
    }

    public boolean updateWorldFramebuffer = false;
    public Framebuffer backgroundFramebuffer = null;

    public void postRenderBlocks() {
        int width = Minecraft.getMinecraft().displayWidth;
        int height = Minecraft.getMinecraft().displayHeight;
        backgroundFramebuffer = checkFramebufferSizes(backgroundFramebuffer,
                width, height);

        if(OpenGlHelper.isFramebufferEnabled() && updateWorldFramebuffer) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, Minecraft.getMinecraft().getFramebuffer().framebufferObject);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, backgroundFramebuffer.framebufferObject);
            GL30.glBlitFramebuffer(0, 0, width, height,
                    0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
        }

        updateWorldFramebuffer = false;
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Post e) {
        if(e.partialRenderTick == 1.0F) return; //rendering in inventory

        String uuid = e.entityPlayer.getUniqueID().toString().replace("-", "");
        String clientUuid = Minecraft.getMinecraft().thePlayer.getUniqueID().toString().replace("-", "");

        if(Minecraft.getMinecraft().thePlayer != null && uuid.equals(clientUuid)) {
            String selCape = NotEnoughUpdates.INSTANCE.config.hidden.selectedCape;
            if(selCape != null && !selCape.isEmpty()) {
                if(localCape == null) {
                    localCape = new MutablePair<>(new NEUCape(selCape), selCape);
                } else {
                    localCape.setValue(selCape);
                }
            }
        }
        if(uuid.equals(clientUuid) && localCape != null && localCape.getRight() != null && !localCape.getRight().equals("null")) {
            localCape.getLeft().onRenderPlayer(e);
        } else if(capeMap.containsKey(uuid)) {
            capeMap.get(uuid).getLeft().onRenderPlayer(e);
        }
    }

    public static void onTickSlow() {
        if(Minecraft.getMinecraft().theWorld == null) return;

        if(playerMap == null) {
            playerMap = HashBiMap.create(Minecraft.getMinecraft().theWorld.playerEntities.size());
        }
        HashSet<String> contains = new HashSet<>();
        for(EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
            String uuid = player.getUniqueID().toString().replace("-", "");
            contains.add(uuid);
            if(!playerMap.containsValue(player) && !playerMap.containsKey(uuid)) {
                playerMap.put(uuid, player);
            }
        }
        playerMap.keySet().retainAll(contains);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if(Minecraft.getMinecraft().theWorld == null) return;

        if(playerMap == null) {
            return;
        }

        String clientUuid = null;
        if(Minecraft.getMinecraft().thePlayer != null) {
            clientUuid = Minecraft.getMinecraft().thePlayer.getUniqueID().toString().replace("-", "");
        }

        boolean hasLocalCape = localCape != null && localCape.getRight() != null && !localCape.getRight().equals("null");

        Set<String> toRemove = new HashSet<>();
        try {
            for(String playerUUID : capeMap.keySet()) {
                EntityPlayer player;
                if(playerUUID.equals(clientUuid)) {
                    player = Minecraft.getMinecraft().thePlayer;
                } else {
                    player = getPlayerForUUID(playerUUID);
                }
                if(player != null) {
                    String capeName = capeMap.get(playerUUID).getRight();
                    if(capeName != null && !capeName.equals("null")) {
                        if(player == Minecraft.getMinecraft().thePlayer && hasLocalCape) {
                            continue;
                        }
                        capeMap.get(playerUUID).getLeft().setCapeTexture(capeName);
                        capeMap.get(playerUUID).getLeft().onTick(event, player);
                    } else {
                        toRemove.add(playerUUID);
                    }
                }
            }
        } catch(Exception e) {}

        if(hasLocalCape) {
            localCape.getLeft().setCapeTexture(localCape.getValue());
            localCape.getLeft().onTick(event, Minecraft.getMinecraft().thePlayer);
        }
        for(String playerName : toRemove) {
            capeMap.remove(playerName);
        }
    }

    public String[] getCapes() {
        return capes;
    }

}
