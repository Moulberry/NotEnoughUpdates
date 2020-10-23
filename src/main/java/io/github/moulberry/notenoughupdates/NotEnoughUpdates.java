package io.github.moulberry.notenoughupdates;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import io.github.moulberry.notenoughupdates.auction.CustomAHGui;
import io.github.moulberry.notenoughupdates.commands.SimpleCommand;
import io.github.moulberry.notenoughupdates.cosmetics.CapeManager;
import io.github.moulberry.notenoughupdates.cosmetics.GuiCosmetics;
import io.github.moulberry.notenoughupdates.gamemodes.GuiGamemodes;
import io.github.moulberry.notenoughupdates.gamemodes.SBGamemodes;
import io.github.moulberry.notenoughupdates.infopanes.CollectionLogInfoPane;
import io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer;
import io.github.moulberry.notenoughupdates.profileviewer.PlayerStats;
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewer;
import io.github.moulberry.notenoughupdates.questing.GuiQuestLine;
import io.github.moulberry.notenoughupdates.questing.SBInfo;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
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
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import javax.net.ssl.*;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod(modid = NotEnoughUpdates.MODID, version = NotEnoughUpdates.VERSION, clientSideOnly = true)
public class NotEnoughUpdates {
    public static final String MODID = "notenoughupdates";
    public static final String VERSION = "1.4-REL";

    public static NotEnoughUpdates INSTANCE = null;

    public NEUManager manager;
    public NEUOverlay overlay;

    private static final long CHAT_MSG_COOLDOWN = 200;
    private long lastChatMessage = 0;
    private long secondLastChatMessage = 0;
    private String currChatMessage = null;

    //Stolen from Biscut and used for detecting whether in skyblock
    private static final Set<String> SKYBLOCK_IN_ALL_LANGUAGES = Sets.newHashSet("SKYBLOCK","\u7A7A\u5C9B\u751F\u5B58");

    private GuiScreen openGui = null;

