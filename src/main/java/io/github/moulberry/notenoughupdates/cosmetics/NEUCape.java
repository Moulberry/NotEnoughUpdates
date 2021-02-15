package io.github.moulberry.notenoughupdates.cosmetics;

import io.github.moulberry.notenoughupdates.util.TexLoc;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.security.Key;
import java.util.*;

public class NEUCape {

    private int currentFrame = 0;
    private int displayFrame = 0;
    private String capeName;
    public ResourceLocation[] capeTextures = null;

    private long lastFrameUpdate = 0;

    private static int ANIM_MODE_LOOP = 0;
    private static int ANIM_MODE_PINGPONG = 1;
    private int animMode = ANIM_MODE_LOOP;

    private List<List<CapeNode>> nodes = null;

    private Random random = new Random();

    private long eventMillis;
    private float eventLength;
    private float eventRandom;

    private static double vertOffset = 1.4;
    private static double shoulderLength = 0.24;
    private static double shoulderWidth = 0.13;

    public static final int HORZ_NODES = 6;
    public static final int VERT_NODES = 22;

    public static float targetDist = 1/20f;

    private EntityPlayer currentPlayer;

    private String shaderName = "cape";

    public NEUCape(String capeName) {
        setCapeTexture(capeName);
    }

    public void setCapeTexture(String capeName) {
        if(this.capeName != null && this.capeName.equalsIgnoreCase(capeName)) return;
        this.capeName = capeName;

        startTime = System.currentTimeMillis();

        if(capeName.equalsIgnoreCase("fade")) {
            shaderName = "fade_cape";
        } else if(capeName.equalsIgnoreCase("space")) {
            shaderName = "space_cape";
        } else if(capeName.equalsIgnoreCase("mcworld")) {
            shaderName = "mcworld_cape";
        } else if(capeName.equalsIgnoreCase("lava")) {
            shaderName = "lava_cape";
        } else if(capeName.equalsIgnoreCase("lightning")) {
            shaderName = "lightning_cape";
        } else if(capeName.equalsIgnoreCase("thebakery")) {
            shaderName = "biscuit_cape";
        } else if(capeName.equalsIgnoreCase("negative")) {
            shaderName = "negative";
        } else if(capeName.equalsIgnoreCase("void")) {
            shaderName = "void";
        } else {
            shaderName = "shiny_cape";
        }

        ResourceLocation staticCapeTex = new ResourceLocation("notenoughupdates:capes/"+capeName+".png");
        capeTextures = new ResourceLocation[1];
        capeTextures[0] = staticCapeTex;

        /*if(rlExists(staticCapeTex)) {
            capeTextures = new ResourceLocation[1];
            capeTextures[0] = staticCapeTex;
        } else {
            List<ResourceLocation> texs = new ArrayList<>();
            for(int i=0; i<99; i++) {
                ResourceLocation frame = new ResourceLocation(
                        "notenoughupdates:capes/"+capeName+"/"+capeName+"_"+String.format("%02d", i)+".png");
                if(rlExists(frame)) {
                    texs.add(frame);
                } else {
                    break;
                }
            }
            capeTextures = new ResourceLocation[texs.size()];
            for(int i=0; i<texs.size(); i++) {
                capeTextures[i] = texs.get(i);
            }
        }*/
    }

