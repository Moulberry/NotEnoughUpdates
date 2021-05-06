package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.miscfeatures.CustomItemEffects;
import io.github.moulberry.notenoughupdates.miscgui.InventoryStorageSelector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {


    @Redirect(method="renderWorldPass", at=@At(
            value="INVOKE",
            target = "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderLast(Lnet/minecraft/client/renderer/RenderGlobal;F)V",
            remap = false)
    )
    public void renderWorldPass_dispatchRenderLast(RenderGlobal context, float partialTicks) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
            double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)partialTicks;
            double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTicks;
            double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)partialTicks;

            GlStateManager.translate(-currentPosition.x + d0, -currentPosition.y + d1, -currentPosition.z + d2);
            ForgeHooksClient.dispatchRenderLast(context, partialTicks);
            GlStateManager.translate(currentPosition.x - d0, currentPosition.y - d1, currentPosition.z - d2);
        } else {
            ForgeHooksClient.dispatchRenderLast(context, partialTicks);
        }
    }

    //orientCamera
    @ModifyVariable(method="orientCamera", at=@At(value="STORE"), ordinal = 0)
    public double orientCamera_d0(double d0) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.x;
        }
        return d0;
    }

    @ModifyVariable(method="orientCamera", at=@At(value="STORE"), ordinal = 1)
    public double orientCamera_d1(double d1) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.y;
        }
        return d1;
    }

    @ModifyVariable(method="orientCamera", at=@At(value="STORE"), ordinal = 2)
    public double orientCamera_d2(double d2) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.z;
        }
        return d2;
    }

    //renderWorldPass
    @ModifyVariable(method="renderWorldPass", at=@At(value="STORE"), ordinal = 0)
    public double renderWorldPass_d0(double d0) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.x;
        }
        return d0;
    }

    @ModifyVariable(method="renderWorldPass", at=@At(value="STORE"), ordinal = 1)
    public double renderWorldPass_d1(double d1) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.y;
        }
        return d1;
    }

    @ModifyVariable(method="renderWorldPass", at=@At(value="STORE"), ordinal = 2)
    public double renderWorldPass_d2(double d2) {
        Vector3f currentPosition = CustomItemEffects.INSTANCE.getCurrentPosition();
        if(currentPosition != null) {
            return currentPosition.z;
        }
        return d2;
    }

}
