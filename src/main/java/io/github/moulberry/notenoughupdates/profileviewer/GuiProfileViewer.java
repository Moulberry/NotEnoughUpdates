package io.github.moulberry.notenoughupdates.profileviewer;

import com.google.common.base.Splitter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.SBAIntegration;
import io.github.moulberry.notenoughupdates.cosmetics.ShaderManager;
import io.github.moulberry.notenoughupdates.itemeditor.GuiElementTextField;
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
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.*;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiProfileViewer extends GuiScreen {

    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
    public static final ResourceLocation pv_basic = new ResourceLocation("notenoughupdates:pv_basic.png");
    public static final ResourceLocation pv_invs = new ResourceLocation("notenoughupdates:pv_invs.png");
    public static final ResourceLocation pv_cols = new ResourceLocation("notenoughupdates:pv_cols.png");
    public static final ResourceLocation pv_pets = new ResourceLocation("notenoughupdates:pv_pets.png");
    public static final ResourceLocation pv_dropdown = new ResourceLocation("notenoughupdates:pv_dropdown.png");
    public static final ResourceLocation pv_bg = new ResourceLocation("notenoughupdates:pv_bg.png");
    public static final ResourceLocation pv_elements = new ResourceLocation("notenoughupdates:pv_elements.png");
    public static final ResourceLocation resource_packs = new ResourceLocation("minecraft:textures/gui/resource_packs.png");
    private static final ResourceLocation creativeInventoryTabs =
            new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

    private static final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

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

    private String profileId = null;
    private boolean profileDropdownSelected = false;

    public enum ProfileViewerPage {
        LOADING(null),
        INVALID_NAME(null),
        BASIC(new ItemStack(Items.paper)),
        INVS(new ItemStack(Item.getItemFromBlock(Blocks.ender_chest))),
        COLS(new ItemStack(Items.painting)),
        PETS(new ItemStack(Items.bone));

        public final ItemStack stack;


        ProfileViewerPage(ItemStack stack) {
            this.stack = stack;
        }
    }

    public GuiProfileViewer(ProfileViewer.Profile profile) {
        this.profile = profile;
        String name = "";
        if(profile != null) {
            name = profile.getHypixelProfile().get("displayname").getAsString();
        }
        playerNameTextField = new GuiElementTextField(name,
                GuiElementTextField.SCALE_TEXT);
        playerNameTextField.setSize(100, 20);
    }

    private GuiElementTextField playerNameTextField;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        currentTime = System.currentTimeMillis();
        if(startTime == 0) startTime = currentTime;

        if(profile == null) {
            currentPage = ProfileViewerPage.INVALID_NAME;
        }
        if(profileId == null && profile != null && profile.getLatestProfile() != null) {
            profileId = profile.getLatestProfile();
        }

        this.sizeX = 431;
        this.sizeY = 202;
        this.guiLeft = (this.width-this.sizeX)/2;
        this.guiTop = (this.height-this.sizeY)/2;

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawDefaultBackground();

        blurBackground();
        renderBlurredBackground(width, height, guiLeft+2, guiTop+2, sizeX-4, sizeY-4);

        GlStateManager.enableDepth();
        GlStateManager.translate(0, 0, 5);
        renderTabs(true);
        GlStateManager.translate(0, 0, -3);

        GlStateManager.disableDepth();
        GlStateManager.translate(0, 0, -2);
        renderTabs(false);
        GlStateManager.translate(0, 0, 2);

        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);

        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_bg);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);

        if(!(currentPage == ProfileViewerPage.LOADING)) {
            playerNameTextField.render(guiLeft+sizeX-100, guiTop+sizeY+5);
            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());

            if(profile != null) {
                renderBlurredBackground(width, height, guiLeft+2, guiTop+sizeY+3+2, 100-4, 20-4);
                Minecraft.getMinecraft().getTextureManager().bindTexture(pv_dropdown);
                Utils.drawTexturedRect(guiLeft, guiTop+sizeY+3, 100, 20,
                        0, 100/200f, 0, 20/185f, GL11.GL_NEAREST);
                Utils.drawStringCenteredScaledMaxWidth(profileId, Minecraft.getMinecraft().fontRendererObj, guiLeft+50,
                        guiTop+sizeY+3+10, true, 90, new Color(63, 224, 208, 255).getRGB());

                if(profileDropdownSelected && !profile.getProfileIds().isEmpty() && scaledResolution.getScaleFactor() != 4) {
                    int dropdownOptionSize = scaledResolution.getScaleFactor()==3?10:20;

                    int numProfiles = profile.getProfileIds().size();
                    int sizeYDropdown = numProfiles*dropdownOptionSize;
                    renderBlurredBackground(width, height, guiLeft+2, guiTop+sizeY+23, 100-4, sizeYDropdown-2);
                    Minecraft.getMinecraft().getTextureManager().bindTexture(pv_dropdown);
                    Utils.drawTexturedRect(guiLeft, guiTop+sizeY+23-3, 100, 3,
                            100/200f, 1, 0, 3/185f, GL11.GL_NEAREST);
                    Utils.drawTexturedRect(guiLeft, guiTop+sizeY+23+sizeYDropdown-4, 100, 4,
                            100/200f, 1, 181/185f, 1, GL11.GL_NEAREST);
                    Utils.drawTexturedRect(guiLeft, guiTop+sizeY+23, 100, sizeYDropdown-4,
                            100/200f, 1, (181-sizeYDropdown)/185f, 181/185f, GL11.GL_NEAREST);

                    for(int yIndex=0; yIndex<profile.getProfileIds().size(); yIndex++) {
                        String otherProfileId = profile.getProfileIds().get(yIndex);
                        Utils.drawStringCenteredScaledMaxWidth(otherProfileId, Minecraft.getMinecraft().fontRendererObj, guiLeft+50,
                                guiTop+sizeY+23+dropdownOptionSize/2f+dropdownOptionSize*yIndex, true, 90, new Color(33, 112, 104, 255).getRGB());
                    }

                }
            }
        }

        GlStateManager.color(1, 1, 1, 1);
        switch (currentPage) {
            case BASIC:
                drawBasicPage(mouseX, mouseY, partialTicks);
                break;
            case INVS:
                drawInvsPage(mouseX, mouseY, partialTicks);
                break;
            case COLS:
                drawColsPage(mouseX, mouseY, partialTicks);
                break;
            case PETS:
                drawPetsPage(mouseX, mouseY, partialTicks);
                break;
            case LOADING:
                Utils.drawStringCentered(EnumChatFormatting.YELLOW+"Loading player profiles...", Minecraft.getMinecraft().fontRendererObj,
                        guiLeft+sizeX/2f, guiTop+101, true, 0);
                break;
            case INVALID_NAME:
                Utils.drawStringCentered(EnumChatFormatting.RED+"Invalid name or API is down!", Minecraft.getMinecraft().fontRendererObj,
                        guiLeft+sizeX/2f, guiTop+101, true, 0);
                break;
        }

        lastTime = currentTime;

        if(tooltipToDisplay != null) {
            List<String> grayTooltip = new ArrayList<>(tooltipToDisplay.size());
            for(String line : tooltipToDisplay) {
                grayTooltip.add(EnumChatFormatting.GRAY + line);
            }
            Utils.drawHoveringText(grayTooltip, mouseX, mouseY, width, height, -1, Minecraft.getMinecraft().fontRendererObj);
            tooltipToDisplay = null;
        }
    }

    private boolean isLoadedProfile() {
        return profile.getProfileInformation(profileId) != null;
    }

    private void renderTabs(boolean renderPressed) {
        int ignoredTabs = 0;
        for(int i=0; i<ProfileViewerPage.values().length; i++) {
            ProfileViewerPage page = ProfileViewerPage.values()[i];
            if(page.stack == null) {
                ignoredTabs++;
                continue;
            }
            boolean pressed = page == currentPage;
            if(pressed == renderPressed) {
                renderTab(page.stack, i-ignoredTabs, pressed);
            }
        }
    }

    private void renderTab(ItemStack stack, int xIndex, boolean pressed) {
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);

        int x = guiLeft+xIndex*28;
        int y = guiTop-28;

        float uMin = 0;
        float uMax = 28/256f;
        float vMin = 20/256f;
        float vMax = 51/256f;
        if(pressed) {
            vMin = 52/256f;
            vMax = 84/256f;

            if(xIndex != 0) {
                uMin = 28/256f;
                uMax = 56/256f;
            }

            renderBlurredBackground(width, height, x+2, y+2, 28-4, 28-4);
        } else {
            renderBlurredBackground(width, height, x+2, y+4, 28-4, 28-4);
        }

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);

        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
        Utils.drawTexturedRect(x, y, 28, pressed?32:31, uMin, uMax, vMin, vMax, GL11.GL_NEAREST);

        GlStateManager.enableDepth();
        Utils.drawItemStack(stack, x+6, y+9);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if(currentPage != ProfileViewerPage.LOADING && currentPage != ProfileViewerPage.INVALID_NAME) {
            int ignoredTabs = 0;
            for(int i=0; i<ProfileViewerPage.values().length; i++) {
                ProfileViewerPage page = ProfileViewerPage.values()[i];
                if(page.stack == null) {
                    ignoredTabs++;
                    continue;
                }
                int i2 = i - ignoredTabs;
                int x = guiLeft+i2*28;
                int y = guiTop-28;

                if(mouseX > x && mouseX < x+28) {
                    if(mouseY > y && mouseY < y+32) {
                        if(currentPage != page) Utils.playPressSound();
                        currentPage = page;
                        inventoryTextField.otherComponentClick();
                        playerNameTextField.otherComponentClick();
                        return;
                    }
                }
            }
        }
        switch (currentPage) {
            case INVS:
                inventoryTextField.setSize(88, 20);
                if(mouseX > guiLeft+19 && mouseX < guiLeft+19+88) {
                    if(mouseY > guiTop+sizeY-26-20 && mouseY < guiTop+sizeY-26) {
                        inventoryTextField.mouseClicked(mouseX, mouseY, mouseButton);
                        playerNameTextField.otherComponentClick();
                        return;
                    }
                }
                break;
            case PETS:
                if(sortedPets == null) break;
                for(int i=petsPage*20; i<Math.min(petsPage*20+20, sortedPets.size()); i++) {
                    int xIndex = (i%20) % COLLS_XCOUNT;
                    int yIndex = (i%20) / COLLS_XCOUNT;

                    float x = 5 + COLLS_XPADDING + (COLLS_XPADDING + 20) * xIndex;
                    float y = 7 + COLLS_YPADDING + (COLLS_YPADDING + 20) * yIndex;

                    if(mouseX > guiLeft+x && mouseX < guiLeft+x+20) {
                        if(mouseY > guiTop+y && mouseY < guiTop+y+20) {
                            selectedPet = i;
                            return;
                        }
                    }
                }
                break;
        }
        if(mouseX > guiLeft+sizeX-100 && mouseX < guiLeft+sizeX) {
            if(mouseY > guiTop+sizeY+5 && mouseY < guiTop+sizeY+25) {
                playerNameTextField.mouseClicked(mouseX, mouseY, mouseButton);
                inventoryTextField.otherComponentClick();
                return;
            }
        }
        if(mouseX > guiLeft && mouseX < guiLeft+100 && profile != null && !profile.getProfileIds().isEmpty()) {
            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            if(mouseY > guiTop+sizeY+3 && mouseY < guiTop+sizeY+23) {
                if(scaledResolution.getScaleFactor() == 4) {
                    profileDropdownSelected = false;
                    int profileNum = 0;
                    for(int index=0; index<profile.getProfileIds().size(); index++) {
                        if(profile.getProfileIds().get(index).equals(profileId)) {
                            profileNum = index;
                            break;
                        }
                    }
                    if(mouseButton == 0) {
                        profileNum++;
                    } else {
                        profileNum--;
                    }
                    if(profileNum >= profile.getProfileIds().size()) profileNum = 0;
                    if(profileNum < 0) profileNum = profile.getProfileIds().size()-1;

                    String newProfileId = profile.getProfileIds().get(profileNum);
                    if(profileId != null && !profileId.equals(newProfileId)) {
                        resetCache();
                    }
                    profileId = newProfileId;
                } else {
                    profileDropdownSelected = !profileDropdownSelected;
                }
            } else if(scaledResolution.getScaleFactor() != 4 && profileDropdownSelected) {
                int dropdownOptionSize = scaledResolution.getScaleFactor()==3?10:20;
                int extraY = mouseY - (guiTop+sizeY+23);
                int index = extraY/dropdownOptionSize;
                if(index >= 0 && index < profile.getProfileIds().size()) {
                    String newProfileId = profile.getProfileIds().get(index);
                    if(profileId != null && !profileId.equals(newProfileId)) {
                        resetCache();
                    }
                    profileId = newProfileId;
                }
            }
            playerNameTextField.otherComponentClick();
            inventoryTextField.otherComponentClick();
            return;
        }
        profileDropdownSelected = false;
        playerNameTextField.otherComponentClick();
        inventoryTextField.otherComponentClick();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        SBAIntegration.keyTyped(keyCode);
        switch (currentPage) {
            case INVS:
                keyTypedInvs(typedChar, keyCode);
                inventoryTextField.keyTyped(typedChar, keyCode);
                break;
            case COLS:
                keyTypedCols(typedChar, keyCode);
                break;
        }
        if(playerNameTextField.getFocus() && !(currentPage == ProfileViewerPage.LOADING)) {
            if(keyCode == Keyboard.KEY_RETURN) {
                currentPage = ProfileViewerPage.LOADING;
                NotEnoughUpdates.profileViewer.getProfileByName(playerNameTextField.getText(), profile -> { //todo: invalid name
                    if(profile != null) profile.resetCache();
                    Minecraft.getMinecraft().displayGuiScreen(new GuiProfileViewer(profile));
                });
            }
            playerNameTextField.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);

        switch (currentPage) {
            case INVS:
                mouseReleasedInvs(mouseX, mouseY, mouseButton);
                break;
            case COLS:
                mouseReleasedCols(mouseX, mouseY, mouseButton);
                break;
            case PETS:
                mouseReleasedPets(mouseX, mouseY, mouseButton);
        }
    }

    protected void keyTypedInvs(char typedChar, int keyCode) throws IOException {
        switch(keyCode) {
            case Keyboard.KEY_1:
            case Keyboard.KEY_NUMPAD1:
                selectedInventory = "inv_contents"; break;
            case Keyboard.KEY_2:
            case Keyboard.KEY_NUMPAD2:
                selectedInventory = "ender_chest_contents"; break;
            case Keyboard.KEY_3:
            case Keyboard.KEY_NUMPAD3:
                selectedInventory = "talisman_bag"; break;
            case Keyboard.KEY_4:
            case Keyboard.KEY_NUMPAD4:
                selectedInventory = "wardrobe_contents"; break;
            case Keyboard.KEY_5:
            case Keyboard.KEY_NUMPAD5:
                selectedInventory = "fishing_bag"; break;
            case Keyboard.KEY_6:
            case Keyboard.KEY_NUMPAD6:
                selectedInventory = "potion_bag"; break;
        }
        Utils.playPressSound();
    }

    protected void keyTypedCols(char typedChar, int keyCode) throws IOException {
        ItemStack stack = null;
        Iterator<ItemStack> items = ProfileViewer.getCollectionCatToCollectionMap().keySet().iterator();
        switch(keyCode) {
            case Keyboard.KEY_5:
            case Keyboard.KEY_NUMPAD5:
                stack = items.next();
            case Keyboard.KEY_4:
            case Keyboard.KEY_NUMPAD4:
                stack = items.next();
            case Keyboard.KEY_3:
            case Keyboard.KEY_NUMPAD3:
                stack = items.next();
            case Keyboard.KEY_2:
            case Keyboard.KEY_NUMPAD2:
                stack = items.next();
            case Keyboard.KEY_1:
            case Keyboard.KEY_NUMPAD1:
                stack = items.next();
        }
        if(stack != null) {
            selectedCollectionCategory = stack;
        }
        Utils.playPressSound();
    }

    private void mouseReleasedPets(int mouseX, int mouseY, int mouseButton) {
        if(mouseY > guiTop+6 && mouseY < guiTop+22) {
            if(mouseX > guiLeft+100-15-12 && mouseX < guiLeft+100-20) {
                if(petsPage > 0) {
                    petsPage--;
                }
                return;
            } else if(mouseX > guiLeft+100+15 && mouseX < guiLeft+100+20+12) {
                if(sortedPets != null && petsPage < Math.ceil(sortedPets.size()/25f)-1) {
                    petsPage++;
                }
                return;
            }
        }
    }

    private void mouseReleasedInvs(int mouseX, int mouseY, int mouseButton) {
        if(mouseButton == 0) {
            int i=0;
            for(Map.Entry<String, ItemStack> entry : invNameToDisplayMap.entrySet()) {
                int xIndex = i%3;
                int yIndex = i/3;

                int x = guiLeft+19+34*xIndex;
                int y = guiTop+26+34*yIndex;

                if(mouseX >= x && mouseX <= x+16) {
                    if(mouseY >= y && mouseY <= y+16) {
                        if(selectedInventory != entry.getKey()) Utils.playPressSound();
                        selectedInventory = entry.getKey();
                        return;
                    }
                }

                i++;
            }

            JsonObject inventoryInfo = profile.getInventoryInfo(profileId);
            if(inventoryInfo == null) return;
            JsonObject collectionInfo = profile.getCollectionInfo(profileId);
            if(collectionInfo == null) return;

            ItemStack[][][] inventories = getItemsForInventory(inventoryInfo, collectionInfo, selectedInventory);
            if(currentInventoryIndex >= inventories.length) currentInventoryIndex = inventories.length-1;
            if(currentInventoryIndex < 0) currentInventoryIndex = 0;

            ItemStack[][] inventory = inventories[currentInventoryIndex];
            if(inventory == null) return;

            int inventoryRows = inventory.length;
            int invSizeY = inventoryRows*18+17+7;

            int y = guiTop+101-invSizeY/2;

            if(mouseY > y+invSizeY && mouseY < y+invSizeY+16) {
                if(mouseX > guiLeft+320-12 && mouseX < guiLeft+320+12) {
                    if(mouseX < guiLeft+320) {
                        currentInventoryIndex--;
                    } else {
                        currentInventoryIndex++;
                    }
                }
            }
        }
    }

    private ItemStack selectedCollectionCategory = null;

    private void mouseReleasedCols(int mouseX, int mouseY, int mouseButton) {
        int collectionCatSize = ProfileViewer.getCollectionCatToCollectionMap().size();
        int collectionCatYSize = (int)(162f/(collectionCatSize-1+0.0000001f));
        int yIndex = 0;
        for(ItemStack stack : ProfileViewer.getCollectionCatToCollectionMap().keySet()) {
            if(mouseX > guiLeft+7 && mouseX < guiLeft+7+20) {
                if(mouseY > guiTop+10+collectionCatYSize*yIndex && mouseY < guiTop+10+collectionCatYSize*yIndex+20) {
                    selectedCollectionCategory = stack;
                    Utils.playPressSound();
                    return;
                }
            }
            yIndex++;
        }
    }

    private class Level {
        float level;
        float currentLevelRequirement;
        float maxXP;
    }

    public Level getLevel(JsonArray levels, int offset, float exp) {
        float xpTotal = 0;
        float level = 1;
        float currentLevelRequirement = 0;
        float remainingToNextLevel = 0;

        boolean addLevel = true;

        for(int i=offset; i<offset+99; i++) {
            currentLevelRequirement = levels.get(i).getAsFloat();
            xpTotal += currentLevelRequirement;

            if(addLevel) {
                if(xpTotal > exp) {
                    remainingToNextLevel = (exp-(xpTotal-currentLevelRequirement))/currentLevelRequirement;
                    addLevel = false;
                } else {
                    level += 1;
                }
            }
        }

        level += remainingToNextLevel;
        if(level <= 0) {
            level = 1;
        } else if(level > 100) {
            level = 100;
        }
        Level levelObj = new Level();
        levelObj.level = level;
        levelObj.currentLevelRequirement = currentLevelRequirement;
        levelObj.maxXP = xpTotal;
        return levelObj;
    }

    private static final HashMap<String, HashMap<String, Float>> PET_STAT_BOOSTS = new HashMap<>();
    static {
        HashMap<String, Float> bigTeeth = new HashMap<>();
        bigTeeth.put("CRIT_CHANCE", 5f);
        PET_STAT_BOOSTS.put("PET_ITEM_BIG_TEETH_COMMON", bigTeeth);

        HashMap<String, Float> hardenedScales = new HashMap<>();
        hardenedScales.put("DEFENCE", 25f);
        PET_STAT_BOOSTS.put("PET_ITEM_HARDENED_SCALES_UNCOMMON", hardenedScales);

        HashMap<String, Float> luckyClover = new HashMap<>();
        luckyClover.put("MAGIC_FIND", 7f);
        PET_STAT_BOOSTS.put("PET_ITEM_LUCKY_CLOVER", luckyClover);

        HashMap<String, Float> sharpenedClaws = new HashMap<>();
        sharpenedClaws.put("CRIT_DAMAGE", 15f);
        PET_STAT_BOOSTS.put("PET_ITEM_SHARPENED_CLAWS_UNCOMMON", sharpenedClaws);
    }
    private static final HashMap<String, HashMap<String, Float>> PET_STAT_BOOSTS_MULT = new HashMap<>();
    static {
        HashMap<String, Float> ironClaws = new HashMap<>();
        ironClaws.put("CRIT_DAMAGE", 1.4f);
        ironClaws.put("CRIT_CHANCE", 1.4f);
        PET_STAT_BOOSTS_MULT.put("PET_ITEM_IRON_CLAWS_COMMON", ironClaws);

        HashMap<String, Float> textbook = new HashMap<>();
        textbook.put("INTELLIGENCE", 2f);
        PET_STAT_BOOSTS_MULT.put("PET_ITEM_TEXTBOOK", textbook);
    }

    private int selectedPet = -1;
    private int petsPage = 0;
    private List<JsonObject> sortedPets = null;
    private List<ItemStack> sortedPetsStack = null;
    private static HashMap<String, String> minionRarityToNumMap = new HashMap<>();
    static {
        minionRarityToNumMap.put("COMMON", "0");
        minionRarityToNumMap.put("UNCOMMON", "1");
        minionRarityToNumMap.put("RARE", "2");
        minionRarityToNumMap.put("EPIC", "3");
        minionRarityToNumMap.put("LEGENDARY", "4");
    }
    private void drawPetsPage(int mouseX, int mouseY, float partialTicks) {
        JsonObject petsInfo = profile.getPetsInfo(profileId);
        if(petsInfo == null) return;
        JsonObject petsJson = Utils.getConstant("pets");
        if(petsJson == null) return;

        String location = null;
        JsonObject status = profile.getPlayerStatus();
        if(status != null && status.has("mode")) {
            location = status.get("mode").getAsString();
        }

        backgroundRotation += (currentTime - lastTime)/400f;
        backgroundRotation %= 360;

        String panoramaIdentifier = "day";
        if(SBScoreboardData.getInstance().currentTimeDate != null) {
            if(SBScoreboardData.getInstance().currentTimeDate.getHours() <= 6 ||
                    SBScoreboardData.getInstance().currentTimeDate.getHours() >= 20) {
                panoramaIdentifier = "night";
            }
        }

        JsonArray pets = petsInfo.get("pets").getAsJsonArray();
        if(sortedPets == null) {
            sortedPets = new ArrayList<>();
            sortedPetsStack = new ArrayList<>();
            for(int i=0; i<pets.size(); i++) {
                sortedPets.add(pets.get(i).getAsJsonObject());
            }
            sortedPets.sort((pet1, pet2) -> {
                String tier1 = pet1.get("tier").getAsString();
                String tierNum1 = minionRarityToNumMap.get(tier1);
                int tierNum1I = Integer.parseInt(tierNum1);
                float exp1 = pet1.get("exp").getAsFloat();

                String tier2 = pet2.get("tier").getAsString();
                String tierNum2 = minionRarityToNumMap.get(tier2);
                int tierNum2I = Integer.parseInt(tierNum2);
                float exp2 = pet2.get("exp").getAsFloat();

                if(tierNum1I != tierNum2I) {
                    return tierNum2I - tierNum1I;
                } else {
                    return (int)(exp2 - exp1);
                }
            });
            for(JsonObject pet : sortedPets) {
                String petname = pet.get("type").getAsString();
                String tier = pet.get("tier").getAsString();
                String heldItem = Utils.getElementAsString(pet.get("heldItem"), null);
                JsonObject heldItemJson = heldItem==null?null:NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(heldItem);
                String tierNum = minionRarityToNumMap.get(tier);
                float exp = pet.get("exp").getAsFloat();
                if(tierNum == null) continue;

                int petRarityOffset = petsJson.get("pet_rarity_offset").getAsJsonObject().get(tier).getAsInt();
                JsonArray levelsArr = petsJson.get("pet_levels").getAsJsonArray();

                Level levelObj = getLevel(levelsArr, petRarityOffset, exp);
                float level = levelObj.level;
                float currentLevelRequirement = levelObj.currentLevelRequirement;
                float maxXP = levelObj.maxXP;
                pet.addProperty("level", level);
                pet.addProperty("currentLevelRequirement", currentLevelRequirement);
                pet.addProperty("maxXP", maxXP);

                JsonObject petItem = NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(petname+";"+tierNum);
                if(petItem == null) continue;

                ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(petItem, false, false);
                HashMap<String, String> replacements = NotEnoughUpdates.INSTANCE.manager.getLoreReplacements(petname, tier, (int)Math.floor(level));

                if(heldItem != null) {
                    HashMap<String, Float> petStatBoots = PET_STAT_BOOSTS.get(heldItem);
                    HashMap<String, Float> petStatBootsMult = PET_STAT_BOOSTS_MULT.get(heldItem);
                    if(petStatBoots != null) {
                        for(Map.Entry<String, Float> entryBoost : petStatBoots.entrySet()) {
                            try {
                                float value = Float.parseFloat(replacements.get(entryBoost.getKey()));
                                replacements.put(entryBoost.getKey(), String.valueOf((int)Math.floor(value+entryBoost.getValue())));
                            } catch(Exception ignored) {}
                        }

                    }
                    if(petStatBootsMult != null) {
                        for(Map.Entry<String, Float> entryBoost : petStatBootsMult.entrySet()) {
                            try {
                                float value = Float.parseFloat(replacements.get(entryBoost.getKey()));
                                replacements.put(entryBoost.getKey(), String.valueOf((int)Math.floor(value*entryBoost.getValue())));
                            } catch(Exception ignored) {}
                        }
                    }
                }

                NBTTagCompound tag = stack.getTagCompound()==null?new NBTTagCompound():stack.getTagCompound();
                if(tag.hasKey("display", 10)) {
                    NBTTagCompound display = tag.getCompoundTag("display");
                    if(display.hasKey("Lore", 9)) {
                        NBTTagList newNewLore = new NBTTagList();
                        NBTTagList newLore = new NBTTagList();
                        NBTTagList lore = display.getTagList("Lore", 8);
                        HashMap<Integer, Integer> blankLocations = new HashMap<>();
                        for(int j=0; j<lore.tagCount(); j++) {
                            String line = lore.getStringTagAt(j);
                            if(line.trim().isEmpty()) {
                                blankLocations.put(blankLocations.size(), j);
                            }
                            for(Map.Entry<String, String> replacement : replacements.entrySet()) {
                                line = line.replace("{"+replacement.getKey()+"}", replacement.getValue());
                            }
                            newLore.appendTag(new NBTTagString(line));
                        }
                        Integer secondLastBlank = blankLocations.get(blankLocations.size()-2);
                        if(heldItemJson != null && secondLastBlank != null) {
                            for(int j=0; j<newLore.tagCount(); j++) {
                                String line = newLore.getStringTagAt(j);

                                if(j == secondLastBlank.intValue()) {
                                    newNewLore.appendTag(new NBTTagString(""));
                                    newNewLore.appendTag(new NBTTagString(EnumChatFormatting.GOLD+"Held Item: "+heldItemJson.get("displayname").getAsString()));
                                    int blanks = 0;
                                    JsonArray heldItemLore = heldItemJson.get("lore").getAsJsonArray();
                                    for(int k=0; k<heldItemLore.size(); k++) {
                                        String heldItemLine = heldItemLore.get(k).getAsString();
                                        if(heldItemLine.trim().isEmpty()) {
                                            blanks++;
                                        } else if(blanks==2) {
                                            newNewLore.appendTag(new NBTTagString(heldItemLine));
                                        } else if(blanks>2) {
                                            break;
                                        }
                                    }
                                }

                                newNewLore.appendTag(new NBTTagString(line));
                            }
                            display.setTag("Lore", newNewLore);
                        } else {
                            display.setTag("Lore", newLore);
                        }
                    }
                    if(display.hasKey("Name", 8)) {
                        String displayName = display.getString("Name");
                        for(Map.Entry<String, String> replacement : replacements.entrySet()) {
                            displayName = displayName.replace("{"+replacement.getKey()+"}", replacement.getValue());
                        }
                        display.setTag("Name", new NBTTagString(displayName));
                    }
                    tag.setTag("display", display);
                }
                stack.setTagCompound(tag);

                sortedPetsStack.add(stack);
            }
        }

        Panorama.drawPanorama(-backgroundRotation, guiLeft+212, guiTop+44, 81, 108, -0.37f, 0.6f,
                getPanoramasForLocation(location==null?"dynamic":location, panoramaIdentifier));

        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_pets);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);

        Utils.drawStringCentered(EnumChatFormatting.DARK_PURPLE+"Pets", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+100, guiTop+14, true, 4210752);
        GlStateManager.color(1, 1, 1, 1);

        JsonElement activePetElement = petsInfo.get("active_pet");
        if(selectedPet == -1 && activePetElement != null && activePetElement.isJsonObject()) {
            JsonObject active = activePetElement.getAsJsonObject();
            for(int i=0; i<sortedPets.size(); i++) {
                if(sortedPets.get(i) == active) {
                    selectedPet = i;
                    break;
                }
            }
        }

        boolean leftHovered = false;
        boolean rightHovered = false;
        if(Mouse.isButtonDown(0)) {
            if(mouseY > guiTop+6 && mouseY < guiTop+22) {
                if(mouseX > guiLeft+100-20-12 && mouseX < guiLeft+100-20) {
                    leftHovered = true;
                } else if(mouseX > guiLeft+100+20 && mouseX < guiLeft+100+20+12) {
                    rightHovered = true;
                }
            }
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(resource_packs);

        if(petsPage > 0) {
            Utils.drawTexturedRect(guiLeft+100-15-12, guiTop+6, 12, 16,
                    29/256f, 53/256f, !leftHovered?0:32/256f, !leftHovered?32/256f:64/256f, GL11.GL_NEAREST);
        }
        if(petsPage < Math.ceil(pets.size()/25f)-1) {
            Utils.drawTexturedRect( guiLeft+100+15, guiTop+6, 12, 16,
                    5/256f, 29/256f, !rightHovered?0:32/256f, !rightHovered?32/256f:64/256f, GL11.GL_NEAREST);
        }

        for(int i=petsPage*20; i<Math.min(petsPage*20+20, sortedPets.size()); i++) {
            JsonObject pet = sortedPets.get(i);
            ItemStack stack = sortedPetsStack.get(i);
            if(pet != null) {
                int xIndex = (i%20) % COLLS_XCOUNT;
                int yIndex = (i%20) / COLLS_XCOUNT;

                float x = 5 + COLLS_XPADDING + (COLLS_XPADDING + 20) * xIndex;
                float y = 7 + COLLS_YPADDING + (COLLS_YPADDING + 20) * yIndex;

                Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
                if(i == selectedPet) {
                    GlStateManager.color(1, 185/255f, 0, 1);
                    Utils.drawTexturedRect(guiLeft+x, guiTop+y, 20, 20,
                            0, 20/256f, 0, 20/256f, GL11.GL_NEAREST);
                } else {
                    GlStateManager.color(1, 1, 1, 1);
                    Utils.drawTexturedRect(guiLeft+x, guiTop+y, 20, 20,
                            0, 20/256f, 0, 20/256f, GL11.GL_NEAREST);
                }

                Utils.drawItemStack(stack, guiLeft+(int)x+2, guiTop+(int)y+2);

                if(mouseX > guiLeft+x && mouseX < guiLeft+x+20) {
                    if(mouseY > guiTop+y && mouseY < guiTop+y+20) {
                        tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                    }
                }
            }
        }

        if(selectedPet >= 0) {
            ItemStack petStack = sortedPetsStack.get(selectedPet);
            String display = petStack.getDisplayName();
            JsonObject pet = sortedPets.get(selectedPet);
            String type = pet.get("type").getAsString();

            for(int i=0; i<4; i++) {
                JsonObject item = NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(type+";"+i);
                if(item != null) {
                    int x = guiLeft+280;
                    float y = guiTop+67+15*(float)Math.sin(((currentTime-startTime)/800f)%(2*Math.PI));

                    int displayLen = Minecraft.getMinecraft().fontRendererObj.getStringWidth(display);
                    int halfDisplayLen = displayLen/2;

                    GlStateManager.translate(x, y, 0);

                    drawRect(-halfDisplayLen-1-28, -1, halfDisplayLen+1-28, 8, new Color(0, 0, 0, 100).getRGB());

                    Minecraft.getMinecraft().fontRendererObj.drawString(display, -halfDisplayLen-28, 0, 0, true);

                    ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(item);
                    GlStateManager.scale(-3.5f, 3.5f, 1);
                    GlStateManager.enableDepth();
                    Utils.drawItemStack(stack, 0, 0);
                    GlStateManager.scale(-1/3.5f, 1/3.5f, 1);
                    GlStateManager.translate(-x, -y, 0);
                    break;
                }
            }

            float level = pet.get("level").getAsFloat();
            float currentLevelRequirement = pet.get("currentLevelRequirement").getAsFloat();
            float exp = pet.get("exp").getAsFloat();
            float maxXP = pet.get("maxXP").getAsFloat();

            String[] split = display.split("] ");
            String colouredName = split[split.length-1];

            renderAlignedString(colouredName, EnumChatFormatting.WHITE+"Level "+(int)Math.floor(level), guiLeft+319, guiTop+28, 98);

            //Utils.drawStringCenteredScaledMaxWidth(, Minecraft.getMinecraft().fontRendererObj, guiLeft+368, guiTop+28+4, true, 98, 0);
            //renderAlignedString(display, EnumChatFormatting.YELLOW+"[LVL "+Math.floor(level)+"]", guiLeft+319, guiTop+28, 98);
            renderBar(guiLeft+319, guiTop+38, 98, (float)Math.floor(level)/100f);

            renderAlignedString(EnumChatFormatting.YELLOW+"To Next LVL", EnumChatFormatting.WHITE.toString()+(int)(level%1*100)+"%", guiLeft+319, guiTop+46, 98);
            renderBar(guiLeft+319, guiTop+56, 98, level%1);

            renderAlignedString(EnumChatFormatting.YELLOW+"To Max LVL", EnumChatFormatting.WHITE.toString()+Math.min(100, (int)(exp/maxXP*100))+"%", guiLeft+319, guiTop+64, 98);
            renderBar(guiLeft+319, guiTop+74, 98, exp/maxXP);

            renderAlignedString(EnumChatFormatting.YELLOW+"Total XP", EnumChatFormatting.WHITE.toString()+shortNumberFormat(exp, 0), guiLeft+319, guiTop+125, 98);
            renderAlignedString(EnumChatFormatting.YELLOW+"Current LVL XP",
                        EnumChatFormatting.WHITE.toString()+shortNumberFormat((level%1)*currentLevelRequirement, 0), guiLeft+319, guiTop+143, 98);
            renderAlignedString(EnumChatFormatting.YELLOW+"Required LVL XP", EnumChatFormatting.WHITE.toString()+shortNumberFormat(currentLevelRequirement, 0), guiLeft+319, guiTop+161, 98);

        }

    }

    private String[] romans = new String[]{"I","II","III","IV","V","VI","VII","VIII","IX","X","XI",
            "XII","XIII","XIV","XV","XVI","XVII","XIX","XX"};

    private final int COLLS_XCOUNT = 5;
    private final int COLLS_YCOUNT = 4;
    private final float COLLS_XPADDING = (190-COLLS_XCOUNT*20)/(float)(COLLS_XCOUNT+1);
    private final float COLLS_YPADDING = (202-COLLS_YCOUNT*20)/(float)(COLLS_YCOUNT+1);

    private void drawColsPage(int mouseX, int mouseY, float partialTicks) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_cols);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);

        JsonObject collectionInfo = profile.getCollectionInfo(profileId);
        if(collectionInfo == null) {
            Utils.drawStringCentered(EnumChatFormatting.RED+"Collection API not enabled!", Minecraft.getMinecraft().fontRendererObj,
                    guiLeft+134, guiTop+101, true, 0);
            return;
        }
        JsonObject resourceCollectionInfo = ProfileViewer.getResourceCollectionInformation();
        if(resourceCollectionInfo == null) return;

        int collectionCatSize = ProfileViewer.getCollectionCatToCollectionMap().size();
        int collectionCatYSize = (int)(162f/(collectionCatSize-1+0.0000001f));
        {
            int yIndex = 0;
            for(ItemStack stack : ProfileViewer.getCollectionCatToCollectionMap().keySet()) {
                if(selectedCollectionCategory == null) selectedCollectionCategory = stack;
                Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
                if(stack == selectedCollectionCategory) {
                    Utils.drawTexturedRect(guiLeft+7, guiTop+10+collectionCatYSize*yIndex, 20, 20,
                            20/256f, 0, 20/256f, 0, GL11.GL_NEAREST);
                    Utils.drawItemStackWithText(stack, guiLeft+10, guiTop+13+collectionCatYSize*yIndex, ""+(yIndex+1));
                } else {
                    Utils.drawTexturedRect(guiLeft+7, guiTop+10+collectionCatYSize*yIndex, 20, 20,
                            0, 20/256f, 0, 20/256f, GL11.GL_NEAREST);
                    Utils.drawItemStackWithText(stack, guiLeft+9, guiTop+12+collectionCatYSize*yIndex, ""+(yIndex+1));
                }
                yIndex++;
            }
        }

        Utils.drawStringCentered(selectedCollectionCategory.getDisplayName() + " Collections", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+134, guiTop+14, true, 4210752);

        JsonObject minionTiers = collectionInfo.get("minion_tiers").getAsJsonObject();
        JsonObject collectionTiers = collectionInfo.get("collection_tiers").getAsJsonObject();
        JsonObject maxAmounts = collectionInfo.get("max_amounts").getAsJsonObject();
        JsonObject totalAmounts = collectionInfo.get("total_amounts").getAsJsonObject();
        JsonObject personalAmounts = collectionInfo.get("personal_amounts").getAsJsonObject();

        List<String> collections = ProfileViewer.getCollectionCatToCollectionMap().get(selectedCollectionCategory);
        if(collections != null) {
            for(int i=0; i<collections.size(); i++) {
                String collection = collections.get(i);
                if(collection != null) {
                    ItemStack collectionItem = ProfileViewer.getCollectionToCollectionDisplayMap().get(collection);
                    if(collectionItem != null) {
                        int xIndex = i%COLLS_XCOUNT;
                        int yIndex = i/COLLS_XCOUNT;

                        float x = 39+COLLS_XPADDING+(COLLS_XPADDING+20)*xIndex;
                        float y = 7+COLLS_YPADDING+(COLLS_YPADDING+20)*yIndex;

                        String tierString;
                        int tier = (int)Utils.getElementAsFloat(collectionTiers.get(collection), 0);
                        if(tier > 20 || tier < 0) {
                            tierString = String.valueOf(tier);
                        } else {
                            tierString = romans[tier];
                        }
                        float amount = Utils.getElementAsFloat(totalAmounts.get(collection), 0);
                        float maxAmount = Utils.getElementAsFloat(maxAmounts.get(collection), 0);
                        Color color = new Color(128, 128, 128, 255);
                        int tierStringColour = color.getRGB();
                        float completedness = 0;
                        if(maxAmount > 0) {
                            completedness = amount/maxAmount;
                        }
                        completedness = Math.min(1, completedness);
                        if(maxAmounts.has(collection) && completedness >= 1) {
                            tierStringColour = new Color(255, 215, 0).getRGB();
                        }

                        GlStateManager.color(1, 1, 1, 1);
                        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
                        Utils.drawTexturedRect(guiLeft+x, guiTop+y, 20, 20*(1-completedness),
                                0, 20/256f, 0, 20*(1-completedness)/256f, GL11.GL_NEAREST);
                        GlStateManager.color(1, 185/255f, 0, 1);
                        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
                        Utils.drawTexturedRect(guiLeft+x, guiTop+y+20*(1-completedness), 20, 20*(completedness),
                                0, 20/256f, 20*(1-completedness)/256f, 20/256f, GL11.GL_NEAREST);
                        Utils.drawItemStack(collectionItem, guiLeft+(int)x+2, guiTop+(int)y+2);

                        if(mouseX > guiLeft+(int)x+2 && mouseX < guiLeft+(int)x+18) {
                            if(mouseY > guiTop+(int)y+2 && mouseY < guiTop+(int)y+18) {
                                tooltipToDisplay = new ArrayList<>();
                                tooltipToDisplay.add(collectionItem.getDisplayName() + " " +
                                        (completedness>=1?EnumChatFormatting.GOLD:EnumChatFormatting.GRAY) + tierString);
                                tooltipToDisplay.add("Collected: " + numberFormat.format(Utils.getElementAsFloat(personalAmounts.get(collection), 0)));
                                tooltipToDisplay.add("Total Collected: " + numberFormat.format(amount));
                            }
                        }

                        GlStateManager.color(1, 1, 1, 1);
                        if(tier >= 0) {
                            Utils.drawStringCentered(tierString, fontRendererObj,
                                    guiLeft+x+10, guiTop+y-4, true,
                                    tierStringColour);
                        }

                        Utils.drawStringCentered(shortNumberFormat(amount, 0)+"", fontRendererObj,
                                guiLeft+x+10, guiTop+y+26, true,
                                color.getRGB());
                    }
                }
            }
        }

        Utils.drawStringCentered(selectedCollectionCategory.getDisplayName() + " Minions", Minecraft.getMinecraft().fontRendererObj,
                guiLeft+326, guiTop+14, true, 4210752);

        float MAX_MINION_TIER = 11f;
        List<String> minions = ProfileViewer.getCollectionCatToMinionMap().get(selectedCollectionCategory);
        if(minions != null) {
            for(int i=0; i<minions.size(); i++) {
                String minion = minions.get(i);
                if(minion != null) {
                    JsonObject minionJson = NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(minion+"_GENERATOR_1");
                    if(minionJson != null) {
                        int xIndex = i%COLLS_XCOUNT;
                        int yIndex = i/COLLS_XCOUNT;

                        float x = 231+COLLS_XPADDING+(COLLS_XPADDING+20)*xIndex;
                        float y = 7+COLLS_YPADDING+(COLLS_YPADDING+20)*yIndex;

                        String tierString;
                        int tier = (int)Utils.getElementAsFloat(minionTiers.get(minion), 0);
                        if(tier-1 >= romans.length || tier-1 < 0) {
                            tierString = String.valueOf(tier);
                        } else {
                            tierString = romans[tier-1];
                        }

                        Color color = new Color(128, 128, 128, 255);
                        int tierStringColour = color.getRGB();
                        float completedness = tier/MAX_MINION_TIER;

                        completedness = Math.min(1, completedness);
                        if(completedness >= 1) {
                            tierStringColour = new Color(255, 215, 0).getRGB();
                        }

                        GlStateManager.color(1, 1, 1, 1);
                        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
                        Utils.drawTexturedRect(guiLeft+x, guiTop+y, 20, 20*(1-completedness),
                                0, 20/256f, 0, 20*(1-completedness)/256f, GL11.GL_NEAREST);
                        GlStateManager.color(1, 185/255f, 0, 1);
                        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
                        Utils.drawTexturedRect(guiLeft+x, guiTop+y+20*(1-completedness), 20, 20*(completedness),
                                0, 20/256f, 20*(1-completedness)/256f, 20/256f, GL11.GL_NEAREST);

                        Utils.drawItemStack(NotEnoughUpdates.INSTANCE.manager.jsonToStack(minionJson), guiLeft+(int)x+2, guiTop+(int)y+2);

                        if(mouseX > guiLeft+(int)x+2 && mouseX < guiLeft+(int)x+18) {
                            if(mouseY > guiTop+(int)y+2 && mouseY < guiTop+(int)y+18) {
                                tooltipToDisplay = NotEnoughUpdates.INSTANCE.manager.jsonToStack(minionJson)
                                        .getTooltip(Minecraft.getMinecraft().thePlayer, false);
                            }
                        }

                        GlStateManager.color(1, 1, 1, 1);
                        if(tier >= 0) {
                            Utils.drawStringCentered(tierString, fontRendererObj,
                                    guiLeft+x+10, guiTop+y-4, true,
                                    tierStringColour);
                        }
                    }
                }
            }
        }

        //190
    }

    private static final LinkedHashMap<String, ItemStack> invNameToDisplayMap = new LinkedHashMap<>();
    static {
        invNameToDisplayMap.put("inv_contents", Utils.createItemStack(Item.getItemFromBlock(Blocks.chest), EnumChatFormatting.GRAY+"Inventory"));
        invNameToDisplayMap.put("ender_chest_contents", Utils.createItemStack(Item.getItemFromBlock(Blocks.ender_chest), EnumChatFormatting.GRAY+"Ender Chest"));
        invNameToDisplayMap.put("talisman_bag", Utils.createItemStack(Items.golden_apple, EnumChatFormatting.GRAY+"Accessory Bag"));
        invNameToDisplayMap.put("wardrobe_contents", Utils.createItemStack(Items.leather_chestplate, EnumChatFormatting.GRAY+"Wardrobe"));
        invNameToDisplayMap.put("fishing_bag", Utils.createItemStack(Items.fish, EnumChatFormatting.GRAY+"Fishing Bag"));
        invNameToDisplayMap.put("potion_bag", Utils.createItemStack(Items.potionitem, EnumChatFormatting.GRAY+"Potion Bag"));
    }

    public int countItemsInInventory(String internalname, JsonObject inventoryInfo, String... invsToSearch) {
        int count = 0;
        for(String inv : invsToSearch) {
            JsonArray invItems = inventoryInfo.get(inv).getAsJsonArray();
            for(int i=0; i<invItems.size(); i++) {
                if(invItems.get(i) == null || !invItems.get(i).isJsonObject()) continue;
                JsonObject item = invItems.get(i).getAsJsonObject();
                if(item.get("internalname").getAsString().equals(internalname)) {
                    if(item.has("count")) {
                        count += item.get("count").getAsInt();
                    } else {
                        count += 1;
                    }
                }
            }
        }
        return count;
    }

    private static final Pattern DAMAGE_PATTERN = Pattern.compile("^Damage: \\+([0-9]+)");
    private static final Pattern STRENGTH_PATTERN = Pattern.compile("^Strength: \\+([0-9]+)");
    private static final Pattern FISHSPEED_PATTERN = Pattern.compile("^Increases fishing speed by \\+([0-9]+)");

    private ItemStack[] findBestItems(JsonObject inventoryInfo, int numItems, String[] invsToSearch, String[] typeMatches, Pattern... importantPatterns) {
        ItemStack[] bestItems = new ItemStack[numItems];
        TreeMap<Integer, Set<ItemStack>> map = new TreeMap<>();
        for(String inv : invsToSearch) {
            JsonArray invItems = inventoryInfo.get(inv).getAsJsonArray();
            for(int i=0; i<invItems.size(); i++) {
                if(invItems.get(i) == null || !invItems.get(i).isJsonObject()) continue;
                JsonObject item = invItems.get(i).getAsJsonObject();
                JsonArray lore = item.get("lore").getAsJsonArray();
                if(Utils.checkItemType(lore, true, typeMatches) >= 0) {
                    int importance = 0;
                    for(int j=0; j<lore.size(); j++) {
                        String line = lore.get(j).getAsString();
                        for(Pattern pattern : importantPatterns) {
                            Matcher matcher = pattern.matcher(Utils.cleanColour(line));
                            if(matcher.find()) {
                                importance += Integer.parseInt(matcher.group(1));
                            }
                        }
                    }
                    map.computeIfAbsent(importance, k->new HashSet<>()).add(
                            NotEnoughUpdates.INSTANCE.manager.jsonToStack(item, false));
                }
            }
        }
        int i=0;
        outer:
        for(int key : map.descendingKeySet()) {
            Set<ItemStack> items = map.get(key);
            for(ItemStack item : items) {
                bestItems[i] = item;
                if(++i >= bestItems.length) break outer;
            }
        }

        return bestItems;
    }

    private int getRowsForInventory(String invName) {
        switch(invName) {
            case "wardrobe_contents":
                return 4;
        }
        return 6;
    }

    private int getIgnoredRowsForInventory(String invName) {
        switch(invName) {
            case "talisman_bag":
            case "fishing_bag":
            case "potion_bag":
                return 1;
        }
        return 0;
    }

    private int getAvailableSlotsForInventory(JsonObject inventoryInfo, JsonObject collectionInfo, String invName) {
        if(collectionInfo == null) return -1;
        JsonObject misc = Utils.getConstant("misc");
        if(misc == null) return -1;
        JsonElement sizesElement = Utils.getElement(misc, "bag_size."+invName+".sizes");
        JsonElement collectionElement = Utils.getElement(misc, "bag_size."+invName+".collection");

        if(sizesElement == null || !sizesElement.isJsonArray()) return -1;
        if(collectionElement == null || !collectionElement.isJsonPrimitive()) return -1;

        JsonArray sizes = sizesElement.getAsJsonArray();
        String collection = collectionElement.getAsString();

        JsonElement tierElement = Utils.getElement(collectionInfo, "collection_tiers."+collection);

        if(tierElement == null || !tierElement.isJsonPrimitive()) {
            return 0;
        }
        int tier = tierElement.getAsInt();

        int currentSlots = 0;
        for(int i=0; i<sizes.size(); i++) {
            JsonObject sizeInfo = sizes.get(i).getAsJsonObject();
            if(sizeInfo.get("tier").getAsInt() <= tier) {
                currentSlots = sizeInfo.get("slots").getAsInt();
            }
        }
        return currentSlots;
    }

    private ItemStack fillerStack = new ItemStack(Item.getItemFromBlock(Blocks.stained_glass_pane), 1, 15);
    public ItemStack[][][] getItemsForInventory(JsonObject inventoryInfo, JsonObject collectionInfo, String invName) {
        if(inventoryItems.containsKey(invName)) return inventoryItems.get(invName);

        JsonArray jsonInv = Utils.getElement(inventoryInfo, invName).getAsJsonArray();

        int rowSize = 9;
        int rows = jsonInv.size()/rowSize;
        int maxRowsPerPage = getRowsForInventory(invName);
        int ignoredRows = getIgnoredRowsForInventory(invName);
        int maxInvSize = rowSize*maxRowsPerPage;

        int numInventories = (jsonInv.size()-1)/maxInvSize+1;

        ItemStack[][][] inventories = new ItemStack[numInventories][][];

        int availableSlots = getAvailableSlotsForInventory(inventoryInfo, collectionInfo, invName);

        for(int i=0; i<numInventories; i++) {
            int thisRows = Math.min(maxRowsPerPage, rows-maxRowsPerPage*i)-ignoredRows;
            if(thisRows <= 0) break;

            ItemStack[][] items = new ItemStack[thisRows][rowSize];

            int invSize = Math.min(jsonInv.size(), maxInvSize+maxInvSize*i);
            for(int j=maxInvSize*i; j<invSize; j++) {
                int xIndex = (j%maxInvSize)%rowSize;
                int yIndex = (j%maxInvSize)/rowSize;
                if(invName.equals("inv_contents")) {
                    yIndex--;
                    if(yIndex < 0) yIndex = rows-1;
                }
                if(yIndex >= thisRows) {
                    break;
                }

                if(jsonInv.get(j) == null || !jsonInv.get(j).isJsonObject()) {
                    if(availableSlots >= 0) {
                        if(j >= availableSlots) {
                            items[yIndex][xIndex] = fillerStack;
                        }
                    }
                    continue;
                }

                JsonObject item = jsonInv.get(j).getAsJsonObject();
                ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(item, false);
                if(item.has("item_contents")) {
                    JsonArray bytesArr = item.get("item_contents").getAsJsonArray();
                    byte[] bytes = new byte[bytesArr.size()];
                    for(int bytesArrI=0; bytesArrI<bytesArr.size(); bytesArrI++) {
                        bytes[bytesArrI] = bytesArr.get(bytesArrI).getAsByte();
                    }
                    //byte[] bytes2 = null;
                    NBTTagCompound tag = stack.getTagCompound();
                    if(tag != null && tag.hasKey("ExtraAttributes", 10)) {
                        NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");
                        for(String key : ea.getKeySet()) {
                            if(key.endsWith("backpack_data") || key.equals("new_year_cake_bag_data")) {
                                ea.setTag(key, new NBTTagByteArray(bytes));
                                break;
                            }
                        }
                        tag.setTag("ExtraAttributes", ea);
                        stack.setTagCompound(tag);
                    }
                }
                items[yIndex][xIndex] = stack;
            }
            inventories[i] = items;
        }

        inventoryItems.put(invName, inventories);
        return inventories;
    }


    private ItemStack[] bestWeapons = null;
    private ItemStack[] bestRods = null;
    private ItemStack[] armorItems = null;
    private HashMap<String, ItemStack[][][]> inventoryItems = new HashMap<>();
    private String selectedInventory = "inv_contents";
    private int currentInventoryIndex = 0;
    private int arrowCount = -1;
    private int greenCandyCount = -1;
    private int purpleCandyCount = -1;
    private GuiElementTextField inventoryTextField = new GuiElementTextField("", GuiElementTextField.SCALE_TEXT);
    private ItemStack lastBackpack;
    private int lastBackpackX;
    private int lastBackpackY;
    private void drawInvsPage(int mouseX, int mouseY, float partialTicks) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_invs);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);
        inventoryTextField.setSize(88, 20);

        JsonObject inventoryInfo = profile.getInventoryInfo(profileId);
        if(inventoryInfo == null) return;
        JsonObject collectionInfo = profile.getCollectionInfo(profileId);

        int invNameIndex=0;
        for(Map.Entry<String, ItemStack> entry : invNameToDisplayMap.entrySet()) {
            int xIndex = invNameIndex%3;
            int yIndex = invNameIndex/3;

            int x = 19+34*xIndex;
            int y = 26+34*yIndex;

            Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
            if(entry.getKey().equals(selectedInventory)) {
                Utils.drawTexturedRect(guiLeft+x-2, guiTop+y-2, 20, 20, 20/256f, 0,
                        20/256f, 0, GL11.GL_NEAREST);
                x++;
                y++;
            } else {
                Utils.drawTexturedRect(guiLeft+x-2, guiTop+y-2, 20, 20, 0, 20/256f,
                        0, 20/256f, GL11.GL_NEAREST);
            }

            Utils.drawItemStackWithText(entry.getValue(), guiLeft+x, guiTop+y, ""+(invNameIndex+1));

            if(mouseX >= guiLeft+x && mouseX <= guiLeft+x+16) {
                if(mouseY >= guiTop+y && mouseY <= guiTop+y+16) {
                    tooltipToDisplay = entry.getValue().getTooltip(Minecraft.getMinecraft().thePlayer, false);
                }
            }

            invNameIndex++;
        }

        inventoryTextField.render(guiLeft+19, guiTop+sizeY-26-20);

        ItemStack[][][] inventories = getItemsForInventory(inventoryInfo, collectionInfo, selectedInventory);
        if(currentInventoryIndex >= inventories.length) currentInventoryIndex = inventories.length-1;
        if(currentInventoryIndex < 0) currentInventoryIndex = 0;

        ItemStack[][] inventory = inventories[currentInventoryIndex];
        if(inventory == null) {
            Utils.drawStringCentered(EnumChatFormatting.RED+"Inventory API not enabled!", Minecraft.getMinecraft().fontRendererObj,
                    guiLeft+317, guiTop+101, true, 0);
            return;
        }

        if(bestWeapons == null) {
            bestWeapons = findBestItems(inventoryInfo, 6, new String[]{"inv_contents", "ender_chest_contents"},
                    new String[]{"SWORD","BOW"}, DAMAGE_PATTERN, STRENGTH_PATTERN);
        }
        if(bestRods == null) {
            bestRods = findBestItems(inventoryInfo, 3, new String[]{"inv_contents", "ender_chest_contents"},
                    new String[]{"FISHING ROD"}, FISHSPEED_PATTERN);
        }

        for(int i=0; i<bestWeapons.length; i++) {
            if(bestWeapons[i] == null) continue;
            ItemStack stack = bestWeapons[i];
            Utils.drawItemStack(stack, guiLeft+143, guiTop+13+18*i);
            if(mouseX >= guiLeft+143-1 && mouseX <= guiLeft+143+16+1) {
                if(mouseY >= guiTop+13+18*i-1 && mouseY <= guiTop+13+18*i+16+1) {
                    tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                }
            }
        }

        for(int i=0; i<bestRods.length; i++) {
            if(bestRods[i] == null) continue;
            ItemStack stack = bestRods[i];
            Utils.drawItemStack(stack, guiLeft+143, guiTop+137+18*i);
            if(mouseX >= guiLeft+143-1 && mouseX <= guiLeft+143+16+1) {
                if(mouseY >= guiTop+137+18*i-1 && mouseY <= guiTop+137+18*i+16+1) {
                    tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                }
            }
        }

        if(armorItems == null) {
            armorItems = new ItemStack[4];
            JsonArray armor = Utils.getElement(inventoryInfo, "inv_armor").getAsJsonArray();
            for(int i=0; i<armor.size(); i++) {
                if(armor.get(i) == null || !armor.get(i).isJsonObject()) continue;
                armorItems[i] = NotEnoughUpdates.INSTANCE.manager.jsonToStack(armor.get(i).getAsJsonObject(), false);
            }
        }

        for(int i=0; i<armorItems.length; i++) {
            ItemStack stack = armorItems[i];
            if(stack != null) {
                Utils.drawItemStack(stack, guiLeft+173, guiTop+67-18*i);
                if(stack != fillerStack) {
                    if(mouseX >= guiLeft+173-1 && mouseX <= guiLeft+173+16+1) {
                        if(mouseY >= guiTop+67-18*i-1 && mouseY <= guiTop+67-18*i+16+1) {
                            tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                        }
                    }
                }
            }
        }

        if(arrowCount == -1) {
            arrowCount = countItemsInInventory("ARROW", inventoryInfo, "quiver");
        }
        if(greenCandyCount == -1) {
            greenCandyCount = countItemsInInventory("GREEN_CANDY", inventoryInfo, "candy_inventory_contents");
        }
        if(purpleCandyCount == -1) {
            purpleCandyCount = countItemsInInventory("PURPLE_CANDY", inventoryInfo, "candy_inventory_contents");
        }

        Utils.drawItemStackWithText(NotEnoughUpdates.INSTANCE.manager.jsonToStack(
                NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("ARROW")), guiLeft+173, guiTop+101,
                ""+(arrowCount>999?shortNumberFormat(arrowCount, 0):arrowCount));
        Utils.drawItemStackWithText(NotEnoughUpdates.INSTANCE.manager.jsonToStack(
                NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("GREEN_CANDY")), guiLeft+173, guiTop+119, ""+greenCandyCount);
        Utils.drawItemStackWithText(NotEnoughUpdates.INSTANCE.manager.jsonToStack(
                NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("PURPLE_CANDY")), guiLeft+173, guiTop+137, ""+purpleCandyCount);
        if(mouseX > guiLeft+173 && mouseX < guiLeft+173+16) {
            if(mouseY > guiTop+101 && mouseY < guiTop+137+16) {
                if(mouseY < guiTop+101+17) {
                    tooltipToDisplay = Utils.createList(EnumChatFormatting.WHITE+"Arrow "+EnumChatFormatting.GRAY+"x"+arrowCount);
                } else if(mouseY < guiTop+119+17) {
                    tooltipToDisplay = Utils.createList(EnumChatFormatting.GREEN+"Green Candy "+EnumChatFormatting.GRAY+"x"+greenCandyCount);
                } else {
                    tooltipToDisplay = Utils.createList(EnumChatFormatting.DARK_PURPLE+"Purple Candy "+EnumChatFormatting.GRAY+"x"+purpleCandyCount);
                }
            }
        }

        int inventoryRows = inventory.length;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(CHEST_GUI_TEXTURE);

        int invSizeY = inventoryRows*18+17+7;

        int x = guiLeft+320-176/2;
        int y = guiTop+101-invSizeY/2;

        this.drawTexturedModalRect(x, y, 0, 0, 176, inventoryRows*18+17);
        this.drawTexturedModalRect(x, y+inventoryRows*18+17, 0, 215, 176, 7);

        boolean leftHovered = false;
        boolean rightHovered = false;
        if(Mouse.isButtonDown(0)) {
            if(mouseY > y+invSizeY && mouseY < y+invSizeY+16) {
                if(mouseX > guiLeft+320-12 && mouseX < guiLeft+320+12) {
                    if(mouseX < guiLeft+320) {
                        leftHovered = true;
                    } else {
                        rightHovered = true;
                    }
                }
            }
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(resource_packs);

        if(currentInventoryIndex > 0) {
            Utils.drawTexturedRect(guiLeft+320-12, y+invSizeY, 12, 16,
                    29/256f, 53/256f, !leftHovered?0:32/256f, !leftHovered?32/256f:64/256f, GL11.GL_NEAREST);
        }
        if(currentInventoryIndex < inventories.length-1) {
            Utils.drawTexturedRect(guiLeft+320, y+invSizeY, 12, 16,
                    5/256f, 29/256f, !rightHovered?0:32/256f, !rightHovered?32/256f:64/256f, GL11.GL_NEAREST);
        }

        fontRendererObj.drawString(Utils.cleanColour(invNameToDisplayMap.get(selectedInventory).getDisplayName()), x+8, y+6, 4210752);

        ItemStack stackToRender = null;
        int overlay = new Color(0, 0, 0, 100).getRGB();
        for(int yIndex=0; yIndex<inventory.length; yIndex++) {
            if(inventory[yIndex] == null) continue;

            for(int xIndex=0; xIndex<inventory[yIndex].length; xIndex++) {
                ItemStack stack = inventory[yIndex][xIndex];

                if(stack != null) Utils.drawItemStack(stack, x+8+xIndex*18, y+18+yIndex*18);

                if(inventoryTextField.getText() != null && !inventoryTextField.getText().isEmpty() &&
                        (stack == null || !NotEnoughUpdates.INSTANCE.manager.doesStackMatchSearch(stack, inventoryTextField.getText()))) {
                    GlStateManager.translate(0, 0, 50);
                    drawRect(x+8+xIndex*18, y+18+yIndex*18, x+8+xIndex*18+16, y+18+yIndex*18+16, overlay);
                    GlStateManager.translate(0, 0, -50);
                }

                if(stack == null || stack == fillerStack) continue;

                if(mouseX >= x+8+xIndex*18 && mouseX <= x+8+xIndex*18+16) {
                    if(mouseY >= y+18+yIndex*18 && mouseY <= y+18+yIndex*18+16) {
                        stackToRender = stack;
                    }
                }
            }
        }
        if(stackToRender == null && !SBAIntegration.isFreezeBackpack()) lastBackpack = null;
        if(SBAIntegration.isFreezeBackpack()) {
            if(lastBackpack != null) {
                SBAIntegration.setActiveBackpack(lastBackpack, lastBackpackX, lastBackpackY);
                GlStateManager.translate(0, 0, 100);
                SBAIntegration.renderActiveBackpack(mouseX, mouseY, fontRendererObj);
                GlStateManager.translate(0, 0, -100);
            }
        } else {
            if(stackToRender != null) {
                String internalname = NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(stackToRender);
                boolean renderedBackpack;
                if(internalname != null && (internalname.endsWith("BACKPACK") || internalname.equals("NEW_YEAR_CAKE_BAG"))) {
                    lastBackpack = stackToRender;
                    lastBackpackX = mouseX;
                    lastBackpackY = mouseY;
                    renderedBackpack = SBAIntegration.setActiveBackpack(lastBackpack, lastBackpackX, lastBackpackY);
                    if(renderedBackpack) {
                        GlStateManager.translate(0, 0, 100);
                        renderedBackpack = SBAIntegration.renderActiveBackpack(mouseX, mouseY, fontRendererObj);
                        GlStateManager.translate(0, 0, -100);
                    }
                } else {
                    renderedBackpack = false;
                }
                if(!renderedBackpack) {
                    lastBackpack = null;
                    tooltipToDisplay = stackToRender.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                }
            }
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
        if(panoramasMap.containsKey(location+identifier)) return panoramasMap.get(location+identifier);
        try {
            ResourceLocation[] panoramasArray = new ResourceLocation[6];
            for(int i=0; i<6; i++) {
                panoramasArray[i] = new ResourceLocation("notenoughupdates:panoramas/"+location+"_"+identifier+"/panorama_"+i+".jpg");
                Minecraft.getMinecraft().getResourceManager().getResource(panoramasArray[i]);
            }
            panoramasMap.put(location+identifier, panoramasArray);
            return panoramasArray;
        } catch(IOException e) {
            try {
                ResourceLocation[] panoramasArray = new ResourceLocation[6];
                for(int i=0; i<6; i++) {
                    panoramasArray[i] = new ResourceLocation("notenoughupdates:panoramas/"+location+"/panorama_"+i+".jpg");
                    Minecraft.getMinecraft().getResourceManager().getResource(panoramasArray[i]);
                }
                panoramasMap.put(location+identifier, panoramasArray);
                return panoramasArray;
            } catch(IOException e2) {
                ResourceLocation[] panoramasArray = new ResourceLocation[6];
                for(int i=0; i<6; i++) {
                    panoramasArray[i] = new ResourceLocation("notenoughupdates:panoramas/unknown/panorama_"+i+".jpg");
                }
                panoramasMap.put(location+identifier, panoramasArray);
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

    private void renderAlignedString(String first, String second, float x, float y, int length) {
        if(fontRendererObj.getStringWidth(first + " " + second) >= length) {
            for(int xOff=-2; xOff<=2; xOff++) {
                for(int yOff=-2; yOff<=2; yOff++) {
                    if(Math.abs(xOff) != Math.abs(yOff)) {
                        Utils.drawStringCenteredScaledMaxWidth(Utils.cleanColourNotModifiers(first + " " + second), Minecraft.getMinecraft().fontRendererObj,
                                x+length/2f+xOff/2f, y+4+yOff/2f, false, length,
                                new Color(0, 0, 0, 200/Math.max(Math.abs(xOff), Math.abs(yOff))).getRGB());
                    }
                }
            }

            GlStateManager.color(1, 1, 1, 1);
            Utils.drawStringCenteredScaledMaxWidth(first + " " + second, Minecraft.getMinecraft().fontRendererObj,
                    x+length/2f, y+4, false, length, 4210752);
        } else {
            for(int xOff=-2; xOff<=2; xOff++) {
                for(int yOff=-2; yOff<=2; yOff++) {
                    if(Math.abs(xOff) != Math.abs(yOff)) {
                        fontRendererObj.drawString(Utils.cleanColourNotModifiers(first),
                                x+xOff/2f, y+yOff/2f,
                                new Color(0, 0, 0, 200/Math.max(Math.abs(xOff), Math.abs(yOff))).getRGB(), false);
                    }
                }
            }

            int secondLen = fontRendererObj.getStringWidth(second);
            GlStateManager.color(1, 1, 1, 1);
            fontRendererObj.drawString(first, x, y, 4210752, false);
            for(int xOff=-2; xOff<=2; xOff++) {
                for(int yOff=-2; yOff<=2; yOff++) {
                    if(Math.abs(xOff) != Math.abs(yOff)) {
                        fontRendererObj.drawString(Utils.cleanColourNotModifiers(second),
                                x+length-secondLen+xOff/2f, y+yOff/2f,
                                new Color(0, 0, 0, 200/Math.max(Math.abs(xOff), Math.abs(yOff))).getRGB(), false);
                    }
                }
            }

            GlStateManager.color(1, 1, 1, 1);
            fontRendererObj.drawString(second, x+length-secondLen, y, 4210752, false);
        }
    }

    private void drawBasicPage(int mouseX, int mouseY, float partialTicks) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

        String location = null;
        JsonObject status = profile.getPlayerStatus();
        if(status != null && status.has("mode")) {
            location = status.get("mode").getAsString();
        }

        int extraRotation = 0;
        if(Mouse.isButtonDown(0) || Mouse.isButtonDown(1)) {
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
            if(SBScoreboardData.getInstance().currentTimeDate.getHours() <= 6 ||
                SBScoreboardData.getInstance().currentTimeDate.getHours() >= 20) {
                panoramaIdentifier = "night";
            }
        }

        Panorama.drawPanorama(-backgroundRotation-extraRotation, guiLeft+23, guiTop+44, 81, 108, 0.37f, 0.8f,
                getPanoramasForLocation(location==null?"unknown":location, panoramaIdentifier));

        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_basic);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);

        if(entityPlayer != null && profile.getHypixelProfile() != null) {
            String playerName = null;
            if(profile.getHypixelProfile().has("prefix")) {
                playerName = Utils.getElementAsString(profile.getHypixelProfile().get("prefix"), "") + " " + entityPlayer.getName();
            } else {
                String rank;
                String monthlyPackageRank = Utils.getElementAsString(profile.getHypixelProfile().get("monthlyPackageRank"), "NONE");
                if(monthlyPackageRank.equals("NONE")) {
                    rank = Utils.getElementAsString(profile.getHypixelProfile().get("rank"),
                            Utils.getElementAsString(profile.getHypixelProfile().get("newPackageRank"), "NONE"));
                } else {
                    rank = monthlyPackageRank;
                }
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

                        playerName = EnumChatFormatting.GRAY.toString() + name;
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
                    }
                }

            }
            if(playerName != null) {
                int rankPrefixLen = fr.getStringWidth(playerName);
                int halfRankPrefixLen = rankPrefixLen/2;

                int x = guiLeft+63;
                int y = guiTop+54;

                drawRect(x-halfRankPrefixLen-1, y-1, x+halfRankPrefixLen+1, y+8, new Color(0, 0, 0, 64).getRGB());

                fr.drawString(playerName, x-halfRankPrefixLen, y, 0, true);
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
                        locationStr = Utils.getElementAsString(Utils.getElement(misc, "area_names."+location), "Unknown");
                    }
                }
            }
            if(locationStr != null) {
                statusStr += EnumChatFormatting.GRAY+" - "+EnumChatFormatting.GREEN+locationStr;
            }

            Utils.drawStringCentered(statusStr, fr, guiLeft+63, guiTop+160, true, 0);
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
            byte b = 0;
            for(EnumPlayerModelParts part : EnumPlayerModelParts.values()) {
                b |= part.getPartMask();
            }
            entityPlayer.getDataWatcher().updateObject(10, b);
        }

        JsonObject profileInfo = profile.getProfileInformation(profileId);
        if(profileInfo == null) return;

        JsonObject skillInfo = profile.getSkillInfo(profileId);
        JsonObject inventoryInfo = profile.getInventoryInfo(profileId);

        if(backgroundClickedX != -1 && Mouse.isButtonDown(1)) {
            for(int i=0; i<entityPlayer.inventory.armorInventory.length; i++) {
                entityPlayer.inventory.armorInventory[i] = null;
            }
        } else {
            if(inventoryInfo != null && inventoryInfo.has("inv_armor")) {
                JsonArray items = inventoryInfo.get("inv_armor").getAsJsonArray();
                if(items != null && items.size() == 4) {
                    for(int i=0; i<entityPlayer.inventory.armorInventory.length; i++) {
                        JsonElement itemElement = items.get(i);
                        if(itemElement != null && itemElement.isJsonObject()) {
                            entityPlayer.inventory.armorInventory[i] = NotEnoughUpdates.INSTANCE.manager.jsonToStack(itemElement.getAsJsonObject(), false);
                        }
                    }
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
        JsonObject petsInfo = profile.getPetsInfo(profileId);
        if(petsInfo != null) {
            JsonElement activePetElement = petsInfo.get("active_pet");
            if(activePetElement != null && activePetElement.isJsonObject()) {
                JsonObject activePet = activePetElement.getAsJsonObject();

                String type = activePet.get("type").getAsString();

                for(int i=0; i<4; i++) {
                    JsonObject item = NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(type+";"+i);
                    if(item != null) {
                        int x = guiLeft+50;
                        float y = guiTop+82+15*(float)Math.sin(((currentTime-startTime)/800f)%(2*Math.PI));
                        GlStateManager.translate(x, y, 0);
                        ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(item);
                        GlStateManager.scale(-1.5f, 1.5f, 1);
                        GlStateManager.enableDepth();
                        Utils.drawItemStack(stack, 0, 0);
                        GlStateManager.scale(-1/1.5f, 1/1.5f, 1);
                        GlStateManager.translate(-x, -y, 0);
                        break;
                    }
                }
            }
        }
        drawEntityOnScreen(guiLeft+63, guiTop+128+7, 36, guiLeft+63-mouseX, guiTop+129-mouseY, entityPlayer);

        PlayerStats.Stats stats = profile.getStats(profileId);

        if(stats != null) {
            Splitter splitter = Splitter.on(" ").omitEmptyStrings().limit(2);
            for(int i=0; i<PlayerStats.defaultStatNames.length; i++) {
                String statName = PlayerStats.defaultStatNames[i];
                String statNamePretty = PlayerStats.defaultStatNamesPretty[i];

                int val = Math.round(stats.get(statName));

                GlStateManager.color(1, 1, 1, 1);
                GlStateManager.enableBlend();
                GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                renderAlignedString(statNamePretty, EnumChatFormatting.WHITE.toString()+val, guiLeft+132, guiTop+32+12.5f*i, 80);

                if(mouseX > guiLeft+132 && mouseX < guiLeft+212) {
                    if(mouseY > guiTop+32+12.5f*i && mouseY < guiTop+40+12.5f*i) {
                        List<String> split = splitter.splitToList(statNamePretty);
                        PlayerStats.Stats baseStats = PlayerStats.getBaseStats();
                        tooltipToDisplay = new ArrayList<>();
                        tooltipToDisplay.add(statNamePretty);
                        int base = Math.round(baseStats.get(statName));
                        tooltipToDisplay.add(EnumChatFormatting.GRAY+"Base "+split.get(1)+": "+EnumChatFormatting.GREEN+base+" "+split.get(0));
                        int passive = Math.round(profile.getPassiveStats(profileId).get(statName)-baseStats.get(statName));
                        tooltipToDisplay.add(EnumChatFormatting.GRAY+"Passive "+split.get(1)+" Bonus: +"+EnumChatFormatting.YELLOW+passive+" "+split.get(0));
                        int itemBonus = Math.round(stats.get(statName)-profile.getPassiveStats(profileId).get(statName));
                        tooltipToDisplay.add(EnumChatFormatting.GRAY+"Item "+split.get(1)+" Bonus: +"+EnumChatFormatting.DARK_PURPLE+itemBonus+" "+split.get(0));
                        int finalStat = Math.round(stats.get(statName));
                        tooltipToDisplay.add(EnumChatFormatting.GRAY+"Final "+split.get(1)+": +"+EnumChatFormatting.RED+finalStat+" "+split.get(0));
                    }
                }
            }
        } else {
            Utils.drawStringCentered(EnumChatFormatting.RED+"Skill/Inv/Coll", Minecraft.getMinecraft().fontRendererObj,
                    guiLeft+172, guiTop+101-10, true, 0);
            Utils.drawStringCentered(EnumChatFormatting.RED+"APIs not", Minecraft.getMinecraft().fontRendererObj,
                    guiLeft+172, guiTop+101, true, 0);
            Utils.drawStringCentered(EnumChatFormatting.RED+"enabled!", Minecraft.getMinecraft().fontRendererObj,
                    guiLeft+172, guiTop+101+10, true, 0);
        }

        if(skillInfo != null) {
            int position = 0;
            for(Map.Entry<String, ItemStack> entry : ProfileViewer.getSkillToSkillDisplayMap().entrySet()) {
                if(entry.getValue() == null || entry.getKey() == null) {
                    position++;
                    continue;
                }

                int yPosition = position % 7;
                int xPosition = position / 7;

                String skillName = entry.getValue().getDisplayName();

                float level = Utils.getElementAsFloat(skillInfo.get("level_"+entry.getKey()), 0);
                int levelFloored = (int)Math.floor(level);

                int x = guiLeft+237+86*xPosition;
                int y = guiTop+31+21*yPosition;

                renderAlignedString(skillName, EnumChatFormatting.WHITE.toString()+levelFloored, x+14, y-4, 60);

                if(skillInfo.get("maxed_"+entry.getKey()).getAsBoolean()) {
                    renderGoldBar(x, y+6, 80);
                } else {
                    renderBar(x, y+6, 80, level%1);
                }

                if(mouseX > x && mouseX < x+80) {
                    if(mouseY > y-4 && mouseY < y+13) {
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

                GL11.glTranslatef((x), (y-6f), 0);
                GL11.glScalef(0.7f, 0.7f, 1);
                Utils.drawItemStackLinear(entry.getValue(), 0, 0);
                GL11.glScalef(1/0.7f, 1/0.7f, 1);
                GL11.glTranslatef(-(x), -(y-6f), 0);

                position++;
            }
        } else {
            Utils.drawStringCentered(EnumChatFormatting.RED+"Skills API not enabled!", Minecraft.getMinecraft().fontRendererObj,
                    guiLeft+322, guiTop+101, true, 0);
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
            width = Math.min(xSize*(completed-0.5f), xSize/2f);
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

    public void resetCache() {
        bestWeapons = null;
        bestRods = null;
        armorItems = null;
        inventoryItems = new HashMap<>();
        currentInventoryIndex = 0;
        arrowCount = -1;
        greenCandyCount = -1;
        purpleCandyCount = -1;
        entityPlayer = null;
        playerLocationSkin = null;
        playerLocationCape = null;
        skinType = null;
    }

    Shader blurShaderHorz = null;
    Framebuffer blurOutputHorz = null;
    Shader blurShaderVert = null;
    Framebuffer blurOutputVert = null;

    /**
     * Creates a projection matrix that projects from our coordinate space [0->width; 0->height] to OpenGL coordinate
     * space [-1 -> 1; 1 -> -1] (Note: flipped y-axis).
     *
     * This is so that we can render to and from the framebuffer in a way that is familiar to us, instead of needing to
     * apply scales and translations manually.
     */
    private Matrix4f createProjectionMatrix(int width, int height) {
        Matrix4f projMatrix  = new Matrix4f();
        projMatrix.setIdentity();
        projMatrix.m00 = 2.0F / (float)width;
        projMatrix.m11 = 2.0F / (float)(-height);
        projMatrix.m22 = -0.0020001999F;
        projMatrix.m33 = 1.0F;
        projMatrix.m03 = -1.0F;
        projMatrix.m13 = 1.0F;
        projMatrix.m23 = -1.0001999F;
        return projMatrix;
    }

    /**
     * Renders whatever is currently in the Minecraft framebuffer to our two framebuffers, applying a horizontal
     * and vertical blur separately in order to significantly save computation time.
     * This is only possible if framebuffers are supported by the system, so this method will exit prematurely
     * if framebuffers are not available. (Apple machines, for example, have poor framebuffer support).
     */
    private double lastBgBlurFactor = -1;
    private void blurBackground() {
        int width = Minecraft.getMinecraft().displayWidth;
        int height = Minecraft.getMinecraft().displayHeight;

        if(blurOutputHorz == null) {
            blurOutputHorz = new Framebuffer(width, height, false);
            blurOutputHorz.setFramebufferFilter(GL11.GL_NEAREST);
        }
        if(blurOutputVert == null) {
            blurOutputVert = new Framebuffer(width, height, false);
            blurOutputVert.setFramebufferFilter(GL11.GL_NEAREST);
        }
        if(blurOutputHorz.framebufferWidth != width || blurOutputHorz.framebufferHeight != height) {
            blurOutputHorz.createBindFramebuffer(width, height);
            blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }
        if(blurOutputVert.framebufferWidth != width || blurOutputVert.framebufferHeight != height) {
            blurOutputVert.createBindFramebuffer(width, height);
            blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }

        if(blurShaderHorz == null) {
            try {
                blurShaderHorz = new Shader(Minecraft.getMinecraft().getResourceManager(), "blur",
                        Minecraft.getMinecraft().getFramebuffer(), blurOutputHorz);
                blurShaderHorz.getShaderManager().getShaderUniform("BlurDir").set(1, 0);
                blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
            } catch(Exception e) { }
        }
        if(blurShaderVert == null) {
            try {
                blurShaderVert = new Shader(Minecraft.getMinecraft().getResourceManager(), "blur",
                        blurOutputHorz, blurOutputVert);
                blurShaderVert.getShaderManager().getShaderUniform("BlurDir").set(0, 1);
                blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
            } catch(Exception e) { }
        }
        if(blurShaderHorz != null && blurShaderVert != null) {
            if(15 != lastBgBlurFactor) {
                blurShaderHorz.getShaderManager().getShaderUniform("Radius").set((float)15);
                blurShaderVert.getShaderManager().getShaderUniform("Radius").set((float)15);
                lastBgBlurFactor = 15;
            }
            GL11.glPushMatrix();
            blurShaderHorz.loadShader(0);
            blurShaderVert.loadShader(0);
            GlStateManager.enableDepth();
            GL11.glPopMatrix();

            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }
    }

    /**
     * Renders a subsection of the blurred framebuffer on to the corresponding section of the screen.
     * Essentially, this method will "blur" the background inside the bounds specified by [x->x+blurWidth, y->y+blurHeight]
     */
    public void renderBlurredBackground(int width, int height, int x, int y, int blurWidth, int blurHeight) {
        float uMin = x/(float)width;
        float uMax = (x+blurWidth)/(float)width;
        float vMin = (height-y)/(float)height;
        float vMax = (height-y-blurHeight)/(float)height;

        blurOutputVert.bindFramebufferTexture();
        GlStateManager.color(1f, 1f, 1f, 1f);
        //Utils.setScreen(width*f, height*f, f);
        Utils.drawTexturedRect(x, y, blurWidth, blurHeight, uMin, uMax, vMin, vMax);
        //Utils.setScreen(width, height, f);
        blurOutputVert.unbindFramebufferTexture();
    }
}
