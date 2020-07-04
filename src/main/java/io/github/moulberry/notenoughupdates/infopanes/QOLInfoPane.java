package io.github.moulberry.notenoughupdates.infopanes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.NEUOverlay;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class QOLInfoPane extends ScrollableInfoPane {

    /**
     * Not currently used in BETA-1.6
     */

    private LinkedHashMap<String, ItemStack> accessoryMap = new LinkedHashMap<>();
    private LinkedHashMap<String, JsonObject> allTalismans = new LinkedHashMap<>();
    private List<JsonObject> recommended = new ArrayList<>();
    private int maxAccessories = 39; //TODO: Get from API

    private Integer[] talismanRarityValue = new Integer[]{1, 2, 4, 7, 10};

    public QOLInfoPane(NEUOverlay overlay, NEUManager manager) {
        super(overlay, manager);

        Comparator<JsonObject> rarityComparator = new Comparator<JsonObject>() {
            @Override
            public int compare(JsonObject o1, JsonObject o2) {
                int rarity1 = overlay.getRarity(o1.get("lore").getAsJsonArray());
                int rarity2 = overlay.getRarity(o2.get("lore").getAsJsonArray());

                int rarityDiff = rarity2 - rarity1;
                if(rarityDiff != 0) {
                    return rarityDiff;
                }

                return o2.get("internalname").getAsString().compareTo(o1.get("internalname").getAsString());
            }
        };

        TreeSet<JsonObject> all = new TreeSet<>(rarityComparator);
        LinkedHashMap<String, JsonObject> highestRarity = new LinkedHashMap<>();
        LinkedHashMap<String, JsonObject> lowerRarity = new LinkedHashMap<>();

        for(Map.Entry<String, JsonObject> entry : manager.getItemInformation().entrySet()) {
            if(overlay.checkItemType(entry.getValue().get("lore").getAsJsonArray(), "ACCESSORY") >= 0) {
                all.add(entry.getValue());
            }
        }
        outer:
        for(JsonObject o : all) {
            String internalname = o.get("internalname").getAsString();
            String name = getTalismanName(internalname);
            int power = getTalismanPower(internalname);
            for(JsonObject o2 : all) {
                if(o != o2) {
                    String internalname2 = o2.get("internalname").getAsString();
                    String name2 = getTalismanName(internalname2);
                    if(name2.equals(name)) {
                        int power2 = getTalismanPower(internalname2);
                        if(power2 > power) {
                            lowerRarity.put(internalname, o);
                            continue outer;
                        }
                    }
                }
            }
            highestRarity.put(internalname, o);
        }
        for(Map.Entry<String, JsonObject> entry : highestRarity.entrySet()) {
            allTalismans.put(entry.getKey(), entry.getValue());
        }
        for(Map.Entry<String, JsonObject> entry : lowerRarity.entrySet()) {
            allTalismans.put(entry.getKey(), entry.getValue());
        }

        HashMap<String, String> args = new HashMap<>();
        String uuid = Minecraft.getMinecraft().thePlayer.getGameProfile().getId().toString();
        args.put("uuid", uuid);
        manager.hypixelApi.getHypixelApiAsync(manager.config.apiKey.value, "skyblock/profiles",
            args, jsonObject -> {
                if(jsonObject.get("success").getAsBoolean()) {
                    JsonObject currProfile = null;
                    for(JsonElement e : jsonObject.get("profiles").getAsJsonArray()) {
                        JsonObject profile = e.getAsJsonObject();
                        String profileId = profile.get("profile_id").getAsString();
                        String cuteName = profile.get("cute_name").getAsString();

                        if(manager.currentProfile.equals(cuteName)) {
                            JsonObject members = profile.get("members").getAsJsonObject();
                            JsonObject profile_member = members.get(uuid.replaceAll("-","")).getAsJsonObject();
                            currProfile = profile_member;
                        }
                    }
                    if(currProfile.has("talisman_bag")) {
                        String b64 = currProfile.get("talisman_bag").getAsJsonObject().get("data").getAsString();
                        try {
                            NBTTagCompound tag = CompressedStreamTools.readCompressed(new ByteArrayInputStream(Base64.getDecoder().decode(b64)));
                            NBTTagList list = tag.getTagList("i", 10);
                            for(int i=0; i<list.tagCount(); i++) {
                                NBTTagCompound accessory = list.getCompoundTagAt(i);
                                if(accessory.hasKey("tag")) {
                                    String accessoryID = accessory.getCompoundTag("tag")
                                            .getCompoundTag("ExtraAttributes").getString("id");
                                    ItemStack accessoryStack = ItemStack.loadItemStackFromNBT(accessory);
                                    accessoryMap.put(accessoryID, accessoryStack);
                                }

                            }
                        } catch(IOException e) {
                        }

                        int lowestRarity = -1;
                        if(accessoryMap.size() >= maxAccessories) {
                            lowestRarity = 999;
                            for(Map.Entry<String, ItemStack> entry : accessoryMap.entrySet()) {
                                JsonObject json = manager.getJsonForItem(entry.getValue());
                                int rarity = overlay.getRarity(json.get("lore").getAsJsonArray());
                                if(rarity < lowestRarity) {
                                    lowestRarity = rarity;
                                }
                            }
                        }
                        System.out.println("lowestrarity:"+lowestRarity);

                        TreeMap<Float, JsonObject> valueMap = new TreeMap<>();
                        outer:
                        for(Map.Entry<String, JsonObject> entry : allTalismans.entrySet()) {
                            int rarity = overlay.getRarity(entry.getValue().get("lore").getAsJsonArray());
                            System.out.println(entry.getKey() + ":" + rarity);
                            if(rarity > lowestRarity) {
                                System.out.println("found greater:"+entry.getKey());
                                float rarityVal = (float)talismanRarityValue[rarity];
                                System.out.println("rarity val:"+rarityVal);
                                float price = manager.getCraftCost(entry.getKey()).craftCost;

                                System.out.println("cc:"+price);
                                if(price < 0) {
                                    System.out.println("invalid price:"+entry.getKey());
                                    continue;
                                }

                                String internalname = entry.getValue().get("internalname").getAsString();
                                String name = getTalismanName(internalname);
                                int power = getTalismanPower(internalname);
                                for(Map.Entry<String, ItemStack> entry2 : accessoryMap.entrySet()) {
                                    try {
                                        JsonObject json = manager.getJsonForItem(entry2.getValue());
                                        String internalname2 = json.get("internalname").getAsString();

                                        if(internalname.equals(internalname2)) {
                                            //continue outer;
                                        }

                                        String name2 = getTalismanName(internalname2);
                                    } catch(Exception e) {
                                        e.printStackTrace();
                                        System.out.println(":( -> " + entry2.getKey());
                                    }


                                    /*if(name2.equals(name)) {
                                        int power2 = getTalismanPower(internalname2);
                                        if(power2 > power) {
                                            continue outer;
                                        }
                                    }*/
                                }

                                valueMap.put(-rarityVal/price, entry.getValue());
                            }
                        }
                        System.out.println("valuemap size:"+valueMap.size());
                        int i=0;
                        for(Map.Entry<Float, JsonObject> entry : valueMap.entrySet()) {
                            recommended.add(entry.getValue());
                            if(++i >= 500) {
                                break;
                            }
                        }
                        System.out.println("recommended size:"+recommended.size());
                    }
                    //jsonObject.get("profiles")
                }
            });
    }


    String[] talismanPowers = new String[]{"RING","ARTIFACT"};
    public int getTalismanPower(String internalname) {
        for(int i=0; i<talismanPowers.length; i++) {
            if(internalname.endsWith("_"+talismanPowers[i])) {
                return i+1;
            }
        }
        return 0;
    }

    public String getTalismanName(String internalname) {
        String[] split = internalname.split("_");
        StringBuilder name = new StringBuilder();
        for(int i=0; i<split.length; i++) {
            name.append(split[i]);
            if(i < split.length-1) {
                name.append("_");
            }
        }
        return name.toString();
    }

    public void render(int width, int height, Color bg, Color fg, ScaledResolution scaledresolution, int mouseX, int mouseY) {
        renderDefaultBackground(width, height, bg);

        int paneWidth = (int)(width/3*overlay.getWidthMult());
        int rightSide = (int)(width*overlay.getInfoPaneOffsetFactor());
        int leftSide = rightSide - paneWidth;

        int y=overlay.getBoxPadding()+10;
        y += renderParagraph(width, height, y, "Current Accessories");

        ItemStack display = null;
        int x=leftSide+overlay.getBoxPadding()+5;
        for(Map.Entry<String, ItemStack> entry : accessoryMap.entrySet()) {
            if(mouseX > x && mouseX < x+16) {
                if(mouseY > y && mouseY < y+16) {
                    display = entry.getValue();
                }
            }

            drawRect(x, y, x+16, y+16, fg.getRGB());
            Utils.drawItemStack(entry.getValue(), x, y);
            x += 20;
            if(x + 20 + (leftSide+overlay.getBoxPadding()+5) > paneWidth) {
                x=leftSide+overlay.getBoxPadding()+5;
                y+=20;
            }
        }

        y+=20;

        y += renderParagraph(width, height, y, "Missing Accessories");

        y+=10;

        x=leftSide+overlay.getBoxPadding()+5;
        for(Map.Entry<String, JsonObject> entry : allTalismans.entrySet()) {
            if(accessoryMap.containsKey(entry.getKey())) {
                continue;
            }
            if(mouseX > x && mouseX < x+16) {
                if(mouseY > y && mouseY < y+16) {
                    display = manager.jsonToStack(entry.getValue());
                }
            }

            drawRect(x, y, x+16, y+16, fg.getRGB());
            Utils.drawItemStack(manager.jsonToStack(entry.getValue()), x, y);
            x += 20;
            if(x + 20 + (leftSide+overlay.getBoxPadding()+5) > paneWidth) {
                x=leftSide+overlay.getBoxPadding()+5;
                y+=20;
            }
        }

        y+=20;
        y += renderParagraph(width, height, y, "Recommended Accessory Upgrades");

        x=leftSide+overlay.getBoxPadding()+5;
        for(JsonObject json : recommended) {
            if(mouseX > x && mouseX < x+16) {
                if(mouseY > y && mouseY < y+16) {
                    display = manager.jsonToStack(json);
                }
            }

            drawRect(x, y, x+16, y+16, fg.getRGB());
            Utils.drawItemStack(manager.jsonToStack(json), x, y);
            x += 20;
            if(x + 20 + (leftSide+overlay.getBoxPadding()+5) > paneWidth) {
                x=leftSide+overlay.getBoxPadding()+5;
                y+=20;
            }
        }

        //L:9/cost, E=6/cost, R=3/cost, C=1/cost


        if(display != null) {
            List<String> list = display.getTooltip(Minecraft.getMinecraft().thePlayer,
                    Minecraft.getMinecraft().gameSettings.advancedItemTooltips);

            for (int i = 0; i < list.size(); ++i){
                if (i == 0){
                    list.set(i, display.getRarity().rarityColor + list.get(i));
                } else {
                    list.set(i, EnumChatFormatting.GRAY + list.get(i));
                }
            }

            Utils.drawHoveringText(list, mouseX, mouseY, width, height, -1, Minecraft.getMinecraft().fontRendererObj);
        }
    }

    public boolean keyboardInput() {
        return false;
    }


    private int renderParagraph(int width, int height, int startY, String text) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

        int paneWidth = (int)(width/3*overlay.getWidthMult());
        int rightSide = (int)(width*overlay.getInfoPaneOffsetFactor());
        int leftSide = rightSide - paneWidth;

        int yOff = 0;
        for(String line : text.split("\n")) {
            yOff += Utils.renderStringTrimWidth(line, fr, false,leftSide+overlay.getBoxPadding() + 5,
                    startY + yOff,
                    width*1/3-overlay.getBoxPadding()*2-10, Color.WHITE.getRGB(), -1);
            yOff += 16;
        }

        return yOff;
    }


}
