package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.NEUEventListener;
import io.github.moulberry.notenoughupdates.NEUOverlay;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.miscfeatures.ItemCooldowns;
import io.github.moulberry.notenoughupdates.miscfeatures.ItemRarityHalo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

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

    @Inject(method="renderItemIntoGUI", at=@At("HEAD"))
    public void renderItemHead(ItemStack stack, int x, int y, CallbackInfo ci) {
        if(NotEnoughUpdates.INSTANCE.overlay.searchMode && NEUEventListener.drawingGuiScreen) {
            boolean matches = false;

            GuiTextField textField = NotEnoughUpdates.INSTANCE.overlay.getTextField();

            if(textField.getText().trim().isEmpty()) {
                matches = true;
            } else if(stack != null) {
                for(String search : textField.getText().split("\\|")) {
                    matches |= NotEnoughUpdates.INSTANCE.manager.doesStackMatchSearch(stack, search.trim());
                }
            }
            if(matches) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0, 100 + Minecraft.getMinecraft().getRenderItem().zLevel);
                GlStateManager.depthMask(false);
                Gui.drawRect(x, y, x+16, y+16, NEUOverlay.overlayColourLight);
                GlStateManager.depthMask(true);
                GlStateManager.popMatrix();
            }
        }
    }

    @Inject(method="renderItemIntoGUI", at=@At("RETURN"))
    public void renderItemReturn(ItemStack stack, int x, int y, CallbackInfo ci) {
        if(stack != null && stack.stackSize != 1) return;
        if(NotEnoughUpdates.INSTANCE.overlay.searchMode && NEUEventListener.drawingGuiScreen) {
            boolean matches = false;

            GuiTextField textField = NotEnoughUpdates.INSTANCE.overlay.getTextField();

            if(textField.getText().trim().isEmpty()) {
                matches = true;
            } else if(stack != null) {
                for(String search : textField.getText().split("\\|")) {
                    matches |= NotEnoughUpdates.INSTANCE.manager.doesStackMatchSearch(stack, search.trim());
                }
            }
            if(!matches) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0, 110 + Minecraft.getMinecraft().getRenderItem().zLevel);
                Gui.drawRect(x, y, x+16, y+16, NEUOverlay.overlayColourDark);
                GlStateManager.popMatrix();
            }
        }
    }

    @Inject(method="renderItemOverlayIntoGUI", at=@At("RETURN"))
    public void renderItemOverlayIntoGUI(FontRenderer fr, ItemStack stack, int xPosition, int yPosition, String text, CallbackInfo ci) {
        if(stack != null && stack.stackSize != 1) {
            if(NotEnoughUpdates.INSTANCE.overlay.searchMode && NEUEventListener.drawingGuiScreen) {
                boolean matches = false;

                GuiTextField textField = NotEnoughUpdates.INSTANCE.overlay.getTextField();

                if(textField.getText().trim().isEmpty()) {
                    matches = true;
                } else {
                    for(String search : textField.getText().split("\\|")) {
                        matches |= NotEnoughUpdates.INSTANCE.manager.doesStackMatchSearch(stack, search.trim());
                    }
                }
                if(!matches) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(0, 0, 110 + Minecraft.getMinecraft().getRenderItem().zLevel);
                    GlStateManager.disableDepth();
                    Gui.drawRect(xPosition, yPosition, xPosition+16, yPosition+16, NEUOverlay.overlayColourDark);
                    GlStateManager.enableDepth();
                    GlStateManager.popMatrix();
                }
            }
        }

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
