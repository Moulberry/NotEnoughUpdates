package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.miscfeatures.ItemCooldowns;
import io.github.moulberry.notenoughupdates.miscfeatures.ItemRarityHalo;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({RenderItem.class})
public abstract class MixinRenderItem {

    private static void func_181565_a(WorldRenderer w, int x, int y, float width, int height,
                               int r, int g, int b, int a) {
        w.begin(7, DefaultVertexFormats.POSITION_COLOR);
        w.pos((x + 0), (y + 0), 0.0D)
                .color(r, g, b, a).endVertex();
        w.pos((x + 0), (y + height), 0.0D)
                .color(r, g, b, a).endVertex();
        w.pos((x + width), (y + height), 0.0D)
                .color(r, g, b, a).endVertex();
        w.pos((x + width), (y + 0), 0.0D)
                .color(r, g, b, a).endVertex();
        Tessellator.getInstance().draw();
    }

    @Inject(method="renderItemOverlayIntoGUI", at=@At("RETURN"))
    public void renderItemOverlayIntoGUI(FontRenderer fr, ItemStack stack, int xPosition, int yPosition, String text, CallbackInfo ci) {
        if(stack == null) return;

        float damageOverride = ItemCooldowns.getDurabilityOverride(stack);

        if(damageOverride >= 0) {
            float barX = 13.0f - damageOverride * 13.0f;
            int col = (int)Math.round(255.0D - damageOverride * 255.0D);
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.disableTexture2D();
            GlStateManager.disableAlpha();
            GlStateManager.disableBlend();

            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            func_181565_a(worldrenderer, xPosition + 2, yPosition + 13, 13, 2, 0, 0, 0, 255);
            func_181565_a(worldrenderer, xPosition + 2, yPosition + 13, 12, 1, (255 - col) / 4, 64, 0, 255);
            func_181565_a(worldrenderer, xPosition + 2, yPosition + 13, barX, 1, 255 - col, col, 0, 255);

            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
        }
    }

}
