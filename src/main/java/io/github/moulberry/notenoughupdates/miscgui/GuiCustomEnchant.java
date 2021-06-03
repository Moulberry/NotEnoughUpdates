package io.github.moulberry.notenoughupdates.miscgui;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.miscfeatures.SlotLocking;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.model.ModelBook;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

public class GuiCustomEnchant extends Gui {

    private static final GuiCustomEnchant INSTANCE = new GuiCustomEnchant();
    private static final ResourceLocation TEXTURE = new ResourceLocation("notenoughupdates:custom_enchant_gui.png");
    private static final ResourceLocation ENCHANTMENT_TABLE_BOOK_TEXTURE = new ResourceLocation("textures/entity/enchanting_table_book.png");
    private static final ModelBook MODEL_BOOK = new ModelBook();

    private enum EnchantState {
        NO_ITEM,
        INVALID_ITEM,
        HAS_ITEM
    }

    private static class Enchantment {
        public int slotIndex;
        public String enchantName;
        public int xpCost = 30;

        public Enchantment(int slotIndex, String enchantName, int xpCost) {
            this.slotIndex = slotIndex;
            this.enchantName = enchantName;
            this.xpCost = xpCost;
        }
    }

    private int guiLeft;
    private int guiTop;
    private boolean shouldOverrideFast = false;

    public float pageOpen;
    public float pageOpenLast;
    public float pageOpenRandom;
    public float pageOpenVelocity;
    public float bookOpen;
    public float bookOpenLast;

    private static List<Enchantment> applicable = new ArrayList<>();
    private static List<Enchantment> removable = new ArrayList<>();

    public Random random = new Random();

    private EnchantState currentState = EnchantState.NO_ITEM;
    private EnchantState lastState = EnchantState.NO_ITEM;

    private static final int X_SIZE = 364;
    private static final int Y_SIZE = 215;

    public static GuiCustomEnchant getInstance() {
        return INSTANCE;
    }

    public boolean shouldOverride(String containerName) {
        shouldOverrideFast = containerName != null &&
                NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() &&
                containerName.equalsIgnoreCase("Enchant Item") && !Keyboard.isKeyDown(Keyboard.KEY_K);
        if(!shouldOverrideFast) {
            currentState = EnchantState.NO_ITEM;
            applicable.clear();
            removable.clear();
        }
        return shouldOverrideFast;
    }