    SimpleCommand collectionLogCommand = new SimpleCommand("neucl", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if(!OpenGlHelper.isFramebufferEnabled()) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED +
                        "This feature requires FBOs to work. Try disabling Optifine's 'Fast Render'."));
            } else {
                if(!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
                    openGui = new GuiInventory(Minecraft.getMinecraft().thePlayer);
                }
                overlay.displayInformationPane(new CollectionLogInfoPane(overlay, manager));
            }
        }
    });

    SimpleCommand itemRenameCommand = new SimpleCommand("neurename", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if(args.length == 0) {
                args = new String[]{"help"};
            }
            String heldUUID = manager.getUUIDForItem(Minecraft.getMinecraft().thePlayer.getHeldItem());
            switch(args[0].toLowerCase()) {
                case "clearall":
                    manager.itemRenameJson = new JsonObject();
                    manager.saveItemRenameConfig();
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[NEU] Cleared custom name for all items"));
                    break;
                case "clear":
                    if(heldUUID == null) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[NEU] Can't clear rename - no UUID"));
                        return;
                    }
                    manager.itemRenameJson.remove(heldUUID);
                    manager.saveItemRenameConfig();
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[NEU] Cleared custom name for held item"));
                    break;
                case "copyuuid":
                    if(heldUUID == null) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[NEU] Can't clear rename - no UUID"));
                        return;
                    }
                    StringSelection selection = new StringSelection(heldUUID);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[NEU] UUID copied to clipboard"));
                    break;
                case "uuid":
                    if(heldUUID == null) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[NEU] Can't get UUID - no UUID"));
                        return;
                    }
                    ChatStyle style = new ChatStyle();
                    style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ChatComponentText(EnumChatFormatting.GRAY+"Click to copy to clipboard")));
                    style.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "neurename copyuuid"));

                    ChatComponentText text = new ChatComponentText(EnumChatFormatting.YELLOW+"[NEU] The UUID of your currently held item is: " +
                            EnumChatFormatting.GREEN + heldUUID);
                    text.setChatStyle(style);
                    sender.addChatMessage(text);
                    break;
                case "set":
                    if(heldUUID == null) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[NEU] Can't rename item - no UUID"));
                        return;
                    }
                    if(args.length == 1) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[NEU] Usage: /neurename set [name...]"));
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    for(int i=1; i<args.length; i++) {
                        sb.append(args[i]);
                        if(i<args.length-1) sb.append(" ");
                    }
                    String name = sb.toString()
                            .replace("\\&", "{amp}")
                            .replace("&", "\u00a7")
                            .replace("{amp}", "&");
                    name = new UnicodeUnescaper().translate(name);
                    manager.itemRenameJson.addProperty(heldUUID, name);
                    manager.saveItemRenameConfig();
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[NEU] Set custom name for held item"));
                    break;
                default:
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[NEU] Unknown subcommand \""+args[0]+"\""));
                case "help":
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[NEU] Available commands:"));
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "help: Print this message"));
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "clearall: Clears all custom names "
                            + EnumChatFormatting.BOLD + "(Cannot be undone)"));
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "clear: Clears held item name "
                            + EnumChatFormatting.BOLD + "(Cannot be undone)"));
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "uuid: Returns the UUID of the currently held item"));
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "set: Sets the custom name of the currently held item"));
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Usage: /neurename set [name...]"));

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

    SimpleCommand reloadRepoCommand = new SimpleCommand("neureloadrepo", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            File items = new File(manager.repoLocation, "items");
            if(items.exists()) {
                File[] itemFiles = new File(manager.repoLocation, "items").listFiles();
                if(itemFiles != null) {
                    for(File f : itemFiles) {
                        String internalname = f.getName().substring(0, f.getName().length()-5);
                        manager.loadItem(internalname);
                    }
                }
            }
        }
    });

    private static HashMap<String, String> petRarityToColourMap = new HashMap<>();
    static {
        petRarityToColourMap.put("UNKNOWN", EnumChatFormatting.RED.toString());

        petRarityToColourMap.put("COMMON", EnumChatFormatting.WHITE.toString());
        petRarityToColourMap.put("UNCOMMON", EnumChatFormatting.GREEN.toString());
        petRarityToColourMap.put("RARE", EnumChatFormatting.BLUE.toString());
        petRarityToColourMap.put("EPIC", EnumChatFormatting.DARK_PURPLE.toString());
        petRarityToColourMap.put("LEGENDARY", EnumChatFormatting.GOLD.toString());
    }
    ScheduledExecutorService peekCommandExecutorService = null;
    SimpleCommand peekCommand = new SimpleCommand("peek", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            String name;
            if(args.length == 0) {
                name = Minecraft.getMinecraft().thePlayer.getName();
            } else {
                name = args[0];
            }
            int id = new Random().nextInt(Integer.MAX_VALUE/2)+Integer.MAX_VALUE/2;

            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new ChatComponentText(
                    EnumChatFormatting.YELLOW+"[PEEK] Getting player information..."), id);
            profileViewer.getProfileByName(name, profile -> {
                if (profile == null) {
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new ChatComponentText(
                            EnumChatFormatting.RED+"[PEEK] Unknown player or api is down."), id);
                } else {
                    profile.resetCache();

                    if(peekCommandExecutorService == null || peekCommandExecutorService.isShutdown()) {
                        peekCommandExecutorService = Executors.newSingleThreadScheduledExecutor();
                    } else {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                                EnumChatFormatting.RED+"[PEEK] New peek command run, cancelling old one."));
                        peekCommandExecutorService.shutdownNow();
                        peekCommandExecutorService = Executors.newSingleThreadScheduledExecutor();
                    }

                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new ChatComponentText(
                            EnumChatFormatting.YELLOW+"[PEEK] Getting player skyblock profiles..."), id);

                    long startTime = System.currentTimeMillis();
                    peekCommandExecutorService.schedule(new Runnable() {
                        public void run() {
                            if(System.currentTimeMillis() - startTime > 10*1000) {

                                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new ChatComponentText(
                                        EnumChatFormatting.RED+"[PEEK] Getting profile info took too long, aborting."), id);
                                return;
                            }

                            String g = EnumChatFormatting.GRAY.toString();

                            JsonObject profileInfo = profile.getProfileInformation(null);
                            if(profileInfo != null) {
                                float overallScore = 0;

                                boolean isMe = name.equalsIgnoreCase("moulberry");

                                PlayerStats.Stats stats = profile.getStats(null);
                                JsonObject skill = profile.getSkillInfo(null);

                                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new ChatComponentText(EnumChatFormatting.GREEN+" "+
                                        EnumChatFormatting.STRIKETHROUGH+"-=-" +EnumChatFormatting.RESET+EnumChatFormatting.GREEN+" "+
                                        Utils.getElementAsString(profile.getHypixelProfile().get("displayname"), name) + "'s Info " +
                                        EnumChatFormatting.STRIKETHROUGH+"-=-"), id);

                                if(skill == null) {
                                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW+"Skills api disabled!"));
                                } else {
                                    float totalSkillLVL = 0;
                                    float totalSkillCount = 0;

                                    for(Map.Entry<String, JsonElement> entry : skill.entrySet()) {
                                        if(entry.getKey().startsWith("level_skill")) {
                                            if(entry.getKey().contains("runecrafting")) continue;
                                            if(entry.getKey().contains("carpentry")) continue;
                                            totalSkillLVL += entry.getValue().getAsFloat();
                                            totalSkillCount++;
                                        }
                                    }

                                    float combat = Utils.getElementAsFloat(skill.get("level_skill_combat"), 0);
                                    float zombie = Utils.getElementAsFloat(skill.get("level_slayer_zombie"), 0);
                                    float spider = Utils.getElementAsFloat(skill.get("level_slayer_spider"), 0);
                                    float wolf = Utils.getElementAsFloat(skill.get("level_slayer_wolf"), 0);

                                    float avgSkillLVL = totalSkillLVL/totalSkillCount;

                                    if(isMe) {
                                        avgSkillLVL = 6;
                                        combat = 4;
                                        zombie = 2;
                                        spider = 1;
                                        wolf = 2;
                                    }

                                    EnumChatFormatting combatPrefix = combat>20?(combat>35?EnumChatFormatting.GREEN:EnumChatFormatting.YELLOW):EnumChatFormatting.RED;
                                    EnumChatFormatting zombiePrefix = zombie>3?(zombie>6?EnumChatFormatting.GREEN:EnumChatFormatting.YELLOW):EnumChatFormatting.RED;
                                    EnumChatFormatting spiderPrefix = spider>3?(spider>6?EnumChatFormatting.GREEN:EnumChatFormatting.YELLOW):EnumChatFormatting.RED;
                                    EnumChatFormatting wolfPrefix = wolf>3?(wolf>6?EnumChatFormatting.GREEN:EnumChatFormatting.YELLOW):EnumChatFormatting.RED;
                                    EnumChatFormatting avgPrefix = avgSkillLVL>20?(avgSkillLVL>35?EnumChatFormatting.GREEN:EnumChatFormatting.YELLOW):EnumChatFormatting.RED;

                                    overallScore += zombie*zombie/81f;
                                    overallScore += spider*spider/81f;
                                    overallScore += wolf*wolf/81f;
                                    overallScore += avgSkillLVL/20f;

                                    int cata = (int)Utils.getElementAsFloat(skill.get("level_skill_catacombs"), 0);
                                    EnumChatFormatting cataPrefix = cata>15?(cata>25?EnumChatFormatting.GREEN:EnumChatFormatting.YELLOW):EnumChatFormatting.RED;

                                    overallScore += cata*cata/2000f;

                                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                                            g+"Combat: "+combatPrefix+(int)Math.floor(combat) +
                                                    (cata > 0 ? g+" - Cata: "+cataPrefix+cata : "")+
                                                    g+" - AVG: " + avgPrefix+(int)Math.floor(avgSkillLVL)));
                                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                                            g+"Slayer: "+zombiePrefix+(int)Math.floor(zombie)+g+"-"+
                                                    spiderPrefix+(int)Math.floor(spider)+g+"-"+wolfPrefix+(int)Math.floor(wolf)));
                                }
                                if(stats == null) {
                                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                                            EnumChatFormatting.YELLOW+"Skills, collection and/or inventory apis disabled!"));
                                } else {
                                    int health = (int)stats.get("health");
                                    int defence = (int)stats.get("defence");
                                    int strength = (int)stats.get("strength");
                                    int intelligence = (int)stats.get("intelligence");

                                    EnumChatFormatting healthPrefix = health>800?(health>1600?EnumChatFormatting.GREEN:EnumChatFormatting.YELLOW):EnumChatFormatting.RED;
                                    EnumChatFormatting defencePrefix = defence>200?(defence>600?EnumChatFormatting.GREEN:EnumChatFormatting.YELLOW):EnumChatFormatting.RED;
                                    EnumChatFormatting strengthPrefix = strength>100?(strength>300?EnumChatFormatting.GREEN:EnumChatFormatting.YELLOW):EnumChatFormatting.RED;
                                    EnumChatFormatting intelligencePrefix = intelligence>300?(intelligence>900?EnumChatFormatting.GREEN:EnumChatFormatting.YELLOW):EnumChatFormatting.RED;

                                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                                            g+"Stats  : "+healthPrefix+health+EnumChatFormatting.RED+"\u2764 "+
                                                    defencePrefix+defence+EnumChatFormatting.GREEN+"\u2748 "+
                                                    strengthPrefix+strength+EnumChatFormatting.RED+"\u2741 "+
                                                    intelligencePrefix+intelligence+EnumChatFormatting.AQUA+"\u270e "));
                                }
                                float bankBalance = Utils.getElementAsFloat(Utils.getElement(profileInfo, "banking.balance"), -1);
                                float purseBalance = Utils.getElementAsFloat(Utils.getElement(profileInfo, "coin_purse"), 0);

                                long networth = profile.getNetWorth(null);
                                float money = Math.max(bankBalance+purseBalance, networth);
                                EnumChatFormatting moneyPrefix = money>50*1000*1000?
                                        (money>200*1000*1000?EnumChatFormatting.GREEN:EnumChatFormatting.YELLOW):EnumChatFormatting.RED;
                                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                                        g+"Purse: "+moneyPrefix+Utils.shortNumberFormat(purseBalance, 0) + g+" - Bank: " +
                                                (bankBalance == -1 ? EnumChatFormatting.YELLOW+"N/A" : moneyPrefix+
                                                        (isMe?"4.8b":Utils.shortNumberFormat(bankBalance, 0))) +
                                                (networth > 0 ? g+" - Net: "+moneyPrefix+Utils.shortNumberFormat(networth, 0) : "")));

                                overallScore += Math.min(2, money/(100f*1000*1000));

                                String activePet = Utils.getElementAsString(Utils.getElement(profile.getPetsInfo(null), "active_pet.type"),
                                        "None Active");
                                String activePetTier = Utils.getElementAsString(Utils.getElement(profile.getPetsInfo(null), "active_pet.tier"), "UNKNOWN");

                                String col = petRarityToColourMap.get(activePetTier);
                                if(col == null) col = EnumChatFormatting.LIGHT_PURPLE.toString();

                                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(g+"Pet    : " +
                                        col + WordUtils.capitalizeFully(activePet.replace("_", " "))));

                                String overall = "Skywars Main";
                                if(isMe) {
                                    overall = Utils.chromaString("Literally the best player to exist");
                                } else if(overallScore < 5 && (bankBalance+purseBalance) > 500*1000*1000) {
                                    overall = EnumChatFormatting.GOLD+"Bill Gates";
                                } else if(overallScore > 9) {
                                    overall = Utils.chromaString("Didn't even think this score was possible");
                                } else if(overallScore > 8) {
                                    overall = Utils.chromaString("Mentally unstable");
                                } else if(overallScore > 7) {
                                    overall = EnumChatFormatting.GOLD+"Why though 0.0";
                                } else if(overallScore > 5.5) {
                                    overall = EnumChatFormatting.GOLD+"Bro stop playing";
                                } else if(overallScore > 4) {
                                    overall = EnumChatFormatting.GREEN+"Kinda sweaty";
                                } else if(overallScore > 3) {
                                    overall = EnumChatFormatting.YELLOW+"Alright I guess";
                                } else if(overallScore > 2) {
                                    overall = EnumChatFormatting.YELLOW+"Ender Non";
                                } else if(overallScore > 1) {
                                    overall = EnumChatFormatting.RED+"Played Skyblock";
                                }

                                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(g+"Overall score: " +
                                       overall + g + " (" + Math.round(overallScore*10)/10f + ")"));

                                peekCommandExecutorService.shutdownNow();
                            } else {
                                peekCommandExecutorService.schedule(this, 200, TimeUnit.MILLISECONDS);
                            }
                        }
                    }, 200, TimeUnit.MILLISECONDS);
                }
            });
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


    SimpleCommand pcStatsCommand = new SimpleCommand("neustats", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            Minecraft mc = Minecraft.getMinecraft();
            StringBuilder builder = new StringBuilder();

            if (args.length > 0 && args[0].toLowerCase().equals("modlist")){
                builder.append("```md\n");
                builder.append("# Mods Loaded").append("\n");
                for (ModContainer modContainer : Loader.instance().getActiveModList()) {
                    builder.append("[").append(modContainer.getName()).append("]")
                            .append("[").append(modContainer.getSource().getName()).append("]\n");
                }
                builder.append("```");
            } else {
                long memorySize = -1;
                try {
                    memorySize = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
                } catch(Exception e){}
                long maxMemory = Runtime.getRuntime().maxMemory();
                long totalMemory = Runtime.getRuntime().totalMemory();
                long freeMemory = Runtime.getRuntime().freeMemory();
                long currentMemory = totalMemory - freeMemory;
                int modCount = Loader.instance().getModList().size();
                int activeModCount = Loader.instance().getActiveModList().size();

                builder.append("```md\n");
                builder.append("# System Stats").append("\n");
                builder.append("[OS]").append("[").append(System.getProperty("os.name")).append("]").append("\n");
                builder.append("[CPU]").append("[").append(OpenGlHelper.getCpu()).append("]").append("\n");
                builder.append("[Display]").append("[").append(String.format("%dx%d (%s)", Display.getWidth(), Display.getHeight(), GL11.glGetString(GL11.GL_VENDOR))).append("]").append("\n");
                builder.append("[GPU]").append("[").append(GL11.glGetString(GL11.GL_RENDERER)).append("]").append("\n");
                builder.append("[GPU Driver]").append("[").append(GL11.glGetString(GL11.GL_VERSION)).append("]").append("\n");
                if(memorySize > 0) {
                    builder.append("[Maximum Memory]").append("[").append(memorySize / 1024L / 1024L).append("MB]").append("\n");
                }
                builder.append("[Shaders]").append("[").append((""+OpenGlHelper.areShadersSupported()).toUpperCase()).append("]").append("\n");
                builder.append("[Framebuffers]").append("[").append((""+OpenGlHelper.isFramebufferEnabled()).toUpperCase()).append("]").append("\n");
                builder.append("# Java Stats").append("\n");
                builder.append("[Java]").append("[").append(String.format("%s %dbit", System.getProperty("java.version"), mc.isJava64bit() ? 64 : 32)).append("]").append("\n");
                builder.append("[Memory]").append("[").append(String.format("% 2d%% %03d/%03dMB", currentMemory * 100L / maxMemory, currentMemory / 1024L / 1024L, maxMemory / 1024L / 1024L)).append("]").append("\n");
                builder.append("[Memory Allocated]").append("[").append(String.format("% 2d%% %03dMB", totalMemory * 100L / maxMemory, totalMemory / 1024L / 1024L)).append("]").append("\n");
                builder.append("# Game Stats").append("\n");
                builder.append("[Current FPS]").append("[").append(Minecraft.getDebugFPS()).append("]").append("\n");
                builder.append("[Loaded Mods]").append("[").append(activeModCount).append("/").append(modCount).append("]").append("\n");
                builder.append("[Forge]").append("[").append(ForgeVersion.getVersion()).append("]").append("\n");
                builder.append("# Neu Settings").append("\n");
                builder.append("[API Key]").append("[").append(!INSTANCE.manager.config.apiKey.value.isEmpty()).append("]").append("\n");
                builder.append("[On Skyblock]").append("[").append(hasSkyblockScoreboard).append("]").append("\n");
                builder.append("[Mod Version]").append("[").append(Loader.instance().getIndexedModList().get(MODID).getSource().getName()).append("]").append("\n");
                builder.append("# Repo Stats").append("\n");
                builder.append("[Last Commit]").append("[").append(manager.latestRepoCommit).append("]").append("\n");
                builder.append("[Loaded Items]").append("[").append(manager.getItemInformation().size()).append("]").append("\n");
                if (activeModCount <= 15) {
                    builder.append("# Mods Loaded").append("\n");
                    for (ModContainer modContainer : Loader.instance().getActiveModList()) {
                        builder.append("[").append(modContainer.getName()).append("]")
                                .append("[").append(modContainer.getSource().getName()).append("]\n");
                    }
                    builder.append("```");
                } else {
                    builder.append("```");
                }
            }
            try {
                StringSelection clipboard = new StringSelection(builder.toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(clipboard, clipboard);
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "[" + EnumChatFormatting.RED + "NotEnoughUpdates" + EnumChatFormatting.GOLD + "]: " + EnumChatFormatting.GREEN + "Dev info copied to clipboard."));
            } catch (Exception ignored) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "[" + EnumChatFormatting.RED + "NotEnoughUpdates" + EnumChatFormatting.GOLD + "]: " + EnumChatFormatting.DARK_RED + "Could not copy to clipboard."));
            }
        }
    });

    public static ProfileViewer profileViewer;

    SimpleCommand.ProcessCommandRunnable viewProfileRunnable = new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if(Loader.isModLoaded("optifine") &&
                    new File(Minecraft.getMinecraft().mcDataDir, "optionsof.txt").exists()) {
                try(InputStream in = new FileInputStream(new File(Minecraft.getMinecraft().mcDataDir, "optionsof.txt"))) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

                    String line;
                    while((line = reader.readLine()) != null) {
                        if(line.contains("ofFastRender:true")) {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED +
                                    "Some parts of the profile viewer do not work with OF Fast Render. Go to Video > Performance to disable it."));
                            break;
                        }
                    }
                } catch(Exception e) {
                }
            }
            if (manager.config.apiKey.value == null || manager.config.apiKey.value.trim().isEmpty()) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED +
                        "Can't view profile, apikey is not set. Run /api new and put the result in settings."));
            } else if (args.length == 0) {
                profileViewer.getProfileByName(Minecraft.getMinecraft().thePlayer.getName(), profile -> {
                    if(profile == null) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED +
                                "Invalid player name/api key. Maybe api is down? Try /api new."));
                    } else {
                        profile.resetCache();
                        openGui = new GuiProfileViewer(profile);
                    }
                });
            } else if (args.length > 1) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED +
                        "Too many arguments. Usage: /neuprofile [name]"));
            } else {
                profileViewer.getProfileByName(args[0], profile -> {
                    if(profile == null) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED +
                                "Invalid player name/api key. Maybe api is down? Try /api new."));
                    } else {
                        profile.resetCache();
                        openGui = new GuiProfileViewer(profile);
                    }
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
            if(!isOnSkyblock()) {
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
            if(!isOnSkyblock()) {
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

    public Color[][] colourMap = null;
    SimpleCommand neumapCommand = new SimpleCommand("neumap", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if(args.length == 1 && args[0].equals("reset")) {
                colourMap = null;
                return;
            }

            if(args.length != 2) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED+
                        "Dev feature if you don't know how to use then don't use it 4Head."));
                return;
            }

            if(args[0].equals("save")) {
                ItemStack stack = Minecraft.getMinecraft().thePlayer.getHeldItem();
                if(stack != null && stack.getItem() instanceof ItemMap) {
                    ItemMap map = (ItemMap) stack.getItem();
                    MapData mapData = map.getMapData(stack, Minecraft.getMinecraft().theWorld);

                    if (mapData == null) return;

                    JsonObject json = new JsonObject();
                    for (int i = 0; i < 16384; ++i) {
                        int x = i % 128;
                        int y = i / 128;

                        int j = mapData.colors[i] & 255;

                        Color c;
                        if (j / 4 == 0) {
                            c = new Color((i + i / 128 & 1) * 8 + 16 << 24, true);
                        } else {
                            c = new Color(MapColor.mapColorArray[j / 4].func_151643_b(j & 3), true);
                        }

                        json.addProperty(x+":"+y, c.getRGB());
                    }

                    try {
                        new File(manager.configLocation, "maps").mkdirs();
                        manager.writeJson(json, new File(manager.configLocation, "maps/"+args[1]+".json"));
                    } catch(Exception e) {
                        e.printStackTrace();
                    }

                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN+
                            "Saved to file."));
                }
            }

            if(args[0].equals("load")) {
                JsonObject json = manager.getJsonFromFile(new File(manager.configLocation, "maps/"+args[1]+".json"));
                colourMap = new Color[128][128];
                for(int x=0; x<128; x++) {
                    for(int y=0; y<128; y++) {
                        colourMap[x][y] = new Color(0, 0, 0, 0);
                    }
                }
                for(Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    int x = Integer.parseInt(entry.getKey().split(":")[0]);
                    int y = Integer.parseInt(entry.getKey().split(":")[1]);

                    colourMap[x][y] = new Color(entry.getValue().getAsInt(), true);
                }
            }
        }
    });

    SimpleCommand cosmeticsCommand = new SimpleCommand("neucosmetics", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            openGui = new GuiCosmetics();
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
        MinecraftForge.EVENT_BUS.register(SBInfo.getInstance());
        MinecraftForge.EVENT_BUS.register(CustomItemEffects.INSTANCE);
        //MinecraftForge.EVENT_BUS.register(new DungeonMap());
        //MinecraftForge.EVENT_BUS.register(new BetterPortals());

        IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        if(resourceManager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager)resourceManager).registerReloadListener(new DungeonBlocks());
        }

        File f = new File(event.getModConfigurationDirectory(), "notenoughupdates");
        f.mkdirs();
        ClientCommandHandler.instance.registerCommand(collectionLogCommand);
        ClientCommandHandler.instance.registerCommand(cosmeticsCommand);
        ClientCommandHandler.instance.registerCommand(linksCommand);
        ClientCommandHandler.instance.registerCommand(gamemodesCommand);
        ClientCommandHandler.instance.registerCommand(resetRepoCommand);
        ClientCommandHandler.instance.registerCommand(reloadRepoCommand);
        ClientCommandHandler.instance.registerCommand(itemRenameCommand);
        ClientCommandHandler.instance.registerCommand(viewProfileCommand);
        ClientCommandHandler.instance.registerCommand(viewProfileShortCommand);
        ClientCommandHandler.instance.registerCommand(viewProfileShort2Command);
        ClientCommandHandler.instance.registerCommand(peekCommand);
        ClientCommandHandler.instance.registerCommand(tutorialCommand);
        ClientCommandHandler.instance.registerCommand(overlayPlacementsCommand);
        ClientCommandHandler.instance.registerCommand(enchantColourCommand);
        ClientCommandHandler.instance.registerCommand(neuAhCommand);
        ClientCommandHandler.instance.registerCommand(pcStatsCommand);
        ClientCommandHandler.instance.registerCommand(neumapCommand);

        manager = new NEUManager(this, f);
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
            if(Minecraft.getMinecraft().thePlayer.openContainer != null) {
                Minecraft.getMinecraft().thePlayer.closeScreen();
            }
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
