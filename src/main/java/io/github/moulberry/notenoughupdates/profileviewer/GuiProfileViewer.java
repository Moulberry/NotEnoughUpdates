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

    public enum ProfileViewerPage {
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
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        currentTime = System.currentTimeMillis();
        if(startTime == 0) startTime = currentTime;

        this.sizeX = 431;
        this.sizeY = 202;
        this.guiLeft = (this.width-this.sizeX)/2;
        this.guiTop = (this.height-this.sizeY)/2;

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawDefaultBackground();

        blurBackground();
        renderBlurredBackground(width, height, guiLeft, guiTop, sizeX, sizeY);

        GlStateManager.translate(0, 0, 5);
        renderTabs(true);
        GlStateManager.translate(0, 0, -3);

        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_bg);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);

        GlStateManager.translate(0, 0, -2);
        renderTabs(false);
        GlStateManager.translate(0, 0, 2);

        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);

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
        for(int i=0; i<ProfileViewerPage.values().length; i++) {
            ProfileViewerPage page = ProfileViewerPage.values()[i];
            boolean pressed = page == currentPage;
            if(pressed == renderPressed) {
                renderTab(page.stack, i, pressed);
            }
        }
    }

    private void renderTab(ItemStack stack, int xIndex, boolean pressed) {
        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);

        int x = guiLeft+xIndex*28;
        int y = guiTop-28;

        float uMin = 0;
        float uMax = 28/256f;
        float vMin = 20/256f;
        float vMax = 52/256f;
        if(pressed) {
            vMin = 52/256f;
            vMax = 84/256f;

            if(xIndex != 0) {
                uMin = 28/256f;
                uMax = 56/256f;
            }

            //if(!Keyboard.isKeyDown(Keyboard.KEY_A)) renderBlurredBackground(width, height, x, y, 28, 28);
        } else {
            //renderBlurredBackground(width, height, x, y+2, 28, 28);
        }

        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);

        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_elements);
        Utils.drawTexturedRect(x, y, 28, 32, uMin, uMax, vMin, vMax, GL11.GL_NEAREST);
        Utils.drawItemStack(stack, x+6, y+9);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for(int i=0; i<ProfileViewerPage.values().length; i++) {
            ProfileViewerPage page = ProfileViewerPage.values()[i];
            int x = guiLeft+i*28;
            int y = guiTop-28;

            if(mouseX > x && mouseX < x+28) {
                if(mouseY > y && mouseY < y+32) {
                    if(currentPage != page) Utils.playPressSound();
                    currentPage = page;
                    inventoryTextField.otherComponentClick();
                    return;
                }
            }
        }
        switch (currentPage) {
            case INVS:
                inventoryTextField.setSize(88, 20);
                if(mouseX > guiLeft+19 && mouseX < guiLeft+19+88) {
                    if(mouseY > guiTop+sizeY-26-20 && mouseY < guiTop+sizeY-26) {
                        inventoryTextField.mouseClicked(mouseX, mouseY, mouseButton);
                        return;
                    }
                }
        }
        inventoryTextField.otherComponentClick();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        switch (currentPage) {
            case INVS:
                keyTypedInvs(typedChar, keyCode);
                inventoryTextField.keyTyped(typedChar, keyCode);
                break;
            case COLS:
                keyTypedCols(typedChar, keyCode);
                break;
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

            JsonObject inventoryInfo = profile.getInventoryInfo(null);
            if(inventoryInfo == null) return;
            JsonObject collectionInfo = profile.getCollectionInfo(null);
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

    private void drawPetsPage(int mouseX, int mouseY, float partialTicks) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_pets);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);

        JsonObject petsInfo = profile.getPetsInfo(null);
        if(petsInfo == null) return;

        JsonArray pets = petsInfo.get("pets").getAsJsonArray();
        for(int i=0; i<pets.size(); i++) {
            JsonObject pet = pets.get(i).getAsJsonObject();

        }

        if(minions != null) {
            for (int i = 0; i < minions.size(); i++) {
                String minion = minions.get(i);
                if (minion != null) {
                    JsonObject minionJson = NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(minion + "_GENERATOR_1");
                    if (minionJson != null) {
                        int xIndex = i % COLLS_XCOUNT;
                        int yIndex = i / COLLS_XCOUNT;

                        float x = 231 + COLLS_XPADDING + (COLLS_XPADDING + 20) * xIndex;
                        float y = 7 + COLLS_YPADDING + (COLLS_YPADDING + 20) * yIndex;
                    }
                }
            }
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

        JsonObject collectionInfo = profile.getCollectionInfo(null);
        if(collectionInfo == null) return;
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

                items[yIndex][xIndex] = NotEnoughUpdates.INSTANCE.manager.jsonToStack(jsonInv.get(j).getAsJsonObject(), false);
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

    private void drawInvsPage(int mouseX, int mouseY, float partialTicks) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(pv_invs);
        Utils.drawTexturedRect(guiLeft, guiTop, sizeX, sizeY, GL11.GL_NEAREST);
        inventoryTextField.setSize(88, 20);

        JsonObject inventoryInfo = profile.getInventoryInfo(null);
        if(inventoryInfo == null) return;
        JsonObject collectionInfo = profile.getCollectionInfo(null);
        if(collectionInfo == null) return;

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

        inventoryTextField.render(guiLeft+19, guiTop+sizeY-26-20);

        int i=0;
        for(Map.Entry<String, ItemStack> entry : invNameToDisplayMap.entrySet()) {
            int xIndex = i%3;
            int yIndex = i/3;

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

            Utils.drawItemStackWithText(entry.getValue(), guiLeft+x, guiTop+y, ""+(i+1));

            if(mouseX >= guiLeft+x && mouseX <= guiLeft+x+16) {
                if(mouseY >= guiTop+y && mouseY <= guiTop+y+16) {
                    tooltipToDisplay = entry.getValue().getTooltip(Minecraft.getMinecraft().thePlayer, false);
                }
            }

            i++;
        }

        ItemStack[][][] inventories = getItemsForInventory(inventoryInfo, collectionInfo, selectedInventory);
        if(currentInventoryIndex >= inventories.length) currentInventoryIndex = inventories.length-1;
        if(currentInventoryIndex < 0) currentInventoryIndex = 0;

        ItemStack[][] inventory = inventories[currentInventoryIndex];
        if(inventory == null) return;

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
                        tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                    }
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

        Panorama.drawPanorama(-backgroundRotation-extraRotation, guiLeft+23, guiTop+44, 81, 108,
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
            byte b = 0;
            for(EnumPlayerModelParts part : EnumPlayerModelParts.values()) {
                b |= part.getPartMask();
            }
            entityPlayer.getDataWatcher().updateObject(10, b);
        }

        JsonObject profileInfo = profile.getProfileInformation(null);
        if(profileInfo == null) return;

        JsonObject skillInfo = profile.getSkillInfo(null);
        JsonObject inventoryInfo = profile.getInventoryInfo(null);
        JsonObject collectionInfo = profile.getCollectionInfo(null);

        if(backgroundClickedX != -1 && Mouse.isButtonDown(1)) {
            for(int i=0; i<entityPlayer.inventory.armorInventory.length; i++) {
                entityPlayer.inventory.armorInventory[i] = null;
            }
        } else {
            if(inventoryInfo != null && inventoryInfo.has("inv_armor")) {
                JsonArray items = inventoryInfo.get("inv_armor").getAsJsonArray();
                for(int i=0; i<entityPlayer.inventory.armorInventory.length; i++) {
                    JsonElement itemElement = items.get(i);
                    if(itemElement != null && itemElement.isJsonObject()) {
                        entityPlayer.inventory.armorInventory[i] = NotEnoughUpdates.INSTANCE.manager.jsonToStack(itemElement.getAsJsonObject(), false);
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
        JsonObject petsInfo = profile.getPetsInfo(null);
        if(petsInfo != null) {
            JsonElement activePetElement = petsInfo.get("active_pet");
            if(activePetElement != null && activePetElement.isJsonObject()) {
                JsonObject activePet = activePetElement.getAsJsonObject();

                String type = activePet.get("type").getAsString();

                JsonObject item = NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(type+";0");
                if(item != null) {
                    int x = guiLeft+50;
                    float y = guiTop+82+15*(float)Math.sin(((currentTime-startTime)/800f)%(2*Math.PI));
                    GlStateManager.translate(x, y, 0);
                    ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(item);
                    GlStateManager.scale(-1.5f, 1.5f, 1);
                    Utils.drawItemStack(stack, 0, 0);
                    GlStateManager.scale(-1/1.5f, 1/1.5f, 1);
                    GlStateManager.translate(-x, -y, 0);
                }
            }
        }
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
                renderAlignedString(statNamePretty, EnumChatFormatting.WHITE.toString()+val, guiLeft+132, guiTop+32+12.5f*i, 80);

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
        float vMin = y/(float)height;
        float vMax = (y+blurHeight)/(float)height;

        blurOutputVert.bindFramebufferTexture();
        GlStateManager.color(1f, 1f, 1f, 1f);
        //Utils.setScreen(width*f, height*f, f);
        Utils.drawTexturedRect(x, y, blurWidth, blurHeight, uMin, uMax, vMax, vMin);
        //Utils.setScreen(width, height, f);
        blurOutputVert.unbindFramebufferTexture();
    }
}
