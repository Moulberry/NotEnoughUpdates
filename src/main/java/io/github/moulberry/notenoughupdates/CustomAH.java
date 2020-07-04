package io.github.moulberry.notenoughupdates;

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
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

import static io.github.moulberry.notenoughupdates.GuiTextures.*;

public class CustomAH extends Gui {

    private static final ResourceLocation creativeTabSearch = new ResourceLocation("textures/gui/container/creative_inventory/tab_item_search.png");
    private static final ResourceLocation creativeInventoryTabs = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

    private List<String> auctionIds = new ArrayList<>();

    private boolean scrollClicked = false;

    private int startingBid = 0;

    private GuiTextField searchField = null;
    private GuiTextField priceField = null;

    private boolean renderOverAuctionView = false;
    private long lastRenderDisable = 0;

    private int eventButton;
    private long lastMouseEvent;

    private int splits = 2;

    private int ySplit = 35;
    private int ySplitSize = 18;

    private float scrollAmount;

    private int guiLeft = 0;
    private int guiTop = 0;

    private NEUManager manager;

    public CustomAH(NEUManager manager) {
        this.manager = manager;
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
        NEUManager.Auction auc = manager.getAuctionItems().get(aucId);

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

    public void drawScreen(int mouseX, int mouseY) {
        Mouse.setGrabbed(false);
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();

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

            int auctionViewLeft = guiLeft+getXSize()+4;

            Minecraft.getMinecraft().getTextureManager().bindTexture(auction_view);
            this.drawTexturedModalRect(auctionViewLeft, guiTop, 0, 0, 78, 172);

            try {
                ItemStack itemStack = auctionView.inventorySlots.getSlot(13).getStack();
                Utils.drawItemStack(itemStack, auctionViewLeft+31, guiTop+35);

                ItemStack bidStack = auctionView.inventorySlots.getSlot(29).getStack();
                Utils.drawItemStack(bidStack, auctionViewLeft+31, guiTop+100);

                ItemStack historyStack = auctionView.inventorySlots.getSlot(33).getStack();

                ItemStack changeBidStack = auctionView.inventorySlots.getSlot(31).getStack();

                if(changeBidStack != null && changeBidStack.getTagCompound().hasKey("AttributeModifiers")) {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(auction_view_buttons);
                    boolean hover = mouseX > auctionViewLeft+31 && mouseX <auctionViewLeft+31+16 &&
                            mouseY > guiTop+126 && mouseY < guiTop+126+16;
                    this.drawTexturedModalRect(auctionViewLeft+31, guiTop+126, hover?16:0, 0, 16, 16);
                } else {
                    changeBidStack = null;
                }

                if(mouseX > auctionViewLeft+31 && mouseX <auctionViewLeft+31+16) {
                    if(mouseY > guiTop+35 && mouseY < guiTop+35+16) {
                        if(itemStack != null) tooltipToRender = itemStack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                    } else if(mouseY > guiTop+100 && mouseY < guiTop+100+16) {
                        if(bidStack != null) tooltipToRender = bidStack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                    } else if(mouseY > guiTop+61 && mouseY < guiTop+61+16) {
                        if(historyStack != null) tooltipToRender = historyStack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                    } else if(mouseY > guiTop+126 && mouseY < guiTop+126+16) {
                        if(changeBidStack != null) tooltipToRender = changeBidStack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                    }
                }
            } catch(NullPointerException e) { //i cant be bothered
            }
        } else if(isEditingPrice()) {
            int auctionViewLeft = guiLeft+getXSize()+4;

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

        Minecraft.getMinecraft().getTextureManager().bindTexture(creativeTabSearch);
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, getXSize(), ySplit);
        int y = guiTop+ySplit;
        for(int i=0; i<splits; i++) {
            this.drawTexturedModalRect(guiLeft, y, 0, ySplit, getXSize(), ySplit+ySplitSize);
            y += ySplitSize;
        }
        this.drawTexturedModalRect(guiLeft, y, 0, ySplit, getXSize(), 136-ySplit);

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
                    ItemStack stack = manager.getAuctionItems().get(aucid).getStack();
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
        this.drawTexturedModalRect(guiLeft+175, guiTop+18+(int)((95+ySplitSize*2)*scrollAmount),
                256-(scrollClicked?12:24), 0, 12, 15);

        if(tooltipToRender != null) {
            Utils.drawHoveringText(tooltipToRender, mouseX, mouseY, width,
                    height, -1, Minecraft.getMinecraft().fontRendererObj);
        }
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

        int dWheel = Mouse.getDWheel();

        scrollAmount = scrollAmount - dWheel/((float)auctionIds.size()-(5+splits));
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

    public void updateSearch() {
        scrollAmount = 0;
        auctionIds.clear();
        try {
            for(Map.Entry<String, NEUManager.Auction> entry : manager.getAuctionItems().entrySet()) {
                if(searchField.getText().length() == 0 ||
                        Utils.cleanColour(entry.getValue().extras).toLowerCase().contains(searchField.getText().toLowerCase())) {
                    auctionIds.add(entry.getKey());
                }
            }
        } catch(ConcurrentModificationException e) {
            updateSearch();
        }
    }

    public boolean keyboardInput() {
        if(isEditingPrice() && Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        } else if(Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
            return false;
        }
        keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
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

        if(mouseButton == 0 && Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
            GuiChest auctionView = (GuiChest) Minecraft.getMinecraft().currentScreen;

            if(mouseX > guiLeft+getXSize()+4+31 && mouseX < guiLeft+getXSize()+4+31+16) {
                if(mouseY > guiTop+100 && mouseY < guiTop+100+16) {
                    Minecraft.getMinecraft().playerController.windowClick(auctionView.inventorySlots.windowId,
                            29, 2, 3, Minecraft.getMinecraft().thePlayer);
                } else if(mouseY > guiTop+126 && mouseY < guiTop+126+16) {
                    Minecraft.getMinecraft().playerController.windowClick(auctionView.inventorySlots.windowId,
                            31, 0, 4, Minecraft.getMinecraft().thePlayer);
                }
            }
        }
        if(mouseButton == 0 && isEditingPrice()) {
            int auctionViewLeft = guiLeft+getXSize()+4;
            System.out.println("1");

            if(mouseX > auctionViewLeft+16 && mouseX < auctionViewLeft+16+64) {
                System.out.println("2");
                if(mouseY > guiTop+32 && mouseY < guiTop+32+16) {
                    System.out.println("3");
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

                NEUManager.Auction auc = manager.getAuctionItems().get(aucid);
                long timeUntilEnd = auc.end - System.currentTimeMillis();

                if(timeUntilEnd > 0) {
                    if(mouseX > itemX && mouseX < itemX+16) {
                        if(mouseY > itemY && mouseY < itemY+16) {
                            startingBid = Math.max(auc.starting_bid, auc.highest_bid_amount);
                            NotEnoughUpdates.INSTANCE.sendChatMessage("/viewauction "+niceAucId(aucid));
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
