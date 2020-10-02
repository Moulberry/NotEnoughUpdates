package io.github.moulberry.notenoughupdates;

import io.github.moulberry.notenoughupdates.util.TexLoc;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class BetterContainers {

    private static final ResourceLocation DYNAMIC_54_BASE = new ResourceLocation("notenoughupdates:dynamic_54/style1/dynamic_54.png");
    private static final ResourceLocation DYNAMIC_54_SLOT = new ResourceLocation("notenoughupdates:dynamic_54/style1/dynamic_54_slot_ctm.png");
    private static final ResourceLocation DYNAMIC_54_BUTTON = new ResourceLocation("notenoughupdates:dynamic_54/style1/dynamic_54_button_ctm.png");
    private static final ResourceLocation rl = new ResourceLocation("notenoughupdates:dynamic_chest_inventory.png");
    private static boolean loaded = false;
    private static DynamicTexture texture = null;

    private static int lastClickedSlot = 0;
    private static int clickedSlot = 0;
    private static long clickedSlotMillis = 0;

    public static void clickSlot(int slot) {
        clickedSlot = slot;
        clickedSlotMillis = System.currentTimeMillis();
    }

    public static int getClickedSlot() {
        if(clickedSlotMillis - System.currentTimeMillis() < 200) {
            return clickedSlot;
        }
        return -1;
    }

    public static void bindHook(TextureManager textureManager, ResourceLocation location) {
        if(isChestOpen()) {
            if(lastClickedSlot != getClickedSlot() || (texture == null && !loaded)) {
                lastClickedSlot = getClickedSlot();
                generateTex(location);
            }
            if(isOverriding()) {
                textureManager.loadTexture(rl, texture);
                textureManager.bindTexture(rl);
                return;
            }
        }
        textureManager.bindTexture(location);
    }

    public static boolean isOverriding() {
        return isChestOpen() && loaded && texture != null && !Keyboard.isKeyDown(Keyboard.KEY_B);
    }

    public static boolean isBlankStack(ItemStack stack) {
        return stack != null && stack.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane) &&
                stack.getDisplayName() != null && stack.getDisplayName().trim().isEmpty();
    }

    public static boolean isButtonStack(ItemStack stack) {
        return stack != null && stack.getItem() != Item.getItemFromBlock(Blocks.stained_glass_pane)
                && NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(stack) == null;
    }

    private static void generateTex(ResourceLocation location) {
        if(!hasItem()) return;
        loaded = true;
        Container container = ((GuiChest)Minecraft.getMinecraft().currentScreen).inventorySlots;
        if(hasNullPane() && container instanceof ContainerChest) {
            try {
                BufferedImage bufferedImageBase = ImageIO.read(Minecraft.getMinecraft().getResourceManager().getResource(DYNAMIC_54_BASE).getInputStream());
                try {
                    int backgroundStyle = NotEnoughUpdates.INSTANCE.manager.config.dynamicMenuBackgroundStyle.value.intValue();
                    backgroundStyle = Math.max(1, Math.min(10, backgroundStyle));
                    bufferedImageBase = ImageIO.read(Minecraft.getMinecraft().getResourceManager().getResource(
                            new ResourceLocation("notenoughupdates:dynamic_54/style"+ backgroundStyle+"/dynamic_54.png")).getInputStream());
                } catch(Exception e) {}
                BufferedImage bufferedImageSlot = ImageIO.read(Minecraft.getMinecraft().getResourceManager().getResource(DYNAMIC_54_SLOT).getInputStream());
                try {
                    int buttonStyle = NotEnoughUpdates.INSTANCE.manager.config.dynamicMenuButtonStyle.value.intValue();
                    buttonStyle = Math.max(1, Math.min(10, buttonStyle));
                    bufferedImageSlot = ImageIO.read(Minecraft.getMinecraft().getResourceManager().getResource(
                            new ResourceLocation("notenoughupdates:dynamic_54/style"+buttonStyle+"/dynamic_54_slot_ctm.png")).getInputStream());
                } catch(Exception e) {}
                BufferedImage bufferedImageButton = ImageIO.read(Minecraft.getMinecraft().getResourceManager().getResource(DYNAMIC_54_BUTTON).getInputStream());
                try {
                    int buttonStyle = NotEnoughUpdates.INSTANCE.manager.config.dynamicMenuButtonStyle.value.intValue();
                    buttonStyle = Math.max(1, Math.min(10, buttonStyle));
                    bufferedImageButton = ImageIO.read(Minecraft.getMinecraft().getResourceManager().getResource(
                            new ResourceLocation("notenoughupdates:dynamic_54/style"+buttonStyle+"/dynamic_54_button_ctm.png")).getInputStream());
                } catch(Exception e) {}

                int horzTexMult = bufferedImageBase.getWidth()/256;
                int vertTexMult = bufferedImageBase.getWidth()/256;
                BufferedImage bufferedImageNew = new BufferedImage(
                        bufferedImageBase.getColorModel(),
                        bufferedImageBase.copyData(null),
                        bufferedImageBase.isAlphaPremultiplied(),
                        null);
                IInventory lower = ((ContainerChest) container).getLowerChestInventory();
                int size = lower.getSizeInventory();
                boolean[][] slots = new boolean[9][size/9];
                boolean[][] buttons = new boolean[9][size/9];
                for (int index = 0; index < size; index++) {
                    ItemStack stack = lower.getStackInSlot(index);
                    buttons[index%9][index/9] = stack != null && stack.getItem() != Item.getItemFromBlock(Blocks.stained_glass_pane)
                            && NotEnoughUpdates.INSTANCE.manager.getInternalNameForItem(stack) == null;

                    if(buttons[index%9][index/9] && getClickedSlot() == index) {
                        buttons[index%9][index/9] = false;
                        slots[index%9][index/9] = true;
                    } else {
                        slots[index%9][index/9] = !isBlankStack(stack) && !buttons[index%9][index/9];
                    }
                }
                for (int index = 0; index < size; index++) {
                    int xi = index%9;
                    int yi = index/9;
                    if(slots[xi][yi] || buttons[xi][yi]) {
                        int x = 7*horzTexMult + xi*18*horzTexMult;
                        int y = 17*vertTexMult + yi*18*vertTexMult;

                        if(buttons[xi][yi]) {
                            boolean up = yi > 0 && buttons[xi][yi-1];
                            boolean right = xi < buttons.length-1 && buttons[xi+1][yi];
                            boolean down = yi < buttons[xi].length-1 && buttons[xi][yi+1];
                            boolean left = xi > 0 && buttons[xi-1][yi];

                            boolean upleft = yi > 0 && xi > 0 && buttons[xi-1][yi-1];
                            boolean upright = yi > 0 && xi < buttons.length-1 && buttons[xi+1][yi-1];
                            boolean downright = xi < buttons.length-1 && yi < buttons[xi+1].length-1 && buttons[xi+1][yi+1];
                            boolean downleft = xi > 0 && yi < buttons[xi-1].length-1 && buttons[xi-1][yi+1];

                            int ctmIndex = getCTMIndex(up, right, down, left, upleft, upright, downright, downleft);
                            int[] rgbs = bufferedImageButton.getRGB((ctmIndex%12)*19*horzTexMult, (ctmIndex/12)*19*vertTexMult,
                                    18*horzTexMult, 18*vertTexMult, null, 0, 18*vertTexMult);
                            bufferedImageNew.setRGB(x, y, 18*horzTexMult, 18*vertTexMult, rgbs, 0, 18*vertTexMult);
                        } else {
                            boolean up = yi > 0 && slots[xi][yi-1];
                            boolean right = xi < slots.length-1 && slots[xi+1][yi];
                            boolean down = yi < slots[xi].length-1 && slots[xi][yi+1];
                            boolean left = xi > 0 && slots[xi-1][yi];

                            boolean upleft = yi > 0 && xi > 0 && slots[xi-1][yi-1];
                            boolean upright = yi > 0 && xi < slots.length-1 && slots[xi+1][yi-1];
                            boolean downright = xi < slots.length-1 && yi < slots[xi+1].length-1 && slots[xi+1][yi+1];
                            boolean downleft = xi > 0 && yi < slots[xi-1].length-1 && slots[xi-1][yi+1];

                            int ctmIndex = getCTMIndex(up, right, down, left, upleft, upright, downright, downleft);
                            int[] rgbs = bufferedImageSlot.getRGB((ctmIndex%12)*19*horzTexMult, (ctmIndex/12)*19*vertTexMult,
                                    18*horzTexMult, 18*vertTexMult, null, 0, 18*vertTexMult);
                            bufferedImageNew.setRGB(x, y, 18*horzTexMult, 18*vertTexMult, rgbs, 0, 18*vertTexMult);
                        }
                    }
                }
                texture = new DynamicTexture(bufferedImageNew);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void reset() {
        texture = null;
        loaded = false;
        clickedSlot = -1;
        clickedSlotMillis = 0;
    }

    private static boolean isChestOpen() {
        return Minecraft.getMinecraft().currentScreen instanceof GuiChest &&
                NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() &&
                (NotEnoughUpdates.INSTANCE.manager.config.dynamicMenuBackgroundStyle.value >= 1 &&
                        NotEnoughUpdates.INSTANCE.manager.config.dynamicMenuButtonStyle.value >= 1);
    }

    private static boolean hasItem() {
        if(!isChestOpen()) return false;
        Container container = ((GuiChest)Minecraft.getMinecraft().currentScreen).inventorySlots;
        if(container instanceof ContainerChest) {
            IInventory lower = ((ContainerChest)container).getLowerChestInventory();
            int size = lower.getSizeInventory();
            for(int index=0; index<size; index++) {
                if(lower.getStackInSlot(index) != null) return true;
            }
        }
        return false;
    }

    private static boolean hasNullPane() {
        if(!isChestOpen()) return false;
        Container container = ((GuiChest)Minecraft.getMinecraft().currentScreen).inventorySlots;
        if(container instanceof ContainerChest) {
            IInventory lower = ((ContainerChest)container).getLowerChestInventory();
            int size = lower.getSizeInventory();
            for(int index=0; index<size; index++) {
                if(isBlankStack(lower.getStackInSlot(index))) return true;
            }
        }
        return false;
    }

    private static int getCTMIndex(boolean up, boolean right, boolean down, boolean left, boolean upleft, boolean upright, boolean downright, boolean downleft) {
        if(up && right && down && left) {
            if(upleft && upright && downright && downleft) {
                return 26;
            } else if(upleft && upright && downright && !downleft) {
                return 33;
            } else if(upleft && upright && !downright && downleft) {
                return 32;
            } else if(upleft && upright && !downright && !downleft) {
                return 11;
            } else if(upleft && !upright && downright && downleft) {
                return 44;
            } else if(upleft && !upright && downright && !downleft) {
                return 35;
            } else if(upleft && !upright && !downright && downleft) {
                return 10;
            } else if(upleft && !upright && !downright && !downleft) {
                return 20;
            } else if(!upleft && upright && downright && downleft) {
                return 45;
            } else if(!upleft && upright && downright && !downleft) {
                return 23;
            } else if(!upleft && upright && !downright && downleft) {
                return 34;
            } else if(!upleft && upright && !downright && !downleft) {
                return 8;
            } else if(!upleft && !upright && downright && downleft) {
                return 22;
            } else if(!upleft && !upright && downright && !downleft) {
                return 9;
            } else if(!upleft && !upright && !downright && downleft) {
                return 21;
            } else {
                return 46;
            }
        } else if(up && right && down && !left) {
            if(!upright && !downright) {
                return 6;
            } else if(!upright) {
                return 28;
            } else if(!downright) {
                return 30;
            } else {
                return 25;
            }
        } else if(up && right && !down && left) {
            if(!upleft && !upright) {
                return 18;
            } else if(!upleft) {
                return 40;
            } else if(!upright) {
                return 42;
            } else {
                return 38;
            }
        } else if(up && right && !down && !left) {
            if(!upright) {
                return 16;
            } else {
                return 37;
            }
        } else if(up && !right && down && left) {
            if(!upleft && !downleft) {
                return 19;
            } else if(!upleft) {
                return 43;
            } else if(!downleft) {
                return 41;
            } else {
                return 27;
            }
        } else if(up && !right && down && !left) {
            return 24;
        } else if(up && !right && !down && left) {
            if(!upleft) {
                return 17;
            } else {
                return 39;
            }
        } else if(up && !right && !down && !left) {
            return 36;
        } else if(!up && right && down && left) {
            if(!downleft && !downright) {
                return 7;
            } else if(!downleft) {
                return 31;
            } else if(!downright) {
                return 29;
            } else {
                return 14;
            }
        } else if(!up && right && down && !left) {
            if(!downright) {
                return 4;
            } else {
                return 13;
            }
        } else if(!up && right && !down && left) {
            return 2;
        } else if(!up && right && !down && !left) {
            return 1;
        } else if(!up && !right && down && left) {
            if(!downleft) {
                return 5;
            } else {
                return 15;
            }
        } else if(!up && !right && down && !left) {
            return 12;
        } else if(!up && !right && !down && left) {
            return 3;
        } else {
            return 0;
        }
    }

}
