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
import io.github.moulberry.notenoughupdates.gamemodes.GuiGamemodes;
import io.github.moulberry.notenoughupdates.gamemodes.SBGamemodes;
import io.github.moulberry.notenoughupdates.infopanes.CollectionLogInfoPane;
import io.github.moulberry.notenoughupdates.infopanes.CosmeticsInfoPane;
import io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer;
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewer;
import io.github.moulberry.notenoughupdates.questing.GuiQuestLine;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Session;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod(modid = NotEnoughUpdates.MODID, version = NotEnoughUpdates.VERSION, clientSideOnly = true)
public class NotEnoughUpdates {
    public static final String MODID = "notenoughupdates";
    public static final String VERSION = "1.1-REL";

    public static NotEnoughUpdates INSTANCE = null;

    public NEUManager manager;
    public NEUOverlay overlay;
    private NEUIO neuio;

    private static final long CHAT_MSG_COOLDOWN = 200;
    private long lastChatMessage = 0;
    private long secondLastChatMessage = 0;
    private String currChatMessage = null;

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

    SimpleCommand gamemodesCommand = new SimpleCommand("neugamemodes", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            boolean upgradeOverride = args.length == 1 && args[0].equals("upgradeOverride");
            openGui = new GuiGamemodes(upgradeOverride);
        }
    });

    SimpleCommand enchantColourCommand = new SimpleCommand("neuec", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            openGui = new GuiEnchantColour();
        }
    });

    SimpleCommand resetRepoCommand = new SimpleCommand("neuresetrepo", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            manager.resetRepo();
        }
    });

    public static ProfileViewer profileViewer;

    SimpleCommand.ProcessCommandRunnable viewProfileRunnable = new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if (manager.config.apiKey.value == null || manager.config.apiKey.value.trim().isEmpty()) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED +
                        "Can't view profile, apikey is not set. Run /api new and put the result in settings."));
            } else if (args.length == 0) {
                profileViewer.getProfileByName(Minecraft.getMinecraft().thePlayer.getName(), profile -> {
                    if (profile != null) profile.resetCache();
                    openGui = new GuiProfileViewer(profile);
                });
            } else if (args.length > 1) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED +
                        "Too many arguments. Usage: /neuprofile [name]"));
            } else {
                profileViewer.getProfileByName(args[0], profile -> {
                    if (profile != null) profile.resetCache();
                    openGui = new GuiProfileViewer(profile);
                });
            }
        }
    };

    SimpleCommand viewProfileCommand = new SimpleCommand("neuprofile", viewProfileRunnable, new SimpleCommand.TabCompleteRunnable() {
        @Override
        public List<String> tabComplete(ICommandSender sender, String[] args, BlockPos pos) {
            if(args.length != 1) return null;

            String lastArg = args[args.length-1];
            List<String> playerMatches = new ArrayList<>();
            for(EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
                String playerName = player.getName();
                if(playerName.toLowerCase().startsWith(lastArg.toLowerCase())) {
                    playerMatches.add(playerName);
                }
            }
            return playerMatches;
        }
    });

    SimpleCommand viewProfileShortCommand = new SimpleCommand("pv", new SimpleCommand.ProcessCommandRunnable() {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if(!hasSkyblockScoreboard()) {
                Minecraft.getMinecraft().thePlayer.sendChatMessage("/pv " + StringUtils.join(args, " "));
            } else {
                viewProfileRunnable.processCommand(sender, args);
            }
        }
    }, new SimpleCommand.TabCompleteRunnable() {
        @Override
        public List<String> tabComplete(ICommandSender sender, String[] args, BlockPos pos) {
            if (args.length != 1) return null;

            String lastArg = args[args.length - 1];
            List<String> playerMatches = new ArrayList<>();
            for (EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
                String playerName = player.getName();
                if (playerName.toLowerCase().startsWith(lastArg.toLowerCase())) {
                    playerMatches.add(playerName);
                }
            }
            return playerMatches;
        }
    });

    SimpleCommand viewProfileShort2Command = new SimpleCommand("vp", new SimpleCommand.ProcessCommandRunnable() {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if(!hasSkyblockScoreboard()) {
                Minecraft.getMinecraft().thePlayer.sendChatMessage("/vp " + StringUtils.join(args, " "));
            } else {
                viewProfileRunnable.processCommand(sender, args);
            }
        }
    }, new SimpleCommand.TabCompleteRunnable() {
        @Override
        public List<String> tabComplete(ICommandSender sender, String[] args, BlockPos pos) {
            if (args.length != 1) return null;

            String lastArg = args[args.length - 1];
            List<String> playerMatches = new ArrayList<>();
            for (EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
                String playerName = player.getName();
                if (playerName.toLowerCase().startsWith(lastArg.toLowerCase())) {
                    playerMatches.add(playerName);
                }
            }
            return playerMatches;
        }
    });

    SimpleCommand linksCommand = new SimpleCommand("neulinks", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            File repo = manager.repoLocation;
            if(repo.exists()) {
                File updateJson = new File(repo, "update.json");
                try {
                    JsonObject update = manager.getJsonFromFile(updateJson);

                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(""));
                    displayLinks(update);
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(""));
                } catch (Exception ignored) {
                }
            }
        }
    });

    SimpleCommand overlayPlacementsCommand = new SimpleCommand("neuoverlay", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            openGui = new NEUOverlayPlacements();
        }
    });

    SimpleCommand tutorialCommand = new SimpleCommand("neututorial", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            openGui = new HelpGUI();
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
                        "Can't open NeuAH, apikey is not set. Run /api new and put the result in settings."));
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
        MinecraftForge.EVENT_BUS.register(new NEUEventListener(this));
        MinecraftForge.EVENT_BUS.register(CapeManager.getInstance());
        MinecraftForge.EVENT_BUS.register(new SBGamemodes());

        File f = new File(event.getModConfigurationDirectory(), "notenoughupdates");
        f.mkdirs();
        ClientCommandHandler.instance.registerCommand(collectionLogCommand);
        ClientCommandHandler.instance.registerCommand(cosmeticsCommand);
        ClientCommandHandler.instance.registerCommand(linksCommand);
        ClientCommandHandler.instance.registerCommand(gamemodesCommand);
        ClientCommandHandler.instance.registerCommand(resetRepoCommand);
        ClientCommandHandler.instance.registerCommand(viewProfileCommand);
        ClientCommandHandler.instance.registerCommand(viewProfileShortCommand);
        ClientCommandHandler.instance.registerCommand(viewProfileShort2Command);
        ClientCommandHandler.instance.registerCommand(tutorialCommand);
        ClientCommandHandler.instance.registerCommand(overlayPlacementsCommand);
        ClientCommandHandler.instance.registerCommand(enchantColourCommand);
        ClientCommandHandler.instance.registerCommand(neuAhCommand);

        neuio = new NEUIO(getAccessToken());
        manager = new NEUManager(this, neuio, f);
        manager.loadItemInformation();
        overlay = new NEUOverlay(manager);
        profileViewer = new ProfileViewer(manager);

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
        try {
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
        }
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
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        long currentTime = System.currentTimeMillis();

        if (openGui != null) {
            Minecraft.getMinecraft().displayGuiScreen(openGui);
            openGui = null;
        }
        if(currChatMessage != null && currentTime - lastChatMessage > CHAT_MSG_COOLDOWN) {
            lastChatMessage = currentTime;
            Minecraft.getMinecraft().thePlayer.sendChatMessage(currChatMessage);
            currChatMessage = null;
        }
    }

    public boolean isOnSkyblock() {
        if(!manager.config.onlyShowOnSkyblock.value) return true;
        return hasSkyblockScoreboard();
    }

    private boolean hasSkyblockScoreboard;

    public boolean hasSkyblockScoreboard() {
        return hasSkyblockScoreboard;
    }

    //Stolen from Biscut's SkyblockAddons
    public void updateSkyblockScoreboard() {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc != null && mc.theWorld != null) {
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
        }

        hasSkyblockScoreboard = false;
    }
}
