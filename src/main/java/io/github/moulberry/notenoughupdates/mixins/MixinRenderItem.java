package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.ItemRarityHalo;
import io.github.moulberry.notenoughupdates.NEUResourceManager;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.vecmath.Vector3f;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

@Mixin({RenderItem.class})
public abstract class MixinRenderItem {

    //Lnet/minecraft/client/renderer/entity/RenderItem;renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/resources/model/IBakedModel;)V
    @Inject(method="Lnet/minecraft/client/renderer/entity/RenderItem;renderItemIntoGUI(Lnet/minecraft/item/ItemStack;II)V",
            at=@At("HEAD"), cancellable = true)
    public void renderItemIntoGUI(ItemStack stack, int x, int y, CallbackInfo ci) {
        if(x == 0 && y == 0 || true) return;
        ItemRarityHalo.onItemRender(stack, x, y);

        if(Keyboard.isKeyDown(Keyboard.KEY_H)) ci.cancel();
    }
}
