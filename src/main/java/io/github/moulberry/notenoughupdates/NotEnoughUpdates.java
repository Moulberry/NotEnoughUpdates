package io.github.moulberry.notenoughupdates;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import io.github.moulberry.notenoughupdates.auction.CustomAHGui;
import io.github.moulberry.notenoughupdates.commands.SimpleCommand;
import io.github.moulberry.notenoughupdates.cosmetics.CapeManager;
import io.github.moulberry.notenoughupdates.infopanes.CollectionLogInfoPane;
import io.github.moulberry.notenoughupdates.infopanes.CosmeticsInfoPane;
import io.github.moulberry.notenoughupdates.questing.GuiQuestLine;
import io.github.moulberry.notenoughupdates.questing.NEUQuesting;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Session;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.Proxy;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.moulberry.notenoughupdates.GuiTextures.*;

@Mod(modid = NotEnoughUpdates.MODID, version = NotEnoughUpdates.VERSION)
public class NotEnoughUpdates {
    public static final String MODID = "notenoughupdates";
    public static final String VERSION = "REL-1.0.0";

    public static NotEnoughUpdates INSTANCE = null;

    public NEUManager manager;
    public NEUOverlay overlay;
    private NEUIO neuio;

    private static final long CHAT_MSG_COOLDOWN = 200;
    private long lastChatMessage = 0;
    private long secondLastChatMessage = 0;
    private String currChatMessage = null;

    private boolean hoverInv = false;
    private boolean focusInv = false;

    private boolean joinedSB = false;

    //Stolen from Biscut and used for detecting whether in skyblock
    private static final Set<String> SKYBLOCK_IN_ALL_LANGUAGES = Sets.newHashSet("SKYBLOCK","\u7A7A\u5C9B\u751F\u5B58");

    //Github Access Token, may change. Value hard-coded.
    //Token is obfuscated so that github doesn't delete it whenever I upload the jar.
    String[] token = new String[]{"b292496d2c","9146a","9f55d0868a545305a8","96344bf"};
    private String getAccessToken() {
        String s = "";
        for(String str : token) {
            s += str;
        }
        return s;
    }

    private GuiScreen openGui = null;

