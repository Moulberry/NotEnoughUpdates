package io.github.moulberry.notenoughupdates.miscgui;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.util.lerp.LerpingInteger;
import io.github.moulberry.notenoughupdates.itemeditor.GuiElementTextField;
import io.github.moulberry.notenoughupdates.util.Constants;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GuiEnchantColour extends GuiScreen {

    public static final ResourceLocation custom_ench_colour = new ResourceLocation("notenoughupdates:custom_ench_colour.png");

    private int guiLeft;
    private int guiTop;
    private final int xSize = 217;
    private int ySize = 0;

    public static final Splitter splitter = Splitter.on(":").limit(5);

    private HashMap<Integer, String> comparators = new HashMap<>();
    private HashMap<Integer, String> modifiers = new HashMap<>();
    private List<GuiElementTextField[]> guiElementTextFields = new ArrayList<>();

    private List<String> enchantNamesPretty = null;

    private LerpingInteger scroll = new LerpingInteger(0, 100);

    public static int BOLD_MODIFIER = 0b1;
    public static int ITALIC_MODIFIER = 0b10;
    public static int OBFUSCATED_MODIFIER = 0b100;
    public static int UNDERLINE_MODIFIER = 0b1000;
    public static int STRIKETHROUGH_MODIFIER = 0b10000;

    private List<String> getEnchantNamesPretty() {
        if(enchantNamesPretty == null) {
            JsonObject enchantsJson = Constants.ENCHANTS;
            if(!enchantsJson.has("enchants_pretty")) {
                return Lists.newArrayList("ERROR");
            } else {
                JsonArray pretty = enchantsJson.getAsJsonArray("enchants_pretty");

                enchantNamesPretty = new ArrayList<>();
                for(int i=0; i<pretty.size(); i++) {
                    enchantNamesPretty.add(pretty.get(i).getAsString());
                }
            }
        }
        return enchantNamesPretty;
    }

    private List<String> getEnchantColours() {
        return NotEnoughUpdates.INSTANCE.config.hidden.enchantColours;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        List<String> enchantColours = getEnchantColours();

        ySize = 53+25*enchantColours.size();
        guiLeft = (width-xSize)/2;

        if(ySize > height) {
            if(scroll.getTarget() > 0) {
                scroll.setTarget(0);
            } else if(scroll.getTarget() < height-ySize) {
                scroll.setTarget(height-ySize);
            }
            scroll.tick();
            guiTop = scroll.getValue();
        } else {
            guiTop = (height-ySize)/2;
            scroll.setValue(0);
            scroll.resetTimer();
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(custom_ench_colour);
        Utils.drawTexturedRect(guiLeft, guiTop, xSize, 21, 0, 1, 0, 21/78f, GL11.GL_NEAREST);
        Utils.drawTexturedRect(guiLeft, guiTop+ySize-32, xSize, 32, 0, 1, 46/78f, 1, GL11.GL_NEAREST);

        fontRendererObj.drawString("Ench Name", guiLeft+10, guiTop+7, 4210752);
        fontRendererObj.drawString("CMP", guiLeft+86, guiTop+7, 4210752);
        fontRendererObj.drawString("LVL", guiLeft+111, guiTop+7, 4210752);
        fontRendererObj.drawString("COL", guiLeft+136, guiTop+7, 4210752);
        fontRendererObj.drawString("DEL", guiLeft+161, guiTop+7, 4210752);

        Utils.drawStringCentered("Add Ench Colour", fontRendererObj, guiLeft+xSize/2+1, guiTop+ySize-20, false, 4210752);

        int yIndex = 0;
        for(String str : enchantColours) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(custom_ench_colour);
            GlStateManager.color(1, 1, 1, 1);
            Utils.drawTexturedRect(guiLeft, guiTop+21+yIndex*25, xSize, 25, 0, 1, 21/78f, 46/78f, GL11.GL_NEAREST);

            List<String> colourOps = splitter.splitToList(str);
            String enchantName = getColourOpIndex(colourOps, 0);
            String comparator = getColourOpIndex(colourOps, 1);
            String comparison = getColourOpIndex(colourOps, 2);
            String colourCode = getColourOpIndex(colourOps, 3);
            String modifier = getColourOpIndex(colourOps, 4);
            modifiers.put(yIndex, modifier);

            if(colourCode.length() > 1) colourCode = String.valueOf(colourCode.toLowerCase().charAt(0));
            if(comparator.length() > 1) comparator = String.valueOf(comparator.toLowerCase().charAt(0));

            Utils.drawStringCentered(comparator, fontRendererObj, guiLeft+96, guiTop+33+25*yIndex, false, 4210752);

            if(guiElementTextFields.size() <= yIndex) {
                guiElementTextFields.add(new GuiElementTextField[3]);
            }
            if(guiElementTextFields.get(yIndex)[0] == null) {
                guiElementTextFields.get(yIndex)[0] = new GuiElementTextField(enchantName, GuiElementTextField.SCALE_TEXT);
                guiElementTextFields.get(yIndex)[0].setSize(75, 20);
            }
            if(guiElementTextFields.get(yIndex)[1] == null) {
                guiElementTextFields.get(yIndex)[1] = new GuiElementTextField(comparison,
                        GuiElementTextField.SCALE_TEXT|GuiElementTextField.NUM_ONLY|GuiElementTextField.NO_SPACE);
                guiElementTextFields.get(yIndex)[1].setSize(20, 20);
            }
            if(guiElementTextFields.get(yIndex)[2] == null) {
                guiElementTextFields.get(yIndex)[2] = new GuiElementTextField(colourCode, GuiElementTextField.SCALE_TEXT);
                guiElementTextFields.get(yIndex)[2].setSize(20, 20);
            }
            guiElementTextFields.get(yIndex)[0].setText(enchantName);
            guiElementTextFields.get(yIndex)[1].setText(comparison);
            comparators.put(yIndex, comparator);
            guiElementTextFields.get(yIndex)[2].setText(colourCode);

            guiElementTextFields.get(yIndex)[0].render(guiLeft+7, guiTop+23+25*yIndex);
            guiElementTextFields.get(yIndex)[1].render(guiLeft+110, guiTop+23+25*yIndex);
            guiElementTextFields.get(yIndex)[2].render(guiLeft+135, guiTop+23+25*yIndex);

            int modifierI = getIntModifier(modifier);
            if((modifierI & GuiEnchantColour.BOLD_MODIFIER) != 0) {
                Minecraft.getMinecraft().fontRendererObj.drawString("\u00a7l\u2713", guiLeft+181, guiTop+23+25*yIndex-2,  0xff202020, true);
            }
            if((modifierI & GuiEnchantColour.ITALIC_MODIFIER) != 0) {
                Minecraft.getMinecraft().fontRendererObj.drawString("\u00a7l\u2713", guiLeft+181, guiTop+23+25*yIndex+10,  0xff202020, true);
            }
            if((modifierI & GuiEnchantColour.UNDERLINE_MODIFIER) != 0) {
                Minecraft.getMinecraft().fontRendererObj.drawString("\u00a7l\u2713", guiLeft+196, guiTop+23+25*yIndex-2,  0xff202020, true);
            }
            if((modifierI & GuiEnchantColour.STRIKETHROUGH_MODIFIER) != 0) {
                Minecraft.getMinecraft().fontRendererObj.drawString("\u00a7l\u2713", guiLeft+196, guiTop+23+25*yIndex+10,  0xff202020, true);
            }

            yIndex++;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        for(int yIndex=0; yIndex<guiElementTextFields.size(); yIndex++) {
            for(int i=0; i<3; i++) {
                guiElementTextFields.get(yIndex)[i].keyTyped(typedChar, keyCode);
                if(guiElementTextFields.get(yIndex)[i].getFocus()) {
                    int addOffset = 0;
                    if(keyCode == Keyboard.KEY_UP) {
                        addOffset -= 1;
                    } else if(keyCode == Keyboard.KEY_DOWN) {
                        addOffset += 1;
                    }

                    NotEnoughUpdates.INSTANCE.config.hidden.enchantColours.remove(yIndex);
                    if(yIndex+addOffset < 0) {
                        addOffset = -yIndex;
                    } else if(yIndex+addOffset > NotEnoughUpdates.INSTANCE.config.hidden.enchantColours.size()) {
                        addOffset = NotEnoughUpdates.INSTANCE.config.hidden.enchantColours.size()-yIndex;
                    }
                    NotEnoughUpdates.INSTANCE.config.hidden.enchantColours.add(yIndex+addOffset,
                            getEnchantOpString(guiElementTextFields.get(yIndex), comparators.get(yIndex), modifiers.get(yIndex)));
                    if(addOffset != 0) {
                        GuiElementTextField[] guiElementTextFieldArray = guiElementTextFields.remove(yIndex);
                        guiElementTextFields.add(yIndex+addOffset, guiElementTextFieldArray);
                    }
                    return;
                }
            }
        }
    }

    public String getEnchantOpString(GuiElementTextField[] tfs, String comparator, String modifiers) {
        return tfs[0].getText() +
                ":" +
                comparator +
                ":" +
                tfs[1].getText() +
                ":" +
                tfs[2].getText() +
                ":" +
                modifiers;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int dWheel = Mouse.getEventDWheel();

        if(dWheel < 0) {
            scroll.setTarget(scroll.getTarget()-50);
            scroll.resetTimer();
        } else if(dWheel > 0) {
            scroll.setTarget(scroll.getTarget()+50);
            scroll.resetTimer();
        }
    }

    public static int getIntModifier(String modifier) {
        try {
            return Integer.parseInt(modifier);
        } catch(NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for(int yIndex=0; yIndex<guiElementTextFields.size(); yIndex++) {
            for(int i=0; i<3; i++) {
                int x = guiLeft+7;
                if(i == 1) x+=103;
                else if(i == 2) x+=128;

                if(mouseX > x && mouseX < x+guiElementTextFields.get(yIndex)[i].getWidth()) {
                    if(mouseY > guiTop+23+25*yIndex && mouseY < guiTop+23+25*yIndex+20) {
                        guiElementTextFields.get(yIndex)[i].mouseClicked(mouseX, mouseY, mouseButton);
                        if(mouseButton == 1) {
                            NotEnoughUpdates.INSTANCE.config.hidden.enchantColours.remove(yIndex);
                            NotEnoughUpdates.INSTANCE.config.hidden.enchantColours.add(yIndex,
                                    getEnchantOpString(guiElementTextFields.get(yIndex), comparators.get(yIndex), modifiers.get(yIndex)));
                        }
                        continue;
                    }
                }
                guiElementTextFields.get(yIndex)[i].otherComponentClick();
            }
            comparators.putIfAbsent(yIndex, ">");
            modifiers.putIfAbsent(yIndex, "0");
            if(mouseX >= guiLeft+180 && mouseX <= guiLeft+210 &&
                    mouseY >= guiTop+23+25*yIndex && mouseY <= guiTop+23+25*yIndex+20) {
                int modifierI = getIntModifier(modifiers.get(yIndex));
                int selectedModifier = -1;

                if(mouseX < guiLeft+195) {
                    if(mouseY < guiTop+23+25*yIndex+10) {
                        selectedModifier = BOLD_MODIFIER;
                    } else {
                        selectedModifier = ITALIC_MODIFIER;
                    }
                } else {
                    if(mouseY < guiTop+23+25*yIndex+10) {
                        selectedModifier = UNDERLINE_MODIFIER;
                    } else {
                        selectedModifier = STRIKETHROUGH_MODIFIER;
                    }
                }

                if(selectedModifier != -1) {
                    int modifierMasked = (modifierI & selectedModifier);
                    int modifierMaskedInverted = selectedModifier - modifierMasked;

                    int modifierInverted = (-1) - selectedModifier;

                    int finalModifier = (modifierI & modifierInverted) | modifierMaskedInverted;

                    modifiers.put(yIndex, ""+finalModifier);

                    NotEnoughUpdates.INSTANCE.config.hidden.enchantColours.remove(yIndex);
                    NotEnoughUpdates.INSTANCE.config.hidden.enchantColours.add(yIndex,
                            getEnchantOpString(guiElementTextFields.get(yIndex), comparators.get(yIndex), modifiers.get(yIndex)));
                }
            }
            if(mouseY > guiTop+23+25*yIndex && mouseY < guiTop+23+25*yIndex+20) {
                if(mouseX > guiLeft+86 && mouseX < guiLeft+86+20) {
                    switch (comparators.get(yIndex)) {
                        case ">":
                            comparators.put(yIndex, "="); break;
                        case "=":
                            comparators.put(yIndex, "<"); break;
                        default:
                            comparators.put(yIndex, ">"); break;
                    }
                    NotEnoughUpdates.INSTANCE.config.hidden.enchantColours.remove(yIndex);
                    NotEnoughUpdates.INSTANCE.config.hidden.enchantColours.add(yIndex,
                            getEnchantOpString(guiElementTextFields.get(yIndex), comparators.get(yIndex), modifiers.get(yIndex)));
                } else if(mouseX > guiLeft+160 && mouseX < guiLeft+160+20) {
                    NotEnoughUpdates.INSTANCE.config.hidden.enchantColours.remove(yIndex);
                    guiElementTextFields.remove(yIndex);
                    comparators.remove(yIndex);
                    modifiers.remove(yIndex);
                }
            }
        }
        if(mouseX >= guiLeft+57 && mouseX <= guiLeft+xSize-57) {
            if(mouseY >= guiTop+ySize-30 && mouseY <= guiTop+ySize-10) {
                NotEnoughUpdates.INSTANCE.config.hidden.enchantColours.add("[a-zA-Z\\- ]+:>:5:9:0");
            }
        }
    }

    public static String getColourOpIndex(List<String> colourOps, int index) {
        if(colourOps.size() > index) {
            return colourOps.get(index);
        } else {
            switch(index) {
                case 0:
                    return "[a-zA-Z\\- ]+";
                case 1:
                    return ">";
                case 2:
                    return "5";
                case 3:
                    return "9";
                case 4:
                    return "0";
            }
        }
        return null;
    }
}