    private void bindTexture() {
        if(capeName.equalsIgnoreCase("negative")) {
            CapeManager.getInstance().updateWorldFramebuffer = true;
            if(CapeManager.getInstance().backgroundFramebuffer != null) {
                CapeManager.getInstance().backgroundFramebuffer.bindFramebufferTexture();
            }
        } else if(capeTextures != null && capeTextures.length>0) {
            long currentTime = System.currentTimeMillis();
            if(currentTime - lastFrameUpdate > 100) {
                lastFrameUpdate = currentTime/100*100;
                currentFrame++;

                if(animMode == ANIM_MODE_PINGPONG) {
                    if(capeTextures.length == 1) {
                        currentFrame = displayFrame = 0;
                    } else {
                        int frameCount = 2*capeTextures.length-2;
                        currentFrame %= frameCount;
                        displayFrame = currentFrame;
                        if(currentFrame >= capeTextures.length) {
                            displayFrame = frameCount - displayFrame;
                        }
                    }
                } else if(animMode == ANIM_MODE_LOOP) {
                    currentFrame %= capeTextures.length;
                    displayFrame = currentFrame;
                }
            }
            Minecraft.getMinecraft().getTextureManager().bindTexture(capeTextures[displayFrame]);
        }
    }

    public boolean rlExists(ResourceLocation loc) {
        try {
            return !Minecraft.getMinecraft().getResourceManager().getAllResources(loc).isEmpty();
        } catch(Exception e) {
            return false;
        }
    }

    public void createCapeNodes(EntityPlayer player) {
        nodes = new ArrayList<>();

        float pX = (float)player.posX % 7789;
        float pY = (float)player.posY;
        float pZ = (float)player.posZ % 7789;

        float uMinTop = 48/1024f;
        float uMaxTop = 246/1024f;
        float uMinBottom = 0/1024f;
        float uMaxBottom = 293/1024f;

        float vMaxSide = 404/1024f;
        float vMaxCenter = 419/1024f;

        for(int i=0; i<VERT_NODES; i++) {
            float uMin = uMinTop + (uMinBottom - uMinTop) * i/(float)(VERT_NODES-1);
            float uMax = uMaxTop + (uMaxBottom - uMaxTop) * i/(float)(VERT_NODES-1);

            List<CapeNode> row = new ArrayList<>();
            for(int j=0; j<HORZ_NODES; j++) {
                float vMin = 0f;
                float centerMult = 1-Math.abs(j-(HORZ_NODES-1)/2f)/((HORZ_NODES-1)/2f);//0-(horzCapeNodes)  -> 0-1-0
                float vMax = vMaxSide + (vMaxCenter - vMaxSide) * centerMult;

                CapeNode node = new CapeNode(pX, pY, pZ);//pX-1, pY+2-i*targetDist, pZ+(j-(horzCapeNodes-1)/2f)*targetDist*2
                node.texU = uMin + (uMax - uMin) * j/(float)(HORZ_NODES-1);
                node.texV = vMin + (vMax - vMin) * i/(float)(VERT_NODES-1);

                node.horzDistMult = 2f+1f*i/(float)(VERT_NODES-1);

                if(j == 0 || j == HORZ_NODES-1) {
                    node.horzSideTexU = 406/1024f * i/(float)(VERT_NODES-1);
                    if(j == 0) {
                        node.horzSideTexVTop = 1 - 20/1024f;
                    } else {
                        node.horzSideTexVTop = 1 - 40/1024f;
                    }
                }
                if(i == 0) {
                    node.vertSideTexU = 198/1024f * j/(float)(HORZ_NODES-1);
                    node.vertSideTexVTop = 1 - 60/1024f;
                } else if(i == VERT_NODES-1) {
                    node.vertSideTexU = 300/1024f * j/(float)(HORZ_NODES-1);
                    node.vertSideTexVTop = 1 - 80/1024f;
                }
                row.add(node);
            }

            nodes.add(row);
        }
        for(int y=0; y<nodes.size(); y++) {
            int xSize = nodes.get(y).size();
            for(int x=0; x<xSize; x++) {
                CapeNode node = nodes.get(y).get(x);

                for(Direction dir : Direction.values()) {
                    for(int i=1; i<=2; i++) {
                        Offset offset = new Offset(dir, i);

                        int xNeighbor = x+offset.getXOffset();
                        int yNeighbor = y+offset.getYOffset();

                        if(xNeighbor >= 0 && xNeighbor < nodes.get(y).size()
                                && yNeighbor >= 0 && yNeighbor < nodes.size()) {
                            CapeNode neighbor = nodes.get(yNeighbor).get(xNeighbor);
                            node.setNeighbor(offset, neighbor);
                        }
                    }
                }
            }
        }
    }

