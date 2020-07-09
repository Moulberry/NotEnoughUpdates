package io.github.moulberry.notenoughupdates;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NEUCape {

    private List<List<Node>> nodes = null;

    int horzNodes = 20;
    float targetDist = 1/30f;

    public void createNodes(EntityPlayer player) {
        nodes = new ArrayList<>();
        for(int i=0; i<50; i++) {
            List<Node> row = new ArrayList<>();
            for(int j=0; j<horzNodes; j++) {
                row.add(new Node(-1, 2-i*targetDist, ((double)j)/(horzNodes-1)));
            }

            nodes.add(row);
        }
        for(int y=0; y<nodes.size(); y++) {
            for(int x=0; x<nodes.get(y).size(); x++) {
                for(Direction dir : Direction.values()) {
                    for(int i=1; i<=2; i++) {
                        Offset offset = new Offset(dir, i);

                        int xNeighbor = x+offset.getXOffset();
                        int yNeighbor = y+offset.getYOffset();

                        if(xNeighbor >= 0 && xNeighbor < nodes.get(y).size()
                            && yNeighbor >= 0 && yNeighbor < nodes.size()) {
                            Node neighbor = nodes.get(yNeighbor).get(xNeighbor);
                            nodes.get(y).get(x).neighbors.put(offset, neighbor);
                        }
                    }
                }
            }
        }
    }

    public void ensureNodesCreated(EntityPlayer player) {
        if(nodes == null) createNodes(player);
    }

    public enum Direction {
        LEFT(-1, 0),
        UP(0, 1),
        RIGHT(1, 0),
        DOWN(0, -1),
        UPLEFT(-1, 1),
        UPRIGHT(1, 1),
        DOWNLEFT(-1, -1),
        DOWNRIGHT(1, -1);

        int xOff;
        int yOff;

        Direction(int xOff, int yOff) {
            this.xOff = xOff;
            this.yOff = yOff;
        }
    }

    public static class Offset {
        Direction direction;
        int steps;

        public Offset(Direction direction, int steps) {
            this.direction = direction;
            this.steps = steps;
        }

        public int getXOffset() {
            return direction.xOff*steps;
        }

        public int getYOffset() {
            return direction.yOff*steps;
        }

        public boolean equals(Object obj) {
            if(obj instanceof Offset) {
                Offset other = (Offset) obj;
                return other.direction == direction && other.steps == steps;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 13*direction.ordinal() + 7*steps;
        }
    }

    public static class Node {
        private Vector3f position;
        private Vector3f acceleration = new Vector3f();
        private HashMap<Offset, Node> neighbors = new HashMap<>();

        public Node(double x, double y, double z) {
            this.position = new Vector3f((float)x, (float)y, (float)z);
        }

        public void updatePosition() {

        }

        public void renderNode() {
            //System.out.println(neighbors.size());
            if(neighbors.containsKey(new Offset(Direction.DOWNRIGHT, 1))) {
                //System.out.println("trying to render");
                Tessellator tessellator = Tessellator.getInstance();
                WorldRenderer worldrenderer = tessellator.getWorldRenderer();
                GlStateManager.color(1F, 1F, 1F, 1F);
                worldrenderer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION);

                Vector3f node2Pos = neighbors.get(new Offset(Direction.DOWN, 1)).position;
                Vector3f node3Pos = neighbors.get(new Offset(Direction.RIGHT, 1)).position;
                Vector3f node4Pos = neighbors.get(new Offset(Direction.DOWNRIGHT, 1)).position;

                worldrenderer.pos(position.x, position.y, position.z).endVertex();
                worldrenderer.pos(node2Pos.x, node2Pos.y, node2Pos.z).endVertex();
                worldrenderer.pos(node3Pos.x, node3Pos.y, node3Pos.z).endVertex();
                worldrenderer.pos(node4Pos.x, node4Pos.y, node4Pos.z).endVertex();

                tessellator.draw();
            }
        }
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Post e) {
        EntityPlayer player = e.entityPlayer;

        ensureNodesCreated(player);
        if(Keyboard.isKeyDown(Keyboard.KEY_R)) createNodes(player);

        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * e.partialRenderTick;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * e.partialRenderTick;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * e.partialRenderTick;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();

        //GL11.glTranslatef(-(float)viewerX, -(float)viewerY, -(float)viewerZ);

        updateCape(player);
        renderCape(player);

        //GL11.glTranslatef((float)viewerX, (float)viewerY, (float)viewerZ);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private void updateCape(EntityPlayer player) {

    }

    private void renderCape(EntityPlayer player) {
        for(int y=0; y<nodes.size()-1; y++) {
            for(int x=0; x<nodes.get(y).size()-1; x++) {
                nodes.get(y).get(x).renderNode();
            }
        }
    }

}
