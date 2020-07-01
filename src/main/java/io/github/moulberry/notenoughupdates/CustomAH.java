package io.github.moulberry.notenoughupdates;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.util.TexLoc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.moulberry.notenoughupdates.GuiTextures.*;

public class CustomAH extends GuiScreen {

    private static final ResourceLocation inventoryBackground = new ResourceLocation("textures/gui/container/inventory.png");
    private static final ResourceLocation creativeTabSearch = new ResourceLocation("textures/gui/container/creative_inventory/tab_item_search.png");
    private static final ResourceLocation creativeInventoryTabs = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

    private int yScrollInitial = 0;
    private boolean scrollClicked = false;


    private int splits = 2;

    private int ySplit = 35;
    private int ySplitSize = 18;
    private int ySize = 136 + ySplitSize*splits;
    private int xSize = 195;

    private float scrollAmount;

    private int guiLeft = 0;
    private int guiTop = 0;

    private NEUManager manager;
    private HashMap<String, JsonObject> auctionItems = new HashMap<>();

    private TexLoc tl = new TexLoc(0, 0, Keyboard.KEY_M);

    public CustomAH(NEUManager manager) {
        this.manager = manager;
        updateAuctions();
    }

    private void updateAuctions() {
        HashMap<Integer, JsonObject> pages = new HashMap<>();

        HashMap<String, String> args = new HashMap<>();
        args.put("page", "0");
        AtomicInteger totalPages = new AtomicInteger(1);
        AtomicInteger currentPages = new AtomicInteger(0);
        manager.hypixelApi.getHypixelApiAsync(manager.config.apiKey.value, "skyblock/profiles",
            args, jsonObject -> {
                if(jsonObject.get("success").getAsBoolean()) {
                    pages.put(0, jsonObject);
                    totalPages.set(jsonObject.get("totalPages").getAsInt());
                    currentPages.incrementAndGet();

                    for(int i=1; i<totalPages.get(); i++) {
                        int j = i;
                        HashMap<String, String> args2 = new HashMap<>();
                        args2.put("page", ""+i);
                        manager.hypixelApi.getHypixelApiAsync(manager.config.apiKey.value, "skyblock/profiles",
                            args2, jsonObject2 -> {
                                if (jsonObject2.get("success").getAsBoolean()) {
                                    pages.put(j, jsonObject2);
                                    currentPages.incrementAndGet();
                                }
                            }
                        );
                    }
                }
            }
        );

        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();//1593549115 () 1593631661919
        long startTime = System.currentTimeMillis();
        ses.schedule(new Runnable() {
            public void run() {
                if(System.currentTimeMillis() - startTime > 20000) return;

                if(currentPages.get() == totalPages.get()) {
                    auctionItems.clear();
                    for(int pageNum : pages.keySet()) {
                        JsonObject page = pages.get(pageNum);
                        JsonArray auctions = page.get("auctions").getAsJsonArray();
                        for(int i=0; i<auctions.size(); i++) {
                            JsonObject auction = auctions.get(i).getAsJsonObject();
                            String auctionUuid = auction.get("uuid").getAsString();
                            String auctioneerUuid = auction.get("auctioneer").getAsString();
                            int end = auction.get("end").getAsInt();
                            String category = auction.get("category").getAsString();

                            String item_bytes = auction.get("item_bytes").getAsString();

                            try {
                                NBTTagCompound tag = CompressedStreamTools.readCompressed(new ByteArrayInputStream(Base64.getDecoder().decode(item_bytes)));
                                tag = tag.getTagList("i", 10).getCompoundTagAt(0);
                                int id = tag.getShort("id");
                                int damage = tag.getShort("Damage");
                                tag = tag.getCompoundTag("tag");

                                String internalname = "";
                                if(tag != null && tag.hasKey("ExtraAttributes", 10)) {
                                    NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");

                                    if(ea.hasKey("id", 8)) {
                                        internalname = ea.getString("id");
                                    }
                                }

                                String[] lore = new String[0];
                                NBTTagCompound display = tag.getCompoundTag("display");

                                if(display.hasKey("Lore", 9)) {
                                    NBTTagList list = display.getTagList("Lore", 8);
                                    lore = new String[list.tagCount()];
                                    for(int k=0; k<list.tagCount(); k++) {
                                        lore[k] = list.getStringTagAt(k);
                                    }
                                }

                                String itemid = Item.getItemById(id).getRegistryName();
                                String displayname = display.getString("Name");
                                String[] info = new String[0];
                                String clickcommand = "";

                                JsonObject item = new JsonObject();

                            } catch(IOException e) {}
                        }
                    }
                    return;
                }

                ses.schedule(this, 1000L, TimeUnit.MILLISECONDS);
            }
        }, 5000L, TimeUnit.MILLISECONDS);

    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        tl.handleKeyboardInput();

        guiLeft = (this.width - xSize)/2;
        guiTop = (this.height - ySize)/2;

        this.mc.getTextureManager().bindTexture(creativeTabSearch);
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySplit);
        int y = guiTop+ySplit;
        for(int i=0; i<splits; i++) {
            this.drawTexturedModalRect(guiLeft, y, 0, ySplit, xSize, ySplit+ySplitSize);
            y += ySplitSize;
        }
        this.drawTexturedModalRect(guiLeft, y, 0, ySplit, xSize, 136-ySplit);

        this.mc.getTextureManager().bindTexture(creativeInventoryTabs);
        this.drawTexturedModalRect(guiLeft+175, guiTop+18+(int)((95+ySplitSize*2)*scrollAmount),
                256-(scrollClicked?12:24), 0, 12, 15);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int y = guiTop+18+(int)((95+ySplitSize*2)*scrollAmount);
        if(mouseX > guiLeft+175 && mouseX < guiLeft+175+12) {
            if(mouseY > y && mouseY < y+15) {
                scrollClicked = true;
                return;
            }
        }
        scrollClicked = false;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        scrollClicked = false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if(scrollClicked) {
            int yMin = guiTop+18                   + 8;
            int yMax = guiTop+18+(95+ySplitSize*2) + 8;

            scrollAmount = (mouseY-yMin)/(float)yMax;
            scrollAmount = Math.max(0, Math.min(1, scrollAmount));
        }
    }
}
