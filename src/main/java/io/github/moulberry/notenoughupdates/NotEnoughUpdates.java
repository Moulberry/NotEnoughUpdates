package io.github.moulberry.notenoughupdates;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.auction.CustomAHGui;
import io.github.moulberry.notenoughupdates.collectionlog.GuiCollectionLog;
import io.github.moulberry.notenoughupdates.commands.Commands;
import io.github.moulberry.notenoughupdates.commands.SimpleCommand;
import io.github.moulberry.notenoughupdates.core.BackgroundBlur;
import io.github.moulberry.notenoughupdates.core.GuiScreenElementWrapper;
import io.github.moulberry.notenoughupdates.core.config.GuiPositionEditor;
import io.github.moulberry.notenoughupdates.core.util.MiscUtils;
import io.github.moulberry.notenoughupdates.cosmetics.CapeManager;
import io.github.moulberry.notenoughupdates.cosmetics.GuiCosmetics;
import io.github.moulberry.notenoughupdates.dungeons.DungeonMap;
import io.github.moulberry.notenoughupdates.dungeons.DungeonWin;
import io.github.moulberry.notenoughupdates.dungeons.GuiDungeonMapEditor;
import io.github.moulberry.notenoughupdates.gamemodes.GuiGamemodes;
import io.github.moulberry.notenoughupdates.miscfeatures.*;
import io.github.moulberry.notenoughupdates.miscgui.*;
import io.github.moulberry.notenoughupdates.miscgui.tutorials.NeuTutorial;
import io.github.moulberry.notenoughupdates.options.NEUConfig;
import io.github.moulberry.notenoughupdates.options.NEUConfigEditor;
import io.github.moulberry.notenoughupdates.overlays.FuelBar;
import io.github.moulberry.notenoughupdates.overlays.OverlayManager;
import io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer;
import io.github.moulberry.notenoughupdates.profileviewer.PlayerStats;
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewer;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Constants;
import io.github.moulberry.notenoughupdates.util.Utils;
import io.github.moulberry.notenoughupdates.util.XPInformation;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.*;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Mod(modid = NotEnoughUpdates.MODID, version = NotEnoughUpdates.VERSION, clientSideOnly = true)
public class NotEnoughUpdates {
    public static final String MODID = "notenoughupdates";
    public static final String VERSION = "2.0.0-REL";
    public static final String PRE_VERSION = "30.2";
    public static final int VERSION_ID = 20000;
    public static final int PRE_VERSION_ID = 3002;

    public static NotEnoughUpdates INSTANCE = null;

    public NEUManager manager;
    public NEUOverlay overlay;
    public NEUConfig config;

    private File configFile;

    public File getConfigFile(){
        return this.configFile;
    }
    public void newConfigFile(){
        this.configFile = new File(NotEnoughUpdates.INSTANCE.getNeuDir(), "configNew.json");
    }

    private static final long CHAT_MSG_COOLDOWN = 200;
    private long lastChatMessage = 0;
    private long secondLastChatMessage = 0;
    private String currChatMessage = null;

    //Stolen from Biscut and used for detecting whether in skyblock
    private static final Set<String> SKYBLOCK_IN_ALL_LANGUAGES = Sets.newHashSet("SKYBLOCK","\u7A7A\u5C9B\u751F\u5B58", "\u7A7A\u5CF6\u751F\u5B58");

    public GuiScreen openGui = null;
    public long lastOpenedGui = 0;

    public Commands commands;



    public static HashMap<String, String> petRarityToColourMap = new HashMap<>();
    static {
        petRarityToColourMap.put("UNKNOWN", EnumChatFormatting.RED.toString());

        petRarityToColourMap.put("COMMON", EnumChatFormatting.WHITE.toString());
        petRarityToColourMap.put("UNCOMMON", EnumChatFormatting.GREEN.toString());
        petRarityToColourMap.put("RARE", EnumChatFormatting.BLUE.toString());
        petRarityToColourMap.put("EPIC", EnumChatFormatting.DARK_PURPLE.toString());
        petRarityToColourMap.put("LEGENDARY", EnumChatFormatting.GOLD.toString());
        petRarityToColourMap.put("MYTHIC", EnumChatFormatting.LIGHT_PURPLE.toString());
    }

    public static ProfileViewer profileViewer;

    public boolean packDevEnabled = false;

    private Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    private File neuDir;

    public File getNeuDir(){ return this.neuDir;}

    public Color[][] colourMap = null;

    /**
     * Instantiates NEUIo, NEUManager and NEUOverlay instances. Registers keybinds and adds a shutdown hook to clear tmp folder.
     * @param event
     */
    @EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        INSTANCE = this;

