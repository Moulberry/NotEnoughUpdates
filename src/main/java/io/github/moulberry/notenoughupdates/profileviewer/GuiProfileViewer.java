package io.github.moulberry.notenoughupdates.profileviewer;

import com.google.common.base.Splitter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.cosmetics.ShaderManager;
import io.github.moulberry.notenoughupdates.questing.SBScoreboardData;
import io.github.moulberry.notenoughupdates.util.TexLoc;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.luaj.vm2.ast.Str;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;

public class GuiProfileViewer extends GuiScreen {

    public static final ResourceLocation pv_basic = new ResourceLocation("notenoughupdates:pv_basic.png");

    private final ProfileViewer.Profile profile;
    private ProfileViewerPage currentPage = ProfileViewerPage.BASIC;
    private int sizeX;
    private int sizeY;
    private int guiLeft;
    private int guiTop;

    private float backgroundRotation = 0;

    private long currentTime = 0;
    private long lastTime = 0;
    private long startTime = 0;

    private List<String> tooltipToDisplay = null;

    public enum ProfileViewerPage {
        BASIC
    }

    public GuiProfileViewer(ProfileViewer.Profile profile) {
        this.profile = profile;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        currentTime = System.currentTimeMillis();
        if(startTime == 0) startTime = currentTime;

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawDefaultBackground();

        switch (currentPage) {
            case BASIC:
                drawBasicPage(mouseX, mouseY, partialTicks);
                break;
        }

        lastTime = currentTime;

        if(tooltipToDisplay != null) {
            Utils.drawHoveringText(tooltipToDisplay, mouseX, mouseY, width, height, -1, Minecraft.getMinecraft().fontRendererObj);
            tooltipToDisplay = null;
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

    public EntityOtherPlayerMP getEntityPlayer() {
        return entityPlayer;
    }

    private EntityOtherPlayerMP entityPlayer = null;
    private ResourceLocation playerLocationSkin = null;
    private ResourceLocation playerLocationCape = null;
    private String skinType = null;

    private HashMap<String, ResourceLocation[]> panoramasMap = new HashMap<>();

    public ResourceLocation[] getPanoramasForLocation(String location, String identifier) {
        if(panoramasMap.containsKey(location)) return panoramasMap.get(location);
        try {
            ResourceLocation[] panoramasArray = new ResourceLocation[6];
            for(int i=0; i<6; i++) {
                panoramasArray[i] = new ResourceLocation("notenoughupdates:panoramas/"+location+"_"+identifier+"/panorama_"+i+".png");
                Minecraft.getMinecraft().getResourceManager().getResource(panoramasArray[i]);
            }
            panoramasMap.put(location, panoramasArray);
            return panoramasArray;
        } catch(IOException e) {
            try {
                ResourceLocation[] panoramasArray = new ResourceLocation[6];
                for(int i=0; i<6; i++) {
                    panoramasArray[i] = new ResourceLocation("notenoughupdates:panoramas/"+location+"/panorama_"+i+".png");
                    Minecraft.getMinecraft().getResourceManager().getResource(panoramasArray[i]);
                }
                panoramasMap.put(location, panoramasArray);
                return panoramasArray;
            } catch(IOException e2) {
                ResourceLocation[] panoramasArray = new ResourceLocation[6];
                for(int i=0; i<6; i++) {
                    panoramasArray[i] = new ResourceLocation("notenoughupdates:panoramas/unknown/panorama_"+i+".png");
                }
                panoramasMap.put(location, panoramasArray);
                return panoramasArray;
            }
        }
    }

    private int backgroundClickedX = -1;

    private static char[] c = new char[]{'k', 'm', 'b', 't'};

    public static String shortNumberFormat(double n, int iteration) {
        double d = ((long) n / 100) / 10.0;
        boolean isRound = (d * 10) %10 == 0;
        return (d < 1000?
                ((d > 99.9 || isRound || (!isRound && d > 9.99)?
                        (int) d * 10 / 10 : d + ""
                ) + "" + c[iteration])
                : shortNumberFormat(d, iteration+1));
    }

    private void drawBasicPage(int mouseX, int mouseY, float partialTicks) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

        this.sizeX = 431;
        this.sizeY = 202;
        this.guiLeft = (this.width-this.sizeX)/2;
        this.guiTop = (this.height-this.sizeY)/2;

        String location = null;
        JsonObject status = profile.getPlayerStatus();
        if(status != null && status.has("mode")) {
            location = status.get("mode").getAsString();
        }

        int extraRotation = 0;
        if(Mouse.isButtonDown(0)) {
            if(backgroundClickedX == -1) {
                if(mouseX > guiLeft+23 && mouseX < guiLeft+23+81) {
                    if(mouseY > guiTop+44 && mouseY < guiTop+44+108) {
                        backgroundClickedX = mouseX;
                    }
                }
            }
        } else {
            if(backgroundClickedX != -1) {
                backgroundRotation += mouseX - backgroundClickedX;
                backgroundClickedX = -1;
            }
        }
        if(backgroundClickedX == -1) {
            backgroundRotation += (currentTime - lastTime)/400f;
        } else {
            extraRotation = mouseX - backgroundClickedX;
        }
        backgroundRotation %= 360;

        String panoramaIdentifier = "day";
        if(SBScoreboardData.getInstance().currentTimeDate != null) {
            if(SBScoreboardData.getInstance().currentTimeDate.before(new Date(0, 0, 0, 6, 0, 0))) {
                if(SBScoreboardData.getInstance().currentTimeDate.after(new Date(0, 0, 0, 20, 0, 0))) {
                    panoramaIdentifier = "night";
                }
            }
        }

        Panorama.drawPanorama(-backgroundRotation-extraRotation, guiLeft+23, guiTop+44, 81, 108,
                getPanoramasForLocation(location==null?"unknown":location, panoramaIdentifier));

        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_basic);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);

