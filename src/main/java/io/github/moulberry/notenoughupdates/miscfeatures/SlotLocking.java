package io.github.moulberry.notenoughupdates.miscfeatures;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.config.KeybindHelper;
import io.github.moulberry.notenoughupdates.core.util.render.RenderUtils;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.audio.SoundCategory;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.function.Consumer;

public class SlotLocking {

    private static final SlotLocking INSTANCE = new SlotLocking();


    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final LockedSlot DEFAULT_LOCKED_SLOT = new LockedSlot();
    private final ResourceLocation LOCK = new ResourceLocation("notenoughupdates:slotlocking/lock.png");
    private final ResourceLocation BOUND = new ResourceLocation("notenoughupdates:slotlocking/bound.png");

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

    public void loadConfig(File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            config = GSON.fromJson(reader, SlotLockingConfig.class);
        } catch (Exception ignored) {
        }
        if (config == null) {
            config = new SlotLockingConfig();
        }
    }

    public void saveConfig(File file) {
        try {
            file.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write(GSON.toJson(config));
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    private LockedSlot[] getDataForProfile() {
        if (!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() || !NotEnoughUpdates.INSTANCE.config.slotLocking.enableSlotLocking)
            return null;

        String profileName = SBInfo.getInstance().currentProfile;
        if (profileName == null) profileName = "generic";

        SlotLockProfile profile = config.profileData.computeIfAbsent(profileName,
                k -> new SlotLockProfile());

        if (profile.currentProfile < 0) profile.currentProfile = 0;
        if (profile.currentProfile >= 9) profile.currentProfile = 8;

        if (profile.slotLockData[profile.currentProfile] == null) {
            profile.slotLockData[profile.currentProfile] = new SlotLockData();
        }

        return profile.slotLockData[profile.currentProfile].lockedSlots;
    }

    private LockedSlot getLockedSlot(LockedSlot[] lockedSlots, int index) {
        if (lockedSlots == null) {
            return DEFAULT_LOCKED_SLOT;
        }

        LockedSlot slot = lockedSlots[index];

        if (slot == null) {
            return DEFAULT_LOCKED_SLOT;
        }

        return slot;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void keyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() || !NotEnoughUpdates.INSTANCE.config.slotLocking.enableSlotLocking) {
            return;
        }
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
            return;
        }
        GuiContainer container = (GuiContainer) Minecraft.getMinecraft().currentScreen;

        int key = NotEnoughUpdates.INSTANCE.config.slotLocking.slotLockKey;
        if (!lockKeyHeld && KeybindHelper.isKeyPressed(key) && !Keyboard.isRepeatEvent()) {
            final ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
            final int scaledWidth = scaledresolution.getScaledWidth();
            final int scaledHeight = scaledresolution.getScaledHeight();
            int mouseX = Mouse.getX() * scaledWidth / Minecraft.getMinecraft().displayWidth;
            int mouseY = scaledHeight - Mouse.getY() * scaledHeight / Minecraft.getMinecraft().displayHeight - 1;

            Slot slot = container.getSlotAtPosition(mouseX, mouseY);
            if (slot != null && slot.getSlotIndex() != 8 && slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) {
                int slotNum = slot.getSlotIndex();
                if (slotNum >= 0 && slotNum <= 39) {
                    boolean isHotbar = slotNum < 9;
                    boolean isInventory = !isHotbar && slotNum < 36;
                    boolean isArmor = !isHotbar && !isInventory;

                    if (isInventory || isArmor) {
                        pairingSlot = slot;
                    } else {
                        pairingSlot = null;
                    }

                    LockedSlot[] lockedSlots = getDataForProfile();

                    if (lockedSlots != null) {
                        if (lockedSlots[slotNum] == null) {
                            lockedSlots[slotNum] = new LockedSlot();
                        }
                        lockedSlots[slotNum].locked = !lockedSlots[slotNum].locked;
                        lockedSlots[slotNum].boundTo = -1;

                        if (NotEnoughUpdates.INSTANCE.config.slotLocking.slotLockSound) {
                            float vol = NotEnoughUpdates.INSTANCE.config.slotLocking.slotLockSoundVol / 100f;
                            if (vol > 0) {
                                if (vol > 1) vol = 1;
                                final float volF = vol;
                                final boolean locked = lockedSlots[slotNum].locked;

                                ISound sound = new PositionedSound(new ResourceLocation("random.orb")) {{
                                    volume = volF;
                                    pitch = locked ? 0.943f : 0.1f;
                                    repeat = false;
                                    repeatDelay = 0;
                                    attenuationType = ISound.AttenuationType.NONE;
                                }};

                                float oldLevel = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.PLAYERS);
                                Minecraft.getMinecraft().gameSettings.setSoundLevel(SoundCategory.PLAYERS, 1);
                                Minecraft.getMinecraft().getSoundHandler().playSound(sound);
                                Minecraft.getMinecraft().gameSettings.setSoundLevel(SoundCategory.PLAYERS, oldLevel);
                            }
                        }

                        if (isHotbar && lockedSlots[slotNum].locked) {
                            for (int i = 9; i <= 39; i++) {
                                if (lockedSlots[i] != null && lockedSlots[i].boundTo == slotNum) {
                                    lockedSlots[i].boundTo = -1;
                                }
                            }
                        }
                    }
                }
            }
        }
        lockKeyHeld = KeybindHelper.isKeyDown(key);
        if (!lockKeyHeld) {
            pairingSlot = null;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void mouseEvent(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() || !NotEnoughUpdates.INSTANCE.config.slotLocking.enableSlotLocking) {
            return;
        }
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
            return;
        }
        GuiContainer container = (GuiContainer) Minecraft.getMinecraft().currentScreen;

        if (NotEnoughUpdates.INSTANCE.config.slotLocking.enableSlotBinding && lockKeyHeld && pairingSlot != null) {
            final ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
            final int scaledWidth = scaledresolution.getScaledWidth();
            final int scaledHeight = scaledresolution.getScaledHeight();
            int mouseX = Mouse.getX() * scaledWidth / Minecraft.getMinecraft().displayWidth;
            int mouseY = scaledHeight - Mouse.getY() * scaledHeight / Minecraft.getMinecraft().displayHeight - 1;

            Slot slot = container.getSlotAtPosition(mouseX, mouseY);
            if (slot != null && slot.getSlotIndex() != 8 && slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) {
                int slotNum = slot.getSlotIndex();
                if (slotNum >= 0 && slotNum <= 39) {

                    boolean isHotbar = slotNum < 9;
                    boolean isInventory = !isHotbar && slotNum < 36;
                    boolean isArmor = !isHotbar && !isInventory;

                    int pairingNum = pairingSlot.getSlotIndex();
                    if (isHotbar && slotNum != pairingNum) {
                        LockedSlot[] lockedSlots = getDataForProfile();
                        if (lockedSlots != null) {
                            if (lockedSlots[slotNum] == null) {
                                lockedSlots[slotNum] = new LockedSlot();
                            }
                            if (!lockedSlots[slotNum].locked) {
                                if (lockedSlots[pairingNum] == null) {
                                    lockedSlots[pairingNum] = new LockedSlot();
                                }

                                lockedSlots[pairingNum].boundTo = slotNum;
                                lockedSlots[pairingNum].locked = false;

                                lockedSlots[slotNum].boundTo = pairingNum;
                            }
                        }
                    }
                }
            } else {
                int pairingNum = pairingSlot.getSlotIndex();
                LockedSlot[] lockedSlots = getDataForProfile();
                if (lockedSlots != null && lockedSlots[pairingNum] != null) {
                    if (lockedSlots[pairingNum].boundTo >= 0) {
                        lockedSlots[lockedSlots[pairingNum].boundTo] = null;
                    }
                    lockedSlots[pairingNum] = null;
                }
            }
        }
    }

    public void toggleLock(int lockIndex) {
        LockedSlot[] lockedSlots = getDataForProfile();

        if (lockedSlots != null) {
            if (lockedSlots[lockIndex] == null) {
                lockedSlots[lockIndex] = new LockedSlot();
            }
            lockedSlots[lockIndex].locked = !lockedSlots[lockIndex].locked;
            lockedSlots[lockIndex].boundTo = -1;

            if (NotEnoughUpdates.INSTANCE.config.slotLocking.slotLockSound) {
                float vol = NotEnoughUpdates.INSTANCE.config.slotLocking.slotLockSoundVol / 100f;
                if (vol > 0) {
                    if (vol > 1) vol = 1;
                    final float volF = vol;
                    final boolean locked = lockedSlots[lockIndex].locked;

                    ISound sound = new PositionedSound(new ResourceLocation("random.orb")) {{
                        volume = volF;
                        pitch = locked ? 0.943f : 0.1f;
                        repeat = false;
                        repeatDelay = 0;
                        attenuationType = ISound.AttenuationType.NONE;
                    }};

                    float oldLevel = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.PLAYERS);
                    Minecraft.getMinecraft().gameSettings.setSoundLevel(SoundCategory.PLAYERS, 1);
                    Minecraft.getMinecraft().getSoundHandler().playSound(sound);
                    Minecraft.getMinecraft().gameSettings.setSoundLevel(SoundCategory.PLAYERS, oldLevel);
                }
            }

            if (lockIndex < 9 && lockedSlots[lockIndex].locked) {
                for (int i = 9; i <= 39; i++) {
                    if (lockedSlots[i] != null && lockedSlots[i].boundTo == lockIndex) {
                        lockedSlots[i].boundTo = -1;
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void drawScreenEvent(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (NotEnoughUpdates.INSTANCE.config.slotLocking.enableSlotBinding && !event.isCanceled() && pairingSlot != null && lockKeyHeld) {
            LockedSlot[] lockedSlots = getDataForProfile();
            LockedSlot lockedSlot = getLockedSlot(lockedSlots, pairingSlot.getSlotIndex());
            if (lockedSlot.boundTo >= 0 && lockedSlot.boundTo < 8) {
                return;
            }

            if (!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
                return;
            }
            GuiContainer container = (GuiContainer) Minecraft.getMinecraft().currentScreen;

            int x1 = container.guiLeft + pairingSlot.xDisplayPosition + 8;
            int y1 = container.guiTop + pairingSlot.yDisplayPosition + 8;
            int x2 = event.mouseX;
            int y2 = event.mouseY;

            if (x2 > x1 - 8 && x2 < x1 + 8 &&
                    y2 > y1 - 8 && y2 < y1 + 8) {
                return;
            }

            drawLinkArrow(x1, y1, x2, y2);
        }
    }

    private void drawLinkArrow(int x1, int y1, int x2, int y2) {
        GlStateManager.color(0x33 / 255f, 0xee / 255f, 0xdd / 255f, 1f);
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

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        int lines = 6;
        for (int i = 0; i < lines; i++) {
            worldrenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
            worldrenderer.pos(x1 - side.x + side.x * i / lines, y1 - side.y + side.y * i / lines, 0.0D).endVertex();
            worldrenderer.pos(x2 - side.x + side.x * i / lines, y2 - side.y + side.y * i / lines, 0.0D).endVertex();
            tessellator.draw();
        }
    }

    public void onWindowClick(Slot slotIn, int slotId, int clickedButton, int clickType, Consumer<Triple<Integer, Integer, Integer>> consumer) {
        LockedSlot locked = getLockedSlot(slotIn);
        if (locked == null) {
            return;
        } else if (locked.locked || (clickType == 2 && SlotLocking.getInstance().isSlotIndexLocked(clickedButton))) {
            consumer.accept(null);
        } else if (NotEnoughUpdates.INSTANCE.config.slotLocking.enableSlotBinding && clickType == 1) {
            GuiContainer container = (GuiContainer) Minecraft.getMinecraft().currentScreen;
            Slot boundSlot = container.inventorySlots.getSlotFromInventory(Minecraft.getMinecraft().thePlayer.inventory, locked.boundTo);

            if (boundSlot == null) {
                return;
            }

            LockedSlot boundLocked = getLockedSlot(boundSlot);

            int id = slotIn.getSlotIndex();
            if (id >= 9 && locked.boundTo >= 0 && locked.boundTo < 8) {
                if (!boundLocked.locked) {
                    consumer.accept(Triple.of(slotId, locked.boundTo, 2));
                    if (boundLocked == DEFAULT_LOCKED_SLOT) {
                        LockedSlot[] lockedSlots = getDataForProfile();
                        lockedSlots[locked.boundTo] = new LockedSlot();
                        lockedSlots[locked.boundTo].boundTo = id;
                    } else {
                        boundLocked.boundTo = id;
                    }
                }
            } else if (id >= 0 && id < 8 && locked.boundTo >= 9 && locked.boundTo <= 39) {
                if (boundLocked.locked || boundLocked.boundTo != id) {
                    locked.boundTo = -1;
                } else {
                    int boundTo = boundSlot.slotNumber;
                    consumer.accept(Triple.of(boundTo, id, 2));
                }
            }
        } else if (NotEnoughUpdates.INSTANCE.config.slotLocking.enableSlotBinding && locked.boundTo != -1 && NotEnoughUpdates.INSTANCE.config.slotLocking.bindingAlsoLocks) {
            consumer.accept(null);
        }
    }

    public void drawSlot(Slot slot) {
        LockedSlot locked = getLockedSlot(slot);
        if (locked != null) {
            if (locked.locked) {
                GlStateManager.translate(0, 0, 400);
                Minecraft.getMinecraft().getTextureManager().bindTexture(LOCK);
                GlStateManager.color(1, 1, 1, 0.5f);
                GlStateManager.depthMask(false);
                RenderUtils.drawTexturedRect(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, 0, 1, 0, 1, GL11.GL_NEAREST);
                GlStateManager.depthMask(true);
                GlStateManager.enableBlend();
                GlStateManager.translate(0, 0, -400);
            } else if (NotEnoughUpdates.INSTANCE.config.slotLocking.enableSlotBinding && slot.canBeHovered() && locked.boundTo >= 0 && locked.boundTo <= 39) {
                if (!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
                    return;
                }
                GuiContainer container = (GuiContainer) Minecraft.getMinecraft().currentScreen;

                final ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
                final int scaledWidth = scaledresolution.getScaledWidth();
                final int scaledHeight = scaledresolution.getScaledHeight();
                int mouseX = Mouse.getX() * scaledWidth / Minecraft.getMinecraft().displayWidth;
                int mouseY = scaledHeight - Mouse.getY() * scaledHeight / Minecraft.getMinecraft().displayHeight - 1;

                Slot boundSlot = container.inventorySlots.getSlotFromInventory(Minecraft.getMinecraft().thePlayer.inventory, locked.boundTo);
                if (boundSlot == null) {
                    return;
                }

                boolean hoverOverSlot = container.isMouseOverSlot(slot, mouseX, mouseY);

                if (hoverOverSlot || slot.getSlotIndex() >= 9) {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(BOUND);
                    GlStateManager.color(1, 1, 1, 0.7f);
                    GlStateManager.depthMask(false);
                    RenderUtils.drawTexturedRect(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, 0, 1, 0, 1, GL11.GL_NEAREST);
                    GlStateManager.depthMask(true);
                    GlStateManager.enableBlend();

                    //Rerender Text over Top
                    if (slot.getStack() != null) {
                        Minecraft.getMinecraft().getRenderItem().renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRendererObj, slot.getStack(),
                                slot.xDisplayPosition, slot.yDisplayPosition, null);
                    }
                } else if (pairingSlot != null && lockKeyHeld && slot.getSlotIndex() < 8) {
                    int x1 = container.guiLeft + pairingSlot.xDisplayPosition;
                    int y1 = container.guiTop + pairingSlot.yDisplayPosition;

                    if (mouseX <= x1 || mouseX >= x1 + 16 ||
                            mouseY <= y1 || mouseY >= y1 + 16) {
                        Gui.drawRect(slot.xDisplayPosition, slot.yDisplayPosition, slot.xDisplayPosition + 16, slot.yDisplayPosition + 16, 0x80ffffff);
                    }
                }

                if (hoverOverSlot) {
                    LockedSlot boundLocked = getLockedSlot(boundSlot);
                    if (boundLocked == null || boundLocked.locked ||
                            (boundSlot.getSlotIndex() >= 9 && boundLocked.boundTo != slot.getSlotIndex())) {
                        locked.boundTo = -1;
                        return;
                    }

                    Minecraft.getMinecraft().getTextureManager().bindTexture(BOUND);
                    GlStateManager.color(1, 1, 1, 0.7f);
                    GlStateManager.depthMask(false);
                    RenderUtils.drawTexturedRect(boundSlot.xDisplayPosition, boundSlot.yDisplayPosition, 16, 16, 0, 1, 0, 1, GL11.GL_NEAREST);
                    GlStateManager.depthMask(true);
                    GlStateManager.enableBlend();

                    //Rerender Text over Top
                    if (boundSlot.getStack() != null) {
                        Minecraft.getMinecraft().getRenderItem().renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRendererObj, boundSlot.getStack(),
                                boundSlot.xDisplayPosition, boundSlot.yDisplayPosition, null);
                    }

                    int maxIter = 100;
                    float x1 = slot.xDisplayPosition + 8;
                    float y1 = slot.yDisplayPosition + 8;
                    float x2 = boundSlot.xDisplayPosition + 8;
                    float y2 = boundSlot.yDisplayPosition + 8;
                    Vector2f vec = new Vector2f(x2 - x1, y2 - y1);
                    vec.normalise(vec);

                    while (x1 > slot.xDisplayPosition && x1 < slot.xDisplayPosition + 16 &&
                            y1 > slot.yDisplayPosition && y1 < slot.yDisplayPosition + 16) {
                        if (maxIter-- < 50) break;
                        x1 += vec.x;
                        y1 += vec.y;
                    }
                    while (x2 > boundSlot.xDisplayPosition && x2 < boundSlot.xDisplayPosition + 16 &&
                            y2 > boundSlot.yDisplayPosition && y2 < boundSlot.yDisplayPosition + 16) {
                        if (maxIter-- < 0) break;
                        x2 -= vec.x;
                        y2 -= vec.y;
                    }

                    drawLinkArrow((int) x1, (int) y1, (int) x2, (int) y2);
                }
            } else if (NotEnoughUpdates.INSTANCE.config.slotLocking.enableSlotBinding && slot.getSlotIndex() < 8 && pairingSlot != null && lockKeyHeld) {
                if (!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
                    return;
                }
                GuiContainer container = (GuiContainer) Minecraft.getMinecraft().currentScreen;

                final ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
                final int scaledWidth = scaledresolution.getScaledWidth();
                final int scaledHeight = scaledresolution.getScaledHeight();
                int mouseX = Mouse.getX() * scaledWidth / Minecraft.getMinecraft().displayWidth;
                int mouseY = scaledHeight - Mouse.getY() * scaledHeight / Minecraft.getMinecraft().displayHeight - 1;

                int x1 = container.guiLeft + pairingSlot.xDisplayPosition;
                int y1 = container.guiTop + pairingSlot.yDisplayPosition;

                if (mouseX <= x1 || mouseX >= x1 + 16 ||
                        mouseY <= y1 || mouseY >= y1 + 16) {
                    Gui.drawRect(slot.xDisplayPosition, slot.yDisplayPosition, slot.xDisplayPosition + 16, slot.yDisplayPosition + 16, 0x80ffffff);
                }
            }
        }
    }

    public LockedSlot getLockedSlot(Slot slot) {
        if (!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() || !NotEnoughUpdates.INSTANCE.config.slotLocking.enableSlotLocking)
            return null;
        if (slot == null) {
            return null;
        }
        if (slot.inventory != Minecraft.getMinecraft().thePlayer.inventory) {
            return null;
        }
        int index = slot.getSlotIndex();
        if (index < 0 || index > 39) {
            return null;
        }
        return getLockedSlotIndex(index);
    }

    public LockedSlot getLockedSlotIndex(int index) {
        if (!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard() || !NotEnoughUpdates.INSTANCE.config.slotLocking.enableSlotLocking) {
            return null;
        }

        LockedSlot[] lockedSlots = getDataForProfile();
        if (lockedSlots == null) {
            return null;
        }
        return getLockedSlot(lockedSlots, index);
    }

    public boolean isSlotLocked(Slot slot) {
        LockedSlot locked = getLockedSlot(slot);
        return locked != null && (locked.locked || (NotEnoughUpdates.INSTANCE.config.slotLocking.bindingAlsoLocks && locked.boundTo != -1));
    }

    public boolean isSlotIndexLocked(int index) {
        LockedSlot locked = getLockedSlotIndex(index);

        return locked != null && (locked.locked || (NotEnoughUpdates.INSTANCE.config.slotLocking.bindingAlsoLocks && locked.boundTo != -1));
    }

}
