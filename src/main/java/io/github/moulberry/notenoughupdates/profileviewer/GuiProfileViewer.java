package io.github.moulberry.notenoughupdates.profileviewer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.TexLoc;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class GuiProfileViewer extends GuiScreen {

    public static final ResourceLocation pv_basic = new ResourceLocation("notenoughupdates:pv_basic.png");

    private final ProfileViewer.Profile profile;
    private ProfileViewerPage currentPage = ProfileViewerPage.BASIC;
    private int sizeX;
    private int sizeY;
    private int guiLeft;
    private int guiTop;

    public enum ProfileViewerPage {
        BASIC
    }

    public GuiProfileViewer(ProfileViewer.Profile profile) {
        this.profile = profile;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawDefaultBackground();

        switch (currentPage) {
            case BASIC:
                drawBasicPage(mouseX, mouseY, partialTicks);
                break;
        }

    }

    private String niceUuid(String uuidStr) {
        if(uuidStr.length()!=32) return uuidStr;

        StringBuilder niceAucId = new StringBuilder();
        niceAucId.append(uuidStr, 0, 8);
        niceAucId.append("-");
        niceAucId.append(uuidStr, 8, 12);
        niceAucId.append("-");
        niceAucId.append(uuidStr, 12, 16);
        niceAucId.append("-");
        niceAucId.append(uuidStr, 16, 20);
        niceAucId.append("-");
        niceAucId.append(uuidStr, 20, 32);
        return niceAucId.toString();
    }

    private EntityOtherPlayerMP entityPlayer = null;
    private ResourceLocation playerLocationSkin = null;
    private ResourceLocation playerLocationCape = null;
    private String skinType = null;

    TexLoc tl = new TexLoc(0, 0, Keyboard.KEY_M);
    TexLoc tl2 = new TexLoc(0, 0, Keyboard.KEY_B);
    TexLoc tl3 = new TexLoc(0, 0, Keyboard.KEY_J);
    private void drawBasicPage(int mouseX, int mouseY, float partialTicks) {
        this.sizeX = 431;
        this.sizeY = 202;
        this.guiLeft = (this.width-this.sizeX)/2;
        this.guiTop = (this.height-this.sizeY)/2;

        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_basic);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);

        tl.handleKeyboardInput();
        tl2.handleKeyboardInput();
        tl3.handleKeyboardInput();

        if(entityPlayer == null) {
            UUID playerUUID = UUID.fromString(niceUuid(profile.getUuid()));
            GameProfile fakeProfile = Minecraft.getMinecraft().getSessionService().fillProfileProperties(new GameProfile(playerUUID, "CoolGuy123"), false);
            for(Property prop : fakeProfile.getProperties().get("textures")) {
                System.out.println(new String(Base64.decodeBase64(prop.getValue()), Charsets.UTF_8));
            }
            entityPlayer = new EntityOtherPlayerMP(Minecraft.getMinecraft().theWorld, fakeProfile) {
                public ResourceLocation getLocationSkin() {
                    return playerLocationSkin == null ? DefaultPlayerSkin.getDefaultSkin(this.getUniqueID()) : playerLocationSkin;
                }

                public ResourceLocation getLocationCape() {
                    return playerLocationCape;
                }

                public String getSkinType() {
                    return skinType == null ? DefaultPlayerSkin.getSkinType(this.getUniqueID()) : skinType;
                }
            };
        }

        JsonObject profileInfo = profile.getProfileInformation(null);
        if(profileInfo == null) return;

        JsonObject skillInfo = profile.getSkillInfo(null);
        JsonObject inventoryInfo = profile.getInventoryInfo(null);
        JsonObject collectionInfo =profile. getCollectionInfo(null);

        if(inventoryInfo != null && inventoryInfo.has("inv_armor")) {
            JsonArray items = inventoryInfo.get("inv_armor").getAsJsonArray();
            for(int i=0; i<entityPlayer.inventory.armorInventory.length; i++) {
                JsonElement itemElement = items.get(i);
                if(itemElement != null && itemElement.isJsonObject()) {
                    entityPlayer.inventory.armorInventory[i] = NotEnoughUpdates.INSTANCE.manager.jsonToStack(itemElement.getAsJsonObject(), false);
                }
            }
        }
        if(playerLocationSkin == null) {
            try {
                Minecraft.getMinecraft().getSkinManager().loadProfileTextures(entityPlayer.getGameProfile(), new SkinManager.SkinAvailableCallback() {
                    public void skinAvailable(MinecraftProfileTexture.Type type, ResourceLocation location, MinecraftProfileTexture profileTexture) {
                        switch (type) {
                            case SKIN:
                                playerLocationSkin = location;
                                skinType = profileTexture.getMetadata("model");

                                if(skinType == null) {
                                    skinType = "default";
                                }

                                break;
                            case CAPE:
                                playerLocationCape = location;
                        }
                    }
                }, false);
            } catch(Exception e){}
        }

        drawEntityOnScreen(guiLeft+63, guiTop+129, 30, guiLeft+63-mouseX, guiTop+129-mouseY, entityPlayer);


        PlayerStats.Stats stats = profile.getStats(null);

        for(int i=0; i<PlayerStats.defaultStatNames.length; i++) {
            String statName = PlayerStats.defaultStatNames[i];
            String statNamePretty = PlayerStats.defaultStatNamesPretty[i];

            int val = Math.round(stats.get(statName));



            for(int xOff=-1; xOff<=1; xOff++) {
                for(int yOff=-1; yOff<=1; yOff++) {
                    if(Math.abs(xOff) != Math.abs(yOff)) {
                        //Utils.drawStringCenteredScaledMaxWidth(Utils.cleanColourNotModifiers(statNamePretty) + " " + val, Minecraft.getMinecraft().fontRendererObj,
                        //        guiLeft+172f+xOff, guiTop+36+12.5f*i+yOff, false, 85, new Color(100, 100, 100, 100).getRGB());
                    }
                }
            }

            GlStateManager.color(1, 1, 1, 1);
            Utils.drawStringCenteredScaledMaxWidth(statNamePretty + " " + EnumChatFormatting.WHITE + val, Minecraft.getMinecraft().fontRendererObj,
                    guiLeft+172f, guiTop+36+12.5f*i, true, 85, Color.BLACK.getRGB());
        }

        int position = 0;
        for(Map.Entry<String, String> entry : skillToSkillNameMap.entrySet()) {
            int yPosition = position % 7;
            int xPosition = position / 7;

            String skillName = entry.getValue();

            float level = (int)skillInfo.get(entry.getKey()).getAsFloat();

            for(int xOff=-1; xOff<=1; xOff++) {
                for(int yOff=-1; yOff<=1; yOff++) {
                    if(Math.abs(xOff) != Math.abs(yOff)) {
                        //Utils.drawStringCenteredScaledMaxWidth(Utils.cleanColourNotModifiers(skillName) + " " + level, Minecraft.getMinecraft().fontRendererObj,
                        //        guiLeft+tl.x+tl2.x*xPosition+xOff, guiTop+tl.y+tl2.y*yPosition+yOff, false, 85, new Color(100, 100, 100, 100).getRGB());
                    }
                }
            }

            GlStateManager.color(1, 1, 1, 1);
            Utils.drawStringCenteredScaledMaxWidth(skillName + " " + EnumChatFormatting.WHITE + level, Minecraft.getMinecraft().fontRendererObj,
                    guiLeft+277+86*xPosition, guiTop+36+21*yPosition, true, 85, Color.BLACK.getRGB());

            position++;
        }
    }

    private static final LinkedHashMap<String, String> skillToSkillNameMap = new LinkedHashMap<>();
    static {
        skillToSkillNameMap.put("level_skill_taming", "Taming");
        skillToSkillNameMap.put("level_skill_mining", "Mining");
        skillToSkillNameMap.put("level_skill_foraging", "Foraging");
        skillToSkillNameMap.put("level_skill_enchanting", "Enchanting");
        skillToSkillNameMap.put("level_skill_carpentry", "Carpentry");
        skillToSkillNameMap.put("level_skill_farming", "Farming");
        skillToSkillNameMap.put("level_skill_combat", "Combat");
        skillToSkillNameMap.put("level_skill_fishing", "Fishing");
        skillToSkillNameMap.put("level_skill_alchemy", "Alchemy");
        skillToSkillNameMap.put("level_skill_runecrafting", "Runecrafting");
        skillToSkillNameMap.put("level_slayer_zombie", "Revenant Slayer");
        skillToSkillNameMap.put("level_slayer_spider", "Tarantula Slayer");
        skillToSkillNameMap.put("level_slayer_wolf", "Sven Slayer");
    }

    /*private void renderBar(float x, float y, float xSize, float ySize, float completed) {
        this.mc.getTextureManager().bindTexture(Gui.icons);

        float yScale = ySize/5;


        if (i > 0)
        {
            int j = 182;
            int k = (int)(this.mc.thePlayer.experience * (float)(j + 1));
            int l = p_175176_1_.getScaledHeight() - 32 + 3;

            Utils.drawTexturedRect(x, y, xSize, );

            this.drawTexturedModalRect(p_175176_2_, l, 0, 64, j, 5);

            if (k > 0)
            {
                this.drawTexturedModalRect(p_175176_2_, l, 0, 69, k, 5);
            }
        }

        if (this.mc.thePlayer.experienceLevel > 0)
        {
            this.mc.mcProfiler.startSection("expLevel");
            int k1 = 8453920;
            String s = "" + this.mc.thePlayer.experienceLevel;
            int l1 = (p_175176_1_.getScaledWidth() - this.getFontRenderer().getStringWidth(s)) / 2;
            int i1 = p_175176_1_.getScaledHeight() - 31 - 4;
            int j1 = 0;
            this.getFontRenderer().drawString(s, l1 + 1, i1, 0);
            this.getFontRenderer().drawString(s, l1 - 1, i1, 0);
            this.getFontRenderer().drawString(s, l1, i1 + 1, 0);
            this.getFontRenderer().drawString(s, l1, i1 - 1, 0);
            this.getFontRenderer().drawString(s, l1, i1, k1);
            this.mc.mcProfiler.endSection();
        }
    }*/

    public static void drawEntityOnScreen(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase ent) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)posX, (float)posY, 50.0F);
        GlStateManager.scale((float)(-scale), (float)scale, (float)scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        float f = ent.renderYawOffset;
        float f1 = ent.rotationYaw;
        float f2 = ent.rotationPitch;
        float f3 = ent.prevRotationYawHead;
        float f4 = ent.rotationYawHead;
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-((float)Math.atan((double)(mouseY / 40.0F))) * 20.0F, 1.0F, 0.0F, 0.0F);
        ent.renderYawOffset = (float)Math.atan((double)(mouseX / 40.0F)) * 20.0F;
        ent.rotationYaw = (float)Math.atan((double)(mouseX / 40.0F)) * 40.0F;
        ent.rotationPitch = -((float)Math.atan((double)(mouseY / 40.0F))) * 20.0F;
        ent.rotationYawHead = ent.rotationYaw;
        ent.prevRotationYawHead = ent.rotationYaw;
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        rendermanager.setPlayerViewY(180.0F);
        rendermanager.setRenderShadow(false);
        rendermanager.renderEntityWithPosYaw(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        rendermanager.setRenderShadow(true);
        ent.renderYawOffset = f;
        ent.rotationYaw = f1;
        ent.rotationPitch = f2;
        ent.prevRotationYawHead = f3;
        ent.rotationYawHead = f4;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }
}
