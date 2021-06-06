package io.github.moulberry.notenoughupdates;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.auction.CustomAHGui;
import io.github.moulberry.notenoughupdates.collectionlog.GuiCollectionLog;
import io.github.moulberry.notenoughupdates.commands.SimpleCommand;
import io.github.moulberry.notenoughupdates.core.BackgroundBlur;
import io.github.moulberry.notenoughupdates.core.GuiScreenElementWrapper;
import io.github.moulberry.notenoughupdates.core.util.MiscUtils;
import io.github.moulberry.notenoughupdates.cosmetics.CapeManager;
import io.github.moulberry.notenoughupdates.cosmetics.GuiCosmetics;
import io.github.moulberry.notenoughupdates.dungeons.DungeonMap;
import io.github.moulberry.notenoughupdates.dungeons.DungeonWin;
import io.github.moulberry.notenoughupdates.dungeons.GuiDungeonMapEditor;
import io.github.moulberry.notenoughupdates.gamemodes.GuiGamemodes;
import io.github.moulberry.notenoughupdates.gamemodes.SBGamemodes;
import io.github.moulberry.notenoughupdates.miscfeatures.*;
import io.github.moulberry.notenoughupdates.miscgui.*;
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
    public static final int VERSION_ID = 20000;

    public static NotEnoughUpdates INSTANCE = null;

    public NEUManager manager;
    public NEUOverlay overlay;
    public NEUConfig config;

    private File configFile;

    private static final long CHAT_MSG_COOLDOWN = 200;
    private long lastChatMessage = 0;
    private long secondLastChatMessage = 0;
    private String currChatMessage = null;

    //Stolen from Biscut and used for detecting whether in skyblock
    private static final Set<String> SKYBLOCK_IN_ALL_LANGUAGES = Sets.newHashSet("SKYBLOCK","\u7A7A\u5C9B\u751F\u5B58", "\u7A7A\u5CF6\u751F\u5B58");

    public GuiScreen openGui = null;
    public long lastOpenedGui = 0;

    SimpleCommand.ProcessCommandRunnable collectionLogRun = new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            openGui = new GuiCollectionLog();
        }
    };

    SimpleCommand collectionLogCommand = new SimpleCommand("neucl", collectionLogRun);
    SimpleCommand collectionLogCommand2 = new SimpleCommand("collectionlog", collectionLogRun);

    SimpleCommand nullzeeSphereCommand = new SimpleCommand("neuzeesphere", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if(args.length != 1) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED+"Usage: /neuzeesphere [on/off] or /neuzeesphere (radius) or /neuzeesphere setCenter"));
                return;
            }
            if(args[0].equalsIgnoreCase("on")) {
                NullzeeSphere.enabled = true;
            } else if(args[0].equalsIgnoreCase("off")) {
                NullzeeSphere.enabled = false;
            } else if(args[0].equalsIgnoreCase("setCenter")) {
                EntityPlayerSP p = ((EntityPlayerSP)sender);
                NullzeeSphere.centerPos = new BlockPos(p.posX, p.posY, p.posZ);
                NullzeeSphere.overlayVBO = null;
            } else {
                try {
                    float radius = Float.parseFloat(args[0]);
                    NullzeeSphere.size = radius;
                    NullzeeSphere.overlayVBO = null;
                } catch(Exception e) {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED+"Can't parse radius: " + args[0]));
                }
            }
        }
    });

    /*SimpleCommand itemRenameCommand = new SimpleCommand("neurename", new SimpleCommand.ProcessCommandRunnable() {
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
                    style.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/neurename copyuuid"));

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
    });*/

    SimpleCommand stWhyCommand = new SimpleCommand("neustwhy", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            NEUEventListener.displayNotification(Lists.newArrayList(
                              "\u00a7eStorage Viewer",
                            "\u00a77Currently, the storage viewer requires you to click twice",
                            "\u00a77in order to switch between pages. This is because Hypixel",
                            "\u00a77has not yet added a shortcut command to go to any enderchest/",
                            "\u00a77storage page.",
                            "\u00a77While it is possible to send the second click",
                            "\u00a77automatically, doing so violates Hypixel's new mod rules."), true);
        }
    });

    SimpleCommand gamemodesCommand = new SimpleCommand("neugamemodes", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            boolean upgradeOverride = args.length == 1 && args[0].equals("upgradeOverride");
            openGui = new GuiGamemodes(upgradeOverride);
        }
    });

    SimpleCommand buttonsCommand = new SimpleCommand("neubuttons", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            openGui = new GuiInvButtonEditor();
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

    SimpleCommand dungeonWinTest = new SimpleCommand("neudungeonwintest", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if(args.length > 0) {
                DungeonWin.TEAM_SCORE = new ResourceLocation("notenoughupdates:dungeon_win/"+args[0].toLowerCase()+".png");
            }

            DungeonWin.displayWin();
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
            Constants.reload();

            configFile = new File(neuDir, "configNew.json");
            if(configFile.exists()) {
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
                    config = gson.fromJson(reader, NEUConfig.class);
                } catch(Exception e) { }
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
                builder.append("[API Key]").append("[").append(!config.apiKey.apiKey.isEmpty()).append("]").append("\n");
                builder.append("[On Skyblock]").append("[").append(hasSkyblockScoreboard).append("]").append("\n");
                builder.append("[Mod Version]").append("[").append(Loader.instance().getIndexedModList().get(MODID).getSource().getName()).append("]").append("\n");
                builder.append("[SB Profile]").append("[").append(SBInfo.getInstance().currentProfile).append("]").append("\n");
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
            if (config.apiKey.apiKey == null || config.apiKey.apiKey.trim().isEmpty()) {
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


    SimpleCommand joinDungeonCommand = new SimpleCommand("join", new SimpleCommand.ProcessCommandRunnable() {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (!hasSkyblockScoreboard()) {
                Minecraft.getMinecraft().thePlayer.sendChatMessage("/join " + StringUtils.join(args, " "));
            } else {
                if(args.length != 1) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                            EnumChatFormatting.RED+"Example Usage: /join f7 or /join 7"));
                } else {
                    String cmd = "/joindungeon catacombs " + args[0].charAt(args[0].length()-1);
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                            EnumChatFormatting.YELLOW+"Running command: "+cmd));
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                            EnumChatFormatting.YELLOW+"The dungeon should start soon. If it doesn't, make sure you have a party of 5 people"));
                    Minecraft.getMinecraft().thePlayer.sendChatMessage(cmd);
                }
            }
        }
    });

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

    SimpleCommand dhCommand = new SimpleCommand("dh", new SimpleCommand.ProcessCommandRunnable() {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/warp dungeon_hub");
        }
    });

    private ScheduledExecutorService devES = Executors.newSingleThreadScheduledExecutor();
    private static final String[] devFailStrings = {"No.", "I said no.", "You aren't allowed to use this.",
            "Are you sure you want to use this? Type 'Yes' in chat.", "Lmao you thought", "Ok please stop",
            "What do you want from me?", "This command almost certainly does nothing useful for you",
            "Ok, this is the last message, after this it will repeat", "No.", "Dammit. I thought that would work. Uhh...",
            "\u00a7dFrom \u00a7c[ADMIN] Minikloon\u00a77: If you use that command again, I'll have to ban you", "",
            "Ok, this is actually the last message, use the command again and you'll crash I promise"};
    private int devFailIndex = 0;
    SimpleCommand devTestCommand = new SimpleCommand("neudevtest", new SimpleCommand.ProcessCommandRunnable() {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if(!Minecraft.getMinecraft().thePlayer.getName().equalsIgnoreCase("Moulberry") &&
                    !Minecraft.getMinecraft().thePlayer.getName().equalsIgnoreCase("LucyCoconut")) {
                if(devFailIndex >= devFailStrings.length) {
                    throw new Error("L") {
                        @Override
                        public void printStackTrace() {
                            throw new Error("Double L");
                        }
                    };
                }
                if(devFailIndex == devFailStrings.length-2) {
                    devFailIndex++;

                    ChatComponentText component = new ChatComponentText("\u00a7cYou are permanently banned from this server!");
                    component.appendText("\n");
                    component.appendText("\n\u00a77Reason: \u00a7rI told you not to run the command - Moulberry");
                    component.appendText("\n\u00a77Find out more: \u00a7b\u00a7nhttps://www.hypixel.net/appeal");
                    component.appendText("\n");
                    component.appendText("\n\u00a77Ban ID: \u00a7r#49871982");
                    component.appendText("\n\u00a77Sharing your Ban ID may affect the processing of your appeal!");
                    Minecraft.getMinecraft().getNetHandler().getNetworkManager().closeChannel(component);
                    return;
                }
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED+devFailStrings[devFailIndex++]));
                return;
            }
            /*if(args.length == 1) {
                DupePOC.doDupe(args[0]);
                return;
            }*/
            if(args.length == 2 && args[0].equalsIgnoreCase("pt")) {
                EnumParticleTypes t = EnumParticleTypes.valueOf(args[1]);
                FishingHelper.type = t;
            }
            if(args.length == 1 && args[0].equalsIgnoreCase("dev")) {
                NotEnoughUpdates.INSTANCE.config.hidden.dev = true;
                return;
            }
            if(args.length == 1 && args[0].equalsIgnoreCase("saveconfig")) {
                saveConfig();
                return;
            }
            if(args.length == 1 && args[0].equalsIgnoreCase("center")) {
                double x = Math.floor(Minecraft.getMinecraft().thePlayer.posX) + 0.5f;
                double z = Math.floor(Minecraft.getMinecraft().thePlayer.posZ) + 0.5f;
                Minecraft.getMinecraft().thePlayer.setPosition(x, Minecraft.getMinecraft().thePlayer.posY, z);
                return;
            }
            if(args.length == 1 && args[0].equalsIgnoreCase("pansc")) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN+"Taking panorama screenshot"));

                AtomicInteger perspective = new AtomicInteger(0);
                FancyPortals.perspectiveId = 0;

                EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
                p.prevRotationYaw = p.rotationYaw = 0;
                p.prevRotationPitch = p.rotationPitch = 90;
                devES.schedule(new Runnable() {
                    @Override
                    public void run() {
                        Minecraft.getMinecraft().addScheduledTask(() -> {
                            ScreenShotHelper.saveScreenshot(new File("C:/Users/James/Desktop/"), "pansc-"+perspective.get()+".png",
                                    Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight,
                                    Minecraft.getMinecraft().getFramebuffer());
                        });
                        if(perspective.incrementAndGet() >= 6) {
                            FancyPortals.perspectiveId = -1;
                            return;
                        }
                        devES.schedule(() -> {
                            FancyPortals.perspectiveId = perspective.get();
                            if(FancyPortals.perspectiveId == 5) {
                                p.prevRotationYaw = p.rotationYaw = 0;
                                p.prevRotationPitch = p.rotationPitch = -90;
                            } else if(FancyPortals.perspectiveId >= 1 && FancyPortals.perspectiveId <= 4) {
                                float yaw = 90*FancyPortals.perspectiveId-180;
                                if(yaw > 180) yaw -= 360;
                                p.prevRotationYaw = p.rotationYaw = yaw;
                                p.prevRotationPitch = p.rotationPitch = 0;
                            }
                            devES.schedule(this, 3000L, TimeUnit.MILLISECONDS);
                        }, 100L, TimeUnit.MILLISECONDS);
                    }
                }, 3000L, TimeUnit.MILLISECONDS);

                return;
            }
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN+"Executing dubious code"));
            /*Minecraft.getMinecraft().thePlayer.rotationYaw = 0;
            Minecraft.getMinecraft().thePlayer.rotationPitch = 0;
            Minecraft.getMinecraft().thePlayer.setPosition(
                    Math.floor(Minecraft.getMinecraft().thePlayer.posX) + Float.parseFloat(args[0]),
                    Minecraft.getMinecraft().thePlayer.posY,
                    Minecraft.getMinecraft().thePlayer.posZ);*/
            //Hot reload me yay!
        }
    });

    public boolean packDevEnabled = false;
    SimpleCommand packDevCommand = new SimpleCommand("neupackdev", new SimpleCommand.ProcessCommandRunnable() {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if(args.length == 1 && args[0].equalsIgnoreCase("getnpc")) {
                double distSq = 25;
                EntityPlayer closestNPC = null;
                EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
                for(EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
                    if(player instanceof AbstractClientPlayer && p != player && player.getUniqueID().version() != 4) {
                        double dSq = player.getDistanceSq(p.posX, p.posY, p.posZ);
                        if(dSq < distSq) {
                            distSq = dSq;
                            closestNPC = player;
                        }
                    }
                }

                if(closestNPC == null) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED+"No NPCs found within 5 blocks :("));
                } else {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN+"Copied entity texture id to clipboard"));
                    MiscUtils.copyToClipboard(((AbstractClientPlayer)closestNPC).getLocationSkin().getResourcePath().replace("skins/", ""));
                }
                return;
            }
            packDevEnabled = !packDevEnabled;
            if(packDevEnabled) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN+"Enabled pack developer mode."));
            } else {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED+"Disabled pack developer mode."));
            }
        }
    });

    SimpleCommand dnCommand = new SimpleCommand("dn", new SimpleCommand.ProcessCommandRunnable() {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/warp dungeon_hub");
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA+"Warping to:"+EnumChatFormatting.YELLOW+" Deez Nuts lmao"));
        }
    });

    SimpleCommand viewCataCommand = new SimpleCommand("cata", new SimpleCommand.ProcessCommandRunnable() {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            GuiProfileViewer.currentPage = GuiProfileViewer.ProfileViewerPage.DUNG;
            viewProfileRunnable.processCommand(sender, args);
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
            if(colourMap == null) {
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(
                        new ResourceLocation("notenoughupdates:maps/F1Full.json")).getInputStream(), StandardCharsets.UTF_8))) {
                    JsonObject json = NotEnoughUpdates.INSTANCE.manager.gson.fromJson(reader, JsonObject.class);

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
                } catch(Exception ignored) { }
            }

            if(!config.hidden.dev) {
                openGui = new GuiDungeonMapEditor();
                return;
            }

            if(args.length == 1 && args[0].equals("reset")) {
                colourMap = null;
                return;
            }

            if(args.length != 2) {
                openGui = new GuiDungeonMapEditor();
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
                            c = new Color(MapColor.mapColorArray[j / 4].getMapColor(j & 3), true);
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

                return;
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

                return;
            }

            openGui = new GuiDungeonMapEditor();
        }
    });

    SimpleCommand cosmeticsCommand = new SimpleCommand("neucosmetics", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if(Loader.isModLoaded("optifine") &&
                    new File(Minecraft.getMinecraft().mcDataDir, "optionsof.txt").exists()) {
                try(InputStream in = new FileInputStream(new File(Minecraft.getMinecraft().mcDataDir, "optionsof.txt"))) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

                    String line;
                    while((line = reader.readLine()) != null) {
                        if(line.contains("ofFastRender:true")) {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED +
                                    "NEU cosmetics do not work with OF Fast Render. Go to Video > Performance to disable it."));
                            return;
                        }
                    }
                } catch(Exception e) {
                }
            }

            openGui = new GuiCosmetics();
        }
    });

    SimpleCommand customizeCommand = new SimpleCommand("neucustomize", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();

            if(held == null) {
                sender.addChatMessage(new ChatComponentText("\u00a7cYou can't customize your hand..."));
                return;
            }

            String heldUUID = manager.getUUIDForItem(held);

            if(heldUUID == null) {
                sender.addChatMessage(new ChatComponentText("\u00a7cHeld item does not have UUID, cannot be customized"));
                return;
            }

            openGui = new GuiItemCustomize(held, heldUUID);
        }
    });

    SimpleCommand.ProcessCommandRunnable settingsRunnable = new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if(args.length > 0) {
                openGui = new GuiScreenElementWrapper(new NEUConfigEditor(config, StringUtils.join(args, " ")));
            } else {
                openGui = new GuiScreenElementWrapper(new NEUConfigEditor(config));
            }
        }
    };

    SimpleCommand settingsCommand = new SimpleCommand("neu", settingsRunnable);
    SimpleCommand settingsCommand2 = new SimpleCommand("neusettings", settingsRunnable);
    SimpleCommand settingsCommand3 = new SimpleCommand("neuconfig", settingsRunnable);

    SimpleCommand calendarCommand = new SimpleCommand("neucalendar", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            Minecraft.getMinecraft().thePlayer.closeScreen();
            CalendarOverlay.setEnabled(true);
            sendChatMessage("/calendar");
        }
    });

    SimpleCommand neuAhCommand = new SimpleCommand("neuah", new SimpleCommand.ProcessCommandRunnable() {
        public void processCommand(ICommandSender sender, String[] args) {
            if(!hasSkyblockScoreboard()) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED+
                        "You must be on Skyblock to use this feature."));
            } else if(config.apiKey.apiKey == null || config.apiKey.apiKey.trim().isEmpty()) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED+
                        "Can't open NeuAH, apikey is not set. Run /api new and put the result in settings."));
            } else {
                openGui = new CustomAHGui();
                manager.auctionManager.customAH.lastOpen = System.currentTimeMillis();
                manager.auctionManager.customAH.clearSearch();
                manager.auctionManager.customAH.updateSearch();

                if(args.length > 0) manager.auctionManager.customAH.setSearch(StringUtils.join(args, " "));
            }
        }
    });

    private Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    private File neuDir;

    /**
     * Instantiates NEUIo, NEUManager and NEUOverlay instances. Registers keybinds and adds a shutdown hook to clear tmp folder.
     * @param event
     */
    @EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        //if(!Minecraft.getMinecraft().getSession().getUsername().equalsIgnoreCase("moulberry")) throw new RuntimeException("moulbeBad");

        INSTANCE = this;

        String uuid = Minecraft.getMinecraft().getSession().getPlayerID();
        if(uuid.equalsIgnoreCase("ea9b1c5a-bf68-4fa2-9492-2d4e69693228")) throw new RuntimeException("Ding-dong, racism is wrong.");

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
        MinecraftForge.EVENT_BUS.register(new SBGamemodes());
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

        //ClientCommandHandler.instance.registerCommand(collectionLogCommand);
        //ClientCommandHandler.instance.registerCommand(collectionLogCommand2);
        ClientCommandHandler.instance.registerCommand(nullzeeSphereCommand);
        ClientCommandHandler.instance.registerCommand(cosmeticsCommand);
        ClientCommandHandler.instance.registerCommand(linksCommand);
        ClientCommandHandler.instance.registerCommand(gamemodesCommand);
        ClientCommandHandler.instance.registerCommand(stWhyCommand);
        ClientCommandHandler.instance.registerCommand(buttonsCommand);
        ClientCommandHandler.instance.registerCommand(resetRepoCommand);
        ClientCommandHandler.instance.registerCommand(reloadRepoCommand);
        //ClientCommandHandler.instance.registerCommand(itemRenameCommand);
        ClientCommandHandler.instance.registerCommand(joinDungeonCommand);
        ClientCommandHandler.instance.registerCommand(viewProfileCommand);
        ClientCommandHandler.instance.registerCommand(viewProfileShortCommand);
        ClientCommandHandler.instance.registerCommand(dhCommand);
        ClientCommandHandler.instance.registerCommand(dnCommand);
        ClientCommandHandler.instance.registerCommand(customizeCommand);
        ClientCommandHandler.instance.registerCommand(devTestCommand);
        ClientCommandHandler.instance.registerCommand(packDevCommand);
        if(!Loader.isModLoaded("skyblockextras")) ClientCommandHandler.instance.registerCommand(viewCataCommand);
        ClientCommandHandler.instance.registerCommand(peekCommand);
        ClientCommandHandler.instance.registerCommand(tutorialCommand);
        ClientCommandHandler.instance.registerCommand(overlayPlacementsCommand);
        ClientCommandHandler.instance.registerCommand(enchantColourCommand);
        ClientCommandHandler.instance.registerCommand(neuAhCommand);
        ClientCommandHandler.instance.registerCommand(pcStatsCommand);
        ClientCommandHandler.instance.registerCommand(neumapCommand);
        ClientCommandHandler.instance.registerCommand(settingsCommand);
        ClientCommandHandler.instance.registerCommand(settingsCommand2);
        ClientCommandHandler.instance.registerCommand(settingsCommand3);
        ClientCommandHandler.instance.registerCommand(dungeonWinTest);
        ClientCommandHandler.instance.registerCommand(calendarCommand);
        ClientCommandHandler.instance.registerCommand(new FairySouls.FairySoulsCommand());

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
