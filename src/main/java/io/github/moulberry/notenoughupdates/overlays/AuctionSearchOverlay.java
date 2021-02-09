package io.github.moulberry.notenoughupdates.overlays;

import com.google.common.base.Splitter;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.GuiElementTextField;
import io.github.moulberry.notenoughupdates.mixins.GuiEditSignAccessor;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.*;

public class AuctionSearchOverlay {

    private static GuiElementTextField textField = new GuiElementTextField("", 200, 20, 0);
    private static boolean searchFieldClicked = false;
    private static String searchString = "";
    private static Splitter SPACE_SPLITTER = Splitter.on(" ").omitEmptyStrings().trimResults();

    private static final int AUTOCOMPLETE_HEIGHT = 118;

    private static Set<String> autocompletedItems = new LinkedHashSet<>();

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
        if(!(Minecraft.getMinecraft().currentScreen instanceof GuiEditSign)) return false;

        String lastContainer = SBInfo.getInstance().lastOpenContainerName;

        if(!lastContainer.equals("Auctions Browser") && !lastContainer.startsWith("Auctions: ")) return false;

        TileEntitySign tes = ((GuiEditSignAccessor)Minecraft.getMinecraft().currentScreen).getTileSign();

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

        Minecraft.getMinecraft().fontRendererObj.drawString("Enter Query:", width/2-100, height/4-10, 0xdddddd, true);

        textField.setText(searchString);
        textField.render(width/2-100, height/4);

        if(textField.getText().trim().isEmpty()) autocompletedItems.clear();

        Gui.drawRect(width/2-101, height/4+25, width/2+101, height/4+25+ AUTOCOMPLETE_HEIGHT, 0xffffffff);
        Gui.drawRect(width/2-100, height/4+25+1, width/2+100, height/4+25-1+ AUTOCOMPLETE_HEIGHT, 0xff000000);

        List<String> tooltipToDisplay = null;

        int num = 0;
        for(String str : autocompletedItems) {
            JsonObject obj = NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(str);
            if(obj != null) {
                ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(obj);
                Gui.drawRect(width/2-96, height/4+30+num*22, width/2+96, height/4+30+num*22+20, 0xff505050);
                Minecraft.getMinecraft().fontRendererObj.drawString(Utils.trimIgnoreColour(stack.getDisplayName().replaceAll("\\[.+]", "")),
                        width/2-74, height/4+35+num*22, 0xdddddd, true);

                GlStateManager.enableDepth();
                Utils.drawItemStack(stack, width/2-94, height/4+32+num*22);

                if(mouseX > width/2-96 && mouseX < width/2+96 && mouseY > height/4+30+num*22 && mouseY < height/4+30+num*22+20) {
                    tooltipToDisplay = stack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                }

                if(++num >= 5) break;
            }
        }

        Minecraft.getMinecraft().fontRendererObj.drawString("Past Searches:", width/2-100, height/4+25+ AUTOCOMPLETE_HEIGHT +5, 0xdddddd, true);

        int pastSearchHeight = 9 + 10 * Math.min(5, NotEnoughUpdates.INSTANCE.config.hidden.previousAuctionSearches.size());

        Gui.drawRect(width/2-101, height/4+ AUTOCOMPLETE_HEIGHT +40, width/2+101, height/4+ AUTOCOMPLETE_HEIGHT +40+pastSearchHeight, 0xffffffff);
        Gui.drawRect(width/2-100, height/4+ AUTOCOMPLETE_HEIGHT +40+1, width/2+100, height/4+ AUTOCOMPLETE_HEIGHT +40-1+pastSearchHeight, 0xff000000);

        for(int i=0; i<5; i++) {
            if(i >= NotEnoughUpdates.INSTANCE.config.hidden.previousAuctionSearches.size()) break;

            String s = NotEnoughUpdates.INSTANCE.config.hidden.previousAuctionSearches.get(i);
            Minecraft.getMinecraft().fontRendererObj.drawString(s, width/2-95, height/4+45+ AUTOCOMPLETE_HEIGHT +i*10, 0xdddddd, true);
        }

