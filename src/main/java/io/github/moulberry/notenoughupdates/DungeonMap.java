package io.github.moulberry.notenoughupdates;

import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec4b;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class DungeonMap {

    private static final ResourceLocation GREEN_CHECK = new ResourceLocation("notenoughupdates:dungeon_map/green_check.png");
    private static final ResourceLocation WHITE_CHECK = new ResourceLocation("notenoughupdates:dungeon_map/white_check.png");
    private static final ResourceLocation QUESTION = new ResourceLocation("notenoughupdates:dungeon_map/question.png");

    private static final ResourceLocation ROOM_RED = new ResourceLocation("notenoughupdates:dungeon_map/rooms_default/red_room.png");
    private static final ResourceLocation ROOM_BROWN = new ResourceLocation("notenoughupdates:dungeon_map/rooms_default/brown_room.png");
    private static final ResourceLocation ROOM_GRAY = new ResourceLocation("notenoughupdates:dungeon_map/rooms_default/gray_room.png");
    private static final ResourceLocation ROOM_GREEN = new ResourceLocation("notenoughupdates:dungeon_map/rooms_default/green_room.png");
    private static final ResourceLocation ROOM_PINK = new ResourceLocation("notenoughupdates:dungeon_map/rooms_default/pink_room.png");
    private static final ResourceLocation ROOM_PURPLE = new ResourceLocation("notenoughupdates:dungeon_map/rooms_default/purple_room.png");
    private static final ResourceLocation ROOM_YELLOW = new ResourceLocation("notenoughupdates:dungeon_map/rooms_default/yellow_room.png");

    private static final int RENDER_ROOM_SIZE = 16;
    private static final int RENDER_CONN_SIZE = 4;

    private final HashMap<RoomOffset, Room> roomMap = new HashMap<>();
    private Color[][] colourMap = new Color[128][128];
    private int startRoomX = -1;
    private int startRoomY = -1;
    private int connectorSize = 5;
    private int roomSize = 0;
    
    private int roomSizeBlocks = 7;

    private final List<MapDecoration> decorations = new ArrayList<>();
    private final List<MapDecoration> lastDecorations = new ArrayList<>();
    private long lastDecorationsMillis = -1;
    private long lastLastDecorationsMillis = -1;

    private Map<EntityPlayer, MapPosition> playerMapPositions = new HashMap<>();
    
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
            ResourceLocation roomTex = null;
            if(colour.getRed() == 114 && colour.getGreen() == 67 && colour.getBlue() == 27) {
                roomTex = ROOM_BROWN;
            } else if(colour.getRed() == 65 && colour.getGreen() == 65 && colour.getBlue() == 65) {
                roomTex = ROOM_GRAY;
            } else if(colour.getRed() == 0 && colour.getGreen() == 124 && colour.getBlue() == 0) {
                roomTex = ROOM_GREEN;
            } else if(colour.getRed() == 242 && colour.getGreen() == 127 && colour.getBlue() == 165) {
                roomTex = ROOM_PINK;
            } else if(colour.getRed() == 178 && colour.getGreen() == 76 && colour.getBlue() == 216) {
                roomTex = ROOM_PURPLE;
            } else if(colour.getRed() == 255 && colour.getGreen() == 0 && colour.getBlue() == 0) {
                roomTex = ROOM_RED;
            } else if(colour.getRed() == 229 && colour.getGreen() == 229 && colour.getBlue() == 51) {
                roomTex = ROOM_YELLOW;
            }

            if(roomTex != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(roomTex);
                GlStateManager.color(1, 1, 1, 1);
                Utils.drawTexturedRect(0, 0, roomSize, roomSize, GL11.GL_LINEAR);
            }

            //Gui.drawRect(0, 0, roomSize, roomSize, colour.getRGB());
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

        /*Set<Color> uniques = new HashSet<>();
        for(Color[] cs : colourMap) {
            for(Color c : cs) {
                uniques.add(c);
            }
        }
        System.out.println("Unique colours:");
        for(Color c : uniques) {
            System.out.println(c + "" + c.getAlpha());
        }*/

        int centerX = 80;
        int centerY = 80;
        int rotation = (int)Minecraft.getMinecraft().thePlayer.rotationYawHead;

        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.rotate(-rotation+180, 0, 0, 1);
        GlStateManager.translate(-(maxRoomX-minRoomX+1)*(RENDER_ROOM_SIZE+RENDER_CONN_SIZE)/2f,
                -(maxRoomY-minRoomY+1)*(RENDER_ROOM_SIZE+RENDER_CONN_SIZE)/2f, 0);

        Gui.drawRect(-10, -10, (maxRoomX-minRoomX)*(RENDER_ROOM_SIZE+RENDER_CONN_SIZE)+RENDER_ROOM_SIZE+10,
                (maxRoomY-minRoomY)*(RENDER_ROOM_SIZE+RENDER_CONN_SIZE)+RENDER_ROOM_SIZE+10,
                new Color(200, 200, 200).getRGB());

        for(Map.Entry<RoomOffset, Room> entry : roomMap.entrySet()) {
            RoomOffset roomOffset = entry.getKey();
            Room room = entry.getValue();

            int x = (roomOffset.x-minRoomX)*(RENDER_ROOM_SIZE+RENDER_CONN_SIZE);
            int y = (roomOffset.y-minRoomY)*(RENDER_ROOM_SIZE+RENDER_CONN_SIZE);

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 0);
            room.render(RENDER_ROOM_SIZE, RENDER_CONN_SIZE);
            GlStateManager.translate(-x, -y, 0);
            GlStateManager.popMatrix();
        }

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.color(1, 1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(mapIcons);
        int k = 0;
        for(int i=0; i<decorations.size(); i++) {
            MapDecoration decoration = decorations.get(i);
            float minU = (float)(decoration.id % 4) / 4.0F;
            float minV = (float)(decoration.id / 4) / 4.0F;

            float x = decoration.position.getRenderX();
            float y = decoration.position.getRenderY();
            float angle = decoration.angle;

            if(decoration.id == 1) {
                angle = Minecraft.getMinecraft().thePlayer.rotationYawHead;
            } else {
                if(false && i < lastDecorations.size()) {
                    MapDecoration last = lastDecorations.get(i);
                    float xLast = last.position.getRenderX();
                    float yLast = last.position.getRenderY();

                    float distSq = (x-xLast)*(x-xLast)+(y-yLast)*(y-yLast);
                    if(distSq < RENDER_ROOM_SIZE*RENDER_ROOM_SIZE) {
                        float angleLast = last.angle;
                        if(angle > 180 && angleLast < 180) angleLast += 360;
                        if(angleLast > 180 && angle < 180) angle += 360;

                        float interpFactor = Math.round((System.currentTimeMillis() - lastDecorationsMillis)*100f)/100f/(lastDecorationsMillis - lastLastDecorationsMillis);
                        interpFactor = Math.max(0, Math.min(1, interpFactor));

                        x = xLast+(x - xLast)*interpFactor;
                        y = yLast+(y - yLast)*interpFactor;
                        angle = angleLast+(angle - angleLast)*interpFactor;
                        angle %= 360;
                    }
                }
            }

            if(decoration.id == 3 || decoration.id == 1) {
                float closestDistSq = RENDER_ROOM_SIZE*RENDER_ROOM_SIZE;
                EntityPlayer closestPlayer = null;
                for(Map.Entry<EntityPlayer, MapPosition> entry : playerMapPositions.entrySet()) {
                    if(Minecraft.getMinecraft().thePlayer.getName().equalsIgnoreCase(entry.getKey().getName()) != (decoration.id == 1)) {
                        continue;
                    }

                    float playerX = entry.getValue().getRenderX();
                    float playerY = entry.getValue().getRenderY();

                    float distSq = (playerX-x)*(playerX-x) + (playerY-y)*(playerY-y);

                    if(distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closestPlayer = entry.getKey();
                    }
                }

                if(closestPlayer != null) {
                    x = playerMapPositions.get(closestPlayer).getRenderX();
                    y = playerMapPositions.get(closestPlayer).getRenderY();
                    angle = closestPlayer.rotationYawHead;
                }
            }

            x -= minRoomX*(RENDER_ROOM_SIZE+RENDER_CONN_SIZE);
            y -= minRoomY*(RENDER_ROOM_SIZE+RENDER_CONN_SIZE);

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, -0.02F);
            GlStateManager.rotate(angle, 0.0F, 0.0F, 1.0F);
            GlStateManager.scale(4.0F, 4.0F, 3.0F);
            GlStateManager.translate(-0.125F, 0.125F, 0.0F);
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
            worldrenderer.pos(-1.0D, 1.0D, 10+((float)k * -0.001F)).tex(minU, minV).endVertex();
            worldrenderer.pos(1.0D, 1.0D, 10+((float)k * -0.001F)).tex(minU+1/4f, minV).endVertex();
            worldrenderer.pos(1.0D, -1.0D, 10+((float)k * -0.001F)).tex(minU+1/4f, minV+1/4f).endVertex();
            worldrenderer.pos(-1.0D, -1.0D, 10+((float)k * -0.001F)).tex(minU, minV+1/4f).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
            k--;
        }

        GlStateManager.rotate(rotation-180, 0, 0, 1);
        GlStateManager.translate(-centerX, -centerY, 0);
        GlStateManager.popMatrix();
    }


    public void updateRoomConnections(RoomOffset roomOffset) {
        if(roomMap.containsKey(roomOffset)) {
            Room room = roomMap.get(roomOffset);

            int otherPixelFilled = 0;
            int otherPixelColour = 0;
            for(int xOff=0; xOff<roomSize; xOff++) {
                for(int yOff=0; yOff<roomSize; yOff++) {
                    int x = startRoomX + roomOffset.x*(roomSize+connectorSize) + xOff;
                    int y = startRoomY + roomOffset.y*(roomSize+connectorSize) + yOff;

                    if(x > 0 && y > 0 && x < colourMap.length && y < colourMap[x].length) {
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

                        if(x > 0 && y > 0 && x < colourMap.length && y < colourMap[x].length) {
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

            room.fillCorner = false;
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
                    if(colourMap[x][y].getAlpha() > 100) {
                        roomMap.put(neighbor, new Room());
                        loadNeighbors(neighbor);
                    }
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

    class MapDecoration {
        MapPosition position;
        int id;
        float angle;

        public MapDecoration(MapPosition position, int id, float angle) {
            this.position = position;
            this.id = id;
            this.angle = angle;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MapDecoration that = (MapDecoration) o;
            return id == that.id &&
                    Float.compare(that.angle, angle) == 0 &&
                    Objects.equals(position, that.position);
        }

        @Override
        public int hashCode() {
            return Objects.hash(position, id, angle);
        }
    }

    private class MapPosition {
        public float roomOffsetX;
        public float connOffsetX;

        public float roomOffsetY;
        public float connOffsetY;

        public MapPosition(float roomOffsetX, float connOffsetX, float roomOffsetY, float connOffsetY) {
            this.roomOffsetX = roomOffsetX;
            this.connOffsetX = connOffsetX;
            this.roomOffsetY = roomOffsetY;
            this.connOffsetY = connOffsetY;
        }

        public float getRenderX() {
            return roomOffsetX*RENDER_ROOM_SIZE + connOffsetX*RENDER_CONN_SIZE;
        }

        public float getRenderY() {
            return roomOffsetY*RENDER_ROOM_SIZE + connOffsetY*RENDER_CONN_SIZE;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MapPosition that = (MapPosition) o;
            return Float.compare(that.roomOffsetX, roomOffsetX) == 0 &&
                    Float.compare(that.connOffsetX, connOffsetX) == 0 &&
                    Float.compare(that.roomOffsetY, roomOffsetY) == 0 &&
                    Float.compare(that.connOffsetY, connOffsetY) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(roomOffsetX, connOffsetX, roomOffsetY, connOffsetY);
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent event) {
        if(event.type == RenderGameOverlayEvent.ElementType.ALL) {
            ItemStack stack = Minecraft.getMinecraft().thePlayer.inventory.mainInventory[8];
            if(NotEnoughUpdates.INSTANCE.colourMap != null || (stack != null && stack.getItem() instanceof ItemMap)) {
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

                roomMap.clear();
                startRoomX = -1;
                startRoomY = -1;
                connectorSize = 5;
                roomSize = 0;

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

                            if(x > 0 && y > 0 && x < colourMap.length && y < colourMap[x].length) {
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

                List<Integer> dists = new ArrayList<>();
                int currentBlockCount = 0;
                for(int i=0; i<300; i++) {
                    IBlockState state = Minecraft.getMinecraft().theWorld.getBlockState(new BlockPos(0, 99, i));
                    if(state == null || state.getBlock() == Blocks.air) {
                        if(currentBlockCount > 0) dists.add(currentBlockCount);
                        currentBlockCount = 0;
                    } else {
                        currentBlockCount++;
                    }
                }
                //roomSizeBlocks = 7;
                currentBlockCount = 0;
                for(int i=0; i<300; i++) {
                    IBlockState state = Minecraft.getMinecraft().theWorld.getBlockState(new BlockPos(i, 99, 0));
                    if(state == null || state.getBlock() == Blocks.air) {
                        if(currentBlockCount > 0) dists.add(currentBlockCount);
                        currentBlockCount = 0;
                    } else {
                        currentBlockCount++;
                    }
                }
                int count = 0;
                int mostCommonDist = -1;
                for(int dist : dists) {
                    if(dist == mostCommonDist) {
                        count++;
                    } else {
                        if(--count < 0) {
                            count = 1;
                            mostCommonDist = dist;
                        }
                    }
                }
                if(mostCommonDist != -1) roomSizeBlocks = Math.max(31, mostCommonDist);
                if(Keyboard.isKeyDown(Keyboard.KEY_N)) System.out.println(roomSizeBlocks + ":" + dists.size());

                Set<String> actualPlayers = new HashSet<>();
                for(ScorePlayerTeam team : Minecraft.getMinecraft().thePlayer.getWorldScoreboard().getTeams()) {
                    if(team.getTeamName().startsWith("a")) {
                        for(String player : team.getMembershipCollection()) {
                            actualPlayers.add(player.toLowerCase());
                        }
                    }
                }

                playerMapPositions.clear();
                for(EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
                    if(actualPlayers.isEmpty() || actualPlayers.contains(player.getName().toLowerCase())) {
                        float roomX = (float)player.posX / (roomSizeBlocks+1);
                        float roomY = (float)player.posZ / (roomSizeBlocks+1);

                        float playerRoomOffsetX = (float) Math.floor(roomX);
                        float playerConnOffsetX = (float) Math.floor(roomX);
                        float playerRoomOffsetY = (float) Math.floor(roomY);
                        float playerConnOffsetY = (float) Math.floor(roomY);

                        float roomXInBlocks = (float)player.posX % (roomSizeBlocks+1);
                        if(roomXInBlocks < 2) { //0,1
                            playerConnOffsetX -= roomXInBlocks/5f;
                        } else if(roomXInBlocks > roomSizeBlocks-3) { //31,30,29
                            playerRoomOffsetX++;
                            playerConnOffsetX += (roomXInBlocks - (roomSizeBlocks-3))/5f;
                        } else {
                            playerRoomOffsetX += (roomXInBlocks-2) / (roomSizeBlocks-5);
                        }

                        float roomYInBlocks = (float)player.posZ % (roomSizeBlocks+1);
                        if(roomYInBlocks < 2) { //0,1
                            playerConnOffsetY -= roomYInBlocks/5f;
                        } else if(roomYInBlocks > roomSizeBlocks-3) { //31,30,29
                            playerRoomOffsetY++;
                            playerConnOffsetY += (roomYInBlocks - (roomSizeBlocks-3))/5f;
                        } else {
                            playerRoomOffsetY += (roomYInBlocks-2) / (roomSizeBlocks-5);
                        }

                        playerRoomOffsetX -= startRoomX/(roomSize+connectorSize);
                        playerRoomOffsetY -= startRoomY/(roomSize+connectorSize);
                        playerConnOffsetX -= startRoomX/(roomSize+connectorSize);
                        playerConnOffsetY -= startRoomY/(roomSize+connectorSize);

                        playerMapPositions.put(player, new MapPosition(playerRoomOffsetX, playerConnOffsetX, playerRoomOffsetY, playerConnOffsetY));
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
                        boolean different = mapData.mapDecorations.size() != decorations.size();

                        List<MapDecoration> decorationsNew = new ArrayList<>();

                        for (Vec4b vec4b : mapData.mapDecorations.values()) {
                            byte b0 = vec4b.func_176110_a();

                            float x = (float)vec4b.func_176112_b() / 2.0F + 64.0F;
                            float y = (float)vec4b.func_176113_c() / 2.0F + 64.0F;

                            float deltaX = x - startRoomX;
                            float deltaY = y - startRoomY;

                            float roomsOffsetX = (int)Math.floor(deltaX / (roomSize+connectorSize));
                            float connOffsetX = (int)Math.floor(deltaX / (roomSize+connectorSize));
                            float xRemainder = deltaX % (roomSize+connectorSize);
                            if(Math.abs(xRemainder) > roomSize) {
                                roomsOffsetX++;
                                connOffsetX += (xRemainder-roomSize)/connectorSize;
                            } else {
                                roomsOffsetX += xRemainder/roomSize;
                            }
                            if(deltaX < 0) {
                                roomsOffsetX++;
                                connOffsetX++;
                            }
                            float roomsOffsetY = (int)Math.floor(deltaY / (roomSize+connectorSize));
                            float connOffsetY = (int)Math.floor(deltaY / (roomSize+connectorSize));
                            float yRemainder = deltaY % (roomSize+connectorSize);
                            if(Math.abs(yRemainder) > roomSize) {
                                roomsOffsetY++;
                                connOffsetY += Math.abs(yRemainder-roomSize)/connectorSize;
                            } else {
                                roomsOffsetY += yRemainder/roomSize;
                            }
                            if(deltaY < 0) {
                                roomsOffsetY++;
                                connOffsetY++;
                            }

                            float angle = (float)(vec4b.func_176111_d() * 360) / 16.0F;

                            MapDecoration decoration = new MapDecoration(new MapPosition(roomsOffsetX, connOffsetX, roomsOffsetY, connOffsetY), (int)b0, angle);
                            if(!different && !decorations.contains(decoration)) {
                                different = true;
                            }
                            decorationsNew.add(decoration);
                        }

                        if(different) {
                            lastDecorations.clear();

                            for(int i=0; i<decorations.size() && i<decorationsNew.size(); i++) {
                                MapDecoration match = decorationsNew.get(i);

                                float lowestDistSq = 999;
                                MapDecoration closest = null;

                                for(int j=0; j<decorations.size(); j++) {
                                    MapDecoration old = decorations.get(j);

                                    if(old.id != match.id) continue;

                                    float xOff = (old.position.roomOffsetX*RENDER_ROOM_SIZE+old.position.connOffsetX*RENDER_CONN_SIZE) -
                                            (match.position.roomOffsetX*RENDER_ROOM_SIZE+match.position.connOffsetX*RENDER_CONN_SIZE);
                                    float yOff = (old.position.roomOffsetY*RENDER_ROOM_SIZE+old.position.connOffsetY*RENDER_CONN_SIZE) -
                                            (match.position.roomOffsetY*RENDER_ROOM_SIZE+match.position.connOffsetY*RENDER_CONN_SIZE);
                                    float distSq = xOff*xOff + yOff*yOff;
                                    if(distSq < lowestDistSq) {
                                        lowestDistSq = distSq;
                                        closest = old;
                                    }
                                }

                                if(closest != null) {
                                    lastDecorations.add(closest);
                                }
                            }

                            decorations.clear();
                            decorations.addAll(decorationsNew);

                            lastLastDecorationsMillis = lastDecorationsMillis;
                            lastDecorationsMillis = System.currentTimeMillis();
                        }
                    }

                }

                if(!roomMap.isEmpty()) {
                    render();
                }
            }
        }
    }

}