    public void tick() {
        GuiContainer chest = ((GuiContainer)Minecraft.getMinecraft().currentScreen);
        ContainerChest cc = (ContainerChest) chest.inventorySlots;

        ItemStack stack = cc.getLowerChestInventory().getStackInSlot(23);
        ItemStack enchantingItemStack = cc.getLowerChestInventory().getStackInSlot(19);

        if(stack == null || enchantingItemStack == null) {
            currentState = EnchantState.NO_ITEM;
        } else if(stack.getItem() != Items.dye) {
            currentState = EnchantState.HAS_ITEM;
        } else if(stack.getItemDamage() == 1) {
            currentState = EnchantState.INVALID_ITEM;
        } else {
            currentState = EnchantState.NO_ITEM;
        }

        applicable.clear();
        removable.clear();
        if(currentState == EnchantState.HAS_ITEM) {
            Set<String> playerEnchantIds = new HashSet<>();

            NBTTagCompound tag = enchantingItemStack.getTagCompound();
            if(tag != null) {
                NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");
                if(ea != null) {
                    NBTTagCompound enchantments = ea.getCompoundTag("enchantments");
                    if(enchantments != null) {
                        playerEnchantIds.addAll(enchantments.getKeySet());
                    }
                }
            }

            for(int i=0; i<15; i++) {
                int slotIndex = 12 + (i%5) + (i/5)*9;
                ItemStack book = cc.getLowerChestInventory().getStackInSlot(slotIndex);
                if(book != null) {
                    NBTTagCompound tagBook = book.getTagCompound();
                    if(tagBook != null) {
                        NBTTagCompound ea = tagBook.getCompoundTag("ExtraAttributes");
                        if(ea != null) {
                            NBTTagCompound enchantments = ea.getCompoundTag("enchantments");
                            if(enchantments != null) {
                                for(String enchId : enchantments.getKeySet()) {
                                    String name = Utils.cleanColour(book.getDisplayName());
                                    if(name.equalsIgnoreCase("Bane of Arthropods")) {
                                        name = "Bane of Arth.";
                                    } else if(name.equalsIgnoreCase("Projectile Protection")) {
                                        name = "Projectile Prot";
                                    } else if(name.equalsIgnoreCase("Blast Protection")) {
                                        name = "Blast Prot";
                                    }
                                    Enchantment enchantment = new Enchantment(slotIndex, name, 30);
                                    if(playerEnchantIds.contains(enchId)) {
                                        removable.add(enchantment);
                                    } else {
                                        applicable.add(enchantment);
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }

        //Update book model state
        if (lastState != currentState) {
            this.lastState = currentState;

            while (true) {
                this.pageOpenRandom += (float)(this.random.nextInt(4) - this.random.nextInt(4));

                if (this.pageOpen > this.pageOpenRandom + 1.0F || this.pageOpen < this.pageOpenRandom - 1.0F) {
                    break;
                }
            }
        }

        this.pageOpenLast = this.pageOpen;
        this.bookOpenLast = this.bookOpen;

        if (currentState == EnchantState.HAS_ITEM) {
            this.bookOpen += 0.2F;
        } else {
            this.bookOpen -= 0.2F;
        }

        this.bookOpen = MathHelper.clamp_float(this.bookOpen, 0.0F, 1.0F);
        float f1 = (this.pageOpenRandom - this.pageOpen) * 0.4F;
        f1 = MathHelper.clamp_float(f1, -0.2F, 0.2F);
        this.pageOpenVelocity += (f1 - this.pageOpenVelocity) * 0.9F;
        this.pageOpen += this.pageOpenVelocity;
    }

    public void render(float partialTicks) {
        if(!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) return;

        GuiContainer chest = ((GuiContainer)Minecraft.getMinecraft().currentScreen);
        ContainerChest cc = (ContainerChest) chest.inventorySlots;

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();
        int mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
        int mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;

        guiLeft = (width-X_SIZE)/2;
        guiTop = (height-Y_SIZE)/2;

        List<String> tooltipToDisplay = null;
        boolean disallowClick = false;
        ItemStack stackOnMouse = Minecraft.getMinecraft().thePlayer.inventory.getItemStack();
        int itemHoverX = -1;
        int itemHoverY = -1;
        boolean hoverLocked = false;

        drawGradientRect(0, 0, width, height, 0xc0101010, 0xd0101010);

        //Base Texture
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        GlStateManager.color(1, 1, 1, 1);
        Utils.drawTexturedRect(guiLeft, guiTop, X_SIZE, Y_SIZE,
                0, X_SIZE/512f, 0, Y_SIZE/512f, GL11.GL_NEAREST);

        //Enchant book model
        renderEnchantBook(scaledResolution, partialTicks);

        //Available enchants (left)
        for(int i=0; i<6; i++) {
            if(applicable.size() <= i) break;

            Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
            GlStateManager.color(1, 1, 1, 1);
            Utils.drawTexturedRect(guiLeft+8, guiTop+18+16*i, 96, 16,
                    0, 96/512f, 249/512f, (249+16)/512f, GL11.GL_NEAREST);

            //Utils.drawTexturedRect(guiLeft+8, guiTop+18+16*i, 16, 16,
            //        0/512f, 16/512f, 217/512f, (217+16)/512f, GL11.GL_NEAREST);

            String levelStr = "35";
            int levelWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(levelStr);
            Minecraft.getMinecraft().fontRendererObj.drawString(levelStr, guiLeft+16-levelWidth/2-1, guiTop+18+16*i+4, 0x2d2102, false);
            Minecraft.getMinecraft().fontRendererObj.drawString(levelStr, guiLeft+16-levelWidth/2+1, guiTop+18+16*i+4, 0x2d2102, false);
            Minecraft.getMinecraft().fontRendererObj.drawString(levelStr, guiLeft+16-levelWidth/2, guiTop+18+16*i+4-1, 0x2d2102, false);
            Minecraft.getMinecraft().fontRendererObj.drawString(levelStr, guiLeft+16-levelWidth/2, guiTop+18+16*i+4+1, 0x2d2102, false);
            Minecraft.getMinecraft().fontRendererObj.drawString(levelStr, guiLeft+16-levelWidth/2, guiTop+18+16*i+4, 0xc8ff8f, false);

            String name = applicable.get(i).enchantName;
            Minecraft.getMinecraft().fontRendererObj.drawString(name, guiLeft+8+16+2, guiTop+18+16*i+4, 0xffffffdd, true);
        }

        //Removable enchants (left)
        for(int i=0; i<6; i++) {
            if(removable.size() <= i) break;

            Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
            GlStateManager.color(1, 1, 1, 1);
            Utils.drawTexturedRect(guiLeft+248, guiTop+18+16*i, 96, 16,
                    0, 96/512f, 249/512f, (249+16)/512f, GL11.GL_NEAREST);

            //Utils.drawTexturedRect(guiLeft+8, guiTop+18+16*i, 16, 16,
            //        0/512f, 16/512f, 217/512f, (217+16)/512f, GL11.GL_NEAREST);

            String levelStr = "35";
            int levelWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(levelStr);
            Minecraft.getMinecraft().fontRendererObj.drawString(levelStr, guiLeft+256-levelWidth/2-1, guiTop+18+16*i+4, 0x2d2102, false);
            Minecraft.getMinecraft().fontRendererObj.drawString(levelStr, guiLeft+256-levelWidth/2+1, guiTop+18+16*i+4, 0x2d2102, false);
            Minecraft.getMinecraft().fontRendererObj.drawString(levelStr, guiLeft+256-levelWidth/2, guiTop+18+16*i+4-1, 0x2d2102, false);
            Minecraft.getMinecraft().fontRendererObj.drawString(levelStr, guiLeft+256-levelWidth/2, guiTop+18+16*i+4+1, 0x2d2102, false);
            Minecraft.getMinecraft().fontRendererObj.drawString(levelStr, guiLeft+256-levelWidth/2, guiTop+18+16*i+4, 0xc8ff8f, false);

            String name = removable.get(i).enchantName;
            Minecraft.getMinecraft().fontRendererObj.drawString(name, guiLeft+248+16+2, guiTop+18+16*i+4, 0xffffffdd, true);
        }

        //Player Inventory Items
        Minecraft.getMinecraft().fontRendererObj.drawString(Minecraft.getMinecraft().thePlayer.inventory.getDisplayName().getUnformattedText(),
                guiLeft+102, guiTop+Y_SIZE - 96 + 2, 0x404040);
        int inventoryStartIndex = cc.getLowerChestInventory().getSizeInventory();
        GlStateManager.enableDepth();
        for(int i=0; i<36; i++) {
            int itemX = guiLeft+102+18*(i%9);
            int itemY = guiTop+133+18*(i/9);

            if(i >= 27) {
                itemY += 4;
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate(guiLeft+102-8, guiTop+191-(inventoryStartIndex/9*18+89), 0);
            Slot slot = cc.getSlot(inventoryStartIndex+i);
            chest.drawSlot(slot);
            GlStateManager.popMatrix();

            if(mouseX >= itemX && mouseX < itemX+18 &&
                    mouseY >= itemY && mouseY < itemY+18) {
                itemHoverX = itemX;
                itemHoverY = itemY;
                hoverLocked = SlotLocking.getInstance().isSlotLocked(slot);

                if(slot.getHasStack()) {
                    tooltipToDisplay = slot.getStack().getTooltip(Minecraft.getMinecraft().thePlayer,
                            Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
                }
            }
        }

        //Item enchant input
        ItemStack itemEnchantInput = cc.getSlot(19).getStack();
        {
            int itemX = guiLeft+174;
            int itemY = guiTop+58;

            if(itemEnchantInput == null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
                GlStateManager.color(1, 1, 1, 1);
                Utils.drawTexturedRect(itemX, itemY, 16, 16,
                        0, 16/512f, 281/512f, (281+16)/512f, GL11.GL_NEAREST);
            } else {
                Utils.drawItemStack(itemEnchantInput, itemX, itemY);

            }

            if(mouseX >= itemX && mouseX < itemX+18 &&
                    mouseY >= itemY && mouseY < itemY+18) {
                itemHoverX = itemX;
                itemHoverY = itemY;

                if(itemEnchantInput != null) {
                    tooltipToDisplay = itemEnchantInput.getTooltip(Minecraft.getMinecraft().thePlayer,
                            Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
                }
            }
        }


        if(itemHoverX >= 0 && itemHoverY >= 0) {
            GlStateManager.disableDepth();
            GlStateManager.colorMask(true, true, true, false);
            Gui.drawRect(itemHoverX, itemHoverY, itemHoverX + 16, itemHoverY + 16,
                    hoverLocked ? 0x80ff8080 : 0x80ffffff);
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.enableDepth();
        }

        GlStateManager.translate(0, 0, 300);
        if(stackOnMouse != null) {
            if(disallowClick) {
                Utils.drawItemStack(new ItemStack(Item.getItemFromBlock(Blocks.barrier)), mouseX - 8, mouseY - 8);
            } else {
                Utils.drawItemStack(stackOnMouse, mouseX - 8, mouseY - 8);
            }
        } else if(tooltipToDisplay != null) {
            Utils.drawHoveringText(tooltipToDisplay, mouseX, mouseY,  width, height, -1,
                    Minecraft.getMinecraft().fontRendererObj);
        }
        GlStateManager.translate(0, 0, -300);
    }

    private void renderEnchantBook(ScaledResolution scaledresolution, float partialTicks) {
        GlStateManager.enableDepth();

        GlStateManager.pushMatrix();
        GlStateManager.matrixMode(5889);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.viewport((scaledresolution.getScaledWidth() - 320) / 2 * scaledresolution.getScaleFactor(),
                (scaledresolution.getScaledHeight() - 240) / 2 * scaledresolution.getScaleFactor(),
                320 * scaledresolution.getScaleFactor(), 240 * scaledresolution.getScaleFactor());
        GlStateManager.translate(0.0F, 0.33F, 0.0F);
        Project.gluPerspective(90.0F, 1.3333334F, 9.0F, 80.0F);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.translate(0.0F, 3.3F, -16.0F);
        GlStateManager.scale(5, 5, 5);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(ENCHANTMENT_TABLE_BOOK_TEXTURE);
        GlStateManager.rotate(20.0F, 1.0F, 0.0F, 0.0F);
        float bookOpenAngle = this.bookOpenLast + (this.bookOpen - this.bookOpenLast) * partialTicks;
        GlStateManager.translate((1.0F - bookOpenAngle) * 0.2F, (1.0F - bookOpenAngle) * 0.1F, (1.0F - bookOpenAngle) * 0.25F);
        GlStateManager.rotate(-(1.0F - bookOpenAngle) * 90.0F - 90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        float pageAngle1 = this.pageOpenLast + (this.pageOpen - this.pageOpenLast) * partialTicks + 0.25F;
        float pageAngle2 = this.pageOpenLast + (this.pageOpen - this.pageOpenLast) * partialTicks + 0.75F;
        pageAngle1 = (pageAngle1 - (float) MathHelper.truncateDoubleToInt(pageAngle1)) * 1.6F - 0.3F;
        pageAngle2 = (pageAngle2 - (float)MathHelper.truncateDoubleToInt(pageAngle2)) * 1.6F - 0.3F;

        if (pageAngle1 < 0.0F) pageAngle1 = 0.0F;
        if (pageAngle1 > 1.0F) pageAngle1 = 1.0F;
        if (pageAngle2 < 0.0F) pageAngle2 = 0.0F;
        if (pageAngle2 > 1.0F) pageAngle2 = 1.0F;

        GlStateManager.enableRescaleNormal();
        MODEL_BOOK.render(null, 0.0F, pageAngle1, pageAngle2, bookOpenAngle, 0.0F, 0.0625F);
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.matrixMode(5889);
        GlStateManager.viewport(0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.enableDepth();
    }

    public void overrideIsMouseOverSlot(Slot slot, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
        if(shouldOverrideFast) {
            boolean playerInv = slot.inventory == Minecraft.getMinecraft().thePlayer.inventory;
            int slotId = slot.getSlotIndex();
            if(playerInv && slotId < 36) {
                slotId -= 9;
                if(slotId < 0) slotId += 36;

                int itemX = guiLeft+102+18*(slotId%9);
                int itemY = guiTop+133+18*(slotId/9);

                if(slotId >= 27) {
                    itemY += 4;
                }

                if(mouseX >= itemX && mouseX < itemX+18 &&
                        mouseY >= itemY && mouseY < itemY+18) {
                    cir.setReturnValue(true);
                } else {
                    cir.setReturnValue(false);
                }
            } else if(slotId == 19) {
                cir.setReturnValue(mouseX >= guiLeft+173 && mouseX < guiLeft+173+18 &&
                        mouseY >= guiTop+57 && mouseY < guiTop+57+18);
            }
        }
    }

    public boolean mouseInput(int mouseX, int mouseY) {
        if(mouseX > guiLeft+102 && mouseX < guiLeft+102+144) {
            if(mouseY > guiTop+133 && mouseY < guiTop+133+54) {
                return false;
            } else if(mouseY > guiTop+133+54+4 && mouseY < guiTop+133+54+4+18) {
                return false;
            }
        }
        if(mouseX >= guiLeft+173 && mouseX < guiLeft+173+18 &&
                    mouseY >= guiTop+57 && mouseY < guiTop+57+18) {
            return false;
        }
        return true;
    }

    public boolean keyboardInput() {
        return Keyboard.getEventKey() != Keyboard.KEY_ESCAPE &&
                (!NotEnoughUpdates.INSTANCE.config.slotLocking.enableSlotLocking ||
                Keyboard.getEventKey() != NotEnoughUpdates.INSTANCE.config.slotLocking.slotLockKey);
    }
}