        if(entityPlayer != null && profile.getHypixelProfile() != null) {
            String rank = Utils.getElementAsString(profile.getHypixelProfile().get("rank"), Utils.getElementAsString(profile.getHypixelProfile().get("newPackageRank"), "NONE"));
            EnumChatFormatting rankPlusColorECF = EnumChatFormatting.getValueByName(Utils.getElementAsString(profile.getHypixelProfile().get("rankPlusColor"), "WHITE"));
            String rankPlusColor = EnumChatFormatting.WHITE.toString();
            if(rankPlusColorECF != null) {
                rankPlusColor = rankPlusColorECF.toString();
            }

            JsonObject misc = Utils.getConstant("misc");
            if(misc != null) {
                if(misc.has("ranks")) {
                    String rankName = Utils.getElementAsString(Utils.getElement(misc, "ranks."+rank+".tag"), null);
                    String rankColor = Utils.getElementAsString(Utils.getElement(misc, "ranks."+rank+".color"), "7");
                    String rankPlus = Utils.getElementAsString(Utils.getElement(misc, "ranks."+rank+".plus"), "");

                    String name = entityPlayer.getName();

                    if(misc.has("special_bois")) {
                        JsonArray special_bois = misc.get("special_bois").getAsJsonArray();
                        for(int i=0; i<special_bois.size(); i++) {
                            if(special_bois.get(i).getAsString().equals(profile.getUuid())) {
                                name = Utils.chromaString(name);
                                break;
                            }
                        }
                    }

                    String playerName = EnumChatFormatting.GRAY.toString() + name;
                    if(rankName != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("\u00A7"+rankColor);
                        sb.append("[");
                        sb.append(rankName);
                        sb.append(rankPlusColor);
                        sb.append(rankPlus);
                        sb.append("\u00A7"+rankColor);
                        sb.append("] ");
                        sb.append(name);
                        playerName = sb.toString();
                    }

                    int rankPrefixLen = fr.getStringWidth(playerName);
                    int halfRankPrefixLen = rankPrefixLen/2;

                    int x = guiLeft+63;
                    int y = guiTop+54;

                    drawRect(x-halfRankPrefixLen-1, y-1, x+halfRankPrefixLen+1, y+8, new Color(0, 0, 0, 64).getRGB());

                    fr.drawString(playerName, x-halfRankPrefixLen, y, 0, true);
                }
            }
        }

