package io.github.moulberry.notenoughupdates.infopanes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.NEUOverlay;
import io.github.moulberry.notenoughupdates.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DevInfoPane extends TextInfoPane {

    public DevInfoPane(NEUOverlay overlay, NEUManager manager) {
        super(overlay, manager, "Dev", "");
        text = getText();
    }

    private String getText() {
        String text = "";
        for(Map.Entry<String, JsonElement> entry : manager.getAuctionPricesJson().get("prices").getAsJsonObject().entrySet()) {
            if(!manager.getItemInformation().keySet().contains(entry.getKey())) {
                if(entry.getKey().contains("-")) {
                    continue;
                }
                if(entry.getKey().startsWith("PERFECT")) continue;
                if(Item.itemRegistry.getObject(new ResourceLocation(entry.getKey().toLowerCase())) != null) {
                    continue;
                }
                text += entry.getKey() + "\n";
            }
        }
        return text;
    }

    AtomicBoolean running = new AtomicBoolean(false);
    ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);

    @Override
    public void keyboardInput() {
        if(Keyboard.isKeyDown(Keyboard.KEY_J)) {
            running.set(!running.get());

            if(running.get()) {
                List<String> add = new ArrayList<>();
                for(Map.Entry<String, JsonObject> item : manager.getItemInformation().entrySet()) {
                    if(item.getValue().has("recipe")) {
                        if(!item.getKey().contains("-") && !item.getKey().contains(";")) {
                            add.add(item.getKey());
                        }
                    }
                }
                AtomicInteger index = new AtomicInteger(0);

                ses.schedule(new Runnable() {
                    public void run() {
                        if(!running.get()) return;

                        int i = index.getAndIncrement();
                        String item = add.get(i).split("-")[0].split(";")[0];
                        Minecraft.getMinecraft().thePlayer.sendChatMessage("/viewrecipe " + item);
                        ses.schedule(this, 1000L, TimeUnit.MILLISECONDS);
                    }
                }, 1000L, TimeUnit.MILLISECONDS);
            }
        }
        /*if(Keyboard.isKeyDown(Keyboard.KEY_J) && !running) {
            running = true;
            List<String> add = new ArrayList<>();
            for(Map.Entry<String, JsonElement> entry : manager.getAuctionPricesJson().get("prices").getAsJsonObject().entrySet()) {
                if(!manager.getItemInformation().keySet().contains(entry.getKey())) {
                    if(entry.getKey().contains("-")) {
                        continue;
                    }
                    if(entry.getKey().startsWith("PERFECT")) continue;
                    if(Item.itemRegistry.getObject(new ResourceLocation(entry.getKey().toLowerCase())) != null) {
                        continue;
                    }
                    add.add(entry.getKey());
                }
            }
            AtomicInteger index = new AtomicInteger(0);

            ses.schedule(new Runnable() {
                public void run() {
                    int i = index.getAndIncrement();
                    String item = add.get(i).split("-")[0].split(";")[0];
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/viewrecipe " + item);
                    ses.schedule(this, 1000L, TimeUnit.MILLISECONDS);
                }
            }, 1000L, TimeUnit.MILLISECONDS);
        }*/
    }
}
