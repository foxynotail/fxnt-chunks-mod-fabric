package net.fxnt.fxntchunks.chunkloading;

import net.minecraft.world.level.ChunkPos;

public class ForceLoadedChunkData {
    public String dimension;
    public ChunkPos chunkPos;
    public String type;
    public String playerID;
    public String playerName;

    public ForceLoadedChunkData(String dimension, ChunkPos chunkPos, String type, String playerID, String playerName) {
        this.dimension = dimension;
        this.chunkPos = chunkPos;
        this.type = type;
        this.playerID = playerID;
        this.playerName = playerName;
    }
}
