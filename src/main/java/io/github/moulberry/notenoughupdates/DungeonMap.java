package io.github.moulberry.notenoughupdates;

import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

        RoomConnection left = new RoomConnection(RoomConnectionType.NONE, new Color(0, true));
        RoomConnection up = new RoomConnection(RoomConnectionType.NONE, new Color(0, true));
        RoomConnection right = new RoomConnection(RoomConnectionType.NONE, new Color(0, true));
        RoomConnection down = new RoomConnection(RoomConnectionType.NONE, new Color(0, true));

        public void render(int roomSize, int connectorSize) {
            Gui.drawRect(0, 0, roomSize, roomSize, colour.getRGB());
        }
    }
    
    public void render(int[] renderTo, int x, int y, int rgb) {
        int i = x+y*128;
        if(i >= 0 && i < renderTo.length) {
            renderTo[i] = rgb;
        }
    }

    public void render(RoomOffset roomOffset, int[] renderTo, int startOffsetX, int startOffsetY) {
        /*for(Map.Entry<RoomOffset, Room> entry : roomMap.entrySet()) {

        }*/
        if(roomMap.containsKey(roomOffset)) {
            Room room = roomMap.get(roomOffset);

            for(int xo=0; xo<16; xo++) {
                for(int yo=0; yo<16; yo++) {
                    int x = (roomOffset.x-startOffsetX)*20+xo;
                    int y = (roomOffset.y-startOffsetY)*20+yo;

                    render(renderTo, x, y, room.colour.getRGB());
                }
            }

            for(int k=0; k<4; k++) {
                RoomConnection connection;
                if(k == 0) {
                    connection = room.up;
                } else if(k == 1) {
                    connection = room.right;
                } else if(k == 2) {
                    connection = room.down;
                } else {
                    connection = room.left;
                }
                if(connection.type == RoomConnectionType.NONE || connection.type == RoomConnectionType.WALL) continue;
                for(int o1=1; o1<=4; o1++) {
                    int min = 0;
                    int max = 16;
                    if(connection.type == RoomConnectionType.CORRIDOR) {
                        min = 6;
                        max = 10;
                    }
                    for (int o2 = min; o2 < max; o2++) {
                        int x;
                        int y;

                        if(k == 0) {
                            x = (roomOffset.x-startOffsetX)*20+o2;
                            y = (roomOffset.y-startOffsetY)*20-o1;
                        } else if(k == 1) {
                            x = (roomOffset.x-startOffsetX)*20+15+o1;
                            y = (roomOffset.y-startOffsetY)*20+o2;
                        } else if(k == 2) {
                            x = (roomOffset.x-startOffsetX)*20+o2;
                            y = (roomOffset.y-startOffsetY)*20+15+o1;
                        } else {
                            x = (roomOffset.x-startOffsetX)*20-o1;
                            y = (roomOffset.y-startOffsetY)*20+o2;
                        }
                        
                        render(renderTo, x, y, connection.colour.getRGB());
                    }
                }
            }
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
                        render(offset, mapTextureData, minRoomX, minRoomY);
                    }

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
