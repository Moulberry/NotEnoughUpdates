package io.github.moulberry.notenoughupdates.miscfeatures;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;

public class FlyFix {

    private static boolean serverWantFly = false;
    private static boolean clientWantFly = false;
    private static long lastAbilitySend = 0;

    public static void onSendAbilities(C13PacketPlayerAbilities packet) {
        if(true) return;
        //if(!NotEnoughUpdates.INSTANCE.config.misc.flyFix) return;
        if(Minecraft.getMinecraft().thePlayer == null) return;
        if(!Minecraft.getMinecraft().thePlayer.capabilities.allowFlying) return;
        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return;
        if(SBInfo.getInstance().getLocation() == null) return;
        if(!SBInfo.getInstance().getLocation().equals("dynamic")) return;

        long currentTime = System.currentTimeMillis();

        clientWantFly = packet.isFlying();
        if(clientWantFly != serverWantFly) lastAbilitySend = currentTime;
    }

    public static void onReceiveAbilities(S39PacketPlayerAbilities packet) {
        if(true) return;
        //if(!NotEnoughUpdates.INSTANCE.config.misc.flyFix) return;
        if(Minecraft.getMinecraft().thePlayer == null) return;
        if(!Minecraft.getMinecraft().thePlayer.capabilities.allowFlying) return;
        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return;
        if(SBInfo.getInstance().getLocation() == null) return;
        if(!SBInfo.getInstance().getLocation().equals("dynamic")) return;

        long currentTime = System.currentTimeMillis();

        serverWantFly = packet.isFlying();

        if(serverWantFly != clientWantFly) {
            if(currentTime - lastAbilitySend > 0 && currentTime - lastAbilitySend < 500) {
                packet.setFlying(clientWantFly);
            } else {
                clientWantFly = serverWantFly;
            }
        }
    }

    public static void tick() {
        if(true) return;
        //if(!NotEnoughUpdates.INSTANCE.config.misc.flyFix) return;
        if(Minecraft.getMinecraft().thePlayer == null) return;
        if(!Minecraft.getMinecraft().thePlayer.capabilities.allowFlying) return;
        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return;
        if(SBInfo.getInstance().getLocation() == null) return;
        if(!SBInfo.getInstance().getLocation().equals("dynamic")) return;

        long currentTime = System.currentTimeMillis();

        if(currentTime - lastAbilitySend > 1000 && currentTime - lastAbilitySend < 5000) {
            if(clientWantFly != serverWantFly) {
                Minecraft.getMinecraft().thePlayer.capabilities.isFlying = serverWantFly;
                Minecraft.getMinecraft().thePlayer.sendPlayerAbilities();
                clientWantFly = serverWantFly;
            }
        } else {
            clientWantFly = Minecraft.getMinecraft().thePlayer.capabilities.isFlying;
        }
    }

}