        if(status != null) {
            JsonElement onlineElement = Utils.getElement(status, "online");
            boolean online = onlineElement != null && onlineElement.isJsonPrimitive() && onlineElement.getAsBoolean();
            String statusStr = online ? EnumChatFormatting.GREEN + "ONLINE" : EnumChatFormatting.RED + "OFFLINE";
            String locationStr = null;
            if(profile.getUuid().equals("20934ef9488c465180a78f861586b4cf")) {
                locationStr = "Ignoring DMs";
            } else {
                if(location != null) {
                    JsonObject misc = Utils.getConstant("misc");
                    if(misc != null) {
                        locationStr = Utils.getElementAsString(Utils.getElement(misc, "area_names."+location), "Unknown Location");
                    }
                }
            }
            if(locationStr != null) {
                statusStr += EnumChatFormatting.GRAY+" - "+EnumChatFormatting.GREEN+locationStr;
            }

            Utils.drawStringCentered(statusStr, fr, guiLeft+63, guiTop+160, true, 0);
        }

        if(profile.getPlayerInformation(null) == null) {
            //TODO: "Downloading player information"
            return;
        }

        if(entityPlayer == null) {
            UUID playerUUID = UUID.fromString(niceUuid(profile.getUuid()));
            GameProfile fakeProfile = Minecraft.getMinecraft().getSessionService().fillProfileProperties(new GameProfile(playerUUID, "CoolGuy123"), false);
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
            entityPlayer.setAlwaysRenderNameTag(false);
            entityPlayer.setCustomNameTag("");
        } else {
            entityPlayer.refreshDisplayName();
        }

        JsonObject profileInfo = profile.getProfileInformation(null);
        if(profileInfo == null) return;

        JsonObject skillInfo = profile.getSkillInfo(null);
        JsonObject inventoryInfo = profile.getInventoryInfo(null);
        JsonObject collectionInfo = profile. getCollectionInfo(null);

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

        GlStateManager.color(1, 1, 1, 1);
        drawEntityOnScreen(guiLeft+63, guiTop+128+7, 36, guiLeft+63-mouseX, guiTop+129-mouseY, entityPlayer);


        PlayerStats.Stats stats = profile.getStats(null);


        if(stats != null) {
            Splitter splitter = Splitter.on(" ").omitEmptyStrings().limit(2);
            for(int i=0; i<PlayerStats.defaultStatNames.length; i++) {
                String statName = PlayerStats.defaultStatNames[i];
                String statNamePretty = PlayerStats.defaultStatNamesPretty[i];

                int val = Math.round(stats.get(statName));

                GlStateManager.color(1, 1, 1, 1);
                GlStateManager.enableBlend();
                GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                for(int xOff=-2; xOff<=2; xOff++) {
                    for(int yOff=-2; yOff<=2; yOff++) {
                        if(Math.abs(xOff) != Math.abs(yOff)) {
                            Utils.drawStringCenteredScaledMaxWidth(Utils.cleanColourNotModifiers(statNamePretty) + " " + val, Minecraft.getMinecraft().fontRendererObj,
                                    guiLeft+172f+xOff/2f, guiTop+36+12.5f*i+yOff/2f, false, 80, new Color(0, 0, 0,
                                            200/Math.max(Math.abs(xOff), Math.abs(yOff))).getRGB());
                        }
                    }
                }

                GlStateManager.color(1, 1, 1, 1);
                Utils.drawStringCenteredScaledMaxWidth(statNamePretty + " " + EnumChatFormatting.WHITE + val, Minecraft.getMinecraft().fontRendererObj,
                        guiLeft+172f, guiTop+36+12.5f*i, false, 80, 4210752);


                if(mouseX > guiLeft+132 && mouseX < guiLeft+212) {
                    if(mouseY > guiTop+32+12.5f*i && mouseY < guiTop+40+12.5f*i) {
                        List<String> split = splitter.splitToList(statNamePretty);
                        PlayerStats.Stats baseStats = PlayerStats.getBaseStats();
                        tooltipToDisplay = new ArrayList<>();
                        tooltipToDisplay.add(statNamePretty);
                        int base = Math.round(baseStats.get(statName));
                        tooltipToDisplay.add(EnumChatFormatting.GRAY+"Base "+split.get(1)+": "+EnumChatFormatting.GREEN+base+" "+split.get(0));
                        int passive = Math.round(profile.getPassiveStats(null).get(statName)-baseStats.get(statName));
                        tooltipToDisplay.add(EnumChatFormatting.GRAY+"Passive "+split.get(1)+" Bonus: +"+EnumChatFormatting.YELLOW+passive+" "+split.get(0));
                        int itemBonus = Math.round(stats.get(statName)-profile.getPassiveStats(null).get(statName));
                        tooltipToDisplay.add(EnumChatFormatting.GRAY+"Item "+split.get(1)+" Bonus: +"+EnumChatFormatting.DARK_PURPLE+itemBonus+" "+split.get(0));
                        int finalStat = Math.round(stats.get(statName));
                        tooltipToDisplay.add(EnumChatFormatting.GRAY+"Final "+split.get(1)+": +"+EnumChatFormatting.RED+finalStat+" "+split.get(0));
                    }
                }
            }
        } else {
            //"stats not enabled in api"
        }

