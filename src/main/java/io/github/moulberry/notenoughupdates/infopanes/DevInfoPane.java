package io.github.moulberry.notenoughupdates.infopanes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.NEUOverlay;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class DevInfoPane extends TextInfoPane {

    /**
     * Provides some dev functions used to help with adding new items/detecting missing items.
     */

    public DevInfoPane(NEUOverlay overlay, NEUManager manager) {
        super(overlay, manager, "Dev", "");
        text = getText();
    }

    private String getText() {
        String text = "";

        /*for(Map.Entry<String, JsonObject> item : manager.getItemInformation().entrySet()) {
            if(!item.getValue().has("infoType") || item.getValue().get("infoType").getAsString().isEmpty()) {
                text += item.getKey() + "\n";
            }
        }*/
        /*for(String s : manager.neuio.getRemovedItems(manager.getItemInformation().keySet())) {
            text += s + "\n";
        }

        if(true) return text;*/

        /*for(Map.Entry<String, JsonObject> item : manager.getItemInformation().entrySet()) {
            if(!item.getValue().has("infoType") || item.getValue().get("infoType").getAsString().isEmpty()) {
                text += item.getKey() + "\n";
            }
        }*/
        //if(true) return text;

        for(String internalname : manager.auctionManager.internalnameToAucIdMap.keySet()) {
            if(!manager.getItemInformation().containsKey(internalname)) {
                text += internalname + "\n";
            }
        }

        /*for(Map.Entry<String, JsonElement> entry : manager.getAuctionPricesJson().get("prices").getAsJsonObject().entrySet()) {
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
        }*/
        return text;
    }

    AtomicBoolean running = new AtomicBoolean(false);
    ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);

    @Override
    public boolean keyboardInput() {
        if(Keyboard.isKeyDown(Keyboard.KEY_J)) {
            running.set(!running.get());

            for(Map.Entry<String, JsonObject> item : manager.getItemInformation().entrySet()) {
                /*if(!item.getValue().has("infoType") || item.getValue().get("infoType").getAsString().isEmpty()) {
                    if(item.getValue().has("info") && item.getValue().get("info").getAsJsonArray().size()>0) {
                        item.getValue().addProperty("infoType", "WIKI_URL");
                        try {
                            manager.writeJsonDefaultDir(item.getValue(), item.getKey()+".json");
                        } catch(IOException e){}
                        manager.loadItem(item.getKey());
                    }
                }*/
                /*if(item.getKey().startsWith("PET_ITEM_")) {
                    item.getValue().addProperty("infoType", "WIKI_URL");
                    JsonArray array = new JsonArray();
                    array.add(new JsonPrimitive("https://hypixel-skyblock.fandom.com/wiki/Pet_Items"));
                    item.getValue().add("info", array);
                    try {
                        manager.writeJsonDefaultDir(item.getValue(), item.getKey()+".json");
                    } catch(IOException e){}
                    manager.loadItem(item.getKey());
                }*/
                /*if(!item.getValue().has("infoType") || item.getValue().get("infoType").getAsString().isEmpty()) {
                    //String prettyName =

                    String itemS = item.getKey().split("-")[0].split(";")[0];
                    StringBuilder prettyName = new StringBuilder();
                    boolean capital = true;
                    for(int i=0; i<itemS.length(); i++) {
                        char c = itemS.charAt(i);
                        if(capital) {
                            prettyName.append(String.valueOf(c).toUpperCase());
                            capital = false;
                        } else {
                            prettyName.append(String.valueOf(c).toLowerCase());
                        }
                        if(String.valueOf(c).equals("_")) {
                            capital = true;
                        }
                    }
                    String prettyNameS = prettyName.toString();
                    File f = manager.getWebFile("https://hypixel-skyblock.fandom.com/wiki/"+prettyNameS);
                    if(f == null) {
                        continue;
                        //#REDIRECT [[Armor of Magma]]
                    }
                    StringBuilder sb = new StringBuilder();
                    try(BufferedReader br = new BufferedReader(new InputStreamReader(
                            new FileInputStream(f), StandardCharsets.UTF_8))) {
                        String l;
                        while((l = br.readLine()) != null){
                            sb.append(l).append("\n");
                        }
                    } catch(IOException e) {
                        continue;
                    }
                    if(sb.toString().isEmpty()) {
                        continue;
                    }
                    if(sb.toString().startsWith("#REDIRECT")) {
                        prettyNameS = sb.toString().split("\\[\\[")[1].split("]]")[0].replaceAll(" ", "_");
                    }
                    item.getValue().addProperty("infoType", "WIKI_URL");
                    JsonArray array = new JsonArray();
                    array.add(new JsonPrimitive("https://hypixel-skyblock.fandom.com/wiki/"+prettyNameS));
                    item.getValue().add("info", array);
                    try {
                        manager.writeJsonDefaultDir(item.getValue(), item.getKey()+".json");
                    } catch(IOException e){}
                    manager.loadItem(item.getKey());
                }*/
            }

            /*if(running.get()) {
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
            }*/
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
        return false;
    }
}
