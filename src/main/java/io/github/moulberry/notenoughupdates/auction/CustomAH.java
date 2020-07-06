package io.github.moulberry.notenoughupdates.auction;

import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.util.TexLoc;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.awt.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

import static io.github.moulberry.notenoughupdates.GuiTextures.*;

public class CustomAH extends Gui {

    private static final ResourceLocation creativeTabSearch =
            new ResourceLocation("textures/gui/container/creative_inventory/tab_item_search.png");
    private static final ResourceLocation creativeInventoryTabs =
            new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

    private List<String> auctionIds = new ArrayList<>();

    private boolean scrollClicked = false;

    private int startingBid = 0;

    private int clickedMainCategory = -1;
    private int clickedSubCategory = -1;

    private GuiTextField searchField = null;
    private GuiTextField priceField = null;

    private boolean renderOverAuctionView = false;
    private long lastRenderDisable = 0;

    private int eventButton;
    private long lastMouseEvent;
    public long lastOpen;
    public long lastGuiScreenSwitch;

    private int splits = 2;

    private int ySplit = 35;
    private int ySplitSize = 18;

    private float scrollAmount;

    private int guiLeft = 0;
    private int guiTop = 0;

    private Category CATEGORY_SWORD = new Category("sword", "Swords", "diamond_sword");
    private Category CATEGORY_ARMOR = new Category("armor", "Armor", "diamond_chestplate");
    private Category CATEGORY_BOWS = new Category("bow", "Bows", "bow");
    private Category CATEGORY_ACCESSORIES = new Category("accessories", "Accessories", "diamond");

    private Category CATEGORY_FISHING_ROD = new Category("fishingrod", "Fishing Rods", "fishing_rod");
    private Category CATEGORY_PICKAXE = new Category("pickaxe", "Pickaxes", "iron_pickaxe");
    private Category CATEGORY_AXE = new Category("axe", "Axes", "iron_axe");
    private Category CATEGORY_SHOVEL = new Category("shovel", "Shovels", "iron_shovel");

    private Category CATEGORY_PET_ITEM = new Category("petitem", "Pet Items", "lead");

    private Category CATEGORY_EBOOKS = new Category("ebook", "Enchanted Books", "enchanted_book");
    private Category CATEGORY_POTIONS = new Category("potion", "Potions", "potion");
    private Category CATEGORY_TRAVEL_SCROLLS = new Category("travelscroll", "Travel Scrolls", "map");

    private Category CATEGORY_REFORGE_STONES = new Category("reforgestone", "Reforge Stones", "anvil");
    private Category CATEGORY_RUNES = new Category("rune", "Runes", "end_portal_frame");
    private Category CATEGORY_FURNITURE = new Category("furniture", "Furniture", "armor_stand");

    private Category CATEGORY_COMBAT = new Category("weapon", "Combat", "golden_sword", CATEGORY_SWORD,
            CATEGORY_BOWS, CATEGORY_ARMOR, CATEGORY_ACCESSORIES);
    private Category CATEGORY_TOOL = new Category("", "Tools", "iron_pickaxe", CATEGORY_FISHING_ROD, CATEGORY_PICKAXE,
            CATEGORY_AXE, CATEGORY_SHOVEL);
    private Category CATEGORY_PET = new Category("pet", "Pets", "bone", CATEGORY_PET_ITEM);
    private Category CATEGORY_CONSUMABLES = new Category("consumables", "Consumables", "apple", CATEGORY_EBOOKS, CATEGORY_POTIONS,
            CATEGORY_TRAVEL_SCROLLS);
    private Category CATEGORY_BLOCKS = new Category("blocks", "Blocks", "cobblestone");
    private Category CATEGORY_MISC = new Category("misc", "Misc", "stick", CATEGORY_REFORGE_STONES, CATEGORY_RUNES,
            CATEGORY_FURNITURE);

    private Category[] mainCategories = new Category[]{CATEGORY_COMBAT, CATEGORY_TOOL, CATEGORY_PET,
            CATEGORY_CONSUMABLES, CATEGORY_BLOCKS, CATEGORY_MISC};

    private static final int SORT_MODE_HIGH = 0;
    private static final int SORT_MODE_LOW = 1;
    private static final int SORT_MODE_SOON = 2;

    private static final String[] rarities = { "COMMON", "UNCOMMON", "RARE", "EPIC",
            "LEGENDARY", "MYTHIC", "SPECIAL", "VERY SPECIAL" };

    private static final int BIN_FILTER_ALL = 0;
    private static final int BIN_FILTER_BIN = 1;
    private static final int BIN_FILTER_AUC = 2;

    private int sortMode = SORT_MODE_HIGH;
    private int rarityFilter = -1;
    private boolean filterMyAuctions = false;
    private int binFilter = BIN_FILTER_ALL;

