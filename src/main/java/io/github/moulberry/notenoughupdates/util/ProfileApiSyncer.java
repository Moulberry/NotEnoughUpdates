package io.github.moulberry.notenoughupdates.util;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

import java.util.HashMap;
import java.util.function.Consumer;

public class ProfileApiSyncer {

    private static ProfileApiSyncer INSTANCE = new ProfileApiSyncer();

    private HashMap<String, Long> resyncTimes = new HashMap<>();
    private HashMap<String, Runnable> syncingCallbacks = new HashMap<>();
    private HashMap<String, Consumer<ProfileViewer.Profile>> finishSyncCallbacks = new HashMap<>();
    private long lastResync;

    public static ProfileApiSyncer getInstance() {
        return INSTANCE;
    }

    public void requestResync(String id, long timeBetween) {
        requestResync(id, timeBetween, null);
    }

    public void requestResync(String id, long timeBetween, Runnable syncingCallback) {
        requestResync(id, timeBetween, null, null);
    }

    public void requestResync(String id, long timeBetween, Runnable syncingCallback, Consumer<ProfileViewer.Profile> finishSyncCallback) {
        resyncTimes.put(id, timeBetween);
        syncingCallbacks.put(id, syncingCallback);
        finishSyncCallbacks.put(id, finishSyncCallback);
    }

    public long getCurrentResyncTime() {
        long time = -1;
        for(long l : resyncTimes.values()) {
            if(l > 0 && (l < time || time == -1)) time = l;
        }
        return time;
    }

    public void tick() {
        if(Minecraft.getMinecraft().thePlayer == null) return;

        long resyncTime = getCurrentResyncTime();

        if(resyncTime < 0) return;

        long currentTime = System.currentTimeMillis();

        if(currentTime - lastResync > resyncTime) {
            lastResync = currentTime;
            resyncTimes.clear();

            for(Runnable r : syncingCallbacks.values()) r.run();
            syncingCallbacks.clear();

            forceResync();
        }
    }

    private void forceResync() {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if(player == null) return;

        String uuid = player.getUniqueID().toString().replace("-", "");
        NotEnoughUpdates.profileViewer.getProfileReset(uuid, (profile) -> {
            for(Consumer<ProfileViewer.Profile> c : finishSyncCallbacks.values()) c.accept(profile);
            finishSyncCallbacks.clear();
        });
    }

}
