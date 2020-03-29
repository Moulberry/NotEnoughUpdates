package io.github.moulberry.notenoughupdates;

import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Session;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.Proxy;
import java.util.Scanner;
import java.util.UUID;

@Mod(modid = NotEnoughUpdates.MODID, version = NotEnoughUpdates.VERSION)
public class NotEnoughUpdates {
    public static final String MODID = "notenoughupdates";
    public static final String VERSION = "1.0.0";

    private NEUManager manager;
    private NEUOverlay overlay;
    private NEUIO neuio;
    
    @EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);

        File f = new File(event.getModConfigurationDirectory(), "notenoughupdates");
        f.mkdirs();
        //Github Access Token, may change. Value hard-coded.
        neuio = new NEUIO("c49fcad378ae46e5c08bf6b0c5502f7e4830bfef");
        manager = new NEUManager(neuio, f);
        manager.loadItemInformation();
        overlay = new NEUOverlay(manager);

        //TODO: login code. Ignore this, used for testing.
        /*try {
            Field field = Minecraft.class.getDeclaredField("session");
            YggdrasilUserAuthentication auth = (YggdrasilUserAuthentication)
                    new YggdrasilAuthenticationService(Proxy.NO_PROXY, UUID.randomUUID().toString())
                            .createUserAuthentication(Agent.MINECRAFT);
            auth.setUsername("...");
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
            e.printStackTrace();
        }*/

    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if(Minecraft.getMinecraft().currentScreen == null
                && event.gui instanceof GuiContainer) {
            overlay.reset();
        }
    }

    @SubscribeEvent
    public void onGuiScreenDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if(event.gui instanceof GuiContainer) {
            overlay.render(event.renderPartialTicks, event.mouseX, event.mouseY);
        }
    }

    @SubscribeEvent
    public void onGuiScreenMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if(event.gui instanceof GuiContainer) {
            if(overlay.mouseInput()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onGuiScreenKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if(event.gui instanceof GuiContainer) {
            if(overlay.keyboardInput()) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * This was code leftover from testing but it ended up in the final mod so I guess its staying here.
     * This makes it so that holding LCONTROL while hovering over an item with NBT will show the NBT of the item.
     * Should probably have this disabled by default via config.
     * @param event
     */
    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if(!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) return;
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
}
