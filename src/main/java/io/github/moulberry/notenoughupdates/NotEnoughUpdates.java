package io.github.moulberry.notenoughupdates;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Session;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import javax.swing.*;
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

@Mod(modid = NotEnoughUpdates.MODID, version = NotEnoughUpdates.VERSION)
public class NotEnoughUpdates {
    public static final String MODID = "notenoughupdates";
    public static final String VERSION = "1.0.0";

    private NEUManager manager;
    private NEUOverlay overlay;
    private NEUIO neuio;

    private static final long CHAT_MSG_COOLDOWN = 200;
    private long lastChatMessage = 0;
    private String currChatMessage = null;

    private boolean hoverInv = false;
    private boolean focusInv = false;

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

    @EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);

        File f = new File(event.getModConfigurationDirectory(), "notenoughupdates");
        f.mkdirs();
        neuio = new NEUIO(getAccessToken());
        manager = new NEUManager(this, neuio, f);
        manager.loadItemInformation();
        overlay = new NEUOverlay(manager);

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

    public void sendChatMessage(String message) {
        if (System.currentTimeMillis() - lastChatMessage > CHAT_MSG_COOLDOWN) {
            lastChatMessage = System.currentTimeMillis();
            Minecraft.getMinecraft().thePlayer.sendChatMessage(message);
            currChatMessage = null;
        } else {
            currChatMessage = message;
        }
    }

    @EventHandler
    public void onTick(TickEvent.ClientTickEvent event) {
        if(currChatMessage != null && System.currentTimeMillis() - lastChatMessage > CHAT_MSG_COOLDOWN) {
            lastChatMessage = System.currentTimeMillis();
            Minecraft.getMinecraft().thePlayer.sendChatMessage(currChatMessage);
            currChatMessage = null;
        }
    }

    AtomicBoolean missingRecipe = new AtomicBoolean(false);
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if(event.gui != null) {
            System.out.println("2");
            if(event.gui instanceof GuiChest) {
                GuiChest eventGui = (GuiChest) event.gui;
                ContainerChest cc = (ContainerChest) eventGui.inventorySlots;
                IInventory lower = cc.getLowerChestInventory();
                System.out.println("3");
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
        //OPEN
        if(Minecraft.getMinecraft().currentScreen == null
                && event.gui instanceof GuiContainer) {
            overlay.reset();
        }
        //CLOSE
        if(Minecraft.getMinecraft().currentScreen != null && event.gui == null) {
            try {
                manager.saveConfig();
            } catch(IOException e) {}
        }
    }

    @SubscribeEvent
    public void onGuiChat(ClientChatReceivedEvent e) {
        String r = null;
        String unformatted = e.message.getUnformattedText().replaceAll("(?i)\\u00A7.", "");
        if(unformatted.startsWith("You are playing on profile: ")) {
            manager.currentProfile = unformatted.substring("You are playing on profile: ".length()).split(" ")[0].trim();
        } else if(unformatted.startsWith("Your profile was changed to: ")) {//Your profile was changed to:
            manager.currentProfile = unformatted.substring("Your profile was changed to: ".length()).split(" ")[0].trim();
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
    }

    @SubscribeEvent
    public void onGuiBackgroundDraw(GuiScreenEvent.BackgroundDrawnEvent event) {
        if(event.gui instanceof GuiContainer && isOnSkyblock()) {
            ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
            int width = scaledresolution.getScaledWidth();

            boolean hoverPane = event.getMouseX() < width*overlay.getInfoPaneOffsetFactor() ||
                    event.getMouseX() > width*overlay.getItemPaneOffsetFactor();
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

    @SubscribeEvent
    public void onGuiScreenDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if(event.gui instanceof GuiContainer && isOnSkyblock()) {
            if(!focusInv) {
                GL11.glTranslatef(0, 0, 300);
                overlay.render(event.mouseX, event.mouseY, hoverInv && focusInv);
                GL11.glTranslatef(0, 0, -300);
            }
            overlay.renderOverlay(event.mouseX, event.mouseY);
        }
    }

    @SubscribeEvent
    public void onGuiScreenMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if(event.gui instanceof GuiContainer && !(hoverInv && focusInv) && isOnSkyblock()) {
            if(overlay.mouseInput()) {
                event.setCanceled(true);
            }
        }
    }

    ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);

    boolean started = false;
    @SubscribeEvent
    public void onGuiScreenKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if(manager.config.enableItemEditing.value && Minecraft.getMinecraft().theWorld != null &&
                Keyboard.getEventKey() == Keyboard.KEY_O && Keyboard.getEventKeyState()) {
            GuiScreen gui = Minecraft.getMinecraft().currentScreen;
            if(gui != null && gui instanceof GuiChest) {
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
        if(event.gui instanceof GuiContainer && isOnSkyblock()) {
            if(overlay.keyboardInput(focusInv)) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * This was code leftover from testing but it ended up in the final mod so I guess its staying here.
     * This makes it so that holding LCONTROL while hovering over an item with NBT will show the NBT of the item.
     * @param event
     */
    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if(Minecraft.getMinecraft().currentScreen != null) {
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
        }
        if(!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || !manager.config.dev.value) return;
        if(event.toolTip.get(event.toolTip.size()-1).startsWith(EnumChatFormatting.DARK_GRAY + "NBT: ")) {
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
        }
    }

    //Stolen from Biscut's SkyblockAddons
    public boolean isOnSkyblock() {
        if(!manager.config.onlyShowOnSkyblock.value) return true;

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
