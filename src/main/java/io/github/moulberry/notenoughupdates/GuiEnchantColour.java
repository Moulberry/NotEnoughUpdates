package io.github.moulberry.notenoughupdates;

import com.google.common.base.Splitter;
import io.github.moulberry.notenoughupdates.itemeditor.GuiElementTextField;
import io.github.moulberry.notenoughupdates.util.LerpingInteger;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
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
    private final int xSize = 176;
    private int ySize = 0;

    private List<String> getEnchantColours() {
        return NotEnoughUpdates.INSTANCE.manager.config.enchantColours.value;
    }

    public static final Splitter splitter = Splitter.on(":").limit(4);

    private HashMap<Integer, String> comparators = new HashMap<>();
    private List<GuiElementTextField[]> guiElementTextFields = new ArrayList<>();

    private LerpingInteger scroll = new LerpingInteger(0, 100);

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


        NotEnoughUpdates.INSTANCE.manager.loadConfig();

        Minecraft.getMinecraft().getTextureManager().bindTexture(custom_ench_colour);
        Utils.drawTexturedRect(guiLeft, guiTop, xSize, 21, 0, 1, 0, 21/78f, GL11.GL_NEAREST);
        Utils.drawTexturedRect(guiLeft, guiTop+ySize-32, xSize, 32, 0, 1, 46/78f, 1, GL11.GL_NEAREST);

        fontRendererObj.drawString("Ench Name", guiLeft+10, guiTop+7, 4210752);
        fontRendererObj.drawString("CMP", guiLeft+71, guiTop+7, 4210752);
        fontRendererObj.drawString("LVL", guiLeft+96, guiTop+7, 4210752);
        fontRendererObj.drawString("COL", guiLeft+121, guiTop+7, 4210752);
        fontRendererObj.drawString("DEL", guiLeft+146, guiTop+7, 4210752);

        Utils.drawStringCentered("Add Ench Colour", fontRendererObj, guiLeft+xSize/2, guiTop+ySize-20, false, 4210752);

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

            if(colourCode.length() > 1) colourCode = String.valueOf(colourCode.toLowerCase().charAt(0));
            if(comparator.length() > 1) comparator = String.valueOf(comparator.toLowerCase().charAt(0));

            Utils.drawStringCentered(comparator, fontRendererObj, guiLeft+81, guiTop+33+25*yIndex, false, 4210752);

            if(guiElementTextFields.size() <= yIndex) {
                guiElementTextFields.add(new GuiElementTextField[3]);
            }
            if(guiElementTextFields.get(yIndex)[0] == null) {
                guiElementTextFields.get(yIndex)[0] = new GuiElementTextField(enchantName, GuiElementTextField.SCALE_TEXT);
                guiElementTextFields.get(yIndex)[0].setSize(56, 20);
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

            guiElementTextFields.get(yIndex)[0].render(guiLeft+10, guiTop+23+25*yIndex);
            guiElementTextFields.get(yIndex)[1].render(guiLeft+96, guiTop+23+25*yIndex);
            guiElementTextFields.get(yIndex)[2].render(guiLeft+121, guiTop+23+25*yIndex);

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

                    NotEnoughUpdates.INSTANCE.manager.config.enchantColours.value.remove(yIndex);
                    if(yIndex+addOffset < 0) {
                        addOffset = -yIndex;
                    } else if(yIndex+addOffset > NotEnoughUpdates.INSTANCE.manager.config.enchantColours.value.size()) {
                        addOffset = NotEnoughUpdates.INSTANCE.manager.config.enchantColours.value.size()-yIndex;
                    }
                    System.out.println(addOffset);
                    NotEnoughUpdates.INSTANCE.manager.config.enchantColours.value.add(yIndex+addOffset,
                            getEnchantOpString(guiElementTextFields.get(yIndex), comparators.get(yIndex)));
                    NotEnoughUpdates.INSTANCE.manager.saveConfig();
                    if(addOffset != 0) {
                        GuiElementTextField[] guiElementTextFieldArray = guiElementTextFields.remove(yIndex);
                        guiElementTextFields.add(yIndex+addOffset, guiElementTextFieldArray);
                    }
                    return;
                }
            }
        }
    }

    public String getEnchantOpString(GuiElementTextField[] tfs, String comparator) {
        StringBuilder enchantOp = new StringBuilder();
        enchantOp.append(tfs[0].getText());
        enchantOp.append(":");
        enchantOp.append(comparator);
        enchantOp.append(":");
        enchantOp.append(tfs[1].getText());
        enchantOp.append(":");
        enchantOp.append(tfs[2].getText());
        return enchantOp.toString();
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

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for(int yIndex=0; yIndex<guiElementTextFields.size(); yIndex++) {
            for(int i=0; i<3; i++) {
                int x = guiLeft+10;
                if(i == 1) x+=86;
                else if(i == 2) x+=111;

                if(mouseX > x && mouseX < x+guiElementTextFields.get(yIndex)[i].getWidth()) {
                    if(mouseY > guiTop+23+25*yIndex && mouseY < guiTop+23+25*yIndex+20) {
                        guiElementTextFields.get(yIndex)[i].mouseClicked(mouseX, mouseY, mouseButton);
                        if(mouseButton == 1) {
                            NotEnoughUpdates.INSTANCE.manager.config.enchantColours.value.remove(yIndex);
                            NotEnoughUpdates.INSTANCE.manager.config.enchantColours.value.add(yIndex,
                                    getEnchantOpString(guiElementTextFields.get(yIndex), comparators.get(yIndex)));
                            NotEnoughUpdates.INSTANCE.manager.saveConfig();
                        }
                        continue;
                    }
                }
                guiElementTextFields.get(yIndex)[i].otherComponentClick();
            }
            comparators.computeIfAbsent(yIndex, k->">");
            if(mouseY > guiTop+23+25*yIndex && mouseY < guiTop+23+25*yIndex+20) {
                if(mouseX > guiLeft+71 && mouseX < guiLeft+71+20) {
                    switch (comparators.get(yIndex)) {
                        case ">":
                            comparators.put(yIndex, "="); break;
                        case "=":
                            comparators.put(yIndex, "<"); break;
                        default:
                            comparators.put(yIndex, ">"); break;
                    }
                    NotEnoughUpdates.INSTANCE.manager.config.enchantColours.value.remove(yIndex);
                    NotEnoughUpdates.INSTANCE.manager.config.enchantColours.value.add(yIndex,
                            getEnchantOpString(guiElementTextFields.get(yIndex), comparators.get(yIndex)));
                    NotEnoughUpdates.INSTANCE.manager.saveConfig();
                } else if(mouseX > guiLeft+146 && mouseX < guiLeft+146+20) {
                    NotEnoughUpdates.INSTANCE.manager.config.enchantColours.value.remove(yIndex);
                    guiElementTextFields.remove(yIndex);
                    comparators.remove(yIndex);
                    NotEnoughUpdates.INSTANCE.manager.saveConfig();
                }
            }
        }
        if(mouseX >= guiLeft+42 && mouseX <= guiLeft+42+88) {
            if(mouseY >= guiTop+ySize-30 && mouseY <= guiTop+ySize-10) {
                NotEnoughUpdates.INSTANCE.manager.config.enchantColours.value.add("[a-zA-Z ]+:>:5:9");
                NotEnoughUpdates.INSTANCE.manager.saveConfig();
            }
        }
    }

    public static String getColourOpIndex(List<String> colourOps, int index) {
        if(colourOps.size() > index) {
            return colourOps.get(index);
        } else {
            switch(index) {
                case 0:
                    return "[a-zA-Z ]+";
                case 1:
                    return ">";
                case 2:
                    return "5";
                case 3:
                    return "9";
            }
        }
        return null;
    }
}
