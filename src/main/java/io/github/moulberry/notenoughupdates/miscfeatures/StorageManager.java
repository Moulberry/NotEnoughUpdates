package io.github.moulberry.notenoughupdates.miscfeatures;

import com.google.gson.*;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.miscgui.StorageOverlay;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class StorageManager {

    private static final StorageManager INSTANCE = new StorageManager();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
            .registerTypeAdapter(ItemStack.class, new ItemStackDeserilizer()).create();

    public static class ItemStackSerializer implements JsonSerializer<ItemStack> {
        @Override
        public JsonElement serialize(ItemStack src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            NBTTagCompound tag = src.serializeNBT();
            return nbtToJson(tag);
        }
    }

    private static final Pattern JSON_FIX_REGEX = Pattern.compile("\"([^,:]+)\":");

    public static class ItemStackDeserilizer implements JsonDeserializer<ItemStack> {
        @Override
        public ItemStack deserialize(JsonElement json, Type typeOfT,
                                     JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject object = json.getAsJsonObject();

                NBTTagCompound tag = JsonToNBT.getTagFromJson(JSON_FIX_REGEX.matcher(object.toString()).replaceAll("$1:"));

                Item item;
                if (tag.hasKey("id", 8)) {
                    item = Item.getByNameOrId(tag.getString("id"));
                } else {
                    item = Item.getItemById(tag.getShort("id"));
                }
                if (item == null) {
                    return null;
                }
                int stackSize = tag.getInteger("Count");
                int damage = tag.getInteger("Damage");

                ItemStack stack = new ItemStack(item, stackSize, damage);

                if (tag.hasKey("tag")) {
                    NBTTagCompound itemTag = tag.getCompoundTag("tag");
                    stack.setTagCompound(itemTag);
                }

                return stack;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private static JsonObject nbtToJson(NBTTagCompound NBTTagCompound) {
        return (JsonObject) loadJson(NBTTagCompound);
    }

    private static JsonElement loadJson(NBTBase tag) {
        if (tag instanceof NBTTagCompound) {
            NBTTagCompound compoundTag = (NBTTagCompound) tag;
            JsonObject jsonObject = new JsonObject();
            for (String key : compoundTag.getKeySet()) {
                jsonObject.add(key, loadJson(compoundTag.getTag(key)));
            }
            return jsonObject;
        } else if (tag instanceof NBTTagList) {
            NBTTagList listTag = (NBTTagList) tag;
            JsonArray jsonArray = new JsonArray();
            for (int i = 0; i < listTag.tagCount(); i++) {
                jsonArray.add(loadJson(listTag.get(i)));
            }
            return jsonArray;
        } else if (tag instanceof NBTTagIntArray) {
            NBTTagIntArray listTag = (NBTTagIntArray) tag;
            int[] arr = listTag.getIntArray();
            JsonArray jsonArray = new JsonArray();
            for (int j : arr) {
                jsonArray.add(new JsonPrimitive(j));
            }
            return jsonArray;
        } else if (tag instanceof NBTTagByteArray) {
            NBTTagByteArray listTag = (NBTTagByteArray) tag;
            byte[] arr = listTag.getByteArray();
            JsonArray jsonArray = new JsonArray();
            for (byte b : arr) {
                jsonArray.add(new JsonPrimitive(b));
            }
            return jsonArray;
        } else if (tag instanceof NBTTagShort) {
            return new JsonPrimitive(((NBTTagShort) tag).getShort());
        } else if (tag instanceof NBTTagInt) {
            return new JsonPrimitive(((NBTTagInt) tag).getInt());
        } else if (tag instanceof NBTTagLong) {
            return new JsonPrimitive(((NBTTagLong) tag).getLong());
        } else if (tag instanceof NBTTagFloat) {
            return new JsonPrimitive(((NBTTagFloat) tag).getFloat());
        } else if (tag instanceof NBTTagDouble) {
            return new JsonPrimitive(((NBTTagDouble) tag).getDouble());
        } else if (tag instanceof NBTTagByte) {
            return new JsonPrimitive(((NBTTagByte) tag).getByte());
        } else if (tag instanceof NBTTagString) {
            return new JsonPrimitive(((NBTTagString) tag).getString());
        } else {
            return new JsonPrimitive("Broken_Json_Deserialize_Tag");
        }
    }

    public static StorageManager getInstance() {
        return INSTANCE;
    }

    private AtomicInteger searchId = new AtomicInteger(0);

    public static class StoragePage {
        public ItemStack[] items = new ItemStack[45];
        public ItemStack backpackDisplayStack;
        public String customTitle;
        public int rows = -1;

        public transient boolean matchesSearch;
        public transient int searchedId;
    }

    public static int MAX_ENDER_CHEST_PAGES = 9;

    public static final ItemStack LOCKED_ENDERCHEST_STACK = Utils.createItemStack(Item.getItemFromBlock(Blocks.stained_glass_pane),
            "\u00a7cLocked Page", 14,
            "\u00a77Unlock more Ender Chest",
            "\u00a77pages in the community",
            "\u00a77shop!");

    public static class StorageConfig {
        public HashMap<String, StoragePage[]> pages = new HashMap<>();
        public final HashMap<Integer, Integer> displayToStorageIdMap = new HashMap<>();
    }

    public StorageConfig storageConfig = new StorageConfig();

    private int currentStoragePage = -1;
    public boolean onStorageMenu = false;

    private String lastSearch = "";

    private boolean[] storagePresent = null;

    //TODO: Replace with /storage {id} when hypixel becomes not lazy
    public int desiredStoragePage = -1;
    public long storageOpenSwitchMillis = 0;

    private ItemStack[] missingBackpackStacks = new ItemStack[18];

    private boolean shouldRenderStorageOverlayCached = false;

    private static final Pattern WINDOW_REGEX = Pattern.compile(".+ Backpack \\((\\d+)/(\\d+)\\)");
    private static final Pattern ECHEST_WINDOW_REGEX = Pattern.compile("Ender Chest \\((\\d+)/(\\d+)\\)");

    public void loadConfig(File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), StandardCharsets.UTF_8))) {
            storageConfig = GSON.fromJson(reader, StorageConfig.class);
        } catch (Exception ignored) {
        }
        if (storageConfig == null) {
            storageConfig = new StorageConfig();
        }
    }

    public void saveConfig(File file) {
        try {
            file.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file)), StandardCharsets.UTF_8))) {
                writer.write(GSON.toJson(storageConfig));
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    public ItemStack getMissingBackpackStack(int storageId) {
        if (missingBackpackStacks[storageId] != null) {
            return missingBackpackStacks[storageId];
        }

        ItemStack stack = Utils.createItemStack(Item.getItemFromBlock(Blocks.stained_glass_pane),
                "\u00a7cEmpty Backpack Slot " + (storageId + 1), 12,
                "",
                "\u00a7eLeft-click a backpack",
                "\u00a7eitem on this slot to place",
                "\u00a7eit!");

        missingBackpackStacks[storageId] = stack;
        return stack;
    }

    public boolean shouldRenderStorageOverlay(String containerName) {
        if (!NotEnoughUpdates.INSTANCE.config.storageGUI.enableStorageGUI) {
            shouldRenderStorageOverlayCached = false;
            return false;
        }

        if (!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) {
            shouldRenderStorageOverlayCached = false;
            return false;
        }

        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) {
            shouldRenderStorageOverlayCached = false;
            return false;
        }

        if (getCurrentWindowId() != -1 && getCurrentPageId() != -1) {
            shouldRenderStorageOverlayCached = true;
            return true;
        }

        shouldRenderStorageOverlayCached = containerName != null && containerName.trim().startsWith("Storage");
        return shouldRenderStorageOverlayCached;
    }

    public boolean shouldRenderStorageOverlayFast() {
        return shouldRenderStorageOverlayCached;
    }

    private StoragePage[] getPagesForProfile() {
        if (!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return null;
        if (SBInfo.getInstance().currentProfile == null) return null;

        return storageConfig.pages.computeIfAbsent(SBInfo.getInstance().currentProfile, k -> new StoragePage[27]);
    }

    public StoragePage getPage(int pageIndex, boolean createPage) {
        if (pageIndex == -1) return null;

        StoragePage[] pages = getPagesForProfile();
        if (pages == null) return null;

        if (createPage && pages[pageIndex] == null) pages[pageIndex] = new StoragePage();

        return pages[pageIndex];
    }

    public void removePage(int pageIndex) {
        if (pageIndex == -1) return;

        StoragePage[] pages = getPagesForProfile();
        if (pages == null) return;

        pages[pageIndex] = null;
    }

    public StoragePage getCurrentPage() {
        return getPage(getCurrentPageId(), true);
    }

    private void setItemSlot(int index, ItemStack item) {
        StoragePage page = getCurrentPage();
        if (page != null) {
            page.items[index] = item;
        }
    }

    public int getCurrentPageId() {
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) {
            currentStoragePage = -1;
            return -1;
        }

        return currentStoragePage;
    }

    public int getCurrentWindowId() {
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) {
            currentStoragePage = -1;
            return -1;
        }

        GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;

        return chest.inventorySlots.windowId;
    }

    public void sendToPage(int page) {
        if (desiredStoragePage != getCurrentPageId() &&
                System.currentTimeMillis() - storageOpenSwitchMillis < 1000) return;
        if (getCurrentPageId() == page) return;

        if (page == 0) {
            NotEnoughUpdates.INSTANCE.sendChatMessage("/enderchest");
        } else if (getCurrentWindowId() != -1 && onStorageMenu) {
            if (page < 9) {
                sendMouseClick(getCurrentWindowId(), 9 + page);
            } else {
                sendMouseClick(getCurrentWindowId(), 27 + page - MAX_ENDER_CHEST_PAGES);
            }
        } else {
            storageOpenSwitchMillis = System.currentTimeMillis();
            desiredStoragePage = page;

            NotEnoughUpdates.INSTANCE.sendChatMessage("/storage " + (desiredStoragePage - 8));
        }
    }

    private void sendMouseClick(int windowId, int slotIndex) {
        EntityPlayerSP playerIn = Minecraft.getMinecraft().thePlayer;
        short short1 = playerIn.openContainer.getNextTransactionID(playerIn.inventory);
        ItemStack itemstack = playerIn.openContainer.getSlot(slotIndex).getStack();
        Minecraft.getMinecraft().getNetHandler().addToSendQueue(new C0EPacketClickWindow(windowId, slotIndex, 0, 0, itemstack, short1));
    }

    public int getDisplayIdForStorageId(int storageId) {
        if (storageId < 0) return -1;
        for (Map.Entry<Integer, Integer> entry : storageConfig.displayToStorageIdMap.entrySet()) {
            if (entry.getValue() == storageId) {
                return entry.getKey();
            }
        }
        return -1;
    }

    public boolean onAnyClick() {
        if (onStorageMenu && desiredStoragePage >= 0) {
            if (desiredStoragePage < 9) {
                sendMouseClick(getCurrentWindowId(), 9 + desiredStoragePage);
            } else {
                sendMouseClick(getCurrentWindowId(), 27 + desiredStoragePage - MAX_ENDER_CHEST_PAGES);
            }
            desiredStoragePage = -1;
            return true;
        }
        return false;
    }

    public void openWindowPacket(S2DPacketOpenWindow packet) {
        shouldRenderStorageOverlayCached = false;
        if (!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return;

        String windowTitle = Utils.cleanColour(packet.getWindowTitle().getUnformattedText());

        Matcher matcher = WINDOW_REGEX.matcher(windowTitle);
        Matcher matcherEchest = ECHEST_WINDOW_REGEX.matcher(windowTitle);

        currentStoragePage = -1;
        onStorageMenu = false;

        if (windowTitle.trim().equals("Storage")) {
            onStorageMenu = true;
        } else if (matcher.matches()) {
            int page = Integer.parseInt(matcher.group(1));

            if (page > 0 && page <= 18) {
                currentStoragePage = page - 1 + MAX_ENDER_CHEST_PAGES;

                int displayId = getDisplayIdForStorageId(currentStoragePage);
                if (displayId >= 0) StorageOverlay.getInstance().scrollToStorage(displayId, false);

                StoragePage spage = getCurrentPage();
                if (spage != null) {
                    spage.rows = packet.getSlotCount() / 9 - 1;
                }
            }
        } else if (matcherEchest.matches()) {
            int page = Integer.parseInt(matcherEchest.group(1));

            if (page > 0 && page <= 9) {
                currentStoragePage = page - 1;

                int displayId = getDisplayIdForStorageId(currentStoragePage);
                if (displayId >= 0) StorageOverlay.getInstance().scrollToStorage(displayId, false);

                StoragePage spage = getCurrentPage();
                if (spage != null) {
                    spage.rows = packet.getSlotCount() / 9 - 1;
                }
            }
        } else {
            StorageOverlay.getInstance().clearSearch();
        }
    }

    public void closeWindowPacket(S2EPacketCloseWindow packet) {
        shouldRenderStorageOverlayCached = false;
    }

    public void setSlotPacket(S2FPacketSetSlot packet) {
        if (!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return;
        if (getCurrentWindowId() == -1 || getCurrentWindowId() != packet.func_149175_c()) return;

        if (getCurrentPageId() != -1) {
            StoragePage page = getCurrentPage();

            int slot = packet.func_149173_d();
            if (page != null && slot >= 9 && slot < 9 + page.rows * 9) {
                setItemSlot(packet.func_149173_d() - 9, packet.func_149174_e());
            }
        } else if (onStorageMenu) {
            if (storagePresent == null) {
                storagePresent = new boolean[27];
            }

            int slot = packet.func_149173_d();
            ItemStack stack = packet.func_149174_e();

            if (slot >= 9 && slot < 18) {
                int index = slot - 9;

                boolean changed = false;
                if (stack.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane) &&
                        stack.getMetadata() == 14) {
                    if (storagePresent[index]) changed = true;
                    storagePresent[index] = false;
                    removePage(index);
                } else {
                    if (!storagePresent[index]) changed = true;
                    storagePresent[index] = true;
                    getPage(index, true).backpackDisplayStack = stack;
                }

                if (changed) {
                    synchronized (storageConfig.displayToStorageIdMap) {
                        storageConfig.displayToStorageIdMap.clear();
                        int displayIndex = 0;
                        for (int i = 0; i < storagePresent.length; i++) {
                            if (storagePresent[i]) {
                                if (lastSearch == null || lastSearch.isEmpty()) {
                                    storageConfig.displayToStorageIdMap.put(displayIndex++, i);
                                } else {
                                    StoragePage page = getPage(i, false);

                                    if (page != null) {
                                        updateSearchForPage(lastSearch, page);
                                        if (page.matchesSearch) {
                                            storageConfig.displayToStorageIdMap.put(displayIndex++, i);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (slot >= 27 && slot < 45) {
                int index = (slot - 27) % 9 + (slot - 27) / 9 * 9 + MAX_ENDER_CHEST_PAGES;

                boolean changed = false;

                if (stack.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane)) {
                    if (storagePresent[index]) changed = true;
                    storagePresent[index] = false;
                    removePage(index);
                } else {
                    if (!storagePresent[index]) changed = true;
                    storagePresent[index] = true;
                    getPage(index, true).backpackDisplayStack = stack;
                }

                if (changed) {
                    synchronized (storageConfig.displayToStorageIdMap) {
                        storageConfig.displayToStorageIdMap.clear();
                        int displayIndex = 0;
                        for (int i = 0; i < storagePresent.length; i++) {
                            if (storagePresent[i]) {
                                if (lastSearch == null || lastSearch.isEmpty()) {
                                    storageConfig.displayToStorageIdMap.put(displayIndex++, i);
                                } else {
                                    StoragePage page = getPage(i, false);

                                    if (page != null) {
                                        updateSearchForPage(lastSearch, page);
                                        if (page.matchesSearch) {
                                            storageConfig.displayToStorageIdMap.put(displayIndex++, i);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void updateSearchForPage(String searchStr, StoragePage page) {
        if (page == null) {
            return;
        }

        if (page.rows <= 0) {
            page.matchesSearch = true;
            return;
        }

        if (page.searchedId > searchId.get()) {
            page.searchedId = -1;
            return;
        }
        if (page.searchedId == searchId.get()) {
            return;
        }

        page.searchedId = searchId.get();

        if (searchStr == null || searchStr.trim().isEmpty()) {
            page.matchesSearch = true;
            return;
        }

        for (ItemStack stack : page.items) {
            if (stack != null && NotEnoughUpdates.INSTANCE.manager.doesStackMatchSearch(stack, searchStr)) {
                page.matchesSearch = true;
                return;
            }
        }
        page.matchesSearch = false;
    }

    public void searchDisplay(String searchStr) {
        if (storagePresent == null) return;

        synchronized (storageConfig.displayToStorageIdMap) {
            storageConfig.displayToStorageIdMap.clear();

            lastSearch = searchStr;
            int sid = searchId.incrementAndGet();
            int displayIndex = 0;
            for (int i = 0; i < storagePresent.length; i++) {
                if (storagePresent[i]) {
                    StoragePage page = getPage(i, false);
                    if (page != null) {
                        if (page.rows > 0) {
                            updateSearchForPage(searchStr, page);
                            if (page.matchesSearch) {
                                storageConfig.displayToStorageIdMap.put(displayIndex++, i);
                            }
                        } else {
                            storageConfig.displayToStorageIdMap.put(displayIndex++, i);
                            page.matchesSearch = true;
                            page.searchedId = sid;
                        }
                    }
                }
            }
        }
    }

    public void setItemsPacket(S30PacketWindowItems packet) {
        if (!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return;
        if (getCurrentWindowId() == -1 || getCurrentWindowId() != packet.func_148911_c()) return;

        if (getCurrentPageId() != -1) {
            StoragePage page = getPage(getCurrentPageId(), false);

            if (page != null) {
                int max = Math.min(page.rows * 9, packet.getItemStacks().length - 9);
                for (int i = 0; i < max; i++) {
                    setItemSlot(i, packet.getItemStacks()[i + 9]);
                }
            }

        }
    }

    public void clientSendWindowClick(C0EPacketClickWindow packet) {
        if (!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return;
        if (getCurrentWindowId() == -1 || getCurrentWindowId() != packet.getWindowId()) return;
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) return;
        ContainerChest containerChest = (ContainerChest) ((GuiChest) Minecraft.getMinecraft().currentScreen).inventorySlots;

        if (getCurrentPageId() != -1) {
            StoragePage page = getCurrentPage();
            if (page == null) return;

            IInventory inv = containerChest.getLowerChestInventory();
            int max = Math.min(9 + page.rows * 9, inv.getSizeInventory());
            for (int i = 9; i < max; i++) {
                setItemSlot(i - 9, inv.getStackInSlot(i));
            }
        }
    }
}
