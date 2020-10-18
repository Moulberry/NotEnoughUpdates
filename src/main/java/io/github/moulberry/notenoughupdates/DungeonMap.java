package io.github.moulberry.notenoughupdates;

import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec4b;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class DungeonMap {

    private int[] mapTextureData = null;
    private DynamicTexture dynamicTexture = new DynamicTexture(128, 128);
    private ResourceLocation dynamicRL = new ResourceLocation("notenoughupdates:dynamic_dungeonmap.png");

    private void setMapRGB(int x, int y, int rgb) {
        if(mapTextureData != null) {
            mapTextureData[x+y*128] = rgb;
        }
    }
    
    private class RoomOffset {
        int x;
        int y;

        public RoomOffset(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public RoomOffset left() {
            return new RoomOffset(x-1, y);
        }

        public RoomOffset right() {
            return new RoomOffset(x+1, y);
        }

        public RoomOffset up() {
            return new RoomOffset(x, y-1);
        }

        public RoomOffset down() {
            return new RoomOffset(x, y+1);
        }

        public RoomOffset[] getNeighbors() {
            return new RoomOffset[]{left(), right(), up(), down()};
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RoomOffset that = (RoomOffset) o;
            return x == that.x && y == that.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    private enum RoomConnectionType {
        NONE, WALL, CORRIDOR, ROOM_DIVIDER
    }

    private class RoomConnection {
        RoomConnectionType type;
        Color colour;

        public RoomConnection(RoomConnectionType type, Color colour) {
            this.type = type;
            this.colour = colour;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RoomConnection that = (RoomConnection) o;
            return type == that.type &&
                    Objects.equals(colour, that.colour);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, colour);
        }
    }

    private class Room {
        Color colour = new Color(0, 0, 0, 0);
        int tickColour = 0;
        boolean fillCorner = false;

        RoomConnection left = new RoomConnection(RoomConnectionType.NONE, new Color(0, true));
        RoomConnection up = new RoomConnection(RoomConnectionType.NONE, new Color(0, true));
        RoomConnection right = new RoomConnection(RoomConnectionType.NONE, new Color(0, true));
        RoomConnection down = new RoomConnection(RoomConnectionType.NONE, new Color(0, true));

        public void render(int roomSize, int connectorSize) {
            Gui.drawRect(0, 0, roomSize, roomSize, colour.getRGB());
            if(tickColour != 0) {
                Gui.drawRect(roomSize/2-4, roomSize/2-4, roomSize/2+4, roomSize/2+4, tickColour);
            }

            if(fillCorner) {
                Gui.drawRect(-connectorSize, -connectorSize, 0, 0, colour.getRGB());
            }

            for(int k=0; k<4; k++) {
                RoomConnection connection = up;
                if(k == 1) connection = right;
                if(k == 2) connection = down;
                if(k == 3) connection = left;

                if(connection.type == RoomConnectionType.NONE || connection.type == RoomConnectionType.WALL) continue;

                int xOffset = 0;
                int yOffset = 0;
                int width = 0;
                int height = 0;

                if(connection == up) {
                    yOffset = -connectorSize;
                    width = roomSize;
                    height = connectorSize;

                    if(connection.type == RoomConnectionType.CORRIDOR) {
                        width = 8;
                        xOffset += 4;
                    }
                } else if(connection == right) {
                    xOffset = roomSize;
                    width = connectorSize;
                    height = roomSize;

                    if(connection.type == RoomConnectionType.CORRIDOR) {
                        height = 8;
                        yOffset += 4;
                    }
                } else if(connection == down) {
                    yOffset = roomSize;
                    width = roomSize;
                    height = connectorSize;

                    if(connection.type == RoomConnectionType.CORRIDOR) {
                        width = 8;
                        xOffset += 4;
                    }
                } else if(connection == left) {
                    xOffset = -connectorSize;
                    width = connectorSize;
                    height = roomSize;

                    if(connection.type == RoomConnectionType.CORRIDOR) {
                        height = 8;
                        yOffset += 4;
                    }
                }

                Gui.drawRect(xOffset, yOffset, xOffset+width, yOffset+height, connection.colour.getRGB());
            }
        }
    }
    
    public void render(int[] renderTo, int x, int y, int rgb) {
        int i = x+y*128;
        if(i >= 0 && i < renderTo.length) {
            renderTo[i] = rgb;
        }
    }

    private static final ResourceLocation mapIcons = new ResourceLocation("textures/map/map_icons.png");

    public void render() {
        int minRoomX = 999;
        int minRoomY = 999;
        int maxRoomX = -999;
        int maxRoomY = -999;
        for(RoomOffset offset : roomMap.keySet()) {
            minRoomX = Math.min(offset.x, minRoomX);
            minRoomY = Math.min(offset.y, minRoomY);
            maxRoomX = Math.max(offset.x, maxRoomX);
            maxRoomY = Math.max(offset.y, maxRoomY);
        }

        int roomSize = 16;
        int connSize = 4;

        Gui.drawRect(8, 8, 8+(maxRoomX-minRoomX+1)*(roomSize+connSize), 8+(maxRoomY-minRoomY+1)*(roomSize+connSize),
                new Color(200, 200, 200).getRGB());

        for(Map.Entry<RoomOffset, Room> entry : roomMap.entrySet()) {
            RoomOffset roomOffset = entry.getKey();
            Room room = entry.getValue();

            int x = (roomOffset.x-minRoomX)*(roomSize+connSize);
            int y = (roomOffset.y-minRoomY)*(roomSize+connSize);

            GlStateManager.pushMatrix();
            GlStateManager.translate(x+10, y+10, 0);
            room.render(roomSize, connSize);
            GlStateManager.translate(-(x+10), -(y+10), 0);
            GlStateManager.popMatrix();
        }

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.color(1, 1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(mapIcons);
        int k = 0;
        for(MapDecoration decoration : decorations) {
            float x = (decoration.roomsOffsetX+decoration.roomInPercentX)*roomSize +
                    (decoration.connOffsetX+decoration.connInPercentX)*connectorSize;
            float y = (decoration.roomsOffsetY+decoration.roomInPercentY)*roomSize +
                    (decoration.connOffsetY+decoration.connInPercentY)*connectorSize;

            x -= minRoomX*(roomSize+connSize);
            y -= minRoomY*(roomSize+connSize);

            //System.out.println(decoration.angle);

            GlStateManager.pushMatrix();
            GlStateManager.translate(x+10, y+10, -0.02F);
            GlStateManager.rotate(decoration.angle, 0.0F, 0.0F, 1.0F);
            GlStateManager.scale(4.0F, 4.0F, 3.0F);
            GlStateManager.translate(-0.125F, 0.125F, 0.0F);
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
            worldrenderer.pos(-1.0D, 1.0D, 10+((float)k * -0.001F)).tex(decoration.minU, decoration.minV).endVertex();
            worldrenderer.pos(1.0D, 1.0D, 10+((float)k * -0.001F)).tex(decoration.minU+1/4f, decoration.minV).endVertex();
            worldrenderer.pos(1.0D, -1.0D, 10+((float)k * -0.001F)).tex(decoration.minU+1/4f, decoration.minV+1/4f).endVertex();
            worldrenderer.pos(-1.0D, -1.0D, 10+((float)k * -0.001F)).tex(decoration.minU, decoration.minV+1/4f).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
            k--;
        }
    }

    private HashMap<RoomOffset, Room> roomMap = new HashMap<>();
    private Color[][] colourMap = new Color[128][128];
    private int startRoomX = -1;
    private int startRoomY = -1;
    private int connectorSize = 5;
    private int roomSize = 0;

    public void updateRoomConnections(RoomOffset roomOffset) {
        if(roomMap.containsKey(roomOffset)) {
            Room room = roomMap.get(roomOffset);

            int otherPixelFilled = 0;
            int otherPixelColour = 0;
            for(int xOff=0; xOff<roomSize; xOff++) {
                for(int yOff=0; yOff<roomSize; yOff++) {
                    int x = startRoomX + roomOffset.x*(roomSize+connectorSize) + xOff;
                    int y = startRoomY + roomOffset.y*(roomSize+connectorSize) + yOff;

                    if(x < colourMap.length && y < colourMap[x].length) {
                        Color c = colourMap[x][y];
                        if(!c.equals(room.colour)) {
                            if(otherPixelColour == c.getRGB()) {
                                otherPixelFilled++;
                            } else {
                                otherPixelFilled--;
                                if(otherPixelFilled <= 0) {
                                    otherPixelFilled = 1;
                                    otherPixelColour = c.getRGB();
                                }
                            }
                        }
                    }
                }
            }

            room.tickColour = 0;
            if((float)otherPixelFilled/roomSize/connectorSize > 0.05) {
                room.tickColour = otherPixelColour;
            }

            for(int k=0; k<4; k++) {
                int totalFilled = 0;

                for(int i=0; i<roomSize; i++) {
                    for(int j=1; j<=connectorSize; j++) {
                        int x = startRoomX + roomOffset.x*(roomSize+connectorSize);
                        int y = startRoomY + roomOffset.y*(roomSize+connectorSize);

                        if(k == 0) {
                            x += i;
                            y -= j;
                        } else if(k == 1) {
                            x += roomSize+j-1;
                            y += i;
                        } else if(k == 2) {
                            x += i;
                            y += roomSize+j-1;
                        } else {
                            x -= j;
                            y += i;
                        }

                        if(x < colourMap.length && y < colourMap[x].length) {
                            if(colourMap[x][y].equals(room.colour)) {
                                totalFilled++;
                            }
                        }
                    }
                }
                float proportionFilled = (float)totalFilled/roomSize/connectorSize;

                RoomConnectionType type = RoomConnectionType.WALL;
                if(proportionFilled > 0.8) {
                    type = RoomConnectionType.ROOM_DIVIDER;
                } else if(proportionFilled > 0.1) {
                    type = RoomConnectionType.CORRIDOR;
                }
                if(k == 0) {
                    room.up = new RoomConnection(type, room.colour);
                } else if(k == 1) {
                    room.right = new RoomConnection(type, room.colour);
                } else if(k == 2) {
                    room.down = new RoomConnection(type, room.colour);
                } else {
                    room.left = new RoomConnection(type, room.colour);
                }
            }

            if(room.left.type == RoomConnectionType.ROOM_DIVIDER && room.up.type == RoomConnectionType.ROOM_DIVIDER) {
                RoomOffset upleft = new RoomOffset(roomOffset.x-1, roomOffset.y-1);
                if(roomMap.containsKey(upleft)) {
                    Room upleftRoom = roomMap.get(upleft);
                    if(upleftRoom.right.type == RoomConnectionType.ROOM_DIVIDER && upleftRoom.down.type == RoomConnectionType.ROOM_DIVIDER) {
                        room.fillCorner = true;
                    }
                }
            }
        }
    }

    public void loadNeighbors(RoomOffset room) {
        if(!roomMap.containsKey(room)) {
            roomMap.put(room, new Room());
        }
        for(RoomOffset neighbor : room.getNeighbors()) {
            if(!roomMap.containsKey(neighbor)) {
                int x = startRoomX + neighbor.x*(roomSize+connectorSize);
                int y = startRoomY + neighbor.y*(roomSize+connectorSize);

                if(x > 0 && y > 0 && x+roomSize < colourMap.length && y+roomSize < colourMap[x].length) {
                    roomMap.put(neighbor, new Room());
                    loadNeighbors(neighbor);
                }
            }
        }
    }

    public void updateRoomColours() {
        for(Map.Entry<RoomOffset, Room> entry : roomMap.entrySet()) {
            int x = startRoomX + entry.getKey().x*(roomSize+connectorSize);
            int y = startRoomY + entry.getKey().y*(roomSize+connectorSize);

            try {
                entry.getValue().colour = colourMap[x][y];
            } catch(Exception e) {}
        }
    }

    private Set<MapDecoration> decorations = new HashSet<>();
    class MapDecoration {
        float roomInPercentX;
        float connInPercentX;
        float roomsOffsetX;
        float connOffsetX;
        float roomInPercentY;
        float connInPercentY;
        float roomsOffsetY;
        float connOffsetY;

        float minU;
        float minV;

        float angle;

        public MapDecoration(float roomInPercentX, float connInPercentX, float roomsOffsetX, float connOffsetX,
                             float roomInPercentY, float connInPercentY, float roomsOffsetY, float connOffsetY, float minU, float minV, float angle) {
            this.roomInPercentX = roomInPercentX;
            this.connInPercentX = connInPercentX;
            this.roomsOffsetX = roomsOffsetX;
            this.connOffsetX = connOffsetX;
            this.roomInPercentY = roomInPercentY;
            this.connInPercentY = connInPercentY;
            this.roomsOffsetY = roomsOffsetY;
            this.connOffsetY = connOffsetY;
            this.minU = minU;
            this.minV = minV;
            this.angle = angle;
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent event) {
        //System.out.println("render overlayw");
        if(event.type == RenderGameOverlayEvent.ElementType.ALL) {
            ItemStack stack = Minecraft.getMinecraft().thePlayer.inventory.mainInventory[8];
            if(NotEnoughUpdates.INSTANCE.colourMap != null || stack != null && stack.getItem() instanceof ItemMap) {
                if(mapTextureData == null) {
                    mapTextureData = dynamicTexture.getTextureData();
                }

                if(NotEnoughUpdates.INSTANCE.colourMap != null) {
                    colourMap = NotEnoughUpdates.INSTANCE.colourMap;
                } else {
                    ItemMap map = (ItemMap) stack.getItem();
                    MapData mapData = map.getMapData(stack, Minecraft.getMinecraft().theWorld);

                    if(mapData == null) return;

                    for (int i = 0; i < 16384; ++i) {
                        int x = i % 128;
                        int y = i / 128;

                        int j = mapData.colors[i] & 255;

                        Color c;
                        if (j / 4 == 0) {
                            c = new Color((i + i / 128 & 1) * 8 + 16 << 24, true);
                        } else {
                            c = new Color(MapColor.mapColorArray[j / 4].func_151643_b(j & 3), true);
                        }

                        colourMap[x][y] = c;
                    }

                    //mapData.
                }

                for(int x=0; x<colourMap.length; x++) {
                    for(int y=0; y<colourMap[x].length; y++) {
                        Color c = colourMap[x][y];
                        if(c.getAlpha() > 80) {
                            if(startRoomX < 0 && startRoomY < 0 && c.getRed() == 0 && c.getGreen() == 124 && c.getBlue() == 0) {
                                roomSize = 0;
                                out:
                                for(int xd=0; xd<=20; xd++) {
                                    for(int yd=0; yd<=20; yd++) {
                                        if(x+xd >= colourMap.length || y+yd >= colourMap[x+xd].length) continue;
                                        Color c2 = colourMap[x+xd][y+yd];

                                        if(c2.getGreen() != 124 || c2.getAlpha() <= 80) {
                                            if(xd < 10 && yd < 10) {
                                                break out;
                                            }
                                        } else {
                                            roomSize = Math.max(roomSize, Math.min(xd+1, yd+1));
                                        }
                                        if(xd == 20 && yd == 20) {
                                            if(roomSize == 0) roomSize = 20;
                                            startRoomX = x;
                                            startRoomY = y;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                for(int i=0; i<roomSize; i++) {
                    for(int k=0; k<4; k++) {
                        for(int j=1; j<8; j++) {
                            int x;
                            int y;

                            if(k == 0) {
                                x = startRoomX+i;
                                y = startRoomY-j;
                            } else if(k == 1) {
                                x = startRoomX+roomSize+j-1;
                                y = startRoomY+i;
                            } else if(k == 2) {
                                x = startRoomX+i;
                                y = startRoomY+roomSize+j-1;
                            } else {
                                x = startRoomX-j;
                                y = startRoomY+i;
                            }

                            if(x < colourMap.length && y < colourMap[x].length) {
                                if(colourMap[x][y].getAlpha() > 80) {
                                    if(j == 1) {
                                        break;
                                    }
                                    connectorSize = Math.min(connectorSize, j-1);
                                }
                            }
                        }
                    }
                }

                loadNeighbors(new RoomOffset(0, 0));
                updateRoomColours();
                for(RoomOffset offset : roomMap.keySet()) {
                    updateRoomConnections(offset);
                }

                if(NotEnoughUpdates.INSTANCE.colourMap == null) {
                    ItemMap map = (ItemMap) stack.getItem();
                    MapData mapData = map.getMapData(stack, Minecraft.getMinecraft().theWorld);

                    if(mapData.mapDecorations.size() > 0) {
                        decorations.clear();
                    }
                    for (Vec4b vec4b : mapData.mapDecorations.values()) {
                        byte b0 = vec4b.func_176110_a();

                        float x = (float)vec4b.func_176112_b() / 2.0F + 64.0F;
                        float y = (float)vec4b.func_176113_c() / 2.0F + 64.0F;
                        float minU = (float)(b0 % 4 + 0) / 4.0F;
                        float minV = (float)(b0 / 4 + 0) / 4.0F;

                        float deltaX = x - startRoomX;
                        float deltaY = y - startRoomY;

                        float roomInPercentX = 0;
                        float connInPercentX = 0;
                        float roomsOffsetX = (int)Math.floor(deltaX / (roomSize+connectorSize));
                        float connOffsetX = (int)Math.floor(deltaX / (roomSize+connectorSize));
                        float xRemainder = deltaX % (roomSize+connectorSize);
                        if(xRemainder > roomSize) {
                            roomsOffsetX++;
                            connInPercentX = (xRemainder-roomSize)/connectorSize;
                        } else {
                            roomInPercentX = xRemainder/roomSize;
                        }
                        float roomInPercentY = 0;
                        float connInPercentY = 0;
                        float roomsOffsetY = (int)Math.floor(deltaY / (roomSize+connectorSize));
                        float connOffsetY = (int)Math.floor(deltaY / (roomSize+connectorSize));
                        float yRemainder = deltaY % (roomSize+connectorSize);
                        if(yRemainder > roomSize) {
                            roomsOffsetY++;
                            connInPercentY = (yRemainder-roomSize)/connectorSize;
                        } else {
                            roomInPercentY = yRemainder/roomSize;
                        }

                        float angle = (float)(vec4b.func_176111_d() * 360) / 16.0F;

                        //System.out.println((float)(vec4b.func_176111_d() * 360) / 16.0F);
                        decorations.add(new MapDecoration(roomInPercentX, connInPercentX, roomsOffsetX, connOffsetX, roomInPercentY, connInPercentY,
                                roomsOffsetY, connOffsetY, minU, minV, angle));
                    }

                }


                //System.out.println("room x: " + startRoomX + "room y: " + startRoomY + " size: " + roomSize + " connector: " + connectorSize);

                //rendering
                for (int i = 0; i < 16384; ++i) {
                    mapTextureData[i] = 0;
                }

                if(!roomMap.isEmpty()) {
                    int minRoomX = 999;
                    int minRoomY = 999;
                    int maxRoomX = -999;
                    int maxRoomY = -999;
                    for(RoomOffset offset : roomMap.keySet()) {
                        minRoomX = Math.min(offset.x, minRoomX);
                        minRoomY = Math.min(offset.y, minRoomY);
                        maxRoomX = Math.max(offset.x, maxRoomX);
                        maxRoomY = Math.max(offset.y, maxRoomY);
                    }

                    for(RoomOffset offset : roomMap.keySet()) {
                        //render(offset, mapTextureData, minRoomX, minRoomY);
                    }

                    render();

                    //process

                    dynamicTexture.updateDynamicTexture();
                    Minecraft.getMinecraft().getTextureManager().loadTexture(dynamicRL, dynamicTexture);

                    GlStateManager.color(1, 1, 1, 1);
                    Minecraft.getMinecraft().getTextureManager().bindTexture(dynamicRL);
                    Utils.drawTexturedRect(0, 0, 128, 128, GL11.GL_NEAREST);
                }
            }
        }
    }

}
