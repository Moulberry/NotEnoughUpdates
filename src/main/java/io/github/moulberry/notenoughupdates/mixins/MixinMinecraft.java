package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow public WorldClient theWorld;

    @Shadow public EntityRenderer entityRenderer;

    @Inject(method="shutdownMinecraftApplet", at=@At("HEAD"))
    public void shutdownMinecraftApplet(CallbackInfo ci) {
        NotEnoughUpdates.INSTANCE.saveConfig();
    }

    @Inject(method="loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at=@At("HEAD"))
    public void onLoadWorld(WorldClient worldClientIn, String loadingMessage, CallbackInfo ci) {
        if(worldClientIn != theWorld) {
            entityRenderer.getMapItemRenderer().clearLoadedMaps();
        }
    }

    @Redirect(method="loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",
            at=@At(value = "INVOKE", target = "Ljava/lang/System;gc()V"))
    public void loadWorld_gc() {
    }

}