        if(skillInfo != null) {
            int position = 0;
            for(Map.Entry<String, ItemStack> entry : ProfileViewer.getSkillToSkillDisplayMap().entrySet()) {
                int yPosition = position % 7;
                int xPosition = position / 7;

                String skillName = entry.getValue().getDisplayName();

                float level = Utils.getElementAsFloat(skillInfo.get("level_"+entry.getKey()), 0);
                int levelFloored = (int)Math.floor(level);

                int x = guiLeft+237+86*xPosition;
                int y = guiTop+31+21*yPosition;

                for(int xOff=-2; xOff<=2; xOff++) {
                    for(int yOff=-2; yOff<=2; yOff++) {
                        if(Math.abs(xOff) != Math.abs(yOff)) {
                            Utils.drawStringCenteredYScaledMaxWidth(Utils.cleanColourNotModifiers(skillName) + " " + levelFloored, Minecraft.getMinecraft().fontRendererObj,
                                x+14+xOff/2f, y+yOff/2f, false, 60, new Color(0, 0, 0,
                                        200/Math.max(Math.abs(xOff), Math.abs(yOff))).getRGB());
                        }
                    }
                }

                GlStateManager.color(1, 1, 1, 1);
                Utils.drawStringCenteredYScaledMaxWidth(EnumChatFormatting.WHITE + skillName + " " + EnumChatFormatting.WHITE + levelFloored, Minecraft.getMinecraft().fontRendererObj,
                        x+14, y, false, 60, 4210752);

                if(skillInfo.get("maxed_"+entry.getKey()).getAsBoolean()) {
                    renderGoldBar(x, y+6, 80);
                } else {
                    renderBar(x, y+6, 80, level%1);
                }

                if(mouseX > x && mouseX < x+80) {
                    if(mouseY > y+4 && mouseY < y+13) {
                        String levelStr;
                        if(skillInfo.get("maxed_"+entry.getKey()).getAsBoolean()) {
                            levelStr = EnumChatFormatting.GOLD+"MAXED!";
                        } else {
                            int maxXp = (int)skillInfo.get("maxxp_"+entry.getKey()).getAsFloat();
                            levelStr = EnumChatFormatting.DARK_PURPLE.toString() + shortNumberFormat(Math.round((level%1)*maxXp), 0) + "/" + shortNumberFormat(maxXp, 0);
                        }

                        tooltipToDisplay = Utils.createList(levelStr);
                    }
                }

                GL11.glTranslatef((x), (y-8*0.7f), 0);
                GL11.glScalef(0.7f, 0.7f, 1);
                Utils.drawItemStackLinear(entry.getValue(), 0, 0);
                GL11.glScalef(1/0.7f, 1/0.7f, 1);
                GL11.glTranslatef(-(x), -(y-8*0.7f), 0);

                position++;
            }
        } else {
            //api not enabled
        }
    }

    private void renderGoldBar(float x, float y, float xSize) {

        Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.icons);
        ShaderManager shaderManager = ShaderManager.getInstance();
        shaderManager.loadShader("make_gold");
        shaderManager.loadData("make_gold", "amount", (startTime-System.currentTimeMillis())/10000f);

        Utils.drawTexturedRect(x, y, xSize/2f, 5, 0/256f, (xSize/2f)/256f, 79/256f, 84/256f, GL11.GL_NEAREST);
        Utils.drawTexturedRect(x+xSize/2f, y, xSize/2f, 5, (182-xSize/2f)/256f, 182/256f, 79/256f, 84/256f, GL11.GL_NEAREST);

        GL20.glUseProgram(0);
    }

    private void renderBar(float x, float y, float xSize, float completed) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.icons);

        completed = Math.round(completed/0.05f)*0.05f;

        float notcompleted = 1-completed;

        int displayNum = 0;//tl.x%5;

        GlStateManager.color(1, 1, 1, 1);
        float width = 0;

        if(completed < 0.5f && (displayNum == 1 || displayNum == 0)) {
            width = (0.5f - completed) * xSize;
            Utils.drawTexturedRect(x+xSize*completed, y, width, 5, xSize*completed/256f, (xSize/2f)/256f, 74/256f, 79/256f, GL11.GL_NEAREST);
        }
        if(completed < 1f && (displayNum == 2 || displayNum == 0)) {
            width = Math.min(xSize*notcompleted, xSize/2f);
            Utils.drawTexturedRect(x+(xSize/2f)+Math.max(xSize*(completed-0.5f), 0), y, width, 5,
                    (182-(xSize/2f)+Math.max(xSize*(completed-0.5f), 0))/256f, 182/256f, 74/256f, 79/256f, GL11.GL_NEAREST);
        }

        if(completed > 0f && (displayNum == 3 || displayNum == 0)) {
            width = Math.min(xSize*completed, xSize/2f);
            Utils.drawTexturedRect(x, y, width, 5,
                    0/256f, width/256f, 79/256f, 84/256f, GL11.GL_NEAREST);
        }
        if(completed > 0.5f && (displayNum == 4 || displayNum == 0)) {
            width = Math.max(xSize*(completed-0.5f), xSize/2f);
            Utils.drawTexturedRect(x+(xSize/2f), y, width, 5,
                    (182-(xSize/2f))/256f, (182-(xSize/2f)+width)/256f, 79/256f, 84/256f, GL11.GL_NEAREST);
        }
    }

    private static final ResourceLocation shadowTextures = new ResourceLocation("textures/misc/shadow.png");
    public static void drawEntityOnScreen(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase ent) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)posX, (float)posY, 50.0F);
        GlStateManager.scale((float)(-scale), (float)scale, (float)scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        float renderYawOffset = ent.renderYawOffset;
        float f1 = ent.rotationYaw;
        float f2 = ent.rotationPitch;
        float f3 = ent.prevRotationYawHead;
        float f4 = ent.rotationYawHead;
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(25, 1.0F, 0.0F, 0.0F);
        ent.renderYawOffset = (float)Math.atan((double)(mouseX / 40.0F)) * 20.0F;
        ent.rotationYaw = (float)Math.atan((double)(mouseX / 40.0F)) * 40.0F;
        ent.rotationPitch = -((float)Math.atan((double)(mouseY / 40.0F))) * 20.0F;
        ent.rotationYawHead = ent.rotationYaw;
        ent.prevRotationYawHead = ent.rotationYaw;
        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        rendermanager.setPlayerViewY(180.0F);
        rendermanager.setRenderShadow(false);
        rendermanager.renderEntityWithPosYaw(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);

        /*{
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            rendermanager.renderEngine.bindTexture(shadowTextures);
            GlStateManager.depthMask(false);
            float f = 0.5f;

            if (ent instanceof EntityLiving) {
                EntityLiving entityliving = (EntityLiving)ent;
                f *= entityliving.getRenderSizeModifier();

                if (entityliving.isChild())
                {
                    f *= 0.5F;
                }
            }

            /*Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

            GlStateManager.color(1, 1, 1, 0.5f);
            Utils.drawTexturedRect(-0.5f*tl.x, -0.5f*tl.x, 1*tl.x, 1*tl.x);

            /*for (BlockPos blockpos : BlockPos.getAllInBoxMutable(new BlockPos(i, k, i1), new BlockPos(j, l, j1))) {
                Block block = world.getBlockState(blockpos.down()).getBlock();

                if (block.getRenderType() != -1 && world.getLightFromNeighbors(blockpos) > 3) {
                    this.func_180549_a(block, x, y, z, blockpos, shadowAlpha, f, d2, d3, d4);
                }
            }

            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
        }*/

        ent.renderYawOffset = renderYawOffset;
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
