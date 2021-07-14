package io.github.moulberry.notenoughupdates.overlays;

import com.google.common.base.Splitter;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.GuiElementTextField;
import io.github.moulberry.notenoughupdates.core.GuiScreenElementWrapper;
import io.github.moulberry.notenoughupdates.options.NEUConfigEditor;
import io.github.moulberry.notenoughupdates.util.Constants;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionSearchOverlay {

    private static final ResourceLocation SEARCH_OVERLAY_TEXTURE = new ResourceLocation("notenoughupdates:auc_search/ah_search_overlay.png");
    private static final ResourceLocation STAR = new ResourceLocation("notenoughupdates:auc_search/star.png");
    private static final ResourceLocation STAR_BOARD = new ResourceLocation("notenoughupdates:auc_search/star_board.png");

    private static GuiElementTextField textField = new GuiElementTextField("", 200, 20, 0);
    private static boolean searchFieldClicked = false;
    private static String searchString = "";
    private static String searchStringExtra = "";
    private static Splitter SPACE_SPLITTER = Splitter.on(" ").omitEmptyStrings().trimResults();

    private static int selectedStars = 0;
    private static boolean atLeast = true;

    private static final int AUTOCOMPLETE_HEIGHT = 118;

    private static final Set<String> autocompletedItems = new LinkedHashSet<>();

    private static final Comparator<String> salesComparator = (o1, o2) -> {
        JsonObject auctionInfo1 = NotEnoughUpdates.INSTANCE.manager.auctionManager.getItemAuctionInfo(o1);
        JsonObject auctionInfo2 = NotEnoughUpdates.INSTANCE.manager.auctionManager.getItemAuctionInfo(o2);

        boolean auc1Invalid = auctionInfo1 == null || !auctionInfo1.has("sales");
        boolean auc2Invalid = auctionInfo2 == null || !auctionInfo2.has("sales");

        if(auc1Invalid && auc2Invalid) return o1.compareTo(o2);
        if(auc1Invalid) return -1;
        if(auc2Invalid) return 1;

        int sales1 = auctionInfo1.get("sales").getAsInt();
        int sales2 = auctionInfo2.get("sales").getAsInt();

        if(sales1 == sales2) return o1.compareTo(o2);
        if(sales1 > sales2) return -1;
        return 1;
    };

    public static boolean shouldReplace() {
        if(!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return false;
        if(!NotEnoughUpdates.INSTANCE.config.auctionHouseSearch.enableSearchOverlay) return false;

        if(!(Minecraft.getMinecraft().currentScreen instanceof GuiEditSign)) {
            if(!NotEnoughUpdates.INSTANCE.config.auctionHouseSearch.keepPreviousSearch) searchString = "";
            return false;
        }

        String lastContainer = SBInfo.getInstance().lastOpenContainerName;

        if(lastContainer == null) return false;
        if(!lastContainer.equals("Auctions Browser") && !lastContainer.startsWith("Auctions: ")) return false;

        TileEntitySign tes = ((GuiEditSign)Minecraft.getMinecraft().currentScreen).tileSign;

        if(tes == null) return false;
        if(tes.getPos().getY() != 0) return false;
        if(!tes.signText[2].getUnformattedText().equals("^^^^^^^^^^^^^^^")) return false;
        if(!tes.signText[3].getUnformattedText().equals("Enter query")) return false;

        return true;
    }

    public static void render() {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();
        int mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
        int mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;

        Utils.drawGradientRect(0, 0, width, height, -1072689136, -804253680);

        int h = NotEnoughUpdates.INSTANCE.config.auctionHouseSearch.showPastSearches ? 219 : 145;

        int topY = height/4;
        if(scaledResolution.getScaleFactor() >= 4) {
            topY = height/2 - h/2 + 5;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(SEARCH_OVERLAY_TEXTURE);
        GlStateManager.color(1, 1, 1, 1);
        Utils.drawTexturedRect(width/2-100, topY-1, 203, h, 0, 203/512f, 0, h/256f, GL11.GL_NEAREST);

        Minecraft.getMinecraft().getTextureManager().bindTexture(STAR_BOARD);
        Utils.drawTexturedRect(width/2+105, topY+27, 55, 13, GL11.GL_NEAREST);

        Minecraft.getMinecraft().getTextureManager().bindTexture(STAR);
        GlStateManager.color(1, 1, 1, 1);
        int stars = atLeast && selectedStars > 0 ? 5 : selectedStars;
        for(int i=0; i<stars; i++) {
            if(i >= selectedStars) {
                GlStateManager.color(1, 1, 1, 0.3f);
            }
            Utils.drawTexturedRect(width/2+108+10*i, topY+29, 9, 10, GL11.GL_NEAREST);
        }

        Gui.drawRect(width/2+106, topY+42, width/2+115, topY+51, 0xffffffff);
        Gui.drawRect(width/2+107, topY+43, width/2+114, topY+50, 0xff000000);
        if(atLeast) Gui.drawRect(width/2+108, topY+44, width/2+113, topY+49, 0xffffffff);
        Minecraft.getMinecraft().fontRendererObj.drawString("At Least?", width/2+117, topY+43, 0xffffff);

        Minecraft.getMinecraft().fontRendererObj.drawString("Enter Query:", width/2-100, topY-10, 0xdddddd, true);

        textField.setFocus(true);
        textField.setText(searchString);
        textField.setSize(149, 20);
        textField.setCustomBorderColour(0xffffff);
        textField.render(width/2-100+1, topY+1);

        if(textField.getText().trim().isEmpty()) autocompletedItems.clear();

        //Gui.drawRect(width/2-101, height/4+25, width/2+101, height/4+25+ AUTOCOMPLETE_HEIGHT, 0xffffffff);
        //Gui.drawRect(width/2-100, height/4+25+1, width/2+100, height/4+25-1+ AUTOCOMPLETE_HEIGHT, 0xff000000);

        List<String> tooltipToDisplay = null;

        int num = 0;
        synchronized(autocompletedItems) {
            for(String str : autocompletedItems) {
                JsonObject obj = NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(str);
                if(obj != null) {
                    ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(obj);
                    //Gui.drawRect(width/2-96, height/4+30+num*22, width/2+96, height/4+30+num*22+20, 0xff505050);

                    Minecraft.getMinecraft().getTextureManager().bindTexture(SEARCH_OVERLAY_TEXTURE);
                    GlStateManager.color(1, 1, 1, 1);
                    Utils.drawTexturedRect(width/2-96+1, topY+30+num*22+1, 193, 21, 214/512f, 407/512f, 0, 21/256f, GL11.GL_NEAREST);

                    String itemName = Utils.trimIgnoreColour(stack.getDisplayName().replaceAll("\\[.+]", ""));
                    if(itemName.contains("Enchanted Book") && str.contains(";")) {
                        String[] lore = NotEnoughUpdates.INSTANCE.manager.getLoreFromNBT(stack.getTagCompound());
                        itemName = lore[0].trim();
                    }

                    Minecraft.getMinecraft().fontRendererObj.drawString(Minecraft.getMinecraft().fontRendererObj.trimStringToWidth(itemName, 165),
                            width/2-74, topY+35+num*22+1, 0xdddddd, true);

                    GlStateManager.enableDepth();
                    Utils.drawItemStack(stack, width/2-94+2, topY+32+num*22+1);

                    if(mouseX > width/2-96 && mouseX < width/2+96 && mouseY > topY+30+num*22 && mouseY < topY+30+num*22+20) {
                        tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                    }

                    if(++num >= 5) break;
                }
            }
        }

        if(NotEnoughUpdates.INSTANCE.config.auctionHouseSearch.showPastSearches) {
            Minecraft.getMinecraft().fontRendererObj.drawString("Past Searches:", width/2-100, topY+25+ AUTOCOMPLETE_HEIGHT +5, 0xdddddd, true);

            for(int i=0; i<5; i++) {
                if(i >= NotEnoughUpdates.INSTANCE.config.hidden.previousAuctionSearches.size()) break;

                String s = NotEnoughUpdates.INSTANCE.config.hidden.previousAuctionSearches.get(i);
                Minecraft.getMinecraft().fontRendererObj.drawString(s, width/2-95+1, topY+45+ AUTOCOMPLETE_HEIGHT +i*10+2, 0xdddddd, true);
            }

            if(tooltipToDisplay != null) {
                Utils.drawHoveringText(tooltipToDisplay, mouseX, mouseY, width, height, -1, Minecraft.getMinecraft().fontRendererObj);
            }
        }

    }

    public static void close() {
        if(NotEnoughUpdates.INSTANCE.config.auctionHouseSearch.keepPreviousSearch) {
            search();
        } else {
            synchronized(autocompletedItems) {
                autocompletedItems.clear();
            }
        }


        TileEntitySign tes = ((GuiEditSign)Minecraft.getMinecraft().currentScreen).tileSign;

        String search = searchString.trim();
        if(searchStringExtra != null && !searchStringExtra.isEmpty()) {
            search += " " + searchStringExtra.trim();
        }
        if(search.length() <= 15) {
            tes.signText[0] = new ChatComponentText(search);
        } else {
            List<String> words = SPACE_SPLITTER.splitToList(search);

            StringBuilder line0 = new StringBuilder();
            StringBuilder line1 = new StringBuilder();

            int currentLine = 0;
            for(String word : words) {
                if(currentLine == 0) {
                    if(line0.length() + word.length() > 15) {
                        currentLine++;
                    } else {
                        line0.append(word);
                        if(line0.length() >= 15) {
                            currentLine++;
                            continue;
                        } else {
                            line0.append(" ");
                        }
                    }
                }
                if(currentLine == 1) {
                    if(line1.length() + word.length() > 15) {
                        line1.append(word, 0, 15 - line1.length());
                        break;
                    } else {
                        line1.append(word);
                        if(line1.length() >= 15) {
                            break;
                        } else {
                            line1.append(" ");
                        }
                    }
                }
                if(line1.length() >= 15) break;
            }

            tes.signText[0] = new ChatComponentText(line0.toString().trim());
            tes.signText[1] = new ChatComponentText(line1.toString().trim());
        }

        if(!searchString.trim().isEmpty()) {
            List<String> previousAuctionSearches = NotEnoughUpdates.INSTANCE.config.hidden.previousAuctionSearches;
            previousAuctionSearches.remove(searchString);
            previousAuctionSearches.remove(searchString);
            previousAuctionSearches.add(0, searchString);
            while(previousAuctionSearches.size() > 5) {
                previousAuctionSearches.remove(previousAuctionSearches.size()-1);
            }
        }

        Minecraft.getMinecraft().displayGuiScreen(null);

        if (Minecraft.getMinecraft().currentScreen == null) {
            Minecraft.getMinecraft().setIngameFocus();
        }
    }

    private static ExecutorService searchES = Executors.newSingleThreadExecutor();
    private static AtomicInteger searchId = new AtomicInteger(0);

    public static void search() {
        final int thisSearchId = searchId.incrementAndGet();

        searchES.submit(() -> {
            if(thisSearchId != searchId.get()) return;

            List<String> title = new ArrayList<>(NotEnoughUpdates.INSTANCE.manager.search("title:"+searchString.trim()));

            if(thisSearchId != searchId.get()) return;

            if(!searchString.trim().contains(" ")) {
                StringBuilder sb = new StringBuilder();
                for(char c : searchString.toCharArray()) {
                    sb.append(c).append(" ");
                }
                title.addAll(NotEnoughUpdates.INSTANCE.manager.search("title:"+sb.toString().trim()));
            }

            if(thisSearchId != searchId.get()) return;

            List<String> desc = new ArrayList<>(NotEnoughUpdates.INSTANCE.manager.search("desc:"+searchString.trim()));
            desc.removeAll(title);

            if(thisSearchId != searchId.get()) return;

            Set<String> auctionableItems = NotEnoughUpdates.INSTANCE.manager.auctionManager.getLowestBinKeySet();
            auctionableItems.addAll(NotEnoughUpdates.INSTANCE.manager.auctionManager.getItemAuctionInfoKeySet());

            if(!auctionableItems.isEmpty()) {
                title.retainAll(auctionableItems);
                desc.retainAll(auctionableItems);

                title.sort(salesComparator);
                desc.sort(salesComparator);
            } else {
                Set<String> bazaarItems = NotEnoughUpdates.INSTANCE.manager.auctionManager.getBazaarKeySet();

                title.removeAll(bazaarItems);
                desc.removeAll(bazaarItems);
            }

            if(thisSearchId != searchId.get()) return;

            synchronized(autocompletedItems) {
                autocompletedItems.clear();
                autocompletedItems.addAll(title);
                autocompletedItems.addAll(desc);
            }
        });
    }

    public static void keyEvent() {
        if(Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
            searchStringExtra = "";
            close();
            if(NotEnoughUpdates.INSTANCE.config.auctionHouseSearch.escFullClose) {
                Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow(Minecraft.getMinecraft().thePlayer.openContainer.windowId));
            }
        } else if(Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
            searchStringExtra = "";
            close();
        } else if(Keyboard.getEventKeyState()) {
            textField.setFocus(true);
            textField.setText(searchString);
            textField.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
            searchString = textField.getText();

            search();
        }
    }

    public static void mouseEvent() {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();
        int mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
        int mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;

        int h = NotEnoughUpdates.INSTANCE.config.auctionHouseSearch.showPastSearches ? 219 : 145;

        int topY = height/4;
        if(scaledResolution.getScaleFactor() >= 4) {
            topY = height/2 - h/2 + 5;
        }

        if(Mouse.getEventButtonState() && mouseX > width/2+105 && mouseX < width/2+105+55 &&
            mouseY > topY+27 && mouseY < topY+40) {
            int starClicked = 5;
            for(int i=1; i<=5; i++) {
                if(mouseX < width/2+108+10*i) {
                    starClicked = i;
                    break;
                }
            }
            if(selectedStars == starClicked) {
                selectedStars = 0;
            } else {
                selectedStars = starClicked;
            }
            return;
        }

        if(Mouse.getEventButtonState() && mouseX >= width/2+106 && mouseX <= width/2+116 &&
                mouseY >= topY+42 && mouseY <= topY+50) {
            atLeast = !atLeast;
            return;
        }

        if(!Mouse.getEventButtonState() && Mouse.getEventButton() == -1 && searchFieldClicked) {
            textField.mouseClickMove(mouseX-2, topY+10, 0, 0);
        }

        if(Mouse.getEventButton() != -1) {
            searchFieldClicked = false;
        }

        if(Mouse.getEventButtonState()) {
            if(mouseY > topY && mouseY < topY+20) {
                if(mouseX > width/2-100) {
                    if(mouseX < width/2+49) {
                        searchFieldClicked = true;
                        textField.mouseClicked(mouseX-2, mouseY, Mouse.getEventButton());

                        if(Mouse.getEventButton() == 1) {
                            searchString = "";
                            synchronized(autocompletedItems) {
                                autocompletedItems.clear();
                            }
                        }
                    } else if(mouseX < width/2+75) {
                        searchStringExtra = "";
                        close();
                    } else if(mouseX < width/2+100) {
                        searchStringExtra = "";
                        close();
                        Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow(Minecraft.getMinecraft().thePlayer.openContainer.windowId));
                        NotEnoughUpdates.INSTANCE.openGui = new GuiScreenElementWrapper(new NEUConfigEditor(
                                NotEnoughUpdates.INSTANCE.config, "AH Search GUI"));
                    }
                }
            } else if(Mouse.getEventButton() == 0) {
                int num = 0;
                synchronized(autocompletedItems) {
                    for(String str : autocompletedItems) {
                        JsonObject obj = NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(str);
                        if(obj != null) {
                            ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(obj);
                            if(mouseX >= width/2-96 && mouseX <= width/2+96 && mouseY >= topY+30+num*22 && mouseY <= topY+30+num*22+20) {
                                searchString = Utils.cleanColour(stack.getDisplayName().replaceAll("\\[.+]", "")).trim();
                                if(searchString.contains("Enchanted Book") && str.contains(";")) {
                                    String[] lore = NotEnoughUpdates.INSTANCE.manager.getLoreFromNBT(stack.getTagCompound());
                                    String[] split = Utils.cleanColour(lore[0]).trim().split(" ");
                                    split[split.length-1] = "";

                                    searchString = StringUtils.join(split, " ").trim();
                                }

                                JsonObject essenceCosts = Constants.ESSENCECOSTS;
                                searchStringExtra = "";
                                if(essenceCosts != null && essenceCosts.has(str) && selectedStars > 0) {
                                    for(int i=0; i<selectedStars; i++) {
                                        searchStringExtra += "\u272A";
                                    }
                                    if(selectedStars < 5 && !atLeast) {
                                        searchStringExtra += " ";
                                        searchStringExtra += stack.getItem().getItemStackDisplayName(stack).substring(0, 1);
                                    }
                                }

                                close();
                                return;
                            }

                            if(++num >= 5) break;
                        }
                    }
                }

                if(NotEnoughUpdates.INSTANCE.config.auctionHouseSearch.showPastSearches) {
                    for(int i=0; i<5; i++) {
                        if(i >= NotEnoughUpdates.INSTANCE.config.hidden.previousAuctionSearches.size()) break;

                        String s = NotEnoughUpdates.INSTANCE.config.hidden.previousAuctionSearches.get(i);
                        if(mouseX >= width/2-95 && mouseX <= width/2+95 && mouseY >= topY+45+AUTOCOMPLETE_HEIGHT+i*10 && mouseY <= topY+45+AUTOCOMPLETE_HEIGHT+i*10+10) {
                            searchString = s;
                            searchStringExtra = "";
                            close();
                            return;
                        }
                    }
                }
            }
        }


    }

}