    SimpleCommand collectionLogCommand = new SimpleCommand("neucl", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if(!OpenGlHelper.isFramebufferEnabled()) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED +
                        "This feature requires FBOs to work. Try disabling Optifine's 'Fast Render'."));
            } else {
                if(!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
                    openGui = new GuiInventory(Minecraft.getMinecraft().thePlayer);
                }
                manager.updatePrices();
                overlay.displayInformationPane(new CollectionLogInfoPane(overlay, manager));
            }
        }
    });


    SimpleCommand questingCommand = new SimpleCommand("neuquest", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            openGui = new GuiQuestLine();
        }
    });

    SimpleCommand overlayPlacementsCommand = new SimpleCommand("neuoverlay", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            openGui = new NEUOverlayPlacements();
        }
    });

    SimpleCommand cosmeticsCommand = new SimpleCommand("neucosmetics", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if(!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
                openGui = new GuiInventory(Minecraft.getMinecraft().thePlayer);
            }
            overlay.displayInformationPane(new CosmeticsInfoPane(overlay, manager));
        }
    });

    SimpleCommand neuAhCommand = new SimpleCommand("neuah", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if(!hasSkyblockScoreboard()) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED+
                        "You must be on Skyblock to use this feature."));
            } else if(manager.config.apiKey.value == null || manager.config.apiKey.value.trim().isEmpty()) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED+
                        "Can't open NeuAH, Api Key is not set. Run /api new and put the result in settings."));
            } else {
                openGui = new CustomAHGui();
                manager.auctionManager.customAH.lastOpen = System.currentTimeMillis();
                manager.auctionManager.customAH.clearSearch();
                manager.auctionManager.customAH.updateSearch();
            }
        }
    });

    /**
     * Instantiates NEUIo, NEUManager and NEUOverlay instances. Registers keybinds and adds a shutdown hook to clear tmp folder.
     * @param event
     */
    @EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        INSTANCE = this;
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(CapeManager.getInstance());

        File f = new File(event.getModConfigurationDirectory(), "notenoughupdates");
        f.mkdirs();
        ClientCommandHandler.instance.registerCommand(collectionLogCommand);
        ClientCommandHandler.instance.registerCommand(cosmeticsCommand);
        ClientCommandHandler.instance.registerCommand(overlayPlacementsCommand);
        //ClientCommandHandler.instance.registerCommand(questingCommand);
        ClientCommandHandler.instance.registerCommand(neuAhCommand);

        neuio = new NEUIO(getAccessToken());
        manager = new NEUManager(this, neuio, f);
        manager.loadItemInformation();
        overlay = new NEUOverlay(manager);

        for(KeyBinding kb : manager.keybinds) {
            ClientRegistry.registerKeyBinding(kb);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                File tmp = new File(f, "tmp");
                if(tmp.exists()) {
                    for(File tmpFile : tmp.listFiles()) {
                        tmpFile.delete();
                    }
                    tmp.delete();
                }

                manager.saveConfig();
            } catch(IOException e) {}
        }));

        //TODO: login code. Ignore this, used for testing.
        /*try {
            Field field = Minecraft.class.getDeclaredField("session");
            YggdrasilUserAuthentication auth = (YggdrasilUserAuthentication)
                    new YggdrasilAuthenticationService(Proxy.NO_PROXY, UUID.randomUUID().toString())
                            .createUserAuthentication(Agent.MINECRAFT);
            auth.setUsername("james.jenour@protonmail.com");
            JPasswordField pf = new JPasswordField();
            JOptionPane.showConfirmDialog(null,
                    pf,
                    "Enter password:",
                    JOptionPane.NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            auth.setPassword(new String(pf.getPassword()));
            System.out.print("Attempting login...");

            auth.logIn();

            Session session = new Session(auth.getSelectedProfile().getName(),
                    auth.getSelectedProfile().getId().toString().replace("-", ""),
                    auth.getAuthenticatedToken(),
                    auth.getUserType().getName());

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.setAccessible(true);
            field.set(Minecraft.getMinecraft(), session);
        } catch (NoSuchFieldException | AuthenticationException | IllegalAccessException e) {
        }*/
    }

    /**
     * If the last chat messages was sent >200ms ago, sends the message.
     * If the last chat message was sent <200 ago, will cache the message for #onTick to handle.
     */
    public void sendChatMessage(String message) {
        if(System.currentTimeMillis() - lastChatMessage > CHAT_MSG_COOLDOWN)  {
            secondLastChatMessage = lastChatMessage;
            lastChatMessage = System.currentTimeMillis();
            Minecraft.getMinecraft().thePlayer.sendChatMessage(message);
            currChatMessage = null;
        } else {
            currChatMessage = message;
        }
    }

    /**
     * 1)Will send the cached message from #sendChatMessage when at least 200ms has passed since the last message.
     * This is used in order to prevent the mod spamming messages.
     * 2)Adds unique items to the collection log
     */
    private HashMap<String, Long> newItemAddMap = new HashMap<>();
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if(openGui != null) {
            Minecraft.getMinecraft().displayGuiScreen(openGui);
            openGui = null;
        }
        if(hasSkyblockScoreboard()) {
            manager.auctionManager.tick();
            if(!joinedSB && manager.config.showUpdateMsg.value) {
                File repo = manager.repoLocation;
                if(repo.exists()) {
                    File updateJson = new File(repo, "update.json");
                    try {
                        JsonObject o = manager.getJsonFromFile(updateJson);

                        String version = o.get("version").getAsString();

                        if(!VERSION.equalsIgnoreCase(version)) {
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
                            ChatComponentText other = null;
                            if(other_text.length() > 0) {
                                other = new ChatComponentText(EnumChatFormatting.GRAY+"["+EnumChatFormatting.BLUE+other_text+EnumChatFormatting.GRAY+"]");
                                other.setChatStyle(Utils.createClickStyle(ClickEvent.Action.OPEN_URL, other_link));
                            }
                            ChatComponentText links = new ChatComponentText("");
                            ChatComponentText separator = new ChatComponentText(
                                    EnumChatFormatting.GRAY+EnumChatFormatting.BOLD.toString()+EnumChatFormatting.STRIKETHROUGH+(other==null?"---":"--"));
                            ChatComponentText discord = new ChatComponentText(EnumChatFormatting.GRAY+"["+EnumChatFormatting.BLUE+"Discord"+EnumChatFormatting.GRAY+"]");
                            discord.setChatStyle(Utils.createClickStyle(ClickEvent.Action.OPEN_URL, discord_link));
                            ChatComponentText youtube = new ChatComponentText(EnumChatFormatting.GRAY+"["+EnumChatFormatting.RED+"YouTube"+EnumChatFormatting.GRAY+"]");
                            youtube.setChatStyle(Utils.createClickStyle(ClickEvent.Action.OPEN_URL, youtube_link));
                            ChatComponentText release = new ChatComponentText(EnumChatFormatting.GRAY+"["+EnumChatFormatting.GREEN+"Release"+EnumChatFormatting.GRAY+"]");
                            release.setChatStyle(Utils.createClickStyle(ClickEvent.Action.OPEN_URL, update_link));
                            ChatComponentText github = new ChatComponentText(EnumChatFormatting.GRAY+"["+EnumChatFormatting.DARK_PURPLE+"GitHub"+EnumChatFormatting.GRAY+"]");
                            github.setChatStyle(Utils.createClickStyle(ClickEvent.Action.OPEN_URL, github_link));


                            links.appendSibling(separator);
                            links.appendSibling(discord);
                            links.appendSibling(separator);
                            links.appendSibling(youtube);
                            links.appendSibling(separator);
                            links.appendSibling(release);
                            links.appendSibling(separator);
                            links.appendSibling(github);
                            links.appendSibling(separator);
                            if(other != null) {
                                links.appendSibling(other);
                                links.appendSibling(separator);
                            }

                            Minecraft.getMinecraft().thePlayer.addChatMessage(links);
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(""));

                        }

                        joinedSB = true;
                    } catch(Exception ignored) {}
                }
            }
            //NEUQuesting.getInstance().tick();
            //GuiQuestLine.questLine.tick();
        }
        if(currChatMessage != null && System.currentTimeMillis() - lastChatMessage > CHAT_MSG_COOLDOWN) {
            lastChatMessage = System.currentTimeMillis();
            Minecraft.getMinecraft().thePlayer.sendChatMessage(currChatMessage);
            currChatMessage = null;
        }
        if(hasSkyblockScoreboard() && manager.getCurrentProfile() != null && manager.getCurrentProfile().length() > 0) {
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

                        if(containerName.equals("Accessory Bag")) {
                            usableContainer = true;
                        }
                    }
                }
                if(usableContainer) {
                    for(ItemStack stack : Minecraft.getMinecraft().thePlayer.openContainer.getInventory()) {
                        processUniqueStack(stack, newItem);
                    }
                    for(ItemStack stack : Minecraft.getMinecraft().thePlayer.inventory.mainInventory) {
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

    private void processUniqueStack(ItemStack stack, HashSet<String> newItem) {
        if(stack != null && stack.hasTagCompound()) {
            String internalname = manager.getInternalNameForItem(stack);
            if(internalname != null) {
                ArrayList<String> log = manager.config.collectionLog.value.computeIfAbsent(
                        manager.getCurrentProfile(), k -> new ArrayList<>());
                if(!log.contains(internalname)) {
                    newItem.add(internalname);
                    if(newItemAddMap.containsKey(internalname)) {
                        if(System.currentTimeMillis() - newItemAddMap.get(internalname) > 1000) {
                            log.add(internalname);
                        }
                    } else {
                        newItemAddMap.put(internalname, System.currentTimeMillis());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent event) {
        if(event.type != null && event.type.equals(RenderGameOverlayEvent.ElementType.BOSSHEALTH) &&
                Minecraft.getMinecraft().currentScreen instanceof GuiContainer && overlay.isUsingMobsFilter()) {
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
        manager.auctionManager.customAH.lastGuiScreenSwitch = System.currentTimeMillis();

        if(event.gui == null && manager.auctionManager.customAH.isRenderOverAuctionView() &&
                !(Minecraft.getMinecraft().currentScreen instanceof CustomAHGui)) {
            event.gui = new CustomAHGui();
        }

        if(!(event.gui instanceof GuiChest || event.gui instanceof GuiEditSign)) {
           manager.auctionManager.customAH.setRenderOverAuctionView(false);
        } else if(event.gui instanceof GuiChest && (manager.auctionManager.customAH.isRenderOverAuctionView() ||
                Minecraft.getMinecraft().currentScreen instanceof CustomAHGui)){
            GuiChest chest = (GuiChest) event.gui;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            String containerName = container.getLowerChestInventory().getDisplayName().getUnformattedText();

            manager.auctionManager.customAH.setRenderOverAuctionView(containerName.trim().equals("Auction View") ||
                    containerName.trim().equals("BIN Auction View") || containerName.trim().equals("Confirm Bid"));
        }

        //OPEN
        if(Minecraft.getMinecraft().currentScreen == null
                && event.gui instanceof GuiContainer) {
            overlay.reset();
            manager.loadConfig();
        }
        //CLOSE
        if(Minecraft.getMinecraft().currentScreen instanceof GuiContainer
                && event.gui == null) {
            try {
                manager.saveConfig();
            } catch(IOException e) {}
        }
        if(event.gui != null && manager.config.dev.value) {
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
                            String resInternalname = manager.getInternalNameForItem(res);

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

                                        JsonObject json = manager.getItemInformation().get(resInternalname);
                                        json.addProperty("crafttext", "Requires: " + col);

                                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("Added: " + resInternalname));
                                        manager.writeJsonDefaultDir(json, resInternalname+".json");
                                        manager.loadItem(resInternalname);
                                    }
                                }
                            }

                            /*JsonArray arr = null;
                            File f = new File(manager.configLocation, "missing.json");
                            try(InputStream instream = new FileInputStream(f)) {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(instream, StandardCharsets.UTF_8));
                                JsonObject json = manager.gson.fromJson(reader, JsonObject.class);
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
                                manager.writeJson(json, f);
                            } catch(IOException e) {}*/



                            /*JsonObject recipe = new JsonObject();

                            String[] x = {"1","2","3"};
                            String[] y = {"A","B","C"};

                            for(int i=0; i<=18; i+=9) {
                                for(int j=0; j<3; j++) {
                                    ItemStack stack = lower.getStackInSlot(10+i+j);
                                    String internalname = "";
                                    if(stack != null) {
                                        internalname = manager.getInternalNameForItem(stack);
                                        if(!manager.getItemInformation().containsKey(internalname)) {
                                            manager.writeItemToFile(stack);
                                        }
                                        internalname += ":"+stack.stackSize;
                                    }
                                    recipe.addProperty(y[i/9]+x[j], internalname);
                                }
                            }

                            JsonObject json = manager.getJsonForItem(res);
                            json.add("recipe", recipe);
                            json.addProperty("internalname", resInternalname);
                            json.addProperty("clickcommand", "viewrecipe");
                            json.addProperty("modver", NotEnoughUpdates.VERSION);

                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("Added: " + resInternalname));
                            manager.writeJsonDefaultDir(json, resInternalname+".json");
                            manager.loadItem(resInternalname);*/
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
            manager.setCurrentProfile(unformatted.substring("You are playing on profile: ".length()).split(" ")[0].trim());
        } else if(unformatted.startsWith("Your profile was changed to: ")) {//Your profile was changed to:
            manager.setCurrentProfile(unformatted.substring("Your profile was changed to: ".length()).split(" ")[0].trim());
        } else if(unformatted.startsWith("Your new API key is ")) {
            manager.config.apiKey.value = unformatted.substring("Your new API key is ".length());
            try { manager.saveConfig(); } catch(IOException ioe) {}
        }
        if(e.message.getFormattedText().equals(EnumChatFormatting.RESET.toString()+
                EnumChatFormatting.RED+"You haven't unlocked this recipe!"+EnumChatFormatting.RESET)) {
            r =  EnumChatFormatting.RED+"You haven't unlocked this recipe!";
        } else if(e.message.getFormattedText().startsWith(EnumChatFormatting.RESET.toString()+
                EnumChatFormatting.RED+"Invalid recipe ")) {
            r = "";
        }
        if(r != null) {
            if(manager.failViewItem(r)) {
                e.setCanceled(true);
            }
            missingRecipe.set(true);
        }
        //System.out.println(e.message);
        if(isOnSkyblock() && manager.config.streamerMode.value && e.message instanceof ChatComponentText) {
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
        if((event.gui instanceof GuiContainer || event.gui instanceof CustomAHGui) && isOnSkyblock()) {
            ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
            int width = scaledresolution.getScaledWidth();

            boolean hoverPane = event.getMouseX() < width*overlay.getInfoPaneOffsetFactor() ||
                    event.getMouseX() > width*overlay.getItemPaneOffsetFactor();

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
                if(focusInv) {
                    try {
                        overlay.render(event.getMouseX(), event.getMouseY(), hoverInv && focusInv);
                    } catch(ConcurrentModificationException e) {e.printStackTrace();}
                    GL11.glTranslatef(0, 0, 10);
                }
            }
        }
    }

    @SubscribeEvent
    public void onGuiScreenDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if(event.gui instanceof CustomAHGui || manager.auctionManager.customAH.isRenderOverAuctionView()) {
            event.setCanceled(true);

            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            int width = scaledResolution.getScaledWidth();
            int height = scaledResolution.getScaledHeight();

            //Dark background
            Utils.drawGradientRect(0, 0, width, height, -1072689136, -804253680);

            if(event.mouseX < width*overlay.getWidthMult()/3 || event.mouseX > width-width*overlay.getWidthMult()/3) {
                manager.auctionManager.customAH.drawScreen(event.mouseX, event.mouseY);
                overlay.render(event.mouseX, event.mouseY, false);
            } else {
                overlay.render(event.mouseX, event.mouseY, false);
                manager.auctionManager.customAH.drawScreen(event.mouseX, event.mouseY);
            }

        }
    }

    /**
     * Will draw the NEUOverlay over the inventory if focusInv == false. (z-translation of 300 is so that NEUOverlay
     * will draw over Items in the inventory (which render at a z value of about 250))
     * @param event
     */
    @SubscribeEvent
    public void onGuiScreenDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if(!(event.gui instanceof CustomAHGui || manager.auctionManager.customAH.isRenderOverAuctionView())) {
            if(event.gui instanceof GuiContainer && isOnSkyblock()) {

                renderDungeonChestOverlay(event.gui);

                if(!focusInv) {
                    GL11.glTranslatef(0, 0, 300);
                    overlay.render(event.mouseX, event.mouseY, hoverInv && focusInv);
                    GL11.glTranslatef(0, 0, -300);
                }
                overlay.renderOverlay(event.mouseX, event.mouseY);
            }
        }
    }

    private void renderDungeonChestOverlay(GuiScreen gui) {
        if(gui instanceof GuiChest && manager.auctionManager.activeAuctions > 0) {
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
                    int chestCost = 0;
                    String line6 = Utils.cleanColour(manager.getLoreFromNBT(rewardChest.getTagCompound())[6]);
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
                        String internal = manager.getInternalNameForItem(item);
                        if(internal != null) {
                            int worth = manager.auctionManager.getLowestBin(internal);
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
        if(event.gui instanceof CustomAHGui || manager.auctionManager.customAH.isRenderOverAuctionView()) {
            event.setCanceled(true);
            manager.auctionManager.customAH.handleMouseInput();
            overlay.mouseInput();
            return;
        }
        if(event.gui instanceof GuiContainer && !(hoverInv && focusInv) && isOnSkyblock()) {
            if(overlay.mouseInput()) {
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
        if(event.gui instanceof CustomAHGui || manager.auctionManager.customAH.isRenderOverAuctionView()) {
            if(manager.auctionManager.customAH.keyboardInput()) {
                event.setCanceled(true);
                Minecraft.getMinecraft().dispatchKeypresses();
            } else if(overlay.keyboardInput(focusInv)) {
                event.setCanceled(true);
            }
            return;
        }

        if(event.gui instanceof GuiContainer && isOnSkyblock()) {
            if(overlay.keyboardInput(focusInv)) {
                event.setCanceled(true);
            }
        }
        if(manager.config.dev.value && manager.config.enableItemEditing.value && Minecraft.getMinecraft().theWorld != null &&
                Keyboard.getEventKey() == Keyboard.KEY_O && Keyboard.getEventKeyState()) {
            GuiScreen gui = Minecraft.getMinecraft().currentScreen;
            if(gui instanceof GuiChest) {
                GuiChest eventGui = (GuiChest) event.gui;
                ContainerChest cc = (ContainerChest) eventGui.inventorySlots;
                IInventory lower = cc.getLowerChestInventory();

                if(lower.getStackInSlot(23) != null &&
                        lower.getStackInSlot(23).getDisplayName().endsWith("Crafting Table")) {
                    ItemStack res = lower.getStackInSlot(25);
                    String resInternalname = manager.getInternalNameForItem(res);
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
                                internalname = manager.getInternalNameForItem(stack);
                                if(!manager.getItemInformation().containsKey(internalname)) {
                                    manager.writeItemToFile(stack);
                                }
                                internalname += ":"+stack.stackSize;
                            }
                            recipe.addProperty(y[i/9]+x[j], internalname);
                        }
                    }

                    JsonObject json = manager.getJsonForItem(res);
                    json.add("recipe", recipe);
                    json.addProperty("internalname", resInternalname);
                    json.addProperty("clickcommand", "viewrecipe");
                    json.addProperty("modver", NotEnoughUpdates.VERSION);
                    try {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("Added: " + resInternalname));
                        manager.writeJsonDefaultDir(json, resInternalname+".json");
                        manager.loadItem(resInternalname);
                    } catch(IOException e) {}
                }
            }
        }
        /*if(Minecraft.getMinecraft().theWorld != null && Keyboard.getEventKey() == Keyboard.KEY_RBRACKET && Keyboard.getEventKeyState()) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            started = true;
            final Object[] items = manager.getItemInformation().values().toArray();
            AtomicInteger i = new AtomicInteger(0);

            Runnable checker = new Runnable() {
                @Override
                public void run() {
                    int in = i.getAndIncrement();
                    /*if(missingRecipe.get()) {
                        String internalname = ((JsonObject)items[in]).get("internalname").getAsString();

                        JsonArray arr = null;
                        File f = new File(manager.configLocation, "missing.json");
                        try(InputStream instream = new FileInputStream(f)) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(instream, StandardCharsets.UTF_8));
                            JsonObject json = manager.gson.fromJson(reader, JsonObject.class);
                            arr = json.getAsJsonArray("missing");
                        } catch(IOException e) {}

                        try {
                            JsonObject json = new JsonObject();
                            if(arr == null) arr = new JsonArray();
                            arr.add(new JsonPrimitive(internalname));
                            json.add("missing", arr);
                            manager.writeJson(json, f);
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

    /**
     * This makes it so that holding LCONTROL while hovering over an item with NBT will show the NBT of the item.
     * @param event
     */
    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if(!isOnSkyblock()) return;
        if(manager.config.hideEmptyPanes.value &&
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
                    String internalname = manager.getInternalNameForItem(event.itemStack);
                    if(internalname != null) {
                        for(int i=0; i<event.toolTip.size(); i++) {
                            String line = event.toolTip.get(i);
                            if(line.contains(EnumChatFormatting.GRAY + "Bidder: ") ||
                                    line.contains(EnumChatFormatting.GRAY + "Starting bid: ") ||
                                    line.contains(EnumChatFormatting.GRAY + "Buy it now: ")) {
                                manager.updatePrices();
                                JsonObject auctionInfo = manager.getItemAuctionInfo(internalname);

                                if(auctionInfo != null) {
                                    NumberFormat format = NumberFormat.getInstance(Locale.US);
                                    int auctionPrice = (int)(auctionInfo.get("price").getAsFloat() / auctionInfo.get("count").getAsFloat());
                                    float costOfEnchants = manager.getCostOfEnchants(internalname,
                                            event.itemStack.getTagCompound());
                                    int priceWithEnchants = auctionPrice+(int)costOfEnchants;

                                    event.toolTip.add(++i, EnumChatFormatting.GRAY + "Average price: " +
                                            EnumChatFormatting.GOLD + format.format(auctionPrice) + " coins");
                                    if(costOfEnchants > 0) {
                                        event.toolTip.add(++i, EnumChatFormatting.GRAY + "Average price (w/ enchants): " +
                                                EnumChatFormatting.GOLD +
                                                format.format(priceWithEnchants) + " coins");
                                    }

                                    if(manager.config.advancedPriceInfo.value) {
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
        if(!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || !manager.config.dev.value) return;
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

    //Stolen from Biscut's SkyblockAddons
    public boolean isOnSkyblock() {
        if(!manager.config.onlyShowOnSkyblock.value) return true;
        return hasSkyblockScoreboard();
    }

    public boolean hasSkyblockScoreboard() {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc != null && mc.theWorld != null) {
            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
            if (sidebarObjective != null) {
                String objectiveName = sidebarObjective.getDisplayName().replaceAll("(?i)\\u00A7.", "");
                for (String skyblock : SKYBLOCK_IN_ALL_LANGUAGES) {
                    if (objectiveName.startsWith(skyblock)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
