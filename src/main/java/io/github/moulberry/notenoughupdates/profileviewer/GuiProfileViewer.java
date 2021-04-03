package io.github.moulberry.notenoughupdates.profileviewer;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.SBAIntegration;
import io.github.moulberry.notenoughupdates.cosmetics.ShaderManager;
import io.github.moulberry.notenoughupdates.itemeditor.GuiElementTextField;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Constants;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.*;
import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import java.awt.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiProfileViewer extends GuiScreen {

    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
    public static final ResourceLocation pv_basic = new ResourceLocation("notenoughupdates:pv_basic.png");
    public static final ResourceLocation pv_dung = new ResourceLocation("notenoughupdates:pv_dung.png");
    public static final ResourceLocation pv_extra = new ResourceLocation("notenoughupdates:pv_extra.png");
    public static final ResourceLocation pv_invs = new ResourceLocation("notenoughupdates:pv_invs.png");
    public static final ResourceLocation pv_cols = new ResourceLocation("notenoughupdates:pv_cols.png");
    public static final ResourceLocation pv_pets = new ResourceLocation("notenoughupdates:pv_pets.png");
    public static final ResourceLocation pv_dropdown = new ResourceLocation("notenoughupdates:pv_dropdown.png");
    public static final ResourceLocation pv_bg = new ResourceLocation("notenoughupdates:pv_bg.png");
    public static final ResourceLocation pv_elements = new ResourceLocation("notenoughupdates:pv_elements.png");
    public static final ResourceLocation resource_packs = new ResourceLocation("minecraft:textures/gui/resource_packs.png");
    public static final ResourceLocation icons = new ResourceLocation("textures/gui/icons.png");

    private static final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

    private final ProfileViewer.Profile profile;
    public static ProfileViewerPage currentPage = ProfileViewerPage.BASIC;
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
        NO_SKYBLOCK(null),
        BASIC(new ItemStack(Items.paper)),
        DUNG(new ItemStack(Item.getItemFromBlock(Blocks.deadbush))),
        EXTRA(new ItemStack(Items.book)),
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
        if(profile != null && profile.getHypixelProfile() != null) {
            name = profile.getHypixelProfile().get("displayname").getAsString();
        }
        playerNameTextField = new GuiElementTextField(name,
                GuiElementTextField.SCALE_TEXT);
        playerNameTextField.setSize(100, 20);

        if(currentPage == ProfileViewerPage.LOADING) {
            currentPage = ProfileViewerPage.BASIC;
        }
    }

    private GuiElementTextField playerNameTextField;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        currentTime = System.currentTimeMillis();
        if(startTime == 0) startTime = currentTime;

        ProfileViewerPage page = currentPage;
        if(profile == null) {
            page = ProfileViewerPage.INVALID_NAME;
        } else if(profile.getPlayerInformation(null) == null) {
            page = ProfileViewerPage.LOADING;
        } else if(profile.getLatestProfile() == null) {
            page = ProfileViewerPage.NO_SKYBLOCK;
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

        if(!(page == ProfileViewerPage.LOADING)) {
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

                    for(int yIndex = 0; yIndex<profile.getProfileIds().size(); yIndex++) {
                        String otherProfileId = profile.getProfileIds().get(yIndex);
                        Utils.drawStringCenteredScaledMaxWidth(otherProfileId, Minecraft.getMinecraft().fontRendererObj, guiLeft+50,
                                guiTop+sizeY+23+dropdownOptionSize/2f+dropdownOptionSize*yIndex, true, 90, new Color(33, 112, 104, 255).getRGB());
                    }

                }
            }
        }

        GlStateManager.color(1, 1, 1, 1);
        switch (page) {
            case BASIC:
                drawBasicPage(mouseX, mouseY, partialTicks);
                break;
            case DUNG:
                drawDungPage(mouseX, mouseY, partialTicks);
                break;
            case EXTRA:
                drawExtraPage(mouseX, mouseY, partialTicks);
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
                String str = EnumChatFormatting.YELLOW+"Loading player profiles.";
                long currentTimeMod = System.currentTimeMillis() % 1000;
                if(currentTimeMod > 333) {
                    if(currentTimeMod < 666) {
                        str += ".";
                    } else {
                        str += "..";
                    }
                }

                Utils.drawStringCentered(str, Minecraft.getMinecraft().fontRendererObj,
                        guiLeft+sizeX/2f, guiTop+101, true, 0);
                break;
            case INVALID_NAME:
                Utils.drawStringCentered(EnumChatFormatting.RED+"Invalid name or API is down!", Minecraft.getMinecraft().fontRendererObj,
                        guiLeft+sizeX/2f, guiTop+101, true, 0);
                break;
            case NO_SKYBLOCK:
                Utils.drawStringCentered(EnumChatFormatting.RED+"No skyblock data found!", Minecraft.getMinecraft().fontRendererObj,
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
            case DUNG:
                mouseClickedDung(mouseX, mouseY, mouseButton);
                break;
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
                    for(int index = 0; index<profile.getProfileIds().size(); index++) {
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
            case DUNG:
                keyTypedDung(typedChar, keyCode);
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

    protected void mouseClickedDung(int mouseX, int mouseY, int mouseButton) {
        if(mouseX >= guiLeft+50 && mouseX <= guiLeft+70 &&
                mouseY >= guiTop+54 && mouseY <= guiTop+64) {
            dungeonLevelTextField.mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            dungeonLevelTextField.otherComponentClick();
        }

        int cW = fontRendererObj.getStringWidth("Calculate");
        if(mouseX >= guiLeft+23+110-17-cW && mouseX <= guiLeft+23+110-17 &&
                mouseY >= guiTop+55 && mouseY <= guiTop+65) {
            calculateFloorLevelXP();
        }

        int y = guiTop+142;

        if(mouseY >= y-2 && mouseY <= y+9) {
            for(int i=1; i<=7; i++) {
                int w = fontRendererObj.getStringWidth(""+i);

                int x = guiLeft+23+110*i/8-w/2;

                if(mouseX >= x-2 && mouseX <= x+7) {
                    floorTime = i;
                    return;
                }
            }
        }
    }

    protected void keyTypedDung(char typedChar, int keyCode) {
        dungeonLevelTextField.keyTyped(typedChar, keyCode);
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
                if(sortedPets != null && petsPage < Math.ceil(sortedPets.size()/20f)-1) {
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

            ItemStack[][][] inventories = getItemsForInventory(inventoryInfo, selectedInventory);
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

    private static final ItemStack DEADBUSH = new ItemStack(Item.getItemFromBlock(Blocks.deadbush));
    private static final ItemStack[] BOSS_HEADS = new ItemStack[7];

    private HashMap<String, ProfileViewer.Level> levelObjCatas = new HashMap<>();
    private HashMap<String, HashMap<String, ProfileViewer.Level>> levelObjClasseses = new HashMap<>();

    private GuiElementTextField dungeonLevelTextField = new GuiElementTextField("", GuiElementTextField.SCALE_TEXT);

    private static final String[] dungSkillsName = {"Healer", "Mage", "Berserk", "Archer", "Tank"};
    private static final ItemStack[] dungSkillsStack = { new ItemStack(Items.potionitem, 1, 16389),
            new ItemStack(Items.blaze_rod), new ItemStack(Items.iron_sword), new ItemStack(Items.bow), new ItemStack(Items.leather_chestplate)};
    private static final String[] bossFloorArr = {"Bonzo", "Scarf", "Professor", "Thorn", "Livid", "Sadan", "Necron"};
    private static final String[] bossFloorHeads = {
            "12716ecbf5b8da00b05f316ec6af61e8bd02805b21eb8e440151468dc656549c",
            "7de7bbbdf22bfe17980d4e20687e386f11d59ee1db6f8b4762391b79a5ac532d",
            "9971cee8b833a62fc2a612f3503437fdf93cad692d216b8cf90bbb0538c47dd8",
            "8b6a72138d69fbbd2fea3fa251cabd87152e4f1c97e5f986bf685571db3cc0",
            "c1007c5b7114abec734206d4fc613da4f3a0e99f71ff949cedadc99079135a0b",
            "fa06cb0c471c1c9bc169af270cd466ea701946776056e472ecdaeb49f0f4a4dc",
            "a435164c05cea299a3f016bbbed05706ebb720dac912ce4351c2296626aecd9a"
    };
    private static int floorTime = 7;
    private int floorLevelTo = -1;
    private int floorLevelToXP = -1;

    private void calculateFloorLevelXP() {
        JsonObject leveling = Constants.LEVELING;
        if(leveling == null)  return;
        ProfileViewer.Level levelObjCata = levelObjCatas.get(profileId);
        if(levelObjCata == null) return;

        try {
            dungeonLevelTextField.setCustomBorderColour(0xffffffff);
            floorLevelTo = Integer.parseInt(dungeonLevelTextField.getText());

            JsonArray levelingArray = Utils.getElement(leveling, "catacombs").getAsJsonArray();

            float remaining = -((levelObjCata.level % 1) * levelObjCata.maxXpForLevel);

            for(int level=0; level<Math.min(floorLevelTo, levelingArray.size()); level++) {
                if(level < Math.floor(levelObjCata.level)) {
                    continue;
                }
                remaining += levelingArray.get(level).getAsFloat();
            }

            if(remaining < 0) {
                remaining = 0;
            }
            floorLevelToXP = (int) remaining;
        } catch(Exception e) {
            dungeonLevelTextField.setCustomBorderColour(0xffff0000);
        }
    }

    private void drawDungPage(int mouseX, int mouseY, float partialTicks) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_dung);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);

        JsonObject hypixelInfo = profile.getHypixelProfile();
        if(hypixelInfo == null) return;
        JsonObject profileInfo = profile.getProfileInformation(profileId);
        if(profileInfo == null) return;

        JsonObject leveling = Constants.LEVELING;
        if(leveling == null)  return;

        int sectionWidth = 110;

        ProfileViewer.Level levelObjCata = levelObjCatas.get(profileId);
        //Catacombs level thingy
        {
            if(levelObjCata == null) {
                float cataXp = Utils.getElementAsFloat(Utils.getElement(profileInfo,
                        "dungeons.dungeon_types.catacombs.experience"), 0);
                levelObjCata = ProfileViewer.getLevel(Utils.getElement(leveling, "catacombs").getAsJsonArray(),
                        cataXp, 50, false);
                levelObjCatas.put(profileId, levelObjCata);
            }

            String skillName = EnumChatFormatting.RED+"Catacombs";
            float level = levelObjCata.level;
            int levelFloored = (int)Math.floor(level);

            if(floorLevelTo == -1 && levelFloored >= 0) {
                dungeonLevelTextField.setText(""+(levelFloored+1));
                calculateFloorLevelXP();
            }

            int x = guiLeft+23;
            int y = guiTop+25;

            renderXpBar(skillName, DEADBUSH, x, y, sectionWidth, levelObjCata, mouseX, mouseY);

            Utils.renderAlignedString(EnumChatFormatting.YELLOW+"Until Cata "+floorLevelTo+": ",
                    EnumChatFormatting.WHITE.toString()+shortNumberFormat(floorLevelToXP, 0), x, y+16, sectionWidth);

            if(mouseX > x && mouseX < x + sectionWidth &&
                    mouseY > y+16 && mouseY < y+24) {
                float xpF5 = 2000;
                float xpF6 = 4000;
                float xpF7 = 20000;
                if(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                    xpF5 *= 1.1;
                    xpF6 *= 1.1;
                    xpF7 *= 1.1;
                }

                long runsF5 = (int)Math.ceil(floorLevelToXP/xpF5);
                long runsF6 = (int)Math.ceil(floorLevelToXP/xpF6);
                long runsF7 = (int)Math.ceil(floorLevelToXP/xpF7);

                float timeF5 = Utils.getElementAsFloat(Utils.getElement(profileInfo,
                        "dungeons.dungeon_types.catacombs.fastest_time_s_plus.5"), 0);
                float timeF6 = Utils.getElementAsFloat(Utils.getElement(profileInfo,
                        "dungeons.dungeon_types.catacombs.fastest_time_s_plus.6"), 0);
                float timeF7 = Utils.getElementAsFloat(Utils.getElement(profileInfo,
                        "dungeons.dungeon_types.catacombs.fastest_time_s_plus.7"), 0);

                tooltipToDisplay = Lists.newArrayList(
              String.format("# F5 Runs (%s xp) : %d", shortNumberFormat(xpF5, 0), runsF5),
                        String.format("# F6 Runs (%s xp) : %d", shortNumberFormat(xpF6, 0), runsF6),
                        String.format("# F7 Runs (%s xp) : %d", shortNumberFormat(xpF7, 0), runsF7),
                        ""
                );
                boolean hasTime = false;
                if(timeF5 > 1000) {
                    tooltipToDisplay.add(String.format("Expected Time (F5) : %s", Utils.prettyTime(runsF5*(long)(timeF5*1.2))));
                    hasTime = true;
                }
                if(timeF6 > 1000) {
                    tooltipToDisplay.add(String.format("Expected Time (F6) : %s", Utils.prettyTime(runsF6*(long)(timeF6*1.2))));
                    hasTime = true;
                }
                if(timeF7 > 1000) {
                    tooltipToDisplay.add(String.format("Expected Time (F7) : %s", Utils.prettyTime(runsF7*(long)(timeF7*1.2))));
                    hasTime = true;
                }
                if(hasTime) {
                    tooltipToDisplay.add("");
                }
                if(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                    tooltipToDisplay.add("[Hold "+EnumChatFormatting.YELLOW+"SHIFT"+EnumChatFormatting.GRAY+" to show without Expert Ring]");
                }
                if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                    if(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) tooltipToDisplay.add("");
                    tooltipToDisplay.add("Number of runs is calculated as [Remaining XP]/[XP per Run].");
                    tooltipToDisplay.add("The [XP per Run] is the average xp gained from an S+ run");
                    tooltipToDisplay.add("The "+EnumChatFormatting.DARK_PURPLE+"Catacombs Expert Ring"+EnumChatFormatting.GRAY+
                            " is assumed to be used, unless "+EnumChatFormatting.YELLOW+"SHIFT"+EnumChatFormatting.GRAY+" is held.");
                    tooltipToDisplay.add("[Time per run] is calculated using fastestSPlus x 120%");
                } else {
                    tooltipToDisplay.add("[Hold "+EnumChatFormatting.YELLOW+"CTRL"+EnumChatFormatting.GRAY+" to see details]");
                }
            }

            dungeonLevelTextField.setSize(20, 10);
            dungeonLevelTextField.render(x+22, y+29);
            int calcLen = fontRendererObj.getStringWidth("Calculate");
            Utils.renderShadowedString(EnumChatFormatting.WHITE+"Calculate", x+sectionWidth-17-calcLen/2f,
                    y+30, 100);

            //Random stats

            float secrets = Utils.getElementAsFloat(Utils.getElement(hypixelInfo,
                    "achievements.skyblock_treasure_hunter"), 0);

            float totalRuns = 0;
            float totalRunsF5 = 0;
            for(int i=1; i<=7; i++) {
                float runs = Utils.getElementAsFloat(Utils.getElement(profileInfo,
                        "dungeons.dungeon_types.catacombs.tier_completions."+i), 0);
                totalRuns += runs;
                if(i >= 5) {
                    totalRunsF5 += runs;
                }
            }

            float mobKills = 0;
            float mobKillsF5 = 0;
            for(int i=1; i<=7; i++) {
                float kills = Utils.getElementAsFloat(Utils.getElement(profileInfo,
                        "dungeons.dungeon_types.catacombs.mobs_killed."+i), 0);
                mobKills += kills;
                if(i >= 5) {
                    mobKillsF5 += kills;
                }
            }

            int miscTopY = y+55;

            Utils.renderAlignedString(EnumChatFormatting.YELLOW+"Total Runs  ",
                    EnumChatFormatting.WHITE.toString()+((int)(totalRuns)), x, miscTopY, sectionWidth);
            Utils.renderAlignedString(EnumChatFormatting.YELLOW+"Total Runs (F5-7)  ",
                    EnumChatFormatting.WHITE.toString()+((int)(totalRunsF5)), x, miscTopY+10, sectionWidth);
            Utils.renderAlignedString(EnumChatFormatting.YELLOW+"Secrets (Total)  ",
                    EnumChatFormatting.WHITE.toString()+shortNumberFormat(secrets, 0), x, miscTopY+20, sectionWidth);
            Utils.renderAlignedString(EnumChatFormatting.YELLOW+"Secrets (/Run)  ",
                    EnumChatFormatting.WHITE.toString()+(Math.round(secrets/Math.max(1, totalRuns)*100)/100f), x, miscTopY+30, sectionWidth);
            Utils.renderAlignedString(EnumChatFormatting.YELLOW+"Mob Kills (Total)  ",
                    EnumChatFormatting.WHITE.toString()+shortNumberFormat(mobKills, 0), x, miscTopY+40, sectionWidth);

            int y3 = y+117;

            for(int i=1; i<=7; i++) {
                int w = fontRendererObj.getStringWidth(""+i);

                int bx = x+sectionWidth*i/8-w/2;

                boolean invert = i == floorTime;
                float uMin = 20/256f;
                float uMax = 29/256f;
                float vMin = 0/256f;
                float vMax = 11/256f;

                GlStateManager.color(1, 1, 1, 1);
                Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
                Utils.drawTexturedRect(bx-2, y3-2, 9, 11,
                        invert ? uMax : uMin, invert ? uMin : uMax,
                        invert ? vMax : vMin, invert ? vMin : vMax, GL11.GL_NEAREST);

                Utils.renderShadowedString(EnumChatFormatting.WHITE.toString()+i, bx+w/2, y3, 10);
            }

            float timeNorm = Utils.getElementAsFloat(Utils.getElement(profileInfo,
                    "dungeons.dungeon_types.catacombs.fastest_time."+floorTime), 0);
            float timeS = Utils.getElementAsFloat(Utils.getElement(profileInfo,
                    "dungeons.dungeon_types.catacombs.fastest_time_s."+floorTime), 0);
            float timeSPLUS = Utils.getElementAsFloat(Utils.getElement(profileInfo,
                    "dungeons.dungeon_types.catacombs.fastest_time_s_plus."+floorTime), 0);
            String timeNormStr = timeNorm <= 0 ? "N/A" : Utils.prettyTime((long)timeNorm);
            String timeSStr = timeS <= 0 ? "N/A" : Utils.prettyTime((long)timeS);
            String timeSPlusStr = timeSPLUS <= 0 ? "N/A" : Utils.prettyTime((long)timeSPLUS);
            Utils.renderAlignedString(EnumChatFormatting.YELLOW+"Floor "+floorTime+" ",
                    EnumChatFormatting.WHITE.toString()+timeNormStr, x, y3+10, sectionWidth);
            Utils.renderAlignedString(EnumChatFormatting.YELLOW+"Floor "+floorTime+" S",
                    EnumChatFormatting.WHITE.toString()+timeSStr, x, y3+20, sectionWidth);
            Utils.renderAlignedString(EnumChatFormatting.YELLOW+"Floor "+floorTime+" S+",
                    EnumChatFormatting.WHITE.toString()+timeSPlusStr, x, y3+30, sectionWidth);
        }

        //Completions
        {
            int x = guiLeft+161;
            int y = guiTop+27;

            Utils.renderShadowedString(EnumChatFormatting.RED+"Boss Collections",
                    x+sectionWidth/2, y, sectionWidth);
            for(int i=1; i<=7; i++) {
                float compl = Utils.getElementAsFloat(Utils.getElement(profileInfo,
                        "dungeons.dungeon_types.catacombs.tier_completions."+i), 0);

                if(BOSS_HEADS[i-1] == null) {
                    String textureLink = bossFloorHeads[i-1];

                    String b64Decoded = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + textureLink + "\"}}}";
                    String b64Encoded = new String(Base64.getEncoder().encode(b64Decoded.getBytes()));

                    ItemStack stack = new ItemStack(Items.skull, 1, 3);
                    NBTTagCompound nbt = new NBTTagCompound();
                    NBTTagCompound skullOwner = new NBTTagCompound();
                    NBTTagCompound properties = new NBTTagCompound();
                    NBTTagList textures = new NBTTagList();
                    NBTTagCompound textures_0 = new NBTTagCompound();

                    String uuid = UUID.nameUUIDFromBytes(b64Encoded.getBytes()).toString();
                    skullOwner.setString("Id", uuid);
                    skullOwner.setString("Name", uuid);

                    textures_0.setString("Value", b64Encoded);
                    textures.appendTag(textures_0);

                    properties.setTag("textures", textures);
                    skullOwner.setTag("Properties", properties);
                    nbt.setTag("SkullOwner", skullOwner);
                    stack.setTagCompound(nbt);

                    BOSS_HEADS[i-1] = stack;
                }

                GlStateManager.pushMatrix();
                GlStateManager.translate(x-4, y+10+20*(i-1), 0);
                GlStateManager.scale(1.3f, 1.3f, 1);
                Utils.drawItemStack(BOSS_HEADS[i-1], 0, 0);
                GlStateManager.popMatrix();

                Utils.renderAlignedString(String.format(EnumChatFormatting.YELLOW+"%s (F%d) ", bossFloorArr[i-1], i),
                        EnumChatFormatting.WHITE.toString()+(int)compl,
                        x+16, y+18+20*(i-1), sectionWidth-15);

            }
        }

        //Skills
        {
            int x = guiLeft+298;
            int y = guiTop+27;

            //Gui.drawRect(x, y, x+120, y+147, 0xffffffff);

            Utils.renderShadowedString(EnumChatFormatting.DARK_PURPLE+"Class Levels",
                    x+sectionWidth/2, y, sectionWidth);

            JsonElement activeClassElement = Utils.getElement(profileInfo, "dungeons.selected_dungeon_class");
            String activeClass = null;
            if(activeClassElement instanceof JsonPrimitive && ((JsonPrimitive) activeClassElement).isString()) {
                activeClass = activeClassElement.getAsString();
            }

            for(int i=0; i<dungSkillsName.length; i++) {
                String skillName = dungSkillsName[i];


                HashMap<String, ProfileViewer.Level> levelObjClasses = levelObjClasseses.computeIfAbsent(profileId, k->new HashMap<>());
                if(!levelObjClasses.containsKey(skillName)) {
                    float cataXp = Utils.getElementAsFloat(Utils.getElement(profileInfo,
                           "dungeons.player_classes."+skillName.toLowerCase()+".experience"), 0);
                    ProfileViewer.Level levelObj = ProfileViewer.getLevel(Utils.getElement(leveling, "catacombs").getAsJsonArray(),
                           cataXp, 50, false);
                    levelObjClasses.put(skillName, levelObj);
                }

                String colour = EnumChatFormatting.WHITE.toString();
                if(skillName.toLowerCase().equals(activeClass)) {
                    colour = EnumChatFormatting.GREEN.toString();
                }

                ProfileViewer.Level levelObj = levelObjClasses.get(skillName);

                renderXpBar(colour+skillName, dungSkillsStack[i], x, y+20+29*i, sectionWidth, levelObj, mouseX, mouseY);
            }
        }
    }

    private void renderXpBar(String skillName, ItemStack stack, int x, int y, int xSize, ProfileViewer.Level levelObj, int mouseX, int mouseY) {
        float level = levelObj.level;
        int levelFloored = (int)Math.floor(level);

        Utils.renderAlignedString(skillName, EnumChatFormatting.WHITE.toString()+levelFloored, x+14, y-4, xSize-20);

        if(levelObj.maxed) {
            renderGoldBar(x, y+6, xSize);
        } else {
            renderBar(x, y+6, xSize, level%1);
        }

        if(mouseX > x && mouseX < x+120) {
            if(mouseY > y-4 && mouseY < y+13) {
                String levelStr;
                if(levelObj.maxed) {
                    levelStr = EnumChatFormatting.GOLD+"MAXED!";
                } else {
                    int maxXp = (int)levelObj.maxXpForLevel;
                    levelStr = EnumChatFormatting.DARK_PURPLE.toString() + shortNumberFormat(Math.round((level%1)*maxXp),
                            0) + "/" + shortNumberFormat(maxXp, 0);
                }

                tooltipToDisplay = Utils.createList(levelStr);
            }
        }

        GL11.glTranslatef((x), (y-6f), 0);
        GL11.glScalef(0.7f, 0.7f, 1);
        Utils.drawItemStackLinear(stack, 0, 0);
        GL11.glScalef(1/0.7f, 1/0.7f, 1);
        GL11.glTranslatef(-(x), -(y-6f), 0);
    }

    public static class PetLevel {
        public float level;
        public float currentLevelRequirement;
        public float maxXP;
        public float levelPercentage;
        public float levelXp;
        public float totalXp;
    }

    public static PetLevel getPetLevel(JsonArray levels, int offset, float exp) {
        float xpTotal = 0;
        float level = 1;
        float currentLevelRequirement = 0;
        float currentLevelProgress = 0;

        boolean addLevel = true;

        for(int i=offset; i<offset+99; i++) {
            if(addLevel) {
                currentLevelRequirement = levels.get(i).getAsFloat();
                xpTotal += currentLevelRequirement;
                if(xpTotal > exp) {
                    currentLevelProgress = (exp-(xpTotal-currentLevelRequirement));
                    addLevel = false;
                } else {
                    level += 1;
                }
            } else {
                xpTotal += levels.get(i).getAsFloat();
            }
        }

        level += currentLevelProgress/currentLevelRequirement;
        if(level <= 0) {
            level = 1;
        } else if(level > 100) {
            level = 100;
        }
        PetLevel levelObj = new PetLevel();
        levelObj.level = level;
        levelObj.currentLevelRequirement = currentLevelRequirement;
        levelObj.maxXP = xpTotal;
        levelObj.levelPercentage = currentLevelProgress/currentLevelRequirement;
        levelObj.levelXp = currentLevelProgress;
        levelObj.totalXp = exp;
        return levelObj;
    }

    public static final HashMap<String, HashMap<String, Float>> PET_STAT_BOOSTS = new HashMap<>();
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
    public static final HashMap<String, HashMap<String, Float>> PET_STAT_BOOSTS_MULT = new HashMap<>();
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
    public static HashMap<String, String> MINION_RARITY_TO_NUM = new HashMap<>();
    static {
        MINION_RARITY_TO_NUM.put("COMMON", "0");
        MINION_RARITY_TO_NUM.put("UNCOMMON", "1");
        MINION_RARITY_TO_NUM.put("RARE", "2");
        MINION_RARITY_TO_NUM.put("EPIC", "3");
        MINION_RARITY_TO_NUM.put("LEGENDARY", "4");
    }
    private void drawPetsPage(int mouseX, int mouseY, float partialTicks) {
        JsonObject petsInfo = profile.getPetsInfo(profileId);
        if(petsInfo == null) return;
        JsonObject petsJson = Constants.PETS;
        if(petsJson == null) return;

        String location = null;
        JsonObject status = profile.getPlayerStatus();
        if(status != null && status.has("mode")) {
            location = status.get("mode").getAsString();
        }

        backgroundRotation += (currentTime - lastTime)/400f;
        backgroundRotation %= 360;

        String panoramaIdentifier = "day";
        if(SBInfo.getInstance().currentTimeDate != null) {
            if(SBInfo.getInstance().currentTimeDate.getHours() <= 6 ||
                    SBInfo.getInstance().currentTimeDate.getHours() >= 20) {
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
                String tierNum1 = MINION_RARITY_TO_NUM.get(tier1);
                int tierNum1I = Integer.parseInt(tierNum1);
                float exp1 = pet1.get("exp").getAsFloat();

                String tier2 = pet2.get("tier").getAsString();
                String tierNum2 = MINION_RARITY_TO_NUM.get(tier2);
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
                String tierNum = MINION_RARITY_TO_NUM.get(tier);
                float exp = pet.get("exp").getAsFloat();
                if(tierNum == null) continue;

                if(pet.has("heldItem") && !pet.get("heldItem").isJsonNull() && pet.get("heldItem").getAsString().equals("PET_ITEM_TIER_BOOST")) {
                    tierNum = ""+(Integer.parseInt(tierNum)+1);
                }

                int petRarityOffset = petsJson.get("pet_rarity_offset").getAsJsonObject().get(tier).getAsInt();
                JsonArray levelsArr = petsJson.get("pet_levels").getAsJsonArray();

                PetLevel levelObj = getPetLevel(levelsArr, petRarityOffset, exp);
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
        if(petsPage < Math.ceil(pets.size()/20f)-1) {
            Utils.drawTexturedRect( guiLeft+100+15, guiTop+6, 12, 16,
                    5/256f, 29/256f, !rightHovered?0:32/256f, !rightHovered?32/256f:64/256f, GL11.GL_NEAREST);
        }

        for(int i=petsPage*20; i<Math.min(petsPage*20+20, Math.min(sortedPetsStack.size(), sortedPets.size())); i++) {
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

                    GlStateManager.pushMatrix();
                    GlStateManager.translate(x, y, 0);

                    drawRect(-halfDisplayLen-1-28, -1, halfDisplayLen+1-28, 8, new Color(0, 0, 0, 100).getRGB());

                    Minecraft.getMinecraft().fontRendererObj.drawString(display, -halfDisplayLen-28, 0, 0, true);

                    ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(item);
                    GlStateManager.enableDepth();
                    GlStateManager.translate(-55, 0, 0);
                    GlStateManager.scale(3.5f, 3.5f, 1);
                    Utils.drawItemStack(stack, 0, 0);
                    GlStateManager.popMatrix();
                    break;
                }
            }

            float level = pet.get("level").getAsFloat();
            float currentLevelRequirement = pet.get("currentLevelRequirement").getAsFloat();
            float exp = pet.get("exp").getAsFloat();
            float maxXP = pet.get("maxXP").getAsFloat();

            String[] split = display.split("] ");
            String colouredName = split[split.length-1];

            Utils.renderAlignedString(colouredName, EnumChatFormatting.WHITE+"Level "+(int)Math.floor(level), guiLeft+319, guiTop+28, 98);

            //Utils.drawStringCenteredScaledMaxWidth(, Minecraft.getMinecraft().fontRendererObj, guiLeft+368, guiTop+28+4, true, 98, 0);
            //renderAlignedString(display, EnumChatFormatting.YELLOW+"[LVL "+Math.floor(level)+"]", guiLeft+319, guiTop+28, 98);
            renderBar(guiLeft+319, guiTop+38, 98, (float)Math.floor(level)/100f);

            Utils.renderAlignedString(EnumChatFormatting.YELLOW+"To Next LVL", EnumChatFormatting.WHITE.toString()+(int)(level%1*100)+"%", guiLeft+319, guiTop+46, 98);
            renderBar(guiLeft+319, guiTop+56, 98, level%1);

            Utils.renderAlignedString(EnumChatFormatting.YELLOW+"To Max LVL", EnumChatFormatting.WHITE.toString()+Math.min(100, (int)(exp/maxXP*100))+"%", guiLeft+319, guiTop+64, 98);
            renderBar(guiLeft+319, guiTop+74, 98, exp/maxXP);

            Utils.renderAlignedString(EnumChatFormatting.YELLOW+"Total XP", EnumChatFormatting.WHITE.toString()+shortNumberFormat(exp, 0), guiLeft+319, guiTop+125, 98);
            Utils.renderAlignedString(EnumChatFormatting.YELLOW+"Current LVL XP",
                        EnumChatFormatting.WHITE.toString()+shortNumberFormat((level%1)*currentLevelRequirement, 0), guiLeft+319, guiTop+143, 98);
            Utils.renderAlignedString(EnumChatFormatting.YELLOW+"Required LVL XP", EnumChatFormatting.WHITE.toString()+shortNumberFormat(currentLevelRequirement, 0), guiLeft+319, guiTop+161, 98);
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

    private boolean useActualMax(String invName) {
        switch(invName) {
            case "talisman_bag":
            case "fishing_bag":
            case "potion_bag":
                return true;
        }
        return false;
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
        JsonObject misc = Constants.MISC;
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
    public ItemStack[][][] getItemsForInventory(JsonObject inventoryInfo, String invName) {
        if(inventoryItems.containsKey(invName)) return inventoryItems.get(invName);

        JsonArray jsonInv = Utils.getElement(inventoryInfo, invName).getAsJsonArray();

        if(jsonInv.size() == 0) return new ItemStack[1][][];

        int jsonInvSize;
        if(useActualMax(invName)) {
            jsonInvSize = (int)Math.ceil(jsonInv.size()/9f)*9;
        } else {
            jsonInvSize = 9*4;
            for(int i=9*4; i<jsonInv.size(); i++) {
                JsonElement item = jsonInv.get(i);
                if(item != null && item.isJsonObject()) {
                    jsonInvSize = (i/9)*9;
                }
            }
        }

        int rowSize = 9;
        int rows = jsonInvSize/rowSize;
        int maxRowsPerPage = getRowsForInventory(invName);
        int maxInvSize = rowSize*maxRowsPerPage;

        int numInventories = (jsonInvSize-1)/maxInvSize+1;

        ItemStack[][][] inventories = new ItemStack[numInventories][][];

        //int availableSlots = getAvailableSlotsForInventory(inventoryInfo, collectionInfo, invName);

        for(int i=0; i<numInventories; i++) {
            int thisRows = Math.min(maxRowsPerPage, rows-maxRowsPerPage*i);
            if(thisRows <= 0) break;

            ItemStack[][] items = new ItemStack[thisRows][rowSize];

            int invSize = Math.min(jsonInvSize, maxInvSize+maxInvSize*i);
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

                if(j >= jsonInv.size()) {
                    items[yIndex][xIndex] = fillerStack;
                    continue;
                }
                if(jsonInv.get(j) == null || !jsonInv.get(j).isJsonObject()) {
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
                            tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer,
                                    Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
                        }
                    }
                }
            }
        }

        ItemStack[][][] inventories = getItemsForInventory(inventoryInfo, selectedInventory);
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

    private TreeMap<Integer, Set<String>> topKills = null;
    private TreeMap<Integer, Set<String>> topDeaths = null;

    private void drawExtraPage(int mouseX, int mouseY, float partialTicks) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_extra);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);

        JsonObject profileInfo = profile.getProfileInformation(profileId);
        if(profileInfo == null) return;
        JsonObject skillInfo = profile.getSkillInfo(profileId);

        float xStart = 22;
        float xOffset = 103;
        float yStartTop = 27;
        float yStartBottom = 109;
        float yOffset = 10;

        float bankBalance = Utils.getElementAsFloat(Utils.getElement(profileInfo, "banking.balance"), 0);
        float purseBalance = Utils.getElementAsFloat(Utils.getElement(profileInfo, "coin_purse"), 0);

        Utils.renderAlignedString(EnumChatFormatting.GOLD+"Bank Balance", EnumChatFormatting.WHITE.toString()+shortNumberFormat(bankBalance, 0),
                guiLeft+xStart, guiTop+yStartTop, 76);
        Utils.renderAlignedString(EnumChatFormatting.GOLD+"Purse", EnumChatFormatting.WHITE.toString()+shortNumberFormat(purseBalance, 0),
                guiLeft+xStart, guiTop+yStartTop+yOffset, 76);


        float fairySouls = Utils.getElementAsFloat(Utils.getElement(profileInfo, "fairy_souls_collected"), 0);
        Utils.renderAlignedString(EnumChatFormatting.LIGHT_PURPLE+"Fairy Souls", EnumChatFormatting.WHITE.toString()+(int)fairySouls+"/220",
                guiLeft+xStart, guiTop+yStartBottom, 76);
        if(skillInfo != null) {
            float totalSkillLVL = 0;
            float totalTrueSkillLVL = 0;
            float totalSlayerLVL = 0;
            float totalSkillCount = 0;
            float totalSlayerCount = 0;

            for(Map.Entry<String, JsonElement> entry : skillInfo.entrySet()) {
                if(entry.getKey().startsWith("level_skill")) {
                    if(entry.getKey().contains("runecrafting")) continue;
                    if(entry.getKey().contains("carpentry")) continue;
                    if(entry.getKey().contains("catacombs")) continue;

                    totalSkillLVL += entry.getValue().getAsFloat();
                    totalTrueSkillLVL += Math.floor(entry.getValue().getAsFloat());
                    totalSkillCount++;
                } else if(entry.getKey().startsWith("level_slayer")) {
                    totalSlayerLVL += entry.getValue().getAsFloat();
                    totalSlayerCount++;
                }
            }

            float avgSkillLVL = totalSkillLVL/totalSkillCount;
            float avgTrueSkillLVL = totalTrueSkillLVL/totalSkillCount;
            float avgSlayerLVL = totalSlayerLVL/totalSlayerCount;

            Utils.renderAlignedString(EnumChatFormatting.RED+"AVG Skill Level", EnumChatFormatting.WHITE.toString()+Math.floor(avgSkillLVL*10)/10,
                    guiLeft+xStart, guiTop+yStartBottom+yOffset, 76);
            Utils.renderAlignedString(EnumChatFormatting.RED+"AVG Slayer Level", EnumChatFormatting.WHITE.toString()+Math.floor(avgSlayerLVL*10)/10,
                    guiLeft+xStart, guiTop+yStartBottom+yOffset*2, 76);
            Utils.renderAlignedString(EnumChatFormatting.RED+"True AVG Skill Level", EnumChatFormatting.WHITE.toString()+Math.floor(avgTrueSkillLVL*10)/10,
                    guiLeft+xStart, guiTop+yStartBottom+yOffset*3, 76);
        }


        float auctions_bids = Utils.getElementAsFloat(Utils.getElement(profileInfo, "stats.auctions_bids"), 0);
        float auctions_highest_bid = Utils.getElementAsFloat(Utils.getElement(profileInfo, "stats.auctions_highest_bid"), 0);
        float auctions_won = Utils.getElementAsFloat(Utils.getElement(profileInfo, "stats.auctions_won"), 0);
        float auctions_created = Utils.getElementAsFloat(Utils.getElement(profileInfo, "stats.auctions_created"), 0);
        float auctions_gold_spent = Utils.getElementAsFloat(Utils.getElement(profileInfo, "stats.auctions_gold_spent"), 0);
        float auctions_gold_earned = Utils.getElementAsFloat(Utils.getElement(profileInfo, "stats.auctions_gold_earned"), 0);

        Utils.renderAlignedString(EnumChatFormatting.DARK_PURPLE+"Auction Bids", EnumChatFormatting.WHITE.toString()+(int)auctions_bids,
                guiLeft+xStart+xOffset, guiTop+yStartTop, 76);
        Utils.renderAlignedString(EnumChatFormatting.DARK_PURPLE+"Highest Bid", EnumChatFormatting.WHITE.toString()+shortNumberFormat(auctions_highest_bid, 0),
                guiLeft+xStart+xOffset, guiTop+yStartTop+yOffset, 76);
        Utils.renderAlignedString(EnumChatFormatting.DARK_PURPLE+"Auctions Won", EnumChatFormatting.WHITE.toString()+(int)auctions_won,
                guiLeft+xStart+xOffset, guiTop+yStartTop+yOffset*2, 76);
        Utils.renderAlignedString(EnumChatFormatting.DARK_PURPLE+"Auctions Created", EnumChatFormatting.WHITE.toString()+(int)auctions_created,
                guiLeft+xStart+xOffset, guiTop+yStartTop+yOffset*3, 76);
        Utils.renderAlignedString(EnumChatFormatting.DARK_PURPLE+"Gold Spent", EnumChatFormatting.WHITE.toString()+shortNumberFormat(auctions_gold_spent, 0),
                guiLeft+xStart+xOffset, guiTop+yStartTop+yOffset*4, 76);
        Utils.renderAlignedString(EnumChatFormatting.DARK_PURPLE+"Gold Earned", EnumChatFormatting.WHITE.toString()+shortNumberFormat(auctions_gold_earned, 0),
                guiLeft+xStart+xOffset, guiTop+yStartTop+yOffset*5, 76);


        float zombie_boss_kills_tier_2 = Utils.getElementAsFloat(Utils.getElement(profileInfo, "slayer_bosses.zombie.boss_kills_tier_2"), 0);
        float zombie_boss_kills_tier_3 = Utils.getElementAsFloat(Utils.getElement(profileInfo, "slayer_bosses.zombie.boss_kills_tier_3"), 0);
        float spider_boss_kills_tier_2 = Utils.getElementAsFloat(Utils.getElement(profileInfo, "slayer_bosses.spider.boss_kills_tier_2"), 0);
        float spider_boss_kills_tier_3 = Utils.getElementAsFloat(Utils.getElement(profileInfo, "slayer_bosses.spider.boss_kills_tier_3"), 0);
        float wolf_boss_kills_tier_2 = Utils.getElementAsFloat(Utils.getElement(profileInfo, "slayer_bosses.wolf.boss_kills_tier_2"), 0);
        float wolf_boss_kills_tier_3 = Utils.getElementAsFloat(Utils.getElement(profileInfo, "slayer_bosses.wolf.boss_kills_tier_3"), 0);

        Utils.renderAlignedString(EnumChatFormatting.DARK_AQUA+"Revenant T3", EnumChatFormatting.WHITE.toString()+(int)zombie_boss_kills_tier_2,
                guiLeft+xStart+xOffset, guiTop+yStartBottom, 76);
        Utils.renderAlignedString(EnumChatFormatting.DARK_AQUA+"Revenant T4", EnumChatFormatting.WHITE.toString()+(int)zombie_boss_kills_tier_3,
                guiLeft+xStart+xOffset, guiTop+yStartBottom+yOffset, 76);
        Utils.renderAlignedString(EnumChatFormatting.DARK_AQUA+"Sven T3", EnumChatFormatting.WHITE.toString()+(int)wolf_boss_kills_tier_2,
                guiLeft+xStart+xOffset, guiTop+yStartBottom+yOffset*2, 76);
        Utils.renderAlignedString(EnumChatFormatting.DARK_AQUA+"Sven T4", EnumChatFormatting.WHITE.toString()+(int)wolf_boss_kills_tier_3,
                guiLeft+xStart+xOffset, guiTop+yStartBottom+yOffset*3, 76);
        Utils.renderAlignedString(EnumChatFormatting.DARK_AQUA+"Tarantula T3", EnumChatFormatting.WHITE.toString()+(int)spider_boss_kills_tier_2,
                guiLeft+xStart+xOffset, guiTop+yStartBottom+yOffset*4, 76);
        Utils.renderAlignedString(EnumChatFormatting.DARK_AQUA+"Tarantula T4", EnumChatFormatting.WHITE.toString()+(int)spider_boss_kills_tier_3,
                guiLeft+xStart+xOffset, guiTop+yStartBottom+yOffset*5, 76);

        float pet_milestone_ores_mined = Utils.getElementAsFloat(Utils.getElement(profileInfo, "stats.pet_milestone_ores_mined"), 0);
        float pet_milestone_sea_creatures_killed = Utils.getElementAsFloat(Utils.getElement(profileInfo, "stats.pet_milestone_sea_creatures_killed"), 0);

        float items_fished = Utils.getElementAsFloat(Utils.getElement(profileInfo, "stats.items_fished"), 0);
        float items_fished_treasure = Utils.getElementAsFloat(Utils.getElement(profileInfo, "stats.items_fished_treasure"), 0);
        float items_fished_large_treasure = Utils.getElementAsFloat(Utils.getElement(profileInfo, "stats.items_fished_large_treasure"), 0);

        Utils.renderAlignedString(EnumChatFormatting.GREEN+"Ores Mined", EnumChatFormatting.WHITE.toString()+(int)pet_milestone_ores_mined,
                guiLeft+xStart+xOffset*2, guiTop+yStartTop, 76);
        Utils.renderAlignedString(EnumChatFormatting.GREEN+"Sea Creatures Killed", EnumChatFormatting.WHITE.toString()+(int)pet_milestone_sea_creatures_killed,
                guiLeft+xStart+xOffset*2, guiTop+yStartTop+yOffset, 76);

        Utils.renderAlignedString(EnumChatFormatting.GREEN+"Items Fished", EnumChatFormatting.WHITE.toString()+(int)items_fished,
                guiLeft+xStart+xOffset*2, guiTop+yStartTop+yOffset*3, 76);
        Utils.renderAlignedString(EnumChatFormatting.GREEN+"Treasures Fished", EnumChatFormatting.WHITE.toString()+(int)items_fished_treasure,
                guiLeft+xStart+xOffset*2, guiTop+yStartTop+yOffset*4, 76);
        Utils.renderAlignedString(EnumChatFormatting.GREEN+"Large Treasures", EnumChatFormatting.WHITE.toString()+(int)items_fished_large_treasure,
                guiLeft+xStart+xOffset*2, guiTop+yStartTop+yOffset*5, 76);

        if(topKills == null) {
            topKills = new TreeMap<>();
            JsonObject stats = profileInfo.get("stats").getAsJsonObject();
            for(Map.Entry<String, JsonElement> entry : stats.entrySet()) {
                if(entry.getKey().startsWith("kills_")) {
                    if(entry.getValue().isJsonPrimitive()) {
                        JsonPrimitive prim = (JsonPrimitive) entry.getValue();
                        if(prim.isNumber()) {
                            String name = WordUtils.capitalizeFully(entry.getKey().substring("kills_".length()).replace("_", " "));
                            Set<String> kills = topKills.computeIfAbsent(prim.getAsInt(), k->new HashSet<>());
                            kills.add(name);
                        }
                    }
                }
            }
        }
        if(topDeaths == null) {
            topDeaths = new TreeMap<>();
            JsonObject stats = profileInfo.get("stats").getAsJsonObject();
            for(Map.Entry<String, JsonElement> entry : stats.entrySet()) {
                if(entry.getKey().startsWith("deaths_")) {
                    if(entry.getValue().isJsonPrimitive()) {
                        JsonPrimitive prim = (JsonPrimitive) entry.getValue();
                        if(prim.isNumber()) {
                            String name = WordUtils.capitalizeFully(entry.getKey().substring("deaths_".length()).replace("_", " "));
                            Set<String> deaths = topDeaths.computeIfAbsent(prim.getAsInt(), k->new HashSet<>());
                            deaths.add(name);
                        }
                    }
                }
            }
        }

        int index = 0;
        for(int killCount : topKills.descendingKeySet()) {
            if(index >= 6) break;
            Set<String> kills = topKills.get(killCount);
            for(String killType : kills) {
                if(index >= 6) break;
                Utils.renderAlignedString(EnumChatFormatting.YELLOW+killType+" Kills", EnumChatFormatting.WHITE.toString()+killCount,
                        guiLeft+xStart+xOffset*3, guiTop+yStartTop+yOffset*index, 76);
                index++;
            }
        }
        index = 0;
        for(int deathCount : topDeaths.descendingKeySet()) {
            if(index >= 6) break;
            Set<String> deaths = topDeaths.get(deathCount);
            for(String deathType : deaths) {
                if(index >= 6) break;
                Utils.renderAlignedString(EnumChatFormatting.YELLOW+"Deaths: "+ deathType, EnumChatFormatting.WHITE.toString()+deathCount,
                        guiLeft+xStart+xOffset*3, guiTop+yStartBottom+yOffset*index, 76);
                index++;
            }
        }
    }

    private int backgroundClickedX = -1;

    private static char[] c = new char[]{'k', 'm', 'b', 't'};

    public static String shortNumberFormat(double n, int iteration) {
        if(n < 1000) {
            if(n % 1 == 0) {
                return Integer.toString((int)n);
            } else {
                return String.format("%.2f", n);
            }
        }

        double d = ((long) n / 100) / 10.0;
        boolean isRound = (d * 10) %10 == 0;
        return (d < 1000?
                ((d > 99.9 || isRound || (!isRound && d > 9.99)?
                        (int) d * 10 / 10 : d + ""
                ) + "" + c[iteration])
                : shortNumberFormat(d, iteration+1));
    }

    private boolean loadingProfile = false;
    private static final ExecutorService profileLoader = Executors.newFixedThreadPool(1);

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
        if(SBInfo.getInstance().currentTimeDate != null) {
            if(SBInfo.getInstance().currentTimeDate.getHours() <= 6 ||
                SBInfo.getInstance().currentTimeDate.getHours() >= 20) {
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
                String rank = Utils.getElementAsString(profile.getHypixelProfile().get("rank"),
                        Utils.getElementAsString(profile.getHypixelProfile().get("newPackageRank"), "NONE"));;
                String monthlyPackageRank = Utils.getElementAsString(profile.getHypixelProfile().get("monthlyPackageRank"), "NONE");
                if(!rank.equals("YOUTUBER") && !monthlyPackageRank.equals("NONE")) {
                    rank = monthlyPackageRank;
                }
                EnumChatFormatting rankPlusColorECF = EnumChatFormatting.getValueByName(Utils.getElementAsString(profile.getHypixelProfile().get("rankPlusColor"),
                        "GOLD"));
                String rankPlusColor = EnumChatFormatting.GOLD.toString();
                if(rankPlusColorECF != null) {
                    rankPlusColor = rankPlusColorECF.toString();
                }

                JsonObject misc = Constants.MISC;
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

        long networth = profile.getNetWorth(profileId);
        if(networth > 0) {
            Utils.drawStringCentered(EnumChatFormatting.GREEN+"Net Worth: "+EnumChatFormatting.GOLD+numberFormat.format(networth), fr, guiLeft+63, guiTop+38, true, 0);
            try {
                double networthInCookies = (networth / NotEnoughUpdates.INSTANCE.manager.auctionManager.getBazaarInfo("BOOSTER_COOKIE").get("avg_buy").getAsDouble());
                String networthIRLMoney = Long.toString(Math.round(((networthInCookies * 325) / 675) * 4.99));

                if(mouseX > guiLeft+8 && mouseX < guiLeft+8+fontRendererObj.getStringWidth("Net Worth: " + numberFormat.format(networth))) {
                    if(mouseY > guiTop+32 && mouseY < guiTop+32+fontRendererObj.FONT_HEIGHT) {
                        tooltipToDisplay = new ArrayList<>();
                        tooltipToDisplay.add(EnumChatFormatting.GREEN+"Net worth in IRL money: "+EnumChatFormatting.DARK_GREEN+"$" +EnumChatFormatting.GOLD+networthIRLMoney);
                        tooltipToDisplay.add("");
                        if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                            tooltipToDisplay.add(EnumChatFormatting.RED+"This is calculated using the current");
                            tooltipToDisplay.add(EnumChatFormatting.RED+"price of booster cookies on bazaar and the price");
                            tooltipToDisplay.add(EnumChatFormatting.RED+"for cookies using gems, then the price of gems");
                            tooltipToDisplay.add(EnumChatFormatting.RED+"is where we get the amount of IRL money you" );
                            tooltipToDisplay.add(EnumChatFormatting.RED+"theoretically have on skyblock in net worth.");
                        } else {
                            tooltipToDisplay.add(EnumChatFormatting.GRAY+"[SHIFT for Info]");
                        }
                        tooltipToDisplay.add("");
                        tooltipToDisplay.add(EnumChatFormatting.RED+"THIS IS IN NO WAY ENDORSING IRL TRADING!");

                    }
                }
            } catch(Exception e){}
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
                    JsonObject misc = Constants.MISC;
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
            if(!loadingProfile || ((ThreadPoolExecutor)profileLoader).getActiveCount() == 0) {
                loadingProfile = true;
                UUID playerUUID = UUID.fromString(niceUuid(profile.getUuid()));

                profileLoader.submit(() -> {
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
                });
            }
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

        if(entityPlayer != null) {
            if(backgroundClickedX != -1 && Mouse.isButtonDown(1)) {
                Arrays.fill(entityPlayer.inventory.armorInventory, null);
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
                } else {
                    Arrays.fill(entityPlayer.inventory.armorInventory, null);
                }
            }
            if(entityPlayer.getUniqueID().toString().equals("ae6193ab-494a-4719-b6e7-d50392c8f012")) {
                entityPlayer.inventory.armorInventory[3] = NotEnoughUpdates.INSTANCE.manager.jsonToStack(
                        NotEnoughUpdates.INSTANCE.manager.getItemInformation().get("SMALL_BACKPACK"));
            }
        }

        if(entityPlayer != null && playerLocationSkin == null) {
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
                        ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(item, false);

                        //Remove extra attributes so no CIT
                        NBTTagCompound stackTag = stack.getTagCompound()==null?new NBTTagCompound():stack.getTagCompound();
                        stackTag.removeTag("ExtraAttributes");
                        stack.setTagCompound(stackTag);

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
        if(entityPlayer != null) {
            drawEntityOnScreen(guiLeft+63, guiTop+128+7, 36, guiLeft+63-mouseX, guiTop+129-mouseY, entityPlayer);
        }

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
                Utils.renderAlignedString(statNamePretty, EnumChatFormatting.WHITE.toString()+val, guiLeft+132, guiTop+27+11f*i, 80);

                if(mouseX > guiLeft+132 && mouseX < guiLeft+212) {
                    if(mouseY > guiTop+27+11f*i && mouseY < guiTop+37+11f*i) {
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

                Utils.renderAlignedString(skillName, EnumChatFormatting.WHITE.toString()+levelFloored, x+14, y-4, 60);

                if(skillInfo.get("maxed_"+entry.getKey()).getAsBoolean()) {
                    renderGoldBar(x, y+6, 80);
                } else {
                    renderBar(x, y+6, 80, level%1);
                }

                if(mouseX > x && mouseX < x+80) {
                    if(mouseY > y-4 && mouseY < y+13) {
                        tooltipToDisplay = new ArrayList<>();
                        tooltipToDisplay.add(skillName);
                        if(skillInfo.get("maxed_"+entry.getKey()).getAsBoolean()) {
                            tooltipToDisplay.add(EnumChatFormatting.GRAY+"Progress: "+EnumChatFormatting.GOLD+"MAXED!");
                        } else {
                            int maxXp = (int)skillInfo.get("maxxp_"+entry.getKey()).getAsFloat();
                            tooltipToDisplay.add(EnumChatFormatting.GRAY+"Progress: "+EnumChatFormatting.DARK_PURPLE.toString() +
                                    shortNumberFormat(Math.round((level%1)*maxXp), 0) + "/" + shortNumberFormat(maxXp, 0));
                        }
                        String totalXpS = NumberFormat.getIntegerInstance().format((int)skillInfo.get("experience_"+entry.getKey()).getAsFloat());
                        tooltipToDisplay.add(EnumChatFormatting.GRAY+"Total XP: " +
                                EnumChatFormatting.DARK_PURPLE+totalXpS);
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
        if(!OpenGlHelper.areShadersSupported()) {
            renderBar(x, y, xSize, 1);
            return;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(icons);
        ShaderManager shaderManager = ShaderManager.getInstance();
        shaderManager.loadShader("make_gold");
        shaderManager.loadData("make_gold", "amount", (startTime-System.currentTimeMillis())/10000f);

        Utils.drawTexturedRect(x, y, xSize/2f, 5, 0/256f, (xSize/2f)/256f, 79/256f, 84/256f, GL11.GL_NEAREST);
        Utils.drawTexturedRect(x+xSize/2f, y, xSize/2f, 5, (182-xSize/2f)/256f, 182/256f, 79/256f, 84/256f, GL11.GL_NEAREST);

        GL20.glUseProgram(0);
    }

    private void renderBar(float x, float y, float xSize, float completed) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(icons);

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
        petsPage = 0;
        sortedPets = null;
        sortedPetsStack = null;
        selectedPet = -1;
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
        if(!OpenGlHelper.isFramebufferEnabled()) return;

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
        if(!OpenGlHelper.isFramebufferEnabled()) return;

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