        neuDir = new File(event.getModConfigurationDirectory(), "notenoughupdates");
        neuDir.mkdirs();

        configFile = new File(neuDir, "configNew.json");

        if(configFile.exists()) {
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
                config = gson.fromJson(reader, NEUConfig.class);
            } catch(Exception e) { }
        }

        ItemCustomizeManager.loadCustomization(new File(neuDir, "itemCustomization.json"));
        StorageManager.getInstance().loadConfig(new File(neuDir, "storageItems.json"));
        FairySouls.load(new File(neuDir, "collected_fairy_souls.json"), gson);
        PetInfoOverlay.loadConfig(new File(neuDir, "petCache.json"));
        SlotLocking.getInstance().loadConfig(new File(neuDir, "slotLocking.json"));

        if(config == null) {
            config = new NEUConfig();
            saveConfig();
        }

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new NEUEventListener(this));
        MinecraftForge.EVENT_BUS.register(CapeManager.getInstance());
        //MinecraftForge.EVENT_BUS.register(new SBGamemodes());
        MinecraftForge.EVENT_BUS.register(new EnchantingSolvers());
        MinecraftForge.EVENT_BUS.register(new CalendarOverlay());
        MinecraftForge.EVENT_BUS.register(SBInfo.getInstance());
        MinecraftForge.EVENT_BUS.register(CustomItemEffects.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new DungeonMap());
        MinecraftForge.EVENT_BUS.register(new SunTzu());
        MinecraftForge.EVENT_BUS.register(new MiningStuff());
        MinecraftForge.EVENT_BUS.register(new FairySouls());
        MinecraftForge.EVENT_BUS.register(new CrystalOverlay());
        MinecraftForge.EVENT_BUS.register(new ItemCooldowns());
        MinecraftForge.EVENT_BUS.register(new DwarvenMinesTextures());
        MinecraftForge.EVENT_BUS.register(new DwarvenMinesWaypoints());
        MinecraftForge.EVENT_BUS.register(new FuelBar());
        //MinecraftForge.EVENT_BUS.register(new FancyPortals());
        MinecraftForge.EVENT_BUS.register(XPInformation.getInstance());
        MinecraftForge.EVENT_BUS.register(OverlayManager.petInfoOverlay);
        MinecraftForge.EVENT_BUS.register(OverlayManager.timersOverlay);
        MinecraftForge.EVENT_BUS.register(new NullzeeSphere());
        MinecraftForge.EVENT_BUS.register(InventoryStorageSelector.getInstance());
        MinecraftForge.EVENT_BUS.register(SlotLocking.getInstance());
        MinecraftForge.EVENT_BUS.register(FishingHelper.getInstance());

        if(Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(CustomSkulls.getInstance());
            ((IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(NPCRetexturing.getInstance());
            ((IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new ItemCustomizeManager.ReloadListener());
        }

        this.commands = new Commands();

        BackgroundBlur.registerListener();

        manager = new NEUManager(this, neuDir);
        manager.loadItemInformation();
        overlay = new NEUOverlay(manager);
        profileViewer = new ProfileViewer(manager);

        for(KeyBinding kb : manager.keybinds) {
            ClientRegistry.registerKeyBinding(kb);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            File tmp = new File(neuDir, "tmp");
            if(tmp.exists()) {
                for(File tmpFile : tmp.listFiles()) {
                    tmpFile.delete();
                }
                tmp.delete();
            }
            //saveConfig();
        }));
    }

    public void saveConfig() {
        try {
            configFile.createNewFile();

            try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
                writer.write(gson.toJson(config));
            }
        } catch(Exception ignored) {}

        try { ItemCustomizeManager.saveCustomization(new File(neuDir, "itemCustomization.json")); } catch(Exception ignored) {}
        try { StorageManager.getInstance().saveConfig(new File(neuDir, "storageItems.json")); } catch(Exception ignored) {}
        try { FairySouls.save(new File(neuDir, "collected_fairy_souls.json"), gson); } catch(Exception ignored) {}
        try { PetInfoOverlay.saveConfig(new File(neuDir, "petCache.json")); } catch(Exception ignored) {}
        try { SlotLocking.getInstance().saveConfig(new File(neuDir, "slotLocking.json")); } catch(Exception ignored) {}
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

    public void displayLinks(JsonObject update) {
        String discord_link = update.get("discord_link").getAsString();
        String youtube_link = update.get("youtube_link").getAsString();
        String twitch_link = update.get("twitch_link").getAsString();
        String update_link = update.get("update_link").getAsString();
        String github_link = update.get("github_link").getAsString();
        String other_text = update.get("other_text").getAsString();
        String other_link = update.get("other_link").getAsString();

        ChatComponentText other = null;
        if(other_text.length() > 0) {
            other = new ChatComponentText(EnumChatFormatting.GRAY+"["+EnumChatFormatting.BLUE+other_text+EnumChatFormatting.GRAY+"]");
            other.setChatStyle(Utils.createClickStyle(ClickEvent.Action.OPEN_URL, other_link));
        }
        ChatComponentText links = new ChatComponentText("");
        ChatComponentText separator = new ChatComponentText(
                EnumChatFormatting.GRAY+EnumChatFormatting.BOLD.toString()+EnumChatFormatting.STRIKETHROUGH+(other==null?"--":"-"));
        ChatComponentText discord = new ChatComponentText(EnumChatFormatting.GRAY+"["+EnumChatFormatting.BLUE+"Discord"+EnumChatFormatting.GRAY+"]");
        discord.setChatStyle(Utils.createClickStyle(ClickEvent.Action.OPEN_URL, discord_link));
        ChatComponentText youtube = new ChatComponentText(EnumChatFormatting.GRAY+"["+EnumChatFormatting.RED+"YouTube"+EnumChatFormatting.GRAY+"]");
        youtube.setChatStyle(Utils.createClickStyle(ClickEvent.Action.OPEN_URL, youtube_link));
        ChatComponentText twitch = new ChatComponentText(EnumChatFormatting.GRAY+"["+EnumChatFormatting.DARK_PURPLE+"Twitch"+EnumChatFormatting.GRAY+"]");
        twitch.setChatStyle(Utils.createClickStyle(ClickEvent.Action.OPEN_URL, twitch_link));
        ChatComponentText release = new ChatComponentText(EnumChatFormatting.GRAY+"["+EnumChatFormatting.GREEN+"Release"+EnumChatFormatting.GRAY+"]");
        release.setChatStyle(Utils.createClickStyle(ClickEvent.Action.OPEN_URL, update_link));
        ChatComponentText github = new ChatComponentText(EnumChatFormatting.GRAY+"["+EnumChatFormatting.DARK_PURPLE+"GitHub"+EnumChatFormatting.GRAY+"]");
        github.setChatStyle(Utils.createClickStyle(ClickEvent.Action.OPEN_URL, github_link));

        links.appendSibling(separator);
        links.appendSibling(discord);
        links.appendSibling(separator);
        links.appendSibling(youtube);
        links.appendSibling(separator);
        links.appendSibling(twitch);
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
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if(Minecraft.getMinecraft().thePlayer == null) {
            openGui = null;
            currChatMessage = null;
            return;
        }
        long currentTime = System.currentTimeMillis();

        if (openGui != null) {
            if(Minecraft.getMinecraft().thePlayer.openContainer != null) {
                Minecraft.getMinecraft().thePlayer.closeScreen();
            }
            Minecraft.getMinecraft().displayGuiScreen(openGui);
            openGui = null;
            lastOpenedGui = System.currentTimeMillis();
        }
        if(currChatMessage != null && currentTime - lastChatMessage > CHAT_MSG_COOLDOWN) {
            lastChatMessage = currentTime;
            Minecraft.getMinecraft().thePlayer.sendChatMessage(currChatMessage);
            currChatMessage = null;
        }
    }

    public boolean isOnSkyblock() {
        if(!config.misc.onlyShowOnSkyblock) return true;
        return hasSkyblockScoreboard();
    }

    private boolean hasSkyblockScoreboard;

    public boolean hasSkyblockScoreboard() {
        return hasSkyblockScoreboard;
    }

    public void updateSkyblockScoreboard() {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc != null && mc.theWorld != null && mc.thePlayer != null) {
            if (mc.isSingleplayer() || mc.thePlayer.getClientBrand() == null ||
                    !mc.thePlayer.getClientBrand().toLowerCase().contains("hypixel")) {
                hasSkyblockScoreboard = false;
                return;
            }

            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
            if (sidebarObjective != null) {
                String objectiveName = sidebarObjective.getDisplayName().replaceAll("(?i)\\u00A7.", "");
                for (String skyblock : SKYBLOCK_IN_ALL_LANGUAGES) {
                    if (objectiveName.startsWith(skyblock)) {
                        hasSkyblockScoreboard = true;
                        return;
                    }
                }
            }

            hasSkyblockScoreboard = false;
        }

    }
}
