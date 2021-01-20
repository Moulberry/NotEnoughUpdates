package io.github.moulberry.notenoughupdates.miscfeatures;

import io.github.moulberry.notenoughupdates.util.SBInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DwarvenMinesTextures {

    private static final byte biomeId1 = (byte)(BiomeGenBase.extremeHillsEdge.biomeID & 255);
    private static final byte[] biomeMap1 = new byte[16*16];
    private static final byte biomeId2 = (byte)(BiomeGenBase.extremeHillsPlus.biomeID & 255);
    private static final byte[] biomeMap2 = new byte[16*16];
    static {
        Arrays.fill(biomeMap1, biomeId1);
        Arrays.fill(biomeMap2, biomeId2);
    }

    public static void tick() {
        if(Minecraft.getMinecraft().theWorld == null) return;

        if(SBInfo.getInstance().getLocation() == null) return;
        if(!SBInfo.getInstance().getLocation().equals("mining_3")) return;

        int playerX = (int)Minecraft.getMinecraft().thePlayer.posX;
        int playerZ = (int)Minecraft.getMinecraft().thePlayer.posZ;

        for(int xC=-10; xC<=10; xC++) {
            for(int zC=-10; zC<=10; zC++) {
                ChunkCoordIntPair pair = new ChunkCoordIntPair(playerX/16+xC, playerZ/16+zC);

                if(!ignoredChunks.contains(pair)) {
                    Minecraft.getMinecraft().theWorld.getChunkFromChunkCoords(pair.chunkXPos, pair.chunkZPos).setBiomeArray(biomeMap1);
                } else {
                    Minecraft.getMinecraft().theWorld.getChunkFromChunkCoords(pair.chunkXPos, pair.chunkZPos).setBiomeArray(biomeMap2);
                }
            }
        }
    }

    private static Set<ChunkCoordIntPair> ignoredChunks = new HashSet<>();
    static {
        ignoredChunks.add(new ChunkCoordIntPair(9, 3));
        ignoredChunks.add(new ChunkCoordIntPair(6, 0));
        ignoredChunks.add(new ChunkCoordIntPair(0, -4));
        ignoredChunks.add(new ChunkCoordIntPair(1, -6));
        ignoredChunks.add(new ChunkCoordIntPair(-1, -3));
        ignoredChunks.add(new ChunkCoordIntPair(6, 5));
        ignoredChunks.add(new ChunkCoordIntPair(-1, -2));
        ignoredChunks.add(new ChunkCoordIntPair(8, -1));
        ignoredChunks.add(new ChunkCoordIntPair(8, -2));
        ignoredChunks.add(new ChunkCoordIntPair(6, 6));
        ignoredChunks.add(new ChunkCoordIntPair(6, 1));
        ignoredChunks.add(new ChunkCoordIntPair(9, -1));
        ignoredChunks.add(new ChunkCoordIntPair(9, 4));
        ignoredChunks.add(new ChunkCoordIntPair(8, 0));
        ignoredChunks.add(new ChunkCoordIntPair(9, 2));
        ignoredChunks.add(new ChunkCoordIntPair(1, -4));
        ignoredChunks.add(new ChunkCoordIntPair(0, -6));
        ignoredChunks.add(new ChunkCoordIntPair(-1, -5));
        ignoredChunks.add(new ChunkCoordIntPair(9, 1));
        ignoredChunks.add(new ChunkCoordIntPair(9, 6));
        ignoredChunks.add(new ChunkCoordIntPair(-1, -6));
        ignoredChunks.add(new ChunkCoordIntPair(6, 4));
        ignoredChunks.add(new ChunkCoordIntPair(1, -3));
        ignoredChunks.add(new ChunkCoordIntPair(9, 5));
        ignoredChunks.add(new ChunkCoordIntPair(1, -2));
        ignoredChunks.add(new ChunkCoordIntPair(0, -5));
        ignoredChunks.add(new ChunkCoordIntPair(7, -1));
        ignoredChunks.add(new ChunkCoordIntPair(7, -2));
        ignoredChunks.add(new ChunkCoordIntPair(9, 0));
        ignoredChunks.add(new ChunkCoordIntPair(6, 3));
        ignoredChunks.add(new ChunkCoordIntPair(0, -3));
        ignoredChunks.add(new ChunkCoordIntPair(-1, -4));
        ignoredChunks.add(new ChunkCoordIntPair(1, -5));
        ignoredChunks.add(new ChunkCoordIntPair(6, 2));
        ignoredChunks.add(new ChunkCoordIntPair(0, -2));
        ignoredChunks.add(new ChunkCoordIntPair(-2, -4));
        ignoredChunks.add(new ChunkCoordIntPair(-2, -5));
        ignoredChunks.add(new ChunkCoordIntPair(-2, -6));
        ignoredChunks.add(new ChunkCoordIntPair(-1, -7));
        ignoredChunks.add(new ChunkCoordIntPair(0, -7));
        ignoredChunks.add(new ChunkCoordIntPair(1, -7));
    }

}