    private static ItemStack CONTROL_SORT = Utils.createItemStack(Item.getItemFromBlock(Blocks.hopper),
            EnumChatFormatting.GREEN+"Sort");
    private static ItemStack CONTROL_TIER = Utils.createItemStack(Items.ender_eye,
            EnumChatFormatting.GREEN+"Item Tier");
    private static ItemStack CONTROL_MYAUC = Utils.createItemStack(Items.gold_ingot,
            EnumChatFormatting.GREEN+"My Auctions");
    private static ItemStack CONTROL_BIN = Utils.createItemStack(Item.getItemFromBlock(Blocks.golden_rail),
            EnumChatFormatting.GREEN+"BIN Filter");
    private ItemStack[] controls = {null,CONTROL_SORT,CONTROL_TIER,null,CONTROL_MYAUC,null,CONTROL_BIN,null,null};

    private NEUManager manager;

    public CustomAH(NEUManager manager) {
        this.manager = manager;
    }

    public void clearSearch() {
        if(System.currentTimeMillis() - lastOpen < 1000) Mouse.setGrabbed(false);

        sortMode = SORT_MODE_HIGH;
        rarityFilter = -1;
        filterMyAuctions = false;
        binFilter = BIN_FILTER_ALL;

        searchField.setText("");
        priceField.setText("");
    }

    public class Category {
        public String categoryMatch;
        public Category[] subcategories;
        public String displayName;
        public ItemStack displayItem;

        public Category(String categoryMatch, String displayName, String displayItem, Category... subcategories) {
            this.categoryMatch = categoryMatch;
            this.subcategories = subcategories;
            this.displayName = displayName;
            this.displayItem = new ItemStack(Item.getByNameOrId(displayItem));
        }

        public String[] getTotalCategories() {
            String[] categories = new String[1+subcategories.length];
            categories[0] = categoryMatch;

            for(int i=0; i<subcategories.length; i++) {
                categories[i+1] = subcategories[i].categoryMatch;
            }
            return categories;
        }
    }

    private void init() {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        this.searchField = new GuiTextField(0, fr, this.guiLeft + 82, this.guiTop + 6,
                89, fr.FONT_HEIGHT);
        this.priceField = new GuiTextField(1, fr, this.guiLeft + 82, this.guiTop + 6,
                89, fr.FONT_HEIGHT);

        this.searchField.setMaxStringLength(15);
        this.searchField.setEnableBackgroundDrawing(false);
        this.searchField.setTextColor(16777215);
        this.searchField.setVisible(true);
        this.searchField.setCanLoseFocus(false);
        this.searchField.setFocused(true);
        this.searchField.setText("");

        this.priceField.setMaxStringLength(10);
        this.priceField.setEnableBackgroundDrawing(false);
        this.priceField.setTextColor(16777215);
        this.priceField.setVisible(true);
        this.priceField.setCanLoseFocus(false);
        this.priceField.setFocused(false);
        this.priceField.setText("");
    }

    public boolean isRenderOverAuctionView() {
        return renderOverAuctionView || (System.currentTimeMillis() - lastRenderDisable) < 500;
    }

    public void setRenderOverAuctionView(boolean renderOverAuctionView) {
        if(this.renderOverAuctionView && !renderOverAuctionView) lastRenderDisable = System.currentTimeMillis();
        this.renderOverAuctionView = renderOverAuctionView;
    }

    private int getXSize() {
        return 195;
    }

    private int getYSize() {
        return 136 + ySplitSize*splits;
    }

    private TexLoc tl = new TexLoc(0, 0, Keyboard.KEY_M);

    public List<String> getTooltipForAucId(String aucId) {
        AuctionManager.Auction auc = manager.auctionManager.getAuctionItems().get(aucId);

        List<String> tooltip = new ArrayList<>();

        for(String line : auc.getStack().getTooltip(Minecraft.getMinecraft().thePlayer, false)) {
            tooltip.add(EnumChatFormatting.GRAY+line);
        }

        long timeUntilEnd = auc.end - System.currentTimeMillis();

        long seconds = timeUntilEnd / 1000 % 60;
        long minutes = (timeUntilEnd / 1000 / 60) % 60;
        long hours = (timeUntilEnd / 1000 / 60 / 60) % 24;
        long days = (timeUntilEnd / 1000 / 60 / 60 / 24);

        String endsIn = EnumChatFormatting.YELLOW+"";
        if(timeUntilEnd < 0) {
            endsIn += "Ended!";
        } else if(minutes == 0 && hours == 0 && days == 0) {
            endsIn += seconds + "s";
        } else if(hours==0 && days==0) {
            endsIn += minutes + "m" + seconds + "s";
        } else if(days==0) {
            if(hours <= 6) {
                endsIn += hours + "h" + minutes + "m" + seconds + "s";
            } else {
                endsIn += hours + "h";
            }
        } else {
            endsIn += days + "d" + hours + "h";
        }

        NumberFormat format = NumberFormat.getInstance(Locale.US);

        tooltip.add(EnumChatFormatting.DARK_GRAY+""+EnumChatFormatting.STRIKETHROUGH+"-----------------");
        tooltip.add(EnumChatFormatting.GRAY+"Seller: [CLICK TO SEE]");

        if(auc.bin) {
            tooltip.add(EnumChatFormatting.GRAY+"Buy it now: "+
                    EnumChatFormatting.GOLD+format.format(auc.starting_bid));
        } else {
            if(auc.bid_count > 0) {
                tooltip.add(EnumChatFormatting.GRAY+"Bids: "+EnumChatFormatting.GREEN+auc.bid_count+" bids");
                tooltip.add("");
                tooltip.add(EnumChatFormatting.GRAY+"Top bid: "+
                        EnumChatFormatting.GOLD+format.format(auc.highest_bid_amount));
                tooltip.add(EnumChatFormatting.GRAY+"Bidder: [CLICK TO SEE]");
            } else {
                tooltip.add(EnumChatFormatting.GRAY+"Starting bid: "+
                        EnumChatFormatting.GOLD+format.format(auc.starting_bid));
            }
        }

        tooltip.add("");
        tooltip.add(EnumChatFormatting.GRAY+"Ends in: "+endsIn);
        tooltip.add("");
        tooltip.add(EnumChatFormatting.YELLOW+"Click to inspect!");

        return tooltip;
    }

