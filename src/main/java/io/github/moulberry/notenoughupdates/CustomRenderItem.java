package io.github.moulberry.notenoughupdates;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3i;

import java.lang.reflect.Method;
import java.util.List;

public class CustomRenderItem {

    public static void renderItemIntoGUI(RenderItem renderItem, ItemStack stack, int x, int y, boolean effect) {
        IBakedModel ibakedmodel = renderItem.getItemModelMesher().getItemModel(stack);
        GlStateManager.pushMatrix();
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        setupGuiTransform(x, y, ibakedmodel.isGui3d());
        ibakedmodel = net.minecraftforge.client.ForgeHooksClient.handleCameraTransforms(ibakedmodel, ItemCameraTransforms.TransformType.GUI);
        renderItem(renderItem, stack, ibakedmodel, effect);
        GlStateManager.disableAlpha();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
    }

    private static void setupGuiTransform(int xPosition, int yPosition, boolean isGui3d) {
        GlStateManager.translate((float)xPosition, (float)yPosition, 0);
        GlStateManager.translate(8.0F, 8.0F, 0.0F);
        GlStateManager.scale(1.0F, 1.0F, -1.0F);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);

        if (isGui3d) {
            GlStateManager.scale(40.0F, 40.0F, 40.0F);
            GlStateManager.rotate(210.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.enableLighting();
        } else {
            GlStateManager.scale(64.0F, 64.0F, 64.0F);
            GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.disableLighting();
        }
    }

    public static void renderItem(RenderItem renderItem, ItemStack stack, IBakedModel model, boolean effect) {
        if (stack != null) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5F, 0.5F, 0.5F);

            if (model.isBuiltInRenderer()) {
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.translate(-0.5F, -0.5F, -0.5F);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.enableRescaleNormal();
                TileEntityItemStackRenderer.instance.renderByItem(stack);
            } else {
                GlStateManager.translate(-0.5F, -0.5F, -0.5F);
                Class<?>[] paramsRM = new Class[]{IBakedModel.class, ItemStack.class};
                Method renderModelMethod = Utils.getMethod(RenderItem.class, paramsRM,
                        "renderModel", "func_175036_a");
                if(renderModelMethod != null) {
                    renderModelMethod.setAccessible(true);
                    try {
                        renderModelMethod.invoke(renderItem, model, stack);
                    } catch(Exception e) {}
                }

                if (stack.hasEffect() && effect) {
                    Class<?>[] paramsRE = new Class[]{IBakedModel.class};
                    Method renderEffectMethod = Utils.getMethod(RenderItem.class, paramsRE,
                            "renderEffect", "func_180451_a");
                    if(renderEffectMethod != null) {
                        renderEffectMethod.setAccessible(true);
                        try {
                            renderEffectMethod.invoke(renderItem, model);
                        } catch(Exception e) {}
                    }
                }
            }

            GlStateManager.popMatrix();
        }
    }

    /*private static void renderModel(IBakedModel model, ItemStack stack)
    {
        renderModel(model, -1, stack);
    }

    private static void renderModel(IBakedModel model, int color)
    {
        renderModel(model, color, (ItemStack)null);
    }

    private static void renderModel(IBakedModel model, int color, ItemStack stack)
    {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.ITEM);

        for (EnumFacing enumfacing : EnumFacing.values())
        {
            renderQuads(worldrenderer, model.getFaceQuads(enumfacing), color, stack);
        }

        renderQuads(worldrenderer, model.getGeneralQuads(), color, stack);
        tessellator.draw();
    }

    private static void putQuadNormal(WorldRenderer renderer, BakedQuad quad)
    {
        Vec3i vec3i = quad.getFace().getDirectionVec();
        renderer.putNormal((float)vec3i.getX(), (float)vec3i.getY(), (float)vec3i.getZ());
    }

    private static void renderQuad(WorldRenderer renderer, BakedQuad quad, int color)
    {
        renderer.addVertexData(quad.getVertexData());
        renderer.putColor4(color);
        putQuadNormal(renderer, quad);
    }

    private static void renderQuads(WorldRenderer renderer, List<BakedQuad> quads, int color, ItemStack stack)
    {
        boolean flag = color == -1 && stack != null;
        int i = 0;

        for (int j = quads.size(); i < j; ++i)
        {
            BakedQuad bakedquad = (BakedQuad)quads.get(i);
            int k = color;

            if (flag && bakedquad.hasTintIndex())
            {
                k = stack.getItem().getColorFromItemStack(stack, bakedquad.getTintIndex());

                if (EntityRenderer.anaglyphEnable)
                {
                    k = TextureUtil.anaglyphColor(k);
                }

                k = k | -16777216;
            }

            net.minecraftforge.client.model.pipeline.LightUtil.renderQuadColor(renderer, bakedquad, k);
        }
    }*/

}
