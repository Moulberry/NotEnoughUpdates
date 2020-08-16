package io.github.moulberry.notenoughupdates;

import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.auction.CustomAHGui;
import io.github.moulberry.notenoughupdates.gamemodes.SBGamemodes;
import io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer;
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewer;
import io.github.moulberry.notenoughupdates.questing.SBScoreboardData;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.event.ClickEvent;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.moulberry.notenoughupdates.GuiTextures.dungeon_chest_worth;

public class NEUEventListener {

    private NotEnoughUpdates neu;

    private boolean hoverInv = false;
    private boolean focusInv = false;

    private boolean joinedSB = false;

    public NEUEventListener(NotEnoughUpdates neu) {
        this.neu = neu;
    }

    private void displayUpdateMessageIfOutOfDate() {
        File repo = neu.manager.repoLocation;
        if(repo.exists()) {
            File updateJson = new File(repo, "update.json");
            try {
                JsonObject o = neu.manager.getJsonFromFile(updateJson);

                String version = o.get("version").getAsString();

                if(!neu.VERSION.equalsIgnoreCase(version)) {
                    String update_msg = o.get("update_msg").getAsString();
                    String discord_link = o.get("discord_link").getAsString();
                    String youtube_link = o.get("youtube_link").getAsString();
                    String update_link = o.get("update_link").getAsString();
                    String github_link = o.get("github_link").getAsString();
                    String other_text = o.get("other_text").getAsString();
                    String other_link = o.get("other_link").getAsString();

                    int first_len = -1;
                    for(String line : update_msg.split("\n")) {
                        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
                        int len = fr.getStringWidth(line);
                        if(first_len == -1) {
                            first_len = len;
                        }
                        int missing_len = first_len-len;
                        if(missing_len > 0) {
                            StringBuilder sb = new StringBuilder(line);
                            for(int i=0; i<missing_len/8; i++) {
                                sb.insert(0, " ");
                            }
                            line = sb.toString();
                        }
                        line = line.replaceAll("\\{version}", version);
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(line));
                    }

                    neu.displayLinks(o);

                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(""));

                }
            } catch(Exception ignored) {}
        }
    }

    /**
     * 1)Will send the cached message from #sendChatMessage when at least 200ms has passed since the last message.
     * This is used in order to prevent the mod spamming messages.
     * 2)Adds unique items to the collection log
     */
    private HashMap<String, Long> newItemAddMap = new HashMap<>();
    private long lastLongUpdate = 0;
    private long lastSkyblockScoreboard = 0;
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if(event.phase != TickEvent.Phase.START) return;

        boolean longUpdate = false;
        long currentTime = System.currentTimeMillis();
        if(currentTime - lastLongUpdate > 1000) {
            longUpdate = true;
            lastLongUpdate = currentTime;
        }
        if(longUpdate) {
            neu.updateSkyblockScoreboard();
            if(neu.hasSkyblockScoreboard()) {
                lastSkyblockScoreboard = currentTime;
                if(!joinedSB) {
                    joinedSB = true;

                    SBGamemodes.loadFromFile();

                    if(neu.manager.config.showUpdateMsg.value) {
                        displayUpdateMessageIfOutOfDate();
                    }

                    if(!neu.manager.config.loadedModBefore.value) {
                        neu.manager.config.loadedModBefore.value = true;
                        try { neu.manager.saveConfig(); } catch(IOException e) {}

                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(""));
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                                EnumChatFormatting.BLUE+"It seems this is your first time using NotEnoughUpdates."));
                        ChatComponentText clickText = new ChatComponentText(
                                EnumChatFormatting.YELLOW+"Click this message if you would like to view a short tutorial.");
                        clickText.setChatStyle(Utils.createClickStyle(ClickEvent.Action.RUN_COMMAND, "/neututorial"));
                        Minecraft.getMinecraft().thePlayer.addChatMessage(clickText);
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(""));
                    }
                }
                SBScoreboardData.getInstance().tick();
                //GuiQuestLine.questLine.tick();
            }
            if(currentTime - lastSkyblockScoreboard < 5*60*1000) { //5 minutes
                neu.manager.auctionManager.tick();
            } else {
                neu.manager.auctionManager.markNeedsUpdate();
            }
            //ItemRarityHalo.resetItemHaloCache();
        }
        if(longUpdate && neu.hasSkyblockScoreboard()) {
            if(neu.manager.getCurrentProfile() == null || neu.manager.getCurrentProfile().length() == 0) {
                ProfileViewer.Profile profile = neu.profileViewer.getProfile(
                        Minecraft.getMinecraft().thePlayer.getUniqueID().toString().replace("-", ""), (json) -> {});
                if(profile != null) {
                    String latest = profile.getLatestProfile();
                    if(latest != null) {
                        neu.manager.setCurrentProfileBackup(profile.getLatestProfile());
                    }
                }
            }
            if(neu.manager.getCurrentProfile() != null && neu.manager.getCurrentProfile().length() > 0) {
                HashSet<String> newItem = new HashSet<>();
                if(Minecraft.getMinecraft().currentScreen instanceof GuiContainer &&
                        !(Minecraft.getMinecraft().currentScreen instanceof GuiCrafting)) {
                    boolean usableContainer = true;
                    for(ItemStack stack : Minecraft.getMinecraft().thePlayer.openContainer.getInventory()) {
                        if(stack == null) {
                            continue;
                        }
                        if(stack.hasTagCompound()) {
                            NBTTagCompound tag = stack.getTagCompound();
                            if(tag.hasKey("ExtraAttributes", 10)) {
                                continue;
                            }
                        }
                        usableContainer = false;
                        break;
                    }
                    if(!usableContainer) {
                        if(Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
                            GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;
                            ContainerChest container = (ContainerChest) chest.inventorySlots;
                            String containerName = container.getLowerChestInventory().getDisplayName().getUnformattedText();

                            if(containerName.equals("Accessory Bag") || containerName.startsWith("Wardrobe")) {
                                usableContainer = true;
                            }
                        }
                    }
                    if(usableContainer) {
                        for(ItemStack stack : Minecraft.getMinecraft().thePlayer.inventory.mainInventory) {
                            processUniqueStack(stack, newItem);
                        }
                        for(ItemStack stack : Minecraft.getMinecraft().thePlayer.openContainer.getInventory()) {
                            processUniqueStack(stack, newItem);
                        }
                    }
                } else {
                    for(ItemStack stack : Minecraft.getMinecraft().thePlayer.inventory.mainInventory) {
                        processUniqueStack(stack, newItem);
                    }
                }
                newItemAddMap.keySet().retainAll(newItem);
            }
        }
    }

    private void processUniqueStack(ItemStack stack, HashSet<String> newItem) {
        if(stack != null && stack.hasTagCompound()) {
            String internalname = neu.manager.getInternalNameForItem(stack);
            if(internalname != null) {
                ArrayList<String> log = neu.manager.config.collectionLog.value.computeIfAbsent(
                        neu.manager.getCurrentProfile(), k -> new ArrayList<>());
                if(!log.contains(internalname)) {
                    newItem.add(internalname);
                    if(newItemAddMap.containsKey(internalname)) {
                        if(System.currentTimeMillis() - newItemAddMap.get(internalname) > 1000) {
                            log.add(internalname);
                            try { neu.manager.saveConfig(); } catch(IOException ignored) {}
                        }
                    } else {
                        newItemAddMap.put(internalname, System.currentTimeMillis());
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority= EventPriority.HIGHEST)
    public void onRenderEntitySpecials(RenderLivingEvent.Specials.Pre event) {
        if(Minecraft.getMinecraft().currentScreen instanceof GuiProfileViewer) {
            if(((GuiProfileViewer)Minecraft.getMinecraft().currentScreen).getEntityPlayer() == event.entity) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent event) {
        if(event.type != null && event.type.equals(RenderGameOverlayEvent.ElementType.BOSSHEALTH) &&
                Minecraft.getMinecraft().currentScreen instanceof GuiContainer && neu.overlay.isUsingMobsFilter()) {
            event.setCanceled(true);
        }
    }

    /**
     * When opening a GuiContainer, will reset the overlay and load the config.
     * When closing a GuiContainer, will save the config.
     * Also includes a dev feature used for automatically acquiring crafting information from the "Crafting Table" GUI.
     */
    AtomicBoolean missingRecipe = new AtomicBoolean(false);
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        neu.manager.auctionManager.customAH.lastGuiScreenSwitch = System.currentTimeMillis();

        if(event.gui == null && neu.manager.auctionManager.customAH.isRenderOverAuctionView() &&
                !(Minecraft.getMinecraft().currentScreen instanceof CustomAHGui)) {
            event.gui = new CustomAHGui();
        }

        if(!(event.gui instanceof GuiChest || event.gui instanceof GuiEditSign)) {
            neu.manager.auctionManager.customAH.setRenderOverAuctionView(false);
        } else if(event.gui instanceof GuiChest && (neu.manager.auctionManager.customAH.isRenderOverAuctionView() ||
                Minecraft.getMinecraft().currentScreen instanceof CustomAHGui)){
            GuiChest chest = (GuiChest) event.gui;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            String containerName = container.getLowerChestInventory().getDisplayName().getUnformattedText();

            neu.manager.auctionManager.customAH.setRenderOverAuctionView(containerName.trim().equals("Auction View") ||
                    containerName.trim().equals("BIN Auction View") || containerName.trim().equals("Confirm Bid") ||
                    containerName.trim().equals("Confirm Purchase"));
        }

        //OPEN
        if(Minecraft.getMinecraft().currentScreen == null
                && event.gui instanceof GuiContainer) {
            neu.overlay.reset();
            neu.manager.loadConfig();
        }
        //CLOSE
        if(Minecraft.getMinecraft().currentScreen instanceof GuiContainer
                && event.gui == null) {
            try {
                neu.manager.saveConfig();
            } catch(IOException e) {}
        }
        if(event.gui != null && neu.manager.config.dev.value) {
            if(event.gui instanceof GuiChest) {
                GuiChest eventGui = (GuiChest) event.gui;
                ContainerChest cc = (ContainerChest) eventGui.inventorySlots;
                IInventory lower = cc.getLowerChestInventory();
                ses.schedule(() -> {
                    if(Minecraft.getMinecraft().currentScreen != event.gui) {
                        return;
                    }
                    if(lower.getStackInSlot(23).getDisplayName().endsWith("Crafting Table")) {
                        try {
                            ItemStack res = lower.getStackInSlot(25);
                            String resInternalname = neu.manager.getInternalNameForItem(res);

                            if(lower.getStackInSlot(48) != null) {
                                String backName = null;
                                NBTTagCompound tag = lower.getStackInSlot(48).getTagCompound();
                                if(tag.hasKey("display", 10)) {
                                    NBTTagCompound nbttagcompound = tag.getCompoundTag("display");
                                    if(nbttagcompound.getTagId("Lore") == 9){
                                        NBTTagList nbttaglist1 = nbttagcompound.getTagList("Lore", 8);
                                        backName = nbttaglist1.getStringTagAt(0);
                                    }
                                }

                                if(backName != null) {
                                    String[] split = backName.split(" ");
                                    if(split[split.length-1].contains("Rewards")) {
                                        String col = backName.substring(split[0].length()+1,
                                                backName.length()-split[split.length-1].length()-1);

                                        JsonObject json = neu.manager.getItemInformation().get(resInternalname);
                                        json.addProperty("crafttext", "Requires: " + col);

                                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("Added: " + resInternalname));
                                        neu.manager.writeJsonDefaultDir(json, resInternalname+".json");
                                        neu.manager.loadItem(resInternalname);
                                    }
                                }
                            }

                            /*JsonArray arr = null;
                            File f = new File(neu.manager.configLocation, "missing.json");
                            try(InputStream instream = new FileInputStream(f)) {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(instream, StandardCharsets.UTF_8));
                                JsonObject json = neu.manager.gson.fromJson(reader, JsonObject.class);
                                arr = json.getAsJsonArray("missing");
                            } catch(IOException e) {}
                            try {
                                JsonObject json = new JsonObject();
                                JsonArray newArr = new JsonArray();
                                for(JsonElement e : arr) {
                                    if(!e.getAsString().equals(resInternalname)) {
                                        newArr.add(e);
                                    }
                                }
                                json.add("missing", newArr);
                                neu.manager.writeJson(json, f);
                            } catch(IOException e) {}*/



                            /*JsonObject recipe = new JsonObject();

                            String[] x = {"1","2","3"};
                            String[] y = {"A","B","C"};

                            for(int i=0; i<=18; i+=9) {
                                for(int j=0; j<3; j++) {
                                    ItemStack stack = lower.getStackInSlot(10+i+j);
                                    String internalname = "";
                                    if(stack != null) {
                                        internalname = neu.manager.getInternalNameForItem(stack);
                                        if(!neu.manager.getItemInformation().containsKey(internalname)) {
                                            neu.manager.writeItemToFile(stack);
                                        }
                                        internalname += ":"+stack.stackSize;
                                    }
                                    recipe.addProperty(y[i/9]+x[j], internalname);
                                }
                            }

                            JsonObject json = neu.manager.getJsonForItem(res);
                            json.add("recipe", recipe);
                            json.addProperty("internalname", resInternalname);
                            json.addProperty("clickcommand", "viewrecipe");
                            json.addProperty("modver", NotEnoughUpdates.VERSION);

                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("Added: " + resInternalname));
                            neu.manager.writeJsonDefaultDir(json, resInternalname+".json");
                            neu.manager.loadItem(resInternalname);*/
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, 200, TimeUnit.MILLISECONDS);
                return;
            }
        }
    }

    /**
     * 1) When receiving "You are playing on profile" messages, will set the current profile.
     * 2) When a /viewrecipe command fails (i.e. player does not have recipe unlocked, will open the custom recipe GUI)
     * 3) Replaces lobby join notifications when streamer mode is active
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onGuiChat(ClientChatReceivedEvent e) {
        String r = null;
        String unformatted = Utils.cleanColour(e.message.getUnformattedText());
        if(unformatted.startsWith("You are playing on profile: ")) {
            neu.manager.setCurrentProfile(unformatted.substring("You are playing on profile: ".length()).split(" ")[0].trim());
        } else if(unformatted.startsWith("Your profile was changed to: ")) {//Your profile was changed to:
            neu.manager.setCurrentProfile(unformatted.substring("Your profile was changed to: ".length()).split(" ")[0].trim());
        } else if(unformatted.startsWith("Your new API key is ")) {
            neu.manager.config.apiKey.value = unformatted.substring("Your new API key is ".length());
            try { neu.manager.saveConfig(); } catch(IOException ioe) {}
        }
        if(e.message.getFormattedText().equals(EnumChatFormatting.RESET.toString()+
                EnumChatFormatting.RED+"You haven't unlocked this recipe!"+EnumChatFormatting.RESET)) {
            r =  EnumChatFormatting.RED+"You haven't unlocked this recipe!";
        } else if(e.message.getFormattedText().startsWith(EnumChatFormatting.RESET.toString()+
                EnumChatFormatting.RED+"Invalid recipe ")) {
            r = "";
        }
        if(r != null) {
            if(neu.manager.failViewItem(r)) {
                e.setCanceled(true);
            }
            missingRecipe.set(true);
        }
        //System.out.println(e.message);
        if(unformatted.startsWith("Sending to server") &&
                neu.isOnSkyblock() && neu.manager.config.streamerMode.value && e.message instanceof ChatComponentText) {
            String m = e.message.getFormattedText();
            String m2 = StreamerMode.filterChat(e.message.getFormattedText());
            if(!m.equals(m2)) {
                e.message = new ChatComponentText(m2);
            }
        }
    }

    /**
     * Sets hoverInv and focusInv variables, representing whether the NEUOverlay should render behind the inventory when
     * (hoverInv == true) and whether mouse/kbd inputs shouldn't be sent to NEUOverlay (focusInv == true).
     *
     * If hoverInv is true, will render the overlay immediately (resulting in the inventory being drawn over the GUI)
     * If hoverInv is false, the overlay will render in #onGuiScreenDraw (resulting in the GUI being drawn over the inv)
     *
     * All of this only matters if players are using gui scale auto which may result in the inventory being drawn
     * over the various panes.
     * @param event
     */
    @SubscribeEvent
    public void onGuiBackgroundDraw(GuiScreenEvent.BackgroundDrawnEvent event) {
        if((shouldRenderOverlay(event.gui) || event.gui instanceof CustomAHGui) && neu.isOnSkyblock()) {
            ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
            int width = scaledresolution.getScaledWidth();

            boolean hoverPane = event.getMouseX() < width*neu.overlay.getInfoPaneOffsetFactor() ||
                    event.getMouseX() > width*neu.overlay.getItemPaneOffsetFactor();

            if(event.gui instanceof GuiContainer) {
                try {
                    int xSize = (int) Utils.getField(GuiContainer.class, event.gui, "xSize", "field_146999_f");
                    int ySize = (int) Utils.getField(GuiContainer.class, event.gui, "ySize", "field_147000_g");
                    int guiLeft = (int) Utils.getField(GuiContainer.class, event.gui, "guiLeft", "field_147003_i");
                    int guiTop = (int) Utils.getField(GuiContainer.class, event.gui, "guiTop", "field_147009_r");

                    hoverInv = event.getMouseX() > guiLeft && event.getMouseX() < guiLeft + xSize &&
                            event.getMouseY() > guiTop && event.getMouseY() < guiTop + ySize;

                    if(hoverPane) {
                        if(!hoverInv) focusInv = false;
                    } else {
                        focusInv = true;
                    }
                } catch(NullPointerException npe) {
                    npe.printStackTrace();
                    focusInv = !hoverPane;
                }
            }
            if(event.gui instanceof GuiItemRecipe) {
                GuiItemRecipe guiItemRecipe = ((GuiItemRecipe)event.gui);
                hoverInv = event.getMouseX() > guiItemRecipe.guiLeft && event.getMouseX() < guiItemRecipe.guiLeft + guiItemRecipe.xSize &&
                        event.getMouseY() > guiItemRecipe.guiTop && event.getMouseY() < guiItemRecipe.guiTop + guiItemRecipe.ySize;

                if(hoverPane) {
                    if(!hoverInv) focusInv = false;
                } else {
                    focusInv = true;
                }
            }
            if(focusInv) {
                try {
                    neu.overlay.render(event.getMouseX(), event.getMouseY(), hoverInv && focusInv);
                } catch(ConcurrentModificationException e) {e.printStackTrace();}
                GL11.glTranslatef(0, 0, 10);
            }
        }

        if(shouldRenderOverlay(event.gui) && neu.isOnSkyblock()) {
            renderDungeonChestOverlay(event.gui);
        }
    }

    @SubscribeEvent
    public void onGuiScreenDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if(event.gui instanceof CustomAHGui || neu.manager.auctionManager.customAH.isRenderOverAuctionView()) {
            event.setCanceled(true);

            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            int width = scaledResolution.getScaledWidth();
            int height = scaledResolution.getScaledHeight();

            //Dark background
            Utils.drawGradientRect(0, 0, width, height, -1072689136, -804253680);

            if(event.mouseX < width*neu.overlay.getWidthMult()/3 || event.mouseX > width-width*neu.overlay.getWidthMult()/3) {
                neu.manager.auctionManager.customAH.drawScreen(event.mouseX, event.mouseY);
                neu.overlay.render(event.mouseX, event.mouseY, false);
            } else {
                neu.overlay.render(event.mouseX, event.mouseY, false);
                neu.manager.auctionManager.customAH.drawScreen(event.mouseX, event.mouseY);
            }
        }
    }

    private static boolean shouldRenderOverlay(Gui gui) {
        boolean validGui = gui instanceof GuiContainer || gui instanceof GuiItemRecipe;
        if(gui instanceof GuiChest) {
            GuiChest eventGui = (GuiChest) gui;
            ContainerChest cc = (ContainerChest) eventGui.inventorySlots;
            String containerName = cc.getLowerChestInventory().getDisplayName().getUnformattedText();
            if(containerName.trim().equals("Fast Travel")) {
                validGui = false;
            }
        }
        return validGui;
    }

    /**
     * Will draw the NEUOverlay over the inventory if focusInv == false. (z-translation of 300 is so that NEUOverlay
     * will draw over Items in the inventory (which render at a z value of about 250))
     * @param event
     */
    @SubscribeEvent
    public void onGuiScreenDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if(!(event.gui instanceof CustomAHGui || neu.manager.auctionManager.customAH.isRenderOverAuctionView())) {
            if(shouldRenderOverlay(event.gui) && neu.isOnSkyblock()) {
                if(!focusInv) {
                    GL11.glTranslatef(0, 0, 300);
                    neu.overlay.render(event.mouseX, event.mouseY, hoverInv && focusInv);
                    GL11.glTranslatef(0, 0, -300);
                }
                neu.overlay.renderOverlay(event.mouseX, event.mouseY);
            }
        }
    }

    private void renderDungeonChestOverlay(GuiScreen gui) {
        if(gui instanceof GuiChest && neu.manager.auctionManager.activeAuctions > 0) {
            try {
                int xSize = (int) Utils.getField(GuiContainer.class, gui, "xSize", "field_146999_f");
                int ySize = (int) Utils.getField(GuiContainer.class, gui, "ySize", "field_147000_g");
                int guiLeft = (int) Utils.getField(GuiContainer.class, gui, "guiLeft", "field_147003_i");
                int guiTop = (int) Utils.getField(GuiContainer.class, gui, "guiTop", "field_147009_r");

                GuiChest eventGui = (GuiChest) gui;
                ContainerChest cc = (ContainerChest) eventGui.inventorySlots;
                IInventory lower = cc.getLowerChestInventory();

                ItemStack rewardChest = lower.getStackInSlot(31);
                if (rewardChest != null && rewardChest.getDisplayName().endsWith(EnumChatFormatting.GREEN+"Open Reward Chest")) {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(dungeon_chest_worth);
                    GL11.glColor4f(1, 1, 1, 1);
                    GlStateManager.disableLighting();
                    Utils.drawTexturedRect(guiLeft+xSize+4, guiTop, 180, 45, 0, 180/256f, 0, 45/256f, GL11.GL_NEAREST);

                    int chestCost = 0;
                    String line6 = Utils.cleanColour(neu.manager.getLoreFromNBT(rewardChest.getTagCompound())[6]);
                    StringBuilder cost = new StringBuilder();
                    for(int i=0; i<line6.length(); i++) {
                        char c = line6.charAt(i);
                        if("0123456789".indexOf(c) >= 0) {
                            cost.append(c);
                        }
                    }
                    if(cost.length() > 0) {
                        chestCost = Integer.parseInt(cost.toString());
                    }

                    boolean missing = false;
                    int totalValue = 0;
                    for(int i=0; i<5; i++) {
                        ItemStack item = lower.getStackInSlot(11+i);
                        String internal = neu.manager.getInternalNameForItem(item);
                        if(internal != null) {
                            float worth = neu.manager.auctionManager.getLowestBin(internal);

                            if(worth == -1) worth = neu.manager.getCraftCost(internal).craftCost;

                            if(worth > 0) {
                                totalValue += worth;
                            } else {
                                missing = true;
                                break;
                            }
                        }
                    }
                    int profitLoss = totalValue - chestCost;

                    NumberFormat format = NumberFormat.getInstance(Locale.US);
                    String valueString;
                    if(!missing) {
                        valueString = EnumChatFormatting.BLUE+"Chest value: " + EnumChatFormatting.GOLD
                                + EnumChatFormatting.BOLD + format.format(totalValue) + " coins";
                    } else {
                        valueString = EnumChatFormatting.BLUE+"Couldn't find item on AH. Item is very rare!";
                    }
                    String plString;
                    if(missing) {
                        plString = "";
                    } else if(profitLoss >= 0) {
                        plString = EnumChatFormatting.BLUE+"Profit/loss: " + EnumChatFormatting.DARK_GREEN
                                + EnumChatFormatting.BOLD + "+" + format.format(profitLoss) + " coins";
                    } else {
                        plString = EnumChatFormatting.BLUE+"Profit/loss: " + EnumChatFormatting.RED
                                + EnumChatFormatting.BOLD + "-" + format.format(-profitLoss) + " coins";
                    }

                    Minecraft.getMinecraft().getTextureManager().bindTexture(dungeon_chest_worth);
                    GL11.glColor4f(1, 1, 1, 1);
                    GlStateManager.disableLighting();
                    Utils.drawTexturedRect(guiLeft+xSize+4, guiTop, 180, 45, 0, 180/256f, 0, 45/256f, GL11.GL_NEAREST);

                    Utils.drawStringCenteredScaledMaxWidth(valueString, Minecraft.getMinecraft().fontRendererObj, guiLeft+xSize+4+90,
                            guiTop+14, true, 170, Color.BLACK.getRGB());
                    Utils.drawStringCenteredScaledMaxWidth(plString, Minecraft.getMinecraft().fontRendererObj, guiLeft+xSize+4+90,
                            guiTop+28, true, 170, Color.BLACK.getRGB());
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a mouse event to NEUOverlay if the inventory isn't hovered AND focused.
     * Will also cancel the event if if NEUOverlay#mouseInput returns true.
     * @param event
     */
    @SubscribeEvent
    public void onGuiScreenMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if(event.gui instanceof CustomAHGui || neu.manager.auctionManager.customAH.isRenderOverAuctionView()) {
            event.setCanceled(true);
            neu.manager.auctionManager.customAH.handleMouseInput();
            neu.overlay.mouseInput();
            return;
        }
        if(shouldRenderOverlay(event.gui) && !(hoverInv && focusInv) && neu.isOnSkyblock()) {
            if(neu.overlay.mouseInput()) {
                event.setCanceled(true);
            }
        }
    }

    ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);

    /**
     * Sends a kbd event to NEUOverlay, cancelling if NEUOverlay#keyboardInput returns true.
     * Also includes a dev function used for creating custom named json files with recipes.
     */
    @SubscribeEvent
    public void onGuiScreenKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if(event.gui instanceof CustomAHGui || neu.manager.auctionManager.customAH.isRenderOverAuctionView()) {
            if(neu.manager.auctionManager.customAH.keyboardInput()) {
                event.setCanceled(true);
                Minecraft.getMinecraft().dispatchKeypresses();
            } else if(neu.overlay.keyboardInput(focusInv)) {
                event.setCanceled(true);
            }
            return;
        }

        if(shouldRenderOverlay(event.gui) && neu.isOnSkyblock()) {
            if(neu.overlay.keyboardInput(focusInv)) {
                event.setCanceled(true);
            }
        }
        if(neu.manager.config.dev.value && neu.manager.config.enableItemEditing.value && Minecraft.getMinecraft().theWorld != null &&
                Keyboard.getEventKey() == Keyboard.KEY_O && Keyboard.getEventKeyState()) {
            GuiScreen gui = Minecraft.getMinecraft().currentScreen;
            if(gui instanceof GuiChest) {
                GuiChest eventGui = (GuiChest) event.gui;
                ContainerChest cc = (ContainerChest) eventGui.inventorySlots;
                IInventory lower = cc.getLowerChestInventory();

                if(lower.getStackInSlot(23) != null &&
                        lower.getStackInSlot(23).getDisplayName().endsWith("Crafting Table")) {
                    ItemStack res = lower.getStackInSlot(25);
                    String resInternalname = neu.manager.getInternalNameForItem(res);
                    JTextField tf = new JTextField();
                    tf.setText(resInternalname);
                    tf.addAncestorListener(new RequestFocusListener());
                    JOptionPane.showOptionDialog(null,
                            tf,
                            "Enter Name:",
                            JOptionPane.NO_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null, new String[]{"Enter"}, "Enter");
                    resInternalname = tf.getText();
                    if(resInternalname.trim().length() == 0) {
                        return;
                    }

                    JsonObject recipe = new JsonObject();

                    String[] x = {"1","2","3"};
                    String[] y = {"A","B","C"};

                    for(int i=0; i<=18; i+=9) {
                        for(int j=0; j<3; j++) {
                            ItemStack stack = lower.getStackInSlot(10+i+j);
                            String internalname = "";
                            if(stack != null) {
                                internalname = neu.manager.getInternalNameForItem(stack);
                                if(!neu.manager.getItemInformation().containsKey(internalname)) {
                                    neu.manager.writeItemToFile(stack);
                                }
                                internalname += ":"+stack.stackSize;
                            }
                            recipe.addProperty(y[i/9]+x[j], internalname);
                        }
                    }

                    JsonObject json = neu.manager.getJsonForItem(res);
                    json.add("recipe", recipe);
                    json.addProperty("internalname", resInternalname);
                    json.addProperty("clickcommand", "viewrecipe");
                    json.addProperty("modver", NotEnoughUpdates.VERSION);
                    try {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("Added: " + resInternalname));
                        neu.manager.writeJsonDefaultDir(json, resInternalname+".json");
                        neu.manager.loadItem(resInternalname);
                    } catch(IOException e) {}
                }
            }
        }
        /*if(Minecraft.getMinecraft().theWorld != null && Keyboard.getEventKey() == Keyboard.KEY_RBRACKET && Keyboard.getEventKeyState()) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            started = true;
            final Object[] items = neu.manager.getItemInformation().values().toArray();
            AtomicInteger i = new AtomicInteger(0);

            Runnable checker = new Runnable() {
                @Override
                public void run() {
                    int in = i.getAndIncrement();
                    /*if(missingRecipe.get()) {
                        String internalname = ((JsonObject)items[in]).get("internalname").getAsString();

                        JsonArray arr = null;
                        File f = new File(neu.manager.configLocation, "missing.json");
                        try(InputStream instream = new FileInputStream(f)) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(instream, StandardCharsets.UTF_8));
                            JsonObject json = neu.manager.gson.fromJson(reader, JsonObject.class);
                            arr = json.getAsJsonArray("missing");
                        } catch(IOException e) {}

                        try {
                            JsonObject json = new JsonObject();
                            if(arr == null) arr = new JsonArray();
                            arr.add(new JsonPrimitive(internalname));
                            json.add("missing", arr);
                            neu.manager.writeJson(json, f);
                        } catch(IOException e) {}
                    }
                    missingRecipe.set(false);

                    ses.schedule(() -> {
                        int index = i.get();
                        JsonObject o = (JsonObject)items[index];
                        if(Minecraft.getMinecraft().currentScreen != null) {
                            Minecraft.getMinecraft().displayGuiScreen(null);
                        }
                        Minecraft.getMinecraft().thePlayer.sendChatMessage("/viewrecipe " + o.get("internalname").getAsString());

                        ses.schedule(this, 1000, TimeUnit.MILLISECONDS);
                    }, 100, TimeUnit.MILLISECONDS);
                }
            };

            int index = i.get();
            JsonObject o = (JsonObject)items[index];
            if(Minecraft.getMinecraft().currentScreen != null) {
                Minecraft.getMinecraft().displayGuiScreen(null);
            }
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/viewrecipe " + o.get("internalname").getAsString());

            ses.schedule(checker, 1000, TimeUnit.MILLISECONDS);
        }*/
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onItemTooltipLow(ItemTooltipEvent event) {
        //NotEnoughUpdates.INSTANCE.neu.manager.config.enchantColours.value
        int index = 0;
        List<String> newTooltip = new ArrayList<>();
        for(String line : event.toolTip) {
            for(String op : neu.manager.config.enchantColours.value) {
                List<String> colourOps = GuiEnchantColour.splitter.splitToList(op);
                String enchantName = GuiEnchantColour.getColourOpIndex(colourOps, 0);
                String comparator = GuiEnchantColour.getColourOpIndex(colourOps, 1);
                String comparison = GuiEnchantColour.getColourOpIndex(colourOps, 2);
                String colourCode = GuiEnchantColour.getColourOpIndex(colourOps, 3);

                if(enchantName.length() == 0) continue;
                if(comparator.length() == 0) continue;
                if(comparison.length() == 0) continue;
                if(colourCode.length() == 0) continue;

                if(enchantName.contains("(") || enchantName.contains(")")) continue;

                int comparatorI = ">=<".indexOf(comparator.charAt(0));

                int levelToFind = -1;
                try {
                    levelToFind = Integer.parseInt(comparison);
                } catch(Exception e) { continue; }

                if(comparatorI < 0) continue;
                if("0123456789abcdefz".indexOf(colourCode.charAt(0)) < 0) continue;

                //item_lore = item_lore.replaceAll("\\u00A79("+lvl4Max+" IV)", EnumChatFormatting.DARK_PURPLE+"$1");
                //9([a-zA-Z ]+?) ([0-9]+|(I|II|III|IV|V|VI|VII|VIII|IX|X))(,|$)
                Pattern pattern;
                try {
                    String prefix = "\u00A79";
                    if(enchantName.startsWith("ULT_")) prefix = "\u00A7l\u00A7d";
                    pattern = Pattern.compile(prefix+"("+enchantName+") ([0-9]+|(I|II|III|IV|V|VI|VII|VIII|IX|X))(,|$)");
                } catch(Exception e) {continue;} //malformed regex
                Matcher matcher = pattern.matcher(line);
                int matchCount = 0;
                while(matcher.find() && matchCount < 5) {
                    matchCount++;
                    int level = -1;
                    String levelStr = matcher.group(2);
                    if(levelStr == null) continue;
                    try {
                        level = Integer.parseInt(levelStr);
                    } catch(Exception e) {
                        switch(levelStr) {
                            case "I":
                                level = 1; break;
                            case "II":
                                level = 2; break;
                            case "III":
                                level = 3; break;
                            case "IV":
                                level = 4; break;
                            case "V":
                                level = 5; break;
                            case "VI":
                                level = 6; break;
                            case "VII":
                                level = 7; break;
                            case "VIII":
                                level = 8; break;
                            case "IX":
                                level = 9; break;
                            case "X":
                                level = 10; break;
                        }
                    }
                    boolean matches = false;
                    if(level > 0) {
                        switch(comparator) {
                            case ">":
                                matches = level > levelToFind; break;
                            case "=":
                                matches = level == levelToFind; break;
                            case "<":
                                matches = level < levelToFind; break;
                        }
                    }
                    if(matches) {
                        if(!colourCode.equals("z")) {
                            line = line.replaceAll("\\u00A79"+matcher.group(1), "\u00A7"+colourCode+matcher.group(1));
                        } else {
                            line = line.replaceAll("\\u00A79"+matcher.group(1), Utils.chromaString(matcher.group(1)));
                        }
                    }
                }
            }
            newTooltip.add(line);
        }
        event.toolTip.clear();
        event.toolTip.addAll(newTooltip);
    }

    /**
     * This makes it so that holding LCONTROL while hovering over an item with NBT will show the NBT of the item.
     * @param event
     */
    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if(!neu.isOnSkyblock()) return;
        if(neu.manager.config.hideEmptyPanes.value &&
                event.itemStack.getItem().equals(Item.getItemFromBlock(Blocks.stained_glass_pane))) {
            String first = Utils.cleanColour(event.toolTip.get(0));
            first = first.replaceAll("\\(.*\\)", "").trim();
            if(first.length() == 0) {
                event.toolTip.clear();
            }
        }
        //AH prices
        /*if(Minecraft.getMinecraft().currentScreen != null) {
            if(Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
                GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;
                ContainerChest container = (ContainerChest) chest.inventorySlots;
                String containerName = container.getLowerChestInventory().getDisplayName().getUnformattedText();
                if(containerName.trim().equals("Auctions Browser")) {
                    String internalname = neu.manager.getInternalNameForItem(event.itemStack);
                    if(internalname != null) {
                        for(int i=0; i<event.toolTip.size(); i++) {
                            String line = event.toolTip.get(i);
                            if(line.contains(EnumChatFormatting.GRAY + "Bidder: ") ||
                                    line.contains(EnumChatFormatting.GRAY + "Starting bid: ") ||
                                    line.contains(EnumChatFormatting.GRAY + "Buy it now: ")) {
                                neu.manager.updatePrices();
                                JsonObject auctionInfo = neu.manager.getItemAuctionInfo(internalname);

                                if(auctionInfo != null) {
                                    NumberFormat format = NumberFormat.getInstance(Locale.US);
                                    int auctionPrice = (int)(auctionInfo.get("price").getAsFloat() / auctionInfo.get("count").getAsFloat());
                                    float costOfEnchants = neu.manager.getCostOfEnchants(internalname,
                                            event.itemStack.getTagCompound());
                                    int priceWithEnchants = auctionPrice+(int)costOfEnchants;

                                    event.toolTip.add(++i, EnumChatFormatting.GRAY + "Average price: " +
                                            EnumChatFormatting.GOLD + format.format(auctionPrice) + " coins");
                                    if(costOfEnchants > 0) {
                                        event.toolTip.add(++i, EnumChatFormatting.GRAY + "Average price (w/ enchants): " +
                                                EnumChatFormatting.GOLD +
                                                format.format(priceWithEnchants) + " coins");
                                    }

                                    if(neu.manager.config.advancedPriceInfo.value) {
                                        int salesVolume = (int) auctionInfo.get("sales").getAsFloat();
                                        int flipPrice = (int)(0.93*priceWithEnchants);

                                        event.toolTip.add(++i, EnumChatFormatting.GRAY + "Flip Price (93%): " +
                                                EnumChatFormatting.GOLD + format.format(flipPrice) + " coins");
                                        event.toolTip.add(++i, EnumChatFormatting.GRAY + "Volume: " +
                                                EnumChatFormatting.GOLD + format.format(salesVolume) + " sales/day");
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }*/
        if(!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || !neu.manager.config.dev.value) return;
        if(event.toolTip.size()>0&&event.toolTip.get(event.toolTip.size()-1).startsWith(EnumChatFormatting.DARK_GRAY + "NBT: ")) {
            event.toolTip.remove(event.toolTip.size()-1);

            StringBuilder sb = new StringBuilder();
            String nbt = event.itemStack.getTagCompound().toString();
            int indent = 0;
            for(char c : nbt.toCharArray()) {
                boolean newline = false;
                if(c == '{' || c == '[') {
                    indent++;
                    newline = true;
                } else if(c == '}' || c == ']') {
                    indent--;
                    sb.append("\n");
                    for(int i=0; i<indent; i++) sb.append("  ");
                } else if(c == ',') {
                    newline = true;
                } else if(c == '\"') {
                    sb.append(EnumChatFormatting.RESET.toString() + EnumChatFormatting.GRAY);
                }

                sb.append(c);
                if(newline) {
                    sb.append("\n");
                    for(int i=0; i<indent; i++) sb.append("  ");
                }
            }
            event.toolTip.add(sb.toString());
            if(Keyboard.isKeyDown(Keyboard.KEY_H)) {
                StringSelection selection = new StringSelection(sb.toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            }
        }
    }
}
