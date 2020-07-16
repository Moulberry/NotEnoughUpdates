package io.github.moulberry.notenoughupdates.mbgui;

import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class MBGuiGroupFloating extends MBGuiGroup {

    private LinkedHashMap<MBGuiElement, MBAnchorPoint> children;

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
    public void recalculate() {
        for(MBGuiElement child : children.keySet()) {
            child.recalculate();
        }

        for(Map.Entry<MBGuiElement, MBAnchorPoint> entry : children.entrySet()) {
            MBGuiElement child = entry.getKey();
            MBAnchorPoint anchorPoint = entry.getValue();
            float x = anchorPoint.anchorPoint.x * width - anchorPoint.anchorPoint.x * child.getWidth() + anchorPoint.offset.x;
            float y = anchorPoint.anchorPoint.y * height - anchorPoint.anchorPoint.y * child.getHeight() + anchorPoint.offset.y;
            childrenPosition.put(child, new Vector2f(x, y));
        }
    }

    @Override
    public Collection<MBGuiElement> getChildren() {
        return children.keySet();
    }

}
