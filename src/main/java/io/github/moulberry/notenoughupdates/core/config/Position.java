package io.github.moulberry.notenoughupdates.core.config;

import com.google.gson.annotations.Expose;
import net.minecraft.client.gui.ScaledResolution;

public class Position {

    @Expose
    private int x;
    @Expose
    private int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Position clone() {
        return new Position(x, y);
    }

    public int getRawX() {
        return x;
    }

    public int getRawY() {
        return y;
    }

    public int getAbsX(ScaledResolution scaledResolution) {
        if(x < 0) {
            return scaledResolution.getScaledWidth() + x;
        } else {
            return x;
        }
    }

    public int getAbsY(ScaledResolution scaledResolution) {
        if(y < 0) {
            return scaledResolution.getScaledHeight() + y;
        } else {
            return y;
        }
    }

    public int moveX(int deltaX, int objWidth, ScaledResolution scaledResolution) {
        int screenWidth = scaledResolution.getScaledWidth();
        boolean wasPositiveX = this.x >= 0;
        this.x += deltaX;

        if(wasPositiveX) {
            if(this.x < 2) {
                deltaX += 2-this.x;
                this.x = 2;
            }
            if(this.x > screenWidth-2) {
                deltaX += screenWidth-2-this.x;
                this.x = screenWidth-2;
            }
        } else {
            if(this.x+objWidth > -2) {
                deltaX += -2-objWidth-this.x;
                this.x = -2-objWidth;
            }
            if(this.x+screenWidth < 2) {
                deltaX += 2-screenWidth-this.x;
                this.x = 2-screenWidth;
            }
        }

        if(this.x >= 0 && this.x+objWidth/2 > screenWidth/2) {
            this.x -= screenWidth;
        }
        if(this.x < 0 && this.x+objWidth/2 <= -screenWidth/2) {
            this.x += screenWidth;
        }
        return deltaX;
    }

    public int moveY(int deltaY, int objHeight, ScaledResolution scaledResolution) {
        int screenHeight = scaledResolution.getScaledHeight();
        boolean wasPositiveY = this.y >= 0;
        this.y += deltaY;

        if(wasPositiveY) {
            if(this.y < 2) {
                deltaY += 2-this.y;
                this.y = 2;
            }
            if(this.y > screenHeight-2) {
                deltaY += screenHeight-2-this.y;
                this.y = screenHeight-2;
            }
        } else {
            if(this.y+objHeight > -2) {
                deltaY += -2-objHeight-this.y;
                this.y = -2-objHeight;
            }
            if(this.y+screenHeight < 2) {
                deltaY += 2-screenHeight-this.y;
                this.y = 2-screenHeight;
            }
        }

        if(this.y >= 0 && this.y-objHeight/2 > screenHeight/2) {
            this.y -= screenHeight;
        }
        if(this.y < 0 && this.y-objHeight/2 <= -screenHeight/2) {
            this.y += screenHeight;
        }
        return deltaY;
    }

}