    public boolean isEditingPrice() {
        return Minecraft.getMinecraft().currentScreen instanceof GuiEditSign;
    }

    private boolean isGuiFiller(ItemStack stack) {
        return stack == null || !stack.hasTagCompound() || !stack.getTagCompound().hasKey("AttributeModifiers");
    }

    private void drawCategorySide(int i) {
        boolean clicked = i == clickedSubCategory;

        int x = guiLeft-28;
        int y = guiTop+17+28*i;
        float uMin = 28/256f;
        float uMax = 56/256f;
        float vMin = 0+(clicked?32/256f:0);
        float vMax = 32/256f+(clicked?32/256f:0);
        float catWidth = 32;
        float catHeight = 28;

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer
                .pos(x, y+catHeight, 0.0D)
                .tex(uMax, vMin).endVertex();
        worldrenderer
                .pos(x+catWidth, y+catHeight, 0.0D)
                .tex(uMax, vMax).endVertex();
        worldrenderer
                .pos(x+catWidth, y, 0.0D)
                .tex(uMin, vMax).endVertex();
        worldrenderer
                .pos(x, y, 0.0D)
                .tex(uMin, vMin).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
    }

    public void drawScreen(int mouseX, int mouseY) {
        if(System.currentTimeMillis() - lastOpen < 1000) Mouse.setGrabbed(false);

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();

        //Dark background
        drawGradientRect(0, 0, width, height, -1072689136, -804253680);

        if(searchField == null || priceField == null) init();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        tl.handleKeyboardInput();

        guiLeft = (width - getXSize())/2;
        guiTop = (height - getYSize())/2;
        this.searchField.xPosition = guiLeft + 82;
        this.searchField.yPosition = guiTop + 6;

        this.searchField.setFocused(true);
        this.priceField.setFocused(false);

        if(!isEditingPrice()) priceField.setText("IAUSHDIUAH");

        List<String> tooltipToRender = null;
        if(Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
            GuiChest auctionView = (GuiChest) Minecraft.getMinecraft().currentScreen;

            float slideAmount = 1-Math.max(0, Math.min(1, (System.currentTimeMillis() - lastGuiScreenSwitch)/200f));
            int auctionViewLeft = guiLeft+getXSize()+4 - (int)(slideAmount*(78+4));

            Minecraft.getMinecraft().getTextureManager().bindTexture(auction_view);
            this.drawTexturedModalRect(auctionViewLeft, guiTop, 0, 0, 78, 172);

            if(auctionViewLeft+31 > guiLeft+getXSize()) {
                try {
                    ItemStack topStack = auctionView.inventorySlots.getSlot(13).getStack();
                    ItemStack leftStack = auctionView.inventorySlots.getSlot(29).getStack();
                    ItemStack middleStack = auctionView.inventorySlots.getSlot(31).getStack();
                    ItemStack rightStack = auctionView.inventorySlots.getSlot(33).getStack();

                    boolean isBin = isGuiFiller(leftStack) || isGuiFiller(leftStack);

                    if(isBin) {
                        leftStack = middleStack;
                        middleStack = null;
                    }
                    Utils.drawItemStack(leftStack, auctionViewLeft+31, guiTop+100);

                    if(!isGuiFiller(leftStack)) {
                        NBTTagCompound tag = leftStack.getTagCompound();
                        NBTTagCompound display = tag.getCompoundTag("display");
                        if (display.hasKey("Lore", 9)) {
                            NBTTagList list = display.getTagList("Lore", 8);
                            String line2 = list.getStringTagAt(1);
                            line2 = Utils.cleanColour(line2);
                            StringBuilder priceNumbers = new StringBuilder();
                            for(int i=0; i<line2.length(); i++) {
                                char c = line2.charAt(i);
                                if((int)c >= 48 && (int)c <= 57) {
                                    priceNumbers.append(c);
                                }
                            }
                            if(priceNumbers.length() > 0) {
                                startingBid = Integer.parseInt(priceNumbers.toString());
                            }
                        }
                    }

                    Utils.drawItemStack(topStack, auctionViewLeft+31, guiTop+35);

                    if(!isGuiFiller(middleStack)) {
                        Minecraft.getMinecraft().getTextureManager().bindTexture(auction_view_buttons);
                        boolean hover = mouseX > auctionViewLeft+31 && mouseX <auctionViewLeft+31+16 &&
                                mouseY > guiTop+126 && mouseY < guiTop+126+16;
                        this.drawTexturedModalRect(auctionViewLeft+31, guiTop+126, hover?16:0, 0, 16, 16);
                    } else {
                        middleStack = null;
                    }

                    if(mouseX > auctionViewLeft+31 && mouseX <auctionViewLeft+31+16) {
                        if(mouseY > guiTop+35 && mouseY < guiTop+35+16) {
                            if(topStack != null) tooltipToRender = topStack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                        } else if(mouseY > guiTop+100 && mouseY < guiTop+100+16) {
                            if(leftStack != null) tooltipToRender = leftStack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                        } else if(mouseY > guiTop+61 && mouseY < guiTop+61+16) {
                            if(rightStack != null) tooltipToRender = rightStack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                        } else if(mouseY > guiTop+126 && mouseY < guiTop+126+16) {
                            if(middleStack != null) tooltipToRender = middleStack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                        }
                    }
                } catch(NullPointerException e) { //i cant be bothered
                }
            }
        } else if(isEditingPrice()) {
            float slideAmount = 1-Math.max(0, Math.min(1, (System.currentTimeMillis() - lastGuiScreenSwitch)/200f));
            int auctionViewLeft = guiLeft+getXSize()+4 - (int)(slideAmount*(96+4));

            if(priceField.getText().equals("IAUSHDIUAH")) priceField.setText(""+startingBid);

            searchField.setFocused(false);
            priceField.setFocused(true);
            priceField.xPosition = auctionViewLeft+18;
            priceField.yPosition = guiTop+18;

            Minecraft.getMinecraft().getTextureManager().bindTexture(auction_price);
            this.drawTexturedModalRect(auctionViewLeft, guiTop, 0, 0, 96, 99);
            priceField.drawTextBox();

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Minecraft.getMinecraft().getTextureManager().bindTexture(auction_view_buttons);
            this.drawTexturedModalRect(auctionViewLeft+16, guiTop+32, 0, 16, 30, 16);
            this.drawTexturedModalRect(auctionViewLeft+16+34, guiTop+32, 0, 16, 30, 16);
            this.drawTexturedModalRect(auctionViewLeft+16, guiTop+50, 0, 16, 30, 16);
            this.drawTexturedModalRect(auctionViewLeft+16+34, guiTop+50, 0, 16, 30, 16);
            this.drawTexturedModalRect(auctionViewLeft+16, guiTop+68, 0, 32, 64, 16);

            FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
            Utils.drawStringCentered("x2", fr, auctionViewLeft+16+15, guiTop+32+8, false,
                    Color.BLACK.getRGB());
            Utils.drawStringCentered("+50%", fr, auctionViewLeft+16+34+15, guiTop+32+8, false,
                    Color.BLACK.getRGB());
            Utils.drawStringCentered("+25%", fr, auctionViewLeft+16+15, guiTop+50+8, false,
                    Color.BLACK.getRGB());
            Utils.drawStringCentered("+10%", fr, auctionViewLeft+16+34+15, guiTop+50+8, false,
                    Color.BLACK.getRGB());
            Utils.drawStringCentered("Set Amount", fr, auctionViewLeft+16+32, guiTop+68+8, false,
                    Color.BLACK.getRGB());
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            GuiEditSign editSign = (GuiEditSign) Minecraft.getMinecraft().currentScreen;
            TileEntitySign tes = (TileEntitySign)Utils.getField(GuiEditSign.class, editSign,
                    "tileSign", "field_146848_f");
            tes.lineBeingEdited = 0;
            tes.signText[0] = new ChatComponentText(priceField.getText());
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(creativeInventoryTabs);
        if(clickedMainCategory == -1) {
            this.drawTexturedModalRect(guiLeft, guiTop-28, 0, 0, 168, 32);
        } else {
            int selStart = clickedMainCategory*28;
            this.drawTexturedModalRect(guiLeft, guiTop-28, 0, 0, selStart, 32);
            this.drawTexturedModalRect(guiLeft+selStart+28, guiTop-28, selStart+28, 0,
                    168-selStart-28, 32);
            if(clickedMainCategory >= 0 && clickedMainCategory < mainCategories.length) {
                Category mainCategory = mainCategories[clickedMainCategory];

                for(int i=0; i<mainCategory.subcategories.length; i++) {
                    if(i != clickedSubCategory) drawCategorySide(i);
                }
            }
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(creativeTabSearch);
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, getXSize(), ySplit);
        int y = guiTop+ySplit;
        for(int i=0; i<splits; i++) {
            this.drawTexturedModalRect(guiLeft, y, 0, ySplit, getXSize(), ySplit+ySplitSize);
            y += ySplitSize;
        }
        this.drawTexturedModalRect(guiLeft, y, 0, ySplit, getXSize(), 136-ySplit);

        //Categories
        Minecraft.getMinecraft().getTextureManager().bindTexture(creativeInventoryTabs);
        if(clickedMainCategory != -1) {
            int selStart = clickedMainCategory*28;

            this.drawTexturedModalRect(guiLeft+selStart, guiTop-28, 28, 32, 28, 32);

            if(clickedMainCategory >= 0 && clickedMainCategory < mainCategories.length) {
                Category mainCategory = mainCategories[clickedMainCategory];

                if(clickedSubCategory >= 0 && clickedSubCategory < mainCategory.subcategories.length) {
                    drawCategorySide(clickedSubCategory);
                }
            }
        }

        //Category icons
        for(int i=0; i<mainCategories.length; i++) {
            Category category = mainCategories[i];
            Utils.drawItemStack(category.displayItem, guiLeft+28*i+6, guiTop-28+9);
        }
        if(clickedMainCategory >= 0 && clickedMainCategory < mainCategories.length) {
            Category mainCategory = mainCategories[clickedMainCategory];

            for(int i=0; i<mainCategory.subcategories.length; i++) {
                Utils.drawItemStack(mainCategory.subcategories[i].displayItem, guiLeft-19, guiTop+23+28*i);
            }
        }

        for(int i=0; i<controls.length; i++) {
            Utils.drawItemStack(controls[i], guiLeft+9+18*i, guiTop+112+18*splits);
            if(mouseX > guiLeft+9+18*i && mouseX < guiLeft+9+18*i+16) {
                if(mouseY > guiTop+112+18*splits && mouseY < guiTop+112+18*splits+16) {
                    tooltipToRender = getTooltipForControl(i);
                }
            }
        }

        int totalItems = auctionIds.size();
        int itemsScroll = (int)Math.floor((totalItems*scrollAmount)/9f)*9;

        int maxItemScroll = Math.max(0, totalItems - (5+splits)*9);
        itemsScroll = Math.min(itemsScroll, maxItemScroll);

        out:
        for(int i=0; i<5+splits; i++) {
            int itemY = guiTop + i*18 + 18;
            for(int j=0; j<9; j++) {
                int itemX = guiLeft + j*18 + 9;
                int id = itemsScroll + i*9 + j;
                if(auctionIds.size() <= id) break out;

                try {
                    String aucid = auctionIds.get(id);

                    GL11.glTranslatef(0,0,100);
                    ItemStack stack = manager.auctionManager.getAuctionItems().get(aucid).getStack();
                    Utils.drawItemStack(stack, itemX, itemY);
                    GL11.glTranslatef(0,0,-100);

                    if(mouseX > itemX && mouseX < itemX+16) {
                        if(mouseY > itemY && mouseY < itemY+16) {
                            tooltipToRender = getTooltipForAucId(aucid);
                        }
                    }
                } catch(Exception e) {
                    break out;
                }
            }
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        searchField.drawTextBox();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if(auctionIds.size() == 0 && searchField.getText().length() == 0) {
            drawRect(guiLeft+8, guiTop+17, guiLeft+170, guiTop+107+18*splits,
                    new Color(100, 100, 100, 100).getRGB());

            FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
            int strWidth = fr.getStringWidth("Loading items...");
            fr.drawString("Loading items...", guiLeft+(8+170-strWidth)/2, guiTop+(17+107+18*splits)/2, Color.BLACK.getRGB());
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(creativeInventoryTabs);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawTexturedModalRect(guiLeft+175, guiTop+18+(int)((95+ySplitSize*2)*scrollAmount),
                256-(scrollClicked?12:24), 0, 12, 15);

        if(tooltipToRender != null) {
            List<String> tooltipGray = new ArrayList<>();
            for(String line : tooltipToRender) {
                tooltipGray.add(EnumChatFormatting.GRAY + line);
            }
            Utils.drawHoveringText(tooltipGray, mouseX, mouseY, width,
                    height, -1, Minecraft.getMinecraft().fontRendererObj);
        }
    }

    public List<String> getTooltipForControl(int index) {
        List<String> lore = new ArrayList<>();
        String arrow = "\u25b6";
        String selPrefix = EnumChatFormatting.DARK_AQUA + " " + arrow + " ";
        String unselPrefix = EnumChatFormatting.GRAY.toString();
        switch(index) {
            case 0: break;
            case 1:
                lore.add("");
                String[] linesSort = {"Highest Bid","Lowest Bid","Ending soon"};
                for(int i=0; i<linesSort.length; i++) {
                    String line = linesSort[i];
                    if(i == sortMode) {
                        line = selPrefix + line;
                    } else {
                        line = unselPrefix + line;
                    }
                    lore.add(line);
                }
                lore.add("");
                lore.add(EnumChatFormatting.YELLOW + "Click to switch sort!");
                return lore;
            case 2:
                lore.add("");
                lore.add(rarityFilter == -1 ? selPrefix : unselPrefix + "No Filter");

                for(int i=0; i<rarities.length; i++) {
                    lore.add(rarityFilter == i ? selPrefix : unselPrefix + "Filter " + rarities[i]);
                }
                lore.add("");
                lore.add(EnumChatFormatting.AQUA + "Right-Click to go backwards!");
                lore.add(EnumChatFormatting.YELLOW + "Click to switch filter!");
                return lore;
            case 3: break;
            case 4:
                lore.add("");
                String off = EnumChatFormatting.RED + "OFF";
                String on = EnumChatFormatting.GREEN + "ON";
                lore.add(unselPrefix+"Filter Own Auctions: " + (filterMyAuctions ? on : off));
                lore.add("");
                lore.add(EnumChatFormatting.YELLOW + "Click to toggle!");
                return lore;
            case 5: break;
            case 6:
                lore.add("");
                String[] linesBin = {"Show All","BIN Only","Auctions Only"};
                for(int i=0; i<linesBin.length; i++) {
                    String line = linesBin[i];
                    if(i == binFilter) {
                        line = selPrefix + line;
                    } else {
                        line = unselPrefix + line;
                    }
                    lore.add(line);
                }
                lore.add("");
                lore.add(EnumChatFormatting.AQUA + "Right-Click to go backwards!");
                lore.add(EnumChatFormatting.YELLOW + "Click to switch filter!");
                return lore;
            case 7: break;
            case 8: break;
        }
        return new ArrayList<>();
    }

    public void handleMouseInput() {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();

        int mouseX = Mouse.getEventX() * width / Minecraft.getMinecraft().displayWidth;
        int mouseY = height - Mouse.getEventY() * height / Minecraft.getMinecraft().displayHeight - 1;
        int mouseButton = Mouse.getEventButton();

        if (Mouse.getEventButtonState()) {
            this.eventButton = mouseButton;
            this.lastMouseEvent = Minecraft.getSystemTime();
            mouseClicked(mouseX, mouseY, this.eventButton);
        } else if (mouseButton != -1) {
            this.eventButton = -1;
            mouseReleased(mouseX, mouseY, mouseButton);
        } else if (this.eventButton != -1 && this.lastMouseEvent > 0L) {
            long l = Minecraft.getSystemTime() - this.lastMouseEvent;
            mouseClickMove(mouseX, mouseY, this.eventButton, l);
        }

        int dWheel = Mouse.getEventDWheel();
        dWheel = Math.max(-1, Math.min(1, dWheel));

        scrollAmount = scrollAmount - dWheel/(float)(auctionIds.size()/9-(5+splits));
        scrollAmount = Math.max(0, Math.min(1, scrollAmount));
    }

    private String niceAucId(String aucId) {
        if(aucId.length()!=32) return aucId;

        StringBuilder niceAucId = new StringBuilder();
        niceAucId.append(aucId, 0, 8);
        niceAucId.append("-");
        niceAucId.append(aucId, 8, 12);
        niceAucId.append("-");
        niceAucId.append(aucId, 12, 16);
        niceAucId.append("-");
        niceAucId.append(aucId, 16, 20);
        niceAucId.append("-");
        niceAucId.append(aucId, 20, 32);
        return niceAucId.toString();
    }

    public Category getCurrentCategory() {
        if(clickedMainCategory < 0 || clickedMainCategory >= mainCategories.length) {
            return null;
        }
        Category mainCategory = mainCategories[clickedMainCategory];
        if(clickedSubCategory < 0 || clickedSubCategory >= mainCategory.subcategories.length) {
            return mainCategory;
        }
        return mainCategory.subcategories[clickedSubCategory];
    }

    public void updateSearch() {
        if(searchField == null || priceField == null) init();

        scrollAmount = 0;
        auctionIds.clear();
        try {
            if(searchField.getText().length() == 0) {
                auctionIds.clear();
                auctionIds.addAll(manager.auctionManager.getAuctionItems().keySet());
            } else {
                auctionIds.clear();
                auctionIds.addAll(manager.search(searchField.getText(), manager.auctionManager.extrasToAucIdMap));

                if(!searchField.getText().trim().contains(" ")) {
                    StringBuilder sb = new StringBuilder();
                    for(char c : searchField.getText().toCharArray()) {
                        sb.append(c).append(" ");
                    }
                    for(String aucid : manager.search(sb.toString(), manager.auctionManager.extrasToAucIdMap)) {
                        if(!auctionIds.contains(aucid)) {
                            auctionIds.add(aucid);
                        }
                    }
                }
            }
            Category currentCategory = getCurrentCategory();
            Set<String> toRemove = new HashSet<>();
            String[] categories = new String[0];
            if(currentCategory != null) categories = currentCategory.getTotalCategories();
            for(String aucid : auctionIds) {
                AuctionManager.Auction auc = manager.auctionManager.getAuctionItems().get(aucid);
                if(auc != null) {
                    boolean match = categories.length == 0;
                    for(String category : categories) {
                        if (category.equalsIgnoreCase(auc.category)) {
                            match = true;
                            break;
                        }
                    }
                    if(rarityFilter > 0 && rarityFilter < rarities.length) {
                        if(!rarities[rarityFilter].equals(auc.rarity)) {
                            match = false;
                        }
                    }
                    if(binFilter == BIN_FILTER_BIN) {
                        match &= auc.bin;
                    } else if(binFilter == BIN_FILTER_AUC) {
                        match &= !auc.bin;
                    }

                    if(!match) {
                        toRemove.add(aucid);
                    }
                } else {
                    toRemove.add(aucid);
                }
            }
            auctionIds.removeAll(toRemove);
            sortItems();
        } catch(ConcurrentModificationException e) {
            updateSearch();
        }
    }

    public void sortItems() throws ConcurrentModificationException {
        auctionIds.sort((o1, o2) -> {
            AuctionManager.Auction auc1 = manager.auctionManager.getAuctionItems().get(o1);
            AuctionManager.Auction auc2 = manager.auctionManager.getAuctionItems().get(o2);

            if(auc1 == null) return 1;
            if(auc2 == null) return -1;

            if(sortMode == SORT_MODE_HIGH) {
                int price1 = Math.max(auc1.highest_bid_amount, auc1.starting_bid);
                int price2 = Math.max(auc2.highest_bid_amount, auc2.starting_bid);
                return price2 - price1;
            } else if(sortMode == SORT_MODE_LOW) {
                int price1 = Math.max(auc1.highest_bid_amount, auc1.starting_bid);
                int price2 = Math.max(auc2.highest_bid_amount, auc2.starting_bid);
                return price1 - price2;
            } else {
                return (int)(auc1.end - auc2.end);
            }
        });
    }

    public boolean keyboardInput() {
        Keyboard.enableRepeatEvents(true);
        if(isEditingPrice() && Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        } else if(Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
            return false;
        }
        if(Keyboard.getEventKeyState()) keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
        return true;
    }

    public void keyTyped(char typedChar, int keyCode) {
        if(searchField == null || priceField == null) init();

        if(!isEditingPrice()) {
            if(this.searchField.textboxKeyTyped(typedChar, keyCode)) {
                this.updateSearch();
            }
        } else {
            priceField.textboxKeyTyped(typedChar, keyCode);
        }
    }

    private void increasePriceByFactor(float factor) {
        String price = priceField.getText().trim();
        StringBuilder priceNumbers = new StringBuilder();
        for(int i=0; i<price.length(); i++) {
            char c = price.charAt(i);
            if((int)c >= 48 && (int)c <= 57) {
                priceNumbers.append(c);
            } else {
                break;
            }
        }
        int priceI = 0;
        if(priceNumbers.length() > 0) {
            priceI = Integer.parseInt(priceNumbers.toString());
        }
        String end = price.substring(priceNumbers.length());
        priceField.setText((int)(priceI*factor) + end);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        searchField.mouseClicked(mouseX, mouseY, mouseButton);

        int totalItems = auctionIds.size();
        int itemsScroll = (int)Math.floor((totalItems*scrollAmount)/9f)*9;

        int maxItemScroll = Math.max(0, totalItems - (5+splits)*9);
        itemsScroll = Math.min(itemsScroll, maxItemScroll);

        //Categories
        if(mouseY > guiTop-28 && mouseY < guiTop+4) {
            if(mouseX > guiLeft && mouseX < guiLeft+168) {
                int offset = mouseX-guiLeft;
                int clickedCat = offset/28;
                if(clickedMainCategory == clickedCat) {
                    clickedMainCategory = -1;
                } else {
                    clickedMainCategory = clickedCat;
                }
                clickedSubCategory = -1;
                updateSearch();
                return;
            }
        }

        if(clickedMainCategory >= 0 && clickedMainCategory < mainCategories.length) {
            Category mainCategory = mainCategories[clickedMainCategory];

            if(mouseX > guiLeft-28 && mouseX < guiLeft) {
                int offset = mouseY-(guiTop+17);
                if(offset > 0) {
                    int clicked = offset/28;
                    if(clicked == clickedSubCategory) {
                        clickedSubCategory = -1;
                        updateSearch();
                        return;
                    } else if(clicked < mainCategory.subcategories.length) {
                        clickedSubCategory = clicked;
                        updateSearch();
                        return;
                    }
                }
            }
        }

        if(mouseY > guiTop+112+18*splits && mouseY < guiTop+112+18*splits+16) {
            if(mouseX > guiLeft+9 && mouseX < guiLeft+9+controls.length*18-2) {
                int offset = mouseX - (guiLeft+9);
                int index = offset/18;
                boolean rightClicked = Mouse.getEventButton() == 1;
                switch(index) {
                    case 0: break;
                    case 1:
                        if(rightClicked) {
                            sortMode--;
                            if(sortMode < SORT_MODE_HIGH) sortMode = SORT_MODE_SOON;
                        } else {
                            sortMode++;
                            if(sortMode > SORT_MODE_SOON) sortMode = SORT_MODE_HIGH;
                        }
                        break;
                    case 2:
                        if(rightClicked) {
                            rarityFilter--;
                            if(rarityFilter < -1) rarityFilter = rarities.length-1;
                        } else {
                            rarityFilter++;
                            if(rarityFilter >= rarities.length) rarityFilter = -1;
                        }
                        break;
                    case 3: break;
                    case 4:
                        filterMyAuctions = !filterMyAuctions;
                        break;
                    case 5: break;
                    case 6:
                        if(rightClicked) {
                            binFilter--;
                            if(binFilter < BIN_FILTER_ALL) binFilter = BIN_FILTER_AUC;
                        } else {
                            binFilter++;
                            if(binFilter > BIN_FILTER_AUC) binFilter = BIN_FILTER_ALL;
                        }
                    case 7: break;
                    case 8: break;
                }
                sortItems();
            }
        }

        for(int i=0; i<controls.length; i++) {
            Utils.drawItemStack(controls[i], guiLeft+9+18*i, guiTop+112+18*splits);
        }

        if(clickedMainCategory >= 0 && clickedMainCategory < mainCategories.length) {
            Category mainCategory = mainCategories[clickedMainCategory];

            for(int i=0; i<mainCategory.subcategories.length; i++) {
                boolean clicked = i == clickedSubCategory;
                Utils.drawTexturedRect(guiLeft-28, guiTop+17*28*i, 32, 28,
                        0, 28/256f, 0+(clicked?32/256f:0), 32/256f+(clicked?32/256f:0));
            }
        }

        if(mouseButton == 0 && Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
            GuiChest auctionView = (GuiChest) Minecraft.getMinecraft().currentScreen;

            if(mouseX > guiLeft+getXSize()+4+31 && mouseX < guiLeft+getXSize()+4+31+16) {
                boolean leftFiller = isGuiFiller(auctionView.inventorySlots.getSlot(29).getStack());
                if(mouseY > guiTop+100 && mouseY < guiTop+100+16) {
                    int slotClick =  leftFiller ? 31 : 29;
                    Minecraft.getMinecraft().playerController.windowClick(auctionView.inventorySlots.windowId,
                            slotClick, 2, 3, Minecraft.getMinecraft().thePlayer);
                } else if(mouseY > guiTop+126 && mouseY < guiTop+126+16 && !leftFiller) {
                    Minecraft.getMinecraft().playerController.windowClick(auctionView.inventorySlots.windowId,
                            31, 2, 3, Minecraft.getMinecraft().thePlayer);
                }
            }
        }
        if(mouseButton == 0 && isEditingPrice()) {
            int auctionViewLeft = guiLeft+getXSize()+4;

            if(mouseX > auctionViewLeft+16 && mouseX < auctionViewLeft+16+64) {
                if(mouseY > guiTop+32 && mouseY < guiTop+32+16) {
                    if(mouseX < auctionViewLeft+16+32) {
                        //top left
                        increasePriceByFactor(2);
                    } else {
                        //top right
                        increasePriceByFactor(1.5f);
                    }
                } else if(mouseY > guiTop+50 && mouseY < guiTop+50+16) {
                    if(mouseX < auctionViewLeft+16+32) {
                        //mid left
                        increasePriceByFactor(1.25f);
                    } else {
                        //mid right
                        increasePriceByFactor(1.1f);
                    }
                } else if(mouseY > guiTop+68 && mouseY < guiTop+68+16) {
                    //bottom
                    Minecraft.getMinecraft().displayGuiScreen(null);
                }
            }

            this.drawTexturedModalRect(auctionViewLeft+16, guiTop+32, 0, 16, 30, 16);
            this.drawTexturedModalRect(auctionViewLeft+16+34, guiTop+32, 0, 16, 30, 16);
            this.drawTexturedModalRect(auctionViewLeft+16, guiTop+50, 0, 16, 30, 16);
            this.drawTexturedModalRect(auctionViewLeft+16+34, guiTop+50, 0, 16, 30, 16);
            this.drawTexturedModalRect(auctionViewLeft+16, guiTop+68, 0, 32, 64, 16);
        }

        out:
        for(int i=0; i<5+splits; i++) {
            int itemY = guiTop + i*18 + 18;
            for(int j=0; j<9; j++) {
                int itemX = guiLeft + j*18 + 9;
                int id = itemsScroll + i*9 + j;
                if(auctionIds.size() <= id) break out;

                String aucid;
                try {
                    aucid = auctionIds.get(id);
                } catch (IndexOutOfBoundsException e) { break out; }

                AuctionManager.Auction auc = manager.auctionManager.getAuctionItems().get(aucid);
                if(auc != null) {
                    long timeUntilEnd = auc.end - System.currentTimeMillis();

                    if(timeUntilEnd > 0) {
                        if(mouseX > itemX && mouseX < itemX+16) {
                            if(mouseY > itemY && mouseY < itemY+16) {
                                if(Minecraft.getMinecraft().currentScreen instanceof GuiEditSign) {
                                    priceField.setText("cancel");
                                    GuiEditSign editSign = (GuiEditSign) Minecraft.getMinecraft().currentScreen;
                                    TileEntitySign tes = (TileEntitySign)Utils.getField(GuiEditSign.class, editSign,
                                            "tileSign", "field_146848_f");
                                    tes.lineBeingEdited = 0;
                                    tes.signText[0] = new ChatComponentText(priceField.getText());
                                }
                                startingBid = Math.max(auc.starting_bid, auc.highest_bid_amount);
                                NotEnoughUpdates.INSTANCE.sendChatMessage("/viewauction "+niceAucId(aucid));
                            }
                        }
                    }
                }
            }
        }

        int y = guiTop+18+(int)((95+ySplitSize*2)*scrollAmount);
        if(mouseX > guiLeft+175 && mouseX < guiLeft+175+12) {
            if(mouseY > y && mouseY < y+15) {
                scrollClicked = true;
                return;
            }
        }
        scrollClicked = false;
    }

    protected void mouseReleased(int mouseX, int mouseY, int state) {
        scrollClicked = false;
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if(scrollClicked) {
            int yMin = guiTop+18                   + 8;
            int yMax = guiTop+18+(95+ySplitSize*2) + 8;

            scrollAmount = (mouseY-yMin)/(float)(yMax-yMin);
            scrollAmount = Math.max(0, Math.min(1, scrollAmount));
        }
    }
}
