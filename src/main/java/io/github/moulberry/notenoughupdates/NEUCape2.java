package io.github.moulberry.notenoughupdates;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.command.CommandBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NEUCape2 {

    //private ResourceLocation capeImageLocation = new ResourceLocation(Morus.MODID, "cape.jpg");
    //private SimpleTexture capeTexture;

    private long millisLastRenderUpdate = 0;

    private int horzNodes = 20;
    private double targetDist = 1/30.0;

    private EntityPlayer player = null;

    private double vertOffset = 1.4;
    private double shoulderLength = 0.3;
    private double shoulderWidth = 0.13;
    private double crouchWidthOffset = -0.05;
    private double maxCrouchOffset = 0.35;

    private double resistance = 0.08;
    private double gravity = 0.04;
    private int steps = 10;

    private List<List<Node>> nodes = new ArrayList<>();

    /*private void reloadCapeImage() {
        if(capeTexture != null) {
            capeTexture.deleteGlTexture();
        }
        capeTexture = new SimpleTexture(capeImageLocation);
        try {
            capeTexture.loadTexture(Minecraft.getMinecraft().getResourceManager());
        } catch(IOException e) {
            e.printStackTrace();
        }
    }*/

    private void resetNodes(EntityPlayer player) {
        nodes.clear();
        for(int i=0; i<50; i++) {
            List<Node> list = new ArrayList<>();
            for(int j=0; j<horzNodes; j++) {
                if(horzNodes == 1) {
                    list.add(new Node(player.posX-1, player.posY+2-i*targetDist, player.posZ, i, j));
                } else if(horzNodes > 1) {
                    list.add(new Node(player.posX-1, player.posY+2-i*targetDist, player.posZ+((double)j)/(horzNodes-1), i, j));
                }
            }

            nodes.add(list);
        }
    }

    class Node {
        public int iIndex;
        public int jIndex;

        public boolean fixed = false;

        public double x;
        public double y;
        public double z;
        public double xOld;
        public double yOld;
        public double zOld;
        public double aX;
        public double aY;
        public double aZ;

        public double normalX;
        public double normalY;
        public double normalZ;

        public Node(double x, double y, double z, int iIndex, int jIndex) {
            this.x = xOld = x;
            this.y = xOld = y;
            this.z = xOld = z;
            this.iIndex = iIndex;
            this.jIndex = jIndex;
        }

        private void updateNormal(Node up, Node left, Node right, Node down, Node up2, Node left2, Node right2, Node down2) {
            Vec3 normal1 = normal(up, left);
            Vec3 normal2 = normal(right, up);
            Vec3 normal3 = normal(down, right);
            Vec3 normal4 = normal(left, down);
            Vec3 normal5 = normal(up2, left2);
            Vec3 normal6 = normal(right2, up2);
            Vec3 normal7 = normal(down2, right2);
            Vec3 normal8 = normal(left2, down2);

            Vec3 avgNormal = normal1.add(normal2).add(normal3).add(normal4)
                    .add(normal5).add(normal6).add(normal7).add(normal8).normalize();

            normalX = avgNormal.xCoord;
            normalY = avgNormal.yCoord;
            normalZ = avgNormal.zCoord;
        }

        private Vec3 normal(Node node1, Node node2) {
            if(node1 == null || node2 == null) {
                return new Vec3(0,0,0);
            }
            Vec3 thisNode = node2vec(this);
            Vec3 node1Vec = node2vec(node1);
            Vec3 node2Vec = node2vec(node2);

            Vec3 thisTo1 = node1Vec.subtract(thisNode);
            Vec3 thisTo2 = node2Vec.subtract(thisNode);

            return thisTo1.crossProduct(thisTo2);

        }

        public void update(double pX, double pY, double pZ, EntityPlayer player) {
            if(fixed) {
                return;
            }

            double xTemp = x;
            double yTemp = y;
            double zTemp = z;

            double res = resistance;

            BlockPos pos = new BlockPos(
                    MathHelper.floor_double(x),
                    MathHelper.floor_double(y),
                    MathHelper.floor_double(z));
            Block block = Minecraft.getMinecraft().theWorld.getBlockState(pos).getBlock();
            if(block.getMaterial().isLiquid()) {
                aX /= 5;
                aY /= 5;
                aZ /= 5;

                res = Math.sqrt(res);
            }

            double xDiff = x-xOld;
            double yDiff = y-yOld;
            double zDiff = z-zOld;

            xDiff = MathHelper.clamp_double(xDiff, -0.5, 0.5);
            yDiff = MathHelper.clamp_double(yDiff, -0.5, 0.5);
            zDiff = MathHelper.clamp_double(zDiff, -0.5, 0.5);

            x = x + xDiff*(1-res)+aX*0.2;
            y = y + yDiff*(1-res)+aY*0.2;
            z = z + zDiff*(1-res)+aZ*0.2;

            resolvePlayerCollision(pX, pY, pZ, player);

            if(!checkCollision(xTemp, yTemp, zTemp)) {
                xOld = xTemp;
                yOld = yTemp;
                zOld = zTemp;
            }

            if(checkCollision(x, y, z)) {
                updateFromBoundingBox();
            }

            aX = 0;
            aY = 0;
            aZ = 0;
        }

        public boolean resolvePlayerCollision(double pX, double pY, double pZ, EntityPlayer player) {
            double angle = Math.toRadians(player.renderYawOffset);

            double offset = 0;

            if(player.getCurrentArmor(1) != null) {
                if(player.isSneaking()) {
                    offset += 0.15;
                } else {
                    offset += 0.06;
                }
            }

            if(player.isSneaking()) {
                offset -= crouchWidthOffset;

                double dY = y - player.posY;

                if(dY < 0.65) {
                    offset += maxCrouchOffset;
                } else if(dY < 1.2) {
                    offset += maxCrouchOffset*(1.2-dY)/0.55;
                }
            }

            double x1 = pX+Math.cos(angle)*2-Math.cos(angle+Math.PI/2)*(shoulderWidth+offset);
            double z1 = pZ+Math.sin(angle)*2-Math.sin(angle+Math.PI/2)*(shoulderWidth+offset);
            double x2 = pX-Math.cos(angle)*2-Math.cos(angle+Math.PI/2)*(shoulderWidth+offset);
            double z2 = pZ-Math.sin(angle)*2-Math.sin(angle+Math.PI/2)*(shoulderWidth+offset);

            boolean crossed = ((x2 - x1)*(z - z1) < (z2 - z1)*(x - x1));

            if(crossed) {
                double dot1 = ((x-x2)*(x1-x2)+(z-z2)*(z1-z2));
                double dot2 = (x1-x2)*(x1-x2)+(z1-z2)*(z1-z2);
                double k = dot1/dot2;

                x = xOld = (x1-x2)*k+x2;
                z = zOld = (z1-z2)*k+z2;

                return true;
            }
            return false;
        }

        public void updateFromBoundingBox() {
            BlockPos pos = new BlockPos(
                    MathHelper.floor_double(x),
                    MathHelper.floor_double(y),
                    MathHelper.floor_double(z));
            Block block = Minecraft.getMinecraft().theWorld.getBlockState(pos).getBlock();
            block.setBlockBoundsBasedOnState(Minecraft.getMinecraft().theWorld, pos);
            AxisAlignedBB bb = block.getSelectedBoundingBox(Minecraft.getMinecraft().theWorld, pos);

            Vec3 center = new Vec3((bb.minX + bb.maxX) / 2, (bb.minY + bb.maxY) / 2, (bb.minZ + bb.maxZ) / 2);
            MovingObjectPosition mop = bb.calculateIntercept(center.add(new Vec3(x, y, z).subtract(center).normalize()), center);

            if(mop == null) {
                return;
            }

            Vec3 vec = mop.hitVec;

            if(vec == null) {
                return;
            }

            double dX = vec.xCoord - x;
            double dY = vec.yCoord - y;
            double dZ = vec.zCoord - z;
            double adX = Math.abs(dX);
            double adY = Math.abs(dY);
            double adZ = Math.abs(dZ);

            double tot = adX + adY + adZ;

            //Simulate a little bit of friction
            if(tot < 0.15 || checkCollision(vec.xCoord, vec.yCoord, vec.zCoord)) {
                x = xOld;
                y = yOld;
                z = zOld;
                return;
            }

            //>0.3 check reduces the movement at corners a little bit
            if(adX/tot > 0.3) x = xOld = vec.xCoord;
            if(adY/tot > 0.3) y = yOld = vec.yCoord;
            if(adZ/tot > 0.3) z = zOld = vec.zCoord;
        }

        public boolean checkCollision(double x, double y, double z) {
            BlockPos pos = new BlockPos(
                    MathHelper.floor_double(x),
                    MathHelper.floor_double(y),
                    MathHelper.floor_double(z));
            Block block = Minecraft.getMinecraft().theWorld.getBlockState(pos).getBlock();

            if(block.getMaterial().isSolid()) {
                block.setBlockBoundsBasedOnState(Minecraft.getMinecraft().theWorld, pos);
                AxisAlignedBB bb = block.getSelectedBoundingBox(Minecraft.getMinecraft().theWorld, pos);

                return bb.isVecInside(new Vec3(x, y, z));
            } else {
                return false;
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if(Minecraft.getMinecraft().theWorld == null || player == null) {
            return;
        }

        long delta = System.currentTimeMillis() - millisLastRenderUpdate;

        double lagFactor = delta/(1000/60.0);
        if(lagFactor > 3) {
            lagFactor = 3;
        }

        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * e.renderTickTime;
        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * e.renderTickTime;
        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * e.renderTickTime;

        updateFixedNodes(playerX, playerY, playerZ, player);

        for(List<Node> nodes2 : nodes) {
            for(Node node : nodes2) {
                node.aY -= gravity*lagFactor;
                node.update(playerX, playerY, playerZ, player);
            }
        }
        for(int step=0; step<steps*lagFactor; step++) {
            for(int i=0; i<nodes.size(); i++) {
                for (int j = 0; j < horzNodes; j++) {
                    Node node = nodes.get(i).get(j);
                    List<Node> struct = new ArrayList<>();
                    List<Node> shear = new ArrayList<>();
                    List<Node> bend = new ArrayList<>();

                    if(i+1 < nodes.size()) struct.add(nodes.get(i+1).get(j));
                    if(j+1 < horzNodes) struct.add(nodes.get(i).get(j+1));
                    if(i-1 >= 0) struct.add(nodes.get(i-1).get(j));
                    if(j-1 >= 0) struct.add(nodes.get(i).get(j-1));

                    if(i+1 < nodes.size() && j+1 < horzNodes) shear.add(nodes.get(i+1).get(j+1));
                    if(i+1 < nodes.size() && j-1 >= 0) shear.add(nodes.get(i+1).get(j-1));
                    if(i-1 >= 0 && j+1 < horzNodes) shear.add(nodes.get(i-1).get(j+1));
                    if(i-1 >= 0 && j-1 >= 0) shear.add(nodes.get(i-1).get(j-1));

                    if(i+2 < nodes.size()) bend.add(nodes.get(i+2).get(j));
                    if(j+2 < horzNodes) bend.add(nodes.get(i).get(j+2));
                    if(i-2 >= 0) bend.add(nodes.get(i-2).get(j));
                    if(j-2 >= 0) bend.add(nodes.get(i).get(j-2));

                    try {
                        updateNode(node, struct, shear, bend);
                    } catch(Exception ex) {

                    }
                }
            }
        }
        for(int i=0; i<nodes.size(); i++) {
            for (int j = 0; j < horzNodes; j++) {
                Node up = null, down = null, left = null, right = null;
                Node up2 = null, down2 = null, left2 = null, right2 = null;

                if(i+1 < nodes.size()) down = nodes.get(i+1).get(j);
                if(j+1 < horzNodes) right = nodes.get(i).get(j+1);
                if(i-1 >= 0) up = nodes.get(i-1).get(j);
                if(j-1 >= 0) left = nodes.get(i).get(j-1);

                if(i+2 < nodes.size()) down2 = nodes.get(i+2).get(j);
                if(j+2 < horzNodes) right2 = nodes.get(i).get(j+2);
                if(i-2 >= 0) up2 = nodes.get(i-2).get(j);
                if(j-2 >= 0) left2 = nodes.get(i).get(j-2);

                nodes.get(i).get(j).updateNormal(up, left, right, down, up2, left2, right2, down2);
            }
        }

        millisLastRenderUpdate = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Post e) {
        EntityPlayer player = e.entityPlayer;

        if(!player.getName().equalsIgnoreCase("Moulberry")) {
            return;
        }

        if(nodes.size() == 0) {
            resetNodes(player);
        }

        this.player = player;

        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();

        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * e.partialRenderTick;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * e.partialRenderTick;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * e.partialRenderTick;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GlStateManager.enableTexture2D();
        int currTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        //GL11.glBindTexture(GL11.GL_TEXTURE_2D, capeTexture.getGlTextureId());

        //ShaderManager shaderManager = ShaderManager.getInstance();

        //shaderManager.loadShader("cape");

        for(int i=0; i<nodes.size(); i++) {
            for(int j=0; j<horzNodes; j++) {
                Node node = nodes.get(i).get(j);
                if(i+1 < nodes.size() && j+1 < horzNodes) {
                    GlStateManager.color(1F, 1F, 1F, 1F);
                    renderNodeConnection(viewerX, viewerY, viewerZ, node,
                            nodes.get(i+1).get(j), nodes.get(i).get(j+1),
                            nodes.get(i+1).get(j+1), true);
                    GlStateManager.color(0.1F, 0.1F, 0.1F, 1F);
                    renderNodeConnection(viewerX, viewerY, viewerZ, node,
                            nodes.get(i+1).get(j), nodes.get(i).get(j+1),
                            nodes.get(i+1).get(j+1), false);
                }
            }
        }

        GlStateManager.color(0.1F, 0.1F, 0.1F, 1F);
        for(int i=0; i<nodes.size(); i++) {
            if(i+1 < nodes.size()) {
                renderSideConnection(viewerX, viewerY, viewerZ,
                        nodes.get(i).get(0), nodes.get(i+1).get(0));
                renderSideConnection(viewerX, viewerY, viewerZ,
                        nodes.get(i).get(horzNodes-1), nodes.get(i+1).get(horzNodes-1));
            }
        }

        for(int j=0; j<horzNodes; j++) {
            if(j+1 < horzNodes) {
                renderSideConnection(viewerX, viewerY, viewerZ,
                        nodes.get(0).get(j), nodes.get(0).get(j+1));
                renderSideConnection(viewerX, viewerY, viewerZ,
                        nodes.get(nodes.size()-1).get(j), nodes.get(nodes.size()-1).get(j+1));
            }
        }

        GL20.glUseProgram(0);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, currTex);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private Vec3 node2vec(Node node) {
        return new Vec3(node.x, node.y, node.z);
    }

    private void renderSideConnection(double pX, double pY, double pZ, Node node1, Node node2) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        worldrenderer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_NORMAL);

        worldrenderer.pos(node1.x-pX, node1.y-pY, node1.z-pZ)
                .normal((float)node1.normalX, (float)node1.normalY, (float)node1.normalZ).endVertex();
        worldrenderer.pos(node2.x-pX, node2.y-pY, node2.z-pZ)
                .normal((float)node2.normalX, (float)node2.normalY, (float)node2.normalZ).endVertex();
        worldrenderer.pos(node1.x-pX+node1.normalX/15, node1.y-pY+node1.normalY/15, node1.z-pZ+node1.normalZ/15)
                .normal((float)node1.normalX, (float)node1.normalY, (float)node1.normalZ).endVertex();
        worldrenderer.pos(node2.x-pX+node2.normalX/15, node2.y-pY+node2.normalY/15, node2.z-pZ+node2.normalZ/15)
                .normal((float)node2.normalX, (float)node2.normalY, (float)node2.normalZ).endVertex();

        tessellator.draw();
    }

    private void renderNodeConnection(double pX, double pY, double pZ, Node node1, Node node2,
                                      Node node3, Node node4, boolean offset) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        //Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(node1.normalY + " " + node2.normalY + " " + node3.normalY + " " + node4.normalY));

        if(offset) {
            worldrenderer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_TEX_NORMAL);

            worldrenderer.pos(node1.x-pX+node1.normalX/15, node1.y-pY+node1.normalY/15, node1.z-pZ+node1.normalZ/15)
                    .tex(((double)node1.jIndex)/(horzNodes-1), ((double)node1.iIndex)/(nodes.size()-1))
                    .normal((float)node1.normalX, (float)node1.normalY, (float)node1.normalZ).endVertex();
            worldrenderer.pos(node2.x-pX+node2.normalX/15, node2.y-pY+node2.normalY/15, node2.z-pZ+node2.normalZ/15)
                    .tex(((double)node2.jIndex)/(horzNodes-1), ((double)node2.iIndex)/(nodes.size()-1))
                    .normal((float)node2.normalX, (float)node2.normalY, (float)node2.normalZ).endVertex();
            worldrenderer.pos(node3.x-pX+node3.normalX/15, node3.y-pY+node3.normalY/15, node3.z-pZ+node3.normalZ/15)
                    .tex(((double)node3.jIndex)/(horzNodes-1), ((double)node3.iIndex)/(nodes.size()-1))
                    .normal((float)node3.normalX, (float)node3.normalY, (float)node3.normalZ).endVertex();
            worldrenderer.pos(node4.x-pX+node4.normalX/15, node4.y-pY+node4.normalY/15, node4.z-pZ+node4.normalZ/15)
                    .tex(((double)node4.jIndex)/(horzNodes-1), ((double)node4.iIndex)/(nodes.size()-1))
                    .normal((float)node4.normalX, (float)node4.normalY, (float)node4.normalZ).endVertex();

        } else {
            worldrenderer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_NORMAL);

            worldrenderer.pos(node1.x-pX, node1.y-pY, node1.z-pZ)
                    .normal((float)node1.normalX, (float)node1.normalY, (float)node1.normalZ).endVertex();
            worldrenderer.pos(node2.x-pX, node2.y-pY, node2.z-pZ)
                    .normal((float)node2.normalX, (float)node2.normalY, (float)node2.normalZ).endVertex();
            worldrenderer.pos(node3.x-pX, node3.y-pY, node3.z-pZ)
                    .normal((float)node3.normalX, (float)node3.normalY, (float)node3.normalZ).endVertex();
            worldrenderer.pos(node4.x-pX, node4.y-pY, node4.z-pZ)
                    .normal((float)node4.normalX, (float)node4.normalY, (float)node4.normalZ).endVertex();
        }

        tessellator.draw();
    }

    private Vec3 scale(Vec3 vector, double amount) {
        return new Vec3(vector.xCoord * amount, vector.yCoord * amount, vector.zCoord * amount);
    }

    private void updateNode(Node node, List<Node> struct, List<Node> shear, List<Node> bend) {
        double shearDist = 1.414*targetDist;
        double bendDist = 2*targetDist; //Potentially differentiate between corners?

        for(Node bendNode : bend) {
            resolve(node, bendNode, bendDist);
        }

        for(Node shearNode : shear) {
            resolve(node, shearNode, shearDist);
        }

        for(Node structNode : struct) {
            resolve(node, structNode, targetDist);
        }
    }

    public void resolve(Node node1, Node node2, double targetDist) {
        double dX = node1.x - node2.x;
        double dY = node1.y - node2.y;
        double dZ = node1.z - node2.z;

        double distSq = dX*dX + dY*dY + dZ*dZ;
        double dist = Math.sqrt(distSq);

        dX *= (1 - targetDist/dist)*0.5;
        dY *= (1 - targetDist/dist)*0.5;
        dZ *= (1 - targetDist/dist)*0.5;

        if(node1.fixed || node2.fixed) {
            dX *= 2;
            dY *= 2;
            dZ *= 2;
        }

        if(!node1.fixed) {
            node1.x -= dX;
            node1.y -= dY;
            node1.z -= dZ;
        }

        if(!node2.fixed) {
            node2.x += dX;
            node2.y += dY;
            node2.z += dZ;
        }
    }

    private void updateFixedNodes(double pX, double pY, double pZ, EntityPlayer player) {
        double angle = Math.toRadians(player.renderYawOffset);

        double shoulderWidth2 = shoulderWidth + (player.isSneaking()?crouchWidthOffset:0);
        if(player.getCurrentArmor(1) != null || player.getCurrentArmor(2) != null) {
            if(player.isSneaking()) {
                shoulderWidth2 += 0.15;
            } else {
                shoulderWidth2 += 0.06;
            }
        }

        Node node = nodes.get(0).get(0);
        node.x = pX+Math.cos(angle)*shoulderLength-Math.cos(angle+Math.PI/2)*shoulderWidth2;
        node.y = pY+vertOffset-(player.isSneaking()?0.2:0);
        node.z = pZ+Math.sin(angle)*shoulderLength-Math.sin(angle+Math.PI/2)*shoulderWidth2;
        node.fixed = true;

        node = nodes.get(0).get(nodes.get(0).size()-1);
        node.x = pX-Math.cos(angle)*shoulderLength-Math.cos(angle+Math.PI/2)*shoulderWidth2;
        node.y = pY+vertOffset-(player.isSneaking()?0.2:0);
        node.z = pZ-Math.sin(angle)*shoulderLength-Math.sin(angle+Math.PI/2)*shoulderWidth2;
        node.fixed = true;



        /*for(int i=0; i<horzNodes; i++) {
            Node node = nodes.get(0).get(i);

            node.x = pX-1;
            node.y = pY+2;
            node.z = pZ+((double)i)/(horzNodes-1);
        }*/
    }


}