    public void ensureCapeNodesCreated(EntityPlayer player) {
        if(nodes == null) createCapeNodes(player);
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

        public Direction rotateRight90() {
            int wantXOff = -yOff;
            int wantYOff = xOff;
            for(Direction dir : values()) {
                if(dir.xOff == wantXOff && dir.yOff == wantYOff) {
                    return dir;
                }
            }
            return this;
        }

        public Direction rotateLeft90() {
            int wantXOff = yOff;
            int wantYOff = -xOff;
            for(Direction dir : values()) {
                if(dir.xOff == wantXOff && dir.yOff == wantYOff) {
                    return dir;
                }
            }
            return this;
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

    private void loadShaderUniforms(ShaderManager shaderManager) {
        String shaderId = "capes/"+shaderName+"/"+shaderName;
        if(shaderName.equalsIgnoreCase("fade_cape")) {
            shaderManager.loadData(shaderId, "millis", (int)(System.currentTimeMillis()-startTime));
        } else if(shaderName.equalsIgnoreCase("space_cape")) {
            shaderManager.loadData(shaderId, "millis", (int)(System.currentTimeMillis()-startTime));
            shaderManager.loadData(shaderId, "eventMillis", (int)(System.currentTimeMillis()-eventMillis));
            shaderManager.loadData(shaderId, "eventRand", eventRandom);
        } else if(shaderName.equalsIgnoreCase("mcworld_cape")) {
            shaderManager.loadData(shaderId, "millis", (int) (System.currentTimeMillis() - startTime));
        } else if(shaderName.equalsIgnoreCase("lava_cape")) {
            shaderManager.loadData(shaderId, "millis", (int) (System.currentTimeMillis() - startTime));
        } else if(shaderName.equalsIgnoreCase("lightning_cape")) {
            shaderManager.loadData(shaderId, "millis", (int) (System.currentTimeMillis() - startTime));
        } else if(shaderName.equalsIgnoreCase("biscuit_cape") || shaderName.equalsIgnoreCase("shiny_cape")) {
            shaderManager.loadData(shaderId, "millis", (int) (System.currentTimeMillis() - startTime));
            shaderManager.loadData(shaderId, "eventMillis", (int)(System.currentTimeMillis()-eventMillis));
        } else if(shaderName.equalsIgnoreCase("negative")) {
            shaderManager.loadData(shaderId, "screensize", new Vector2f(
                    Minecraft.getMinecraft().displayWidth,
                    Minecraft.getMinecraft().displayHeight
            ));
        } else if(shaderName.equalsIgnoreCase("void")) {
            shaderManager.loadData(shaderId, "millis", (int) (System.currentTimeMillis() - startTime));
            shaderManager.loadData(shaderId, "screensize", new Vector2f(
                    Minecraft.getMinecraft().displayWidth,
                    Minecraft.getMinecraft().displayHeight
            ));
        }
    }

    long lastRender = 0;
    public void onRenderPlayer(RenderPlayerEvent.Post e) {
        EntityPlayer player = e.entityPlayer;

        if(currentPlayer != null && currentPlayer != player) return;

        if(player.getActivePotionEffect(Potion.invisibility) != null) return;
        if(player.isSpectator() || player.isInvisible()) return;

        ensureCapeNodesCreated(player);

        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        double viewerX = (viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * e.partialRenderTick) % 7789;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * e.partialRenderTick;
        double viewerZ = (viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * e.partialRenderTick) % 7789;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        bindTexture();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableCull();

        if(shaderName.equals("mcworld_cape")) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        } else {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }

        GL11.glTranslatef(-(float)viewerX, -(float)viewerY, -(float)viewerZ);

        ShaderManager shaderManager = ShaderManager.getInstance();
        shaderManager.loadShader("capes/"+shaderName+"/"+shaderName);
        loadShaderUniforms(shaderManager);

        renderCape(player, e.partialRenderTick);

        GL11.glTranslatef((float)viewerX, (float)viewerY, (float)viewerZ);

        GL20.glUseProgram(0);

        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        lastRender = System.currentTimeMillis();
    }

    private boolean notRendering = false;
    public void onTick(TickEvent.ClientTickEvent event, EntityPlayer player) {
        if(player == null) return;

        if(System.currentTimeMillis() - lastRender < 500) {
            if(currentPlayer == null) {
                currentPlayer = player;
            } else if(currentPlayer != player) {
                return;
            }

            ensureCapeNodesCreated(player);

            for(int y=0; y<nodes.size(); y++) {
                for(int x=0; x<nodes.get(y).size(); x++) {
                    CapeNode node = nodes.get(y).get(x);
                    node.lastPosition.x = node.position.x;
                    node.lastPosition.y = node.position.y;
                    node.lastPosition.z = node.position.z;
                }
            }
            updateCape(player);

            notRendering = false;
        } else {
            currentPlayer = null;

            notRendering = true;
        }
    }

    private Vector3f updateFixedCapeNodes(EntityPlayer player) {
        double pX = player.posX % 7789;//player.lastTickPosX + (player.posX - player.lastTickPosX) * partialRenderTick;
        double pY = player.posY;//player.lastTickPosY + (player.posY - player.lastTickPosY) * partialRenderTick;
        double pZ = player.posZ % 7789;//player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialRenderTick;
        double angle = Math.toRadians(player.renderYawOffset);

        double vertOffset2 = vertOffset + (player.isSneaking() ? -0.22f : 0) + (player.getCurrentArmor(2) != null ? 0.06f : 0);
        double shoulderWidth2 = shoulderWidth + (player.getCurrentArmor(2) != null ? 0.08f : 0);

        float xOff = (float)(Math.cos(angle)*shoulderLength);
        float zOff = (float)(Math.sin(angle)*shoulderLength);

        float totalDX = 0;
        float totalDY = 0;
        float totalDZ = 0;
        int totalMovements = 0;

        int xSize = nodes.get(0).size();
        for(int i=0; i<xSize; i++) {
            float mult = 1 - 2f*i/(xSize-1); //1 -> -1
            float widthMult = 1.25f-(1.414f*i/(xSize-1) - 0.707f)*(1.414f*i/(xSize-1) - 0.707f);
            CapeNode node = nodes.get(0).get(i);
            float x = (float)pX+(float)(xOff*mult-widthMult*Math.cos(angle+Math.PI/2)*shoulderWidth2);
            float y = (float)pY+(float)(vertOffset2);
            float z = (float)pZ+(float)(zOff*mult-widthMult*Math.sin(angle+Math.PI/2)*shoulderWidth2);
            totalDX += x - node.position.x;
            totalDY += y - node.position.y;
            totalDZ += z - node.position.z;
            totalMovements++;
            node.position.x = x;
            node.position.y = y;
            node.position.z = z;
            node.fixed = true;
        }

        float avgDX = totalDX/totalMovements;
        float avgDY = totalDY/totalMovements;
        float avgDZ = totalDZ/totalMovements;

        return new Vector3f(avgDX, avgDY, avgDZ);
    }

    private void updateFixedCapeNodesPartial(EntityPlayer player, float partialRenderTick) {
        double pX = (player.lastTickPosX + (player.posX - player.lastTickPosX) * partialRenderTick) % 7789;
        double pY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialRenderTick;
        double pZ = (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialRenderTick) % 7789;
        double angle = Math.toRadians(player.renderYawOffset);

        double vertOffset2 = vertOffset + (player.isSneaking() ? -0.22f : 0) + (player.getCurrentArmor(2) != null ? 0.06f : 0);
        double shoulderWidth2 = shoulderWidth + (player.getCurrentArmor(2) != null ? 0.08f : 0);

        float xOff = (float)(Math.cos(angle)*shoulderLength);
        float zOff = (float)(Math.sin(angle)*shoulderLength);

        int xSize = nodes.get(0).size();
        for(int i=0; i<xSize; i++) {
            float mult = 1 - 2f*i/(xSize-1); //1 -> -1
            float widthMult = 1.25f-(1.414f*i/(xSize-1) - 0.707f)*(1.414f*i/(xSize-1) - 0.707f);
            CapeNode node = nodes.get(0).get(i);
            node.renderPosition.x = (float)pX+(float)(xOff*mult-widthMult*Math.cos(angle+Math.PI/2)*shoulderWidth2);
            node.renderPosition.y = (float)pY+(float)(vertOffset2);
            node.renderPosition.z = (float)pZ+(float)(zOff*mult-widthMult*Math.sin(angle+Math.PI/2)*shoulderWidth2);
            node.fixed = true;
        }
    }

    TexLoc tl = new TexLoc(10, 75, Keyboard.KEY_M);

    private double deltaAngleAccum;
    private double oldPlayerAngle;
    private int crouchTicks = 0;
    long startTime = 0;
    private void updateCape(EntityPlayer player) {
        Vector3f capeTranslation = updateFixedCapeNodes(player);

        if(shaderName.equals("space_cape")) {
            long currentTime = System.currentTimeMillis();
            if(currentTime-startTime > eventMillis-startTime+eventLength) {
                eventMillis = currentTime;
                eventLength = random.nextFloat()*2000+4000;
                eventRandom = random.nextFloat();
            }
        } else if(shaderName.equals("biscuit_cape") || shaderName.equals("shiny_cape")) {
            long currentTime = System.currentTimeMillis();
            if(currentTime-startTime > eventMillis-startTime+eventLength) {
                eventMillis = currentTime;
                eventLength = random.nextFloat()*3000+3000;
            }
        }

        if(notRendering) {
            for (int y = 0; y < nodes.size(); y++) {
                for (int x = 0; x < nodes.get(y).size(); x++) {
                    CapeNode node = nodes.get(y).get(x);
                    if(!node.fixed) {
                        Vector3f.add(node.position, capeTranslation, node.position);
                        node.lastPosition.set(node.position);
                        node.renderPosition.set(node.position);
                    }
                }
            }
        }

        double playerAngle = Math.toRadians(player.renderYawOffset);
        double deltaAngle = playerAngle - oldPlayerAngle;
        if(deltaAngle > Math.PI) {
            deltaAngle = 2*Math.PI - deltaAngle;
        }
        if(deltaAngle < -Math.PI) {
            deltaAngle = 2*Math.PI + deltaAngle;
        }
        deltaAngleAccum *= 0.5f;
        deltaAngleAccum += deltaAngle;

        float dX = (float)Math.cos(playerAngle+Math.PI/2f);
        float dZ = (float)Math.sin(playerAngle+Math.PI/2f);

        float factor = (float)(deltaAngleAccum*deltaAngleAccum);

        tl.handleKeyboardInput();

        float capeTransLength = capeTranslation.length();

        float capeTranslationFactor = 0f;
        if(capeTransLength > 0.5f) {
            capeTranslationFactor = (capeTransLength-0.5f)/capeTransLength;
        }
        Vector3f lookDir = new Vector3f(dX, 0, dZ);
        Vector3f lookDirNorm = lookDir.normalise(null);
        float dot = Vector3f.dot(capeTranslation, lookDirNorm);
        if(dot < 0) { //Moving backwards
            for(int y=0; y<nodes.size(); y++) {
                for(int x=0; x<nodes.get(y).size(); x++) {
                    CapeNode node = nodes.get(y).get(x);
                    if(!node.fixed) {
                        node.position.x += lookDirNorm.x*dot;
                        node.position.y += lookDirNorm.y*dot;
                        node.position.z += lookDirNorm.z*dot;
                    }
                }
            }
            //Apply small backwards force
            factor = 0.05f;
        }

        if(factor > 0) {
            for(int y=0; y<nodes.size(); y++) {
                for(int x=0; x<nodes.get(y).size(); x++) {
                    nodes.get(y).get(x).applyForce(-dX*factor, 0, -dZ*factor);
                }
            }
        }

        if(capeTranslationFactor > 0f) {
            float capeDX = capeTranslation.x*capeTranslationFactor;
            float capeDY = capeTranslation.y*capeTranslationFactor;
            float capeDZ = capeTranslation.z*capeTranslationFactor;

            for(int y=0; y<nodes.size(); y++) {
                for(int x=0; x<nodes.get(y).size(); x++) {
                    CapeNode node = nodes.get(y).get(x);
                    if(!node.fixed) {
                        node.position.x += capeDX;
                        node.position.y += capeDY;
                        node.position.z += capeDZ;
                    }
                }
            }
        }

        //Wind
        float currTime = (System.currentTimeMillis()-startTime)/1000f;
        float windRandom = Math.abs((float)(0.5f*Math.sin(0.22f*currTime)+Math.sin(0.44f*currTime)*Math.sin(0.47f*currTime)));
        double windDir = playerAngle+Math.PI/4f*Math.sin(0.2f*currTime);

        float windDX = (float)Math.cos(windDir+Math.PI/2f);
        float windDZ = (float)Math.sin(windDir+Math.PI/2f);
        for(int y=0; y<nodes.size(); y++) {
            for(int x=0; x<nodes.get(y).size(); x++) {
                nodes.get(y).get(x).applyForce(-windDX*windRandom*0.01f, 0, -windDZ*windRandom*0.01f);
            }
        }

        if(player.isSneaking()) {
            crouchTicks++;
            float mult = 0.5f;
            if(crouchTicks < 5) {
                mult = 2f;
            }
            for(int y=0; y<8; y++) {
                for(int x=0; x<nodes.get(y).size(); x++) {
                    nodes.get(y).get(x).applyForce(-dX*mult, 0, -dZ*mult);
                }
            }
        } else {
            crouchTicks = 0;
        }

        oldPlayerAngle = playerAngle;

        for(int y=0; y<nodes.size(); y++) {
            for(int x=0; x<nodes.get(y).size(); x++) {
                nodes.get(y).get(x).update();
            }
        }
        int updates = player == Minecraft.getMinecraft().thePlayer ? 50 : 50;
        for(int i=0; i<updates; i++) {
            for(int y=0; y<nodes.size(); y++) {
                for(int x=0; x<nodes.get(y).size(); x++) {
                    nodes.get(y).get(x).resolveAll(2+1f*y/nodes.size(), false);
                }
            }
        }
    }

    private int ssbo = -1;
    private void generateSSBO() {
        ssbo = GL15.glGenBuffers();
        loadSBBO();
    }

    private void loadSBBO() {
        FloatBuffer buff = BufferUtils.createFloatBuffer(CapeNode.FLOAT_NUM*HORZ_NODES*VERT_NODES);
        for(int y=0; y<VERT_NODES; y++) {
            for(int x=0; x<HORZ_NODES; x++) {
                nodes.get(y).get(x).loadIntoBuffer(buff);
            }
        }
        buff.flip();

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssbo);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, buff, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    private void resolveAllCompute() {
        if(ssbo == -1) {
            generateSSBO();
        }
        loadSBBO();

        int program = ShaderManager.getInstance().getShader("node");

        int block_index = GL43.glGetProgramResourceIndex(program, GL43.GL_SHADER_STORAGE_BLOCK, "nodes_buffer");
        int ssbo_binding_point_index = 0;
        GL43.glShaderStorageBlockBinding(program, block_index, ssbo_binding_point_index);
        int binding_point_index = 0;
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, binding_point_index, ssbo);

        GL20.glUseProgram(program);

        for(int i=0; i<30; i++) {
            GL43.glDispatchCompute(VERT_NODES, 1, 1);
            GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
        }

        GL20.glUseProgram(0);

        FloatBuffer buff = BufferUtils.createFloatBuffer(CapeNode.FLOAT_NUM*HORZ_NODES*VERT_NODES);

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssbo);
        GL15.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, buff);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

        for(int y=0; y<VERT_NODES; y++) {
            for(int x=0; x<HORZ_NODES; x++) {
                nodes.get(y).get(x).readFromBuffer(buff);
            }
        }
    }

    private Vector3f avgFixedRenderPosition() {
        Vector3f accum = new Vector3f();
        int numFixed = 0;
        for(int y=0; y<nodes.size(); y++) {
            for(int x=0; x<nodes.get(y).size(); x++) {
                CapeNode node = nodes.get(y).get(x);
                if(node.fixed) {
                    Vector3f.add(accum, node.renderPosition, accum);
                    numFixed++;
                }
            }
        }
        if(numFixed != 0) {
            accum.scale(1f/numFixed);
        }
        return accum;
    }

    private void renderCape(EntityPlayer player, float partialRenderTick) {
        ensureCapeNodesCreated(player);
        if(System.currentTimeMillis() - lastRender > 500) {
            updateCape(player);
        }
        updateFixedCapeNodesPartial(player, partialRenderTick);

        for(int y=0; y<nodes.size(); y++) {
            for(int x=0; x<nodes.get(y).size(); x++) {
                CapeNode node = nodes.get(y).get(x);

                node.resetNormal();

                if(node.fixed) continue;

                Vector3f avgPositionFixed = avgFixedRenderPosition();

                Vector3f newPosition = new Vector3f();
                newPosition.x = node.lastPosition.x + (node.position.x - node.lastPosition.x) * partialRenderTick;
                newPosition.y = node.lastPosition.y + (node.position.y - node.lastPosition.y) * partialRenderTick;
                newPosition.z = node.lastPosition.z + (node.position.z - node.lastPosition.z) * partialRenderTick;

                int length = node.oldRenderPosition.length;
                int fps = Minecraft.getDebugFPS();
                if(fps < 50) {
                    length = 2;
                } else if(fps < 100) {
                    length = 2+(int)((fps-50)/50f*3);
                }

                if(node.oldRenderPosition[length-1] == null) {
                    node.renderPosition = newPosition;
                } else {
                    Vector3f accum = new Vector3f();
                    for(int i=0; i<length; i++) {
                        Vector3f.add(accum, node.oldRenderPosition[i], accum);
                        Vector3f.add(accum, avgPositionFixed, accum);
                    }
                    accum.scale(1/(float)length);

                    float blendFactor = 0.5f+0.3f*y/(float)(nodes.size()-1); //0.5/0.5 -> 0.8/0.2 //0-1
                    accum.scale(blendFactor);
                    newPosition.scale(1-blendFactor);
                    Vector3f.add(accum, newPosition, accum);
                    node.renderPosition = accum;
                }

                for(int i=node.oldRenderPosition.length-1; i>=0; i--) {
                    if(i > 0) {
                        node.oldRenderPosition[i] = node.oldRenderPosition[i-1];
                    } else {
                        node.oldRenderPosition[i] = Vector3f.sub(node.renderPosition, avgPositionFixed, null);
                    }
                }
            }
        }
        for(int y=0; y<nodes.size(); y++) {
            for(int x=0; x<nodes.get(y).size(); x++) {
                nodes.get(y).get(x).renderNode();
            }
        }
    }

}
