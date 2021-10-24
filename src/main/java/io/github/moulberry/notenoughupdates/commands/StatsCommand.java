package io.github.moulberry.notenoughupdates.commands;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.DiscordMarkdownBuilder;
import io.github.moulberry.notenoughupdates.util.HastebinUploader;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsCommand extends ClientCommandBase {

    private final ExecutorService threadPool = Executors.newFixedThreadPool(1);

    public StatsCommand() {
        super("neustats");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0){
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "modlist":
                    clipboardAndSendMessage(createModList(new DiscordMarkdownBuilder()).toString());
                    break;
                case "dump":
                    modPrefixedMessage(EnumChatFormatting.GREEN + "This will upload a dump of the java classes your game has loaded how big they are and how many there are. This can take a few seconds as it is uploading to HasteBin.");
                    threadPool.submit(() -> {
                        try {
                            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                            final ObjectName objectName = ObjectName.getInstance("com.sun.management:type=DiagnosticCommand");
                            final DiagnosticCommandMXBean proxy = JMX.newMXBeanProxy(server, objectName, DiagnosticCommandMXBean.class);
                            clipboardAndSendMessage(HastebinUploader.upload(proxy.gcClassHistogram(new String[0]).replace("[", "[]"), HastebinUploader.Mode.NORMAL));
                        }catch (Exception e){
                            clipboardAndSendMessage(null);
                        }
                    });
                    break;
            }
        } else {
            clipboardAndSendMessage(createStats());
        }

    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "modlist", "dump") : null;
    }

    public interface DiagnosticCommandMXBean {
        String gcClassHistogram(String[] array);
    }

    private static void clipboardAndSendMessage(String data) {
        if (data == null) {
            modPrefixedMessage(EnumChatFormatting.DARK_RED + "Error occurred trying to perform command.");
            return;
        }
        try {
            StringSelection clipboard = new StringSelection(data);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(clipboard, clipboard);
            modPrefixedMessage(EnumChatFormatting.GREEN + "Dev info copied to clipboard.");
        } catch (Exception ignored) {
            modPrefixedMessage(EnumChatFormatting.DARK_RED + "Could not copy to clipboard.");
        }
    }

    private static void modPrefixedMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "[" + EnumChatFormatting.RED + "NotEnoughUpdates" + EnumChatFormatting.GOLD + "]: " + message));

    }

    private static String createStats() {
        DiscordMarkdownBuilder builder = new DiscordMarkdownBuilder();
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long currentMemory = totalMemory - freeMemory;
        int activeModCount = Loader.instance().getActiveModList().size();

        builder.category("System Stats");
        builder.append("OS", System.getProperty("os.name"));
        builder.append("CPU", OpenGlHelper.getCpu());
        builder.append("Display", String.format("%dx%d (%s)", Display.getWidth(), Display.getHeight(), GL11.glGetString(GL11.GL_VENDOR)));
        builder.append("GPU", GL11.glGetString(GL11.GL_RENDERER));
        builder.append("GPU Driver", GL11.glGetString(GL11.GL_VERSION));
        if(getMemorySize() > 0) builder.append("Maximum Memory", (getMemorySize() / 1024L / 1024L) + "MB");
        builder.append("Shaders", (""+OpenGlHelper.isFramebufferEnabled()).toUpperCase());
        builder.category("Java Stats");
        builder.append("Java", String.format("%s %dbit", System.getProperty("java.version"), Minecraft.getMinecraft().isJava64bit() ? 64 : 32));
        builder.append("Memory", String.format("% 2d%% %03d/%03dMB", currentMemory * 100L / maxMemory, currentMemory / 1024L / 1024L, maxMemory / 1024L / 1024L));
        builder.append("Memory Allocated", String.format("% 2d%% %03dMB", totalMemory * 100L / maxMemory, totalMemory / 1024L / 1024L));
        builder.category("Game Stats");
        builder.append("FPS", String.valueOf(Minecraft.getDebugFPS()));
        builder.append("Loaded Mods", String.valueOf(activeModCount));
        builder.append("Forge", ForgeVersion.getVersion());
        builder.category("Neu Settings"); 
        builder.append("API Key", NotEnoughUpdates.INSTANCE.config.apiKey.apiKey.isEmpty() ? "FALSE" : "TRUE");
        builder.append("On Skyblock", NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() ? "TRUE" : "FALSE");
        builder.append("Mod Version", Loader.instance().getIndexedModList().get(NotEnoughUpdates.MODID).getSource().getName());
        builder.append("SB Profile", SBInfo.getInstance().currentProfile);
        builder.category("Repo Stats");
        builder.append("Last Commit", NotEnoughUpdates.INSTANCE.manager.latestRepoCommit);
        builder.append("Loaded Items", String.valueOf(NotEnoughUpdates.INSTANCE.manager.getItemInformation().size()));
        if (activeModCount <= 15) createModList(builder);

        return builder.toString();
    }

    private static DiscordMarkdownBuilder createModList(DiscordMarkdownBuilder builder) {
        builder.category("Mods Loaded");
        Loader.instance().getActiveModList().forEach(mod -> builder.append(mod.getName(), mod.getSource().getName()));
        return builder;
    }

    private static long getMemorySize(){
        try {
            return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
        } catch(Exception e){
            try {
                return ((com.sun.management.UnixOperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
            } catch(Exception ignored){/*IGNORE*/}
        }
        return -1;
    }

}
