package io.github.moulberry.notenoughupdates.mbgui;

import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public abstract class MBGuiGroupHorz extends MBGuiGroup {

    private List<MBGuiElement> children;

    public MBGuiGroupHorz(List<MBGuiElement> children) {
        this.children = children;
        recalculate();
    }

    public abstract int getPadding();

    public Collection<MBGuiElement> getChildren() {
        return children;
    }

    public void recalculate() {
        for(MBGuiElement child : children) {
            child.recalculate();
        }

        width = 0;
        for(int i=0; i<children.size(); i++) {
            MBGuiElement child = children.get(i);
            childrenPosition.put(child, new Vector2f(width, 0));
            width += child.getWidth();
            if(i != children.size()-1) width += getPadding();
        }

        height = 0;
        for(MBGuiElement child : children) {
            int childHeight = child.getHeight();
            if(childHeight > height) {
                height = childHeight;
            }
        }
    }

}