        if(tooltipToDisplay != null) {
            Utils.drawHoveringText(tooltipToDisplay, mouseX, mouseY, width, height, -1, Minecraft.getMinecraft().fontRendererObj);
        }
    }

    public static void close() {
        TileEntitySign tes = ((GuiEditSignAccessor)Minecraft.getMinecraft().currentScreen).getTileSign();

        if(searchString.length() <= 15) {
            tes.signText[0] = new ChatComponentText(searchString.substring(0, Math.min(searchString.length(), 15)));
        } else {
            List<String> words = SPACE_SPLITTER.splitToList(searchString);

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

    public static void keyEvent() {
        if(Keyboard.getEventKey() == Keyboard.KEY_ESCAPE || Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
            close();
        } else if(Keyboard.getEventKeyState()) {
            textField.setText(searchString);
            textField.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
            searchString = textField.getText();

            autocompletedItems.clear();

            List<String> title = new ArrayList<>(NotEnoughUpdates.INSTANCE.manager.search("title:"+searchString.trim()));
            if(!searchString.trim().contains(" ")) {
                StringBuilder sb = new StringBuilder();
                for(char c : searchString.toCharArray()) {
                    sb.append(c).append(" ");
                }
                title.addAll(NotEnoughUpdates.INSTANCE.manager.search("title:"+sb.toString().trim()));
            }
            List<String> desc = new ArrayList<>(NotEnoughUpdates.INSTANCE.manager.search("desc:"+searchString.trim()));
            desc.removeAll(title);

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

            autocompletedItems.addAll(title);
            autocompletedItems.addAll(desc);
        }
    }

    public static void mouseEvent() {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();
        int mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
        int mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;

        if(!Mouse.getEventButtonState() && Mouse.getEventButton() == -1 && searchFieldClicked) {
            textField.mouseClickMove(mouseX-2, height/4+10, 0, 0);
        }

        if(Mouse.getEventButton() != -1) {
            searchFieldClicked = false;
        }

        if(mouseX > width/2-100 && mouseX < width/2+100 && mouseY > height/4 && mouseY < height/4+20) {
            if(Mouse.getEventButtonState()) {
                searchFieldClicked = true;
                textField.mouseClicked(mouseX-2, mouseY, Mouse.getEventButton());

                if(Mouse.getEventButton() == 1) {
                    searchString = "";
                    autocompletedItems.clear();
                }
            }
        } else if(Mouse.getEventButtonState() && Mouse.getEventButton() == 0) {
            int num = 0;
            for(String str : autocompletedItems) {
                JsonObject obj = NotEnoughUpdates.INSTANCE.manager.getItemInformation().get(str);
                if(obj != null) {
                    ItemStack stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(obj);
                    if(mouseX >= width/2-96 && mouseX <= width/2+96 && mouseY >= height/4+30+num*22 && mouseY <= height/4+30+num*22+20) {
                        searchString = Utils.cleanColour(stack.getDisplayName().replaceAll("\\[.+]", "")).trim();
                        close();
                        return;
                    }

                    if(++num >= 5) break;
                }
            }

            for(int i=0; i<5; i++) {
                if(i >= NotEnoughUpdates.INSTANCE.config.hidden.previousAuctionSearches.size()) break;

                String s = NotEnoughUpdates.INSTANCE.config.hidden.previousAuctionSearches.get(i);
                if(mouseX >= width/2-95 && mouseX <= width/2+95 && mouseY >= height/4+45+AUTOCOMPLETE_HEIGHT+i*10 && mouseY <= height/4+45+AUTOCOMPLETE_HEIGHT+i*10+10) {
                    searchString = s;
                    close();
                    return;
                }
            }
        }
    }

}
