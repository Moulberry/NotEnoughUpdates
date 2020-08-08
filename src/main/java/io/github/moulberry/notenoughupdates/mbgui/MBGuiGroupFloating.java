package io.github.moulberry.notenoughupdates.mbgui;

import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class MBGuiGroupFloating extends MBGuiGroup {

    private LinkedHashMap<MBGuiElement, MBAnchorPoint> children;
    private GuiContainer lastContainer = null;
    private HashMap<MBGuiElement, Vector2f> childrenPositionOffset = new HashMap<>();

    public MBGuiGroupFloating(int width, int height, LinkedHashMap<MBGuiElement, MBAnchorPoint> children) {
        this.width = width;
        this.height = height;
        this.children = children;
        recalculate();
    }

    public Map<MBGuiElement, MBAnchorPoint> getChildrenMap() {
        return Collections.unmodifiableMap(children);
    }

    @Override
    public Map<MBGuiElement, Vector2f> getChildrenPosition() {
        GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;

        if(currentScreen instanceof GuiContainer) {
            GuiContainer currentContainer = (GuiContainer) currentScreen;
            if(lastContainer != currentContainer) {
                lastContainer = currentContainer;
                for(Map.Entry<MBGuiElement, MBAnchorPoint> entry : children.entrySet()) {
                    MBGuiElement child = entry.getKey();
                    MBAnchorPoint anchorPoint = entry.getValue();

                    Vector2f childPos;
                    if(childrenPosition.containsKey(child)) {
                        childPos = new Vector2f(childrenPosition.get(child));
                    } else {
                        childPos = new Vector2f();
                    }

                    if(anchorPoint.anchorPoint == MBAnchorPoint.AnchorPoint.INV_BOTMID) {
                        try {
                            int xSize = (int) Utils.getField(GuiContainer.class, currentContainer, "xSize", "field_146999_f");
                            int ySize = (int) Utils.getField(GuiContainer.class, currentContainer, "ySize", "field_147000_g");
                            int guiLeft = (int) Utils.getField(GuiContainer.class, currentContainer, "guiLeft", "field_147003_i");
                            int guiTop = (int) Utils.getField(GuiContainer.class, currentContainer, "guiTop", "field_147009_r");

                            int defGuiLeft = (this.width - xSize) / 2;
                            int defGuiTop = (this.height - ySize) / 2;

                            childPos.x += guiLeft-defGuiLeft + (anchorPoint.anchorPoint.x-0.5f)*xSize;
                            childPos.y += guiTop-defGuiTop + (anchorPoint.anchorPoint.y-0.5f)*ySize;
                        } catch(Exception ignored) {
                        }
                    }

                    childrenPositionOffset.put(child, childPos);
                }
            }
            return Collections.unmodifiableMap(childrenPositionOffset);
        } else {
            return Collections.unmodifiableMap(childrenPosition);
        }
    }

    @Override
    public void recalculate() {
        lastContainer = null;

        for(MBGuiElement child : children.keySet()) {
            child.recalculate();
        }

        for(Map.Entry<MBGuiElement, MBAnchorPoint> entry : children.entrySet()) {
            MBGuiElement child = entry.getKey();
            MBAnchorPoint anchorPoint = entry.getValue();
            float x = anchorPoint.anchorPoint.x * width - anchorPoint.anchorPoint.x * child.getWidth() + anchorPoint.offset.x;
            float y = anchorPoint.anchorPoint.y * height - anchorPoint.anchorPoint.y * child.getHeight() + anchorPoint.offset.y;

            if(anchorPoint.anchorPoint == MBAnchorPoint.AnchorPoint.INV_BOTMID) {
                x = width*0.5f + anchorPoint.offset.x;
                y = height*0.5f + anchorPoint.offset.y;
            }

            childrenPosition.put(child, new Vector2f(x, y));
        }
    }

    @Override
    public Collection<MBGuiElement> getChildren() {
        return children.keySet();
    }

}
