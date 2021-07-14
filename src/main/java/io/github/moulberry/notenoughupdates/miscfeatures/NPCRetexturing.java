package io.github.moulberry.notenoughupdates.miscfeatures;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class NPCRetexturing implements IResourceManagerReloadListener {

    private static final NPCRetexturing INSTANCE = new NPCRetexturing();

    private static final ResourceLocation npcRetexturingJson = new ResourceLocation("notenoughupdates:npccustomtextures/config.json");

    private final Gson gson = new GsonBuilder().create();

    public static class Skin {
        public ResourceLocation skinLocation;
        public boolean skinny;

        public Skin(ResourceLocation skinLocation, boolean skinny) {
            this.skinLocation = skinLocation;
            this.skinny = skinny;
        }
    }

    private HashMap<AbstractClientPlayer, Skin> skinOverrideCache = new HashMap<>();
    private HashMap<String, Skin> skinMap = new HashMap<>();

    private boolean gettingSkin = false;

    public Skin getSkin(AbstractClientPlayer player) {
        if(gettingSkin) return null;

        if(player.getUniqueID().version() == 4) return null;

        if(skinOverrideCache.containsKey(player)) {
            return skinOverrideCache.get(player);
        }

        gettingSkin = true;
        ResourceLocation loc = player.getLocationSkin();
        gettingSkin = false;

        if(skinMap.containsKey(loc.getResourcePath())) {
            Skin skin = skinMap.get(loc.getResourcePath());
            skinOverrideCache.put(player, skin);
            return skin;
        }

        skinOverrideCache.put(player, null);
        return null;
    }

    public void tick() {
        skinOverrideCache.clear();
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        skinMap.clear();

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(
                Minecraft.getMinecraft().getResourceManager().getResource(npcRetexturingJson).getInputStream(), StandardCharsets.UTF_8))) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            if(json == null) return;

            for(Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if(entry.getValue().isJsonObject()) {
                    JsonObject val = entry.getValue().getAsJsonObject();

                    Skin skin = new Skin(new ResourceLocation(val.get("skin").getAsString()), val.get("skinny").getAsBoolean());
                    skinMap.put("skins/"+entry.getKey(), skin);
                }
            }
        } catch(Exception e) {
        }
    }


    public static NPCRetexturing getInstance() {
        return INSTANCE;
    }

}
