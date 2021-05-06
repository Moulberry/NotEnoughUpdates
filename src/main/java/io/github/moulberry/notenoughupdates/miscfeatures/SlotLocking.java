package io.github.moulberry.notenoughupdates.miscfeatures;

import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.config.KeybindHelper;
import io.github.moulberry.notenoughupdates.core.util.render.RenderUtils;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.util.HashMap;
import java.util.function.Consumer;

public class SlotLocking {

    private static final SlotLocking INSTANCE = new SlotLocking();

    private static final LockedSlot DEFAULT_LOCKED_SLOT = new LockedSlot();
    private final ResourceLocation LOCK = new ResourceLocation("notenoughupdates:slotlocking/lock.png");

    public static SlotLocking getInstance() {
        return INSTANCE;
    }

    public static class LockedSlot {
        public boolean locked = false;
        public int boundTo = -1;
    }

    public static class SlotLockData {
        public LockedSlot[] lockedSlots = new LockedSlot[40];
    }

    public static class SlotLockProfile {
        int currentProfile = 0;

        public SlotLockData[] slotLockData = new SlotLockData[9];
    }

    public static class SlotLockingConfig {
        public HashMap<String, SlotLockProfile> profileData = new HashMap<>();
    }

    private SlotLockingConfig config = new SlotLockingConfig();
    private boolean lockKeyHeld = false;
    private Slot pairingSlot = null;

    private LockedSlot[] getDataForProfile() {
        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return null;
        if(SBInfo.getInstance().currentProfile == null) return null;

        SlotLockProfile profile = config.profileData.computeIfAbsent(SBInfo.getInstance().currentProfile,
                k->new SlotLockProfile());

        if(profile.currentProfile < 0) profile.currentProfile = 0;
        if(profile.currentProfile >= 9) profile.currentProfile = 8;

        if(profile.slotLockData[profile.currentProfile] == null) {
            profile.slotLockData[profile.currentProfile] = new SlotLockData();
        }

        return profile.slotLockData[profile.currentProfile].lockedSlots;
    }

    private LockedSlot getLockedSlot(LockedSlot[] lockedSlots, int index) {
        if(lockedSlots == null) {
            return DEFAULT_LOCKED_SLOT;
        }

        LockedSlot slot = lockedSlots[index];

        if(slot == null) {
            return DEFAULT_LOCKED_SLOT;
        }

        return slot;
    }

    @SubscribeEvent
    public void keyboardInput(GuiScreenEvent.KeyboardInputEvent.Post event) {
        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) {
            return;
        }
        if(!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
            return;
        }
        GuiContainer container = (GuiContainer) Minecraft.getMinecraft().currentScreen;

