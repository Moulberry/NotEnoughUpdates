package io.github.moulberry.notenoughupdates.util;

import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class SBAIntegration {

    private static boolean hasSBA = true;
    private static Class<?> skyblockAddonsClass = null;
    private static Method skyblockAddons_getInstance = null;
    private static Method skyblockAddons_getUtils = null;
    private static Class<?> backpackManagerClass = null;
    private static Method backpackManager_getFromItem = null;
    private static Class<?> backpackClass = null;
    private static Method backpackClass_setX= null;
    private static Method backpackClass_setY = null;
    private static Class<?> utilsClass = null;
    private static Method utils_setBackpackToPreview = null;
    public static boolean setActiveBackpack(ItemStack stack, int mouseX, int mouseY) {
        if(!hasSBA) return false;
        try {
            if(skyblockAddonsClass == null) {
                skyblockAddonsClass = Class.forName("codes.biscuit.skyblockaddons.SkyblockAddons");
            }
            if(skyblockAddons_getInstance == null) {
                skyblockAddons_getInstance = skyblockAddonsClass.getDeclaredMethod("getInstance");
            }
            if(skyblockAddons_getUtils == null) {
                skyblockAddons_getUtils = skyblockAddonsClass.getDeclaredMethod("getUtils");
            }
            if(backpackManagerClass == null) {
                backpackManagerClass = Class.forName("codes.biscuit.skyblockaddons.features.backpacks.BackpackManager");
            }
            if(backpackManager_getFromItem == null) {
                backpackManager_getFromItem = backpackManagerClass.getDeclaredMethod("getFromItem", ItemStack.class);
            }
            if(backpackClass == null) {
                try { backpackClass = Class.forName("codes.biscuit.skyblockaddons.features.backpacks.Backpack"); } catch(Exception ignored){}
            }
            if(backpackClass == null) {
                backpackClass = Class.forName("codes.biscuit.skyblockaddons.features.backpacks.ContainerPreview");
            }
            if(backpackClass_setX == null) {
                backpackClass_setX = backpackClass.getDeclaredMethod("setX", int.class);
            }
            if(backpackClass_setY == null) {
                backpackClass_setY = backpackClass.getDeclaredMethod("setY", int.class);
            }
            if(utilsClass == null) {
                utilsClass = Class.forName("codes.biscuit.skyblockaddons.utils.Utils");
            }
            if(utils_setBackpackToPreview == null) {
                try { utils_setBackpackToPreview = utilsClass.getDeclaredMethod("setBackpackToPreview", backpackClass); } catch(Exception ignored){}
            }
            if(utils_setBackpackToPreview == null) {
                utils_setBackpackToPreview = utilsClass.getDeclaredMethod("setContainerPreviewToRender", backpackClass);;
            }
        } catch(Exception e) {
            e.printStackTrace();
            hasSBA = false;
            return false;
        }
        try {
            Object skyblockAddons = skyblockAddons_getInstance.invoke(null);
            Object utils = skyblockAddons_getUtils.invoke(skyblockAddons);
            if(stack == null) {
                utils_setBackpackToPreview.invoke(utils, (Object) null);
            } else {
                Object backpack = backpackManager_getFromItem.invoke(null, stack);
                backpackClass_setX.invoke(backpack, mouseX);
                backpackClass_setY.invoke(backpack, mouseY);
                utils_setBackpackToPreview.invoke(utils, backpack);
            }
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static Field guiContainerHook_freezeBackpack = null;
    public static boolean isFreezeBackpack() {
        if(!hasSBA) return false;
        try {
            if(guiContainerHookClass == null) {
                guiContainerHookClass = Class.forName("codes.biscuit.skyblockaddons.asm.hooks.GuiContainerHook");
            }
            if(guiContainerHook_freezeBackpack == null) {
                guiContainerHook_freezeBackpack = guiContainerHookClass.getDeclaredField("freezeBackpack");
                guiContainerHook_freezeBackpack.setAccessible(true);
            }
        } catch(Exception e) {
            e.printStackTrace();
            hasSBA = false;
            return false;
        }
        try {
            return (boolean) guiContainerHook_freezeBackpack.get(null);
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean setFreezeBackpack(boolean freezeBackpack) {
        if(!hasSBA) return false;
        try {
            if(guiContainerHookClass == null) {
                guiContainerHookClass = Class.forName("codes.biscuit.skyblockaddons.asm.hooks.GuiContainerHook");
            }
            if(guiContainerHook_freezeBackpack == null) {
                guiContainerHook_freezeBackpack = guiContainerHookClass.getDeclaredField("freezeBackpack");
                guiContainerHook_freezeBackpack.setAccessible(true);
            }
        } catch(Exception e) {
            e.printStackTrace();
            hasSBA = false;
            return false;
        }
        try {
            guiContainerHook_freezeBackpack.set(null, freezeBackpack);
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Method guiContainerHook_keyTyped = null;
    private static Method skyblockAddons_getFreezeBackpackKey = null;
    public static boolean keyTyped(int keyCode) {
        if(!hasSBA) return false;
        try {
            if(skyblockAddonsClass == null) {
                skyblockAddonsClass = Class.forName("codes.biscuit.skyblockaddons.SkyblockAddons");
            }
            if(skyblockAddons_getInstance == null) {
                skyblockAddons_getInstance = skyblockAddonsClass.getDeclaredMethod("getInstance");
            }
            if(skyblockAddons_getFreezeBackpackKey == null) {
                skyblockAddons_getFreezeBackpackKey = skyblockAddonsClass.getDeclaredMethod("getFreezeBackpackKey");
            }
            if(guiContainerHookClass == null) {
                guiContainerHookClass = Class.forName("codes.biscuit.skyblockaddons.asm.hooks.GuiContainerHook");
            }
            if(guiContainerHook_keyTyped == null) {
                guiContainerHook_keyTyped = guiContainerHookClass.getDeclaredMethod("keyTyped", int.class);
            }
        } catch(Exception e) {
            e.printStackTrace();
            hasSBA = false;
            return false;
        }
        try {
            Object skyblockAddons = skyblockAddons_getInstance.invoke(null);
            if(!isFreezeBackpack() && ((KeyBinding)skyblockAddons_getFreezeBackpackKey.invoke(skyblockAddons)).getKeyCode() == keyCode) {
                setFreezeBackpack(true);
            } else {
                guiContainerHook_keyTyped.invoke(null, keyCode);
            }
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static Class<?> guiContainerHookClass = null;
    private static Method guiContainerHook_drawBackpacks = null;
    public static boolean renderActiveBackpack(int mouseX, int mouseY, FontRenderer fontRendererObj) {
        if(!hasSBA) return false;
        try {
            if(guiContainerHookClass == null) {
                guiContainerHookClass = Class.forName("codes.biscuit.skyblockaddons.asm.hooks.GuiContainerHook");
            }
            if(guiContainerHook_drawBackpacks == null) {
                guiContainerHook_drawBackpacks = guiContainerHookClass.getDeclaredMethod("drawBackpacks",
                        GuiContainer.class, int.class, int.class, FontRenderer.class);
            }
        } catch(Exception e) {
            e.printStackTrace();
            hasSBA = false;
            return false;
        }
        try {
            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            int width = scaledResolution.getScaledWidth();
            int height = scaledResolution.getScaledHeight();

            if(Minecraft.getMinecraft().currentScreen instanceof GuiContainer) {
                guiContainerHook_drawBackpacks.invoke(null, Minecraft.getMinecraft().currentScreen, mouseX, mouseY, fontRendererObj);
            } else {
                GuiContainer container = new GuiContainer(null) {
                    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
                    }
                };
                container.setWorldAndResolution(Minecraft.getMinecraft(), width, height);

                guiContainerHook_drawBackpacks.invoke(null, container, mouseX, mouseY, fontRendererObj);
            }
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
