package io.github.moulberry.notenoughupdates.mbgui;

import org.lwjgl.util.vector.Vector2f;

import java.io.Serializable;

public class MBAnchorPoint implements Serializable {

    public enum AnchorPoint {
        TOPLEFT(0, 0), TOPMID(0.5f, 0), TOPRIGHT(1, 0),
        MIDRIGHT(1, 0.5f), BOTRIGHT(1, 1), BOTMID(0.5f, 1),
        BOTLEFT(0, 1), MIDLEFT(0, 0.5f), MIDMID(0.5f, 0.5f);

        public final float x;
        public final float y;

        AnchorPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public AnchorPoint anchorPoint;
    public Vector2f offset;

    public MBAnchorPoint(AnchorPoint anchorPoint, Vector2f offset) {
        this.anchorPoint = anchorPoint;
        this.offset = offset;
    }

    public static MBAnchorPoint createFromString(String str) {
        if(str == null || str.split(":").length != 3) {
            return null;
        }

        try {
            String[] split = str.split(":");
            AnchorPoint point = AnchorPoint.valueOf(split[0].toUpperCase());
            Vector2f pos = new Vector2f(Float.valueOf(split[1]), Float.valueOf(split[2]));
            return new MBAnchorPoint(point, pos);
        } catch(Exception e) { return null; }
    }

    @Override
    public String toString() {
        return anchorPoint.toString() + ":" + offset.x + ":" + offset.y;
    }
}