        int key = Keyboard.KEY_L;
        if(!lockKeyHeld && KeybindHelper.isKeyPressed(key)) {
            final ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
            final int scaledWidth = scaledresolution.getScaledWidth();
            final int scaledHeight = scaledresolution.getScaledHeight();
            int mouseX = Mouse.getX() * scaledWidth / Minecraft.getMinecraft().displayWidth;
            int mouseY = scaledHeight - Mouse.getY() * scaledHeight / Minecraft.getMinecraft().displayHeight - 1;

            Slot slot = container.getSlotAtPosition(mouseX, mouseY);
            if(slot != null && slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) {
                int slotNum = slot.getSlotIndex();
                if(slotNum >= 0 && slotNum <= 39) {
                    boolean isHotbar = slotNum < 9;
                    boolean isInventory = !isHotbar && slotNum < 36;
                    boolean isArmor = !isHotbar && !isInventory;

                    if(isInventory || isArmor) {
                        pairingSlot = slot;
                    } else {
                        pairingSlot = null;
                    }

                    LockedSlot[] lockedSlots = getDataForProfile();

                    if(lockedSlots != null) {
                        if(lockedSlots[slotNum] == null) {
                            lockedSlots[slotNum] = new LockedSlot();
                            lockedSlots[slotNum].locked = true;
                            lockedSlots[slotNum].boundTo = -1;
                        } else {
                            lockedSlots[slotNum].locked = !lockedSlots[slotNum].locked;
                            lockedSlots[slotNum].boundTo = -1;
                        }
                    }
                }
            }
        }
        lockKeyHeld = KeybindHelper.isKeyDown(key);
        if(!lockKeyHeld) {
            pairingSlot = null;
        }
    }

    @SubscribeEvent
    public void mouseEvent(GuiScreenEvent.MouseInputEvent event) {
        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) {
            return;
        }
        if(!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
            return;
        }
        GuiContainer container = (GuiContainer) Minecraft.getMinecraft().currentScreen;

        if(lockKeyHeld && pairingSlot != null) {
            final ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
            final int scaledWidth = scaledresolution.getScaledWidth();
            final int scaledHeight = scaledresolution.getScaledHeight();
            int mouseX = Mouse.getX() * scaledWidth / Minecraft.getMinecraft().displayWidth;
            int mouseY = scaledHeight - Mouse.getY() * scaledHeight / Minecraft.getMinecraft().displayHeight - 1;

            Slot slot = container.getSlotAtPosition(mouseX, mouseY);
            if(slot != null && slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) {
                int slotNum = slot.getSlotIndex();
                if(slotNum >= 0 && slotNum <= 39) {
                    boolean isHotbar = slotNum < 9;
                    boolean isInventory = !isHotbar && slotNum < 36;
                    boolean isArmor = !isHotbar && !isInventory;

                    int pairingNum = pairingSlot.getSlotIndex();
                    if(isHotbar && slotNum != pairingNum) {
                        LockedSlot[] lockedSlots = getDataForProfile();
                        if(lockedSlots != null) {
                            if(lockedSlots[pairingNum] == null) {
                                lockedSlots[pairingNum] = new LockedSlot();
                            }
                            lockedSlots[pairingNum].boundTo = slotNum;
                            lockedSlots[pairingNum].locked = false;
                        }
                    }
                }
            }
        }
    }


    @SubscribeEvent(priority = EventPriority.LOW)
    public void drawScreenEvent(GuiScreenEvent.DrawScreenEvent.Post event) {
        if(!event.isCanceled() && pairingSlot != null && lockKeyHeld) {
            if(!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
                return;
            }
            GuiContainer container = (GuiContainer) Minecraft.getMinecraft().currentScreen;

            int x2 = event.mouseX;
            int y2 = event.mouseY;

            LockedSlot[] lockedSlots = getDataForProfile();
            LockedSlot lockedSlot = getLockedSlot(lockedSlots, pairingSlot.getSlotIndex());
            if(lockedSlot.boundTo >= 0 && lockedSlot.boundTo < 9) {
                Slot boundSlot = container.inventorySlots.getSlotFromInventory(Minecraft.getMinecraft().thePlayer.inventory, lockedSlot.boundTo);
                x2 = container.guiLeft+boundSlot.xDisplayPosition+8;
                y2 = container.guiTop+boundSlot.yDisplayPosition+8;
            }

            drawLinkArrow(container.guiLeft+pairingSlot.xDisplayPosition+8,
                    container.guiTop+pairingSlot.yDisplayPosition+8,
                    x2, y2);
        }
    }

    private void drawLinkArrow(int x1, int y1, int x2, int y2) {
        GlStateManager.color(0x33/255f, 0xff/255f, 0xcc/255f, 1f);
        GlStateManager.disableLighting();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        GlStateManager.translate(0, 0, 500);
        drawLine(x1, y1, x2, y2);
        GlStateManager.translate(0, 0, -500);

        GlStateManager.enableTexture2D();
    }

    private void drawLine(int x1, int y1, int x2, int y2) {
        Vector2f vec = new Vector2f(x2 - x1, y2 - y1);
        vec.normalise(vec);
        Vector2f side = new Vector2f(vec.y, -vec.x);

        GL11.glLineWidth(1f);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        /*worldrenderer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x1, y1, 0.0D).endVertex();
        worldrenderer.pos(x1+vec.x-side.x, y1+vec.y-side.y, 0.0D).endVertex();
        worldrenderer.pos(x1+vec.x+side.x, y1+vec.y+side.y, 0.0D).endVertex();
        worldrenderer.pos(x2-vec.x-side.x, y2-vec.y-side.y, 0.0D).endVertex();
        worldrenderer.pos(x2-vec.x+side.x, y2-vec.y+side.y, 0.0D).endVertex();
        worldrenderer.pos(x2, y2, 0.0D).endVertex();*/

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x1, y1, 0.0D).endVertex();
        worldrenderer.pos(x2, y2, 0.0D).endVertex();
        tessellator.draw();

        worldrenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x1-side.x/2f, y1-side.y/2f, 0.0D).endVertex();
        worldrenderer.pos(x2-side.x/2f, y2-side.y/2f, 0.0D).endVertex();
        tessellator.draw();

        worldrenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x1+side.x/2f, y1+side.y/2f, 0.0D).endVertex();
        worldrenderer.pos(x2+side.x/2f, y2+side.y/2f, 0.0D).endVertex();
        tessellator.draw();
    }

    public void onWindowClick(Slot slotIn, int slotId, int clickedButton, int clickType, Consumer<Triple<Integer, Integer, Integer>> consumer) {
        LockedSlot locked = getLockedSlot(slotIn);
        if(locked == null) {
            return;
        } else if(isSlotLocked(slotIn) || (clickType == 2 && SlotLocking.getInstance().isSlotIndexLocked(clickedButton))) {
            consumer.accept(null);
        } else if(clickType == 1 && locked.boundTo >= 0 && locked.boundTo < 9) {
            if(slotId > 9) {
                consumer.accept(Triple.of(slotId, locked.boundTo, 2));

                GuiContainer container = (GuiContainer) Minecraft.getMinecraft().currentScreen;
                Slot boundSlot = container.inventorySlots.getSlotFromInventory(Minecraft.getMinecraft().thePlayer.inventory, locked.boundTo);

                LockedSlot boundLocked = getLockedSlot(boundSlot);


            }

        }
    }

    public void drawSlot(Slot slot) {
        LockedSlot locked = getLockedSlot(slot);
        if(locked != null) {
            if(locked.locked) {
                GlStateManager.translate(0, 0, 400);
                Minecraft.getMinecraft().getTextureManager().bindTexture(LOCK);
                GlStateManager.color(1, 1, 1, 0.5f);
                GlStateManager.depthMask(false);
                RenderUtils.drawTexturedRect(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, 0, 1, 0, 1, GL11.GL_NEAREST);
                GlStateManager.depthMask(true);
                GlStateManager.enableBlend();
                GlStateManager.translate(0, 0, -400);
            } else if(slot.canBeHovered() && locked.boundTo >= 0 && locked.boundTo < 9) {
                if(!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
                    return;
                }
                GuiContainer container = (GuiContainer) Minecraft.getMinecraft().currentScreen;

                final ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
                final int scaledWidth = scaledresolution.getScaledWidth();
                final int scaledHeight = scaledresolution.getScaledHeight();
                int mouseX = Mouse.getX() * scaledWidth / Minecraft.getMinecraft().displayWidth;
                int mouseY = scaledHeight - Mouse.getY() * scaledHeight / Minecraft.getMinecraft().displayHeight - 1;

                if(container.isMouseOverSlot(slot, mouseX, mouseY)) {
                    Slot boundSlot = container.inventorySlots.getSlotFromInventory(Minecraft.getMinecraft().thePlayer.inventory, locked.boundTo);
                    int x2 = boundSlot.xDisplayPosition+8;
                    int y2 = boundSlot.yDisplayPosition+8;

                    drawLinkArrow(slot.xDisplayPosition+8,
                            slot.yDisplayPosition+8, x2, y2);
                }
            }
        }
    }

    public LockedSlot getLockedSlot(Slot slot) {
        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return null;
        if(slot == null) {
            return null;
        }
        if(slot.inventory != Minecraft.getMinecraft().thePlayer.inventory) {
            return null;
        }
        int index = slot.getSlotIndex();
        if(index < 0 || index > 39) {
            return null;
        }
        return getLockedSlotIndex(index);
    }

    public LockedSlot getLockedSlotIndex(int index) {
        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return null;

        LockedSlot[] lockedSlots = getDataForProfile();
        if(lockedSlots == null) return null;
        return getLockedSlot(lockedSlots, index);
    }

    public boolean isSlotLocked(Slot slot) {
        LockedSlot locked = getLockedSlot(slot);
        return locked != null && locked.locked;
    }

    public boolean isSlotIndexLocked(int index) {
        LockedSlot locked = getLockedSlotIndex(index);

        return locked != null && locked.locked;
    }

}
